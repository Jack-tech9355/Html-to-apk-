package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = IndigoPrimary,
  onPrimary = IndigoOnPrimary,
  primaryContainer = IndigoContainer,
  onPrimaryContainer = OnIndigoContainer,
  secondary = CyanSecondary,
  onSecondary = OnCyanSecondary,
  secondaryContainer = CyanContainer,
  onSecondaryContainer = OnCyanContainer,
  background = DarkBackground,
  onBackground = TextPrimaryDark,
  surface = DarkSurface,
  onSurface = TextPrimaryDark,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = TextSecondaryDark,
  surfaceContainer = Color(0xFF131C31),
  surfaceContainerHigh = Color(0xFF1B253F),
  surfaceContainerHighest = DarkSurfaceHighest,
  error = AccentRed,
  errorContainer = Color(0xFF450A0A),
  onError = Color.White,
  onErrorContainer = Color(0xFFFCA5A5)
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF4F46E5),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFEEF2FF),
  onPrimaryContainer = Color(0xFF312E81),
  secondary = Color(0xFF0284C7),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE0F2FE),
  onSecondaryContainer = Color(0xFF075985),
  background = Color(0xFFF8FAFC),
  onBackground = Color(0xFF0F172A),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = Color(0xFF475569),
  surfaceContainer = Color(0xFFF8FAFC),
  surfaceContainerHigh = Color(0xFFF1F5F9),
  surfaceContainerHighest = Color(0xFFE2E8F0),
  error = Color(0xFFDC2626),
  errorContainer = Color(0xFFFEE2E2),
  onError = Color.White,
  onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our handcrafted developer theme for distinct branding
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
