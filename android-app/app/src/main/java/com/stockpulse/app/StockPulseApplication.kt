package com.stockpulse.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application de STOCKPULSE que activa Hilt (requerida cuando
 * se usa @AndroidEntryPoint en MainActivity).
 */
@HiltAndroidApp
class StockPulseApplication : Application()
