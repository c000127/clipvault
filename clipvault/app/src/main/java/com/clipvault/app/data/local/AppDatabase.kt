package com.clipvault.app.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.clipvault.app.data.local.dao.AiProviderDao
import com.clipvault.app.data.local.dao.ClipItemDao
import com.clipvault.app.data.local.dao.ItemTagDao
import com.clipvault.app.data.local.dao.TagDao
import com.clipvault.app.data.local.dao.ContentAttachmentDao
import com.clipvault.app.data.local.entity.AiProvider
import com.clipvault.app.data.local.entity.ClipItem
import com.clipvault.app.data.local.entity.ItemTag
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.data.local.entity.ContentAttachment

@Database(
    entities = [
        ClipItem::class,
        Tag::class,
        ItemTag::class,
        AiProvider::class,
        ContentAttachment::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipItemDao(): ClipItemDao
    abstract fun tagDao(): TagDao
    abstract fun itemTagDao(): ItemTagDao
    abstract fun aiProviderDao(): AiProviderDao
    abstract fun contentAttachmentDao(): ContentAttachmentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create content_attachments table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `content_attachments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `thumbnailPath` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_content_attachments_itemId` ON `content_attachments` (`itemId`)")

                // 2. Rename items table to items_old
                db.execSQL("ALTER TABLE `items` RENAME TO `items_old`")

                // 3. Create new items table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `thumbnailPath` TEXT NOT NULL,
                        `fetchedContent` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `sourceApp` TEXT NOT NULL
                    )
                """.trimIndent())

                // 4. Query items_old and migrate
                val cursor = db.query("SELECT id, type, content, note, thumbnailPath, fetchedContent, createdAt, updatedAt, sourceApp FROM items_old")
                try {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val type = cursor.getString(1)
                        val oldContent = cursor.getString(2)
                        val note = cursor.getString(3) ?: ""
                        val thumbnailPath = cursor.getString(4) ?: ""
                        val fetchedContent = cursor.getString(5) ?: ""
                        val createdAt = cursor.getLong(6)
                        val updatedAt = cursor.getLong(7)
                        val sourceApp = cursor.getString(8) ?: ""

                        // Calculate new content text
                        val baseText = if (type == "text") oldContent else ""
                        val newContent = if (note.isNotBlank()) {
                            if (baseText.isNotBlank()) "$baseText\n\n[备注]: $note" else "[备注]: $note"
                        } else {
                            baseText
                        }

                        val newThumbnailPath = if (type == "image") oldContent else thumbnailPath

                        val itemValues = ContentValues().apply {
                            put("id", id)
                            put("type", "mixed")
                            put("content", newContent)
                            put("thumbnailPath", newThumbnailPath)
                            put("fetchedContent", fetchedContent)
                            put("createdAt", createdAt)
                            put("updatedAt", updatedAt)
                            put("sourceApp", sourceApp)
                        }
                        db.insert("items", SQLiteDatabase.CONFLICT_REPLACE, itemValues)

                        // If type != "text", create attachment
                        if (type != "text") {
                            val attachmentValues = ContentValues().apply {
                                put("itemId", id)
                                put("type", type)
                                put("filePath", oldContent)
                                put("thumbnailPath", if (type == "image") oldContent else "")
                                put("orderIndex", 0)
                            }
                            db.insert("content_attachments", SQLiteDatabase.CONFLICT_REPLACE, attachmentValues)
                        }
                    }
                } finally {
                    cursor.close()
                }

                // 5. Drop old table
                db.execSQL("DROP TABLE `items_old`")
            }
        }
    }
}
