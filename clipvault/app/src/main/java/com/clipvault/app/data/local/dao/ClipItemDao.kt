package com.clipvault.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.clipvault.app.data.local.entity.ClipItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ClipItem>): List<Long>

    @Update
    suspend fun update(item: ClipItem)

    @Delete
    suspend fun delete(item: ClipItem)

    @Query("DELETE FROM items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM items WHERE id = :id")
    fun getById(id: Long): Flow<ClipItem?>

    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun getAllPaged(): PagingSource<Int, ClipItem>

    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<ClipItem>>

    @Query("SELECT * FROM items ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ClipItem>

    // 全文搜索：content + note + 关联的 Tag.name
    @Query("""
        SELECT DISTINCT i.* FROM items i
        LEFT JOIN item_tags it ON i.id = it.itemId
        LEFT JOIN tags t ON it.tagId = t.id
        WHERE i.content LIKE '%' || :query || '%'
           OR i.note LIKE '%' || :query || '%'
           OR t.name LIKE '%' || :query || '%'
        ORDER BY i.createdAt DESC
    """)
    fun search(query: String): PagingSource<Int, ClipItem>

    @Query("""
        SELECT DISTINCT i.* FROM items i
        LEFT JOIN item_tags it ON i.id = it.itemId
        LEFT JOIN tags t ON it.tagId = t.id
        WHERE i.content LIKE '%' || :query || '%'
           OR i.note LIKE '%' || :query || '%'
           OR t.name LIKE '%' || :query || '%'
        ORDER BY i.createdAt DESC
    """)
    fun searchFlow(query: String): Flow<List<ClipItem>>

    // 按 Tag 过滤（使用 CTE 递归获取所有子节点下的收藏）
    @Transaction
    @Query("""
        SELECT DISTINCT i.* FROM items i
        INNER JOIN item_tags it ON i.id = it.itemId
        WHERE it.tagId IN (
            WITH RECURSIVE tag_tree(id, depth) AS (
                SELECT :tagId, 0
                UNION ALL
                SELECT t.id, tt.depth + 1
                FROM tags t INNER JOIN tag_tree tt ON t.parentId = tt.id
                WHERE tt.depth < 50
            )
            SELECT id FROM tag_tree
        )
        ORDER BY i.createdAt DESC
    """)
    fun getItemsByTagWithChildren(tagId: Long): PagingSource<Int, ClipItem>

    @Query("""
        SELECT DISTINCT i.* FROM items i
        INNER JOIN item_tags it ON i.id = it.itemId
        WHERE it.tagId IN (
            WITH RECURSIVE tag_tree(id, depth) AS (
                SELECT :tagId, 0
                UNION ALL
                SELECT t.id, tt.depth + 1
                FROM tags t INNER JOIN tag_tree tt ON t.parentId = tt.id
                WHERE tt.depth < 50
            )
            SELECT id FROM tag_tree
        )
        ORDER BY i.createdAt DESC
    """)
    fun getItemsByTagWithChildrenFlow(tagId: Long): Flow<List<ClipItem>>

    @Query("SELECT COUNT(*) FROM items")
    suspend fun getCount(): Int
}
