package com.drakahell.claudesearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.drakahell.claudesearch.ui.home.HomeRoute
import com.drakahell.claudesearch.ui.theme.ClaudeSearchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClaudeSearchTheme {
                HomeRoute()
            }
        }
    }
}
