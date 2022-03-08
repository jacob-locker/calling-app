package com.locker.callingapp.repository.auth

import androidx.activity.ComponentActivity
import com.locker.callingapp.model.AuthResult

interface AuthUiProvider {
    fun showAuthUi(activity: ComponentActivity, onAuthResult: (AuthResult) -> Unit = {})
}