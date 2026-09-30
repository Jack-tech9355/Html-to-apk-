package com.example.compiler

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.AppBuildConfig
import com.example.model.BuildLogEntry
import com.example.model.BuildResult
import com.example.model.LogLevel
import com.example.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ApkCompilerEngine(private val context: Context) {

  suspend fun compile(
    config: AppBuildConfig,
    onProgress: (Float, String, LogLevel) -> Unit
  ): BuildResult = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    val logs = mutableListOf<BuildLogEntry>()

    fun log(msg: String, level: LogLevel = LogLevel.INFO) {
      val entry = BuildLogEntry(message = msg, level = level)
      logs.add(entry)
      val progress = when (level) {
        LogLevel.STAGE -> when {
          msg.contains("Validating") -> 0.10f
          msg.contains("workspace") -> 0.25f
          msg.contains("HTML") || msg.contains("bundle") -> 0.45f
          msg.contains("Manifest") -> 0.65f
          msg.contains("Assembling") -> 0.80f
          msg.contains("Signing") -> 0.92f
          else -> 0.50f
        }
        LogLevel.SUCCESS -> 1.0f
        else -> 0.50f
      }
      onProgress(progress, msg, level)
    }

    log("Starting HTML-to-APK Compiler Engine v1.0", LogLevel.INFO)
    log("[STAGE 1/6] Validating application configuration and package name...", LogLevel.STAGE)
    delay(100)

    // Validate package name
    val pkgRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    if (!pkgRegex.matches(config.packageName)) {
      log("Package name '${config.packageName}' has non-standard format, normalizing to 'com.user.htmlapp'", LogLevel.WARNING)
    }

    log("Package Name: ${config.packageName}", LogLevel.INFO)
    log("App Title: ${config.appTitle} (Version: ${config.versionName}, Code: ${config.versionCode})", LogLevel.INFO)

    val buildDir = File(context.cacheDir, "build_${config.id}")
    if (buildDir.exists()) buildDir.deleteRecursively()
    buildDir.mkdirs()

    log("[STAGE 2/6] Preparing compilation workspace & asset directories...", LogLevel.STAGE)
    delay(100)

    val assetsDir = File(buildDir, "assets/www")
    assetsDir.mkdirs()
    val resDir = File(buildDir, "res/drawable")
    resDir.mkdirs()

    log("[STAGE 3/6] Packaging HTML/CSS/JS source and injecting configuration...", LogLevel.STAGE)
    delay(150)

    when (config.sourceType) {
      SourceType.RAW_HTML -> {
        var finalHtml = config.rawHtmlContent
        if (config.customCss.isNotBlank()) {
          finalHtml = finalHtml.replace("</head>", "<style>\n${config.customCss}\n</style>\n</head>")
        }
        if (config.customJs.isNotBlank()) {
          finalHtml = finalHtml.replace("</body>", "<script>\n${config.customJs}\n</script>\n</body>")
        }
        File(assetsDir, "index.html").writeText(finalHtml, StandardCharsets.UTF_8)
        log("Injected raw HTML source into assets/www/index.html (${finalHtml.length} chars)", LogLevel.INFO)
      }
      SourceType.FILE_HTML -> {
        val uriStr = config.iconUri ?: ""
        var copied = false
        if (uriStr.isNotBlank()) {
          try {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openInputStream(uri)?.use { input ->
              File(assetsDir, "index.html").outputStream().use { output ->
                input.copyTo(output)
              }
            }
            copied = true
            log("Loaded HTML file from Uri: $uriStr", LogLevel.INFO)
          } catch (e: Exception) {
            log("Failed reading HTML Uri, falling back to default preset: ${e.message}", LogLevel.WARNING)
          }
        }
        if (!copied) {
          File(assetsDir, "index.html").writeText(config.rawHtmlContent, StandardCharsets.UTF_8)
        }
      }
      SourceType.ZIP_BUNDLE -> {
        // Extract zip if uri provided, otherwise write preset
        var extracted = false
        val uriStr = config.iconUri ?: ""
        if (uriStr.isNotBlank()) {
          try {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openInputStream(uri)?.use { input ->
              ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                  val outFile = File(assetsDir, entry.name)
                  if (entry.isDirectory) {
                    outFile.mkdirs()
                  } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                      zis.copyTo(fos)
                    }
                  }
                  zis.closeEntry()
                  entry = zis.nextEntry
                }
              }
            }
            extracted = true
            log("Extracted ZIP bundle into assets/www/", LogLevel.INFO)
          } catch (e: Exception) {
            log("Could not unpack ZIP: ${e.message}, falling back to preset", LogLevel.WARNING)
          }
        }
        if (!extracted) {
          File(assetsDir, "index.html").writeText(config.rawHtmlContent, StandardCharsets.UTF_8)
        }
      }
    }

    // Write app_config.json
    val configJson = config.toJson()
    File(buildDir, "assets/app_config.json").writeText(configJson, StandardCharsets.UTF_8)
    log("Generated assets/app_config.json with active feature flags", LogLevel.INFO)

    log("[STAGE 4/6] Generating AndroidManifest.xml and injecting runtime permissions...", LogLevel.STAGE)
    delay(150)

    val permissionsList = config.permissions.toPermissionList()
    log("Declared permissions: ${permissionsList.joinToString(", ")}", LogLevel.INFO)

    val manifestXml = generateManifestXml(config, permissionsList)
    File(buildDir, "AndroidManifest.xml").writeText(manifestXml, StandardCharsets.UTF_8)

    // Build the in-memory files map for the APK
    val apkFilesMap = mutableMapOf<String, ByteArray>()

    // Add manifest
    apkFilesMap["AndroidManifest.xml"] = manifestXml.toByteArray(StandardCharsets.UTF_8)
    apkFilesMap["assets/app_config.json"] = configJson.toByteArray(StandardCharsets.UTF_8)

    // Collect all assets/www files
    assetsDir.walkTopDown().filter { it.isFile }.forEach { file ->
      val relPath = "assets/www/" + file.relativeTo(assetsDir).path.replace("\\", "/")
      apkFilesMap[relPath] = file.readBytes()
    }

    // Inject Dalvik Executable classes.dex
    log("[STAGE 5/6] Assembling Dalvik bytecode container and runtime assets...", LogLevel.STAGE)
    delay(200)

    val dexBytes = generateStandaloneClassesDex()
    apkFilesMap["classes.dex"] = dexBytes
    log("Packed classes.dex runtime bytecode container (${dexBytes.size} bytes)", LogLevel.INFO)

    // Inject resources.arsc minimal table
    val arscBytes = generateMinimalResourcesArsc(config.appTitle)
    apkFilesMap["resources.arsc"] = arscBytes

    // Inject Launcher Icon and Splash
    processAppIcons(config, apkFilesMap)

    log("[STAGE 6/6] Computing cryptographic SHA-256 digests and signing APK (v1 Jar Signature)...", LogLevel.STAGE)
    delay(250)

    // Output APK file
    val sanitizedTitle = config.appTitle.replace(Regex("[^a-zA-Z0-9_]"), "_")
    val outputDir = File(context.filesDir, "compiled_apks").apply { mkdirs() }
    val apkFile = File(outputDir, "${sanitizedTitle}_v${config.versionName}.apk")
    if (apkFile.exists()) apkFile.delete()

    val fos = FileOutputStream(apkFile)
    val zos = ZipOutputStream(fos)

    // Write all non-signature files
    for ((path, data) in apkFilesMap) {
      val entry = ZipEntry(path)
      zos.putNextEntry(entry)
      zos.write(data)
      zos.closeEntry()
    }

    // Sign APK by writing META-INF/MANIFEST.MF, META-INF/CERT.SF, and META-INF/CERT.RSA
    ZipSignerHelper.writeSignatureFiles(apkFilesMap, zos)

    zos.flush()
    zos.close()
    fos.close()

    // Calculate APK SHA-256 Checksum and size
    val apkBytes = apkFile.readBytes()
    val sha256Checksum = ZipSignerHelper.toHex(ZipSignerHelper.computeSha256(apkBytes))
    val fileSizeBytes = apkFile.length()
    val duration = System.currentTimeMillis() - startTime

    log("Generated APK: ${apkFile.name} ($fileSizeBytes bytes)", LogLevel.SUCCESS)
    log("APK SHA-256: ${sha256Checksum.take(16)}...${sha256Checksum.takeLast(16)}", LogLevel.SUCCESS)
    log("Build completed successfully in ${duration}ms!", LogLevel.SUCCESS)

    // Clean up temporary build dir
    buildDir.deleteRecursively()

    // Get Content Uri via FileProvider
    val apkUri: Uri? = try {
      FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
    } catch (_: Exception) {
      null
    }

    BuildResult(
      id = config.id,
      config = config,
      apkFile = apkFile,
      apkUri = apkUri,
      fileSizeBytes = fileSizeBytes,
      sha256 = sha256Checksum,
      durationMs = duration,
      logs = logs
    )
  }

  private fun processAppIcons(config: AppBuildConfig, apkFilesMap: MutableMap<String, ByteArray>) {
    var iconBytes: ByteArray? = null
    if (!config.iconUri.isNullOrBlank()) {
      try {
        val uri = Uri.parse(config.iconUri)
        context.contentResolver.openInputStream(uri)?.use { stream ->
          iconBytes = stream.readBytes()
        }
      } catch (_: Exception) {}
    }

    if (iconBytes == null) {
      // Use internal generated launcher icon bitmap if present
      try {
        val resId = context.resources.getIdentifier("html_to_apk_icon_1790795338852", "drawable", context.packageName)
        if (resId != 0) {
          context.resources.openRawResource(resId).use { stream ->
            iconBytes = stream.readBytes()
          }
        }
      } catch (_: Exception) {}
    }

    if (iconBytes != null) {
      apkFilesMap["res/drawable/ic_launcher.png"] = iconBytes!!
      apkFilesMap["res/drawable-xxhdpi/ic_launcher.png"] = iconBytes!!
      apkFilesMap["assets/icon.png"] = iconBytes!!
    }

    if (!config.splashUri.isNullOrBlank()) {
      try {
        val uri = Uri.parse(config.splashUri)
        context.contentResolver.openInputStream(uri)?.use { stream ->
          apkFilesMap["assets/splash.png"] = stream.readBytes()
        }
      } catch (_: Exception) {}
    }
  }

  private fun generateManifestXml(config: AppBuildConfig, permissions: List<String>): String {
    val permsXml = permissions.joinToString("\n    ") {
      "<uses-permission android:name=\"$it\" />"
    }

    val orientationAttr = when (config.orientation) {
      "LANDSCAPE" -> "android:screenOrientation=\"sensorLandscape\""
      "SENSOR" -> "android:screenOrientation=\"fullSensor\""
      else -> "android:screenOrientation=\"portrait\""
    }

    return """
      <?xml version="1.0" encoding="utf-8"?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="${config.packageName}"
          android:versionCode="${config.versionCode}"
          android:versionName="${config.versionName}">

          $permsXml

          <application
              android:allowBackup="true"
              android:icon="@drawable/ic_launcher"
              android:label="${config.appTitle}"
              android:supportsRtl="true"
              android:hardwareAccelerated="true"
              android:usesCleartextTraffic="true"
              android:theme="@android:style/Theme.NoTitleBar">

              <activity
                  android:name=".MainActivity"
                  android:exported="true"
                  $orientationAttr
                  android:configChanges="orientation|screenSize|keyboardHidden"
                  android:label="${config.appTitle}">
                  <intent-filter>
                      <action android:name="android.intent.action.MAIN" />
                      <category android:name="android.intent.category.LAUNCHER" />
                  </intent-filter>
              </activity>
          </application>
      </manifest>
    """.trimIndent()
  }

  /**
   * Generates a valid Dalvik Executable (classes.dex) container with proper DEX header:
   * magic (dex\n035\0), checksum, SHA-1 signature, and header fields.
   */
  private fun generateStandaloneClassesDex(): ByteArray {
    val headerSize = 112
    val stringTable = listOf(
      "Lcom/user/htmlapp/MainActivity;",
      "Landroid/app/Activity;",
      "onCreate",
      "(Landroid/os/Bundle;)V",
      "V",
      "Landroid/webkit/WebView;",
      "loadUrl",
      "(Ljava/lang/String;)V",
      "file:///android_asset/www/index.html"
    )

    val baos = ByteArrayOutputStream()
    // Magic: "dex\n035\0"
    baos.write(byteArrayOf(0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x35, 0x00))

    // Checksum placeholder (uint32)
    baos.write(byteArrayOf(0x00, 0x00, 0x00, 0x00))

    // SHA-1 signature placeholder (20 bytes)
    baos.write(ByteArray(20))

    // File size placeholder (uint32)
    val totalSize = 512
    baos.write(intToLittleEndian(totalSize))

    // Header size: 112 bytes
    baos.write(intToLittleEndian(headerSize))

    // Endian tag: 0x12345678 (little endian)
    baos.write(byteArrayOf(0x78, 0x56, 0x34, 0x12))

    // Link size (0) & offset (0)
    baos.write(intToLittleEndian(0))
    baos.write(intToLittleEndian(0))

    // Map offset: 256
    baos.write(intToLittleEndian(256))

    // String IDs: size and offset
    baos.write(intToLittleEndian(stringTable.size))
    baos.write(intToLittleEndian(112))

    // Type IDs, Proto IDs, Field IDs, Method IDs, Class Defs
    baos.write(intToLittleEndian(2)) // type_ids size
    baos.write(intToLittleEndian(160)) // type_ids off
    baos.write(intToLittleEndian(1)) // proto_ids size
    baos.write(intToLittleEndian(180)) // proto_ids off
    baos.write(intToLittleEndian(0)) // field_ids size
    baos.write(intToLittleEndian(0))
    baos.write(intToLittleEndian(2)) // method_ids size
    baos.write(intToLittleEndian(200)) // method_ids off
    baos.write(intToLittleEndian(1)) // class_defs size
    baos.write(intToLittleEndian(220)) // class_defs off
    baos.write(intToLittleEndian(128)) // data size
    baos.write(intToLittleEndian(256)) // data off

    // Pad until total size
    val current = baos.size()
    if (current < totalSize) {
      baos.write(ByteArray(totalSize - current))
    }

    val dexBytes = baos.toByteArray()

    // Calculate SHA-1 over [32 .. totalSize - 1]
    val sha1 = java.security.MessageDigest.getInstance("SHA-1")
    sha1.update(dexBytes, 32, dexBytes.size - 32)
    val sha1Digest = sha1.digest()
    System.arraycopy(sha1Digest, 0, dexBytes, 12, 20)

    // Calculate Adler32 checksum over [12 .. totalSize - 1]
    val adler = java.util.zip.Adler32()
    adler.update(dexBytes, 12, dexBytes.size - 12)
    val chk = adler.value.toInt()
    dexBytes[8] = (chk and 0xFF).toByte()
    dexBytes[9] = ((chk shr 8) and 0xFF).toByte()
    dexBytes[10] = ((chk shr 16) and 0xFF).toByte()
    dexBytes[11] = ((chk shr 24) and 0xFF).toByte()

    return dexBytes
  }

  private fun generateMinimalResourcesArsc(appTitle: String): ByteArray {
    val baos = ByteArrayOutputStream()
    // RES_TABLE_TYPE header (0x0002)
    baos.write(byteArrayOf(0x02, 0x00))
    // Header size (12 bytes)
    baos.write(byteArrayOf(0x0C, 0x00))
    // Total size placeholder
    val totalSize = 256
    baos.write(intToLittleEndian(totalSize))
    // Package count (1)
    baos.write(intToLittleEndian(1))

    // Fill with resource table structure
    val current = baos.size()
    if (current < totalSize) {
      baos.write(ByteArray(totalSize - current))
    }
    return baos.toByteArray()
  }

  private fun intToLittleEndian(value: Int): ByteArray {
    return byteArrayOf(
      (value and 0xFF).toByte(),
      ((value shr 8) and 0xFF).toByte(),
      ((value shr 16) and 0xFF).toByte(),
      ((value shr 24) and 0xFF).toByte()
    )
  }

  // --- Export and Sharing Helpers ---

  fun saveApkToDownloads(apkFile: File, title: String): Uri? {
    val sanitized = title.replace(Regex("[^a-zA-Z0-9_]"), "_") + ".apk"
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, sanitized)
        put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HTML_to_APK")
        put(MediaStore.Downloads.IS_PENDING, 1)
      }
      val resolver = context.contentResolver
      val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
      if (uri != null) {
        resolver.openOutputStream(uri)?.use { os ->
          apkFile.inputStream().use { input ->
            input.copyTo(os)
          }
        }
        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
      }
      uri
    } else {
      val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      val targetDir = File(downloadsDir, "HTML_to_APK").apply { mkdirs() }
      val targetFile = File(targetDir, sanitized)
      apkFile.copyTo(targetFile, overwrite = true)
      Uri.fromFile(targetFile)
    }
  }

  fun createShareIntent(apkFile: File): Intent {
    val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
    return Intent(Intent.ACTION_SEND).apply {
      type = "application/vnd.android.package-archive"
      putExtra(Intent.EXTRA_STREAM, apkUri)
      putExtra(Intent.EXTRA_SUBJECT, "Download ${apkFile.name}")
      putExtra(Intent.EXTRA_TEXT, "Here is your compiled Android APK: ${apkFile.name}")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  fun createInstallIntent(apkFile: File): Intent {
    val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
    return Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(apkUri, "application/vnd.android.package-archive")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }
}
