package com.codka.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.codka.app.ui.home.HomeScreen
import com.codka.app.ui.theme.CodkaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodkaTheme {
                HomeScreen()
            }
        }
    }
}
