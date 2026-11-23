package com.telefam.company.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.telefam.company.app.nativecore.AppScreen
import com.telefam.company.app.nativecore.ThemeMode
import com.telefam.company.app.ui.screens.AuthScreen
import com.telefam.company.app.ui.screens.HomeScreen
import com.telefam.company.app.ui.screens.ProfileSetupScreen
import com.telefam.company.app.ui.theme.MyApplicationTheme
import com.telefam.company.app.viewmodel.TelefamViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TelefamViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val contacts by viewModel.contacts.collectAsStateWithLifecycle()
            val allContacts by viewModel.allCachedContacts.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (state.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(state.noticeMessage) {
                if (state.noticeMessage.isNotEmpty()) {
                    snackbarHostState.showSnackbar(state.noticeMessage)
                    viewModel.onClearNotice()
                }
            }

            MyApplicationTheme(darkTheme = isDark) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (state.screen) {
                        AppScreen.AUTH_EMAIL,
                        AppScreen.AUTH_OTP -> AuthScreen(
                            state = state,
                            onEmailChanged = viewModel::onEmailChanged,
                            onSendOtp = viewModel::onSendOtp,
                            onOtpChanged = viewModel::onOtpChanged,
                            onVerifyOtp = viewModel::onVerifyOtp,
                            onChangeEmail = viewModel::onChangeEmail,
                            onContinueWithGoogle = viewModel::onContinueWithGoogle
                        )

                        AppScreen.PROFILE_SETUP -> ProfileSetupScreen(
                            state = state,
                            countries = viewModel.countries,
                            onBack = viewModel::onLogout,
                            onFieldChanged = viewModel::onProfileFieldChanged,
                            onUploadCroppedPhoto = viewModel::onUploadCroppedPhoto,
                            onSubmitProfile = viewModel::onSubmitProfile
                        )

                        AppScreen.HOME -> HomeScreen(
                            state = state,
                            contacts = contacts,
                            allContacts = allContacts,
                            countries = viewModel.countries,
                            isDark = isDark,
                            onCycleTheme = viewModel::onCycleTheme,
                            onSelectHomeTab = viewModel::onSelectHomeTab,
                            onSelectContactsTab = viewModel::onSelectContactsTab,
                            onContactsSearchChanged = viewModel::onContactsSearchChanged,
                            onToggleFollowContact = viewModel::onToggleFollowContact,
                            onSelectProfileContentTab = viewModel::onSelectProfileContentTab,
                            onShareProfile = {
                                shareOwnerProfile(state.profile.fullName, state.profile.username)
                            },
                            onClearError = viewModel::onClearError,
                            onUploadAvatarPhoto = viewModel::onUploadCroppedPhoto,
                            onUploadBackgroundPhoto = viewModel::onUploadBackgroundPhoto,
                            onDeleteBackgroundPhoto = viewModel::onDeleteBackgroundPhoto,
                            onCheckUsername = viewModel::onCheckUsername,
                            onSaveProfile = viewModel::onSaveEditedProfile,
                            onComingSoon = viewModel::onComingSoon
                        )
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
                    ) { data ->
                        Snackbar(snackbarData = data)
                    }
                }
            }
        }
    }

    private fun shareOwnerProfile(fullName: String, username: String) {
        val display = fullName.ifBlank { username.ifBlank { "Telefam" } }
        val handle = if (username.isNotBlank()) " (@$username)" else ""
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, display)
            putExtra(Intent.EXTRA_TEXT, "$display$handle on Telefam")
        }
        runCatching {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.telefam_profile_share_profile)))
        }
    }
}
