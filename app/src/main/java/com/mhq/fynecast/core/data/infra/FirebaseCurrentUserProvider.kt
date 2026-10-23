package com.mhq.fynecast.core.data.infra

import com.google.firebase.auth.FirebaseAuth
import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class FirebaseCurrentUserProvider(
    private val firebaseAuth: FirebaseAuth
) : CurrentUserProvider {

    override fun observeUserId(): Flow<String?> = callbackFlow {
        val firebaseAuthAuthStateListener =
            FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }

        firebaseAuth.addAuthStateListener(firebaseAuthAuthStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(firebaseAuthAuthStateListener) }
    }.distinctUntilChanged()

    override fun currentUserId(): String? = firebaseAuth.currentUser?.uid
}