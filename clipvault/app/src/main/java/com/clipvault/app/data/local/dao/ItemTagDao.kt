package com.clipvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clipvault.app.data.local.entity.ItemTag
import com.clipvault.app.data.local.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(itemTag: ItemTag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(itemTags: List<ItemTag>)

    @Query("DELETE FROM item_tags WHERE itemId = :itemId AND tagId = :tagId")
    suspend fun delete(itemId: Long, tagId: Long)

    @Query("DELETE FROM item_tags WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: Long)

    @Query("DELETE FROM item_tags WHERE tagId = :tagId")
    suspend fun deleteByTagId(tagId: Long)

    // 按 itemId 查关联的 Tag
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN item_tags it ON t.id = it.tagId
        WHERE it.itemId = :itemId
        ORDER BY t.name ASC
    """)
    fun getTagsByItemId(itemId: Long): Flow<List<Tag>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN item_tags it ON t.id = it.tagId
        WHERE it.itemId = :itemId
        ORDER BY t.name ASC
    """)
    suspend fun getTagsByItemIdOnce(itemId: Long): List<Tag>

    // 按 tagId 查关联的 Item ID
    @Query("SELECT itemId FROM item_tags WHERE tagId = :tagId")
    suspend fun getItemIdsByTagId(tagId: Long): List<Long>

    @Query("SELECT itemId FROM item_tags WHERE tagId = :tagId")
    fun getItemIdsByTagIdFlow(tagId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM item_tags WHERE itemId = :itemId")
    suspend fun getTagCountForItem(itemId: Long): Int

    @Query("SELECT COUNT(*) FROM item_tags WHERE tagId = :tagId")
    suspend fun getItemCountForTag(tagId: Long): Int
}
