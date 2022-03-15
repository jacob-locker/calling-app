package com.locker.callingapp.repository.cloud.dao.firebase

import android.util.Log
import com.google.firebase.database.*
import com.locker.callingapp.TAG
import com.locker.callingapp.model.CallRoom
import com.locker.callingapp.repository.auth.UserRepository
import com.locker.callingapp.repository.call.*
import com.locker.callingapp.repository.cloud.CallRoomCloudDao
import com.locker.callingapp.repository.cloud.CloudResult
import com.locker.callingapp.repository.cloud.WebRtcCloudDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WebRtcFirebaseDao(
    userRepository: UserRepository,
    callRoomCloudDao: CallRoomCloudDao,
    coroutineScope: CoroutineScope,
    private val firebaseDatabase: FirebaseDatabase
) :
    BaseFirebaseDao(userRepository, coroutineScope), WebRtcCloudDao {

    private var currentRoom: CallRoom = CallRoom.None

    override val connections: Flow<List<ConnectionInfo>>
        get() = mutableConnections
    private val mutableConnections = MutableStateFlow(emptyList<ConnectionInfo>())

    override val candidates: Flow<List<IceCandidate>>
        get() = mutableCandidates
    private val mutableCandidates = MutableStateFlow(emptyList<IceCandidate>())

    private val candidatesListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d(this@WebRtcFirebaseDao.TAG, "onDataChange: candidates")
            val newCandidates = snapshot.children.mapNotNull { it.getValue(FirebaseIceCandidate::class.java)?.toIceCandidate() }
            Log.d(this@WebRtcFirebaseDao.TAG, "newCandidates: $newCandidates")
            mutableCandidates.value = newCandidates
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
    }

    private val connectionsListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            mutableConnections.value = snapshot.children.mapNotNull {
                it.getValue(FirebaseConnection::class.java)?.toConnection()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
    }

    init {
        coroutineScope.launch {
            callRoomCloudDao.activeRoom.collect {
                changeRoom(it)
            }
        }
    }

    override suspend fun addCandidate(iceCandidate: IceCandidate): CloudResult<Boolean> = suspendCoroutine { cont ->
        val ref = getCandidatesRef(iceCandidate.roomId)
        val id = ref.push().key
        getCandidatesRef(iceCandidate.roomId + "/$id").setValue(
            iceCandidate.toFirebaseIceCandidate(
                id
            )
        ).addOnSuccessListener {
            cont.resume(CloudResult.Success(true))
        }.addOnFailureListener {
            cont.resume(CloudResult.Failure(it))
        }
    }

    override suspend fun setConnection(connectionInfo: ConnectionInfo) =
        setCloudValue(
            getConnectionsRef(connectionInfo.roomId + "/${currentUser?.id}"),
            connectionInfo.toFirebaseConnection()
        )

    override suspend fun removeAllCandidatesForUser(userId: String, roomId: String): CloudResult<Boolean> {
        return retrieveCloudResult(getCandidatesRef(roomId)) {
            it.children.forEach { child ->
                val candidate = child.getValue(FirebaseIceCandidate::class.java)
                if (candidate?.userId == userId) {
                    child.ref.removeValue()
                }
            }
            true
        }
    }

    override suspend fun removeAllConnectionsForUser(
        userId: String,
        roomId: String
    ): CloudResult<Boolean> = suspendCoroutine { cont ->
        getConnectionsRef("$roomId/$userId").removeValue()
            .addOnSuccessListener {
                cont.resume(CloudResult.Success(true))
            }.addOnFailureListener {
                cont.resume(CloudResult.Failure(it))
            }
    }

    private fun changeRoom(newRoom: CallRoom) {
        removeListenersFromRoom(currentRoom)
        addListenersToRoom(newRoom)
        currentRoom = newRoom
    }

    private fun removeListenersFromRoom(room: CallRoom) {
        if (room is CallRoom.Active) {
            getCandidatesRef(room.id).removeEventListener(candidatesListener)
            getConnectionsRef(room.id).removeEventListener(connectionsListener)
        }
    }

    private fun addListenersToRoom(room: CallRoom) {
        if (room is CallRoom.Active) {
            getCandidatesRef(room.id).addValueEventListener(candidatesListener)
            getConnectionsRef(room.id).addValueEventListener(connectionsListener)
        }
    }

    private fun getCandidatesRef(roomId: String) =
        firebaseDatabase.getReference("${CANDIDATES_PATH}/${roomId}")

    private fun getConnectionsRef(roomId: String) =
        firebaseDatabase.getReference("${CONNECTIONS_PATH}/${roomId}")

    companion object {
        private const val WEBRTC_PATH = "webrtc"
        private const val CANDIDATES_PATH = "${WEBRTC_PATH}/candidates"
        private const val CONNECTIONS_PATH = "${WEBRTC_PATH}/connections"
    }
}