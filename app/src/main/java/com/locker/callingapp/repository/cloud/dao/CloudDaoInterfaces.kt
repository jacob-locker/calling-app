package com.locker.callingapp.repository.cloud

import com.locker.callingapp.model.*
import com.locker.callingapp.repository.call.ConnectionInfo
import com.locker.callingapp.repository.call.IceCandidate
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

    suspend fun addUserToRoom(user: User? = currentUser, roomId: String?) : CloudResult<CallRoom.Active>

    suspend fun removeUserFromRoom(user: User? = currentUser, callRoom: CallRoom.Active?) : CloudResult<Boolean>

    suspend fun isCurrentUserInRoom() : CloudResult<Boolean>
}

interface WebRtcCloudDao : CloudDao {
    val connections: Flow<List<ConnectionInfo>>
    val candidates: Flow<List<IceCandidate>>

    suspend fun addCandidate(iceCandidate: IceCandidate) : CloudResult<Boolean>

    suspend fun setConnection(connectionInfo: ConnectionInfo) : CloudResult<Boolean>

    suspend fun removeAllCandidatesForUser(userId: String, roomId: String) : CloudResult<Boolean>

    suspend fun removeAllConnectionsForUser(userId: String, roomId: String) : CloudResult<Boolean>
}