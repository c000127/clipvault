package com.clipvault.app.di

import android.content.Context
import androidx.room.Room
import com.clipvault.app.data.local.AppDatabase
import com.clipvault.app.data.local.dao.AiProviderDao
import com.clipvault.app.data.local.dao.ClipItemDao
import com.clipvault.app.data.local.dao.ItemTagDao
import com.clipvault.app.data.local.dao.TagDao
import com.clipvault.app.data.local.dao.ContentAttachmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val dbLock = Any()
    @Volatile
    private var dbInstance: AppDatabase? = null

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return dbInstance ?: synchronized(dbLock) {
            dbInstance ?: try {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "clipvault.db"
                )
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .build().also {
                    dbInstance = it
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseModule", "Failed to open or initialize clipvault.db, falling back to in-memory database", e)
                try {
                    Room.inMemoryDatabaseBuilder(
                        context,
                        AppDatabase::class.java
                    ).build().also {
                        dbInstance = it
                    }
                } catch (ex: Exception) {
                    throw RuntimeException("Fatal: Failed to initialize even in-memory database", ex)
                }
            }
        }
    }

    @Provides
    fun provideClipItemDao(database: AppDatabase): ClipItemDao {
        return database.clipItemDao()
    }

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    fun provideItemTagDao(database: AppDatabase): ItemTagDao {
        return database.itemTagDao()
    }

    @Provides
    fun provideAiProviderDao(database: AppDatabase): AiProviderDao {
        return database.aiProviderDao()
    }

    @Provides
    fun provideContentAttachmentDao(database: AppDatabase): ContentAttachmentDao {
        return database.contentAttachmentDao()
    }
}
