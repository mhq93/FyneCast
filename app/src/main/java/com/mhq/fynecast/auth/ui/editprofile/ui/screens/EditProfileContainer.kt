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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.ui.editprofile.ui.EditProfileViewModel
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.launch

@Composable
fun EditProfileContainer(
    editProfileViewModel: EditProfileViewModel = viewModel(factory = EditProfileViewModel.factory),
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val profileImageUri by editProfileViewModel.profileImageUri.collectAsState()
    val username by editProfileViewModel.username.collectAsState()
    val isUsernameValid by editProfileViewModel.isUsernameValid.collectAsState()
    val userEmail by editProfileViewModel.userEmail.collectAsState()
    val isEmailValid by editProfileViewModel.isEmailValid.collectAsState()

    val context = LocalContext.current
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
            onUsernameChanged = editProfileViewModel::onUsernameChanged,
            onUserEmailChanged = editProfileViewModel::onUserEmailChanged,
            onBackClick = onBackClick,
            onSaveClick = {
                editProfileViewModel.handleSavingProfileChanges()
            },
            contentPadding = contentPadding,
            modifier = modifier,
            profileImageUri = profileImageUri,
            onProfileImagePicked = editProfileViewModel::onProfileImagePicked
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) {
            data -> AuthWarningSnackbar(data)
        }
    }
}