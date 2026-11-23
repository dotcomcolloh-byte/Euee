package com.telefam.company.desktop.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.desktop.ui.components.DesktopAsyncImage
import com.telefam.company.desktop.ui.components.DesktopDatePickerDialog
import com.telefam.company.desktop.ui.components.DesktopPhotoCropDialog
import com.telefam.company.desktop.ui.icons.CountryFlagBadge
import com.telefam.company.desktop.ui.icons.TelefamIcons
import com.telefam.company.desktop.ui.theme.TelefamCrimson
import com.telefam.company.desktop.ui.theme.TelefamSuccess
import com.telefam.company.shared.TelefamStrings
import com.telefam.company.shared.model.BackgroundPhotoItem
import com.telefam.company.shared.model.ContactItem
import com.telefam.company.shared.model.CountryOption
import com.telefam.company.shared.model.ProfileUiState
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBackgroundBottomSheet(
    backgrounds: List<BackgroundPhotoItem>,
    httpClient: OkHttpClient,
    isUploading: Boolean,
    errorMessage: String,
    onPickImageFile: () -> Pair<ByteArray, String>?,
    onDismiss: () -> Unit,
    onUploadBackground: (ByteArray, String, Double, Double, Double, Double) -> Unit,
    onDeleteBackground: (String) -> Unit
) {
    var pendingCrop by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }

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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${TelefamStrings.PROFILE_BACKGROUNDS_TITLE} (${backgrounds.size}/5)",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = TelefamIcons.X,
                        contentDescription = TelefamStrings.CANCEL,
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
                            DesktopAsyncImage(
                                url = bg.imageUrl,
                                httpClient = httpClient,
                                contentDescription = null,
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
                onClick = { pendingCrop = onPickImageFile() },
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
                        text = "${TelefamStrings.PROFILE_ADD_BACKGROUND} (${backgrounds.size}/5)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    pendingCrop?.let { (bytes, mime) ->
        DesktopPhotoCropDialog(
            imageBytes = bytes,
            aspectRatio = 16f / 10f,
            isCircularOverlay = false,
            onDismiss = { pendingCrop = null },
            onApplyCrop = { cx, cy, cw, ch ->
                pendingCrop = null
                onUploadBackground(bytes, mime, cx, cy, cw, ch)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileBottomSheet(
    profile: ProfileUiState,
    errorMessage: String,
    countries: List<CountryOption>,
    httpClient: OkHttpClient,
    onPickImageFile: () -> Pair<ByteArray, String>?,
    onDismiss: () -> Unit,
    onUploadAvatarPhoto: (ByteArray, String, Double, Double, Double, Double) -> Unit,
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

    var pendingAvatarCrop by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    var showCountrySheet by remember { mutableStateOf(false) }
    var showGenderMenu by remember { mutableStateOf(false) }
    var showDobPicker by remember { mutableStateOf(false) }

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
                    text = TelefamStrings.PROFILE_EDIT_PROFILE,
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = TelefamIcons.X,
                        contentDescription = TelefamStrings.CANCEL,
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .border(2.dp, TelefamCrimson, CircleShape)
                    .clickable { pendingAvatarCrop = onPickImageFile() }
                    .testTag("edit_avatar_button"),
                contentAlignment = Alignment.Center
            ) {
                if (profile.avatarUrl.isNotEmpty()) {
                    DesktopAsyncImage(
                        url = profile.avatarUrl,
                        httpClient = httpClient,
                        contentDescription = null,
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

            EditFieldLabel(TelefamIcons.User, TelefamStrings.FULL_NAME_LABEL, textPrimary)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EditFieldLabel(
                    TelefamIcons.AtSign,
                    TelefamStrings.USERNAME_LABEL,
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
                            text = "${TelefamStrings.PROFILE_USERNAME_LOCKED_UNTIL} $lockedUntilFormatted",
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

            EditFieldLabel(TelefamIcons.Phone, TelefamStrings.PHONE_LABEL, textPrimary)
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
                        Text(text = dialCode, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = TelefamIcons.ChevronDown,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(28.dp).background(outlineColor))
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

            EditFieldLabel(TelefamIcons.Users, TelefamStrings.GENDER_LABEL, textPrimary)
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
                            text = gender.ifEmpty { TelefamStrings.GENDER_HINT },
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

            EditFieldLabel(TelefamIcons.Calendar, TelefamStrings.DOB_LABEL, textPrimary)
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
                        text = dob.ifEmpty { TelefamStrings.DOB_HINT },
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

            EditFieldLabel(TelefamIcons.MapPin, TelefamStrings.LOCATION_LABEL, textPrimary)
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

            EditFieldLabel(TelefamIcons.MessageSquare, TelefamStrings.BIO_LABEL, textPrimary)
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
                                Text(text = TelefamStrings.BIO_HINT, color = textSecondary, fontSize = 15.sp)
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
                onClick = { onSave(fullName, username, phoneIso, phoneNumber, gender, dob, location, bio) },
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
                Text(text = TelefamStrings.PROFILE_SAVE, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    if (showCountrySheet) {
        ModalBottomSheet(onDismissRequest = { showCountrySheet = false }, containerColor = surfaceColor) {
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
                        Text(text = c.name, color = textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(text = c.dialCode, color = TelefamCrimson, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = outlineColor.copy(alpha = 0.3f))
                }
            }
        }
    }

    if (showDobPicker) {
        DesktopDatePickerDialog(
            onDismiss = { showDobPicker = false },
            onDateSelected = { dob = it }
        )
    }

    pendingAvatarCrop?.let { (bytes, mime) ->
        DesktopPhotoCropDialog(
            imageBytes = bytes,
            aspectRatio = 1f,
            isCircularOverlay = true,
            onDismiss = { pendingAvatarCrop = null },
            onApplyCrop = { cx, cy, cw, ch ->
                pendingAvatarCrop = null
                onUploadAvatarPhoto(bytes, mime, cx, cy, cw, ch)
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
        Icon(imageVector = icon, contentDescription = null, tint = TelefamCrimson, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListBottomSheet(
    initialTab: String,
    contacts: List<ContactItem>,
    httpClient: OkHttpClient,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onToggleFollow: (ContactItem) -> Unit
) {
    var activeListTab by remember(initialTab) { mutableStateOf(initialTab) }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val displayed = remember(contacts, activeListTab) {
        if (activeListTab == "followers") contacts.filter { it.followsMe } else contacts.filter { it.iFollow }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "following" to TelefamStrings.PROFILE_FOLLOWING,
                    "followers" to TelefamStrings.PROFILE_FOLLOWERS
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
                        text = TelefamStrings.CONTACTS_EMPTY,
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
                            httpClient = httpClient,
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
    httpClient: OkHttpClient,
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
        isFriend -> TelefamStrings.CONTACTS_ACTION_FRIENDS
        isFollowingOnly -> TelefamStrings.CONTACTS_ACTION_FOLLOWING
        isFollowBack -> TelefamStrings.CONTACTS_ACTION_FOLLOW_BACK
        else -> TelefamStrings.CONTACTS_ACTION_FOLLOW
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            isFriend -> if (isDark) Color(0x2616A34A) else Color(0x1816A34A)
            isFollowingOnly -> if (isDark) Color(0xFF1F2026) else Color(0xFFF3F4F6)
            else -> if (isDark) Color(0x24C80E18) else Color(0x14C80E18)
        }
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
            DesktopAsyncImage(
                url = contact.avatarUrl,
                httpClient = httpClient,
                contentDescription = contact.fullName,
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
                text = "@${contact.username} \u00B7 ${contact.countryName}",
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
                Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
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
