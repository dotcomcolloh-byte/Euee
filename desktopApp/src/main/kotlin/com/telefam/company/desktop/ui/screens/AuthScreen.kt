package com.telefam.company.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.desktop.ui.icons.GoogleLogoVector
import com.telefam.company.desktop.ui.icons.TelefamIcons
import com.telefam.company.desktop.ui.theme.TelefamBlush
import com.telefam.company.desktop.ui.theme.TelefamCrimson
import com.telefam.company.shared.TelefamStrings
import com.telefam.company.shared.model.AppScreen
import com.telefam.company.shared.model.TelefamUiState

@Composable
fun DesktopAuthScreen(
    state: TelefamUiState,
    onEmailChanged: (String) -> Unit,
    onSendOtp: () -> Unit,
    onOtpChanged: (String) -> Unit,
    onVerifyOtp: () -> Unit,
    onChangeEmail: () -> Unit,
    onContinueWithGoogle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val topRightWave = Path().apply {
                moveTo(w * 0.55f, 0f)
                cubicTo(w * 0.70f, h * 0.05f, w * 0.88f, h * 0.02f, w, h * 0.07f)
                lineTo(w, 0f)
                close()
            }
            drawPath(topRightWave, color = TelefamBlush)

            val topLeftWave = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.30f, 0f)
                cubicTo(w * 0.18f, h * 0.04f, w * 0.08f, h * 0.03f, 0f, h * 0.06f)
                close()
            }
            drawPath(topLeftWave, color = TelefamBlush)

            val bottomWave = Path().apply {
                moveTo(0f, h * 0.87f)
                cubicTo(w * 0.25f, h * 0.81f, w * 0.48f, h * 0.91f, w * 0.72f, h * 0.86f)
                cubicTo(w * 0.86f, h * 0.83f, w * 0.95f, h * 0.85f, w, h * 0.83f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(bottomWave, color = TelefamCrimson)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                imageVector = TelefamIcons.TelefamLogo,
                contentDescription = TelefamStrings.APP_NAME,
                modifier = Modifier.size(86.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = TelefamStrings.APP_NAME,
                color = TelefamCrimson,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = TelefamStrings.TAGLINE,
                color = Color(0xFF6B7280),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F2F4))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.screen == AppScreen.AUTH_EMAIL) {
                        EmailAuthCardContent(
                            state = state,
                            onEmailChanged = onEmailChanged,
                            onSendOtp = onSendOtp,
                            onContinueWithGoogle = onContinueWithGoogle
                        )
                    } else {
                        OtpAuthCardContent(
                            state = state,
                            onOtpChanged = onOtpChanged,
                            onVerifyOtp = onVerifyOtp,
                            onResendOtp = onSendOtp,
                            onChangeEmail = onChangeEmail
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailAuthCardContent(
    state: TelefamUiState,
    onEmailChanged: (String) -> Unit,
    onSendOtp: () -> Unit,
    onContinueWithGoogle: () -> Unit
) {
    Text(
        text = TelefamStrings.ENTER_EMAIL_TITLE,
        color = TelefamCrimson,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = TelefamStrings.ENTER_EMAIL_SUBTITLE,
        color = Color(0xFF6B7280),
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = TelefamStrings.EMAIL_LABEL,
        color = Color(0xFF1A1C1E),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.email,
        onValueChange = onEmailChanged,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("email_input"),
        placeholder = {
            Text(text = TelefamStrings.EMAIL_HINT, color = Color(0xFF9CA3AF))
        },
        leadingIcon = {
            Icon(
                imageVector = TelefamIcons.Mail,
                contentDescription = null,
                tint = TelefamCrimson,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onSendOtp() }),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TelefamCrimson,
            unfocusedBorderColor = Color(0xFFDDE0E5),
            focusedTextColor = Color(0xFF1A1C1E),
            unfocusedTextColor = Color(0xFF1A1C1E),
            cursorColor = TelefamCrimson
        )
    )

    if (state.errorMessage.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = state.errorMessage,
            color = TelefamCrimson,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("auth_error_text")
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onSendOtp,
        enabled = !state.isLoading && state.lockRemainingSec == 0,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("send_otp_button"),
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TelefamCrimson,
            contentColor = Color.White,
            disabledContainerColor = TelefamCrimson.copy(alpha = 0.55f),
            disabledContentColor = Color.White
        )
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else if (state.lockRemainingSec > 0) {
            Text(text = state.lockTimerText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        } else {
            Text(text = TelefamStrings.SEND_OTP, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
        Text(
            text = TelefamStrings.OR_DIVIDER,
            color = Color(0xFF6B7280),
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
    }

    Spacer(modifier = Modifier.height(18.dp))

    OutlinedButton(
        onClick = onContinueWithGoogle,
        enabled = !state.isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("google_signin_button"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFDDE0E5)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1A1C1E)
        )
    ) {
        GoogleLogoVector(size = 20.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = TelefamStrings.CONTINUE_GOOGLE,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun OtpAuthCardContent(
    state: TelefamUiState,
    onOtpChanged: (String) -> Unit,
    onVerifyOtp: () -> Unit,
    onResendOtp: () -> Unit,
    onChangeEmail: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Text(
        text = TelefamStrings.ENTER_OTP_TITLE,
        color = TelefamCrimson,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = TelefamStrings.OTP_SENT_TO,
        color = Color(0xFF6B7280),
        fontSize = 13.sp
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = state.email,
        color = Color(0xFF1A1C1E),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag("otp_target_email")
    )

    Spacer(modifier = Modifier.height(22.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusRequester.requestFocus() },
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = state.otp,
            onValueChange = onOtpChanged,
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
                .testTag("otp_input"),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onVerifyOtp() }),
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    for (i in 0 until 6) {
                        val ch = state.otp.getOrNull(i)?.toString()
                        val isActive = i == state.otp.length.coerceAtMost(5)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .border(
                                    width = if (isActive) 1.5.dp else 1.dp,
                                    color = if (isActive) TelefamCrimson else Color(0xFFDDE0E5),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(Color.White, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (ch != null) {
                                Text(
                                    text = ch,
                                    color = Color(0xFF1A1C1E),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF9CA3AF), RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    if (state.errorMessage.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = state.errorMessage,
            color = TelefamCrimson,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("otp_error_text")
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onVerifyOtp,
        enabled = !state.isLoading && state.lockRemainingSec == 0,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("verify_otp_button"),
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TelefamCrimson,
            contentColor = Color.White,
            disabledContainerColor = TelefamCrimson.copy(alpha = 0.55f),
            disabledContentColor = Color.White
        )
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else if (state.lockRemainingSec > 0) {
            Text(
                text = state.lockTimerText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("otp_lock_countdown")
            )
        } else {
            Text(
                text = TelefamStrings.VERIFY_OTP,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    if (state.resendRemainingSec > 0) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${TelefamStrings.RESEND_CODE_IN} ", color = Color(0xFF6B7280), fontSize = 13.sp)
            Text(
                text = state.resendTimerText,
                color = TelefamCrimson,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("resend_countdown_text")
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${TelefamStrings.AFTER_COOLDOWN_PREFIX} ${state.resendTimerText} ${TelefamStrings.AFTER_COOLDOWN_MIDDLE} ",
                color = Color(0xFF6B7280),
                fontSize = 12.sp
            )
            Text(
                text = TelefamStrings.RESEND_CODE,
                color = TelefamCrimson.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        Box(
            modifier = Modifier
                .height(48.dp)
                .clickable(enabled = !state.isLoading && state.lockRemainingSec == 0) { onResendOtp() }
                .padding(horizontal = 16.dp)
                .testTag("resend_otp_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = TelefamStrings.RESEND_CODE,
                color = TelefamCrimson,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Box(
        modifier = Modifier
            .height(48.dp)
            .clickable { onChangeEmail() }
            .padding(horizontal = 16.dp)
            .testTag("change_email_button"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = TelefamStrings.CHANGE_EMAIL,
            color = TelefamCrimson,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
