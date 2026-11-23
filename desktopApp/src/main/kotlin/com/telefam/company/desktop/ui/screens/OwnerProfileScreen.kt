package com.telefam.company.desktop.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.telefam.company.shared.model.BackgroundPhotoItem
import com.telefam.company.shared.model.ContactItem
import com.telefam.company.shared.model.CountryOption
import com.telefam.company.shared.model.ProfileContentTab
import com.telefam.company.shared.model.TelefamUiState
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient

@Composable
fun DesktopOwnerProfileScreen(
    state: TelefamUiState,
    allContacts: List<ContactItem>,
    countries: List<CountryOption>,
    httpClient: OkHttpClient,
    isDark: Boolean,
    onPickImageFile: () -> Pair<ByteArray, String>?,
    onBack: () -> Unit,
    onSelectContentTab: (String) -> Unit,
    onComingSoon: (String) -> Unit,
    onShareProfile: () -> Unit,
    onToggleFollowContact: (ContactItem) -> Unit,
    onClearError: () -> Unit,
    onUploadAvatarPhoto: (ByteArray, String, Double, Double, Double, Double) -> Unit,
    onUploadBackgroundPhoto: (ByteArray, String, Double, Double, Double, Double) -> Unit,
    onDeleteBackgroundPhoto: (String) -> Unit,
    onCheckUsername: (String) -> Unit,
    onSaveProfile: (String, String, String, String, String, String, String, String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val profile = state.profile
    val bgColor = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showEditBackgroundSheet by remember { mutableStateOf(false) }
    var followListInitialTab by remember { mutableStateOf<String?>(null) }

    val followingCount = if (allContacts.isNotEmpty()) allContacts.count { it.iFollow } else profile.followingCount
    val followersCount = if (allContacts.isNotEmpty()) allContacts.count { it.followsMe } else profile.followersCount

    val displayName = profile.fullName.ifBlank { profile.username.ifBlank { TelefamStrings.APP_NAME } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OwnerProfileBackgroundBanner(
                displayName = displayName,
                backgrounds = profile.backgroundPhotos,
                httpClient = httpClient,
                isDark = isDark,
                isUploading = profile.backgroundUploading,
                onBack = onBack,
                onSubscribers = { onComingSoon(TelefamStrings.PROFILE_SUBSCRIBERS) },
                onSettings = { onComingSoon(TelefamStrings.PROFILE_SETTINGS) },
                onMenu = { onComingSoon(TelefamStrings.PROFILE_MENU) },
                onEditBackground = {
                    onClearError()
                    showEditBackgroundSheet = true
                }
            )

            val avatarSize = 96.dp
            val ringBrush = remember {
                Brush.linearGradient(
                    listOf(Color(0xFFE91E63), TelefamCrimson, Color(0xFFFF5722))
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 158.dp)
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(ringBrush)
                    .padding(2.5.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable {
                        onClearError()
                        showEditProfileSheet = true
                    }
                    .testTag("profile_owner_avatar"),
                contentAlignment = Alignment.Center
            ) {
                if (profile.avatarUrl.isNotEmpty()) {
                    DesktopAsyncImage(
                        url = profile.avatarUrl,
                        httpClient = httpClient,
                        contentDescription = displayName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    val initials = displayName.trim().take(2).uppercase().ifEmpty { "TF" }
                    Text(
                        text = initials,
                        color = TelefamCrimson,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "@${profile.username.ifBlank { "user" }}",
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("profile_username_text")
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItem(
                    count = followingCount,
                    label = TelefamStrings.PROFILE_FOLLOWING,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { followListInitialTab = "following" },
                    modifier = Modifier.weight(1f).testTag("profile_following_stat")
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(outlineColor.copy(alpha = 0.55f))
                )
                ProfileStatItem(
                    count = followersCount,
                    label = TelefamStrings.PROFILE_FOLLOWERS,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { followListInitialTab = "followers" },
                    modifier = Modifier.weight(1f).testTag("profile_followers_stat")
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(outlineColor.copy(alpha = 0.55f))
                )
                ProfileStatItem(
                    count = profile.likesCount,
                    label = TelefamStrings.PROFILE_LIKES,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = null,
                    modifier = Modifier.weight(1f).testTag("profile_likes_stat")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onClearError()
                        showEditProfileSheet = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("profile_edit_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelefamCrimson,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = TelefamIcons.Pencil,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = TelefamStrings.PROFILE_EDIT_PROFILE,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onShareProfile,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("profile_share_button"),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, outlineColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = textPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = TelefamIcons.Share2,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = TelefamStrings.PROFILE_SHARE_PROFILE,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            if (profile.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .testTag("profile_location_text")
                ) {
                    Icon(
                        imageVector = TelefamIcons.MapPin,
                        contentDescription = null,
                        tint = TelefamCrimson,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = profile.location,
                        color = textSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (profile.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = profile.bio,
                    color = textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .testTag("profile_bio_text")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ProfileContentTabsRow(
                selectedTab = state.profileContentTab,
                postsCount = profile.postsCount,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                outlineColor = outlineColor,
                onSelectTab = onSelectContentTab
            )

            if (state.profileContentTab == ProfileContentTab.POSTS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onComingSoon("Filter") }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TelefamStrings.PROFILE_ALL_POSTS,
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = TelefamIcons.ChevronDown,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { onComingSoon("Filter") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = TelefamIcons.SlidersHorizontal,
                            contentDescription = "Filter",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = state.profileContentTab,
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    (slideInHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetX = { if (forward) it / 5 else -it / 5 }
                    ) + fadeIn(tween(220)))
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                targetOffsetX = { if (forward) -it / 5 else it / 5 }
                            ) + fadeOut(tween(180))
                        )
                },
                label = "profileContentTabAnim"
            ) { tab ->
                val emptyText = when (tab) {
                    ProfileContentTab.POSTS -> TelefamStrings.PROFILE_NO_POSTS
                    ProfileContentTab.SAVED,
                    ProfileContentTab.REPOSTS,
                    ProfileContentTab.LOCKED -> TelefamStrings.COMING_SOON
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp)
                        .padding(vertical = 44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyText,
                        color = textSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.testTag("profile_tab_empty_text")
                    )
                }
            }
        }
    }

    if (showEditBackgroundSheet) {
        EditBackgroundBottomSheet(
            backgrounds = profile.backgroundPhotos,
            httpClient = httpClient,
            isUploading = profile.backgroundUploading,
            errorMessage = state.errorMessage,
            onPickImageFile = onPickImageFile,
            onDismiss = {
                onClearError()
                showEditBackgroundSheet = false
            },
            onUploadBackground = onUploadBackgroundPhoto,
            onDeleteBackground = onDeleteBackgroundPhoto
        )
    }

    if (showEditProfileSheet) {
        EditProfileBottomSheet(
            profile = profile,
            errorMessage = state.errorMessage,
            countries = countries,
            httpClient = httpClient,
            onPickImageFile = onPickImageFile,
            onDismiss = {
                onClearError()
                showEditProfileSheet = false
            },
            onUploadAvatarPhoto = onUploadAvatarPhoto,
            onCheckUsername = onCheckUsername,
            onSave = { fn, un, iso, phone, gender, dob, loc, bio ->
                val ok = onSaveProfile(fn, un, iso, phone, gender, dob, loc, bio)
                if (ok) {
                    showEditProfileSheet = false
                }
                ok
            }
        )
    }

    followListInitialTab?.let { initialTab ->
        FollowListBottomSheet(
            initialTab = initialTab,
            contacts = allContacts,
            httpClient = httpClient,
            isDark = isDark,
            onDismiss = { followListInitialTab = null },
            onToggleFollow = onToggleFollowContact
        )
    }
}

@Composable
private fun OwnerProfileBackgroundBanner(
    displayName: String,
    backgrounds: List<BackgroundPhotoItem>,
    httpClient: OkHttpClient,
    isDark: Boolean,
    isUploading: Boolean,
    onBack: () -> Unit,
    onSubscribers: () -> Unit,
    onSettings: () -> Unit,
    onMenu: () -> Unit,
    onEditBackground: () -> Unit
) {
    var activeIndex by remember(backgrounds.size) { mutableIntStateOf(0) }

    LaunchedEffect(backgrounds) {
        if (backgrounds.size > 1) {
            while (true) {
                delay(5000L)
                activeIndex = (activeIndex + 1) % backgrounds.size
            }
        } else {
            activeIndex = 0
        }
    }

    val infiniteMotion = rememberInfiniteTransition(label = "bgMotion")
    val ambientScale by infiniteMotion.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAmbientScale"
    )

    val fallbackBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF0E0F12), Color(0xFF1D1F26), Color(0xFF2B2D36)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFF1A1B1E), Color(0xFF2D2F34), Color(0xFF565961)))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(fallbackBrush)
            .testTag("profile_background_banner")
    ) {
        if (backgrounds.isNotEmpty()) {
            val safeIdx = activeIndex.coerceIn(0, backgrounds.lastIndex)
            AnimatedContent(
                targetState = backgrounds[safeIdx],
                transitionSpec = {
                    (fadeIn(tween(750)) + slideInHorizontally(tween(750)) { it / 12 })
                        .togetherWith(
                            fadeOut(tween(650)) + slideOutHorizontally(tween(650)) { -it / 12 }
                        )
                },
                label = "backgroundCarousel",
                modifier = Modifier.fillMaxSize()
            ) { photo ->
                DesktopAsyncImage(
                    url = photo.imageUrl,
                    httpClient = httpClient,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(ambientScale)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.48f),
                            Color.Black.copy(alpha = 0.14f),
                            Color.Black.copy(alpha = 0.42f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BannerCircleIconButton(
                icon = TelefamIcons.ArrowLeft,
                contentDescription = TelefamStrings.CANCEL,
                onClick = onBack,
                modifier = Modifier.testTag("profile_back_button")
            )

            Text(
                text = displayName,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("profile_display_name")
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BannerCircleIconButton(
                    icon = TelefamIcons.Crown,
                    contentDescription = TelefamStrings.PROFILE_SUBSCRIBERS,
                    onClick = onSubscribers,
                    modifier = Modifier.testTag("profile_subscribers_button")
                )
                BannerCircleIconButton(
                    icon = TelefamIcons.Settings,
                    contentDescription = TelefamStrings.PROFILE_SETTINGS,
                    onClick = onSettings,
                    modifier = Modifier.testTag("profile_settings_button")
                )
                BannerCircleIconButton(
                    icon = TelefamIcons.Menu,
                    contentDescription = TelefamStrings.PROFILE_MENU,
                    onClick = onMenu,
                    modifier = Modifier.testTag("profile_menu_button")
                )
            }
        }

        val indicatorCount = backgrounds.size.coerceAtLeast(1)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 22.dp)
                .testTag("profile_bg_indicators"),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(indicatorCount) { idx ->
                val isSelected = idx == (activeIndex % indicatorCount)
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 18.dp else 6.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(dotWidth)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color.White else Color.White.copy(alpha = 0.45f)
                        )
                        .clickable(enabled = backgrounds.size > 1) { activeIndex = idx }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 8.dp)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onEditBackground)
                .testTag("profile_edit_background_button"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.62f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = TelefamIcons.Camera,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = TelefamStrings.PROFILE_EDIT_BACKGROUND,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileStatItem(
    count: Int,
    label: String,
    textPrimary: Color,
    textSecondary: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val clickableMod = if (onClick != null) {
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    } else {
        modifier.heightIn(min = 48.dp)
    }
    Column(
        modifier = clickableMod.padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = count.toString(), color = textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = textSecondary, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun ProfileContentTabsRow(
    selectedTab: ProfileContentTab,
    postsCount: Int,
    textPrimary: Color,
    textSecondary: Color,
    outlineColor: Color,
    onSelectTab: (String) -> Unit
) {
    val items = listOf(
        Triple(ProfileContentTab.POSTS, "posts", TelefamIcons.LayoutGrid to "${TelefamStrings.PROFILE_TAB_POSTS} $postsCount"),
        Triple(ProfileContentTab.SAVED, "saved", TelefamIcons.Bookmark to TelefamStrings.PROFILE_TAB_SAVED),
        Triple(ProfileContentTab.REPOSTS, "reposts", TelefamIcons.Repeat2 to TelefamStrings.PROFILE_TAB_REPOSTS),
        Triple(ProfileContentTab.LOCKED, "locked", TelefamIcons.Lock to TelefamStrings.PROFILE_TAB_LOCKED)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (tabEnum, key, iconLabel) ->
                val (icon, label) = iconLabel
                val selected = selectedTab == tabEnum
                val tint by animateColorAsState(
                    targetValue = if (selected) textPrimary else textSecondary,
                    animationSpec = tween(200)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { onSelectTab(key) }
                        .testTag("profile_tab_$key"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = label,
                            color = tint,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (selected) TelefamCrimson else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(color = outlineColor.copy(alpha = 0.35f))
    }
}
