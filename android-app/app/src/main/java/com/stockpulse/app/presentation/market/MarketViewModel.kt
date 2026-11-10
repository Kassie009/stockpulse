package com.stockpulse.app.presentation.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.MarketQuote
import com.stockpulse.app.domain.repository.MarketRepository
import com.stockpulse.app.domain.repository.MarketResult
import com.stockpulse.app.domain.repository.MarketSearchResult
import com.stockpulse.app.domain.repository.TradingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val stocks: List<MarketQuote> = emptyList(),
    val recordedAt: String? = null,
    val errorMessage: String? = null,
    // Busqueda
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<MarketSearchResult> = emptyList(),
    // Cantidad de acciones que posee el usuario (para el dialogo de venta)
    val heldQuantity: Map<String, Int> = emptyMap()
)

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val marketRepository: MarketRepository,
    private val tradingRepository: TradingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state

    private var searchJob: Job? = null

    init {
        loadPopular()
        loadHeldQuantity()
        autoRefreshLoop()
        viewModelScope.launch {
            com.stockpulse.app.data.sync.SyncBus.signal.collect {
                loadPopular()
                loadHeldQuantity()
            }
        }
    }

    /** Actualiza los precios solo cada 30s (los veras moverse sin tocar). */
    private fun autoRefreshLoop() {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                loadPopular()
            }
        }
    }

    /** Cuantas acciones posee el usuario por simbolo (info para el dialogo). */
    private fun loadHeldQuantity() {
        viewModelScope.launch {
            val summary = (tradingRepository.summary() as? com.stockpulse.app.domain.repository.PortfolioResult.Success)?.summary
            if (summary != null) {
                _state.update {
                    it.copy(heldQuantity = summary.holdings.associate { h -> h.symbol to h.quantity })
                }
            }
        }
    }

    /** Refresca las cantidades poseidas (tras una operacion). */
    fun refreshHoldings() = loadHeldQuantity()

    fun loadPopular() {
        _state.update { it.copy(isRefreshing = it.stocks.isNotEmpty()) }
        viewModelScope.launch {
            when (val result = marketRepository.popular()) {
                is com.stockpulse.app.domain.repository.MarketResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            stocks = result.stocks,
                            recordedAt = result.recordedAt,
                            errorMessage = null
                        )
                    }
                }
                is com.stockpulse.app.domain.repository.MarketResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        searchJob?.cancel()
        _state.update { it.copy(query = query) }

        if (query.trim().length < 2) {
            _state.update { it.copy(isSearching = false, searchResults = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            delay(600) // debounce: no spamear el backend mientras escribes
            when (val result = marketRepository.search(query.trim())) {
                is com.stockpulse.app.domain.repository.SearchStocksResult.Success ->
                    _state.update {
                        it.copy(isSearching = false, searchResults = result.results)
                    }
                is com.stockpulse.app.domain.repository.SearchStocksResult.Failure ->
                    _state.update {
                        it.copy(isSearching = false, searchResults = emptyList())
                    }
            }
        }
    }
}
