package com.ezio.unishare.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
// Ensure AppTypography is imported if not automatically resolved, or remove unused text style imports
// import androidx.compose.ui.text.TextStyle
// import androidx.compose.ui.text.font.FontFamily
// import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// The AppTypography definition that was here has been removed.
// It should be defined in your ui/theme/Type.kt file.

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.Black, // Example: ensure contrast, adjust as needed
    secondary = SecondaryLight,
    onSecondary = Color.Black, // Adjust as needed
    tertiary = TertiaryLight,
    onTertiary = Color.Black, // Adjust as needed
    background = BackgroundLight,
    onBackground = Color.Black, // Adjust as needed
    surface = SurfaceLight,
    onSurface = Color.Black, // Adjust as needed
    error = ErrorLight,
    onError = Color.White // Adjust as needed
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color.White, // Adjust as needed
    secondary = SecondaryDark,
    onSecondary = Color.White, // Adjust as needed
    tertiary = TertiaryDark,
    onTertiary = Color.White, // Adjust as needed
    background = BackgroundDark,
    onBackground = Color.White, // Adjust as needed
    surface = SurfaceDark,
    onSurface = Color.White, // Adjust as needed
    error = ErrorDark,
    onError = Color.Black // Adjust as needed
)

@Composable
fun PeerRentTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Dynamic color on Android 12+
    content: @Composable() () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            // Make sure window is not null before trying to use it
            window?.let { w ->
                w.statusBarColor = colorScheme.primary.toArgb() // Or any color you want
                WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // This will use AppTypography from Type.kt
        content = content
    )
}
