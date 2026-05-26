package com.clipvault.app.ui.theme

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Expressive shape definitions
val BentoAsymmetricCardShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 16.dp,
    bottomEnd = 28.dp,
    bottomStart = 16.dp
)

val ExpressiveBottomSheetShape = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 16.dp,
    bottomEnd = 0.dp,
    bottomStart = 0.dp
)

val PillShape = RoundedCornerShape(50.dp)

// Standard Motion Schemes
object ClipVaultMotion {
    // 弹性弹簧：用于卡片按压、展开
    val BouncySpring = androidx.compose.animation.core.spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 400f
    )
    
    // 快速缓动：用于淡入淡出、平移
    val FastOutSlowIn = androidx.compose.animation.core.FastOutSlowInEasing
    
    // 标准动画时长
    val ShortDuration = 200
    val MediumDuration = 400
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0D0D0D),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1E1E1E)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBF7), // warm light mode background
    surface = Color(0xFFFFFBF7),
    surfaceContainerLow = Color(0xFFF7F2EB),
    surfaceContainer = Color(0xFFEEE8DF),
    surfaceContainerHigh = Color(0xFFE5DFD4)
)

// High Contrast schemes for WCAG AA compliance
private val HighContrastLightColorScheme = lightColorScheme(
    primary = Color(0xFF381E72), // Darker purple for high contrast
    secondary = Color(0xFF332D41),
    tertiary = Color(0xFF492532),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0EAE1),
    surfaceContainer = Color(0xFFE2DBD0),
    surfaceContainerHigh = Color(0xFFD3CBBF)
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF3EDFF), // Lighter purple for high contrast against black
    secondary = Color(0xFFE8E0F5),
    tertiary = Color(0xFFFFD9E2),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF000000),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF1A1A1A)
)

val ClipVaultShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ClipVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Detect system contrast level on API 34+
    val contrast = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            uiModeManager?.contrast ?: 0f
        } else {
            0f
        }
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Android 12+ dynamic colors (system automatically adjusts contrast on API 34+)
            if (darkTheme) {
                dynamicDarkColorScheme(context).copy(
                    background = Color(0xFF000000), // Enforce pure OLED black
                    surface = Color(0xFF000000)
                )
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> {
            if (contrast >= 0.5f) HighContrastDarkColorScheme else DarkColorScheme
        }
        else -> {
            if (contrast >= 0.5f) HighContrastLightColorScheme else LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ClipVaultShapes,
        content = content
    )
}
