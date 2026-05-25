package com.clipvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clipvault.app.data.local.entity.ContentAttachment
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentAttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: ContentAttachment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<ContentAttachment>): List<Long>

    @Query("SELECT * FROM content_attachments WHERE itemId = :itemId ORDER BY orderIndex ASC")
    fun getAttachmentsByItemId(itemId: Long): Flow<List<ContentAttachment>>

    @Query("SELECT * FROM content_attachments WHERE itemId = :itemId ORDER BY orderIndex ASC")
    suspend fun getAttachmentsByItemIdOnce(itemId: Long): List<ContentAttachment>

    @Query("DELETE FROM content_attachments WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: Long)

    @Query("DELETE FROM content_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
