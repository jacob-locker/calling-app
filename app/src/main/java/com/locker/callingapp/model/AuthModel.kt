package com.locker.callingapp.model

import androidx.activity.ComponentActivity
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseUser

data class User(var id: String? = null, var name: String? = null)

sealed class AuthResult {
    class Success(val user: User) : AuthResult()
    object Canceled : Failed()
    open class Failed : AuthResult()
}

fun FirebaseUser.toUser() = User(uid, displayName ?: "")
fun FirebaseAuthUIAuthenticationResult.toAuthResult(user: User?) = when (resultCode) {
    ComponentActivity.RESULT_OK -> {
        AuthResult.Success(user!!)
    }
    ComponentActivity.RESULT_CANCELED -> {
        AuthResult.Canceled
    }
    else -> {
        AuthResult.Failed()
    }
}