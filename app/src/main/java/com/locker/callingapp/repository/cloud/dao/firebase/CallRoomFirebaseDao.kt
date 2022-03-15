package com.locker.callingapp.repository.cloud.dao.firebase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.locker.callingapp.TAG
import com.locker.callingapp.model.*
import com.locker.callingapp.repository.auth.UserRepository
import com.locker.callingapp.repository.cloud.CallRoomCloudDao
import com.locker.callingapp.repository.cloud.CloudResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CallRoomFirebaseDao(
    userRepository: UserRepository,
    coroutineScope: CoroutineScope,
    private val firebaseDatabase: FirebaseDatabase
) : BaseFirebaseDao(userRepository, coroutineScope), CallRoomCloudDao {

    private val mutableActiveRoom = MutableStateFlow<CallRoom>(CallRoom.None)

    private val roomListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            snapshot.getValue(FirebaseCallRoom::class.java)?.toActiveCallRoom()?.let {
                Log.d(this@CallRoomFirebaseDao.TAG, "OnDataChange: CallRoom $it")
                mutableActiveRoom.value = it
            } ?: updateActiveRoom(CallRoom.None)
        }

        override fun onCancelled(error: DatabaseError) {

        }

    }

    override val activeRoom: StateFlow<CallRoom>
        get() = mutableActiveRoom

    override suspend fun createRoom() = CloudResult.Success(createCallRoom().apply {
        getCallsRefById(id).setValue(this)
    }.toActiveCallRoom())

    private fun createCallRoom() = FirebaseCallRoom(
        firebaseDatabase.reference.database.getReference(CALLS_PATH).push().key, mutableListOf()
    )

    override suspend fun addUserToRoom(
        user: User?,
        roomId: String?,
    ) = retrieveRoomCloudResult(roomId) { snapshot ->
        Log.d(this@CallRoomFirebaseDao.TAG, "Adding user to room")

        val room = snapshot.getValue(FirebaseCallRoom::class.java)
        if (room != null) {
            if (room.users == null) {
                room.users = mutableListOf()
            }

            if (user?.id !in room.users ?: emptyList()) {
                room.users?.add(user?.id)
                snapshot.ref.setValue(room)
            }

            if (user?.id == currentUser?.id) {
                updateActiveRoom(room.toActiveCallRoom())
            }
        }

        room?.toActiveCallRoom()
    }

    override suspend fun removeUserFromRoom(
        user: User?,
        callRoom: CallRoom.Active?
    ) = retrieveRoomCloudResult(callRoom?.id) { snapshot ->
        Log.d(this@CallRoomFirebaseDao.TAG, "Leaving room")

        val room = snapshot.getValue(FirebaseCallRoom::class.java)
        room?.users?.remove(user?.id)
        if (room?.users?.isEmpty() == true) {
            snapshot.ref.removeValue()
        } else {
            snapshot.ref.setValue(room)
        }

        if (user?.id == currentUser?.id) {
            updateActiveRoom(CallRoom.None)
        }

        true
    }

    override suspend fun isCurrentUserInRoom(): CloudResult<Boolean> = suspendCoroutine { cont ->
        firebaseDatabase.reference.database.getReference(CALLS_PATH).get().addOnSuccessListener { snapshot ->
            val userInRoom = snapshot.children.map { child -> child.getValue(FirebaseCallRoom::class.java) }.any {
                it?.users?.any { userId -> userId != null && userId == currentUser?.id } ?: false
            }
            cont.resume(CloudResult.Success(userInRoom))
        }.addOnFailureListener {
            cont.resume(CloudResult.Failure(it))
        }
    }

    private fun updateActiveRoom(newRoom: CallRoom) {
        val currentRoom = mutableActiveRoom.value
        if (currentRoom is CallRoom.Active && newRoom is CallRoom.Active) {
            if (currentRoom.id != newRoom.id) {
                // Re-attach Listener
                attachRoomListener(newRoom)
                removeRoomListener(currentRoom)
            }
        } else if (currentRoom is CallRoom.Active) {
            removeRoomListener(currentRoom)
        } else if (newRoom is CallRoom.Active) {
            attachRoomListener(newRoom)
        }

        mutableActiveRoom.value = newRoom
    }

    private fun attachRoomListener(currentRoom: CallRoom.Active?) {
        getCallsRef(currentRoom)?.addValueEventListener(roomListener)
    }

    private fun removeRoomListener(room: CallRoom.Active?) {
        getCallsRef(room)?.removeEventListener(roomListener)
    }

    private suspend fun <T> retrieveRoomCloudResult(
        roomId: String?,
        converter: (DataSnapshot) -> T?
    ) = retrieveCloudResult(getCallsRefById(roomId), converter)

    private fun getCallsRef(call: CallRoom.Active?) =
        call?.let { getCallsRefById(it.id) }

    private fun getCallsRefById(roomId: String?) =
        firebaseDatabase.reference.database.getReference("$CALLS_PATH/$roomId")

    companion object {
        const val CALLS_PATH = "calls"
    }
}