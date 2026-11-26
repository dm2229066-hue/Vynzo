package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// VYNZO Brand Signature Palette — Prism Indigo & Electric Coral
val VynzoIndigo = Color(0xFF6366F1)
val VynzoViolet = Color(0xFF8B5CF6)
val VynzoRose = Color(0xFFEC4899)
val VynzoCoral = Color(0xFFFF6B4A)
val VynzoCyan = Color(0xFF06B6D4)
val VynzoEmerald = Color(0xFF10B981)
val VynzoAmber = Color(0xFFF59E0B)
val VynzoErrorRed = Color(0xFFEF4444)

// Dark Mode Surfaces (Deep Obsidian)
val DarkBackground = Color(0xFF0B0A12)
val DarkSurface = Color(0xFF13111F)
val DarkSurfaceVariant = Color(0xFF1C192E)
val DarkSurfaceElevated = Color(0xFF242038)
val DarkBorder = Color(0xFF2E2A45)
val DarkOnSurface = Color(0xFFF5F3FF)
val DarkOnSurfaceMuted = Color(0xFFA7A3C2)

// Light Mode Surfaces (Crisp Alabaster)
val LightBackground = Color(0xFFFAF9FD)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0EEF8)
val LightSurfaceElevated = Color(0xFFE7E4F3)
val LightBorder = Color(0xFFDAD6EB)
val LightOnSurface = Color(0xFF12101F)
val LightOnSurfaceMuted = Color(0xFF5F5B7A)

val VynzoBrandGradient = Brush.linearGradient(
  colors = listOf(VynzoIndigo, VynzoRose, VynzoCoral)
)

val VynzoStoryRingGradient = Brush.sweepGradient(
  colors = listOf(VynzoCoral, VynzoRose, VynzoViolet, VynzoCyan, VynzoCoral)
)

val VynzoSoftGlowGradient = Brush.horizontalGradient(
  colors = listOf(VynzoIndigo.copy(alpha = 0.18f), VynzoRose.copy(alpha = 0.14f))
)
