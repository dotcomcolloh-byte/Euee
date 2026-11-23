import SwiftUI

struct AuthView: View {
    @ObservedObject var viewModel: TelefamViewModel
    @FocusState private var otpFocused: Bool
    @FocusState private var emailFocused: Bool

    private var state: TelefamUiState { viewModel.uiState }

    var body: some View {
        ZStack {
            Color.white.ignoresSafeArea()

            AuthDecorativeWavesBackground()
                .ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    Spacer(minLength: 28)

                    TelefamLogoView(size: 86)

                    Spacer().frame(height: 10)

                    Text(TelefamStrings.appName)
                        .font(.system(size: 32, weight: .heavy))
                        .foregroundColor(TelefamColors.crimson)

                    Spacer().frame(height: 4)

                    Text(TelefamStrings.tagline)
                        .font(.system(size: 14, weight: .regular))
                        .foregroundColor(TelefamColors.lightTextSecondary)

                    Spacer().frame(height: 28)

                    VStack(spacing: 0) {
                        if state.screen == .authEmail {
                            emailAuthCardContent
                        } else {
                            otpAuthCardContent
                        }
                    }
                    .padding(.horizontal, 22)
                    .padding(.vertical, 26)
                    .frame(maxWidth: 440)
                    .background(Color.white)
                    .cornerRadius(24)
                    .overlay(
                        RoundedRectangle(cornerRadius: 24)
                            .stroke(Color(hex: 0xF1F2F4), lineWidth: 1)
                    )
                    .shadow(color: Color.black.opacity(0.08), radius: 14, x: 0, y: 6)

                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 24)
                .frame(maxWidth: .infinity)
            }
        }
    }

    @ViewBuilder
    private var emailAuthCardContent: some View {
        Text(TelefamStrings.enterEmailTitle)
            .font(.system(size: 21, weight: .bold))
            .foregroundColor(TelefamColors.crimson)

        Spacer().frame(height: 4)

        Text(TelefamStrings.enterEmailSubtitle)
            .font(.system(size: 13))
            .foregroundColor(TelefamColors.lightTextSecondary)
            .multilineTextAlignment(.center)

        Spacer().frame(height: 20)

        Text(TelefamStrings.emailLabel)
            .font(.system(size: 14, weight: .semibold))
            .foregroundColor(TelefamColors.lightTextPrimary)
            .frame(maxWidth: .infinity, alignment: .leading)

        Spacer().frame(height: 8)

        HStack(spacing: 10) {
            LucideIcon(kind: .mail, size: 20, color: TelefamColors.crimson)
            TextField(
                "",
                text: Binding(
                    get: { state.email },
                    set: { viewModel.onEmailChanged($0) }
                ),
                prompt: Text(TelefamStrings.emailHint).foregroundColor(Color(hex: 0x9CA3AF))
            )
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .keyboardType(.emailAddress)
            .textContentType(.emailAddress)
            .submitLabel(.done)
            .focused($emailFocused)
            .onSubmit { viewModel.onSendOtp() }
            .foregroundColor(TelefamColors.lightTextPrimary)
            .font(.system(size: 15))
            .accessibilityIdentifier("email_input")
        }
        .padding(.horizontal, 14)
        .frame(height: 52)
        .background(Color.white)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(emailFocused ? TelefamColors.crimson : Color(hex: 0xDDE0E5), lineWidth: emailFocused ? 1.5 : 1)
        )

        if !state.errorMessage.isEmpty {
            Spacer().frame(height: 10)
            Text(state.errorMessage)
                .font(.system(size: 13))
                .foregroundColor(TelefamColors.crimson)
                .multilineTextAlignment(.center)
                .accessibilityIdentifier("auth_error_text")
        }

        Spacer().frame(height: 16)

        Button {
            emailFocused = false
            viewModel.onSendOtp()
        } label: {
            Group {
                if state.isLoading {
                    ProgressView()
                        .tint(.white)
                } else if state.lockRemainingSec > 0 {
                    Text(state.lockTimerText)
                        .font(.system(size: 15, weight: .bold))
                } else {
                    Text(TelefamStrings.sendOtp)
                        .font(.system(size: 15, weight: .bold))
                }
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(
                Capsule().fill(TelefamColors.crimson.opacity((!state.isLoading && state.lockRemainingSec == 0) ? 1.0 : 0.55))
            )
        }
        .disabled(state.isLoading || state.lockRemainingSec > 0)
        .accessibilityIdentifier("send_otp_button")

        Spacer().frame(height: 18)

        HStack(spacing: 12) {
            Rectangle().fill(Color(hex: 0xE5E7EB)).frame(height: 1)
            Text(TelefamStrings.orDivider)
                .font(.system(size: 13))
                .foregroundColor(TelefamColors.lightTextSecondary)
            Rectangle().fill(Color(hex: 0xE5E7EB)).frame(height: 1)
        }

        Spacer().frame(height: 18)

        Button {
            emailFocused = false
            viewModel.onContinueWithApple()
        } label: {
            HStack(spacing: 10) {
                AppleLogoView(size: 20, color: TelefamColors.lightTextPrimary)
                Text(TelefamStrings.continueApple)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(TelefamColors.lightTextPrimary)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(Color.white)
            .cornerRadius(14)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(Color(hex: 0xDDE0E5), lineWidth: 1)
            )
        }
        .disabled(state.isLoading)
        .accessibilityIdentifier("apple_signin_button")
    }

    @ViewBuilder
    private var otpAuthCardContent: some View {
        Text(TelefamStrings.enterOtpTitle)
            .font(.system(size: 21, weight: .bold))
            .foregroundColor(TelefamColors.crimson)

        Spacer().frame(height: 6)

        Text(TelefamStrings.otpSentTo)
            .font(.system(size: 13))
            .foregroundColor(TelefamColors.lightTextSecondary)

        Spacer().frame(height: 2)

        Text(state.email)
            .font(.system(size: 14, weight: .bold))
            .foregroundColor(TelefamColors.lightTextPrimary)
            .accessibilityIdentifier("otp_target_email")

        Spacer().frame(height: 22)

        ZStack {
            TextField(
                "",
                text: Binding(
                    get: { state.otp },
                    set: { viewModel.onOtpChanged($0) }
                )
            )
            .keyboardType(.numberPad)
            .textContentType(.oneTimeCode)
            .focused($otpFocused)
            .onSubmit { viewModel.onVerifyOtp() }
            .frame(width: 1, height: 1)
            .opacity(0.01)
            .accessibilityIdentifier("otp_input")

            HStack(spacing: 8) {
                ForEach(0..<6, id: \.self) { index in
                    let chars = Array(state.otp)
                    let charStr = index < chars.count ? String(chars[index]) : nil
                    let isActive = (index == min(chars.count, 5))

                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color.white)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(isActive ? TelefamColors.crimson : Color(hex: 0xDDE0E5), lineWidth: isActive ? 1.5 : 1)
                            )

                        if let charStr {
                            Text(charStr)
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(TelefamColors.lightTextPrimary)
                        } else {
                            Circle()
                                .fill(Color(hex: 0x9CA3AF))
                                .frame(width: 6, height: 6)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { otpFocused = true }
        }

        if !state.errorMessage.isEmpty {
            Spacer().frame(height: 12)
            Text(state.errorMessage)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(TelefamColors.crimson)
                .multilineTextAlignment(.center)
                .accessibilityIdentifier("otp_error_text")
        }

        Spacer().frame(height: 20)

        Button {
            otpFocused = false
            viewModel.onVerifyOtp()
        } label: {
            Group {
                if state.isLoading {
                    ProgressView().tint(.white)
                } else if state.lockRemainingSec > 0 {
                    Text(state.lockTimerText)
                        .font(.system(size: 15, weight: .bold))
                        .accessibilityIdentifier("otp_lock_countdown")
                } else {
                    Text(TelefamStrings.verifyOtp)
                        .font(.system(size: 15, weight: .bold))
                }
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(
                Capsule().fill(TelefamColors.crimson.opacity((!state.isLoading && state.lockRemainingSec == 0) ? 1.0 : 0.55))
            )
        }
        .disabled(state.isLoading || state.lockRemainingSec > 0)
        .accessibilityIdentifier("verify_otp_button")

        Spacer().frame(height: 18)

        if state.resendRemainingSec > 0 {
            VStack(spacing: 4) {
                HStack(spacing: 4) {
                    Text(TelefamStrings.resendCodeIn)
                        .font(.system(size: 13))
                        .foregroundColor(TelefamColors.lightTextSecondary)
                    Text(state.resendTimerText)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(TelefamColors.crimson)
                        .accessibilityIdentifier("resend_countdown_text")
                }
                HStack(spacing: 4) {
                    Text("\(TelefamStrings.afterCooldownPrefix) \(state.resendTimerText) \(TelefamStrings.afterCooldownMiddle)")
                        .font(.system(size: 12))
                        .foregroundColor(TelefamColors.lightTextSecondary)
                    Text(TelefamStrings.resendCode)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(TelefamColors.crimson.opacity(0.6))
                }
            }
        } else {
            Button {
                viewModel.onSendOtp()
            } label: {
                Text(TelefamStrings.resendCode)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(TelefamColors.crimson)
                    .frame(minHeight: 48)
                    .padding(.horizontal, 16)
            }
            .disabled(state.isLoading || state.lockRemainingSec > 0)
            .accessibilityIdentifier("resend_otp_button")
        }

        Spacer().frame(height: 6)

        Button {
            otpFocused = false
            viewModel.onChangeEmail()
        } label: {
            Text(TelefamStrings.changeEmail)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(TelefamColors.crimson)
                .frame(minHeight: 48)
                .padding(.horizontal, 16)
        }
        .accessibilityIdentifier("change_email_button")
    }
}

private struct AuthDecorativeWavesBackground: View {
    var body: some View {
        Canvas { context, size in
            let w = size.width
            let h = size.height

            var topRightWave = Path()
            topRightWave.move(to: CGPoint(x: w * 0.55, y: 0))
            topRightWave.addCurve(
                to: CGPoint(x: w, y: h * 0.07),
                control1: CGPoint(x: w * 0.70, y: h * 0.05),
                control2: CGPoint(x: w * 0.88, y: h * 0.02)
            )
            topRightWave.addLine(to: CGPoint(x: w, y: 0))
            topRightWave.closeSubpath()
            context.fill(topRightWave, with: .color(TelefamColors.blush))

            var topLeftWave = Path()
            topLeftWave.move(to: CGPoint(x: 0, y: 0))
            topLeftWave.addLine(to: CGPoint(x: w * 0.30, y: 0))
            topLeftWave.addCurve(
                to: CGPoint(x: 0, y: h * 0.06),
                control1: CGPoint(x: w * 0.18, y: h * 0.04),
                control2: CGPoint(x: w * 0.08, y: h * 0.03)
            )
            topLeftWave.closeSubpath()
            context.fill(topLeftWave, with: .color(TelefamColors.blush))

            var bottomWave = Path()
            bottomWave.move(to: CGPoint(x: 0, y: h * 0.87))
            bottomWave.addCurve(
                to: CGPoint(x: w * 0.72, y: h * 0.86),
                control1: CGPoint(x: w * 0.25, y: h * 0.81),
                control2: CGPoint(x: w * 0.48, y: h * 0.91)
            )
            bottomWave.addCurve(
                to: CGPoint(x: w, y: h * 0.83),
                control1: CGPoint(x: w * 0.86, y: h * 0.83),
                control2: CGPoint(x: w * 0.95, y: h * 0.85)
            )
            bottomWave.addLine(to: CGPoint(x: w, y: h))
            bottomWave.addLine(to: CGPoint(x: 0, y: h))
            bottomWave.closeSubpath()
            context.fill(bottomWave, with: .color(TelefamColors.crimson))
        }
    }
}
