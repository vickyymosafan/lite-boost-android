package com.optimizer.android.di

import com.optimizer.android.data.repository.AppRepositoryImpl
import com.optimizer.android.data.repository.DnsShieldRepositoryImpl
import com.optimizer.android.data.repository.MediaRepositoryImpl
import com.optimizer.android.data.repository.NotificationRepositoryImpl
import com.optimizer.android.data.repository.SystemRepositoryImpl
import com.optimizer.android.domain.repository.AppRepository
import com.optimizer.android.domain.repository.DnsShieldRepository
import com.optimizer.android.domain.repository.MediaRepository
import com.optimizer.android.domain.repository.NotificationRepository
import com.optimizer.android.domain.repository.SystemRepository
import com.optimizer.android.domain.repository.VpnConfigRepository
import com.optimizer.android.data.repository.VpnConfigRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindSystemRepository(systemRepositoryImpl: SystemRepositoryImpl): SystemRepository

    @Binds
    @Singleton
    fun bindAppRepository(appRepositoryImpl: AppRepositoryImpl): AppRepository

    @Binds
    @Singleton
    fun bindNotificationRepository(notificationRepositoryImpl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    fun bindMediaRepository(mediaRepositoryImpl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    fun bindVpnConfigRepository(vpnConfigRepositoryImpl: VpnConfigRepositoryImpl): VpnConfigRepository

    @Binds
    @Singleton
    fun bindDnsShieldRepository(dnsShieldRepositoryImpl: DnsShieldRepositoryImpl): DnsShieldRepository
}
