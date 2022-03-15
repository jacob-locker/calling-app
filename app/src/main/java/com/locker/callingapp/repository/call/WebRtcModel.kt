package com.locker.callingapp.repository.call

import org.webrtc.SessionDescription

data class ConnectionInfo(
    val roomId: String,
    val fromUserId: String,
    val toUserId: String,
    val sdp: String,
    val type: String
)
val ConnectionInfo.sessionDescription get() = SessionDescription(SessionDescription.Type.valueOf(type), sdp)

data class FirebaseConnection(
    var roomId: String? = null,
    var fromUserId: String? = null,
    var toUserId: String? = null,
    var sdp: String? = null,
    var type: String? = null
)

fun ConnectionInfo.toFirebaseConnection() = FirebaseConnection(roomId, fromUserId, toUserId, sdp, type)
fun FirebaseConnection.toConnection() = ConnectionInfo(roomId!!, fromUserId!!, toUserId!!, sdp!!, type!!)

data class IceCandidate(
    val id: String,
    val userId: String,
    val roomId: String,
    val isJoin: Boolean,
    val serverUrl: String,
    val sdpMid: String,
    val sdp: String,
    val sdpMLineIndex: Int
)

data class FirebaseIceCandidateList(var candidateList: MutableList<FirebaseIceCandidate>? = null)
data class FirebaseIceCandidate(
    var id: String? = null,
    var userId: String? = null,
    var roomId: String? = null,
    var isJoin: Boolean? = null,
    var serverUrl: String? = null,
    var sdpMid: String? = null,
    var sdp: String? = null,
    var sdpMLineIndex: Int? = null,
)

fun org.webrtc.IceCandidate.toIceCandidate(roomId: String, userId: String, isJoin: Boolean) = IceCandidate(
    "unknown",
    userId,
    roomId,
    isJoin,
    serverUrl, sdpMid, sdp, sdpMLineIndex
)

fun IceCandidate.toFirebaseIceCandidate(overrideId: String? = null) = FirebaseIceCandidate(
    overrideId ?: id, userId, roomId, isJoin, serverUrl, sdpMid, sdp, sdpMLineIndex
)

fun IceCandidate.toWebRtcIceCandidate() = org.webrtc.IceCandidate(
    sdpMid, sdpMLineIndex, sdp
)

fun FirebaseIceCandidate.toIceCandidate() = IceCandidate(
    id!!, userId!!, roomId!!, isJoin!!, serverUrl!!, sdpMid!!, sdp!!, sdpMLineIndex!!
)