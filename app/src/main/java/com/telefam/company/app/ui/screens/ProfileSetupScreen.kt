package com.telefam.company.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telefam.company.app.R
import com.telefam.company.app.nativecore.CountryOption
import com.telefam.company.app.nativecore.TelefamUiState
import com.telefam.company.app.ui.components.PhotoCropDialog
import com.telefam.company.app.ui.icons.CountryFlagBadge
import com.telefam.company.app.ui.icons.TelefamIcons
import com.telefam.company.app.ui.theme.TelefamBlush
import com.telefam.company.app.ui.theme.TelefamCrimson
import com.telefam.company.app.ui.theme.TelefamSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    state: TelefamUiState,
    countries: List<CountryOption>,
    onBack: () -> Unit,
    onFieldChanged: (String, String) -> Unit,
    onUploadCroppedPhoto: (Uri, Double, Double, Double, Double) -> Unit,
    onSubmitProfile: () -> Unit
) {
    BackHandler { onBack() }

    val profile = state.profile
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var localPhotoPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var showCountrySheet by remember { mutableStateOf(false) }
    var showGenderMenu by remember { mutableStateOf(false) }
    var showDobPicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingCropUri = uri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Curved crimson header matching reference Image 2
        Surface(
            color = TelefamCrimson,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 14.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .testTag("profile_back_button")
                ) {
                    Icon(
                        imageVector = TelefamIcons.ArrowLeft,
                        contentDescription = stringResource(R.string.telefam_cancel),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.telefam_profile_setup_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.telefam_profile_setup_subtitle),
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar upload ring with dashed crimson border and + badge
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("upload_photo_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = TelefamBlush,
                            radius = size.minDimension / 2f - 2.dp.toPx()
                        )
                        drawCircle(
                            color = TelefamCrimson,
                            radius = size.minDimension / 2f - 2.dp.toPx(),
                            style = Stroke(
                                width = 1.6.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            )
                        )
                    }

                    val previewSource: Any? = when {
                        localPhotoPreviewUri != null -> localPhotoPreviewUri
                        profile.avatarUrl.isNotEmpty() -> profile.avatarUrl
                        else -> null
                    }
                    if (previewSource != null) {
                        AsyncImage(
                            model = previewSource,
                            contentDescription = stringResource(R.string.telefam_upload_photo_title),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(98.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = TelefamIcons.Camera,
                            contentDescription = stringResource(R.string.telefam_upload_photo_title),
                            tint = TelefamCrimson,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    if (profile.photoUploading) {
                        CircularProgressIndicator(
                            color = TelefamCrimson,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(30.dp)
                            .background(TelefamCrimson, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = TelefamIcons.Plus,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.telefam_upload_photo_title),
                    color = Color(0xFF1A1C1E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.telefam_upload_photo_subtitle),
                    color = Color(0xFF6B7280),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Full Name
                FieldLabel(TelefamIcons.User, stringResource(R.string.telefam_full_name_label))
                OutlinedTextField(
                    value = profile.fullName,
                    onValueChange = { onFieldChanged("fullName", it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("full_name_input"),
                    placeholder = { Text(stringResource(R.string.telefam_full_name_hint), color = Color(0xFF9CA3AF)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = profileTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Username with server uniqueness status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FieldLabel(
                        TelefamIcons.AtSign,
                        stringResource(R.string.telefam_username_label),
                        modifier = Modifier.weight(1f)
                    )
                    if (profile.usernameChecking) {
                        CircularProgressIndicator(
                            color = TelefamCrimson,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (profile.usernameChecked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (profile.usernameAvailable) {
                                Icon(
                                    imageVector = TelefamIcons.CheckCircle,
                                    contentDescription = null,
                                    tint = TelefamSuccess,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = profile.usernameStatusMsg,
                                color = if (profile.usernameAvailable) TelefamSuccess else TelefamCrimson,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("username_status_text")
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = profile.username,
                    onValueChange = { onFieldChanged("username", it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    placeholder = { Text(stringResource(R.string.telefam_username_hint), color = Color(0xFF9CA3AF)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = profileTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Number with Country Picker
                FieldLabel(TelefamIcons.Phone, stringResource(R.string.telefam_phone_label))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (profile.phoneError.isNotEmpty()) TelefamCrimson else Color(0xFFDDE0E5)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .height(56.dp)
                                .clickable { showCountrySheet = true }
                                .padding(horizontal = 12.dp)
                                .testTag("country_selector_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CountryFlagBadge(iso2 = profile.phoneCountryIso)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = profile.phoneDialCode,
                                color = Color(0xFF1A1C1E),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = TelefamIcons.ChevronDown,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(Color(0xFFDDE0E5))
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = profile.phoneNumber,
                            onValueChange = { onFieldChanged("phoneNumber", it) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                                .testTag("phone_input"),
                            decorationBox = { inner ->
                                if (profile.phoneNumber.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.telefam_phone_hint),
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 15.sp
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }
                if (profile.phoneError.isNotEmpty()) {
                    Text(
                        text = profile.phoneError,
                        color = TelefamCrimson,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gender Dropdown
                FieldLabel(TelefamIcons.Users, stringResource(R.string.telefam_gender_label))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clickable { showGenderMenu = true }
                            .testTag("gender_selector"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFDDE0E5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = profile.gender.ifEmpty { stringResource(R.string.telefam_gender_hint) },
                                color = if (profile.gender.isEmpty()) Color(0xFF9CA3AF) else Color(0xFF1A1C1E),
                                fontSize = 15.sp
                            )
                            Icon(
                                imageVector = TelefamIcons.ChevronDown,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
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
                                    onFieldChanged("gender", option)
                                    showGenderMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date of Birth (13+ enforced)
                FieldLabel(TelefamIcons.Calendar, stringResource(R.string.telefam_dob_label))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clickable { showDobPicker = true }
                        .testTag("dob_selector"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (profile.dobError.isNotEmpty()) TelefamCrimson else Color(0xFFDDE0E5)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = profile.dateOfBirth.ifEmpty { stringResource(R.string.telefam_dob_hint) },
                            color = if (profile.dateOfBirth.isEmpty()) Color(0xFF9CA3AF) else Color(0xFF1A1C1E),
                            fontSize = 15.sp
                        )
                        Icon(
                            imageVector = TelefamIcons.ChevronDown,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (profile.dobError.isNotEmpty()) {
                    Text(
                        text = profile.dobError,
                        color = TelefamCrimson,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Location
                FieldLabel(TelefamIcons.MapPin, stringResource(R.string.telefam_location_label))
                OutlinedTextField(
                    value = profile.location,
                    onValueChange = { onFieldChanged("location", it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_input"),
                    placeholder = { Text(stringResource(R.string.telefam_location_hint), color = Color(0xFF9CA3AF)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = profileTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bio with 0/150 counter
                FieldLabel(TelefamIcons.MessageSquare, stringResource(R.string.telefam_bio_label))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 106.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFDDE0E5))
                ) {
                    Box(modifier = Modifier.padding(14.dp)) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = profile.bio,
                            onValueChange = { onFieldChanged("bio", it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp)
                                .testTag("bio_input"),
                            decorationBox = { inner ->
                                if (profile.bio.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.telefam_bio_hint),
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 15.sp
                                    )
                                }
                                inner()
                            }
                        )
                        Text(
                            text = "${profile.bio.length}/150",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }

                if (state.errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.errorMessage,
                        color = TelefamCrimson,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("profile_error_text")
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onSubmitProfile,
                    enabled = !state.isLoading && !profile.photoUploading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("profile_continue_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelefamCrimson,
                        contentColor = Color.White
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.telefam_continue),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showCountrySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCountrySheet = false },
            containerColor = Color.White
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .padding(horizontal = 16.dp)
            ) {
                items(countries, key = { it.iso2 }) { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable {
                                onFieldChanged("phoneCountryIso", c.iso2)
                                showCountrySheet = false
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountryFlagBadge(iso2 = c.iso2)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = c.name,
                            color = Color(0xFF1A1C1E),
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
                    HorizontalDivider(color = Color(0xFFF3F4F6))
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
                            onFieldChanged("dateOfBirth", sdf.format(Date(ms)))
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

    pendingCropUri?.let { uri ->
        PhotoCropDialog(
            imageUri = uri,
            onDismiss = { pendingCropUri = null },
            onApplyCrop = { cx, cy, cw, ch ->
                localPhotoPreviewUri = uri
                pendingCropUri = null
                onUploadCroppedPhoto(uri, cx, cy, cw, ch)
            }
        )
    }
}

@Composable
private fun FieldLabel(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TelefamCrimson,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Color(0xFF1A1C1E),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun profileTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TelefamCrimson,
    unfocusedBorderColor = Color(0xFFDDE0E5),
    focusedTextColor = Color(0xFF1A1C1E),
    unfocusedTextColor = Color(0xFF1A1C1E),
    cursorColor = TelefamCrimson
)
