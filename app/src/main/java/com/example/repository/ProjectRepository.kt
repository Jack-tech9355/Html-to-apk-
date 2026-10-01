package com.example.repository

import android.content.Context
import com.example.model.AppBuildConfig
import com.example.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class SavedProject(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val description: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val config: AppBuildConfig
) {
  fun toJson(): JSONObject {
    return JSONObject().apply {
      put("id", id)
      put("name", name)
      put("description", description)
      put("createdAt", createdAt)
      put("updatedAt", updatedAt)
      put("configJson", config.toJson())
    }
  }

  companion object {
    fun fromJson(json: JSONObject): SavedProject {
      val configJsonStr = json.optString("configJson", "")
      val config = if (configJsonStr.isNotBlank()) {
        try {
          AppBuildConfig.fromJson(configJsonStr)
        } catch (_: Exception) {
          AppBuildConfig()
        }
      } else AppBuildConfig()

      return SavedProject(
        id = json.optString("id", UUID.randomUUID().toString()),
        name = json.optString("name", config.appTitle),
        description = json.optString("description", ""),
        createdAt = json.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
        config = config
      )
    }
  }
}

class ProjectRepository(private val context: Context) {

  private val storageFile: File
    get() = File(context.filesDir, "saved_projects.json")

  private val _projects = MutableStateFlow<List<SavedProject>>(emptyList())
  val projects: StateFlow<List<SavedProject>> = _projects.asStateFlow()

  init {
    loadProjects()
  }

  fun loadProjects() {
    try {
      if (!storageFile.exists()) {
        val seeded = createDefaultSeedProjects()
        saveProjectsToDisk(seeded)
        _projects.value = seeded
        return
      }

      val jsonStr = storageFile.readText(Charsets.UTF_8)
      if (jsonStr.isBlank()) {
        _projects.value = emptyList()
        return
      }

      val jsonArray = JSONArray(jsonStr)
      val list = mutableListOf<SavedProject>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(SavedProject.fromJson(obj))
      }
      _projects.value = list.sortedByDescending { it.updatedAt }
    } catch (_: Exception) {
      _projects.value = createDefaultSeedProjects()
    }
  }

  suspend fun saveProject(project: SavedProject) = withContext(Dispatchers.IO) {
    val current = _projects.value.toMutableList()
    val index = current.indexOfFirst { it.id == project.id }
    val updatedProject = project.copy(updatedAt = System.currentTimeMillis())

    if (index >= 0) {
      current[index] = updatedProject
    } else {
      current.add(0, updatedProject)
    }

    _projects.value = current.sortedByDescending { it.updatedAt }
    saveProjectsToDisk(_projects.value)
  }

  suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
    val current = _projects.value.filter { it.id != projectId }
    _projects.value = current
    saveProjectsToDisk(current)
  }

  suspend fun createNewProject(
    name: String,
    sourceType: SourceType,
    webUrl: String = "",
    customPackage: String? = null
  ): SavedProject = withContext(Dispatchers.IO) {
    val slug = name.lowercase().replace(Regex("[^a-z0-9]"), "").ifEmpty { "app" }
    val pkg = customPackage?.takeIf { it.isNotBlank() } ?: "com.htmltoapk.$slug"

    val newConfig = when (sourceType) {
      SourceType.WEB_URL -> {
        AppBuildConfig(
          appTitle = name,
          packageName = pkg,
          sourceType = SourceType.WEB_URL,
          webUrl = webUrl.ifBlank { "https://example.com" },
          enableTitlebar = true,
          enableToolbar = true,
          enableSwipeRefresh = true
        )
      }
      SourceType.RAW_HTML -> {
        AppBuildConfig(
          appTitle = name,
          packageName = pkg,
          sourceType = SourceType.RAW_HTML,
          rawHtmlContent = AppBuildConfig.DEFAULT_HTML_PRESET,
          enableTitlebar = false,
          enableToolbar = true,
          enableSwipeRefresh = true
        )
      }
      else -> {
        AppBuildConfig(
          appTitle = name,
          packageName = pkg,
          sourceType = sourceType
        )
      }
    }

    val project = SavedProject(
      id = UUID.randomUUID().toString(),
      name = name,
      description = if (sourceType == SourceType.WEB_URL) "Website wrapper for $webUrl" else "Native HTML5 Application",
      config = newConfig
    )

    saveProject(project)
    project
  }

  private fun saveProjectsToDisk(list: List<SavedProject>) {
    try {
      val jsonArray = JSONArray()
      for (p in list) {
        jsonArray.put(p.toJson())
      }
      storageFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
    } catch (_: Exception) {}
  }

  private fun createDefaultSeedProjects(): List<SavedProject> {
    return listOf(
      SavedProject(
        id = "demo_game",
        name = "Retro Arcade Game",
        description = "HTML5 Canvas micro-game with touch sound controls and high score engine",
        config = AppBuildConfig(
          appTitle = "Retro Arcade Game",
          packageName = "com.htmltoapk.retrogames",
          sourceType = SourceType.RAW_HTML,
          rawHtmlContent = AppBuildConfig.PRESET_RETRO_GAME,
          enableTitlebar = false,
          enableToolbar = false,
          enableSwipeRefresh = false,
          allowLongPressCopy = false
        )
      ),
      SavedProject(
        id = "demo_web_portal",
        name = "Web App Wrapper",
        description = "Website-to-APK conversion with native pull-to-refresh & toolbar",
        config = AppBuildConfig(
          appTitle = "Web App Wrapper",
          packageName = "com.htmltoapk.webappportal",
          sourceType = SourceType.WEB_URL,
          webUrl = "https://example.com",
          enableTitlebar = true,
          enableToolbar = true,
          enableSwipeRefresh = true
        )
      ),
      SavedProject(
        id = "demo_cyber_dashboard",
        name = "Telemetry Dashboard",
        description = "Dark cyberpunk analytics interface with native AndroidBridge integration",
        config = AppBuildConfig(
          appTitle = "Telemetry Dashboard",
          packageName = "com.htmltoapk.cyberdash",
          sourceType = SourceType.RAW_HTML,
          rawHtmlContent = AppBuildConfig.PRESET_CYBER_DASHBOARD,
          enableTitlebar = true,
          enableToolbar = true,
          enableSwipeRefresh = true
        )
      )
    )
  }
}
