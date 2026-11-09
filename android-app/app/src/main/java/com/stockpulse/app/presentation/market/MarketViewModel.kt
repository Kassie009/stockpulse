package com.stockpulse.app.presentation.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.MarketQuote
import com.stockpulse.app.domain.repository.MarketRepository
import com.stockpulse.app.domain.repository.MarketSearchResult
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
    val searchResults: List<MarketSearchResult> = emptyList()
)

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state

    private var searchJob: Job? = null

    init {
        loadPopular()
    }

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
