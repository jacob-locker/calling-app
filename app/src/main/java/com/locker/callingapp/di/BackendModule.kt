package com.locker.callingapp.di

import android.content.Context
import com.locker.callingapp.repository.call.WebRtcProxy
import com.locker.callingapp.repository.call.WebRtcProxyImpl
import com.locker.callingapp.repository.cloud.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackendModule {
    @Provides
    @Singleton
    fun provideCloudProxy(
        invitesCloudDao: InvitesCloudDao,
        callRoomCloudDao: CallRoomCloudDao,
        webRtcProxy: WebRtcProxy
    ): CloudProxy =
        CloudProxyImpl(invitesCloudDao, callRoomCloudDao, webRtcProxy)

    @Provides
    @Singleton
    fun provideWebRtcProxy(
        @ApplicationContext context: Context,
        webRtcCloudDao: WebRtcCloudDao
    ): WebRtcProxy = WebRtcProxyImpl(context, webRtcCloudDao, CoroutineScope(Dispatchers.IO))
}