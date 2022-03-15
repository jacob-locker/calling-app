package com.locker.callingapp.repository.cloud.dao.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.locker.callingapp.model.User
import com.locker.callingapp.repository.auth.UserRepository
import com.locker.callingapp.repository.cloud.CloudDao
import com.locker.callingapp.repository.cloud.CloudResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class BaseFirebaseDao(
    private val userRepository: UserRepository,
    private val coroutineScope: CoroutineScope
) : CloudDao {
    override var currentUser: User? = null

    init {
        coroutineScope.launch {
            userRepository.userFlow.collect { user ->
                onCurrentUserChanged(currentUser, user)
                currentUser = user
            }
        }
    }

    protected open fun onCurrentUserChanged(currentUser: User?, newUser: User?) = run { }

    protected suspend fun <T> retrieveCloudResult(
        databaseReference: DatabaseReference?,
        converter: (DataSnapshot) -> T?
    ): CloudResult<T> = suspendCoroutine { cont ->
        databaseReference?.get()
            ?.addOnSuccessListener {
                val converted = converter(it)
                cont.resume(if (converted != null) CloudResult.Success(converted) else CloudResult.Failure(null))
            }
            ?.addOnFailureListener { cont.resume(CloudResult.Failure(it)) }
            ?: cont.resume(CloudResult.Failure(null))
    }

    protected suspend fun setCloudValue(
        databaseReference: DatabaseReference?,
        value: Any
    ): CloudResult<Boolean> = suspendCoroutine { cont ->
        databaseReference?.setValue(value)
            ?.addOnSuccessListener { cont.resume(CloudResult.Success(true)) }
            ?.addOnFailureListener { cont.resume(CloudResult.Failure(it)) }
            ?: cont.resume(CloudResult.Failure(null))
    }
}
