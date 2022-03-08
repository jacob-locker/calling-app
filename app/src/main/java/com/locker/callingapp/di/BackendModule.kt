package com.locker.callingapp.di

import com.locker.callingapp.repository.cloud.CallRoomCloudDao
import com.locker.callingapp.repository.cloud.CloudProxy
import com.locker.callingapp.repository.cloud.CloudProxyImpl
import com.locker.callingapp.repository.cloud.InvitesCloudDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackendModule {

    @Provides
    @Singleton
    fun provideCloudProxy(invitesCloudDao: InvitesCloudDao, callRoomCloudDao: CallRoomCloudDao): CloudProxy =
        CloudProxyImpl(invitesCloudDao, callRoomCloudDao)
}