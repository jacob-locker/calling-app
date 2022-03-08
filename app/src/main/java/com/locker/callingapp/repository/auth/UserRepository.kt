package com.locker.callingapp.repository.auth

import com.locker.callingapp.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val userFlow: StateFlow<User?>
    val allUsers: Flow<List<User>>

    fun setCurrentUser(user: User)
    suspend fun getUser(id: String) : User?
}