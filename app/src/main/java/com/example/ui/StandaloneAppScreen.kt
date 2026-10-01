package com.example.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppBuildConfig
import com.example.webview.GeneratedAppWebView
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneAppScreen(
  config: AppBuildConfig,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showSplash by remember { mutableStateOf(true) }

  // Load splash or icon image from APK assets
  val splashBitmap = remember {
    try {
      context.assets.open("splash_image.png").use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
      try {
        context.assets.open("app_icon.png").use { BitmapFactory.decodeStream(it) }
      } catch (_: Exception) {
        null
      }
    }
  }

  LaunchedEffect(Unit) {
    // Show splash for 1.8 seconds while web view initializes
    delay(1800)
    showSplash = false
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      if (config.enableTitlebar && !showSplash) {
        TopAppBar(
          title = {
            Text(
              text = config.appTitle,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface
          )
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Main Web Application View
      GeneratedAppWebView(
        config = config,
        modifier = Modifier.fillMaxSize()
      )

      // Splash Screen Overlay
      AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
          ) {
            if (splashBitmap != null) {
              Image(
                bitmap = splashBitmap.asImageBitmap(),
                contentDescription = config.appTitle,
                modifier = Modifier
                  .size(110.dp)
                  .clip(RoundedCornerShape(22.dp))
              )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
              text = config.appTitle,
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )

            Spacer(modifier = Modifier.height(28.dp))

            CircularProgressIndicator(
              modifier = Modifier.size(32.dp),
              color = Color(0xFF6366F1),
              strokeWidth = 3.dp
            )
          }
        }
      }
    }
  }
}
