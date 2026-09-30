package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.compiler.ApkCompilerEngine
import com.example.model.AppBuildConfig
import com.example.model.AppPermissions
import com.example.model.BuildLogEntry
import com.example.model.BuildResult
import com.example.model.LogLevel
import com.example.model.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class AppScreen {
  HOME,
  BUILDER,
  PROGRESS,
  LIVE_PREVIEW
}

sealed interface BuildUiState {
  data object Idle : BuildUiState
  data class Building(
    val progress: Float,
    val statusText: String,
    val logs: List<BuildLogEntry>
  ) : BuildUiState
  data class Success(val result: BuildResult) : BuildUiState
  data class Error(val error: String, val logs: List<BuildLogEntry>) : BuildUiState
}

class HTMLToAPKViewModel(application: Application) : AndroidViewModel(application) {

  private val compilerEngine = ApkCompilerEngine(application)

  private val _currentScreen = MutableStateFlow(AppScreen.HOME)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  private val _builderStep = MutableStateFlow(1)
  val builderStep: StateFlow<Int> = _builderStep.asStateFlow()

  private val _config = MutableStateFlow(AppBuildConfig())
  val config: StateFlow<AppBuildConfig> = _config.asStateFlow()

  private val _buildState = MutableStateFlow<BuildUiState>(BuildUiState.Idle)
  val buildState: StateFlow<BuildUiState> = _buildState.asStateFlow()

  private val _recentBuilds = MutableStateFlow<List<BuildResult>>(emptyList())
  val recentBuilds: StateFlow<List<BuildResult>> = _recentBuilds.asStateFlow()

  private val _previewLogs = MutableStateFlow<List<String>>(emptyList())
  val previewLogs: StateFlow<List<String>> = _previewLogs.asStateFlow()

  // Navigation
  fun navigateTo(screen: AppScreen) {
    _currentScreen.value = screen
  }

  fun setBuilderStep(step: Int) {
    _builderStep.value = step.coerceIn(1, 5)
  }

  fun nextBuilderStep() {
    if (_builderStep.value < 5) {
      _builderStep.value += 1
    }
  }

  fun prevBuilderStep() {
    if (_builderStep.value > 1) {
      _builderStep.value -= 1
    } else {
      _currentScreen.value = AppScreen.HOME
    }
  }

  // Configuration Updates
  fun updateTitle(title: String) {
    _config.update {
      val sanitized = title.trim().ifEmpty { "My Web App" }
      val autoPkg = "com.user." + sanitized.lowercase().replace(Regex("[^a-z0-9]"), "")
      it.copy(
        appTitle = title,
        packageName = if (it.packageName == "com.user.htmlapp") autoPkg else it.packageName
      )
    }
  }

  fun updatePackageName(pkg: String) {
    _config.update { it.copy(packageName = pkg.trim()) }
  }

  fun updateVersion(versionName: String, versionCode: Int) {
    _config.update { it.copy(versionName = versionName, versionCode = versionCode) }
  }

  fun updateRawHtml(html: String) {
    _config.update { it.copy(rawHtmlContent = html, sourceType = SourceType.RAW_HTML) }
  }

  fun updateCustomCss(css: String) {
    _config.update { it.copy(customCss = css) }
  }

  fun updateCustomJs(js: String) {
    _config.update { it.copy(customJs = js) }
  }

  fun updateSourceType(type: SourceType, uri: String? = null) {
    _config.update {
      it.copy(
        sourceType = type,
        iconUri = uri ?: it.iconUri
      )
    }
  }

  fun updateIconUri(uri: String?) {
    _config.update { it.copy(iconUri = uri) }
  }

  fun updateSplashUri(uri: String?) {
    _config.update { it.copy(splashUri = uri) }
  }

  fun updateToggleTitlebar(enabled: Boolean) {
    _config.update { it.copy(enableTitlebar = enabled) }
  }

  fun updateToggleToolbar(enabled: Boolean) {
    _config.update { it.copy(enableToolbar = enabled) }
  }

  fun updateToggleSwipeRefresh(enabled: Boolean) {
    _config.update { it.copy(enableSwipeRefresh = enabled) }
  }

  fun updateToggleLongPressCopy(enabled: Boolean) {
    _config.update { it.copy(allowLongPressCopy = enabled) }
  }

