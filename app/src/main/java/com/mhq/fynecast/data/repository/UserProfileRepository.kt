package com.mhq.fynecast.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.mhq.fynecast.ui.screens.settings.screens.SettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

//Firebase storage...
class UserProfileRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance()

    private val _profileState = MutableStateFlow(SettingsState())
    val profileState: StateFlow<SettingsState> = _profileState.asStateFlow()

    // Uploads local device image to cloud storage and saves profile text fields
    suspend fun updateProfile(newName: String, newEmail: String, localImageUriString: String?) {
        withContext(Dispatchers.IO) {
            val userId = firebaseAuth.currentUser?.uid ?: "anonymous_user"
            var cloudImageUrl = _profileState.value.profileImageUri // Default to existing image if no new one is picked

            // 1. If the user selected a new photo, upload it to Firebase Storage
            if (localImageUriString != null && localImageUriString.startsWith("content://")) {
                try {
                    val storageRef = firebaseStorage.reference.child("profile_pictures/$userId.jpg")
                    val localUri = Uri.parse(localImageUriString)

                    // Upload file and suspend until completion
                    storageRef.putFile(localUri).await()

                    // Retrieve the permanent cloud link
                    cloudImageUrl = storageRef.downloadUrl.await().toString()
                } catch (e: Exception) {
                    // Fallback or log upload failure, but still proceed to save text updates
                    e.printStackTrace()
                }
            }

            // 2. Commit the new records to your local state framework
            _profileState.value = SettingsState(
                uesrname = newName,
                email = newEmail,
                profileImageUri = cloudImageUrl
            )
        }
    }
}

//class UserProfileRepository {
//    private val _profileState = MutableStateFlow(UserProfileState())
//    val profileState: StateFlow<UserProfileState> = _profileState.asStateFlow()
//
//    fun updateProfile(newName: String, newEmail: String, newImageUri: String?) {
//        _profileState.value = UserProfileState(
//            name = newName,
//            email = newEmail,
//            profileImageUri = newImageUri
//        )
//    }
//}