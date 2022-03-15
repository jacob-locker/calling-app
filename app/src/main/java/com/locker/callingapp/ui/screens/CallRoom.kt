package com.locker.callingapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.locker.callingapp.CallRoomViewModel.UiAction
import com.locker.callingapp.CallRoomViewModel.UiState
import com.locker.callingapp.model.User
import com.locker.callingapp.ui.navigation.Navigator
import com.locker.callingapp.ui.navigation.PreviewNavigator
import kotlinx.coroutines.Job

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallRoomScreen(uiState: UiState, action: (UiAction) -> Job, navigator: Navigator = PreviewNavigator) {
    BackHandler {
        action(UiAction.LeaveRoom)
        navigator.popBackstack()
    }
    Scaffold(modifier = Modifier
        .fillMaxSize()
        .background(Color.LightGray)) {
        Column {
            RoomUserList(self = uiState.self, usersInRoom = uiState.usersInRoom)
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun RoomUserList(self: User, usersInRoom: List<User>) {
    LazyRow {
        items(usersInRoom.size, { usersInRoom[it].id!! }) {
            RoomUserItem(user = usersInRoom[it], modifier = Modifier.animateItemPlacement())
        }
    }
}

@Composable
fun RoomUserItem(user: User, modifier: Modifier = Modifier) {
    Card(modifier = modifier
        .size(128.dp)
        .padding(8.dp),
    shape = RoundedCornerShape(16.dp),
    elevation = 4.dp) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(text = user.name?.split(" ")?.get(0) ?: "", modifier = Modifier
                .wrapContentSize(Alignment.Center)
                .padding(8.dp)
                .align(
                    Alignment.Center
                ), style = MaterialTheme.typography.h5)
        }
    }
}