package com.locker.callingapp.repository.call

import com.locker.callingapp.model.CallRoom
import com.locker.callingapp.model.RoomInvite
import com.locker.callingapp.model.User
import com.locker.callingapp.repository.cloud.CloudResult
import kotlinx.coroutines.flow.Flow

interface WebRtcProxy {
    fun start(roomId: String)
    fun stop(roomId: String)
    suspend fun call(userId: String) : CloudResult<Boolean>
}