package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BuildProgressScreen
import com.example.ui.HomeScreen
import com.example.ui.LivePreviewScreen
import com.example.ui.ProjectBuilderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.HTMLToAPKViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true) {
        val viewModel: HTMLToAPKViewModel = viewModel()
        val currentScreen by viewModel.currentScreen.collectAsState()

        Scaffold(
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          when (currentScreen) {
            AppScreen.HOME -> {
              HomeScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
              )
            }
            AppScreen.BUILDER -> {
              ProjectBuilderScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
              )
            }
            AppScreen.PROGRESS -> {
              BuildProgressScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
              )
            }
            AppScreen.LIVE_PREVIEW -> {
              LivePreviewScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
              )
            }
          }
        }
      }
    }
  }
}
