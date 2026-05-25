package com.clipvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.clipvault.app.data.local.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<Tag>): List<Long>

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM tags WHERE id = :id")
    fun getById(id: Long): Flow<Tag?>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getByIdOnce(id: Long): Tag?

    @Query("SELECT * FROM tags WHERE parentId IS NULL ORDER BY name ASC")
    fun getRootTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE parentId = :parentId ORDER BY name ASC")
    fun getChildren(parentId: Long): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE parentId = :parentId ORDER BY name ASC")
    suspend fun getChildrenOnce(parentId: Long): List<Tag>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsOnce(): List<Tag>

    // CTE 递归查询：获取某个 Tag 及其所有子节点（深度限制 50 层）
    @Query("""
        WITH RECURSIVE tag_tree(id, name, parentId, createdAt, depth) AS (
            SELECT id, name, parentId, createdAt, 0 FROM tags WHERE id = :rootTagId
            UNION ALL
            SELECT t.id, t.name, t.parentId, t.createdAt, tt.depth + 1
            FROM tags t INNER JOIN tag_tree tt ON t.parentId = tt.id
            WHERE tt.depth < 50
        )
        SELECT id, name, parentId, createdAt FROM tag_tree
    """)
    suspend fun getTagTree(rootTagId: Long): List<Tag>

    // 获取某个 Tag 的所有子节点 ID（不含自身）
    @Query("""
        WITH RECURSIVE tag_tree(id, depth) AS (
            SELECT :rootTagId, 0
            UNION ALL
            SELECT t.id, tt.depth + 1
            FROM tags t INNER JOIN tag_tree tt ON t.parentId = tt.id
            WHERE tt.depth < 50
        )
        SELECT id FROM tag_tree WHERE id != :rootTagId
    """)
    suspend fun getDescendantIds(rootTagId: Long): List<Long>

    // 检查某个 Tag 是否是另一个 Tag 的后代（环形引用防护）
    @Query("""
        WITH RECURSIVE tag_tree(id, depth) AS (
            SELECT :potentialAncestorId, 0
            UNION ALL
            SELECT t.id, tt.depth + 1
            FROM tags t INNER JOIN tag_tree tt ON t.parentId = tt.id
            WHERE tt.depth < 50
        )
        SELECT COUNT(*) FROM tag_tree WHERE id = :tagId
    """)
    suspend fun isDescendantOf(tagId: Long, potentialAncestorId: Long): Int

    // 更新 parentId（移动 Tag 到新的父节点）
    @Query("UPDATE tags SET parentId = :newParentId WHERE id = :tagId")
    suspend fun updateParentId(tagId: Long, newParentId: Long?)

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getCount(): Int

    // 按名称搜索 Tag
    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchTags(query: String): Flow<List<Tag>>
}
