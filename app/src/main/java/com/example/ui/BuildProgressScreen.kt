package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildLogEntry
import com.example.model.BuildResult
import com.example.model.LogLevel
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BuildUiState
import com.example.viewmodel.HTMLToAPKViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildProgressScreen(
  viewModel: HTMLToAPKViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val buildState by viewModel.buildState.collectAsState()
  val listState = rememberLazyListState()

  BackHandler {
    viewModel.navigateTo(AppScreen.HOME)
  }

  // Auto-scroll logs to bottom
  val currentLogs = when (val state = buildState) {
    is BuildUiState.Building -> state.logs
    is BuildUiState.Success -> state.result.logs
    is BuildUiState.Error -> state.logs
    else -> emptyList()
  }

  LaunchedEffect(currentLogs.size) {
    if (currentLogs.isNotEmpty()) {
      listState.animateScrollToItem(currentLogs.size - 1)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    TopAppBar(
      title = {
        Text(
          text = when (buildState) {
            is BuildUiState.Success -> "Compilation Complete"
            is BuildUiState.Error -> "Compilation Failed"
            else -> "Compiling Android APK"
          },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      },
      navigationIcon = {
        IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
      )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      when (val state = buildState) {
        is BuildUiState.Building -> {
          BuildingProgressHeader(progress = state.progress, statusText = state.statusText)
        }
        is BuildUiState.Success -> {
          BuildSuccessHeader(
            result = state.result,
            onInstall = { viewModel.installApk(context, state.result) },
            onSave = { viewModel.saveApkToDownloads(context, state.result) },
            onShare = { viewModel.shareApk(context, state.result) },
            onPreview = { viewModel.navigateTo(AppScreen.LIVE_PREVIEW) }
          )
        }
        is BuildUiState.Error -> {
          BuildErrorHeader(
            error = state.error,
            onRetry = { viewModel.startBuild() },
            onEdit = {
              viewModel.setBuilderStep(1)
              viewModel.navigateTo(AppScreen.BUILDER)
            }
          )
        }
        BuildUiState.Idle -> {
          Text("No active build task.")
        }
      }

      // Terminal Log Console Card
      Text(
        text = "Compiler Console Stream",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
      )

      Card(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .testTag("compiler_log_console"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
      ) {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(currentLogs) { entry ->
            LogEntryRow(entry)
          }
        }
      }

      // Bottom Return Home Button
      if (buildState !is BuildUiState.Building) {
        Button(
          onClick = { viewModel.navigateTo(AppScreen.HOME) },
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
          Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Back to Dashboard", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun BuildingProgressHeader(progress: Float, statusText: String) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = RoundedCornerShape(18.dp)
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
          progress = { progress },
          modifier = Modifier.size(72.dp),
          strokeWidth = 6.dp,
          color = Color(0xFF6366F1),
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
          text = "${(progress * 100).toInt()}%",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = statusText,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      Spacer(modifier = Modifier.height(12.dp))

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        color = Color(0xFF6366F1),
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )
    }
  }
}

@Composable
private fun BuildSuccessHeader(
  result: BuildResult,
  onInstall: () -> Unit,
  onSave: () -> Unit,
  onShare: () -> Unit,
  onPreview: () -> Unit
) {
  AnimatedVisibility(
    visible = true,
    enter = fadeIn() + slideInVertically()
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("build_success_card"),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      shape = RoundedCornerShape(18.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Color(0xFF10B981),
              modifier = Modifier.size(28.dp)
            )
          }

          Column {
            Text(
              text = "APK Package Ready & Signed!",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF10B981)
            )
            Text(
              text = "${result.apkFile?.name ?: "app.apk"} • ${result.formattedSize}",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Checksum & Details
        Text(
          text = "SHA-256: ${result.sha256.take(12)}...${result.sha256.takeLast(12)}",
          style = MaterialTheme.typography.labelSmall,
          fontFamily = FontFamily.Monospace,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Duration: ${result.durationMs}ms • Package: ${result.config.packageName}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Actions Grid
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onInstall,
            modifier = Modifier
              .weight(1.2f)
              .testTag("install_generated_apk_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
          ) {
            Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Install APK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }

          Button(
            onClick = onSave,
            modifier = Modifier
              .weight(1f)
              .testTag("save_downloads_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Download", fontSize = 13.sp)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onShare,
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share APK", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = onPreview,
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Live Preview", fontSize = 12.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun BuildErrorHeader(
  error: String,
  onRetry: () -> Unit,
  onEdit: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    shape = RoundedCornerShape(18.dp)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Error,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(32.dp)
        )
        Column {
          Text(
            text = "Build Failed",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
          )
          Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = onRetry,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Retry Build")
        }

        OutlinedButton(
          onClick = onEdit,
          modifier = Modifier.weight(1f)
        ) {
          Text("Edit Settings")
        }
      }
    }
  }
}

@Composable
private fun LogEntryRow(entry: BuildLogEntry) {
  val (color, tag) = when (entry.level) {
    LogLevel.STAGE -> Color(0xFF38BDF8) to "[STAGE]"
    LogLevel.SUCCESS -> Color(0xFF4ADE80) to "[OK]"
    LogLevel.WARNING -> Color(0xFFFBBF24) to "[WARN]"
    LogLevel.ERROR -> Color(0xFFF87171) to "[ERR]"
    LogLevel.INFO -> Color(0xFF94A3B8) to "[INFO]"
  }

  Row(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = tag,
      color = color,
      fontFamily = FontFamily.Monospace,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.width(54.dp)
    )
    Text(
      text = entry.message,
      color = if (entry.level == LogLevel.SUCCESS) Color(0xFFE2E8F0) else Color(0xFFCBD5E1),
      fontFamily = FontFamily.Monospace,
      fontSize = 11.sp,
      lineHeight = 15.sp,
      modifier = Modifier.weight(1f)
    )
  }
}
