import PhotosUI
import SwiftUI
import UIKit

struct ProfileSetupView: View {
    @ObservedObject var viewModel: TelefamViewModel

    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var pendingCropImage: (Data, String)?
    @State private var localPreviewImage: UIImage?
    @State private var showCountrySheet = false
    @State private var showDobSheet = false
    @State private var selectedDobDate = Calendar(identifier: .gregorian).date(byAdding: .year, value: -18, to: Date()) ?? Date()

    private var state: TelefamUiState { viewModel.uiState }
    private var profile: ProfileUiState { viewModel.uiState.profile }

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                HStack {
                    Button {
                        viewModel.onChangeEmail()
                    } label: {
                        LucideIcon(kind: .arrowLeft, size: 22, color: .white, lineWidth: 2.2)
                            .frame(width: 48, height: 48)
                    }
                    .accessibilityIdentifier("profile_back_button")
                    Spacer()
                }

                VStack(spacing: 2) {
                    Text(TelefamStrings.profileSetupTitle)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)
                    Text(TelefamStrings.profileSetupSubtitle)
                        .font(.system(size: 13))
                        .foregroundColor(.white.opacity(0.88))
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 14)
            .background(
                TelefamColors.crimson
                    .clipShape(RoundedCornersShape(bottomLeft: 24, bottomRight: 24))
                    .ignoresSafeArea(edges: .top)
            )

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                        ZStack(alignment: .bottomTrailing) {
                            ZStack {
                                Circle()
                                    .fill(TelefamColors.blush)
                                    .overlay(
                                        Circle()
                                            .stroke(style: StrokeStyle(lineWidth: 1.6, dash: [7, 5]))
                                            .foregroundColor(TelefamColors.crimson)
                                    )

                                if let localPreviewImage {
                                    Image(uiImage: localPreviewImage)
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                        .frame(width: 98, height: 98)
                                        .clipShape(Circle())
                                } else if !profile.avatarUrl.isEmpty {
                                    CachedRemoteImage(urlString: profile.avatarUrl, bridge: viewModel.bridge) {
                                        LucideIcon(kind: .camera, size: 34, color: TelefamColors.crimson)
                                    }
                                    .frame(width: 98, height: 98)
                                    .clipShape(Circle())
                                } else {
                                    LucideIcon(kind: .camera, size: 34, color: TelefamColors.crimson)
                                }

                                if profile.photoUploading {
                                    ProgressView()
                                        .tint(TelefamColors.crimson)
                                }
                            }
                            .frame(width: 112, height: 112)

                            ZStack {
                                Circle()
                                    .fill(TelefamColors.crimson)
                                    .frame(width: 30, height: 30)
                                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                                LucideIcon(kind: .plus, size: 15, color: .white, lineWidth: 2.4)
                            }
                            .offset(x: -2, y: -2)
                        }
                    }
                    .accessibilityIdentifier("upload_photo_button")

                    Spacer().frame(height: 12)

                    Text(TelefamStrings.uploadPhotoTitle)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(TelefamColors.lightTextPrimary)

                    Spacer().frame(height: 4)

                    Text(TelefamStrings.uploadPhotoSubtitle)
                        .font(.system(size: 13))
                        .foregroundColor(TelefamColors.lightTextSecondary)
                        .multilineTextAlignment(.center)

                    Spacer().frame(height: 22)

                    setupFieldLabel(kind: .user, title: TelefamStrings.fullNameLabel)
                    TextField(
                        "",
                        text: Binding(
                            get: { profile.fullName },
                            set: { viewModel.onProfileFieldChanged(field: "fullName", value: $0) }
                        ),
                        prompt: Text(TelefamStrings.fullNameHint).foregroundColor(Color(hex: 0x9CA3AF))
                    )
                    .padding(.horizontal, 14)
                    .frame(height: 54)
                    .background(Color.white)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: 0xDDE0E5), lineWidth: 1))
                    .foregroundColor(TelefamColors.lightTextPrimary)
                    .accessibilityIdentifier("full_name_input")

                    Spacer().frame(height: 16)

                    HStack {
                        setupFieldLabel(kind: .atSign, title: TelefamStrings.usernameLabel)
                        Spacer()
                        if profile.usernameChecking {
                            ProgressView().scaleEffect(0.75).tint(TelefamColors.crimson)
                        } else if profile.usernameChecked {
                            HStack(spacing: 4) {
                                if profile.usernameAvailable {
                                    LucideIcon(kind: .checkCircle, size: 15, color: TelefamColors.success)
                                }
                                Text(profile.usernameStatusMsg)
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundColor(profile.usernameAvailable ? TelefamColors.success : TelefamColors.crimson)
                                    .accessibilityIdentifier("username_status_text")
                            }
                        }
                    }

                    TextField(
                        "",
                        text: Binding(
                            get: { profile.username },
                            set: { viewModel.onProfileFieldChanged(field: "username", value: $0) }
                        ),
                        prompt: Text(TelefamStrings.usernameHint).foregroundColor(Color(hex: 0x9CA3AF))
                    )
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .padding(.horizontal, 14)
                    .frame(height: 54)
                    .background(Color.white)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: 0xDDE0E5), lineWidth: 1))
                    .foregroundColor(TelefamColors.lightTextPrimary)
                    .accessibilityIdentifier("username_input")

                    Spacer().frame(height: 16)

                    setupFieldLabel(kind: .phone, title: TelefamStrings.phoneLabel)
                    HStack(spacing: 0) {
                        Button {
                            showCountrySheet = true
                        } label: {
                            HStack(spacing: 6) {
                                CountryFlagView(iso2: profile.phoneCountryIso)
                                Text(profile.phoneDialCode)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(TelefamColors.lightTextPrimary)
                                LucideIcon(kind: .chevronDown, size: 16, color: TelefamColors.lightTextSecondary)
                            }
                            .padding(.horizontal, 12)
                            .frame(height: 56)
                        }
                        .accessibilityIdentifier("country_selector_button")

                        Rectangle()
                            .fill(Color(hex: 0xDDE0E5))
                            .frame(width: 1, height: 30)

                        TextField(
                            "",
                            text: Binding(
                                get: { profile.phoneNumber },
                                set: { viewModel.onProfileFieldChanged(field: "phoneNumber", value: $0) }
                            ),
                            prompt: Text(TelefamStrings.phoneHint).foregroundColor(Color(hex: 0x9CA3AF))
                        )
                        .keyboardType(.phonePad)
                        .padding(.horizontal, 14)
                        .foregroundColor(TelefamColors.lightTextPrimary)
                        .font(.system(size: 15))
                        .accessibilityIdentifier("phone_input")
                    }
                    .frame(height: 56)
                    .background(Color.white)
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(profile.phoneError.isEmpty ? Color(hex: 0xDDE0E5) : TelefamColors.crimson, lineWidth: 1)
                    )

                    if !profile.phoneError.isEmpty {
                        Text(profile.phoneError)
                            .font(.system(size: 12))
                            .foregroundColor(TelefamColors.crimson)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.top, 4)
                    }

                    Spacer().frame(height: 16)

                    setupFieldLabel(kind: .users, title: TelefamStrings.genderLabel)
                    Menu {
                        ForEach(["Male", "Female", "Non-binary", "Prefer not to say"], id: \.self) { option in
                            Button(option) {
                                viewModel.onProfileFieldChanged(field: "gender", value: option)
                            }
                        }
                    } label: {
                        HStack {
                            Text(profile.gender.isEmpty ? TelefamStrings.genderHint : profile.gender)
                                .font(.system(size: 15))
                                .foregroundColor(profile.gender.isEmpty ? Color(hex: 0x9CA3AF) : TelefamColors.lightTextPrimary)
                            Spacer()
                            LucideIcon(kind: .chevronDown, size: 18, color: TelefamColors.lightTextSecondary)
                        }
                        .padding(.horizontal, 16)
                        .frame(height: 54)
                        .background(Color.white)
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: 0xDDE0E5), lineWidth: 1))
                    }
                    .accessibilityIdentifier("gender_selector")

                    Spacer().frame(height: 16)

                    setupFieldLabel(kind: .calendar, title: TelefamStrings.dobLabel)
                    Button {
                        showDobSheet = true
                    } label: {
                        HStack {
                            Text(profile.dateOfBirth.isEmpty ? TelefamStrings.dobHint : profile.dateOfBirth)
                                .font(.system(size: 15))
                                .foregroundColor(profile.dateOfBirth.isEmpty ? Color(hex: 0x9CA3AF) : TelefamColors.lightTextPrimary)
                            Spacer()
                            LucideIcon(kind: .chevronDown, size: 18, color: TelefamColors.lightTextSecondary)
                        }
                        .padding(.horizontal, 16)
                        .frame(height: 54)
                        .background(Color.white)
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(profile.dobError.isEmpty ? Color(hex: 0xDDE0E5) : TelefamColors.crimson, lineWidth: 1)
                        )
                    }
                    .accessibilityIdentifier("dob_selector")

                    if !profile.dobError.isEmpty {
                        Text(profile.dobError)
                            .font(.system(size: 12))
                            .foregroundColor(TelefamColors.crimson)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.top, 4)
                    }

                    Spacer().frame(height: 16)

                    setupFieldLabel(kind: .mapPin, title: TelefamStrings.locationLabel)
                    TextField(
                        "",
                        text: Binding(
                            get: { profile.location },
                            set: { viewModel.onProfileFieldChanged(field: "location", value: $0) }
                        ),
                        prompt: Text(TelefamStrings.locationHint).foregroundColor(Color(hex: 0x9CA3AF))
                    )
                    .padding(.horizontal, 14)
                    .frame(height: 54)
                    .background(Color.white)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: 0xDDE0E5), lineWidth: 1))
                    .foregroundColor(TelefamColors.lightTextPrimary)
                    .accessibilityIdentifier("location_input")

                    Spacer().frame(height: 16)

                    setupFieldLabel(kind: .messageSquare, title: TelefamStrings.bioLabel)
                    ZStack(alignment: .bottomTrailing) {
                        TextEditor(
                            text: Binding(
                                get: { profile.bio },
                                set: { viewModel.onProfileFieldChanged(field: "bio", value: String($0.prefix(150))) }
                            )
                        )
                        .scrollContentBackground(.hidden)
                        .font(.system(size: 15))
                        .foregroundColor(TelefamColors.lightTextPrimary)
                        .frame(minHeight: 92)
                        .padding(8)
                        .padding(.bottom, 18)
                        .overlay(alignment: .topLeading) {
                            if profile.bio.isEmpty {
                                Text(TelefamStrings.bioHint)
                                    .font(.system(size: 15))
                                    .foregroundColor(Color(hex: 0x9CA3AF))
                                    .padding(.horizontal, 13)
                                    .padding(.vertical, 16)
                                    .allowsHitTesting(false)
                            }
                        }
                        .accessibilityIdentifier("bio_input")

                        Text("\(profile.bio.count)/150")
                            .font(.system(size: 12))
                            .foregroundColor(Color(hex: 0x9CA3AF))
                            .padding(12)
                    }
                    .background(Color.white)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: 0xDDE0E5), lineWidth: 1))

                    if !state.errorMessage.isEmpty {
                        Spacer().frame(height: 12)
                        Text(state.errorMessage)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(TelefamColors.crimson)
                            .multilineTextAlignment(.center)
                            .accessibilityIdentifier("profile_error_text")
                    }

                    Spacer().frame(height: 22)

                    Button {
                        viewModel.onSubmitProfile()
                    } label: {
                        Group {
                            if state.isLoading {
                                ProgressView().tint(.white)
                            } else {
                                Text(TelefamStrings.continueText)
                                    .font(.system(size: 16, weight: .bold))
                            }
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(
                            RoundedRectangle(cornerRadius: 14)
                                .fill(TelefamColors.crimson.opacity((!state.isLoading && !profile.photoUploading) ? 1.0 : 0.6))
                        )
                    }
                    .disabled(state.isLoading || profile.photoUploading)
                    .accessibilityIdentifier("profile_continue_button")

                    Spacer().frame(height: 24)
                }
                .frame(maxWidth: 540)
                .padding(.horizontal, 20)
                .padding(.vertical, 20)
                .frame(maxWidth: .infinity)
            }
            .background(Color.white)
        }
        .background(Color.white.ignoresSafeArea())
        .onChange(of: selectedPhotoItem) { newItem in
            guard let newItem else { return }
            Task {
                if let data = try? await newItem.loadTransferable(type: Data.self) {
                    let mime = Self.detectImageMime(data)
                    await MainActor.run {
                        pendingCropImage = (data, mime)
                        selectedPhotoItem = nil
                    }
                }
            }
        }
        .sheet(isPresented: $showCountrySheet) {
            CountryPickerSheet(countries: viewModel.countries) { chosen in
                viewModel.onProfileFieldChanged(field: "phoneCountryIso", value: chosen.iso2)
                showCountrySheet = false
            }
        }
        .sheet(isPresented: $showDobSheet) {
            DobPickerSheet(selectedDate: $selectedDobDate) { formatted in
                viewModel.onProfileFieldChanged(field: "dateOfBirth", value: formatted)
                showDobSheet = false
            } onCancel: {
                showDobSheet = false
            }
        }
        .fullScreenCover(isPresented: Binding(
            get: { pendingCropImage != nil },
            set: { if !$0 { pendingCropImage = nil } }
        )) {
            if let (data, mime) = pendingCropImage {
                PhotoCropSheet(
                    imageData: data,
                    mimeType: mime,
                    aspectRatio: 1.0,
                    isCircularOverlay: true,
                    onCancel: { pendingCropImage = nil },
                    onApplyCrop: { cx, cy, cw, ch in
                        localPreviewImage = UIImage(data: data)
                        pendingCropImage = nil
                        viewModel.onUploadCroppedAvatar(data: data, mimeType: mime, cropX: cx, cropY: cy, cropW: cw, cropH: ch)
                    }
                )
            }
        }
    }

    private func setupFieldLabel(kind: LucideIconKind, title: String) -> some View {
        HStack(spacing: 8) {
            LucideIcon(kind: kind, size: 18, color: TelefamColors.crimson)
            Text(title)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(TelefamColors.lightTextPrimary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, 8)
    }

    static func detectImageMime(_ data: Data) -> String {
        if data.count >= 8 && data.prefix(4) == Data([0x89, 0x50, 0x4E, 0x47]) {
            return "image/png"
        }
        if data.count >= 12 && data.prefix(4) == Data([0x52, 0x49, 0x46, 0x46]) && data[8..<12] == Data([0x57, 0x45, 0x42, 0x50]) {
            return "image/webp"
        }
        return "image/jpeg"
    }
}

