package com.locker.callingapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locker.callingapp.model.CallRoom
import com.locker.callingapp.model.CallRoomRequest
import com.locker.callingapp.model.User
import com.locker.callingapp.repository.auth.UserRepository
import com.locker.callingapp.repository.call.WebRtcProxy
import com.locker.callingapp.repository.cloud.CloudProxy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class CallRoomViewModel @Inject constructor(
    private val cloudProxy: CloudProxy,
    private val userRepository: UserRepository,
    private val webRtcProxy: WebRtcProxy
) : ViewModel() {

    val state: StateFlow<UiState>

    private val actionStateFlow = MutableStateFlow<UiAction>(UiAction.None)
    val accept: (UiAction) -> Job = { action ->
        viewModelScope.launch { actionStateFlow.emit(action) }
    }

    init {
        val activeRoomFlow = cloudProxy.activeRoom
            .filterIsInstance<CallRoom.Active>()
            .onEach { room ->
                room.userIds.map { id ->
                    userRepository.getUser(id)?.let { room.userInfo.add(it) }
                }
            }

        state = combine(
            actionStateFlow,
            userRepository.userFlow,
            activeRoomFlow,
            ::Triple
        ).map { (action, user, activeRoom) ->
            if (action is UiAction.LeaveRoom) {
                Log.d(this@CallRoomViewModel.TAG, "makeCallRequest: Leave")
                cloudProxy.makeCallRequest(CallRoomRequest.Leave(activeRoom))
                accept(UiAction.None)
                UiState(user!!, emptyList())
            } else {
                UiState(user!!, activeRoom.userInfo.distinctBy { it.id })
            }

        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = UiState(userRepository.userFlow.value!!, emptyList())
            )
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared")
    }

    data class UiState(val self: User, val usersInRoom: List<User>)

    sealed class UiAction {
        object None : UiAction()
        object LeaveRoom : UiAction()
    }
}