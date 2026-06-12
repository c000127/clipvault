package com.clipvault.app.ui.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// [自适应] 响应式间距/尺寸 Token 系统
// 不同设备形态下使用不同的间距、列数、卡片尺寸
// 搜索来源: developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes
data class AdaptiveTokens(
    val pageHorizontal: Dp,
    val pageVertical: Dp,
    val sectionGap: Dp,
    val itemSpacing: Dp,
    val cardCorner: Dp,
    val cardElevation: Dp,
    val gridColumns: Int,
    val contentMaxWidth: Dp,
    val dialogMaxWidth: Dp,
    val listItemHeight: Dp,
    val fabSize: Dp
) {
    companion object {
        // [自适应] 根据设备形态返回对应的 Token 集
        fun forDevice(formFactor: DeviceFormFactor): AdaptiveTokens = when (formFactor) {
            DeviceFormFactor.PhonePortrait -> AdaptiveTokens(
                pageHorizontal = 12.dp,
                pageVertical = 8.dp,
                sectionGap = 8.dp,
                itemSpacing = 4.dp,
                cardCorner = 12.dp,
                cardElevation = 0.dp,
                gridColumns = 2,
                contentMaxWidth = Dp.Unspecified,
                dialogMaxWidth = Dp.Unspecified,
                listItemHeight = 48.dp,
                fabSize = 48.dp
            )
            DeviceFormFactor.PhoneLandscape -> AdaptiveTokens(
                pageHorizontal = 16.dp,
                pageVertical = 8.dp,
                sectionGap = 12.dp,
                itemSpacing = 6.dp,
                cardCorner = 12.dp,
                cardElevation = 0.dp,
                gridColumns = 3,
                contentMaxWidth = Dp.Unspecified,
                dialogMaxWidth = 480.dp,
                listItemHeight = 48.dp,
                fabSize = 48.dp
            )
            DeviceFormFactor.Foldable -> AdaptiveTokens(
                pageHorizontal = 20.dp,
                pageVertical = 12.dp,
                sectionGap = 12.dp,
                itemSpacing = 8.dp,
                cardCorner = 16.dp,
                cardElevation = 1.dp,
                gridColumns = 3,
                contentMaxWidth = 720.dp,
                dialogMaxWidth = 560.dp,
                listItemHeight = 52.dp,
                fabSize = 56.dp
            )
            DeviceFormFactor.Tablet -> AdaptiveTokens(
                pageHorizontal = 24.dp,
                pageVertical = 16.dp,
                sectionGap = 16.dp,
                itemSpacing = 12.dp,
                cardCorner = 16.dp,
                cardElevation = 1.dp,
                gridColumns = 4,
                contentMaxWidth = 960.dp,
                dialogMaxWidth = 640.dp,
                listItemHeight = 56.dp,
                fabSize = 56.dp
            )
            DeviceFormFactor.Desktop -> AdaptiveTokens(
                pageHorizontal = 32.dp,
                pageVertical = 20.dp,
                sectionGap = 20.dp,
                itemSpacing = 16.dp,
                cardCorner = 16.dp,
                cardElevation = 2.dp,
                gridColumns = 5,
                contentMaxWidth = 1200.dp,
                dialogMaxWidth = 720.dp,
                listItemHeight = 56.dp,
                fabSize = 56.dp
            )
        }
    }
}
