package com.locker.callingapp.repository.cloud

import com.locker.callingapp.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CloudProxy {
    val activeRoom: StateFlow<CallRoom>

    val invites: Flow<List<RoomInvite>>

    suspend fun makeCallRequest(callRoomRequest: CallRoomRequest) : CloudResult<Boolean>
}