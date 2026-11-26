package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class VynzoThemeMode(val wireValue: String, val label: String) {
  SYSTEM("system", "System Default"),
  DARK("dark", "Obsidian Dark"),
  LIGHT("light", "Alabaster Light");

  companion object {
    fun fromWire(value: String?): VynzoThemeMode =
      entries.firstOrNull { it.wireValue == value } ?: DARK
  }
}

@Immutable
data class VynzoExtraColors(
  val elevatedSurface: Color,
  val hairlineBorder: Color,
  val mutedText: Color,
  val accentCyan: Color,
  val accentRose: Color,
  val accentCoral: Color,
  val successEmerald: Color,
  val warningAmber: Color,
  val isDark: Boolean
)

val LocalVynzoColors = staticCompositionLocalOf {
  VynzoExtraColors(
    elevatedSurface = DarkSurfaceElevated,
    hairlineBorder = DarkBorder,
    mutedText = DarkOnSurfaceMuted,
    accentCyan = VynzoCyan,
    accentRose = VynzoRose,
    accentCoral = VynzoCoral,
    successEmerald = VynzoEmerald,
    warningAmber = VynzoAmber,
    isDark = true
  )
}

private val VynzoDarkColorScheme = darkColorScheme(
  primary = VynzoIndigo,
  onPrimary = Color.White,
  primaryContainer = Color(0xFF282454),
  onPrimaryContainer = Color(0xFFE0E0FF),
  secondary = VynzoRose,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF3E1935),
  onSecondaryContainer = Color(0xFFFFD8EC),
  tertiary = VynzoCoral,
  onTertiary = Color.White,
  background = DarkBackground,
  onBackground = DarkOnSurface,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceMuted,
  outline = DarkBorder,
  error = VynzoErrorRed,
  onError = Color.White
)

private val VynzoLightColorScheme = lightColorScheme(
  primary = VynzoIndigo,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE8E7FF),
  onPrimaryContainer = Color(0xFF1B1957),
  secondary = VynzoRose,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFFFE4F0),
  onSecondaryContainer = Color(0xFF4D0B2E),
  tertiary = VynzoCoral,
  onTertiary = Color.White,
  background = LightBackground,
  onBackground = LightOnSurface,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceMuted,
  outline = LightBorder,
  error = VynzoErrorRed,
  onError = Color.White
)

val VynzoShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun VynzoTheme(
  themeMode: VynzoThemeMode = VynzoThemeMode.DARK,
  content: @Composable () -> Unit
) {
  val useDark = when (themeMode) {
    VynzoThemeMode.SYSTEM -> isSystemInDarkTheme()
    VynzoThemeMode.DARK -> true
    VynzoThemeMode.LIGHT -> false
  }

  val colorScheme = if (useDark) VynzoDarkColorScheme else VynzoLightColorScheme
  val extraColors = if (useDark) {
    VynzoExtraColors(
      elevatedSurface = DarkSurfaceElevated,
      hairlineBorder = DarkBorder,
      mutedText = DarkOnSurfaceMuted,
      accentCyan = VynzoCyan,
      accentRose = VynzoRose,
      accentCoral = VynzoCoral,
      successEmerald = VynzoEmerald,
      warningAmber = VynzoAmber,
      isDark = true
    )
  } else {
    VynzoExtraColors(
      elevatedSurface = LightSurfaceElevated,
      hairlineBorder = LightBorder,
      mutedText = LightOnSurfaceMuted,
      accentCyan = VynzoCyan,
      accentRose = VynzoRose,
      accentCoral = VynzoCoral,
      successEmerald = VynzoEmerald,
      warningAmber = VynzoAmber,
      isDark = false
    )
  }

  CompositionLocalProvider(LocalVynzoColors provides extraColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = VynzoTypography,
      shapes = VynzoShapes,
      content = content
    )
  }
}
