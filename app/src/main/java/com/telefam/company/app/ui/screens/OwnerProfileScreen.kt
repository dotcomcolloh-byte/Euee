package com.telefam.company.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telefam.company.app.R
import com.telefam.company.app.nativecore.BackgroundPhotoItem
import com.telefam.company.app.nativecore.ContactItem
import com.telefam.company.app.nativecore.CountryOption
import com.telefam.company.app.nativecore.ProfileContentTab
import com.telefam.company.app.nativecore.ProfileUiState
import com.telefam.company.app.nativecore.TelefamUiState
import com.telefam.company.app.ui.components.PhotoCropDialog
import com.telefam.company.app.ui.icons.CountryFlagBadge
import com.telefam.company.app.ui.icons.TelefamIcons
import com.telefam.company.app.ui.theme.TelefamCrimson
import com.telefam.company.app.ui.theme.TelefamSuccess
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun OwnerProfileScreen(
    state: TelefamUiState,
    allContacts: List<ContactItem>,
    countries: List<CountryOption>,
    isDark: Boolean,
    onBack: () -> Unit,
    onSelectContentTab: (String) -> Unit,
    onComingSoon: (String) -> Unit,
    onShareProfile: () -> Unit,
    onToggleFollowContact: (ContactItem) -> Unit,
    onClearError: () -> Unit,
    onUploadAvatarPhoto: (Uri, Double, Double, Double, Double) -> Unit,
    onUploadBackgroundPhoto: (Uri, Double, Double, Double, Double) -> Unit,
    onDeleteBackgroundPhoto: (String) -> Unit,
    onCheckUsername: (String) -> Unit = {},
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

    val followingCount = if (allContacts.isNotEmpty()) {
        allContacts.count { it.iFollow }
    } else {
        profile.followingCount
    }
    val followersCount = if (allContacts.isNotEmpty()) {
        allContacts.count { it.followsMe }
    } else {
        profile.followersCount
    }

    val scrollState = rememberScrollState()
    val displayName = profile.fullName.ifBlank { profile.username.ifBlank { "Telefam" } }
    val subscribersLabel = stringResource(R.string.telefam_profile_subscribers)
    val settingsLabel = stringResource(R.string.telefam_profile_settings)
    val menuLabel = stringResource(R.string.telefam_profile_menu)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Banner + overlapping centered circular avatar
        Box(modifier = Modifier.fillMaxWidth()) {
            OwnerProfileBackgroundBanner(
                displayName = displayName,
                backgrounds = profile.backgroundPhotos,
                isDark = isDark,
                isUploading = profile.backgroundUploading,
                onBack = onBack,
                onSubscribers = { onComingSoon(subscribersLabel) },
                onSettings = { onComingSoon(settingsLabel) },
                onMenu = { onComingSoon(menuLabel) },
                onEditBackground = {
                    onClearError()
                    showEditBackgroundSheet = true
                }
            )

            // Centered overlapping circular avatar with pink-crimson gradient ring
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
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    val initials = displayName
                        .trim()
                        .take(2)
                        .uppercase()
                        .ifEmpty { "TF" }
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

            // 3-Column Stats: Following | Followers | Likes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItem(
                    count = followingCount,
                    label = stringResource(R.string.telefam_profile_following),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { followListInitialTab = "following" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_following_stat")
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(outlineColor.copy(alpha = 0.55f))
                )
                ProfileStatItem(
                    count = followersCount,
                    label = stringResource(R.string.telefam_profile_followers),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { followListInitialTab = "followers" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_followers_stat")
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(outlineColor.copy(alpha = 0.55f))
                )
                ProfileStatItem(
                    count = profile.likesCount,
                    label = stringResource(R.string.telefam_profile_likes),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_likes_stat")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Edit profile + Share profile buttons
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
                        text = stringResource(R.string.telefam_profile_edit_profile),
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
                        text = stringResource(R.string.telefam_profile_share_profile),
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

            // 4 Shortcut Content Tabs: Posts 0, Saved, Reposts, Locked
            ProfileContentTabsRow(
                selectedTab = state.profileContentTab,
                postsCount = profile.postsCount,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                outlineColor = outlineColor,
                onSelectTab = onSelectContentTab
            )

            // Filter row: All posts v ... Filter sliders (Posts tab)
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
                            text = stringResource(R.string.telefam_profile_all_posts),
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
                    ProfileContentTab.POSTS -> stringResource(R.string.telefam_profile_no_posts)
                    ProfileContentTab.SAVED,
                    ProfileContentTab.REPOSTS,
                    ProfileContentTab.LOCKED -> stringResource(R.string.telefam_coming_soon)
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
            isUploading = profile.backgroundUploading,
            errorMessage = state.errorMessage,
            isDark = isDark,
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
            isDark = isDark,
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
            Brush.verticalGradient(
                listOf(Color(0xFF0E0F12), Color(0xFF1D1F26), Color(0xFF2B2D36))
            )
        } else {
            Brush.verticalGradient(
                listOf(Color(0xFF1A1B1E), Color(0xFF2D2F34), Color(0xFF565961))
            )
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
                AsyncImage(
                    model = photo.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(ambientScale)
                )
            }
        }

        // Top/bottom dark gradient scrim so overlay controls remain crisp on any photo
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

        // Top navigation & action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BannerCircleIconButton(
                icon = TelefamIcons.ArrowLeft,
                contentDescription = stringResource(R.string.telefam_cancel),
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
                    contentDescription = stringResource(R.string.telefam_profile_subscribers),
                    onClick = onSubscribers,
                    modifier = Modifier.testTag("profile_subscribers_button")
                )
                BannerCircleIconButton(
                    icon = TelefamIcons.Settings,
                    contentDescription = stringResource(R.string.telefam_profile_settings),
                    onClick = onSettings,
                    modifier = Modifier.testTag("profile_settings_button")
                )
                BannerCircleIconButton(
                    icon = TelefamIcons.Menu,
                    contentDescription = stringResource(R.string.telefam_profile_menu),
                    onClick = onMenu,
                    modifier = Modifier.testTag("profile_menu_button")
                )
            }
        }

        // Left animated carousel dot/pill indicators
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
                    ),
                    label = "dotW"
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

        // Bottom-right "Edit background" dark pill button
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
                        text = stringResource(R.string.telefam_profile_edit_background),
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
        Text(
            text = count.toString(),
            color = textPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = textSecondary,
            fontSize = 12.sp,
            maxLines = 1
        )
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
        Triple(
            ProfileContentTab.POSTS,
            "posts",
            TelefamIcons.LayoutGrid to "${stringResource(R.string.telefam_profile_tab_posts)} $postsCount"
        ),
        Triple(
            ProfileContentTab.SAVED,
            "saved",
            TelefamIcons.Bookmark to stringResource(R.string.telefam_profile_tab_saved)
        ),
        Triple(
            ProfileContentTab.REPOSTS,
            "reposts",
            TelefamIcons.Repeat2 to stringResource(R.string.telefam_profile_tab_reposts)
        ),
        Triple(
            ProfileContentTab.LOCKED,
            "locked",
            TelefamIcons.Lock to stringResource(R.string.telefam_profile_tab_locked)
        )
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
                    animationSpec = tween(200),
                    label = "tabTint"
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
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = tint,
                            modifier = Modifier.size(16.dp)
                        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBackgroundBottomSheet(
    backgrounds: List<BackgroundPhotoItem>,
    isUploading: Boolean,
    errorMessage: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onUploadBackground: (Uri, Double, Double, Double, Double) -> Unit,
    onDeleteBackground: (String) -> Unit
) {
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingCropUri = uri
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.telefam_profile_backgrounds_title)} (${backgrounds.size}/5)",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = TelefamIcons.X,
                        contentDescription = stringResource(R.string.telefam_cancel),
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = TelefamCrimson,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .testTag("background_error_text")
                )
            }

            if (backgrounds.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(backgrounds, key = { it.id }) { bg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, outlineColor, RoundedCornerShape(14.dp))
                        ) {
                            AsyncImage(
                                model = bg.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { onDeleteBackground(bg.id) },
                                enabled = !isUploading,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .testTag("delete_bg_${bg.id}")
                            ) {
                                Icon(
                                    imageVector = TelefamIcons.Trash2,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Button(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = backgrounds.size < 5 && !isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("add_background_button"),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelefamCrimson,
                    contentColor = Color.White
                )
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = TelefamIcons.Camera,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${stringResource(R.string.telefam_profile_add_background)} (${backgrounds.size}/5)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    pendingCropUri?.let { uri ->
        PhotoCropDialog(
            imageUri = uri,
            aspectRatio = 16f / 10f,
            isCircularOverlay = false,
            onDismiss = { pendingCropUri = null },
            onApplyCrop = { cx, cy, cw, ch ->
                pendingCropUri = null
                onUploadBackground(uri, cx, cy, cw, ch)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileBottomSheet(
    profile: ProfileUiState,
    errorMessage: String,
    countries: List<CountryOption>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onUploadAvatarPhoto: (Uri, Double, Double, Double, Double) -> Unit,
    onCheckUsername: (String) -> Unit,
    onSave: (String, String, String, String, String, String, String, String) -> Boolean
) {
    var fullName by remember(profile.fullName) { mutableStateOf(profile.fullName) }
    var username by remember(profile.username) { mutableStateOf(profile.username) }
    var phoneIso by remember(profile.phoneCountryIso) { mutableStateOf(profile.phoneCountryIso) }
    var phoneNumber by remember(profile.phoneNumber) { mutableStateOf(profile.phoneNumber) }
    var gender by remember(profile.gender) { mutableStateOf(profile.gender) }
    var dob by remember(profile.dateOfBirth) { mutableStateOf(profile.dateOfBirth) }
    var location by remember(profile.location) { mutableStateOf(profile.location) }
    var bio by remember(profile.bio) { mutableStateOf(profile.bio) }

    var pendingAvatarCropUri by remember { mutableStateOf<Uri?>(null) }
    var showCountrySheet by remember { mutableStateOf(false) }
    var showGenderMenu by remember { mutableStateOf(false) }
    var showDobPicker by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pendingAvatarCropUri = uri
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val selectedCountry = remember(phoneIso, countries) {
        countries.firstOrNull { it.iso2.equals(phoneIso, ignoreCase = true) }
    }
    val dialCode = selectedCountry?.dialCode ?: profile.phoneDialCode

    val lockedUntilFormatted = remember(profile.nextUsernameChangeAtEpochSec) {
        if (profile.nextUsernameChangeAtEpochSec > 0L) {
            SimpleDateFormat("MMM d, yyyy", Locale.US)
                .format(Date(profile.nextUsernameChangeAtEpochSec * 1000L))
        } else {
            ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.telefam_profile_edit_profile),
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = TelefamIcons.X,
                        contentDescription = stringResource(R.string.telefam_cancel),
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Avatar upload / change
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .border(2.dp, TelefamCrimson, CircleShape)
                    .clickable {
                        avatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("edit_avatar_button"),
                contentAlignment = Alignment.Center
            ) {
                if (profile.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = TelefamIcons.Camera,
                        contentDescription = null,
                        tint = TelefamCrimson,
                        modifier = Modifier.size(28.dp)
                    )
                }
                if (profile.photoUploading) {
                    CircularProgressIndicator(
                        color = TelefamCrimson,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TelefamCrimson,
                unfocusedBorderColor = outlineColor,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                disabledTextColor = textSecondary,
                disabledBorderColor = outlineColor.copy(alpha = 0.5f),
                cursorColor = TelefamCrimson
            )

            // Full Name
            EditFieldLabel(TelefamIcons.User, stringResource(R.string.telefam_full_name_label), textPrimary)
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it.take(64) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_full_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Username (locked for 14 days after last change)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EditFieldLabel(
                    TelefamIcons.AtSign,
                    stringResource(R.string.telefam_username_label),
                    textPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (!profile.canChangeUsername && lockedUntilFormatted.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("edit_username_locked_badge")
                    ) {
                        Icon(
                            imageVector = TelefamIcons.Lock,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stringResource(R.string.telefam_profile_username_locked_until)} $lockedUntilFormatted",
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                    }
                } else if (profile.canChangeUsername && username != profile.username) {
                    if (profile.usernameChecking) {
                        CircularProgressIndicator(
                            color = TelefamCrimson,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (profile.usernameChecked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (profile.usernameAvailable) {
                                Icon(
                                    imageVector = TelefamIcons.CheckCircle,
                                    contentDescription = null,
                                    tint = TelefamSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = profile.usernameStatusMsg,
                                color = if (profile.usernameAvailable) TelefamSuccess else TelefamCrimson,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("edit_username_status_text")
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = username,
                onValueChange = {
                    if (profile.canChangeUsername) {
                        val next = it.lowercase().take(24)
                        username = next
                        if (next != profile.username) {
                            onCheckUsername(next)
                        }
                    }
                },
                enabled = profile.canChangeUsername,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_username_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone Number
            EditFieldLabel(TelefamIcons.Phone, stringResource(R.string.telefam_phone_label), textPrimary)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                color = surfaceColor,
                border = BorderStroke(1.dp, outlineColor)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .height(54.dp)
                            .clickable { showCountrySheet = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountryFlagBadge(iso2 = phoneIso)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = dialCode,
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = TelefamIcons.ChevronDown,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(outlineColor)
                    )
                    BasicTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it.take(20) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = TextStyle(color = textPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(TelefamCrimson),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .testTag("edit_phone_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gender
            EditFieldLabel(TelefamIcons.Users, stringResource(R.string.telefam_gender_label), textPrimary)
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { showGenderMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceColor,
                    border = BorderStroke(1.dp, outlineColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = gender.ifEmpty { stringResource(R.string.telefam_gender_hint) },
                            color = if (gender.isEmpty()) textSecondary else textPrimary,
                            fontSize = 15.sp
                        )
                        Icon(
                            imageVector = TelefamIcons.ChevronDown,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showGenderMenu,
                    onDismissRequest = { showGenderMenu = false }
                ) {
                    listOf("Male", "Female", "Non-binary", "Prefer not to say").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                gender = option
                                showGenderMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date of Birth
            EditFieldLabel(TelefamIcons.Calendar, stringResource(R.string.telefam_dob_label), textPrimary)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { showDobPicker = true },
                shape = RoundedCornerShape(12.dp),
                color = surfaceColor,
                border = BorderStroke(1.dp, outlineColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dob.ifEmpty { stringResource(R.string.telefam_dob_hint) },
                        color = if (dob.isEmpty()) textSecondary else textPrimary,
                        fontSize = 15.sp
                    )
                    Icon(
                        imageVector = TelefamIcons.ChevronDown,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            EditFieldLabel(TelefamIcons.MapPin, stringResource(R.string.telefam_location_label), textPrimary)
            OutlinedTextField(
                value = location,
                onValueChange = { location = it.take(80) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_location_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bio (0/150)
            EditFieldLabel(TelefamIcons.MessageSquare, stringResource(R.string.telefam_bio_label), textPrimary)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
                shape = RoundedCornerShape(12.dp),
                color = surfaceColor,
                border = BorderStroke(1.dp, outlineColor)
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    BasicTextField(
                        value = bio,
                        onValueChange = { bio = it.take(150) },
                        textStyle = TextStyle(color = textPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(TelefamCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp)
                            .testTag("edit_bio_input"),
                        decorationBox = { inner ->
                            if (bio.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.telefam_bio_hint),
                                    color = textSecondary,
                                    fontSize = 15.sp
                                )
                            }
                            inner()
                        }
                    )
                    Text(
                        text = "${bio.length}/150",
                        color = textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = TelefamCrimson,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("edit_profile_error_text")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    onSave(fullName, username, phoneIso, phoneNumber, gender, dob, location, bio)
                },
                enabled = !profile.photoUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("edit_profile_save_button"),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelefamCrimson,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.telefam_profile_save),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    if (showCountrySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCountrySheet = false },
            containerColor = surfaceColor
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .padding(horizontal = 16.dp)
            ) {
                items(countries, key = { it.iso2 }) { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable {
                                phoneIso = c.iso2
                                showCountrySheet = false
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountryFlagBadge(iso2 = c.iso2)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = c.name,
                            color = textPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = c.dialCode,
                            color = TelefamCrimson,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider(color = outlineColor.copy(alpha = 0.3f))
                }
            }
        }
    }

    if (showDobPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDobPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { ms ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            dob = sdf.format(Date(ms))
                        }
                        showDobPicker = false
                    }
                ) {
                    Text(stringResource(R.string.telefam_apply_crop), color = TelefamCrimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDobPicker = false }) {
                    Text(stringResource(R.string.telefam_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    pendingAvatarCropUri?.let { uri ->
        PhotoCropDialog(
            imageUri = uri,
            aspectRatio = 1f,
            isCircularOverlay = true,
            onDismiss = { pendingAvatarCropUri = null },
            onApplyCrop = { cx, cy, cw, ch ->
                pendingAvatarCropUri = null
                onUploadAvatarPhoto(uri, cx, cy, cw, ch)
            }
        )
    }
}

@Composable
private fun EditFieldLabel(
    icon: ImageVector,
    label: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TelefamCrimson,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowListBottomSheet(
    initialTab: String,
    contacts: List<ContactItem>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onToggleFollow: (ContactItem) -> Unit
) {
    var activeListTab by remember(initialTab) { mutableStateOf(initialTab) }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val displayed = remember(contacts, activeListTab) {
        if (activeListTab == "followers") {
            contacts.filter { it.followsMe }
        } else {
            contacts.filter { it.iFollow }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "following" to stringResource(R.string.telefam_profile_following),
                    "followers" to stringResource(R.string.telefam_profile_followers)
                ).forEach { (key, title) ->
                    val selected = activeListTab == key
                    Surface(
                        onClick = { activeListTab = key },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("follow_sheet_tab_$key"),
                        shape = RoundedCornerShape(21.dp),
                        color = if (selected) TelefamCrimson else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = title,
                                color = if (selected) Color.White else textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (displayed.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.telefam_contacts_empty),
                        color = textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.testTag("follow_sheet_empty")
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(displayed, key = { it.userId }) { contact ->
                        FollowSheetRow(
                            contact = contact,
                            isDark = isDark,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onToggleFollow = { onToggleFollow(contact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowSheetRow(
    contact: ContactItem,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onToggleFollow: () -> Unit
) {
    val avatarBg = if (isDark) Color(0xFF1A2D3D) else Color(0xFFE7EFF7)
    val avatarFg = if (isDark) Color(0xFF8CC4FF) else Color(0xFF1D4E7A)

    val isFriend = contact.isFriend || (contact.iFollow && contact.followsMe)
    val isFollowingOnly = contact.iFollow && !contact.followsMe
    val isFollowBack = !contact.iFollow && contact.followsMe

    val btnLabel = when {
        isFriend -> stringResource(R.string.telefam_contacts_action_friends)
        isFollowingOnly -> stringResource(R.string.telefam_contacts_action_following)
        isFollowBack -> stringResource(R.string.telefam_contacts_action_follow_back)
        else -> stringResource(R.string.telefam_contacts_action_follow)
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            isFriend -> if (isDark) Color(0x2616A34A) else Color(0x1816A34A)
            isFollowingOnly -> if (isDark) Color(0xFF1F2026) else Color(0xFFF3F4F6)
            else -> if (isDark) Color(0x24C80E18) else Color(0x14C80E18)
        },
        label = "fsBg"
    )
    val borderColor = when {
        isFriend -> TelefamSuccess
        isFollowingOnly -> if (isDark) Color(0xFF3E4049) else Color(0xFFD1D5DB)
        else -> TelefamCrimson
    }
    val contentColor = when {
        isFriend -> if (isDark) Color(0xFF4ADE80) else TelefamSuccess
        isFollowingOnly -> textPrimary
        else -> if (isDark) Color(0xFFFF6B73) else TelefamCrimson
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("follow_sheet_row_${contact.userId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (contact.avatarUrl.isNotEmpty()) {
            AsyncImage(
                model = contact.avatarUrl,
                contentDescription = contact.fullName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.fullName.trim().take(2).uppercase()
                        .ifEmpty { contact.username.take(2).uppercase() },
                    color = avatarFg,
                    fontSize = 15.sp,
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
            Text(
                text = "@${contact.username} · ${contact.countryName}",
                color = textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onToggleFollow)
                .testTag("follow_sheet_action_${contact.userId}"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(17.dp),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = btnLabel,
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