struct CountryPickerSheet: View {
    let countries: [CountryOption]
    let onSelect: (CountryOption) -> Void

    @Environment(\.telefamPalette) private var palette

    var body: some View {
        NavigationStack {
            List(countries) { c in
                Button {
                    onSelect(c)
                } label: {
                    HStack(spacing: 12) {
                        CountryFlagView(iso2: c.iso2)
                        Text(c.name)
                            .font(.system(size: 15))
                            .foregroundColor(palette.textPrimary)
                        Spacer()
                        Text(c.dialCode)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(TelefamColors.crimson)
                    }
                    .frame(minHeight: 44)
                }
                .listRowBackground(palette.surface)
            }
            .listStyle(.plain)
            .background(palette.surface)
            .navigationTitle("Select Country")
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.medium, .large])
    }
}

struct DobPickerSheet: View {
    @Binding var selectedDate: Date
    let onConfirm: (String) -> Void
    let onCancel: () -> Void

    var body: some View {
        let maxDate = Date()
        VStack(spacing: 16) {
            HStack {
                Button(TelefamStrings.cancel, action: onCancel)
                    .foregroundColor(TelefamColors.lightTextSecondary)
                    .frame(minWidth: 48, minHeight: 48)
                Spacer()
                Text(TelefamStrings.dobLabel)
                    .font(.system(size: 17, weight: .bold))
                Spacer()
                Button(TelefamStrings.applyCrop) {
                    let f = DateFormatter()
                    f.calendar = Calendar(identifier: .gregorian)
                    f.locale = Locale(identifier: "en_US_POSIX")
                    f.dateFormat = "yyyy-MM-dd"
                    onConfirm(f.string(from: selectedDate))
                }
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(TelefamColors.crimson)
                .frame(minWidth: 48, minHeight: 48)
            }
            .padding(.horizontal, 16)

            DatePicker(
                TelefamStrings.dobLabel,
                selection: $selectedDate,
                in: ...maxDate,
                displayedComponents: .date
            )
            .datePickerStyle(.graphical)
            .tint(TelefamColors.crimson)
            .padding(.horizontal, 12)

            Spacer()
        }
        .padding(.top, 8)
        .presentationDetents([.medium, .large])
    }
}