  fun updateToggleZoom(enabled: Boolean) {
    _config.update { it.copy(allowZoom = enabled) }
  }

  fun updateOrientation(orientation: String) {
    _config.update { it.copy(orientation = orientation) }
  }

  fun updatePermission(
    internet: Boolean? = null,
    camera: Boolean? = null,
    storage: Boolean? = null,
    location: Boolean? = null,
    microphone: Boolean? = null
  ) {
    _config.update { current ->
      val p = current.permissions
      current.copy(
        permissions = AppPermissions(
          internet = internet ?: p.internet,
          camera = camera ?: p.camera,
          storage = storage ?: p.storage,
          location = location ?: p.location,
          microphone = microphone ?: p.microphone
        )
      )
    }
  }

  fun loadPreset(presetType: String) {
    when (presetType) {
      "DEFAULT" -> {
        _config.update {
          it.copy(
            appTitle = "Counter & Bridge",
            rawHtmlContent = AppBuildConfig.DEFAULT_HTML_PRESET,
            enableSwipeRefresh = true,
            allowLongPressCopy = true,
            enableToolbar = true
          )
        }
      }
      "GAME" -> {
        _config.update {
          it.copy(
            appTitle = "Retro Tap Game",
            rawHtmlContent = AppBuildConfig.PRESET_RETRO_GAME,
            enableSwipeRefresh = false,
            allowLongPressCopy = false,
            enableTitlebar = false,
            enableToolbar = false,
            orientation = "PORTRAIT"
          )
        }
      }
      "CYBER" -> {
        _config.update {
          it.copy(
            appTitle = "Cyber Telemetry",
            rawHtmlContent = AppBuildConfig.PRESET_CYBER_DASHBOARD,
            enableSwipeRefresh = true,
            allowLongPressCopy = true,
            enableToolbar = true,
            orientation = "PORTRAIT"
          )
        }
      }
    }
  }

  fun addPreviewLog(log: String) {
    _previewLogs.update { (it + log).takeLast(50) }
  }

  fun clearPreviewLogs() {
    _previewLogs.value = emptyList()
  }

  // Compilation
  fun startBuild() {
    val cfg = _config.value
    _currentScreen.value = AppScreen.PROGRESS
    val logList = mutableListOf<BuildLogEntry>()

    _buildState.value = BuildUiState.Building(
      progress = 0.05f,
      statusText = "Initializing compiler engine...",
      logs = logList
    )

    viewModelScope.launch {
      try {
        val result = compilerEngine.compile(cfg) { progress, message, level ->
          logList.add(BuildLogEntry(message = message, level = level))
          _buildState.value = BuildUiState.Building(
            progress = progress,
            statusText = message,
            logs = ArrayList(logList)
          )
        }

        _buildState.value = BuildUiState.Success(result)
        _recentBuilds.update { listOf(result) + it.take(9) }
      } catch (e: Exception) {
        val errEntry = BuildLogEntry(message = "Build failed: ${e.message}", level = LogLevel.ERROR)
        logList.add(errEntry)
        _buildState.value = BuildUiState.Error(
          error = e.localizedMessage ?: "Unknown compilation error occurred.",
          logs = ArrayList(logList)
        )
      }
    }
  }

  // Export / Actions
  fun saveApkToDownloads(context: Context, result: BuildResult) {
    val file = result.apkFile ?: return
    try {
      val uri = compilerEngine.saveApkToDownloads(file, result.config.appTitle)
      if (uri != null) {
        Toast.makeText(context, "Saved APK to Downloads/HTML_to_APK folder!", Toast.LENGTH_LONG).show()
      } else {
        Toast.makeText(context, "Could not save to Downloads.", Toast.LENGTH_SHORT).show()
      }
    } catch (e: Exception) {
      Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun shareApk(context: Context, result: BuildResult) {
    val file = result.apkFile ?: return
    try {
      val intent = compilerEngine.createShareIntent(file)
      context.startActivity(android.content.Intent.createChooser(intent, "Share APK via"))
    } catch (e: Exception) {
      Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun installApk(context: Context, result: BuildResult) {
    val file = result.apkFile ?: return
    try {
      val intent = compilerEngine.createInstallIntent(file)
      context.startActivity(intent)
    } catch (e: Exception) {
      Toast.makeText(context, "Install error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }
}
