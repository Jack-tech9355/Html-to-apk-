package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppBuildConfig
import com.example.model.SourceType
import com.example.viewmodel.AppScreen
import com.example.viewmodel.HTMLToAPKViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectBuilderScreen(
  viewModel: HTMLToAPKViewModel,
  modifier: Modifier = Modifier
) {
  val currentStep by viewModel.builderStep.collectAsState()
  val config by viewModel.config.collectAsState()

  BackHandler {
    viewModel.prevBuilderStep()
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
            text = "Project Builder",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Step $currentStep of 5: ${getStepTitle(currentStep)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
        }
      },
      navigationIcon = {
        IconButton(onClick = { viewModel.prevBuilderStep() }) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
      },
      actions = {
        IconButton(
          onClick = { viewModel.navigateTo(AppScreen.LIVE_PREVIEW) },
          modifier = Modifier.testTag("preview_app_icon_button")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = "Live Preview", tint = MaterialTheme.colorScheme.primary)
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
      )
    )

    // Stepper Tabs Row
    ScrollableTabRow(
      selectedTabIndex = currentStep - 1,
      edgePadding = 16.dp,
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      contentColor = MaterialTheme.colorScheme.primary
    ) {
      val stepNames = listOf("Source", "Branding", "UI Toggles", "Permissions", "Build")
      stepNames.forEachIndexed { index, name ->
        val stepNum = index + 1
        Tab(
          selected = currentStep == stepNum,
          onClick = { viewModel.setBuilderStep(stepNum) },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(20.dp)
                  .background(
                    if (currentStep == stepNum) MaterialTheme.colorScheme.primary
                    else if (currentStep > stepNum) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                  ),
                contentAlignment = Alignment.Center
              ) {
                if (currentStep > stepNum) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                } else {
                  Text(
                    text = "$stepNum",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentStep == stepNum) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(name, fontWeight = if (currentStep == stepNum) FontWeight.Bold else FontWeight.Normal)
            }
          }
        )
      }
    }

    // Main Step Content
    Box(modifier = Modifier.weight(1f)) {
      when (currentStep) {
        1 -> Step1SourceCode(viewModel, config)
        2 -> Step2Branding(viewModel, config)
        3 -> Step3Toggles(viewModel, config)
        4 -> Step4Permissions(viewModel, config)
        5 -> Step5ReviewAndBuild(viewModel, config)
      }
    }

    // Bottom Navigation Action Bar
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainer,
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (currentStep > 1) {
          OutlinedButton(
            onClick = { viewModel.prevBuilderStep() },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Back")
          }
        } else {
          OutlinedButton(
            onClick = { viewModel.navigateTo(AppScreen.HOME) },
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel")
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (currentStep < 5) {
          Button(
            onClick = { viewModel.nextBuilderStep() },
            modifier = Modifier
              .weight(1f)
              .testTag("step_next_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Text("Next")
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
          }
        } else {
          Button(
            onClick = { viewModel.startBuild() },
            modifier = Modifier
              .weight(1.3f)
              .testTag("compile_apk_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
          ) {
            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Compile APK", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

private fun getStepTitle(step: Int): String = when (step) {
  1 -> "Source Code Input"
  2 -> "App Branding & Identity"
  3 -> "UI & Behavior Toggles"
  4 -> "Permissions Manager"
  5 -> "Review & Build APK"
  else -> ""
}

// -------------------------------------------------------------
// STEP 1: SOURCE CODE
// -------------------------------------------------------------
@Composable
private fun Step1SourceCode(viewModel: HTMLToAPKViewModel, config: AppBuildConfig) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let {
      try {
        val stream = context.contentResolver.openInputStream(it)
        val text = stream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
        viewModel.updateRawHtml(text)
        viewModel.updateSourceType(SourceType.FILE_HTML, it.toString())
      } catch (_: Exception) {}
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = "Source Code Input",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Provide HTML, CSS, JavaScript directly or choose a starter template.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // Source Type selector chips
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = config.sourceType == SourceType.RAW_HTML,
          onClick = { viewModel.updateSourceType(SourceType.RAW_HTML) },
          label = { Text("Code Editor") },
          leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
        )
        FilterChip(
          selected = config.sourceType == SourceType.WEB_URL,
          onClick = { viewModel.updateSourceType(SourceType.WEB_URL) },
          label = { Text("Website URL") },
          leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp)) }
        )
        FilterChip(
          selected = config.sourceType == SourceType.FILE_HTML,
          onClick = { filePickerLauncher.launch(arrayOf("text/html", "application/zip", "*/*")) },
          label = { Text("Import File") },
          leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp)) }
        )
      }
    }

    if (config.sourceType == SourceType.WEB_URL) {
      // Website URL Input Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Website to APK Converter",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = "Convert any web URL or Progressive Web App (PWA) into an installable Android APK with native pull-to-refresh and offline support.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
              value = config.webUrl,
              onValueChange = { viewModel.updateWebUrl(it) },
              label = { Text("Website / Web App URL") },
              placeholder = { Text("https://myportfolio.netlify.app") },
              modifier = Modifier.fillMaxWidth().testTag("web_url_input"),
              singleLine = true,
              trailingIcon = {
                IconButton(onClick = {
                  val clip = clipboardManager.getText()?.text
                  if (!clip.isNullOrBlank()) viewModel.updateWebUrl(clip)
                }) {
                  Icon(Icons.Default.ContentCopy, contentDescription = "Paste URL", modifier = Modifier.size(18.dp))
                }
              }
            )

            // Quick preset URL suggestions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilledTonalButton(
                onClick = { viewModel.updateWebUrl("https://example.com") },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Text("example.com", fontSize = 11.sp)
              }
              FilledTonalButton(
                onClick = { viewModel.updateWebUrl("https://google.com") },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Text("google.com", fontSize = 11.sp)
              }
            }
          }
        }
      }
    } else {
      // Preset quick starter selector
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Load Template:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
          )
          FilledTonalButton(
            onClick = { viewModel.loadPreset("DEFAULT") },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text("Basic App", fontSize = 11.sp)
          }
          FilledTonalButton(
            onClick = { viewModel.loadPreset("GAME") },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text("Retro Game", fontSize = 11.sp)
          }
          FilledTonalButton(
            onClick = { viewModel.loadPreset("CYBER") },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text("Dashboard", fontSize = 11.sp)
          }
        }
      }

      // Code Editor Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "index.html (${config.rawHtmlContent.length} chars)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Row {
                IconButton(onClick = {
                  val clipText = clipboardManager.getText()?.text
                  if (!clipText.isNullOrEmpty()) {
                    viewModel.updateRawHtml(clipText)
                  }
                }) {
                  Icon(Icons.Default.ContentCopy, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.updateRawHtml("") }) {
                  Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                }
              }
            }

            OutlinedTextField(
              value = config.rawHtmlContent,
              onValueChange = { viewModel.updateRawHtml(it) },
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp, max = 380.dp)
                .testTag("html_code_editor"),
              textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp
              ),
              placeholder = { Text("<!DOCTYPE html>\n<html>\n  <body>\n    <h1>Hello Android!</h1>\n  </body>\n</html>") }
            )
          }
        }
      }
    }

    // Optional CSS / JS expandable sections
    item {
      var showAdvanced by remember { mutableStateOf(false) }
      OutlinedButton(
        onClick = { showAdvanced = !showAdvanced },
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (showAdvanced) "Hide Injected CSS & JS" else "Add Custom Injected CSS & JS")
      }

      AnimatedVisibility(visible = showAdvanced) {
        Column(
          modifier = Modifier.padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedTextField(
            value = config.customCss,
            onValueChange = { viewModel.updateCustomCss(it) },
            label = { Text("Injected Custom CSS") },
            placeholder = { Text("body { background: #000; }") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
          )

          OutlinedTextField(
            value = config.customJs,
            onValueChange = { viewModel.updateCustomJs(it) },
            label = { Text("Injected Custom JavaScript") },
            placeholder = { Text("console.log('App started');") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// STEP 2: BRANDING & IDENTITY
// -------------------------------------------------------------
@Composable
private fun Step2Branding(viewModel: HTMLToAPKViewModel, config: AppBuildConfig) {
  val iconPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    viewModel.updateIconUri(uri?.toString())
  }

  val splashPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    viewModel.updateSplashUri(uri?.toString())
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = "App Branding & Identity",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Customize how your application appears in the launcher and system.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // App Title
    item {
      OutlinedTextField(
        value = config.appTitle,
        onValueChange = { viewModel.updateTitle(it) },
        label = { Text("App Name / Title") },
        placeholder = { Text("e.g. My Awesome App") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("app_title_input"),
        singleLine = true
      )
    }

    // Package Name
    item {
      OutlinedTextField(
        value = config.packageName,
        onValueChange = { viewModel.updatePackageName(it) },
        label = { Text("Android Package Name (Application ID)") },
        placeholder = { Text("com.user.htmlapp") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("package_name_input"),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        supportingText = {
          Text("Format: com.company.appname (lowercase letters, numbers, dots)")
        }
      )
    }

    // Version Name & Code
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = config.versionName,
          onValueChange = { viewModel.updateVersion(it, config.versionCode) },
          label = { Text("Version Name") },
          modifier = Modifier.weight(1f),
          singleLine = true
        )
        OutlinedTextField(
          value = config.versionCode.toString(),
          onValueChange = {
            val code = it.toIntOrNull() ?: 1
            viewModel.updateVersion(config.versionName, code)
          },
          label = { Text("Version Code") },
          modifier = Modifier.weight(1f),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
      }
    }

    // App Launcher Icon Picker Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              if (!config.iconUri.isNullOrBlank()) {
                AsyncImage(
                  model = config.iconUri,
                  contentDescription = "Launcher Icon",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              } else {
                Icon(
                  imageVector = Icons.Default.Android,
                  contentDescription = "Default Icon",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(32.dp)
                )
              }
            }

            Column {
              Text(
                text = "Launcher Icon",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (config.iconUri != null) "Custom icon selected" else "Using default adaptive icon",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          FilledTonalButton(
            onClick = { iconPickerLauncher.launch("image/*") },
            modifier = Modifier.testTag("choose_icon_button")
          ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Choose")
          }
        }
      }
    }

    // Splash Screen Image Picker Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
              contentAlignment = Alignment.Center
            ) {
              if (!config.splashUri.isNullOrBlank()) {
                AsyncImage(
                  model = config.splashUri,
                  contentDescription = "Splash Screen",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              } else {
                Icon(
                  imageVector = Icons.Default.Image,
                  contentDescription = "Default Splash",
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(32.dp)
                )
              }
            }

            Column {
              Text(
                text = "Splash Screen Image",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (config.splashUri != null) "Custom splash image loaded" else "Default theme splash screen",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          FilledTonalButton(
            onClick = { splashPickerLauncher.launch("image/*") }
          ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Choose")
          }
        }
      }
    }

    // Screen Orientation Selector
    item {
      Text(
        text = "Screen Orientation",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("PORTRAIT" to "Portrait", "LANDSCAPE" to "Landscape", "SENSOR" to "Auto-Rotate").forEach { (key, label) ->
          FilterChip(
            selected = config.orientation == key,
            onClick = { viewModel.updateOrientation(key) },
            label = { Text(label) }
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// STEP 3: UI & BEHAVIOR TOGGLES
// -------------------------------------------------------------
@Composable
private fun Step3Toggles(viewModel: HTMLToAPKViewModel, config: AppBuildConfig) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(
        text = "UI & Behavior Toggles",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Configure native runtime shell behaviors for the embedded WebView.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    item {
      ToggleItem(
        title = "Titlebar Visible",
        subtitle = "Display native Android Top App Bar showing the app title and status.",
        checked = config.enableTitlebar,
        onCheckedChange = { viewModel.updateToggleTitlebar(it) },
        tag = "toggle_titlebar"
      )
    }

    item {
      ToggleItem(
        title = "Navigation Toolbar",
        subtitle = "Show bottom navigation bar with Back, Forward, Reload, Home, and Share buttons.",
        checked = config.enableToolbar,
        onCheckedChange = { viewModel.updateToggleToolbar(it) },
        tag = "toggle_toolbar"
      )
    }

    item {
      ToggleItem(
        title = "Swipe-Down to Refresh",
        subtitle = "Allow pulling down from the top edge to reload the current web page.",
        checked = config.enableSwipeRefresh,
        onCheckedChange = { viewModel.updateToggleSwipeRefresh(it) },
        tag = "toggle_swipe_refresh"
      )
    }

    item {
      ToggleItem(
        title = "Allow Text Selection & Copy",
        subtitle = "When disabled, blocks context menus, long-click highlights, and injects user-select: none.",
        checked = config.allowLongPressCopy,
        onCheckedChange = { viewModel.updateToggleLongPressCopy(it) },
        tag = "toggle_long_press_copy"
      )
    }

    item {
      ToggleItem(
        title = "Allow Pinch-to-Zoom",
        subtitle = "Enables multi-touch pinch zooming controls inside the web view.",
        checked = config.allowZoom,
        onCheckedChange = { viewModel.updateToggleZoom(it) },
        tag = "toggle_zoom"
      )
    }
  }
}

@Composable
private fun ToggleItem(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  tag: String
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = RoundedCornerShape(16.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 16.sp
        )
      }

      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag(tag)
      )
    }
  }
}

// -------------------------------------------------------------
// STEP 4: PERMISSIONS MANAGER
// -------------------------------------------------------------
@Composable
private fun Step4Permissions(viewModel: HTMLToAPKViewModel, config: AppBuildConfig) {
  val perms = config.permissions

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(
        text = "Android Permissions Manager",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Select which hardware resources and system capabilities to declare in AndroidManifest.xml.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    item {
      PermissionCard(
        title = "Internet Access",
        manifestTag = "android.permission.INTERNET",
        description = "Required to fetch external APIs, fonts, CDNs, or load remote web content.",
        icon = Icons.Default.Language,
        checked = perms.internet,
        onCheckedChange = { viewModel.updatePermission(internet = it) },
        testTag = "perm_internet"
      )
    }

    item {
      PermissionCard(
        title = "Storage Read / Write",
        manifestTag = "READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE",
        description = "Needed if HTML apps download assets, save offline database files, or access local files.",
        icon = Icons.Default.SdCard,
        checked = perms.storage,
        onCheckedChange = { viewModel.updatePermission(storage = it) },
        testTag = "perm_storage"
      )
    }

    item {
      PermissionCard(
        title = "Camera Access",
        manifestTag = "android.permission.CAMERA",
        description = "Required for WebRTC video, QR code scanners, HTML5 video inputs, and photo capture.",
        icon = Icons.Default.CameraAlt,
        checked = perms.camera,
        onCheckedChange = { viewModel.updatePermission(camera = it) },
        testTag = "perm_camera"
      )
    }

    item {
      PermissionCard(
        title = "Location Access (GPS)",
        manifestTag = "android.permission.ACCESS_FINE_LOCATION",
        description = "Grants HTML5 Geolocation API (navigator.geolocation) for maps and positioning.",
        icon = Icons.Default.LocationOn,
        checked = perms.location,
        onCheckedChange = { viewModel.updatePermission(location = it) },
        testTag = "perm_location"
      )
    }

    item {
      PermissionCard(
        title = "Microphone Access",
        manifestTag = "android.permission.RECORD_AUDIO",
        description = "Allows WebRTC voice chat, speech-to-text, and audio recording in the HTML app.",
        icon = Icons.Default.Mic,
        checked = perms.microphone,
        onCheckedChange = { viewModel.updatePermission(microphone = it) },
        testTag = "perm_microphone"
      )
    }
  }
}

@Composable
private fun PermissionCard(
  title: String,
  manifestTag: String,
  description: String,
  icon: ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCheckedChange(!checked) },
    colors = CardDefaults.cardColors(
      containerColor = if (checked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
    ),
    shape = RoundedCornerShape(16.dp),
    border = if (checked) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .background(
            if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = manifestTag,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 15.sp
        )
      }

      Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag(testTag),
        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
      )
    }
  }
}

