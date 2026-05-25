package com.clipvault.app.di

import com.clipvault.app.data.repository.AiProviderRepository
import com.clipvault.app.data.repository.ClipItemRepository
import com.clipvault.app.data.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // Repositories are already constructor-injected with @Inject and @Singleton.
    // No additional bindings needed - Hilt can create them automatically.
}
