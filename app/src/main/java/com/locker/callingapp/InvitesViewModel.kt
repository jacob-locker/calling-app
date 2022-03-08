package com.locker.callingapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locker.callingapp.model.CallRoom
import com.locker.callingapp.model.CallRoomRequest
import com.locker.callingapp.model.RoomInvite
import com.locker.callingapp.model.User
import com.locker.callingapp.repository.auth.UserRepository
import com.locker.callingapp.repository.cloud.CloudProxy
import com.locker.callingapp.repository.cloud.CloudResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvitesViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val cloudProxy: CloudProxy
) : ViewModel() {

    private val actionStateFlow = MutableStateFlow<UiAction>(UiAction.None)
    val accept: (UiAction) -> Job = { action ->
        viewModelScope.launch { actionStateFlow.emit(action) }
    }

    val state: StateFlow<UiState>

    init {
        state = combine(
            actionStateFlow,
            userRepository.userFlow,
            userRepository.allUsers,
            cloudProxy.invites,
            ::Combined
        ).map { (action, user, allUsers, roomInvites) ->
            executeUserAction(action)

            if (user == null) {
                UiState.NeedsAuthentication
            } else {
                UiState.Authenticated(user, allUsers, roomInvites)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = userRepository.userFlow.value.let { user ->
                if (user == null) {
                    UiState.NeedsAuthentication
                } else {
                    UiState.Authenticated(user, emptyList(), emptyList())
                }
            }
        )
    }

    private fun executeUserAction(action: UiAction) {
        viewModelScope.launch {
            when (action) {
                is UiAction.Invite -> cloudProxy.makeCallRequest(CallRoomRequest.Invite(action.user, CallRoom.None))
                is UiAction.None -> flowOf(CloudResult.Success(true))
                is UiAction.AcceptInvite -> cloudProxy.makeCallRequest(CallRoomRequest.Join(action.roomInvite))
                is UiAction.RejectInvite -> cloudProxy.makeCallRequest(CallRoomRequest.Reject(action.roomInvite))
            }.collect {
                val actionSuccess = it is CloudResult.Success && it.value
                if (action is UiAction.Invite) {
                    action.onInviteResult(actionSuccess)
                } else if (action is UiAction.AcceptInvite) {
                    action.onAcceptInviteResult(actionSuccess)
                }
                accept(UiAction.None)
            }
        }
    }

    data class Combined(val uiAction: UiAction, val user: User?, val allUsers: List<User>, val invites: List<RoomInvite>)
    sealed class UiAction {
        object None : UiAction()
        class Invite(val user: User, val onInviteResult: (Boolean) -> Unit) : UiAction()
        class AcceptInvite(val roomInvite: RoomInvite, val onAcceptInviteResult: (Boolean) -> Unit) : UiAction()

        class RejectInvite(val roomInvite: RoomInvite) : UiAction()
    }

    sealed class UiState {
        object NeedsAuthentication : UiState()
        class Authenticated(val user: User, val allUsers: List<User>, val invites: List<RoomInvite>) : UiState()
        class Calling(val recipient: User) : UiState()
    }

    enum class NavItem {
        START_ROOM,
        INVITES
    }
}


