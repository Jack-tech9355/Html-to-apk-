package com.example.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.AppBuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratedAppWebView(
  config: AppBuildConfig,
  modifier: Modifier = Modifier,
  onClose: (() -> Unit)? = null,
  onConsoleLog: ((String) -> Unit)? = null
) {
  val context = LocalContext.current
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var canGoBack by remember { mutableStateOf(false) }
  var canGoForward by remember { mutableStateOf(false) }
  var pageTitle by remember { mutableStateOf(config.appTitle) }
  var currentUrl by remember { mutableStateOf("about:blank") }
  var pageProgress by remember { mutableIntStateOf(0) }
  var isLoading by remember { mutableStateOf(true) }
  var isRefreshing by remember { mutableStateOf(false) }
  var pullDistance by remember { mutableFloatStateOf(0f) }

  // File upload callback handler for WebChromeClient
  var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
  ) { uris ->
    fileUploadCallback?.onReceiveValue(uris.toTypedArray())
    fileUploadCallback = null
  }

  // Handle hardware back navigation
  BackHandler(enabled = canGoBack) {
    webViewInstance?.let { wv ->
      if (wv.canGoBack()) {
        wv.goBack()
      }
    }
  }

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Titlebar (Top App Bar) if enabled
    if (config.enableTitlebar) {
      TopAppBar(
        title = {
          Column {
            Text(
              text = pageTitle,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = config.packageName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        actions = {
          if (onClose != null) {
            IconButton(onClick = onClose) {
              Icon(imageVector = Icons.Default.Close, contentDescription = "Close Preview")
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
      )
    }

    // Loading Progress Bar
    if (isLoading && pageProgress in 1..99) {
      LinearProgressIndicator(
        progress = { pageProgress / 100f },
        modifier = Modifier.fillMaxWidth().height(3.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = Color.Transparent
      )
    }

    // Main WebView Container with optional Swipe Refresh gesture
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .pointerInput(config.enableSwipeRefresh) {
          if (config.enableSwipeRefresh) {
            detectDragGestures(
              onDragStart = { pullDistance = 0f },
              onDragEnd = {
                if (pullDistance > 160f) {
                  isRefreshing = true
                  webViewInstance?.reload()
                }
                pullDistance = 0f
              },
              onDragCancel = { pullDistance = 0f },
              onDrag = { change, dragAmount ->
                if (webViewInstance?.scrollY == 0 && dragAmount.y > 0) {
                  pullDistance = (pullDistance + dragAmount.y).coerceAtLeast(0f)
                  change.consume()
                }
              }
            )
          }
        }
    ) {
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
          createConfiguredWebView(
            context = ctx,
            config = config,
            onTitleChanged = { title -> pageTitle = title },
            onProgressChanged = { progress ->
              pageProgress = progress
              isLoading = progress < 100
              if (progress >= 100) isRefreshing = false
            },
            onUrlChanged = { url ->
              currentUrl = url
            },
            onNavStateChanged = { back, forward ->
              canGoBack = back
              canGoForward = forward
            },
            onFilePickerRequested = { callback ->
              fileUploadCallback = callback
              filePickerLauncher.launch(arrayOf("*/*"))
            },
            onConsoleLog = onConsoleLog
          ).also { wv ->
            webViewInstance = wv
            loadContentIntoWebView(wv, config)
          }
        },
        update = { wv ->
          webViewInstance = wv
        }
      )

      // Swipe Refresh visual indicator
      androidx.compose.animation.AnimatedVisibility(
        visible = isRefreshing || pullDistance > 80f,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = (pullDistance * 0.4f).coerceIn(12f, 72f).dp)
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
          shadowElevation = 6.dp,
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            if (isRefreshing) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary
              )
            } else {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Pull to Refresh",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }

    // Navigation / Toolbar if enabled
    if (config.enableToolbar) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { webViewInstance?.goBack() },
            enabled = canGoBack
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }

          IconButton(
            onClick = { webViewInstance?.goForward() },
            enabled = canGoForward
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
          }

          IconButton(
            onClick = {
              isRefreshing = true
              webViewInstance?.reload()
            }
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reload")
          }

          IconButton(
            onClick = {
              webViewInstance?.let { wv ->
                loadContentIntoWebView(wv, config)
              }
            }
          ) {
            Icon(Icons.Default.Home, contentDescription = "Home")
          }

          IconButton(
            onClick = {
              val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, config.appTitle)
                putExtra(android.content.Intent.EXTRA_TEXT, "Shared from ${config.appTitle}")
              }
              context.startActivity(android.content.Intent.createChooser(shareIntent, "Share"))
            }
          ) {
            Icon(Icons.Default.Share, contentDescription = "Share")
          }
        }
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      webViewInstance?.apply {
        stopLoading()
        clearHistory()
        removeAllViews()
        destroy()
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
  context: Context,
  config: AppBuildConfig,
  onTitleChanged: (String) -> Unit,
  onProgressChanged: (Int) -> Unit,
  onUrlChanged: (String) -> Unit,
  onNavStateChanged: (Boolean, Boolean) -> Unit,
  onFilePickerRequested: (ValueCallback<Array<Uri>>) -> Unit,
  onConsoleLog: ((String) -> Unit)?
): WebView {
  return WebView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )

    // Feature Toggle: Disable long press & text copy if allowLongPressCopy is false
    if (!config.allowLongPressCopy) {
      isLongClickable = false
      setOnLongClickListener { true }
      isHapticFeedbackEnabled = false
    }

    settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      databaseEnabled = true
      loadWithOverviewMode = true
      useWideViewPort = true
      setSupportZoom(config.allowZoom)
      builtInZoomControls = config.allowZoom
      displayZoomControls = false
      allowFileAccess = true
      allowContentAccess = true
      mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
      cacheMode = WebSettings.LOAD_DEFAULT
      userAgentString = "${settings.userAgentString} HTMLToAPK/${config.versionName}"
    }

    // Attach AndroidBridge for native interaction
    addJavascriptInterface(AndroidBridge(context, config), "AndroidBridge")

    webChromeClient = object : WebChromeClient() {
      override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
      }

      override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let { onTitleChanged(it) }
      }

      override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
      ): Boolean {
        if (filePathCallback != null) {
          onFilePickerRequested(filePathCallback)
          return true
        }
        return false
      }

      override fun onPermissionRequest(request: PermissionRequest?) {
        // Auto-grant WebRTC permissions (Camera, Microphone) if enabled in config
        val grantedResources = mutableListOf<String>()
        request?.resources?.forEach { res ->
          if (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE && config.permissions.camera) {
            grantedResources.add(res)
          } else if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE && config.permissions.microphone) {
            grantedResources.add(res)
          }
        }
        if (grantedResources.isNotEmpty()) {
          request?.grant(grantedResources.toTypedArray())
        } else {
          request?.deny()
        }
      }

      override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
      ) {
        if (config.permissions.location) {
          callback?.invoke(origin, true, false)
        } else {
          callback?.invoke(origin, false, false)
        }
      }

      override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
          onConsoleLog?.invoke("[${it.messageLevel()}] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
        }
        return super.onConsoleMessage(consoleMessage)
      }
    }

    webViewClient = object : WebViewClient() {
      override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { onUrlChanged(it) }
        onNavStateChanged(canGoBack(), canGoForward())
      }

      override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onNavStateChanged(canGoBack(), canGoForward())

        // If allowLongPressCopy is disabled, inject user-select: none CSS & event cancelers
        if (!config.allowLongPressCopy) {
          val jsNoSelect = """
            (function() {
              var css = '* { -webkit-user-select: none !important; user-select: none !important; -webkit-touch-callout: none !important; }';
              var head = document.head || document.getElementsByTagName('head')[0];
              var style = document.createElement('style');
              style.type = 'text/css';
              style.appendChild(document.createTextNode(css));
              head.appendChild(style);
              document.addEventListener('contextmenu', function(e) { e.preventDefault(); return false; }, true);
              document.addEventListener('selectstart', function(e) { e.preventDefault(); return false; }, true);
            })();
          """.trimIndent()
          view?.evaluateJavascript(jsNoSelect, null)
        }
      }

      override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file:///")) {
          return false
        }
        return try {
          val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
          context.startActivity(intent)
          true
        } catch (_: Exception) {
          true
        }
      }
    }
  }
}

private fun loadContentIntoWebView(webView: WebView, config: AppBuildConfig) {
  var content = config.rawHtmlContent
  if (config.customCss.isNotBlank()) {
    content = content.replace("</head>", "<style>\n${config.customCss}\n</style>\n</head>")
  }
  if (config.customJs.isNotBlank()) {
    content = content.replace("</body>", "<script>\n${config.customJs}\n</script>\n</body>")
  }
  webView.loadDataWithBaseURL("https://appassets.androidcache.net/", content, "text/html", "UTF-8", null)
}
