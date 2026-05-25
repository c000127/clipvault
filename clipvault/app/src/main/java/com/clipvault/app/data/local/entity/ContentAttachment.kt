package com.clipvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "content_attachments",
    foreignKeys = [
        ForeignKey(
            entity = ClipItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class ContentAttachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val type: String,        // "image" | "link" | "media" | "file"
    val filePath: String,    // 本地文件路径 / URL
    val thumbnailPath: String = "",   // 缩略图（图片类型用）
    val orderIndex: Int = 0  // 显示排序
)
