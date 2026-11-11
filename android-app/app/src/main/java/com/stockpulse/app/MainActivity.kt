package com.stockpulse.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.stockpulse.app.navigation.StockPulseApp
import com.stockpulse.app.ui.theme.StockPulseTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity del telefono (FASE 9.5):
 * - FragmentActivity: requisito del BiometricPrompt (huella/PIN)
 * - FLAG_SECURE: la app se ve negra en el multitarea y no es capturable
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent {
            StockPulseTheme {
                StockPulseApp()
            }
        }
    }
}
