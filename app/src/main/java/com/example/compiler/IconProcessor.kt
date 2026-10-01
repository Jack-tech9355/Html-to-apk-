package com.example.compiler

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object IconProcessor {

  data class GeneratedIconAssets(
    val mipmapIcons: Map<String, ByteArray>,
    val appIconPng: ByteArray?,
    val splashPng: ByteArray?
  )

  fun process(context: Context, iconUriStr: String?, splashUriStr: String?): GeneratedIconAssets {
    val mipmaps = mutableMapOf<String, ByteArray>()
    var appIconBytes: ByteArray? = null
    var splashBytes: ByteArray? = null

    // 1. Process custom icon into all Android mipmap densities
    val iconBitmap = loadBitmap(context, iconUriStr)
    if (iconBitmap != null) {
      val baos = ByteArrayOutputStream()
      iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
      appIconBytes = baos.toByteArray()

      val sizes = mapOf(
        "res/mipmap-mdpi-v4" to 48,
        "res/mipmap-hdpi-v4" to 72,
        "res/mipmap-xhdpi-v4" to 96,
        "res/mipmap-xxhdpi-v4" to 144,
        "res/mipmap-xxxhdpi-v4" to 192
      )

      for ((folder, size) in sizes) {
        val scaled = Bitmap.createScaledBitmap(iconBitmap, size, size, true)
        val sbaos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 100, sbaos)
        val pngData = sbaos.toByteArray()
        mipmaps["$folder/ic_launcher.png"] = pngData
        mipmaps["$folder/ic_launcher_round.png"] = pngData
      }
    }

    // 2. Process custom splash image
    val splashBitmap = loadBitmap(context, splashUriStr)
    if (splashBitmap != null) {
      val baos = ByteArrayOutputStream()
      splashBitmap.compress(Bitmap.CompressFormat.PNG, 95, baos)
      splashBytes = baos.toByteArray()
    } else if (iconBitmap != null) {
      splashBytes = appIconBytes
    }

    return GeneratedIconAssets(mipmaps, appIconBytes, splashBytes)
  }

  fun copyToPermanentCache(context: Context, uri: Uri, prefix: String): String? {
    return try {
      val dir = File(context.filesDir, "branding").apply { mkdirs() }
      val target = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(target).use { output ->
          input.copyTo(output)
        }
      }
      target.absolutePath
    } catch (_: Exception) {
      null
    }
  }

  private fun loadBitmap(context: Context, uriStr: String?): Bitmap? {
    if (uriStr.isNullOrBlank()) return null
    return try {
      if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
        context.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
          BitmapFactory.decodeStream(it)
        }
      } else {
        val file = File(uriStr)
        if (file.exists()) {
          BitmapFactory.decodeFile(file.absolutePath)
        } else null
      }
    } catch (_: Exception) {
      null
    }
  }
}
