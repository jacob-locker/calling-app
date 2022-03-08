package com.locker.callingapp.repository.auth

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.locker.callingapp.model.AuthResult
import com.locker.callingapp.model.toAuthResult
import com.locker.callingapp.model.toUser
import javax.inject.Inject

class FirebaseAuthUiProvider(private val authUI: AuthUI) : AuthUiProvider {
    private var signInLauncher: ActivityResultLauncher<Intent>? = null

    override fun showAuthUi(activity: ComponentActivity, onAuthResult: (AuthResult) -> Unit) {
        val signInIntent = authUI
            .createSignInIntentBuilder()
            .setAvailableProviders(arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
//                AuthUI.IdpConfig.GoogleBuilder().build(),
            ))
            .build()

        if (signInLauncher == null) {
            signInLauncher = activity.registerForActivityResult(
                FirebaseAuthUIActivityResultContract()
            ) { res ->
                onAuthResult(res.toAuthResult(FirebaseAuth.getInstance().currentUser?.toUser()))
            }
        }

        signInLauncher?.launch(signInIntent)
    }
}