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
import com.example.repository.ProjectRepository
import com.example.repository.SavedProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
  val projectRepository = ProjectRepository(application)

  val savedProjects: StateFlow<List<SavedProject>> = projectRepository.projects

  private val _activeProjectId = MutableStateFlow<String?>(null)
  val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

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

  // --- Project Management & Persistence ---

  fun createProject(
    name: String,
    sourceType: SourceType,
    webUrl: String = "",
    customPackage: String? = null
  ) {
    viewModelScope.launch {
      val project = projectRepository.createNewProject(
        name = name,
        sourceType = sourceType,
        webUrl = webUrl,
        customPackage = customPackage
      )
      _activeProjectId.value = project.id
      _config.value = project.config
      _builderStep.value = 1
      _currentScreen.value = AppScreen.BUILDER
    }
  }

  fun openProject(project: SavedProject) {
    _activeProjectId.value = project.id
    _config.value = project.config
    _builderStep.value = 1
    _currentScreen.value = AppScreen.BUILDER
  }

  fun saveCurrentProject() {
    viewModelScope.launch {
      val currentConfig = _config.value
      val currentId = _activeProjectId.value ?: currentConfig.id
      val project = SavedProject(
        id = currentId,
        name = currentConfig.appTitle,
        description = if (currentConfig.sourceType == SourceType.WEB_URL) currentConfig.webUrl else "HTML5 Application",
        config = currentConfig
      )
      projectRepository.saveProject(project)
    }
  }

  fun deleteProject(projectId: String) {
    viewModelScope.launch {
      projectRepository.deleteProject(projectId)
      if (_activeProjectId.value == projectId) {
        _activeProjectId.value = null
        _config.value = AppBuildConfig()
      }
    }
  }

  // Configuration Updates
  fun updateTitle(title: String) {
    _config.update {
      val sanitized = title.trim().ifEmpty { "My Web App" }
      val autoPkg = "com.htmltoapk." + sanitized.lowercase().replace(Regex("[^a-z0-9]"), "").ifEmpty { "app" }
      it.copy(
        appTitle = title,
        packageName = if (it.packageName == "com.htmltoapk.mywebapp" || it.packageName.startsWith("com.htmltoapk.")) autoPkg else it.packageName
      )
    }
    saveCurrentProject()
  }

  fun updatePackageName(pkg: String) {
    _config.update { it.copy(packageName = pkg.trim()) }
    saveCurrentProject()
  }

  fun updateVersion(versionName: String, versionCode: Int) {
    _config.update { it.copy(versionName = versionName, versionCode = versionCode) }
    saveCurrentProject()
  }

  fun updateWebUrl(url: String) {
    _config.update { it.copy(webUrl = url.trim(), sourceType = SourceType.WEB_URL) }
    saveCurrentProject()
  }

  fun updateOfflineFallbackHtml(html: String) {
    _config.update { it.copy(offlineFallbackHtml = html) }
    saveCurrentProject()
  }

  fun updateRawHtml(html: String) {
    _config.update { it.copy(rawHtmlContent = html, sourceType = SourceType.RAW_HTML) }
    saveCurrentProject()
  }

  fun updateCustomCss(css: String) {
    _config.update { it.copy(customCss = css) }
    saveCurrentProject()
  }

  fun updateCustomJs(js: String) {
    _config.update { it.copy(customJs = js) }
    saveCurrentProject()
  }

  fun updateSourceType(type: SourceType, uri: String? = null) {
    _config.update {
      it.copy(
        sourceType = type,
        iconUri = uri ?: it.iconUri
      )
    }
    saveCurrentProject()
  }

  fun updateIconUri(uri: String?) {
    _config.update { it.copy(iconUri = uri) }
    saveCurrentProject()
  }

  fun updateSplashUri(uri: String?) {
    _config.update { it.copy(splashUri = uri) }
    saveCurrentProject()
  }

  fun updateToggleTitlebar(enabled: Boolean) {
    _config.update { it.copy(enableTitlebar = enabled) }
    saveCurrentProject()
  }

  fun updateToggleToolbar(enabled: Boolean) {
    _config.update { it.copy(enableToolbar = enabled) }
    saveCurrentProject()
  }

  fun updateToggleSwipeRefresh(enabled: Boolean) {
    _config.update { it.copy(enableSwipeRefresh = enabled) }
    saveCurrentProject()
  }

  fun updateToggleLongPressCopy(enabled: Boolean) {
    _config.update { it.copy(allowLongPressCopy = enabled) }
    saveCurrentProject()
  }

  fun updateToggleZoom(enabled: Boolean) {
    _config.update { it.copy(allowZoom = enabled) }
    saveCurrentProject()
  }

  fun updateOrientation(orientation: String) {
    _config.update { it.copy(orientation = orientation) }
    saveCurrentProject()
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
    saveCurrentProject()
  }

  fun loadPreset(presetType: String) {
    when (presetType) {
      "DEFAULT" -> {
        _config.update {
          it.copy(
            appTitle = "Counter & Bridge",
            packageName = "com.htmltoapk.counterbridge",
            sourceType = SourceType.RAW_HTML,
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
            appTitle = "Retro Arcade Game",
            packageName = "com.htmltoapk.retrogames",
            sourceType = SourceType.RAW_HTML,
            rawHtmlContent = AppBuildConfig.PRESET_RETRO_GAME,
            enableSwipeRefresh = false,
            allowLongPressCopy = false,
            enableTitlebar = false,
            enableToolbar = false
          )
        }
      }
      "CYBER" -> {
        _config.update {
          it.copy(
            appTitle = "Telemetry Dashboard",
            packageName = "com.htmltoapk.cyberdash",
            sourceType = SourceType.RAW_HTML,
            rawHtmlContent = AppBuildConfig.PRESET_CYBER_DASHBOARD,
            enableSwipeRefresh = true,
            allowLongPressCopy = true,
            enableTitlebar = true,
            enableToolbar = true
          )
        }
      }
      "WEB_URL" -> {
        _config.update {
          it.copy(
            appTitle = "Web App Portal",
            packageName = "com.htmltoapk.webappportal",
            sourceType = SourceType.WEB_URL,
            webUrl = "https://example.com",
            enableTitlebar = true,
            enableToolbar = true,
            enableSwipeRefresh = true
          )
        }
      }
    }
    saveCurrentProject()
  }

  // --- Build Orchestration ---

  fun startBuild() {
    _currentScreen.value = AppScreen.PROGRESS
    _buildState.value = BuildUiState.Building(
      progress = 0.05f,
      statusText = "Initializing build engine...",
      logs = listOf(BuildLogEntry(message = "Build task queued", level = LogLevel.INFO))
    )

    viewModelScope.launch {
      try {
        val result = compilerEngine.compile(_config.value) { progress, status, level ->
          val currentLogs = when (val state = _buildState.value) {
            is BuildUiState.Building -> state.logs + BuildLogEntry(message = status, level = level)
            else -> listOf(BuildLogEntry(message = status, level = level))
          }
          _buildState.value = BuildUiState.Building(progress, status, currentLogs)
        }

        _buildState.value = BuildUiState.Success(result)
        _recentBuilds.update { listOf(result) + it.take(9) }
        saveCurrentProject()
      } catch (e: Exception) {
        val currentLogs = when (val state = _buildState.value) {
          is BuildUiState.Building -> state.logs + BuildLogEntry(message = "ERROR: ${e.message}", level = LogLevel.ERROR)
          else -> listOf(BuildLogEntry(message = "ERROR: ${e.message}", level = LogLevel.ERROR))
        }
        _buildState.value = BuildUiState.Error(
          error = e.message ?: "Unknown compilation error",
          logs = currentLogs
        )
      }
    }
  }

  fun saveApkToDownloads(context: Context, result: BuildResult) {
    result.apkFile?.let { file ->
      val uri = compilerEngine.saveApkToDownloads(file, result.config.appTitle)
      if (uri != null) {
        Toast.makeText(context, "APK saved to Downloads/HTML_to_APK", Toast.LENGTH_LONG).show()
      } else {
        Toast.makeText(context, "Failed to save APK", Toast.LENGTH_SHORT).show()
      }
    }
  }

  fun shareApk(context: Context, result: BuildResult) {
    result.apkFile?.let { file ->
      val intent = compilerEngine.createShareIntent(file)
      context.startActivity(android.content.Intent.createChooser(intent, "Share Compiled APK"))
    }
  }

  fun installApk(context: Context, result: BuildResult) {
    result.apkFile?.let { file ->
      try {
        val intent = compilerEngine.createInstallIntent(file)
        context.startActivity(intent)
      } catch (e: Exception) {
        Toast.makeText(context, "Install launch failed: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
  }

  fun addPreviewLog(log: String) {
    _previewLogs.update { (it + log).takeLast(100) }
  }

  fun clearPreviewLogs() {
    _previewLogs.value = emptyList()
  }
}
