package com.locker.callingapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locker.callingapp.MainViewModel.*
import com.locker.callingapp.R
import com.locker.callingapp.model.RoomInvite
import com.locker.callingapp.model.User
import com.locker.callingapp.repository.cloud.isSuccessful
import com.locker.callingapp.showToast
import com.locker.callingapp.ui.navigation.NavTarget
import com.locker.callingapp.ui.navigation.Navigator
import com.locker.callingapp.ui.navigation.PreviewNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    uiState: UiState,
    action: (UiAction) -> Job,
    navigator: Navigator = PreviewNavigator
) {
    var selectedNavItem by remember { mutableStateOf(NavItem.START_ROOM) }
    Scaffold(content = { innerPadding ->
        MainContent(
            innerPadding = innerPadding,
            selectedNavItem = selectedNavItem,
            uiState = uiState,
            action = action,
            navigator = navigator
        )
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
fun MainContent(
    innerPadding: PaddingValues,
    selectedNavItem: NavItem,
    uiState: UiState,
    action: (UiAction) -> Job,
    navigator: Navigator = PreviewNavigator
) {
    Box(modifier = Modifier.padding(innerPadding)) {
        when (uiState) {
            is UiState.AcceptedInvite -> {
                if (uiState.cloudResult.isSuccessful()) {
                    AcceptedInviteSuccessfulContent(navigator, action)
                } else {
                    AcceptedInviteErrorContent(uiState = uiState, action = action)
                }
            }
            is UiState.InvitedUser -> {
                if (uiState.cloudResult.isSuccessful()) {
                    InvitedUserSuccessfulContent(navigator, action)
                } else {
                    InvitedUserErrorContent(uiState = uiState, action = action)
                }
            }
            else -> {}
        }

        if (uiState is UiState.Authenticated) {
            AnimatedVisibility(visible = uiState !is TransitionState, exit = fadeOut(), enter = fadeIn()) {
                AuthenticatedContent(
                    selectedNavItem = selectedNavItem,
                    uiState = uiState,
                    action = action
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthenticatedContent(
    selectedNavItem: NavItem,
    uiState: UiState.Authenticated,
    action: (UiAction) -> Job
) {
    AnimatedVisibility(
        visible = selectedNavItem == NavItem.START_ROOM,
        exit = fadeOut() + slideOutHorizontally(),
        enter = fadeIn() + slideInHorizontally()
    ) {
        StartRoomContent(
            uiState = uiState,
            action = action
        )
    }

    AnimatedVisibility(
        visible = selectedNavItem == NavItem.INVITES,
        exit = fadeOut() + slideOutHorizontally { it / 2 },
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 })
    ) {
        InviteContent(
            state = uiState,
            action = action
        )
    }
}

@Composable
fun AcceptedInviteSuccessfulContent(navigator: Navigator, action: (UiAction) -> Job) {
    // Just need to navigate to call room now
    navigateToCallRoom(navigator, action)
}

@Composable
fun AcceptedInviteErrorContent(uiState: UiState.AcceptedInvite, action: (UiAction) -> Job) {
    LocalContext.current.showToast("Could not join room!")
    action(UiAction.RejectInvite(uiState.roomInvite))
}

@Composable
fun InvitedUserSuccessfulContent(navigator: Navigator, action: (UiAction) -> Job) {
    // Just need to navigate to call room now
    navigateToCallRoom(navigator, action)
}

@Composable
fun navigateToCallRoom(navigator: Navigator, action: (UiAction) -> Job) {
    navigator.navigateTo(NavTarget.CallRoom)
    action(UiAction.None)
}

@Composable
fun InvitedUserErrorContent(uiState: UiState.InvitedUser, action: (UiAction) -> Job) {
    LocalContext.current.showToast("Error in sending invite!")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InviteContent(
    state: UiState.Authenticated,
    action: (UiAction) -> Job
) {
    AnimatedVisibility(visible = state.invites.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
        InvitationList(invites = state.invites, onItemClick = { roomInvite, accepted ->
            if (accepted) {
                action(UiAction.AcceptInvite(roomInvite))
            } else {
                action(UiAction.RejectInvite(roomInvite))
            }
        })
    }

    var delayShowing by remember { mutableStateOf(false) }
    if (state.invites.isEmpty()) {
        LaunchedEffect(state is TransitionState) {
            delay(500)
            delayShowing = true
        }
    }

    AnimatedVisibility(visible = delayShowing && state.invites.isEmpty() && state !is TransitionState, enter = fadeIn(), exit = fadeOut()) {
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
    uiState: UiState.Authenticated,
    action: (UiAction) -> Job
) {
    Box(modifier = Modifier.fillMaxSize()) {
        UserList(state = uiState, onItemClick = { user ->
            action(UiAction.Invite(user))
        })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InvitationList(invites: List<RoomInvite>, onItemClick: (RoomInvite, Boolean) -> Unit) {
    LazyColumn {
        items(invites.size, { invites[it].inviteId }) {
            InvitationItem(invites[it], onItemClick, modifier = Modifier.animateItemPlacement())
        }
    }
}

@Composable
fun InvitationItem(invite: RoomInvite, onItemClick: (RoomInvite, Boolean) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
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