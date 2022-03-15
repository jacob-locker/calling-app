package com.locker.callingapp

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.locker.callingapp.model.AuthResult
import com.locker.callingapp.model.CallRoom
import com.locker.callingapp.model.User
import com.locker.callingapp.repository.auth.AuthUiProvider
import com.locker.callingapp.repository.auth.UserRepository
import com.locker.callingapp.repository.call.WebRtcProxy
import com.locker.callingapp.repository.cloud.CloudResult
import com.locker.callingapp.ui.navigation.*
import com.locker.callingapp.ui.theme.CallingAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), Navigator {

    @Inject
    lateinit var authUiProvider: AuthUiProvider

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var webRtcProxy: WebRtcProxy

    private val mainViewModel by viewModels<MainViewModel>()

    private val callRoomViewModel by viewModels<CallRoomViewModel>()

    private val navCommandSharedFlow = MutableSharedFlow<NavCommand>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (userRepository.userFlow.value == null) {
            showAuthUi()
        } else {
            showContent()
        }
    }

    override val startDestination: NavTarget
        get() = NavTarget.Invites

    override val navCommandFlow: SharedFlow<NavCommand>
        get() = navCommandSharedFlow.asSharedFlow()

    override fun navigateTo(navTarget: NavTarget) {
        if (navTarget.requiredPermissions.isNotEmpty()) {
            checkPermissions(navTarget.requiredPermissions, navTarget.permissionRequestCode) {
                onCallRoomPermissionGranted()
            }
        } else {
            navCommandSharedFlow.tryEmit(NavCommand.Navigate(navTarget))
        }
    }

    override fun popBackstack() {
        navCommandSharedFlow.tryEmit(NavCommand.PopBackstack)
    }

    private fun onCallRoomPermissionGranted() {
        navCommandSharedFlow.tryEmit(NavCommand.Navigate(NavTarget.CallRoom))
    }

    private fun onCallRoomPermissionDenied() {
        showToast("Permission denied.  Cannot enter room.")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NavTarget.CallRoom.permissionRequestCode) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onCallRoomPermissionGranted()
            } else {
                onCallRoomPermissionDenied()
            }
        }
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

    private fun checkPermissions(permissions: List<Permission>, requestCode: Int, onPermissionsGranted: () -> Unit) {
        if (permissions.all {
                ContextCompat.checkSelfPermission(
                    this,
                    it.androidPermission
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            onPermissionsGranted()
        } else {
            requestPermissions(permissions, requestCode)
        }
    }

    private fun requestPermissions(
        permissions: List<Permission>,
        requestCode: Int,
        dialogShown: Boolean = false
    ) {
        if (permissions.any {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    it.androidPermission
                )
            } && !dialogShown) {
            showPermissionRationaleDialog(permissions, requestCode)
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissions.map { it.androidPermission }.toTypedArray(),
                requestCode
            )
        }
    }

    private fun showPermissionRationaleDialog(permissions: List<Permission>, requestCode: Int) {
        val requiredPermissions = permissions.joinToString(" and ") { it.friendlyName }
        AlertDialog.Builder(this)
            .setTitle("$requiredPermissions Required")
            .setMessage("This app needs $requiredPermissions to function.")
            .setPositiveButton("Grant") { dialog, _ ->
                dialog.dismiss()
                requestPermissions(permissions, requestCode, true)
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                showToast("Permission Denied.")
            }
            .show()
    }

    @OptIn(ExperimentalAnimationApi::class)
    private fun showContent() {
        setContent {
            CallingAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    NavigationComponent(
                        mainViewModel,
                        callRoomViewModel,
                        rememberNavController(),
                        this
                    )
                }
            }
        }
    }
}