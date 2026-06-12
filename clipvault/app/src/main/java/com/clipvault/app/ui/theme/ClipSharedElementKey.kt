package com.clipvault.app.ui.theme

// [动效] Shared Element Key — 禁止使用字符串 key
// 参考 Jetsnack 的 SnackSharedElementKey 设计
data class ClipSharedElementKey(
    val clipId: Long,
    val type: ClipSharedElementType
)

enum class ClipSharedElementType {
    Bounds,      // 容器变换（卡片→详情页）— sharedBounds
    Title,       // 标题文字 — sharedElement
    Content,     // 内容预览 — sharedElement
}
