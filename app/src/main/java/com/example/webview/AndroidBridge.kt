package com.example.webview

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.example.model.AppBuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AndroidBridge(
  private val context: Context,
  private val config: AppBuildConfig,
  private val onToastReceived: ((String) -> Unit)? = null
) {

  @JavascriptInterface
  fun showToast(message: String) {
    CoroutineScope(Dispatchers.Main).launch {
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      onToastReceived?.invoke(message)
    }
  }

  @JavascriptInterface
  fun vibrate(milliseconds: Long) {
    try {
      val duration = milliseconds.coerceIn(10L, 1000L)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator?.vibrate(
          VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
        )
      } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(duration)
        }
      }
    } catch (_: Exception) {}
  }

  @JavascriptInterface
  fun getAppConfig(): String {
    return config.toJson()
  }

  @JavascriptInterface
  fun getDeviceInfo(): String {
    return """
      {
        "model": "${Build.MODEL}",
        "manufacturer": "${Build.MANUFACTURER}",
        "androidVersion": "${Build.VERSION.RELEASE}",
        "apiLevel": ${Build.VERSION.SDK_INT}
      }
    """.trimIndent()
  }
}
