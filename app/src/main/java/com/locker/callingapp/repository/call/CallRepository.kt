package com.locker.callingapp.repository.call

import com.locker.callingapp.model.BaseCallerInformation
import com.locker.callingapp.model.CallerInformation
import kotlinx.coroutines.flow.Flow

interface CallRepository {
    val selfInfo: CallerInformation

    val incomingCall: Flow<BaseCallerInformation>

    fun placeOutgoingCall(): Flow<Boolean>
}