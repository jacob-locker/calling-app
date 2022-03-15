package com.locker.callingapp.repository.cloud

import com.locker.callingapp.model.CallRoom
import com.locker.callingapp.model.CallRoomRequest
import com.locker.callingapp.model.RoomInvite
import com.locker.callingapp.model.User
import com.locker.callingapp.repository.call.WebRtcProxy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class CloudProxyImpl(
    private val invitesCloudDao: InvitesCloudDao,
    private val callRoomCloudDao: CallRoomCloudDao,
    private val webRtcProxy: WebRtcProxy
) : CloudProxy {

    override val activeRoom: StateFlow<CallRoom>
        get() = callRoomCloudDao.activeRoom

    override val invites: Flow<List<RoomInvite>>
        get() = invitesCloudDao.invites

    override suspend fun makeCallRequest(callRoomRequest: CallRoomRequest) =
        when (callRoomRequest) {
            is CallRoomRequest.Invite -> invite(callRoomRequest)
            is CallRoomRequest.Join -> joinRoom(callRoomRequest.roomInvite.roomId).also {
                invitesCloudDao.removeInvite(
                    callRoomRequest.roomInvite
                )
            }
            is CallRoomRequest.Leave -> leaveRoom(callRoomRequest)
            is CallRoomRequest.Reject -> rejectInvite(callRoomRequest)
        }

    private suspend fun invite(inviteRequest: CallRoomRequest.Invite) =
        when (inviteRequest.callRoom) {
            is CallRoom.None -> {
                callRoomCloudDao.createRoom()
                    .thenContinue { room ->
                        joinRoom(room.id)
                    }.thenMap { room ->
                        inviteUserToRoom(inviteRequest.user, room)
                    }
            }
            is CallRoom.Active -> {
                inviteUserToRoom(inviteRequest.user, inviteRequest.callRoom)
            }
        }

    private suspend fun inviteUserToRoom(
        user: User,
        activeRoom: CallRoom.Active
    ): CloudResult<Boolean> =
        invitesCloudDao.addInvite(
            to = user,
            roomId = activeRoom.id
        ).toBooleanResult()
            .also { add ->
                if (add.isSuccessful()) {
                    webRtcProxy.call(user.id!!)
                }
            }

    private suspend fun joinRoom(roomId: String) =
        callRoomCloudDao.addUserToRoom(roomId = roomId)
            .toBooleanResult()
            .also { add ->
                if (add.isSuccessful()) {
                    webRtcProxy.start(roomId)
                }
            }

    private suspend fun leaveRoom(callRoomRequest: CallRoomRequest.Leave) =
        callRoomCloudDao.removeUserFromRoom(callRoom = callRoomRequest.callRoom)
            .also { remove ->
                if (remove.isSuccessful()) {
                    webRtcProxy.stop(callRoomRequest.callRoom.id)
                }
            }

    private suspend fun rejectInvite(callRoomRequest: CallRoomRequest.Reject) =
        invitesCloudDao.removeInvite(callRoomRequest.roomInvite)
}





