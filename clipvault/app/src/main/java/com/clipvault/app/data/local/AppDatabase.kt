package com.clipvault.app.data.local

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.clipvault.app.data.local.dao.AiProviderDao
import com.clipvault.app.data.local.dao.BehaviorDao
import com.clipvault.app.data.local.dao.ClipItemDao
import com.clipvault.app.data.local.dao.InsightDao
import com.clipvault.app.data.local.dao.ItemTagDao
import com.clipvault.app.data.local.dao.TagDao
import com.clipvault.app.data.local.dao.ContentAttachmentDao
import com.clipvault.app.data.local.entity.AiProvider
import com.clipvault.app.data.local.entity.BehaviorLog
import com.clipvault.app.data.local.entity.ClipItem
import com.clipvault.app.data.local.entity.ItemTag
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.data.local.entity.UserInsight
import com.clipvault.app.data.local.entity.ContentAttachment

// [自适应] 数据库升级: version 3→4，新增 behavior_logs 和 user_insights 表
@Database(
    entities = [
        ClipItem::class,
        Tag::class,
        ItemTag::class,
        AiProvider::class,
        ContentAttachment::class,
        BehaviorLog::class,
        UserInsight::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipItemDao(): ClipItemDao
    abstract fun tagDao(): TagDao
    abstract fun itemTagDao(): ItemTagDao
    abstract fun aiProviderDao(): AiProviderDao
    abstract fun contentAttachmentDao(): ContentAttachmentDao
    abstract fun behaviorDao(): BehaviorDao
    abstract fun insightDao(): InsightDao

    companion object {
        @Volatile
        var migrationFailed = false

        private val dbLock = Any()
        @Volatile
        private var dbInstance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return dbInstance ?: synchronized(dbLock) {
                dbInstance ?: try {
                    val db = androidx.room.Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "clipvault.db"
                    )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .setQueryCallback({ sql, args ->
                        android.util.Log.d("AppDB", "SQL: $sql | Args: $args")
                    }, { it.run() })
                    .build()

                    // Verify DB is accessible
                    db.openHelper.writableDatabase

                    db.also { dbInstance = it }
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Database init failed", e)
                    dbInstance = null
                    migrationFailed = true
                    throw RuntimeException(
                        "Database initialization failed. Your data is preserved in the original file. " +
                        "Please report this issue.", e
                    )
                }
            }
        }

        private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName))
            return cursor.use { it.moveToFirst() }
        }

        // [自适应] Migration 3→4: 新增 behavior_logs 和 user_insights 表
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("AppDB", "Starting MIGRATION_3_4 (adaptive tables)...")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `behavior_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `metadata` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `sessionId` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavior_logs_eventType` ON `behavior_logs` (`eventType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavior_logs_timestamp` ON `behavior_logs` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavior_logs_sessionId` ON `behavior_logs` (`sessionId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_insights` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `insightType` TEXT NOT NULL,
                        `key` TEXT NOT NULL,
                        `value` REAL NOT NULL,
                        `sampleCount` INTEGER NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_insights_insightType_key` ON `user_insights` (`insightType`, `key`)")

                android.util.Log.d("AppDB", "MIGRATION_3_4 completed successfully.")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("AppDB", "Starting MIGRATION_2_3...")
                db.execSQL("ALTER TABLE `items` ADD COLUMN `aiSummary` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `items` ADD COLUMN `aiSummaryHistory` TEXT NOT NULL DEFAULT '[]'")
                android.util.Log.d("AppDB", "MIGRATION_2_3 completed successfully.")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    android.util.Log.d("AppDB", "Starting MIGRATION_1_2...")

                    // Disable foreign keys during migration
                    db.execSQL("PRAGMA foreign_keys = OFF;")

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

                    // 5. Rebuild item_tags to correctly reference the new items table
                    val hasItemTagsOldTable = tableExists(db, "item_tags_old")
                    val hasItemTagsTable = tableExists(db, "item_tags")

                    if (hasItemTagsTable && !hasItemTagsOldTable) {
                        db.execSQL("ALTER TABLE `item_tags` RENAME TO `item_tags_old`")
                        android.util.Log.d("AppDB", "Renamed item_tags to item_tags_old")
                    }

                    // Create new item_tags table with foreign keys pointing to items
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `item_tags` (
                            `itemId` INTEGER NOT NULL,
                            `tagId` INTEGER NOT NULL,
                            PRIMARY KEY(`itemId`, `tagId`),
                            FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                            FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())
                    // Copy item_tags data
                    if (tableExists(db, "item_tags_old")) {
                        db.execSQL("INSERT OR REPLACE INTO `item_tags` (`itemId`, `tagId`) SELECT `itemId`, `tagId` FROM `item_tags_old`")
                        db.execSQL("DROP TABLE `item_tags_old`")
                        android.util.Log.d("AppDB", "Migrated item_tags and dropped item_tags_old")
                    }

                    // Create indices on the new item_tags table (after dropping item_tags_old to avoid name clashes!)
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_tags_itemId` ON `item_tags` (`itemId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_tags_tagId` ON `item_tags` (`tagId`)")

                    // 6. Verify migration and drop old items table
                    val verifyCursor = db.query("SELECT COUNT(*) FROM `items`")
                    val itemsCount = if (verifyCursor.moveToFirst()) verifyCursor.getInt(0) else 0
                    verifyCursor.close()
                    android.util.Log.d("AppDB", "Migration verification: items count = $itemsCount")

                    if (tableExists(db, "items_old")) {
                        db.execSQL("DROP TABLE `items_old`")
                        android.util.Log.d("AppDB", "Dropped table items_old")
                    }

                    // Re-enable foreign keys after migration
                    db.execSQL("PRAGMA foreign_keys = ON;")
                    android.util.Log.d("AppDB", "MIGRATION_1_2 completed successfully.")
                } catch (e: Exception) {
                    android.util.Log.e("AppDB", "MIGRATION_1_2 FAILED!", e)
                    migrationFailed = true
                    try {
                        db.execSQL("PRAGMA foreign_keys = ON;")
                    } catch (ex: Exception) {
                        // ignore
                    }
                    throw e
                }
            }
        }
    }
}
