package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AppScreen
import com.example.viewmodel.HTMLToAPKViewModel
import com.example.webview.GeneratedAppWebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePreviewScreen(
  viewModel: HTMLToAPKViewModel,
  modifier: Modifier = Modifier
) {
  val config by viewModel.config.collectAsState()
  val logs by viewModel.previewLogs.collectAsState()
  var showConsole by remember { mutableStateOf(false) }

  BackHandler {
    viewModel.navigateTo(AppScreen.HOME)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    TopAppBar(
      title = {
        Column {
          Text(
            text = "Live Interactive Preview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${config.appTitle} • ${config.packageName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      navigationIcon = {
        IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
      },
      actions = {
        IconButton(onClick = { showConsole = !showConsole }) {
          Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = "Console Logs",
            tint = if (showConsole) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
          )
        }
        Button(
          onClick = {
            viewModel.setBuilderStep(5)
            viewModel.navigateTo(AppScreen.BUILDER)
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
          modifier = Modifier.padding(end = 8.dp)
        ) {
          Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Build APK", fontSize = 12.sp)
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
      )
    )

    // Main Live Preview WebView Container
    Box(modifier = Modifier.weight(1f)) {
      GeneratedAppWebView(
        config = config,
        modifier = Modifier.fillMaxSize(),
        onConsoleLog = { logMsg ->
          viewModel.addPreviewLog(logMsg)
        }
      )

      // Collapsible Console Drawer Overlay
      androidx.compose.animation.AnimatedVisibility(
        visible = showConsole,
        modifier = Modifier.align(Alignment.BottomCenter)
      ) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(8.dp)
            .testTag("preview_console_overlay"),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
          Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "JavaScript Console (${logs.size})",
                color = Color(0xFF38BDF8),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Row {
                IconButton(onClick = { viewModel.clearPreviewLogs() }, modifier = Modifier.size(24.dp)) {
                  Text("CLR", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { showConsole = false }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                }
              }
            }

            LazyColumn(
              modifier = Modifier.fillMaxSize().padding(top = 6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              if (logs.isEmpty()) {
                item {
                  Text(
                    text = "No console outputs recorded yet. Call console.log() or AndroidBridge in HTML.",
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                  )
                }
              } else {
                items(logs) { log ->
                  Text(
                    text = "> $log",
                    color = Color(0xFFCBD5E1),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
