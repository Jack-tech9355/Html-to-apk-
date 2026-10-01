package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.compiler.AxmlPackageModifier
import com.example.compiler.ZipSignerHelper
import com.example.model.AppBuildConfig
import com.example.model.AppPermissions
import com.example.model.SourceType
import com.example.repository.ProjectRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HTML to APK", appName)
  }

  @Test
  fun `app build config serialization roundtrip`() {
    val original = AppBuildConfig(
      appTitle = "Test Calculator",
      packageName = "com.test.calculator",
      versionCode = 2,
      versionName = "1.1.0",
      enableTitlebar = true,
      enableToolbar = true,
      enableSwipeRefresh = false,
      allowLongPressCopy = false,
      webUrl = "https://myportfolio.com",
      sourceType = SourceType.WEB_URL,
      permissions = AppPermissions(internet = true, camera = true)
    )

    val jsonStr = original.toJson()
    val restored = AppBuildConfig.fromJson(jsonStr)

    assertEquals(original.appTitle, restored.appTitle)
    assertEquals(original.packageName, restored.packageName)
    assertEquals(original.versionCode, restored.versionCode)
    assertEquals(original.versionName, restored.versionName)
    assertEquals(original.enableTitlebar, restored.enableTitlebar)
    assertEquals(original.enableToolbar, restored.enableToolbar)
    assertEquals(original.webUrl, restored.webUrl)
    assertEquals(SourceType.WEB_URL, restored.sourceType)
    assertEquals(true, restored.permissions.camera)
  }

  @Test
  fun `project repository saves and loads projects`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ProjectRepository(context)

    val project = repo.createNewProject(
      name = "My Personal Web App",
      sourceType = SourceType.WEB_URL,
      webUrl = "https://portfolio.me"
    )

    assertNotNull(project.id)
    assertEquals("My Personal Web App", project.name)
    assertEquals("com.htmltoapk.mypersonalwebapp", project.config.packageName)
    assertEquals("https://portfolio.me", project.config.webUrl)

    val projects = repo.projects.value
    assertTrue(projects.any { it.id == project.id })
  }

  @Test
  fun `zip signer helper computes sha256`() {
    val data = "Hello HTML to APK".toByteArray()
    val hash = ZipSignerHelper.computeSha256(data)
    assertNotNull(hash)
    val hex = ZipSignerHelper.toHex(hash)
    assertEquals(64, hex.length)
  }
}
