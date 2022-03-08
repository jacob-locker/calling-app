package com.locker.callingapp.ui.navigation

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.locker.callingapp.CallRoomViewModel
import com.locker.callingapp.InvitesViewModel
import com.locker.callingapp.ui.screens.CallRoomScreen
import com.locker.callingapp.ui.screens.InvitesScreen
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun NavigationComponent(invitesViewModel: InvitesViewModel, callRoomViewModel: CallRoomViewModel, navController: NavHostController) {

    NavHost(navController = navController, startDestination = Navigator.StartDestination.label) {
        composable(Navigator.NavTarget.Invites.label) {
            InvitesScreen(invitesViewModel.state.collectAsState().value, invitesViewModel.accept, navController)
        }
        composable(Navigator.NavTarget.CallRoom.label) {
            CallRoomScreen(uiState = callRoomViewModel.state.collectAsState().value, callRoomViewModel.accept, navController)
        }
    }
}