package com.locker.callingapp.model

sealed class BaseCallerInformation
data class CallerInformation(val name: String) : BaseCallerInformation()
object EmptyCallerInformation : BaseCallerInformation()

sealed class CallRoomRequest {
    class Invite(val user: User, val callRoom: CallRoom) : CallRoomRequest()
    class Join(val roomInvite: RoomInvite) : CallRoomRequest()
    class Leave(val callRoom: CallRoom.Active) : CallRoomRequest()
    class Reject(val roomInvite: RoomInvite) : CallRoomRequest()
}

sealed class CallState {
    object None : CallState()
    class IncomingCallAccepted(val caller: User) : CallState()
    class IncomingCall(val caller: User) : CallState()
    class IncomingCallTimeout(val incomingCallState: IncomingCall) : CallState()
    class OutgoingCallAccepted(val outgoingCallState: OutgoingCall) : CallState()
    class OutgoingCall(val recipient: User) : CallState()
    class OutgoingCallError(val outgoingCallState: OutgoingCall) : CallState()
    class OutgoingCallTimeout(val outgoingCallState: OutgoingCall) : CallState()
}

data class RoomInvite(val inviteId: String, val from: User, val to: User, val roomId: String)
sealed class CallRoom {
    object None : CallRoom()
    class Active(val id: String, val userIds: List<String>, val userInfo: MutableList<User> = mutableListOf()) : CallRoom()
}

data class FirebaseUserCallState(
    var outgoingCalls: MutableList<String?>? = null,
    var incomingCalls: MutableList<String?>? = null,
    var inCallWith: String? = null)

data class FirebaseCallRoom(var id: String? = null, var users: MutableList<String?>? = null)
data class FirebaseInvite(var inviteId: String? = null, var roomId: String? = null, var from: User? = null, var to: User? = null)

fun FirebaseInvite.toRoomInvite() = RoomInvite(inviteId = inviteId!!, from = from!!, to = to!!, roomId = roomId!!)
fun FirebaseCallRoom.toActiveCallRoom() = CallRoom.Active(id = id!!, userIds = if (users == null) emptyList() else users!!.filterNotNull())
fun CallRoom.Active.toFirebaseCallRoom() = FirebaseCallRoom(id = id, users = userIds.toMutableList())