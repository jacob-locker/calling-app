package com.locker.callingapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.locker.callingapp.CallRoomViewModel
import com.locker.callingapp.MainViewModel
import com.locker.callingapp.ui.screens.CallRoomScreen
import com.locker.callingapp.ui.screens.MainScreen

@Composable
fun NavigationComponent(mainViewModel: MainViewModel, callRoomViewModel: CallRoomViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Navigator.StartDestination.label) {
        composable(Navigator.NavTarget.Invites.label) {
            MainScreen(mainViewModel.state.collectAsState().value, mainViewModel.accept, navController)
        }
        composable(Navigator.NavTarget.CallRoom.label) {
            CallRoomScreen(uiState = callRoomViewModel.state.collectAsState().value, callRoomViewModel.accept, navController)
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