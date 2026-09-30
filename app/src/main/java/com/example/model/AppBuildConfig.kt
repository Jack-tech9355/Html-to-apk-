package com.example.model

import org.json.JSONArray
import org.json.JSONObject

enum class SourceType {
  RAW_HTML,
  FILE_HTML,
  ZIP_BUNDLE
}

enum class LogLevel {
  INFO,
  STAGE,
  SUCCESS,
  WARNING,
  ERROR
}

data class BuildLogEntry(
  val timestamp: Long = System.currentTimeMillis(),
  val message: String,
  val level: LogLevel = LogLevel.INFO
)

data class AppPermissions(
  val internet: Boolean = true,
  val camera: Boolean = false,
  val storage: Boolean = false,
  val location: Boolean = false,
  val microphone: Boolean = false
) {
  fun toPermissionList(): List<String> {
    val list = mutableListOf<String>()
    if (internet) {
      list.add("android.permission.INTERNET")
      list.add("android.permission.ACCESS_NETWORK_STATE")
    }
    if (camera) {
      list.add("android.permission.CAMERA")
    }
    if (storage) {
      list.add("android.permission.READ_EXTERNAL_STORAGE")
      list.add("android.permission.WRITE_EXTERNAL_STORAGE")
    }
    if (location) {
      list.add("android.permission.ACCESS_FINE_LOCATION")
      list.add("android.permission.ACCESS_COARSE_LOCATION")
    }
    if (microphone) {
      list.add("android.permission.RECORD_AUDIO")
    }
    return list
  }

  fun toJson(): JSONObject {
    return JSONObject().apply {
      put("internet", internet)
      put("camera", camera)
      put("storage", storage)
      put("location", location)
      put("microphone", microphone)
    }
  }

  companion object {
    fun fromJson(json: JSONObject): AppPermissions {
      return AppPermissions(
        internet = json.optBoolean("internet", true),
        camera = json.optBoolean("camera", false),
        storage = json.optBoolean("storage", false),
        location = json.optBoolean("location", false),
        microphone = json.optBoolean("microphone", false)
      )
    }
  }
}

