package com.clipvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ClipItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String = "mixed",           // always "mixed"
    val content: String,        // 文本内容 / 描述
    val thumbnailPath: String = "",  // 封面缩略图
    val fetchedContent: String = "", // Jsoup 抓取的页面纯文本，link 类型专用
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceApp: String = ""  // 来源应用包名
) {
    @Ignore
    var attachments: List<ContentAttachment> = emptyList()
}
