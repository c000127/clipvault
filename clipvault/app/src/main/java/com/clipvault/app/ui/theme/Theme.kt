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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
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

// [动效] 全局动效 Token 体系 — 所有动画必须引用此对象中的规范，禁止硬编码数值
object ClipVaultMotion {
    // [动效] 时长 Token（毫秒）
    const val Instant = 100       // 微反馈：ripple、选中态高亮
    const val Quick = 200         // 轻量过渡：chip 出现、badge 闪烁、dismiss 动画
    const val Standard = 300      // 标准页面内动画：内容展开、状态切换
    const val Deliberate = 500    // 强调型动画：hero 进场、空状态出现、大容器展开

    // [动效] 缓动 Token
    val DefaultEasing = androidx.compose.animation.core.FastOutSlowInEasing
    val EmphasizedEasing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val DecelerateEasing = androidx.compose.animation.core.CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val AccelerateEasing = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)
    val LinearEasing = androidx.compose.animation.core.LinearEasing

    // [动效] Spring Token（Float 类型，适用于 animateFloatAsState / Animatable）
    val Snappy = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessHigh
    )
    val Responsive = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
    )
    val Bouncy = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
    )
    // [动效] scaleIn/Out 弹簧（轻弹）
    val ScaleIn = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
    )
    // [动效] expand/shrink 容器弹簧
    val GentleExpand = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
    )

    // [动效] 页面转场专用 Spring（IntOffset 类型，供 NavHost 使用）
    val PageSlide = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
    )
    // [动效] expand/shrink 容器弹簧（IntSize 类型，供 expandVertically/shrinkVertically 使用）
    val ExpandSpring = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
    )
    // [动效] 容器变换专用 Spring（Bounds transform，参考 Jetsnack spatialExpressiveSpring）
    val BoundsTransform = androidx.compose.animation.core.spring<Rect>(
        dampingRatio = 0.8f,
        stiffness = 380f
    )
    // [动效] 空间弹簧（bounds transform, position changes — 参考 Jetsnack）
    val SpatialExpressiveSpring = androidx.compose.animation.core.spring<Rect>(
        dampingRatio = 0.8f,
        stiffness = 380f
    )
    // [动效] 非空间弹簧（fadeIn/Out, opacity — 参考 Jetsnack nonSpatialExpressiveSpring）
    val NonSpatialExpressiveSpring = androidx.compose.animation.core.spring<Float>(
        dampingRatio = 1f,
        stiffness = 1600f
    )
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

/**
 * Compose 1.11 Preview Wrapper to simplify preview declarations.
 */
@Composable
fun PreviewWrapper(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    ClipVaultTheme(darkTheme = darkTheme, dynamicColor = false) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}
