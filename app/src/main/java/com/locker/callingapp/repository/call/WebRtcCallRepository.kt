package com.locker.callingapp.repository.call

import com.google.firebase.database.FirebaseDatabase
import com.locker.callingapp.model.BaseCallerInformation
import com.locker.callingapp.model.CallerInformation
import kotlinx.coroutines.flow.Flow

class WebRtcCallRepository(val firebaseDatabase: FirebaseDatabase) : CallRepository {
    override val selfInfo: CallerInformation
        get() = TODO("Not yet implemented")
    override val incomingCall: Flow<BaseCallerInformation>
        get() = TODO("Not yet implemented")

    override fun placeOutgoingCall(): Flow<Boolean> {
        TODO("Not yet implemented")
    }
}