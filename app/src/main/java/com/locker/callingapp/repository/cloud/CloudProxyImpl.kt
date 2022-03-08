package com.locker.callingapp.repository.cloud

import com.locker.callingapp.model.CallRoom
import com.locker.callingapp.model.CallRoomRequest
import com.locker.callingapp.model.RoomInvite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.webrtc.*

class CloudProxyImpl(
    private val invitesCloudDao: InvitesCloudDao,
    private val callRoomCloudDao: CallRoomCloudDao
) : CloudProxy {

    override val activeRoom: StateFlow<CallRoom>
        get() = callRoomCloudDao.activeRoom

    override val invites: Flow<List<RoomInvite>>
        get() = invitesCloudDao.invites

    override fun makeCallRequest(callRoomRequest: CallRoomRequest) = flow {
        val result = when (callRoomRequest) {
            is CallRoomRequest.Invite -> invite(callRoomRequest)
            is CallRoomRequest.Join -> joinRoom(callRoomRequest).also { invitesCloudDao.removeInvite(callRoomRequest.roomInvite) }
            is CallRoomRequest.Leave -> leaveRoom(callRoomRequest)
            is CallRoomRequest.Reject -> rejectInvite(callRoomRequest)
        }

        emit(result)
    }.flowOn(Dispatchers.IO)

    private suspend fun invite(inviteRequest: CallRoomRequest.Invite) =
        when (inviteRequest.callRoom) {
            is CallRoom.None -> {
                val newRoomResult = callRoomCloudDao.createRoom()

                if (newRoomResult is CloudResult.Success) {
                    val newRoomId = newRoomResult.value.id
                    joinRoomById(newRoomId)
                    invitesCloudDao.addInvite(
                        to = inviteRequest.user,
                        roomId = newRoomId
                    ).toBooleanResult()
                } else {
                    newRoomResult.toBooleanResult()
                }
            }
            is CallRoom.Active -> {
                invitesCloudDao.addInvite(
                    to = inviteRequest.user,
                    roomId = inviteRequest.callRoom.id
                ).toBooleanResult()
            }
        }

    private suspend fun joinRoom(joinRequest: CallRoomRequest.Join) =
        joinRoomById(roomId = joinRequest.roomInvite.roomId)

    private suspend fun joinRoomById(roomId: String) =
        callRoomCloudDao.addUserToRoom(roomId = roomId)

    private suspend fun leaveRoom(callRoomRequest: CallRoomRequest.Leave) =
        callRoomCloudDao.removeUserFromRoom(callRoom = callRoomRequest.callRoom)

    private suspend fun rejectInvite(callRoomRequest: CallRoomRequest.Reject) =
        invitesCloudDao.removeInvite(callRoomRequest.roomInvite)
}





