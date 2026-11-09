package com.handleliste.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Green = Color(0xFF1F7A4D)
val Leaf = Color(0xFF145C39)     // deep green (cards, update dialog)
val Mint = Color(0xFFDFF3E7)
val Sun = Color(0xFFFFD54F)      // deal yellow
val Tomato = Color(0xFFB3361F)   // sale tags (dark enough to read on white: 6:1)
val Ink = Color(0xFF14201A)
val Paper = Color(0xFFF4F7F2)

private val Light = lightColorScheme(
    primary = Green, onPrimary = Color.White,
    primaryContainer = Mint, onPrimaryContainer = Leaf,
    secondary = Tomato, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE9E4), onSecondaryContainer = Color(0xFF7A1F12),
    tertiary = Color(0xFF8A6D00), tertiaryContainer = Color(0xFFFFF3C4), onTertiaryContainer = Color(0xFF4A3A00),
    background = Paper, onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFE8EEE8), onSurfaceVariant = Color(0xFF4F5E55),
    surfaceContainer = Color(0xFFEFF4EE), surfaceContainerHigh = Color(0xFFE9EFE8),
    outline = Color(0xFFB9C7BE), outlineVariant = Color(0xFFD6E0D9),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF6FD69C), onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF1E4D33), onPrimaryContainer = Color(0xFFB9F2CE),
    secondary = Color(0xFFFF9C88), onSecondary = Color(0xFF5C1306),
    secondaryContainer = Color(0xFF5C2216), onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = Sun, tertiaryContainer = Color(0xFF4A3A00), onTertiaryContainer = Color(0xFFFFE9A0),
    background = Color(0xFF0F1512), onBackground = Color(0xFFE2E9E4),
    surface = Color(0xFF161D19), onSurface = Color(0xFFE2E9E4),
    surfaceVariant = Color(0xFF26302A), onSurfaceVariant = Color(0xFFB4C2B9),
    surfaceContainer = Color(0xFF1B231E), surfaceContainerHigh = Color(0xFF222B25),
    outline = Color(0xFF5E6B63), outlineVariant = Color(0xFF39443D),
)

@Composable
fun HandlelisteTheme(content: @Composable () -> Unit) {
    val t = Typography()
    val scheme = if (isSystemInDarkTheme()) Dark else Light
    MaterialTheme(
        colorScheme = scheme,
        typography = t.copy(
            headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
            titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
            titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
        ),
    ) {
        // Text drawn straight on the app background (full-screen panels outside the Scaffold) gets
        // the readable text colour for light or dark mode, instead of plain black.
        CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
    }
}
