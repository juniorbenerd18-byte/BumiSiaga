package com.example.siagabumi.ui.theme

import androidx.compose.ui.graphics.Color

val Teal80 = Color(0xFFA7F3D0)
val TealGrey80 = Color(0xFFCCE8E0)
val Amber80 = Color(0xFFFDE68A)

val Teal40 = Color(0xFF0F766E)
val TealGrey40 = Color(0xFF335C57)
val Amber40 = Color(0xFFB45309)

// Magnitude Colors
val MagGreen = Color(0xFF2E7D32)   // < 4.0
val MagYellow = Color(0xFFF57F17)  // 4.0 - 4.9
val MagOrange = Color(0xFFE65100)  // 5.0 - 5.9
val MagRed = Color(0xFFC62828)     // >= 6.0

fun getMagnitudeColor(magnitude: Double): Color = when {
    magnitude < 4.0 -> MagGreen
    magnitude < 5.0 -> MagYellow
    magnitude < 6.0 -> MagOrange
    else -> MagRed
}
