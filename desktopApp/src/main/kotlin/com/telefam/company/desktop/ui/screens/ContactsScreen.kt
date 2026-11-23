package com.telefam.company.desktop.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.desktop.ui.components.DesktopAsyncImage
import com.telefam.company.desktop.ui.icons.TelefamIcons
import com.telefam.company.desktop.ui.theme.TelefamCrimson
import com.telefam.company.desktop.ui.theme.TelefamSuccess
import com.telefam.company.shared.TelefamStrings
import com.telefam.company.shared.model.ContactItem
import com.telefam.company.shared.model.ContactsTab
import com.telefam.company.shared.model.ThemeMode
import okhttp3.OkHttpClient

@Composable
fun DesktopContactsScreenContent(
    contacts: List<ContactItem>,
    selectedTab: ContactsTab,
    searchQuery: String,
    themeMode: ThemeMode,
    isDark: Boolean,
    httpClient: OkHttpClient,
    onSearchQueryChanged: (String) -> Unit,
    onSelectTab: (String) -> Unit,
    onCycleTheme: () -> Unit,
    onToggleFollow: (ContactItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val searchBg = if (isDark) Color(0xFF1F2026) else Color.White
    val searchBorder = if (isDark) Color(0xFF2D2E35) else Color.White.copy(alpha = 0.7f)
    val chipBg = if (isDark) Color(0xFF22181A) else Color(0xFFA70B14)
    val chipBorder = if (isDark) Color(0xFF4B2428) else Color.White.copy(alpha = 0.25f)

    Column(
        modifier = modifier
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
                    .height(48.dp),
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
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = TelefamStrings.CONTACTS_SEARCH_HINT,
                                color = textSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            singleLine = true,
                            textStyle = TextStyle(color = textPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(TelefamCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contacts_search_input")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val themeLabel = when (themeMode) {
                ThemeMode.SYSTEM -> "System"
                ThemeMode.LIGHT -> "Light"
                ThemeMode.DARK -> "Dark"
            }
            val themeIcon = when (themeMode) {
                ThemeMode.SYSTEM -> TelefamIcons.Monitor
                ThemeMode.LIGHT -> TelefamIcons.Sun
                ThemeMode.DARK -> TelefamIcons.Moon
            }
            Surface(
                onClick = onCycleTheme,
                modifier = Modifier
                    .height(40.dp)
                    .testTag("contacts_theme_toggle"),
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
        }

        val tabs = listOf(
            ContactsTab.ALL to ("all" to TelefamStrings.CONTACTS_TAB_ALL),
            ContactsTab.SUGGESTIONS to ("suggestions" to TelefamStrings.CONTACTS_TAB_SUGGESTIONS),
            ContactsTab.FOLLOWERS to ("followers" to TelefamStrings.CONTACTS_TAB_FOLLOWERS),
            ContactsTab.FOLLOWING to ("following" to TelefamStrings.CONTACTS_TAB_FOLLOWING),
            ContactsTab.FRIENDS to ("friends" to TelefamStrings.CONTACTS_TAB_FRIENDS)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tabEnum, keyLabel) ->
                val (key, label) = keyLabel
                val isSelected = selectedTab == tabEnum
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )
                val pillTextColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF1A1C1E) else Color.White,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )
                Surface(
                    onClick = { onSelectTab(key) },
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("contacts_tab_$key"),
                    shape = RoundedCornerShape(18.dp),
                    color = pillBg
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = pillTextColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(bgColor),
            contentAlignment = Alignment.TopCenter
        ) {
            val sectionHeader = when (selectedTab) {
                ContactsTab.ALL -> TelefamStrings.CONTACTS_TAB_ALL.uppercase()
                ContactsTab.SUGGESTIONS -> TelefamStrings.CONTACTS_TAB_SUGGESTIONS.uppercase()
                ContactsTab.FOLLOWERS -> TelefamStrings.CONTACTS_TAB_FOLLOWERS.uppercase()
                ContactsTab.FOLLOWING -> TelefamStrings.CONTACTS_TAB_FOLLOWING.uppercase()
                ContactsTab.FRIENDS -> TelefamStrings.CONTACTS_TAB_FRIENDS.uppercase()
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "section_header") {
                    Text(
                        text = sectionHeader,
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .testTag("contacts_section_header")
                    )
                }

                if (contacts.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = TelefamIcons.Users,
                                    contentDescription = null,
                                    tint = textSecondary.copy(alpha = 0.65f),
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = TelefamStrings.CONTACTS_EMPTY,
                                    color = textSecondary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.testTag("contacts_empty_text")
                                )
                            }
                        }
                    }
                } else {
                    items(contacts, key = { it.userId }) { contact ->
                        ContactListRow(
                            contact = contact,
                            isDark = isDark,
                            httpClient = httpClient,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onToggleFollow = { onToggleFollow(contact) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactListRow(
    contact: ContactItem,
    isDark: Boolean,
    httpClient: OkHttpClient,
    textPrimary: Color,
    textSecondary: Color,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarBg = if (isDark) Color(0xFF1A2D3D) else Color(0xFFE7EFF7)
    val avatarFg = if (isDark) Color(0xFF8CC4FF) else Color(0xFF1D4E7A)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("contact_row_${contact.userId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (contact.avatarUrl.isNotEmpty()) {
            DesktopAsyncImage(
                url = contact.avatarUrl,
                httpClient = httpClient,
                contentDescription = contact.fullName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        } else {
            val initials = contact.fullName
                .trim()
                .take(2)
                .uppercase()
                .ifEmpty { contact.username.take(2).uppercase() }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = avatarFg,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.fullName.ifBlank { contact.username },
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "@${contact.username} \u00B7 ${contact.countryName}",
                color = textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        ContactRelationshipButton(
            contact = contact,
            isDark = isDark,
            onClick = onToggleFollow
        )
    }
}

@Composable
private fun ContactRelationshipButton(
    contact: ContactItem,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val isFriend = contact.isFriend || (contact.iFollow && contact.followsMe)
    val isFollowingOnly = contact.iFollow && !contact.followsMe
    val isFollowBack = !contact.iFollow && contact.followsMe

    val label = when {
        isFriend -> TelefamStrings.CONTACTS_ACTION_FRIENDS
        isFollowingOnly -> TelefamStrings.CONTACTS_ACTION_FOLLOWING
        isFollowBack -> TelefamStrings.CONTACTS_ACTION_FOLLOW_BACK
        else -> TelefamStrings.CONTACTS_ACTION_FOLLOW
    }

    val icon = when {
        isFriend -> TelefamIcons.Users
        isFollowingOnly -> TelefamIcons.UserCheck
        else -> TelefamIcons.UserPlus
    }

    val targetContainer = when {
        isFriend -> if (isDark) Color(0x2616A34A) else Color(0x1816A34A)
        isFollowingOnly -> if (isDark) Color(0xFF1F2026) else Color(0xFFF3F4F6)
        else -> if (isDark) Color(0x24C80E18) else Color(0x14C80E18)
    }
    val targetBorder = when {
        isFriend -> TelefamSuccess
        isFollowingOnly -> if (isDark) Color(0xFF3E4049) else Color(0xFFD1D5DB)
        else -> TelefamCrimson
    }
    val targetContent = when {
        isFriend -> if (isDark) Color(0xFF4ADE80) else TelefamSuccess
        isFollowingOnly -> MaterialTheme.colorScheme.onSurface
        else -> if (isDark) Color(0xFFFF6B73) else TelefamCrimson
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )
    val borderColor by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )
    val contentColor by animateColorAsState(
        targetValue = targetContent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )
    val scale by animateFloatAsState(
        targetValue = if (isFriend) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = Modifier
            .height(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag("contact_action_${contact.userId}"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}
