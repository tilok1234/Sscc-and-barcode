package com.ssccscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ssccscanner.ui.AppRoot
import com.ssccscanner.ui.theme.SsccScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SsccScannerTheme {
                AppRoot()
            }
        }
    }
}
