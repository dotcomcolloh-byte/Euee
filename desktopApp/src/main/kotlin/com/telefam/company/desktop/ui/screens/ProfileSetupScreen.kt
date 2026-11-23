package com.telefam.company.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.desktop.ui.components.DesktopAsyncImage
import com.telefam.company.desktop.ui.components.DesktopDatePickerDialog
import com.telefam.company.desktop.ui.components.DesktopPhotoCropDialog
import com.telefam.company.desktop.ui.icons.CountryFlagBadge
import com.telefam.company.desktop.ui.icons.TelefamIcons
import com.telefam.company.desktop.ui.theme.TelefamBlush
import com.telefam.company.desktop.ui.theme.TelefamCrimson
import com.telefam.company.desktop.ui.theme.TelefamSuccess
import com.telefam.company.shared.TelefamStrings
import com.telefam.company.shared.model.CountryOption
import com.telefam.company.shared.model.TelefamUiState
import okhttp3.OkHttpClient
import org.jetbrains.skia.Image as SkiaImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopProfileSetupScreen(
    state: TelefamUiState,
    countries: List<CountryOption>,
    httpClient: OkHttpClient,
    onPickImageFile: () -> Pair<ByteArray, String>?,
    onBack: () -> Unit,
    onFieldChanged: (String, String) -> Unit,
    onUploadCroppedPhoto: (ByteArray, String, Double, Double, Double, Double) -> Unit,
    onSubmitProfile: () -> Unit
) {
    val profile = state.profile
    var pendingCrop by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    var localPreviewBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showCountrySheet by remember { mutableStateOf(false) }
    var showGenderMenu by remember { mutableStateOf(false) }
    var showDobPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Surface(
            color = TelefamCrimson,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                        contentDescription = TelefamStrings.CANCEL,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = TelefamStrings.PROFILE_SETUP_TITLE,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = TelefamStrings.PROFILE_SETUP_SUBTITLE,
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
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clickable {
                            pendingCrop = onPickImageFile()
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

                    val localBitmap = remember(localPreviewBytes) {
                        localPreviewBytes?.let {
                            runCatching { SkiaImage.makeFromEncoded(it).toComposeImageBitmap() }.getOrNull()
                        }
                    }

                    if (localBitmap != null) {
                        Image(
                            bitmap = localBitmap,
                            contentDescription = TelefamStrings.UPLOAD_PHOTO_TITLE,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(98.dp)
                                .clip(CircleShape)
                        )
                    } else if (profile.avatarUrl.isNotEmpty()) {
                        DesktopAsyncImage(
                            url = profile.avatarUrl,
                            httpClient = httpClient,
                            contentDescription = TelefamStrings.UPLOAD_PHOTO_TITLE,
                            modifier = Modifier
                                .size(98.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = TelefamIcons.Camera,
                            contentDescription = TelefamStrings.UPLOAD_PHOTO_TITLE,
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
                    text = TelefamStrings.UPLOAD_PHOTO_TITLE,
                    color = Color(0xFF1A1C1E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = TelefamStrings.UPLOAD_PHOTO_SUBTITLE,
                    color = Color(0xFF6B7280),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                FieldLabel(TelefamIcons.User, TelefamStrings.FULL_NAME_LABEL)
                OutlinedTextField(
                    value = profile.fullName,
                    onValueChange = { onFieldChanged("fullName", it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("full_name_input"),
                    placeholder = { Text(TelefamStrings.FULL_NAME_HINT, color = Color(0xFF9CA3AF)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = profileTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FieldLabel(
                        TelefamIcons.AtSign,
                        TelefamStrings.USERNAME_LABEL,
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
                    placeholder = { Text(TelefamStrings.USERNAME_HINT, color = Color(0xFF9CA3AF)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = profileTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                FieldLabel(TelefamIcons.Phone, TelefamStrings.PHONE_LABEL)
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
                        BasicTextField(
                            value = profile.phoneNumber,
                            onValueChange = { onFieldChanged("phoneNumber", it) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            textStyle = TextStyle(color = Color(0xFF1A1C1E), fontSize = 15.sp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                                .testTag("phone_input"),
                            decorationBox = { inner ->
                                if (profile.phoneNumber.isEmpty()) {
                                    Text(
                                        text = TelefamStrings.PHONE_HINT,
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

                FieldLabel(TelefamIcons.Users, TelefamStrings.GENDER_LABEL)
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
                                text = profile.gender.ifEmpty { TelefamStrings.GENDER_HINT },
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

                FieldLabel(TelefamIcons.Calendar, TelefamStrings.DOB_LABEL)
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
                            text = profile.dateOfBirth.ifEmpty { TelefamStrings.DOB_HINT },
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

                FieldLabel(TelefamIcons.MapPin, TelefamStrings.LOCATION_LABEL)
                OutlinedTextField(
                    value = profile.location,
                    onValueChange = { onFieldChanged("location", it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_input"),
                    placeholder = { Text(TelefamStrings.LOCATION_HINT, color = Color(0xFF9CA3AF)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = profileTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                FieldLabel(TelefamIcons.MessageSquare, TelefamStrings.BIO_LABEL)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 106.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFDDE0E5))
                ) {
                    Box(modifier = Modifier.padding(14.dp)) {
                        BasicTextField(
                            value = profile.bio,
                            onValueChange = { onFieldChanged("bio", it) },
                            textStyle = TextStyle(color = Color(0xFF1A1C1E), fontSize = 15.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp)
                                .testTag("bio_input"),
                            decorationBox = { inner ->
                                if (profile.bio.isEmpty()) {
                                    Text(
                                        text = TelefamStrings.BIO_HINT,
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
                            text = TelefamStrings.CONTINUE,
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
        DesktopDatePickerDialog(
            onDismiss = { showDobPicker = false },
            onDateSelected = { formatted -> onFieldChanged("dateOfBirth", formatted) }
        )
    }

    pendingCrop?.let { (bytes, mime) ->
        DesktopPhotoCropDialog(
            imageBytes = bytes,
            onDismiss = { pendingCrop = null },
            onApplyCrop = { cx, cy, cw, ch ->
                localPreviewBytes = bytes
                pendingCrop = null
                onUploadCroppedPhoto(bytes, mime, cx, cy, cw, ch)
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
