package com.locker.callingapp.repository.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.locker.callingapp.model.User
import com.locker.callingapp.model.toUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseUserRepository(private val firebaseAuth: FirebaseAuth,
                             private val firebaseDatabase: FirebaseDatabase) : UserRepository,
    FirebaseAuth.AuthStateListener {
    override val userFlow: StateFlow<User?>
        get() = mutableUserFlow

    override val allUsers get() = allUsersStateFlow

    override fun setCurrentUser(user: User) {
        updateCurrentUser(user)
    }

    private val allUsersStateFlow: MutableStateFlow<List<User>> = MutableStateFlow(emptyList())

    override suspend fun getUser(id: String) = suspendCoroutine<User?> { cont ->
            getUserRefForUser(id)?.get()?.addOnCompleteListener {
                val user = it.result.getValue(User::class.java) ?: User()
                cont.resume(user)
            } ?: cont.resume(null)
    }

    private val mutableUserFlow = MutableStateFlow(firebaseAuth.currentUser?.toUser())

    private val databaseReference: DatabaseReference by lazy {
        firebaseDatabase.reference
    }

    init {
        firebaseAuth.addAuthStateListener(this)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsersStateFlow.value = snapshot.children.mapNotNull { it.getValue(User::class.java) }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        }

        getAllUsersRef().addValueEventListener(listener)
    }

    override fun onAuthStateChanged(fbAuth: FirebaseAuth) {
        fbAuth.currentUser?.toUser()?.let { user ->
            updateCurrentUser(user)
        }
    }

    private fun updateCurrentUser(user: User) {
        getUserRefForUser(user.id)?.get()?.addOnCompleteListener {
            val currentUser = it.result?.getValue(User::class.java)
            if (currentUser == null) {
                addUserToDatabase(user)
            } else if (currentUser.name == "") {
                currentUser.name = user.name
                addUserToDatabase(currentUser)
            }
        }

        mutableUserFlow.value = user
    }

    private fun addUserToDatabase(user: User) {
        getUserRefForUser(user.id)?.setValue(user)
    }

    private fun getUserRefForUser(userId: String?) =
        userId?.let {
            databaseReference.database
                .getReference("$USERS_PATH/$it")
        }

    private fun getAllUsersRef() = databaseReference.database.getReference(USERS_PATH)

    companion object {
        const val USERS_PATH = "users"
    }
}