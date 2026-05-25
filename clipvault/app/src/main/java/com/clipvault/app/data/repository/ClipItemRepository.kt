package com.clipvault.app.data.repository

import androidx.paging.PagingSource
import com.clipvault.app.data.local.dao.ClipItemDao
import com.clipvault.app.data.local.dao.ItemTagDao
import com.clipvault.app.data.local.dao.ContentAttachmentDao
import com.clipvault.app.data.local.entity.ClipItem
import com.clipvault.app.data.local.entity.ItemTag
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.data.local.entity.ContentAttachment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

import com.clipvault.app.data.local.AppDatabase
import androidx.room.withTransaction

@Singleton
class ClipItemRepository @Inject constructor(
    private val database: AppDatabase,
    private val clipItemDao: ClipItemDao,
    private val itemTagDao: ItemTagDao,
    private val contentAttachmentDao: ContentAttachmentDao
) {
    // CRUD 操作
    suspend fun insertWithTagsAndAttachments(item: ClipItem, tagIds: List<Long>, attachments: List<ContentAttachment>): Long {
        return database.withTransaction {
            val itemId = clipItemDao.insert(item)
            tagIds.forEach { tagId ->
                itemTagDao.insert(ItemTag(itemId = itemId, tagId = tagId))
            }
            attachments.forEach { attachment ->
                contentAttachmentDao.insert(attachment.copy(itemId = itemId))
            }
            itemId
        }
    }

    suspend fun insert(item: ClipItem): Long = clipItemDao.insert(item)


    suspend fun insertAll(items: List<ClipItem>): List<Long> = clipItemDao.insertAll(items)

    suspend fun update(item: ClipItem) = clipItemDao.update(item)

    suspend fun delete(item: ClipItem) = clipItemDao.delete(item)

    suspend fun deleteByIds(ids: List<Long>) = clipItemDao.deleteByIds(ids)

    // 查询操作
    fun getById(id: Long): Flow<ClipItem?> = clipItemDao.getById(id).map { item ->
        item?.apply {
            attachments = contentAttachmentDao.getAttachmentsByItemIdOnce(id)
        }
    }

    fun getAllPaged(): PagingSource<Int, ClipItem> = clipItemDao.getAllPaged()

    fun getAllFlow(): Flow<List<ClipItem>> = clipItemDao.getAllFlow()

    suspend fun getRecent(limit: Int): List<ClipItem> = clipItemDao.getRecent(limit)

    suspend fun getCount(): Int = clipItemDao.getCount()

    // 全文搜索
    fun search(query: String): PagingSource<Int, ClipItem> = clipItemDao.search(query)

    fun searchFlow(query: String): Flow<List<ClipItem>> = clipItemDao.searchFlow(query)

    // 按 Tag 过滤（含子节点递归）
    fun getItemsByTagWithChildren(tagId: Long): PagingSource<Int, ClipItem> =
        clipItemDao.getItemsByTagWithChildren(tagId)

    fun getItemsByTagWithChildrenFlow(tagId: Long): Flow<List<ClipItem>> =
        clipItemDao.getItemsByTagWithChildrenFlow(tagId)

    fun getItemsByTagsWithChildren(tagIds: List<Long>): PagingSource<Int, ClipItem> =
        clipItemDao.getItemsByTagsWithChildren(tagIds)

    fun getItemsByTagsAndSearchWithChildren(tagIds: List<Long>, query: String): PagingSource<Int, ClipItem> =
        clipItemDao.getItemsByTagsAndSearchWithChildren(tagIds, query)

    // Tag 关联操作
    suspend fun insertAttachments(attachments: List<ContentAttachment>) = contentAttachmentDao.insertAll(attachments)
    suspend fun getAttachmentsForItemOnce(itemId: Long): List<ContentAttachment> = contentAttachmentDao.getAttachmentsByItemIdOnce(itemId)
    fun getAttachmentsForItem(itemId: Long): Flow<List<ContentAttachment>> = contentAttachmentDao.getAttachmentsByItemId(itemId)

    suspend fun addTagToItem(itemId: Long, tagId: Long) {
        itemTagDao.insert(ItemTag(itemId = itemId, tagId = tagId))
    }

    suspend fun removeTagFromItem(itemId: Long, tagId: Long) {
        itemTagDao.delete(itemId = itemId, tagId = tagId)
    }

    suspend fun clearTagsForItem(itemId: Long) {
        itemTagDao.deleteByItemId(itemId)
    }

    fun getTagsForItem(itemId: Long): Flow<List<Tag>> = itemTagDao.getTagsByItemId(itemId)

    suspend fun getTagsForItemOnce(itemId: Long): List<Tag> = itemTagDao.getTagsByItemIdOnce(itemId)

    suspend fun setTagsForItem(itemId: Long, tagIds: List<Long>) {
        itemTagDao.deleteByItemId(itemId)
        itemTagDao.insertAll(tagIds.map { ItemTag(itemId = itemId, tagId = it) })
    }
}
