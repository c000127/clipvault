package com.clipvault.app.ui.adaptive

// [自适应] 设备形态枚举 — 基于 WindowSizeClass + 设备特征
// 搜索来源: developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes
enum class DeviceFormFactor {
    PhonePortrait,    // sw < 600dp, 竖屏
    PhoneLandscape,   // sw < 600dp, 横屏
    Foldable,         // 600dp ≤ sw < 840dp
    Tablet,           // sw ≥ 840dp
    Desktop;          // 键盘+鼠标 (ChromeOS, desktop windowing)

    val isLargeScreen: Boolean
        get() = this == Foldable || this == Tablet || this == Desktop

    val isPhone: Boolean
        get() = this == PhonePortrait || this == PhoneLandscape
}
