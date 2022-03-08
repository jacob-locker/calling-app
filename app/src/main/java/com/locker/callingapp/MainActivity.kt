package com.locker.callingapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.locker.callingapp.model.AuthResult
import com.locker.callingapp.model.User
import com.locker.callingapp.repository.auth.AuthUiProvider
import com.locker.callingapp.repository.auth.UserRepository
import com.locker.callingapp.ui.navigation.NavigationComponent
import com.locker.callingapp.ui.navigation.Navigator
import com.locker.callingapp.ui.theme.CallingAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authUiProvider: AuthUiProvider

    @Inject
    lateinit var userRepository: UserRepository

    private val invitesViewModel by viewModels<InvitesViewModel>()
    private val callRoomViewModel by viewModels<CallRoomViewModel>()

    private val onBack: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (userRepository.userFlow.value == null) {
            showAuthUi()
        } else {
            showContent()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun onAuthFailed(authFailure: AuthResult.Failed) {
        // Sign in failed. If response is null the user canceled the
        // sign-in flow using the back button. Otherwise check
        // response.getError().getErrorCode() and handle the error.
        // ...
        when (authFailure) {
            is AuthResult.Canceled -> showAuthUi()
            else -> onCompleteAuthFailure()
        }
    }

    private fun onCompleteAuthFailure() {
        Toast.makeText(
            this@MainActivity,
            "We couldn't sign you in. Please try again later.",
            Toast.LENGTH_LONG
        )
            .show()

        // Close the app
        finish()
    }

    private fun showAuthUi() {
        authUiProvider.showAuthUi(this) { authResult ->
            when (authResult) {
                is AuthResult.Failed -> onAuthFailed(authResult)
                is AuthResult.Success -> showContent().also {
                    userRepository.setCurrentUser(
                        authResult.user
                    )
                }
            }
        }
    }

    private fun showContent() {
        setContent {
            CallingAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    NavigationComponent(invitesViewModel, callRoomViewModel, navController = rememberNavController())
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CallingAppTheme {

    }
}