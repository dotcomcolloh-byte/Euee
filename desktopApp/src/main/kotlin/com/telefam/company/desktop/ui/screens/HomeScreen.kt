package com.telefam.company.desktop.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.desktop.ui.components.DesktopAsyncImage
import com.telefam.company.desktop.ui.icons.TelefamIcons
import com.telefam.company.desktop.ui.theme.TelefamCrimson
import com.telefam.company.shared.TelefamStrings
import com.telefam.company.shared.model.ContactItem
import com.telefam.company.shared.model.CountryOption
import com.telefam.company.shared.model.HomeTab
import com.telefam.company.shared.model.TelefamUiState
import com.telefam.company.shared.model.ThemeMode
import okhttp3.OkHttpClient

@Composable
fun DesktopHomeScreen(
    state: TelefamUiState,
    contacts: List<ContactItem>,
    allContacts: List<ContactItem>,
    countries: List<CountryOption>,
    httpClient: OkHttpClient,
    isDark: Boolean,
    onPickImageFile: () -> Pair<ByteArray, String>?,
    onCycleTheme: () -> Unit,
    onSelectHomeTab: (HomeTab) -> Unit,
    onSelectContactsTab: (String) -> Unit,
    onContactsSearchChanged: (String) -> Unit,
    onToggleFollowContact: (ContactItem) -> Unit,
    onSelectProfileContentTab: (String) -> Unit,
    onShareProfile: () -> Unit,
    onClearError: () -> Unit,
    onUploadAvatarPhoto: (ByteArray, String, Double, Double, Double, Double) -> Unit,
    onUploadBackgroundPhoto: (ByteArray, String, Double, Double, Double, Double) -> Unit,
    onDeleteBackgroundPhoto: (String) -> Unit,
    onCheckUsername: (String) -> Unit,
    onSaveProfile: (String, String, String, String, String, String, String, String) -> Boolean,
    onComingSoon: (String) -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AnimatedContent(
            targetState = state.homeTab,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                (slideInHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    initialOffsetX = { full -> if (forward) full / 4 else -full / 4 }
                ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                    .togetherWith(
                        slideOutHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            targetOffsetX = { full -> if (forward) -full / 4 else full / 4 }
                        ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                    )
            },
            label = "homeTabTransition"
        ) { activeTab ->
            when (activeTab) {
                HomeTab.CONTACTS -> DesktopContactsScreenContent(
                    contacts = contacts,
                    selectedTab = state.contactsTab,
                    searchQuery = state.contactsSearchQuery,
                    themeMode = state.themeMode,
                    isDark = isDark,
                    httpClient = httpClient,
                    onSearchQueryChanged = onContactsSearchChanged,
                    onSelectTab = onSelectContactsTab,
                    onCycleTheme = onCycleTheme,
                    onToggleFollow = onToggleFollowContact
                )

                HomeTab.PROFILE -> DesktopOwnerProfileScreen(
                    state = state,
                    allContacts = allContacts,
                    countries = countries,
                    httpClient = httpClient,
                    isDark = isDark,
                    onPickImageFile = onPickImageFile,
                    onBack = { onSelectHomeTab(HomeTab.CHATS) },
                    onSelectContentTab = onSelectProfileContentTab,
                    onComingSoon = onComingSoon,
                    onShareProfile = onShareProfile,
                    onToggleFollowContact = onToggleFollowContact,
                    onClearError = onClearError,
                    onUploadAvatarPhoto = onUploadAvatarPhoto,
                    onUploadBackgroundPhoto = onUploadBackgroundPhoto,
                    onDeleteBackgroundPhoto = onDeleteBackgroundPhoto,
                    onCheckUsername = onCheckUsername,
                    onSaveProfile = onSaveProfile
                )

                HomeTab.CHATS -> ChatsHomeContent(
                    state = state,
                    isDark = isDark,
                    onCycleTheme = onCycleTheme,
                    onComingSoon = onComingSoon
                )
            }
        }

        HomeBottomNavigationBar(
            activeTab = state.homeTab,
            surfaceColor = surfaceColor,
            inactiveColor = textSecondary,
            avatarUrl = state.profile.avatarUrl,
            httpClient = httpClient,
            onSelectHomeTab = onSelectHomeTab,
            onComingSoon = onComingSoon
        )
    }
}