// -------------------------------------------------------------
// STEP 5: REVIEW & BUILD
// -------------------------------------------------------------
@Composable
private fun Step5ReviewAndBuild(viewModel: HTMLToAPKViewModel, config: AppBuildConfig) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = "Review & Build Configuration",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Verify your project configuration before the pre-compiled template engine starts assembling.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // App Overview Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              if (!config.iconUri.isNullOrBlank()) {
                AsyncImage(
                  model = config.iconUri,
                  contentDescription = "Icon",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              } else {
                Icon(
                  Icons.Default.Android,
                  contentDescription = null,
                  modifier = Modifier.size(36.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }

            Column {
              Text(
                text = config.appTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = config.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = "Version: ${config.versionName} (${config.versionCode})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

          // Key-Value review rows
          ReviewRow(label = "Source Mode", value = config.sourceType.name)
          ReviewRow(label = "HTML Size", value = "${config.rawHtmlContent.length} characters")
          ReviewRow(label = "Titlebar", value = if (config.enableTitlebar) "Enabled" else "Disabled")
          ReviewRow(label = "Toolbar", value = if (config.enableToolbar) "Enabled" else "Disabled")
          ReviewRow(label = "Swipe Refresh", value = if (config.enableSwipeRefresh) "Enabled" else "Disabled")
          ReviewRow(label = "Text Selection", value = if (config.allowLongPressCopy) "Allowed" else "Blocked (Protected)")
          ReviewRow(label = "Orientation", value = config.orientation)

          val permList = config.permissions.toPermissionList()
          ReviewRow(
            label = "Active Permissions",
            value = if (permList.isEmpty()) "None" else "${permList.size} permissions"
          )
        }
      }
    }

    // Live Preview Button
    item {
      OutlinedButton(
        onClick = { viewModel.navigateTo(AppScreen.LIVE_PREVIEW) },
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Interactive Live Preview Before Build")
      }
    }

    // Big Build Button
    item {
      Button(
        onClick = { viewModel.startBuild() },
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("big_compile_apk_button"),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
        shape = RoundedCornerShape(16.dp)
      ) {
        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text("Start Compilation & Generate APK", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun ReviewRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold
    )
  }
}
