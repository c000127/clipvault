package com.clipvault.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clipvault.app.data.local.dao.AiProviderDao
import com.clipvault.app.data.local.dao.ClipItemDao
import com.clipvault.app.data.local.dao.ItemTagDao
import com.clipvault.app.data.local.dao.TagDao
import com.clipvault.app.data.local.entity.AiProvider
import com.clipvault.app.data.local.entity.ClipItem
import com.clipvault.app.data.local.entity.ItemTag
import com.clipvault.app.data.local.entity.Tag

@Database(
    entities = [
        ClipItem::class,
        Tag::class,
        ItemTag::class,
        AiProvider::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipItemDao(): ClipItemDao
    abstract fun tagDao(): TagDao
    abstract fun itemTagDao(): ItemTagDao
    abstract fun aiProviderDao(): AiProviderDao
}
