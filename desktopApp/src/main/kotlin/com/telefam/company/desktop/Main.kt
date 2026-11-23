package com.telefam.company.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.telefam.company.desktop.platform.DesktopSecurityBridge
import com.telefam.company.desktop.ui.screens.DesktopAuthScreen
import com.telefam.company.desktop.ui.screens.DesktopHomeScreen
import com.telefam.company.desktop.ui.screens.DesktopProfileSetupScreen
import com.telefam.company.desktop.ui.theme.TelefamDesktopTheme
import com.telefam.company.desktop.viewmodel.DesktopTelefamViewModel
import com.telefam.company.shared.TelefamStrings
import com.telefam.company.shared.model.AppScreen
import com.telefam.company.shared.model.ThemeMode
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

fun main() = application {
    val bridge = remember { DesktopSecurityBridge() }
    val viewModel = remember { DesktopTelefamViewModel(bridge) }
    val windowState = rememberWindowState(
        placement = WindowPlacement.Floating,
        width = 460.dp,
        height = 840.dp
    )

    Window(
        onCloseRequest = {
            viewModel.close()
            exitApplication()
        },
        title = TelefamStrings.APP_NAME,
        state = windowState
    ) {
        window.minimumSize = Dimension(390, 700)

        val state by viewModel.uiState.collectAsState()
        val contacts by viewModel.contacts.collectAsState()
        val allContacts by viewModel.allCachedContacts.collectAsState()
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

        TelefamDesktopTheme(darkTheme = isDark) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (state.screen) {
                    AppScreen.AUTH_EMAIL,
                    AppScreen.AUTH_OTP -> DesktopAuthScreen(
                        state = state,
                        onEmailChanged = viewModel::onEmailChanged,
                        onSendOtp = viewModel::onSendOtp,
                        onOtpChanged = viewModel::onOtpChanged,
                        onVerifyOtp = viewModel::onVerifyOtp,
                        onChangeEmail = viewModel::onChangeEmail,
                        onContinueWithGoogle = viewModel::onContinueWithGoogle
                    )

                    AppScreen.PROFILE_SETUP -> DesktopProfileSetupScreen(
                        state = state,
                        countries = viewModel.countries,
                        httpClient = bridge.okHttpClient,
                        onPickImageFile = bridge::pickImageFile,
                        onBack = viewModel::onLogout,
                        onFieldChanged = viewModel::onProfileFieldChanged,
                        onUploadCroppedPhoto = viewModel::onUploadCroppedAvatar,
                        onSubmitProfile = viewModel::onSubmitProfile
                    )

                    AppScreen.HOME -> DesktopHomeScreen(
                        state = state,
                        contacts = contacts,
                        allContacts = allContacts,
                        countries = viewModel.countries,
                        httpClient = bridge.okHttpClient,
                        isDark = isDark,
                        onPickImageFile = bridge::pickImageFile,
                        onCycleTheme = viewModel::onCycleTheme,
                        onSelectHomeTab = viewModel::onSelectHomeTab,
                        onSelectContactsTab = viewModel::onSelectContactsTab,
                        onContactsSearchChanged = viewModel::onContactsSearchChanged,
                        onToggleFollowContact = viewModel::onToggleFollowContact,
                        onSelectProfileContentTab = viewModel::onSelectProfileContentTab,
                        onShareProfile = {
                            val display = state.profile.fullName.ifBlank {
                                state.profile.username.ifBlank { TelefamStrings.APP_NAME }
                            }
                            val handle = if (state.profile.username.isNotBlank()) " (@${state.profile.username})" else ""
                            val text = "$display$handle on Telefam"
                            runCatching {
                                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                            }
                            viewModel.onShowNotice(text)
                        },
                        onClearError = viewModel::onClearError,
                        onUploadAvatarPhoto = viewModel::onUploadCroppedAvatar,
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
                        .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
                ) { data ->
                    Snackbar(snackbarData = data)
                }
            }
        }
    }
}
