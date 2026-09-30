package com.example.model

import android.net.Uri
import java.io.File

data class BuildResult(
  val id: String = java.util.UUID.randomUUID().toString(),
  val config: AppBuildConfig,
  val apkFile: File?,
  val apkUri: Uri?,
  val fileSizeBytes: Long,
  val sha256: String,
  val durationMs: Long,
  val timestamp: Long = System.currentTimeMillis(),
  val logs: List<BuildLogEntry> = emptyList()
) {
  val formattedSize: String
    get() {
      if (fileSizeBytes <= 0) return "0 KB"
      val kb = fileSizeBytes / 1024.0
      return if (kb < 1024) {
        String.format("%.1f KB", kb)
      } else {
        String.format("%.2f MB", kb / 1024.0)
      }
    }
}
