package com.mhq.fynecast.auth.ui.editprofile.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.ui.editprofile.ui.EditProfileViewModel
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.launch

@Composable
fun EditProfileContainer(
    editProfileViewModel: EditProfileViewModel = viewModel(factory = AppViewModelFactory),
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val initialProfile by editProfileViewModel.initialProfile.collectAsStateWithLifecycle()
    var profileImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var username by rememberSaveable { mutableStateOf("") }
    var userEmail by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isSeeded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(initialProfile) {
        val profile = initialProfile ?: return@LaunchedEffect
        if (!isSeeded) {
            username = profile.name
            userEmail = profile.email
            profileImageUri = profile.profileImageUri
            isSeeded = true
        }
    }

    val isUsernameValid = editProfileViewModel.isUsernameValid(username)
    val isEmailValid = editProfileViewModel.isEmailValid(userEmail)
    // Social accounts have no password yet: switching the email needs one for the new email.
    val showPasswordFields = initialProfile?.let {
        !it.hasPasswordLogin && !userEmail.equals(it.email, ignoreCase = true)
    } ?: false
    val isNewPasswordValid = editProfileViewModel.isPasswordValid(newPassword)
    val doPasswordsMatch = editProfileViewModel.doPasswordsMatch(newPassword, confirmPassword)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        editProfileViewModel.navigationEvent.collect {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        editProfileViewModel.uiEvent.collect { errorMessage ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = errorMessage.asString(context),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        EditProfileContent(
            username = username,
            userEmail = userEmail,
            isUsernameValid = isUsernameValid,
            isEmailValid = isEmailValid,
            onUsernameChanged = { username = it },
            onUserEmailChanged = { userEmail = it.trim() },
            showPasswordFields = showPasswordFields,
            newPassword = newPassword,
            isNewPasswordValid = isNewPasswordValid,
            onNewPasswordChanged = { newPassword = it },
            confirmPassword = confirmPassword,
            doPasswordsMatch = doPasswordsMatch,
            onConfirmPasswordChanged = { confirmPassword = it },
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
            onBackClick = onBackClick,
            onSaveClick = {
                editProfileViewModel.onSaveProfileAction(
                    username,
                    userEmail,
                    profileImageUri,
                    newPassword.takeIf { showPasswordFields },
                    confirmPassword.takeIf { showPasswordFields }
                )
            },
            contentPadding = contentPadding,
            modifier = modifier,
            profileImageUri = profileImageUri,
            onProfileImagePicked = {
                profileImageUri = it
            }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) { data ->
            AuthWarningSnackbar(data)
        }
    }
}