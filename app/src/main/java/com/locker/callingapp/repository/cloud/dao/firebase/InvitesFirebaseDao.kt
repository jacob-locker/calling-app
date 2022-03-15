package com.locker.callingapp.repository.cloud.dao.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.locker.callingapp.model.*
import com.locker.callingapp.repository.auth.UserRepository
import com.locker.callingapp.repository.call.ConnectionInfo
import com.locker.callingapp.repository.cloud.CloudResult
import com.locker.callingapp.repository.cloud.InvitesCloudDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class InvitesFirebaseDao @Inject constructor(
    private val userRepository: UserRepository,
    coroutineScope: CoroutineScope,
    private val firebaseDatabase: FirebaseDatabase
) : BaseFirebaseDao(userRepository, coroutineScope), InvitesCloudDao {

    private val invitesListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            mutableInvites.value = snapshot.getRoomInvites()
        }

        override fun onCancelled(error: DatabaseError) {
            // TODO: Not sure what to do here
        }
    }

    override fun onCurrentUserChanged(currentUser: User?, newUser: User?) {
        super.onCurrentUserChanged(currentUser, newUser)

        getInvitesRef(currentUser)?.removeEventListener(invitesListener)
        getInvitesRef(newUser)?.addValueEventListener(invitesListener)
    }

    override val invites: Flow<List<RoomInvite>>
        get() = mutableInvites
    private val mutableInvites = MutableStateFlow(emptyList<RoomInvite>())

    override suspend fun getInvites(user: User?) = retrieveInvitesCloudResult(user) { it.getRoomInvites() }

    override suspend fun addInvite(from: User?, to: User?, roomId: String?) =
        retrieveInvitesCloudResult(to) { snapshot ->
            snapshot.getFirebaseInvites().toMutableList().apply {
                add(FirebaseInvite(snapshot.ref.push().key, roomId, from, to))
            }.also { snapshot.ref.setValue(it) }[0].toRoomInvite()
        }

    override suspend fun removeInvite(roomInvite: RoomInvite) =
        retrieveInvitesCloudResult(roomInvite.to) { snapshot ->
            val invites = snapshot.getFirebaseInvites()
            val childIterator = snapshot.children.iterator()
            invites.forEach {
                val childSnapshot = childIterator.next()
                if (it.inviteId == roomInvite.inviteId) {
                    childSnapshot.ref.removeValue()
                }
            }

            true
        }

    private suspend fun <T> retrieveInvitesCloudResult(
        user: User?,
        converter: (DataSnapshot) -> T
    ): CloudResult<T> = retrieveCloudResult(getInvitesRef(user), converter)

    private fun getInvitesRef(user: User?) =
        user?.let { firebaseDatabase.reference.database.getReference("$INVITES_PATH/${it.id}") }

    private fun DataSnapshot.getFirebaseInvites() =
        children.mapNotNull { it.getValue(FirebaseInvite::class.java) }

    private fun DataSnapshot.getRoomInvites() = getFirebaseInvites().map { it.toRoomInvite() }

    companion object {
        const val INVITES_PATH = "invites"
    }
}