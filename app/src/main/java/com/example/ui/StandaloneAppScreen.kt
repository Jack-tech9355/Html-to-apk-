package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.AppBuildConfig
import com.example.webview.GeneratedAppWebView

@Composable
fun StandaloneAppScreen(
  config: AppBuildConfig,
  modifier: Modifier = Modifier
) {
  Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
    GeneratedAppWebView(
      config = config,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    )
  }
}