@Composable
private fun ChatsHomeContent(
    state: TelefamUiState,
    isDark: Boolean,
    onCycleTheme: () -> Unit,
    onComingSoon: (String) -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    val searchBg = if (isDark) Color(0xFF1F2026) else Color.White
    val searchBorder = if (isDark) Color(0xFF2D2E35) else Color.White.copy(alpha = 0.6f)
    val chipBg = if (isDark) Color(0xFF22181A) else Color(0xFFA70B14)
    val chipBorder = if (isDark) Color(0xFF4B2428) else Color.White.copy(alpha = 0.25f)
    val emptyCircleBg = if (isDark) Color(0xFF2B1215) else Color(0xFFFDECEC)
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TelefamCrimson)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { onComingSoon("Search") }
                    .testTag("home_search_bar"),
                shape = RoundedCornerShape(24.dp),
                color = searchBg,
                border = BorderStroke(1.dp, searchBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = TelefamIcons.Search,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = TelefamStrings.SEARCH_HINT,
                        color = textSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val themeLabel = when (state.themeMode) {
                ThemeMode.SYSTEM -> "System"
                ThemeMode.LIGHT -> "Light"
                ThemeMode.DARK -> "Dark"
            }
            val themeIcon = when (state.themeMode) {
                ThemeMode.SYSTEM -> TelefamIcons.Monitor
                ThemeMode.LIGHT -> TelefamIcons.Sun
                ThemeMode.DARK -> TelefamIcons.Moon
            }
            Surface(
                modifier = Modifier
                    .height(40.dp)
                    .clickable { onCycleTheme() }
                    .testTag("home_theme_toggle"),
                shape = RoundedCornerShape(20.dp),
                color = chipBg,
                border = BorderStroke(1.dp, chipBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = themeIcon,
                        contentDescription = themeLabel,
                        tint = if (isDark) TelefamCrimson else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = themeLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = { onComingSoon("Notifications") },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("home_notifications_button")
            ) {
                Icon(
                    imageVector = TelefamIcons.Bell,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }

            IconButton(
                onClick = { onComingSoon("Menu") },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("home_more_button")
            ) {
                Icon(
                    imageVector = TelefamIcons.MoreVertical,
                    contentDescription = "More",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = bgColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(emptyCircleBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = TelefamIcons.Messages,
                            contentDescription = null,
                            tint = TelefamCrimson,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = TelefamStrings.START_CONVERSATION,
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = TelefamStrings.START_CONVERSATION_SUBTITLE,
                        color = textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Button(
                        onClick = { onComingSoon("Start chat") },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("start_chat_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TelefamCrimson,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = TelefamIcons.MessagePlus,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = TelefamStrings.START_CHAT,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeBottomNavigationBar(
    activeTab: HomeTab,
    surfaceColor: Color,
    inactiveColor: Color,
    avatarUrl: String,
    httpClient: OkHttpClient,
    onSelectHomeTab: (HomeTab) -> Unit,
    onComingSoon: (String) -> Unit
) {
    val chatsColor = if (activeTab == HomeTab.CHATS) TelefamCrimson else inactiveColor
    val contactsColor = if (activeTab == HomeTab.CONTACTS) TelefamCrimson else inactiveColor
    val profileColor = if (activeTab == HomeTab.PROFILE) TelefamCrimson else inactiveColor

    Surface(
        color = surfaceColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem(
                    label = TelefamStrings.NAV_CHATS,
                    color = chatsColor,
                    onClick = { onSelectHomeTab(HomeTab.CHATS) },
                    modifier = Modifier.weight(1f).testTag("nav_chats")
                ) {
                    Icon(
                        imageVector = TelefamIcons.Messages,
                        contentDescription = TelefamStrings.NAV_CHATS,
                        tint = chatsColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                BottomNavItem(
                    label = TelefamStrings.NAV_CONTACTS,
                    color = contactsColor,
                    onClick = { onSelectHomeTab(HomeTab.CONTACTS) },
                    modifier = Modifier.weight(1f).testTag("nav_contacts")
                ) {
                    Icon(
                        imageVector = TelefamIcons.Users,
                        contentDescription = TelefamStrings.NAV_CONTACTS,
                        tint = contactsColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = (-8).dp)
                        .clickable { onComingSoon("Post") }
                        .testTag("nav_post"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = TelefamCrimson,
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = TelefamIcons.Plus,
                                contentDescription = TelefamStrings.NAV_POST,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = TelefamStrings.NAV_POST,
                        color = inactiveColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                BottomNavItem(
                    label = TelefamStrings.NAV_FEEDS,
                    color = inactiveColor,
                    onClick = { onComingSoon("Feeds") },
                    modifier = Modifier.weight(1f).testTag("nav_feeds")
                ) {
                    Icon(
                        imageVector = TelefamIcons.Layers,
                        contentDescription = TelefamStrings.NAV_FEEDS,
                        tint = inactiveColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                BottomNavItem(
                    label = TelefamStrings.NAV_PROFILE,
                    color = profileColor,
                    onClick = { onSelectHomeTab(HomeTab.PROFILE) },
                    modifier = Modifier.weight(1f).testTag("nav_profile")
                ) {
                    if (avatarUrl.isNotEmpty()) {
                        DesktopAsyncImage(
                            url = avatarUrl,
                            httpClient = httpClient,
                            contentDescription = TelefamStrings.NAV_PROFILE,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(if (activeTab == HomeTab.PROFILE) 2.dp else 1.dp, TelefamCrimson, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (activeTab == HomeTab.PROFILE) 1.5.dp else 1.2.dp,
                                    color = if (activeTab == HomeTab.PROFILE) TelefamCrimson else profileColor.copy(alpha = 0.7f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = TelefamIcons.User,
                                contentDescription = TelefamStrings.NAV_PROFILE,
                                tint = profileColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .height(54.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
