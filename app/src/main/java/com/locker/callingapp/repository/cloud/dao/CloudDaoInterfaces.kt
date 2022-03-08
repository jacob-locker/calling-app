package com.locker.callingapp.repository.cloud

import com.locker.callingapp.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CloudDao {
    val currentUser: User?
}

interface InvitesCloudDao : CloudDao {
    val invites: Flow<List<RoomInvite>>

    suspend fun getInvites(user: User? = currentUser) : CloudResult<List<RoomInvite>>

    suspend fun addInvite(from: User? = currentUser, to: User?, roomId: String?) : CloudResult<RoomInvite>

    suspend fun removeInvite(roomInvite: RoomInvite) : CloudResult<Boolean>
}

interface CallRoomCloudDao : CloudDao {
    val activeRoom: StateFlow<CallRoom>

    suspend fun createRoom() : CloudResult<CallRoom.Active>

    suspend fun addUserToRoom(user: User? = currentUser, roomId: String?) : CloudResult<Boolean>

    suspend fun removeUserFromRoom(user: User? = currentUser, callRoom: CallRoom.Active?) : CloudResult<Boolean>
}