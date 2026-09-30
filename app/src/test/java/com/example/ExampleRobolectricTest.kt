package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.compiler.ZipSignerHelper
import com.example.model.AppBuildConfig
import com.example.model.AppPermissions
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
    assertEquals(original.enableSwipeRefresh, restored.enableSwipeRefresh)
    assertEquals(original.allowLongPressCopy, restored.allowLongPressCopy)
    assertEquals(true, restored.permissions.camera)
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
