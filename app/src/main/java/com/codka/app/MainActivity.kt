package com.codka.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.codka.app.ui.CodkaApp
import com.codka.app.ui.theme.CodkaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark status-bar icons on Codka's light background.
        enableEdgeToEdge()
        setContent {
            CodkaTheme {
                CodkaApp()
            }
        }
    }
}
