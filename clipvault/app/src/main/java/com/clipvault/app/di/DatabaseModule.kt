package com.clipvault.app.di

import android.content.Context
import androidx.room.Room
import com.clipvault.app.data.local.AppDatabase
import com.clipvault.app.data.local.dao.AiProviderDao
import com.clipvault.app.data.local.dao.ClipItemDao
import com.clipvault.app.data.local.dao.ItemTagDao
import com.clipvault.app.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "clipvault.db"
        )
            // Use destructive migration as safety net for schema changes.
            // This preserves data on same schema version and only triggers
            // on actual version bumps. Prevents white screen from schema mismatch.
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
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
}
