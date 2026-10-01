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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildResult
import com.example.model.SourceType
import com.example.repository.SavedProject
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
  val savedProjects by viewModel.savedProjects.collectAsState()
  var showNewProjectDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showNewProjectDialog = true },
        containerColor = Color(0xFF6366F1),
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_new_project")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Add, contentDescription = "New Project")
          Spacer(modifier = Modifier.width(6.dp))
          Text("New Project", fontWeight = FontWeight.Bold)
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Hero Header Card
      item {
        HeroHeaderCard(
          onCreateClick = { showNewProjectDialog = true },
          onPreviewClick = { viewModel.navigateTo(AppScreen.LIVE_PREVIEW) }
        )
      }

      // Quick Create Bar
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              viewModel.createProject("My Website App", SourceType.WEB_URL, "https://example.com")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Website to APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = {
              viewModel.createProject("My HTML5 App", SourceType.RAW_HTML)
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("HTML5 to APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // My Saved Projects Section
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "My Projects (${savedProjects.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          TextButton(onClick = { showNewProjectDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Create")
          }
        }
      }

      if (savedProjects.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(14.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "No saved projects yet",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Create a project with HTML code or a Website URL to get started.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(12.dp))
              FilledTonalButton(onClick = { showNewProjectDialog = true }) {
                Text("+ Create First Project")
              }
            }
          }
        }
      } else {
        items(savedProjects, key = { it.id }) { project ->
          SavedProjectCard(
            project = project,
            onOpen = { viewModel.openProject(project) },
            onQuickBuild = {
              viewModel.openProject(project)
              viewModel.startBuild()
            },
            onPreview = {
              viewModel.openProject(project)
              viewModel.navigateTo(AppScreen.LIVE_PREVIEW)
            },
            onDelete = { viewModel.deleteProject(project.id) }
          )
        }
      }

      // Quick Starter Presets Section
      item {
        Text(
          text = "Quick Starter Presets",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
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
            title = "Web Portal",
            icon = Icons.Default.Language,
            color = Color(0xFF0EA5E9),
            modifier = Modifier.weight(1f),
            onClick = {
              viewModel.loadPreset("WEB_URL")
              viewModel.setBuilderStep(1)
              viewModel.navigateTo(AppScreen.BUILDER)
            }
          )
        }
      }

      // Features Overview
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
              text = "Independent APK Architecture",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            FeaturePoint(icon = Icons.Default.Security, text = "Unique Application ID per project (never overwrites or updates builder app)")
            FeaturePoint(icon = Icons.Default.Language, text = "Website URL wrapper with offline fallback and pull-to-refresh")
            FeaturePoint(icon = Icons.Default.Speed, text = "Signed with Google ApkSigner (v1 + v2 + v3 schemes)")
          }
        }
      }

      // Recent Builds History
      if (recentBuilds.isNotEmpty()) {
        item {
          Text(
            text = "Recently Compiled APKs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp)
          )
        }
        items(recentBuilds) { build ->
          RecentBuildCard(
            build = build,
            onInstall = { viewModel.installApk(context, build) },
            onSave = { viewModel.saveApkToDownloads(context, build) },
            onShare = { viewModel.shareApk(context, build) }
          )
        }
      }
    }
  }

  // Interactive Create Project Dialog
  if (showNewProjectDialog) {
    CreateProjectDialog(
      onDismiss = { showNewProjectDialog = false },
      onCreate = { name, type, webUrl, customPackage ->
        showNewProjectDialog = false
        viewModel.createProject(name, type, webUrl, customPackage)
      }
    )
  }
}

