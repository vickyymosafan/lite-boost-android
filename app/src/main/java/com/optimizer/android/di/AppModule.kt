package com.optimizer.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideSystemRepository(impl: com.optimizer.android.data.repository.SystemRepositoryImpl): com.optimizer.android.domain.repository.SystemRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideAppRepository(impl: com.optimizer.android.data.repository.AppRepositoryImpl): com.optimizer.android.domain.repository.AppRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(impl: com.optimizer.android.data.repository.NotificationRepositoryImpl): com.optimizer.android.domain.repository.NotificationRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideMediaRepository(impl: com.optimizer.android.data.repository.MediaRepositoryImpl): com.optimizer.android.domain.repository.MediaRepository {
        return impl
    }
}
