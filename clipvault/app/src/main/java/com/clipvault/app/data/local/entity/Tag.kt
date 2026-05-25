package com.clipvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.NO_ACTION  // 应用层控制删除逻辑，见 Task 7
        )
    ],
    indices = [Index("parentId")]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,  // null = 根节点
    val createdAt: Long = System.currentTimeMillis()
)
