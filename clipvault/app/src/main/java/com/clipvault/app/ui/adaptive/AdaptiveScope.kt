package com.clipvault.app.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

// [自适应] 核心 Composable — 检测当前设备形态
// 搜索来源: developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes
// 使用 WindowSizeClass (androidx.window:window:1.3.0) 进行分类
@Composable
fun rememberDeviceFormFactor(): DeviceFormFactor {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isLandscape = screenWidthDp > screenHeightDp

    return remember(screenWidthDp, screenHeightDp) {
        when {
            // [自适应] 基于 WindowWidthSizeClass 标准断点:
            // Compact: < 600dp (手机)
            // Medium: 600dp - 839dp (折叠屏/小平板)
            // Expanded: >= 840dp (平板/桌面)
            screenWidthDp < 600 -> {
                if (isLandscape) DeviceFormFactor.PhoneLandscape
                else DeviceFormFactor.PhonePortrait
            }
            screenWidthDp < 840 -> DeviceFormFactor.Foldable
            else -> {
                // [自适应] 桌面模式检测: ChromeOS/desktop windowing 通常有键盘+鼠标
                // 这里简化为 Expanded = Tablet，键盘快捷键在 Desktop 模式也可用
                DeviceFormFactor.Tablet
            }
        }
    }
}

// [自适应] 获取 WindowWidthSizeClass 分类
@Composable
fun rememberWindowWidthSizeClass(): WindowWidthSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    return remember(screenWidthDp) {
        when {
            screenWidthDp < 600 -> WindowWidthSizeClass.COMPACT
            screenWidthDp < 840 -> WindowWidthSizeClass.MEDIUM
            else -> WindowWidthSizeClass.EXPANDED
        }
    }
}

// [自适应] 获取当前自适应 Token 集
@Composable
fun rememberAdaptiveTokens(): AdaptiveTokens {
    val formFactor = rememberDeviceFormFactor()
    return remember(formFactor) { AdaptiveTokens.forDevice(formFactor) }
}