@Composable
private fun SavedProjectCard(
  project: SavedProject,
  onOpen: () -> Unit,
  onQuickBuild: () -> Unit,
  onPreview: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onOpen),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(
                if (project.config.sourceType == SourceType.WEB_URL) Color(0xFF0EA5E9).copy(alpha = 0.2f)
                else Color(0xFF6366F1).copy(alpha = 0.2f)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (project.config.sourceType == SourceType.WEB_URL) Icons.Default.Language else Icons.Default.Code,
              contentDescription = null,
              tint = if (project.config.sourceType == SourceType.WEB_URL) Color(0xFF38BDF8) else Color(0xFF818CF8),
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = project.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = project.config.packageName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = if (project.config.sourceType == SourceType.WEB_URL) "Target: ${project.config.webUrl}" else project.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilledTonalButton(
          onClick = onOpen,
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Edit", fontSize = 12.sp)
        }

        FilledTonalButton(
          onClick = onPreview,
          modifier = Modifier.weight(1.1f),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Preview", fontSize = 12.sp)
        }

        Button(
          onClick = onQuickBuild,
          modifier = Modifier.weight(1.2f),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Build APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun CreateProjectDialog(
  onDismiss: () -> Unit,
  onCreate: (name: String, type: SourceType, webUrl: String, customPackage: String) -> Unit
) {
  var projectName by remember { mutableStateOf("My App") }
  var selectedType by remember { mutableStateOf(SourceType.RAW_HTML) }
  var webUrl by remember { mutableStateOf("https://") }

  val slug = projectName.lowercase().replace(Regex("[^a-z0-9]"), "").ifEmpty { "app" }
  val autoPackage = "com.htmltoapk.$slug"

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = "Create New Project", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Create an independent Android application that installs side-by-side on device.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = projectName,
          onValueChange = { projectName = it },
          label = { Text("Project Name") },
          placeholder = { Text("e.g. My Portfolio") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text(
          text = "Select Project Mode:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedType == SourceType.RAW_HTML,
            onClick = { selectedType = SourceType.RAW_HTML },
            label = { Text("HTML5 Code") },
            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
          )
          FilterChip(
            selected = selectedType == SourceType.WEB_URL,
            onClick = { selectedType = SourceType.WEB_URL },
            label = { Text("Website URL") },
            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp)) }
          )
        }

        if (selectedType == SourceType.WEB_URL) {
          OutlinedTextField(
            value = webUrl,
            onValueChange = { webUrl = it },
            label = { Text("Website URL") },
            placeholder = { Text("https://myportfolio.netlify.app") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        // Package Name Display
        Column {
          Text(
            text = "Unique Package ID (Application ID):",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = autoPackage,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "✓ Distinct ID guarantees Android installs as a new app without asking to update.",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF10B981),
            fontSize = 11.sp
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (projectName.isNotBlank()) {
            onCreate(projectName.trim(), selectedType, webUrl.trim(), autoPackage)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
      ) {
        Text("Create & Open", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun HeroHeaderCard(
  onCreateClick: () -> Unit,
  onPreviewClick: () -> Unit
) {
  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("hero_header_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF131C31))
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF312E81).copy(alpha = 0.5f),
              Color(0xFF0F172A)
            )
          )
        )
        .padding(20.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF6366F1)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "HTML to APK",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Native Android App Generator",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF38BDF8)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Build production-ready, installable Android APKs directly from HTML5/CSS/JS or any live website URL with zero coding required.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFCBD5E1),
          lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onCreateClick,
            modifier = Modifier
              .weight(1.3f)
              .testTag("hero_create_project_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create Project", fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onPreviewClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sandbox")
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
    color = MaterialTheme.colorScheme.surfaceContainer,
    tonalElevation = 2.dp
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun FeaturePoint(icon: ImageVector, text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun RecentBuildCard(
  build: BuildResult,
  onInstall: () -> Unit,
  onSave: () -> Unit,
  onShare: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = RoundedCornerShape(14.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = build.config.appTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${build.config.packageName} • ${build.formattedSize}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onInstall,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Install", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        FilledTonalButton(
          onClick = onSave,
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Download", fontSize = 11.sp)
        }

        OutlinedButton(
          onClick = onShare,
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Share", fontSize = 11.sp)
        }
      }
    }
  }
}
