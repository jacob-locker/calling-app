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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val cloudProxy: CloudProxy
) : ViewModel() {

    private val actionStateFlow = MutableStateFlow<UiAction>(UiAction.None)
    val accept: (UiAction) -> Job = { action ->
        viewModelScope.launch { actionStateFlow.emit(action) }
    }

    val state: StateFlow<UiState>

    init {
        val executedActionFlow = actionStateFlow.mapLatest {
            Pair(it, executeUserAction(it))
        }

        state = combine(
            executedActionFlow,
            userRepository.userFlow,
            userRepository.allUsers,
            cloudProxy.invites,
            ::Combined
        ).map { (executedAction, user, allUsers, roomInvites) ->
            when {
                user == null -> {
                    UiState.NeedsAuthentication
                }
                executedAction.first is UiAction.AcceptInvite -> {
                    UiState.AcceptedInvite(
                        user, allUsers, roomInvites,
                        (executedAction.first as UiAction.AcceptInvite).roomInvite,
                        executedAction.second
                    )
                }
                executedAction.first is UiAction.Invite -> {
                    UiState.InvitedUser(
                        user, allUsers, roomInvites,
                        executedAction.second
                    )
                }
                else -> {
                    UiState.Authenticated(user, allUsers, roomInvites)
                }
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

    private suspend fun executeUserAction(action: UiAction) =
        when (action) {
            is UiAction.Invite -> cloudProxy.makeCallRequest(
                CallRoomRequest.Invite(
                    action.user,
                    CallRoom.None
                )
            )
            is UiAction.None -> CloudResult.Success(true)
            is UiAction.AcceptInvite -> cloudProxy.makeCallRequest(CallRoomRequest.Join(action.roomInvite))
            is UiAction.RejectInvite -> cloudProxy.makeCallRequest(CallRoomRequest.Reject(action.roomInvite))
        }

    data class Combined(
        val uiAction: Pair<UiAction, CloudResult<Boolean>>,
        val user: User?,
        val allUsers: List<User>,
        val invites: List<RoomInvite>
    )

    sealed class UiAction {
        object None : UiAction()
        class Invite(val user: User) : UiAction()
        class AcceptInvite(val roomInvite: RoomInvite) : UiAction()

        class RejectInvite(val roomInvite: RoomInvite) : UiAction()
    }

    interface TransitionState

    sealed class UiState {
        object NeedsAuthentication : UiState()
        class AcceptedInvite(
            user: User,
            allUsers: List<User>,
            invites: List<RoomInvite>,
            val roomInvite: RoomInvite,
            val cloudResult: CloudResult<Boolean>
        ) : UiState.Authenticated(user, allUsers, invites), TransitionState

        class InvitedUser(
            user: User,
            allUsers: List<User>,
            invites: List<RoomInvite>,
            val cloudResult: CloudResult<Boolean>
        ) : UiState.Authenticated(user, allUsers, invites), TransitionState

        open class Authenticated(
            val user: User,
            val allUsers: List<User>,
            val invites: List<RoomInvite>
        ) : UiState()

        class Calling(val recipient: User) : UiState()
    }

    enum class NavItem {
        START_ROOM,
        INVITES
    }
}


