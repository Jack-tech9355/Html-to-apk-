// Native Android Bridge Handler
function triggerNativeToast() {
  if (window.AndroidBridge && typeof window.AndroidBridge.showToast === 'function') {
    window.AndroidBridge.showToast("⚡ Native Toast triggered from HTML Web App!");
    if (typeof window.AndroidBridge.vibrate === 'function') {
      window.AndroidBridge.vibrate(50);
    }
  } else {
    alert("Triggered from Web (AndroidBridge ready in native environment)");
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const configElement = document.getElementById('configText');
  if (window.AndroidBridge && typeof window.AndroidBridge.getAppConfig === 'function') {
    try {
      const cfg = JSON.parse(window.AndroidBridge.getAppConfig());
      if (configElement) {
        configElement.textContent = `Running ${cfg.appTitle || 'Web App'} (${cfg.packageName || 'native'})`;
      }
    } catch (e) {
      console.error(e);
    }
  }
});
