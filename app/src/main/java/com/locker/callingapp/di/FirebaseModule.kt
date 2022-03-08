package com.locker.callingapp.di

import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.locker.callingapp.repository.auth.AuthUiProvider
import com.locker.callingapp.repository.auth.FirebaseAuthUiProvider
import com.locker.callingapp.repository.auth.FirebaseUserRepository
import com.locker.callingapp.repository.auth.UserRepository
import com.locker.callingapp.repository.cloud.CallRoomCloudDao
import com.locker.callingapp.repository.cloud.InvitesCloudDao
import com.locker.callingapp.repository.cloud.dao.firebase.CallRoomFirebaseDao
import com.locker.callingapp.repository.cloud.dao.firebase.InvitesFirebaseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth() = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuthUI() = AuthUI.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase() = FirebaseDatabase.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth, firebaseDatabase: FirebaseDatabase): UserRepository = FirebaseUserRepository(firebaseAuth, firebaseDatabase)

    @Provides
    @Singleton
    fun provideAuthUiProvider(authUI: AuthUI): AuthUiProvider = FirebaseAuthUiProvider(authUI)

    @Provides
    @Singleton
    fun provideFirebaseCallRoomDao(
        userRepository: UserRepository,
        firebaseDatabase: FirebaseDatabase
    ): CallRoomCloudDao =
        CallRoomFirebaseDao(userRepository, firebaseDatabase)

    @Provides
    @Singleton
    fun provideFirebaseInvitesDao(
        userRepository: UserRepository,
        firebaseDatabase: FirebaseDatabase
    ): InvitesCloudDao = InvitesFirebaseDao(userRepository, firebaseDatabase)
}