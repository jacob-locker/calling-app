package com.locker.callingapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.locker.callingapp.CallRoomViewModel
import com.locker.callingapp.MainActivity
import com.locker.callingapp.MainViewModel
import com.locker.callingapp.ui.screens.CallRoomScreen
import com.locker.callingapp.ui.screens.MainScreen
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun NavigationComponent(
    mainViewModel: MainViewModel,
    callRoomViewModel: CallRoomViewModel,
    navHostController: NavHostController,
    navigator: Navigator
) {
    LaunchedEffect("navigation") {
        navigator.navCommandFlow.onEach { command ->
            when (command) {
                is NavCommand.Navigate -> navHostController.navigate(command.target.label)
                is NavCommand.PopBackstack -> navHostController.popBackStack()
            }
        }.launchIn(this)
    }
    NavHost(
        navController = navHostController,
        startDestination = navigator.startDestination.label
    ) {
        composable(NavTarget.Invites.label) {
            MainScreen(mainViewModel.state.collectAsState().value, mainViewModel.accept, navigator)
        }
        composable(NavTarget.CallRoom.label) {
            CallRoomScreen(
                uiState = callRoomViewModel.state.collectAsState().value,
                callRoomViewModel.accept,
                navigator
            )
        }
    }
}

//@OptIn(ExperimentalAnimationApi::class)
//@Composable
//fun AnimatedNavigationComponent(mainViewModel: MainViewModel, callRoomViewModel: CallRoomViewModel) {
//
//    val navController = rememberAnimatedNavController()
//    AnimatedNavHost(navController = navController, startDestination = Navigator.StartDestination.label) {
//        composable(Navigator.NavTarget.Invites.label) {
//            MainScreen(mainViewModel.state.collectAsState().value, mainViewModel.accept, navController)
//        }
//        composable(Navigator.NavTarget.CallRoom.label) {
//            CallRoomScreen(uiState = callRoomViewModel.state.collectAsState().value, callRoomViewModel.accept, navController)
//        }
//    }
//}