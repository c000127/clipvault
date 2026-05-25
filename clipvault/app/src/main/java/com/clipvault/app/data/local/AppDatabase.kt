package com.clipvault.app.data.local

import android.content.Context
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
        var migrationFailed = false

        private val dbLock = Any()
        @Volatile
        private var dbInstance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return dbInstance ?: synchronized(dbLock) {
                dbInstance ?: try {
                    androidx.room.Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "clipvault.db"
                    )
                    .addMigrations(MIGRATION_1_2)
                    .setQueryCallback({ sql, args ->
                        android.util.Log.d("AppDB", "SQL: $sql | Args: $args")
                    }, { it.run() })
                    .build().also {
                        dbInstance = it
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "AppDatabase builder failed", e)
                    migrationFailed = true
                    throw e
                }
            }
        }

        private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName))
            return cursor.use { it.moveToFirst() }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    android.util.Log.d("AppDB", "Starting MIGRATION_1_2...")

                    // 1. Create content_attachments table if not exists
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

                    val hasOldTable = tableExists(db, "items_old")
                    val hasItemsTable = tableExists(db, "items")

                    // 2. Rename items table to items_old if it exists and old doesn't
                    if (hasItemsTable && !hasOldTable) {
                        db.execSQL("ALTER TABLE `items` RENAME TO `items_old`")
                        android.util.Log.d("AppDB", "Renamed items to items_old")
                    }

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

                    // 4. Query items_old and migrate safely
                    if (tableExists(db, "items_old")) {
                        val cursor = db.query("SELECT * FROM items_old")
                        try {
                            val idIdx = cursor.getColumnIndex("id")
                            val typeIdx = cursor.getColumnIndex("type")
                            val contentIdx = cursor.getColumnIndex("content")
                            val noteIdx = cursor.getColumnIndex("note")
                            val thumbIdx = cursor.getColumnIndex("thumbnailPath")
                            val fetchedIdx = cursor.getColumnIndex("fetchedContent")
                            val createdIdx = cursor.getColumnIndex("createdAt")
                            val updatedIdx = cursor.getColumnIndex("updatedAt")
                            val sourceIdx = cursor.getColumnIndex("sourceApp")

                            while (cursor.moveToNext()) {
                                try {
                                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else -1L
                                    val type = if (typeIdx >= 0) cursor.getString(typeIdx) else "text"
                                    val oldContent = if (contentIdx >= 0) cursor.getString(contentIdx) else ""
                                    val note = if (noteIdx >= 0 && !cursor.isNull(noteIdx)) cursor.getString(noteIdx) else ""
                                    val thumbnailPath = if (thumbIdx >= 0 && !cursor.isNull(thumbIdx)) cursor.getString(thumbIdx) else ""
                                    val fetchedContent = if (fetchedIdx >= 0 && !cursor.isNull(fetchedIdx)) cursor.getString(fetchedIdx) else ""
                                    val createdAt = if (createdIdx >= 0) cursor.getLong(createdIdx) else System.currentTimeMillis()
                                    val updatedAt = if (updatedIdx >= 0) cursor.getLong(updatedIdx) else System.currentTimeMillis()
                                    val sourceApp = if (sourceIdx >= 0 && !cursor.isNull(sourceIdx)) cursor.getString(sourceIdx) else ""

                                    // Calculate new content text
                                    val baseText = if (type == "text") oldContent else ""
                                    val newContent = if (note.isNotBlank()) {
                                        if (baseText.isNotBlank()) "$baseText\n\n[备注]: $note" else "[备注]: $note"
                                    } else {
                                        baseText
                                    }

                                    val newThumbnailPath = if (type == "image") oldContent else thumbnailPath

                                    val itemValues = ContentValues().apply {
                                        if (id != -1L) {
                                            put("id", id)
                                        }
                                        put("type", "mixed")
                                        put("content", newContent)
                                        put("thumbnailPath", newThumbnailPath)
                                        put("fetchedContent", fetchedContent)
                                        put("createdAt", createdAt)
                                        put("updatedAt", updatedAt)
                                        put("sourceApp", sourceApp)
                                    }
                                    val insertedId = db.insert("items", SQLiteDatabase.CONFLICT_REPLACE, itemValues)
                                    val targetId = if (id != -1L) id else insertedId

                                    // If type != "text", create attachment
                                    if (type != "text" && targetId != -1L) {
                                        val attachmentValues = ContentValues().apply {
                                            put("itemId", targetId)
                                            put("type", type)
                                            put("filePath", oldContent)
                                            put("thumbnailPath", if (type == "image") oldContent else "")
                                            put("orderIndex", 0)
                                        }
                                        db.insert("content_attachments", SQLiteDatabase.CONFLICT_REPLACE, attachmentValues)
                                    }
                                } catch (rowEx: Exception) {
                                    android.util.Log.e("AppDB", "Migration error for row, skipping row", rowEx)
                                }
                            }
                        } finally {
                            cursor.close()
                        }
                    }

                    // 5. Verify migration and drop old table
                    val verifyCursor = db.query("SELECT COUNT(*) FROM `items`")
                    val itemsCount = if (verifyCursor.moveToFirst()) verifyCursor.getInt(0) else 0
                    verifyCursor.close()
                    android.util.Log.d("AppDB", "Migration verification: items count = $itemsCount")

                    if (tableExists(db, "items_old")) {
                        db.execSQL("DROP TABLE `items_old`")
                        android.util.Log.d("AppDB", "Dropped table items_old")
                    }
                    android.util.Log.d("AppDB", "MIGRATION_1_2 completed successfully.")
                } catch (e: Exception) {
                    android.util.Log.e("AppDB", "MIGRATION_1_2 FAILED!", e)
                    migrationFailed = true
                    throw e
                }
            }
        }
    }
}
