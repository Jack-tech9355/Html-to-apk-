package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildResult
import com.example.viewmodel.AppScreen
import com.example.viewmodel.HTMLToAPKViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
  viewModel: HTMLToAPKViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val recentBuilds by viewModel.recentBuilds.collectAsState()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Hero Header
    item {
      HeroHeaderCard(
        onCreateClick = {
          viewModel.setBuilderStep(1)
          viewModel.navigateTo(AppScreen.BUILDER)
        },
        onPreviewClick = {
          viewModel.navigateTo(AppScreen.LIVE_PREVIEW)
        }
      )
    }

    // Quick Starter Presets
    item {
      Text(
        text = "Quick Starter Presets",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        PresetChip(
          title = "Counter & Bridge",
          icon = Icons.Default.Code,
          color = Color(0xFF6366F1),
          modifier = Modifier.weight(1f),
          onClick = {
            viewModel.loadPreset("DEFAULT")
            viewModel.setBuilderStep(1)
            viewModel.navigateTo(AppScreen.BUILDER)
          }
        )
        PresetChip(
          title = "Retro Tap Game",
          icon = Icons.Default.Gamepad,
          color = Color(0xFF10B981),
          modifier = Modifier.weight(1f),
          onClick = {
            viewModel.loadPreset("GAME")
            viewModel.setBuilderStep(1)
            viewModel.navigateTo(AppScreen.BUILDER)
          }
        )
        PresetChip(
          title = "Cyber Telemetry",
          icon = Icons.Default.Speed,
          color = Color(0xFF0EA5E9),
          modifier = Modifier.weight(1f),
          onClick = {
            viewModel.loadPreset("CYBER")
            viewModel.setBuilderStep(1)
            viewModel.navigateTo(AppScreen.BUILDER)
          }
        )
      }
    }

    // Engine Capabilities & Specs Card
    item {
      EngineSpecsCard()
    }

    // Recent Builds Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Generated APK History",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        if (recentBuilds.isNotEmpty()) {
          Text(
            text = "${recentBuilds.size} APK${if (recentBuilds.size > 1) "s" else ""}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    if (recentBuilds.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Android,
              contentDescription = null,
              modifier = Modifier.size(48.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No APKs generated yet",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Tap 'New Project' above to package your first HTML app into an APK.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }
    } else {
      items(recentBuilds, key = { it.id }) { buildResult ->
        RecentBuildCard(
          build = buildResult,
          onInstall = { viewModel.installApk(context, buildResult) },
          onShare = { viewModel.shareApk(context, buildResult) },
          onDownload = { viewModel.saveApkToDownloads(context, buildResult) },
          onPreview = {
            viewModel.updateTitle(buildResult.config.appTitle)
            viewModel.updateRawHtml(buildResult.config.rawHtmlContent)
            viewModel.navigateTo(AppScreen.LIVE_PREVIEW)
          }
        )
      }
    }
  }
}

@Composable
private fun HeroHeaderCard(
  onCreateClick: () -> Unit,
  onPreviewClick: () -> Unit
) {
  val gradient = Brush.linearGradient(
    colors = listOf(
      Color(0xFF312E81),
      Color(0xFF1E1B4B),
      Color(0xFF0F172A)
    )
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("hero_header_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
  ) {
    Box(
      modifier = Modifier
        .background(gradient)
        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        .padding(20.dp)
    ) {
      Column {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            color = Color(0xFF6366F1).copy(alpha = 0.25f),
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f))
          ) {
            Text(
              text = "⚡ PRE-COMPILED TEMPLATE ENGINE",
              color = Color(0xFFA5B4FC),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              letterSpacing = 0.8.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "HTML to Android APK",
          fontSize = 24.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "Package raw HTML, CSS, JavaScript, or ZIP bundles into standalone, signed Android APKs completely offline.",
          fontSize = 13.sp,
          color = Color(0xFFCBD5E1),
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onCreateClick,
            modifier = Modifier
              .weight(1f)
              .testTag("new_project_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("New Project", fontWeight = FontWeight.Bold)
          }

          FilledTonalButton(
            onClick = onPreviewClick,
            modifier = Modifier
              .weight(1f)
              .testTag("live_preview_button"),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = Color(0xFF1E293B),
              contentColor = Color(0xFFE2E8F0)
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Live Preview")
          }
        }
      }
    }
  }
}

@Composable
private fun PresetChip(
  title: String,
  icon: ImageVector,
  color: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .background(color.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun EngineSpecsCard() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = "Compilation Pipeline Specs",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        SpecItem(label = "Signature", value = "v1 JAR (SHA256)")
        SpecItem(label = "Target SDK", value = "Android 14 (API 34)")
        SpecItem(label = "Runtime", value = "Hardware WebKit")
      }
    }
  }
}

@Composable
private fun SpecItem(label: String, value: String) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
private fun RecentBuildCard(
  build: BuildResult,
  onInstall: () -> Unit,
  onShare: () -> Unit,
  onDownload: () -> Unit,
  onPreview: () -> Unit
) {
  val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(build.timestamp))

  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("recent_build_${build.id}"),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Android,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
          }
          Column {
            Text(
              text = build.config.appTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${build.config.packageName} • v${build.config.versionName}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = build.formattedSize,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Built on $dateStr • ${build.durationMs}ms compile time",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onInstall,
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text("Install", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = onDownload,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
        }

        OutlinedButton(
          onClick = onShare,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
        }

        OutlinedButton(
          onClick = onPreview,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = "Preview", modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}