struct RoundedCornersShape: Shape {
    var topLeft: CGFloat = 0
    var topRight: CGFloat = 0
    var bottomLeft: CGFloat = 0
    var bottomRight: CGFloat = 0

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let tl = min(topLeft, min(rect.width, rect.height) / 2)
        let tr = min(topRight, min(rect.width, rect.height) / 2)
        let bl = min(bottomLeft, min(rect.width, rect.height) / 2)
        let br = min(bottomRight, min(rect.width, rect.height) / 2)

        path.move(to: CGPoint(x: rect.minX + tl, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX - tr, y: rect.minY))
        path.addArc(center: CGPoint(x: rect.maxX - tr, y: rect.minY + tr), radius: tr, startAngle: .degrees(-90), endAngle: .degrees(0), clockwise: false)
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY - br))
        path.addArc(center: CGPoint(x: rect.maxX - br, y: rect.maxY - br), radius: br, startAngle: .degrees(0), endAngle: .degrees(90), clockwise: false)
        path.addLine(to: CGPoint(x: rect.minX + bl, y: rect.maxY))
        path.addArc(center: CGPoint(x: rect.minX + bl, y: rect.maxY - bl), radius: bl, startAngle: .degrees(90), endAngle: .degrees(180), clockwise: false)
        path.addLine(to: CGPoint(x: rect.minX, y: rect.minY + tl))
        path.addArc(center: CGPoint(x: rect.minX + tl, y: rect.minY + tl), radius: tl, startAngle: .degrees(180), endAngle: .degrees(270), clockwise: false)
        path.closeSubpath()
        return path
    }
}
