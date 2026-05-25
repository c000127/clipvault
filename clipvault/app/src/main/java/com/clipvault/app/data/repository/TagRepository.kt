package com.clipvault.app.data.repository

import com.clipvault.app.data.local.dao.ItemTagDao
import com.clipvault.app.data.local.dao.TagDao
import com.clipvault.app.data.local.entity.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao,
    private val itemTagDao: ItemTagDao
) {
    // CRUD 操作
    suspend fun insert(tag: Tag): Long = tagDao.insert(tag)

    suspend fun insertAll(tags: List<Tag>): List<Long> = tagDao.insertAll(tags)

    suspend fun update(tag: Tag) = tagDao.update(tag)

    suspend fun delete(tag: Tag) = tagDao.delete(tag)

    suspend fun deleteById(id: Long) = tagDao.deleteById(id)

    // 查询操作
    fun getById(id: Long): Flow<Tag?> = tagDao.getById(id)

    suspend fun getByIdOnce(id: Long): Tag? = tagDao.getByIdOnce(id)

    fun getRootTags(): Flow<List<Tag>> = tagDao.getRootTags()

    fun getChildren(parentId: Long): Flow<List<Tag>> = tagDao.getChildren(parentId)

    suspend fun getChildrenOnce(parentId: Long): List<Tag> = tagDao.getChildrenOnce(parentId)

    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun getAllTagsOnce(): List<Tag> = tagDao.getAllTagsOnce()

    suspend fun getCount(): Int = tagDao.getCount()

    // 按名称搜索 Tag
    fun searchTags(query: String): Flow<List<Tag>> = tagDao.searchTags(query)

    // CTE 递归查询：获取某个 Tag 及其所有子节点
    suspend fun getTagTree(rootTagId: Long): List<Tag> = tagDao.getTagTree(rootTagId)

    // 获取某个 Tag 的所有子节点 ID（不含自身）
    suspend fun getDescendantIds(rootTagId: Long): List<Long> = tagDao.getDescendantIds(rootTagId)

    // 环形引用防护：检查某个 Tag 是否是另一个 Tag 的后代
    suspend fun isDescendantOf(tagId: Long, potentialAncestorId: Long): Boolean {
        return tagDao.isDescendantOf(tagId, potentialAncestorId) > 0
    }

    // 移动 Tag 到新的父节点（带环形引用防护）
    suspend fun moveTag(tagId: Long, newParentId: Long?): Result<Unit> {
        // 不能移动到自身
        if (tagId == newParentId) return Result.failure(IllegalArgumentException("Cannot move tag to itself"))

        // 如果 newParentId 不为 null，检查是否会造成环形引用
        if (newParentId != null) {
            val isCircular = isDescendantOf(newParentId, tagId)
            if (isCircular) return Result.failure(IllegalStateException("Cannot move tag to its descendant"))
        }

        tagDao.updateParentId(tagId, newParentId)
        return Result.success(Unit)
    }

    // 删除 Tag（应用层事务：子节点上移一层 + 清理关联）
    suspend fun deleteTagWithReparenting(tagId: Long) {
        val tag = tagDao.getByIdOnce(tagId) ?: return
        val children = tagDao.getChildrenOnce(tagId)
        val parentId = tag.parentId

        // 将子节点的 parentId 更新为被删节点的 parentId（上移一层）
        for (child in children) {
            tagDao.updateParentId(child.id, parentId)
        }

        // 删除该 Tag 关联的所有 ItemTag 记录
        itemTagDao.deleteByTagId(tagId)

        // 删除目标 Tag
        tagDao.deleteById(tagId)
    }

    suspend fun getItemCountForTag(tagId: Long): Int = itemTagDao.getItemCountForTag(tagId)
}