data class AppBuildConfig(
  val id: String = java.util.UUID.randomUUID().toString(),
  val appTitle: String = "My Web App",
  val packageName: String = "com.user.htmlapp",
  val versionCode: Int = 1,
  val versionName: String = "1.0.0",
  val enableTitlebar: Boolean = false,
  val enableToolbar: Boolean = true,
  val enableSwipeRefresh: Boolean = true,
  val allowLongPressCopy: Boolean = true,
  val allowZoom: Boolean = false,
  val permissions: AppPermissions = AppPermissions(),
  val iconUri: String? = null,
  val splashUri: String? = null,
  val sourceType: SourceType = SourceType.RAW_HTML,
  val rawHtmlContent: String = DEFAULT_HTML_PRESET,
  val customCss: String = "",
  val customJs: String = "",
  val orientation: String = "PORTRAIT",
  val themeColor: String = "#4F46E5",
  val createdAt: Long = System.currentTimeMillis()
) {
  fun toJson(): String {
    val root = JSONObject().apply {
      put("id", id)
      put("appTitle", appTitle)
      put("packageName", packageName)
      put("versionCode", versionCode)
      put("versionName", versionName)
      put("enableTitlebar", enableTitlebar)
      put("enableToolbar", enableToolbar)
      put("enableSwipeRefresh", enableSwipeRefresh)
      put("allowLongPressCopy", allowLongPressCopy)
      put("allowZoom", allowZoom)
      put("permissions", permissions.toJson())
      put("iconUri", iconUri ?: "")
      put("splashUri", splashUri ?: "")
      put("sourceType", sourceType.name)
      put("rawHtmlContent", rawHtmlContent)
      put("customCss", customCss)
      put("customJs", customJs)
      put("orientation", orientation)
      put("themeColor", themeColor)
      put("createdAt", createdAt)

      val permList = JSONArray()
      permissions.toPermissionList().forEach { permList.put(it) }
      put("manifestPermissions", permList)
    }
    return root.toString(2)
  }

  companion object {
    fun fromJson(jsonStr: String): AppBuildConfig {
      val json = JSONObject(jsonStr)
      return AppBuildConfig(
        id = json.optString("id", java.util.UUID.randomUUID().toString()),
        appTitle = json.optString("appTitle", "My Web App"),
        packageName = json.optString("packageName", "com.user.htmlapp"),
        versionCode = json.optInt("versionCode", 1),
        versionName = json.optString("versionName", "1.0.0"),
        enableTitlebar = json.optBoolean("enableTitlebar", false),
        enableToolbar = json.optBoolean("enableToolbar", true),
        enableSwipeRefresh = json.optBoolean("enableSwipeRefresh", true),
        allowLongPressCopy = json.optBoolean("allowLongPressCopy", true),
        allowZoom = json.optBoolean("allowZoom", false),
        permissions = json.optJSONObject("permissions")?.let { AppPermissions.fromJson(it) } ?: AppPermissions(),
        iconUri = json.optString("iconUri").takeIf { it.isNotBlank() },
        splashUri = json.optString("splashUri").takeIf { it.isNotBlank() },
        sourceType = try {
          SourceType.valueOf(json.optString("sourceType", SourceType.RAW_HTML.name))
        } catch (_: Exception) {
          SourceType.RAW_HTML
        },
        rawHtmlContent = json.optString("rawHtmlContent", DEFAULT_HTML_PRESET),
        customCss = json.optString("customCss", ""),
        customJs = json.optString("customJs", ""),
        orientation = json.optString("orientation", "PORTRAIT"),
        themeColor = json.optString("themeColor", "#4F46E5"),
        createdAt = json.optLong("createdAt", System.currentTimeMillis())
      )
    }

    val DEFAULT_HTML_PRESET = """
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <title>HTML5 Native App</title>
        <style>
          :root { --primary: #6366f1; --bg: #0b0f19; --card: #151c2e; --text: #f1f5f9; --sub: #94a3b8; }
          * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
          body { font-family: -apple-system, system-ui, sans-serif; background: var(--bg); color: var(--text); padding: 24px 16px; min-height: 100vh; display: flex; flex-direction: column; }
          .header { text-align: center; margin-top: 16px; margin-bottom: 24px; }
          .badge { display: inline-block; background: rgba(99,102,241,0.2); border: 1px solid rgba(99,102,241,0.4); color: #818cf8; font-size: 11px; font-weight: 700; padding: 4px 12px; border-radius: 999px; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 12px; }
          h1 { font-size: 26px; font-weight: 800; background: linear-gradient(135deg, #fff 0%, #cbd5e1 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 6px; }
          p.sub { font-size: 14px; color: var(--sub); }
          .grid { display: flex; flex-direction: column; gap: 14px; flex: 1; }
          .card { background: var(--card); border: 1px solid rgba(255,255,255,0.06); border-radius: 16px; padding: 18px; box-shadow: 0 4px 20px rgba(0,0,0,0.3); }
          .card h3 { font-size: 16px; font-weight: 700; margin-bottom: 6px; display: flex; align-items: center; gap: 8px; }
          .card p { font-size: 13px; color: var(--sub); line-height: 1.5; margin-bottom: 12px; }
          .btn { display: inline-flex; align-items: center; justify-content: center; width: 100%; padding: 12px; border-radius: 10px; font-weight: 600; font-size: 14px; border: none; cursor: pointer; background: var(--primary); color: white; transition: opacity 0.2s; }
          .btn:active { opacity: 0.85; }
          .counter-val { font-size: 32px; font-weight: 800; color: #38bdf8; text-align: center; margin: 12px 0; }
          .row { display: flex; gap: 10px; }
          .row .btn { flex: 1; }
          footer { text-align: center; padding: 24px 0 8px; font-size: 11px; color: #475569; }
        </style>
      </head>
      <body>
        <div class="header">
          <div class="badge">Offline APK Ready</div>
          <h1>My Web Application</h1>
          <p class="sub">Compiled with HTML to APK Pre-Compiled Template Engine</p>
        </div>
        <div class="grid">
          <div class="card">
            <h3>⚡ Native Bridge Test</h3>
            <p>Communicate with Android APIs like native toasts and vibration.</p>
            <button class="btn" onclick="testNativeBridge()">Trigger Native Toast</button>
          </div>
          <div class="card">
            <h3>🔢 Interactive Counter</h3>
            <p>Fast reactive DOM state running on local hardware-accelerated WebView.</p>
            <div class="counter-val" id="counter">0</div>
            <div class="row">
              <button class="btn" style="background: #334155" onclick="changeCounter(-1)">- Decrement</button>
              <button class="btn" onclick="changeCounter(1)">+ Increment</button>
            </div>
          </div>
          <div class="card">
            <h3>🎨 Dynamic Theme</h3>
            <p>Custom CSS, WebGL, animations, and responsive layouts are supported natively.</p>
            <button class="btn" style="background: #0284c7" onclick="randomColor()">Randomize Background</button>
          </div>
        </div>
        <footer>Built with HTML to APK Builder &copy; 2026</footer>
        <script>
          let count = 0;
          function changeCounter(delta) {
            count += delta;
            document.getElementById('counter').innerText = count;
            if (window.AndroidBridge && typeof window.AndroidBridge.vibrate === 'function') {
              window.AndroidBridge.vibrate(20);
            }
          }
          function testNativeBridge() {
            if (window.AndroidBridge && typeof window.AndroidBridge.showToast === 'function') {
              window.AndroidBridge.showToast("⚡ Native Android Toast from HTML App! (Count: " + count + ")");
              window.AndroidBridge.vibrate(50);
            } else {
              alert("Hello from HTML App! Android bridge will fire natively inside the APK.");
            }
          }
          function randomColor() {
            const hues = [210, 240, 280, 320, 160];
            const hue = hues[Math.floor(Math.random() * hues.length)];
            document.body.style.background = 'radial-gradient(circle at top, hsl(' + hue + ', 50%, 15%), #0b0f19 80%)';
          }
        </script>
      </body>
      </html>
    """.trimIndent()

    val PRESET_RETRO_GAME = """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <title>Retro Tap Canvas</title>
        <style>
          * { margin: 0; padding: 0; box-sizing: border-box; }
          body { background: #000; color: #fff; font-family: monospace; overflow: hidden; touch-action: none; text-align: center; }
          #scoreBoard { position: absolute; top: 12px; width: 100%; font-size: 20px; font-weight: bold; color: #4ade80; text-shadow: 0 0 10px rgba(74,222,128,0.8); z-index: 10; pointer-events: none; }
          canvas { display: block; width: 100vw; height: 100vh; }
        </style>
      </head>
      <body>
        <div id="scoreBoard">SCORE: <span id="score">0</span> | BEST: <span id="best">0</span></div>
        <canvas id="c"></canvas>
        <script>
          const canvas = document.getElementById('c');
          const ctx = canvas.getContext('2d');
          let width = canvas.width = window.innerWidth;
          let height = canvas.height = window.innerHeight;
          let score = 0, best = 0;
          let particles = [];
          let targets = [];
          
          function spawnTarget() {
            targets.push({
              x: Math.random() * (width - 80) + 40,
              y: Math.random() * (height - 180) + 90,
              r: 28,
              hue: Math.floor(Math.random() * 360),
              life: 180
            });
          }

          function loop() {
            ctx.fillStyle = 'rgba(10, 10, 20, 0.2)';
            ctx.fillRect(0, 0, width, height);

            if (Math.random() < 0.04 && targets.length < 5) spawnTarget();

            for (let i = targets.length - 1; i >= 0; i--) {
              let t = targets[i];
              t.life--;
              ctx.beginPath();
              ctx.arc(t.x, t.y, t.r, 0, Math.PI * 2);
              ctx.fillStyle = 'hsl(' + t.hue + ', 90%, 60%)';
              ctx.shadowColor = ctx.fillStyle;
              ctx.shadowBlur = 15;
              ctx.fill();
              ctx.shadowBlur = 0;
              if (t.life <= 0) targets.splice(i, 1);
            }

            for (let i = particles.length - 1; i >= 0; i--) {
              let p = particles[i];
              p.x += p.vx; p.y += p.vy; p.life--;
              ctx.beginPath();
              ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
              ctx.fillStyle = p.color;
              ctx.fill();
              if (p.life <= 0) particles.splice(i, 1);
            }
            requestAnimationFrame(loop);
          }

          function hit(x, y) {
            let found = false;
            for (let i = targets.length - 1; i >= 0; i--) {
              let t = targets[i];
              let dist = Math.hypot(t.x - x, t.y - y);
              if (dist <= t.r + 15) {
                found = true;
                score += 10;
                if (score > best) best = score;
                document.getElementById('score').innerText = score;
                document.getElementById('best').innerText = best;
                for (let k = 0; k < 16; k++) {
                  let angle = Math.random() * Math.PI * 2;
                  let speed = Math.random() * 6 + 2;
                  particles.push({
                    x: t.x, y: t.y,
                    vx: Math.cos(angle) * speed,
                    vy: Math.sin(angle) * speed,
                    r: Math.random() * 3 + 2,
                    color: 'hsl(' + t.hue + ', 100%, 70%)',
                    life: 25
                  });
                }
                targets.splice(i, 1);
                if (window.AndroidBridge && typeof window.AndroidBridge.vibrate === 'function') {
                  window.AndroidBridge.vibrate(25);
                }
                break;
              }
            }
          }

          window.addEventListener('pointerdown', (e) => hit(e.clientX, e.clientY));
          window.addEventListener('resize', () => { width = canvas.width = window.innerWidth; height = canvas.height = window.innerHeight; });
          loop();
        </script>
      </body>
      </html>
    """.trimIndent()

    val PRESET_CYBER_DASHBOARD = """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <title>Cyber Telemetry</title>
        <style>
          * { margin:0; padding:0; box-sizing:border-box; }
          body { background:#030712; color:#38bdf8; font-family:'Courier New', monospace; padding:16px; min-height:100vh; }
          .hud-header { border-bottom:1px solid #1e293b; padding-bottom:12px; margin-bottom:16px; display:flex; justify-content:space-between; align-items:center; }
          .hud-title { font-size:18px; font-weight:bold; letter-spacing:2px; color:#f8fafc; }
          .hud-status { color:#22c55e; font-size:12px; }
          .stats-grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-bottom:16px; }
          .stat-box { background:#0f172a; border:1px solid #1e3a8a; border-radius:8px; padding:12px; }
          .stat-label { font-size:10px; color:#94a3b8; text-transform:uppercase; }
          .stat-val { font-size:22px; font-weight:bold; color:#60a5fa; margin-top:4px; }
          .log-box { background:#020617; border:1px solid #334155; border-radius:8px; padding:12px; height:180px; overflow-y:auto; font-size:11px; color:#a5f3fc; line-height:1.6; }
          .ctrl-btn { width:100%; margin-top:16px; padding:12px; background:#2563eb; color:white; border:none; border-radius:8px; font-weight:bold; font-family:inherit; cursor:pointer; }
          .ctrl-btn:active { background:#1d4ed8; }
        </style>
      </head>
      <body>
        <div class="hud-header">
          <div class="hud-title">⚡ SYS_NODE // V3</div>
          <div class="hud-status">● LIVE_FEED</div>
        </div>
        <div class="stats-grid">
          <div class="stat-box">
            <div class="stat-label">CPU Load</div>
            <div class="stat-val" id="cpuVal">24.8%</div>
          </div>
          <div class="stat-box">
            <div class="stat-label">Memory</div>
            <div class="stat-val" id="memVal">1.4 GB</div>
          </div>
          <div class="stat-box">
            <div class="stat-label">Throughput</div>
            <div class="stat-val" id="netVal">48 MB/s</div>
          </div>
          <div class="stat-box">
            <div class="stat-label">Latency</div>
            <div class="stat-val" id="pingVal">12 ms</div>
          </div>
        </div>
        <div class="stat-label" style="margin-bottom:6px;">Terminal Stream:</div>
        <div class="log-box" id="termLogs">
          <div>[00:00:01] System boot verified.</div>
          <div>[00:00:02] Local WebKit container attached.</div>
          <div>[00:00:03] Hardware acceleration: ACTIVE.</div>
        </div>
        <button class="ctrl-btn" onclick="sendSignal()">SEND NATIVE INTERRUPT</button>
        <script>
          function sendSignal() {
            const logs = document.getElementById('termLogs');
            const d = new Date().toTimeString().split(' ')[0];
            const div = document.createElement('div');
            div.textContent = '[' + d + '] Native pulse triggered.';
            logs.appendChild(div);
            logs.scrollTop = logs.scrollHeight;
            if (window.AndroidBridge) {
              window.AndroidBridge.showToast("Telemetry signal transmitted!");
              window.AndroidBridge.vibrate(30);
            }
          }
          setInterval(() => {
            document.getElementById('cpuVal').textContent = (Math.random() * 20 + 20).toFixed(1) + '%';
            document.getElementById('netVal').textContent = (Math.random() * 30 + 35).toFixed(0) + ' MB/s';
            document.getElementById('pingVal').textContent = (Math.random() * 8 + 8).toFixed(0) + ' ms';
          }, 1500);
        </script>
      </body>
      </html>
    """.trimIndent()
  }
}
