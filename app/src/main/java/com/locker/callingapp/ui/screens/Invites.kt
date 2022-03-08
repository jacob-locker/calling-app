package com.locker.callingapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.locker.callingapp.R
import com.locker.callingapp.model.User
import kotlinx.coroutines.Job
import com.locker.callingapp.InvitesViewModel.*
import com.locker.callingapp.model.RoomInvite
import com.locker.callingapp.ui.navigation.Navigator

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InvitesScreen(
    uiState: UiState,
    action: (UiAction) -> Job,
    navController: NavController = rememberNavController()
) {
    var selectedNavItem by remember { mutableStateOf(NavItem.START_ROOM) }
    Scaffold(content = { innerPadding ->
        // Apply the padding globally to the whole BottomNavScreensController
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedVisibility(visible = selectedNavItem == NavItem.START_ROOM, exit = fadeOut() + slideOutHorizontally(), enter = fadeIn() + slideInHorizontally()) {
                StartRoomContent(
                    uiState = uiState,
                    action = action,
                    navController = navController
                )
            }
            AnimatedVisibility(visible = selectedNavItem == NavItem.INVITES, exit = fadeOut() + slideOutHorizontally({ it / 2}), enter = fadeIn() + slideInHorizontally(initialOffsetX = { it /2})) {
                InviteContent(
                    state = uiState as UiState.Authenticated,
                    action = action,
                    navController = navController
                )
            }
        }
    },
        bottomBar = {
            BottomNavigation(modifier = Modifier.wrapContentSize()) {

                BottomNavigationItem(selected = selectedNavItem == NavItem.START_ROOM,
                    onClick = { selectedNavItem = NavItem.START_ROOM }, icon = {
                        Icon(
                            imageVector = Icons.Outlined.Call,
                            contentDescription = "",
                            modifier = Modifier.size(28.dp), tint = Color.White
                        )
                    }, label = {
                        if (selectedNavItem == NavItem.START_ROOM) {
                            Text("Start Call", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Start Call")
                        }
                    })

                BottomNavigationItem(
                    selected = selectedNavItem == NavItem.INVITES,
                    onClick = { selectedNavItem = NavItem.INVITES }, icon = {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "",
                            modifier = Modifier.size(28.dp), tint = Color.White
                        )
                    }, label = {
                        if (selectedNavItem == NavItem.INVITES) {
                            Text("Notifications", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Notifications")
                        }
                    })
            }
        })
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InviteContent(
    state: UiState.Authenticated,
    action: (UiAction) -> Job,
    navController: NavController = rememberNavController(),
) {
    AnimatedVisibility(visible = state.invites.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
        InvitationList(invites = state.invites, onItemClick = { roomInvite, accepted ->
            if (accepted) {
                action(UiAction.AcceptInvite(roomInvite) { success ->
                    if (success) {
                        navController.navigate(Navigator.NavTarget.CallRoom.label)
                    } else {
                        action(UiAction.RejectInvite(roomInvite))
                    }
                })
            } else {
                action(UiAction.RejectInvite(roomInvite))
            }
        })
    }

    AnimatedVisibility(visible = state.invites.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                "No Notifications ;(", modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.h4
            )
        }
    }
}

@Composable
fun StartRoomContent(
    uiState: UiState,
    action: (UiAction) -> Job,
    navController: NavController = rememberNavController()
) {
    when (uiState) {
        is UiState.Authenticated -> AuthenticatedContent(state = uiState,
            onItemClick = { user ->
                action(UiAction.Invite(user) { success ->
                    if (success) {
                        navController.navigate(Navigator.NavTarget.CallRoom.label)
                    }
                })
            })
        is UiState.NeedsAuthentication -> NeedsAuthenticationContent(state = uiState)
        is UiState.Calling -> CallingContent(callingUiState = uiState) {

        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthenticatedContent(
    state: UiState.Authenticated,
    onItemClick: (User) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        UserList(state = state, onItemClick = onItemClick)
    }

}

@Composable
fun InvitationList(invites: List<RoomInvite>, onItemClick: (RoomInvite, Boolean) -> Unit) {
    LazyColumn {
        items(invites.size) {
            InvitationItem(invites[it], onItemClick)
        }
    }
}

@Composable
fun InvitationItem(invite: RoomInvite, onItemClick: (RoomInvite, Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Text(
                "Invitation: ${invite.from.name}",
                modifier = Modifier.align(Alignment.TopStart),
                style = MaterialTheme.typography.h6
            )
            Button(
                onClick = { onItemClick(invite, true) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(0.5f)
                    .padding(8.dp)
            ) {
                Text("Accept")
            }
            Button(
                onClick = { onItemClick(invite, false) },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.5f)
                    .padding(8.dp)
            ) {
                Text("Cancel")
            }
        }

    }
}

@Composable
fun UserList(state: UiState.Authenticated, onItemClick: (User) -> Unit) {
    LazyColumn {
        items(state.allUsers.size, { idx -> state.allUsers[idx].id ?: "" }) {
            if (state.allUsers[it].id != state.user.id) {
                UserItem(state.allUsers[it], onItemClick)
            }
        }
    }
}

@Composable
fun UserItem(user: User, onClick: (User) -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .height(128.dp)
            .padding(8.dp)
            .clickable {
                onClick(user)
            },
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Text(
            modifier = Modifier.wrapContentSize(),
            text = "Call ${user.name}",
            maxLines = 1,
            style = MaterialTheme.typography.h4
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UserItemPreview() {
    UserItem(user = User("dlkafjdlkjfa", "Jacob Locker")) {}
}

@Composable
fun CallingContent(callingUiState: UiState.Calling, onEndCall: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .wrapContentSize(),
            text = "Calling ${callingUiState.recipient.name} ...",
            style = MaterialTheme.typography.h5
        )

        Image(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(96.dp)
                .padding(8.dp)
                .clickable { onEndCall() },
            painter = painterResource(id = R.drawable.end_call), contentDescription = "End Call"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CallingContentPreview() {
    CallingContent(callingUiState = UiState.Calling(User("alkdjflkaj", "John Jones"))) {

    }
}

@Composable
fun NeedsAuthenticationContent(state: UiState.NeedsAuthentication) {
    Text("Waiting for Authentication...")
}