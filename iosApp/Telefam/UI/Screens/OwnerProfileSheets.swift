import PhotosUI
import SwiftUI

struct EditBackgroundSheet: View {
    let backgrounds: [BackgroundPhotoItem]
    let bridge: TelefamCoreBridge
    let isUploading: Bool
    let errorMessage: String
    let onDismiss: () -> Void
    let onUploadBackground: (Data, String, Double, Double, Double, Double) -> Void
    let onDeleteBackground: (String) -> Void

    @Environment(\.telefamPalette) private var palette
    @State private var selectedItem: PhotosPickerItem?
    @State private var pendingCrop: (Data, String)?

    var body: some View {
        NavigationStack {
            VStack(spacing: 14) {
                if !errorMessage.isEmpty {
                    Text(errorMessage)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(TelefamColors.crimson)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .accessibilityIdentifier("background_error_text")
                }

                if !backgrounds.isEmpty {
                    ScrollView {
                        VStack(spacing: 10) {
                            ForEach(backgrounds) { bg in
                                ZStack(alignment: .topTrailing) {
                                    CachedRemoteImage(urlString: bg.imageUrl, bridge: bridge) {
                                        Rectangle().fill(palette.surfaceVariant)
                                    }
                                    .aspectRatio(16.0 / 9.0, contentMode: .fill)
                                    .frame(maxWidth: .infinity)
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 14)
                                            .stroke(palette.outline, lineWidth: 1)
                                    )

                                    Button {
                                        onDeleteBackground(bg.id)
                                    } label: {
                                        ZStack {
                                            Circle().fill(Color.black.opacity(0.65))
                                            LucideIcon(kind: .trash2, size: 18, color: .white)
                                        }
                                        .frame(width: 40, height: 40)
                                        .frame(width: 48, height: 48)
                                    }
                                    .disabled(isUploading)
                                    .padding(4)
                                    .accessibilityIdentifier("delete_bg_\(bg.id)")
                                }
                            }
                        }
                    }
                    .frame(maxHeight: 320)
                }

                PhotosPicker(selection: $selectedItem, matching: .images) {
                    HStack(spacing: 8) {
                        if isUploading {
                            ProgressView().tint(.white)
                        } else {
                            LucideIcon(kind: .camera, size: 18, color: .white)
                            Text("\(TelefamStrings.profileAddBackground) (\(backgrounds.count)/5)")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.white)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Capsule().fill(TelefamColors.crimson.opacity((backgrounds.count < 5 && !isUploading) ? 1.0 : 0.5)))
                }
                .disabled(backgrounds.count >= 5 || isUploading)
                .accessibilityIdentifier("add_background_button")

                Spacer(minLength: 0)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(palette.surface.ignoresSafeArea())
            .navigationTitle("\(TelefamStrings.profileBackgroundsTitle) (\(backgrounds.count)/5)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: onDismiss) {
                        LucideIcon(kind: .xMark, size: 20, color: palette.textSecondary)
                            .frame(width: 44, height: 44)
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .onChange(of: selectedItem) { item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self) {
                    let mime = ProfileSetupView.detectImageMime(data)
                    await MainActor.run {
                        pendingCrop = (data, mime)
                        selectedItem = nil
                    }
                }
            }
        }
        .fullScreenCover(isPresented: Binding(
            get: { pendingCrop != nil },
            set: { if !$0 { pendingCrop = nil } }
        )) {
            if let (data, mime) = pendingCrop {
                PhotoCropSheet(
                    imageData: data,
                    mimeType: mime,
                    aspectRatio: 16.0 / 10.0,
                    isCircularOverlay: false,
                    onCancel: { pendingCrop = nil },
                    onApplyCrop: { cx, cy, cw, ch in
                        pendingCrop = nil
                        onUploadBackground(data, mime, cx, cy, cw, ch)
                    }
                )
            }
        }
    }
}

struct EditProfileSheet: View {
    let profile: ProfileUiState
    let errorMessage: String
    let countries: [CountryOption]
    let bridge: TelefamCoreBridge
    let onDismiss: () -> Void
    let onUploadAvatarPhoto: (Data, String, Double, Double, Double, Double) -> Void
    let onCheckUsername: (String) -> Void
    let onSave: (String, String, String, String, String, String, String, String) -> Bool

    @Environment(\.telefamPalette) private var palette

    @State private var fullName: String
    @State private var username: String
    @State private var phoneIso: String
    @State private var phoneNumber: String
    @State private var gender: String
    @State private var dob: String
    @State private var location: String
    @State private var bio: String

    @State private var selectedAvatarItem: PhotosPickerItem?
    @State private var pendingAvatarCrop: (Data, String)?
    @State private var showCountrySheet = false
    @State private var showDobSheet = false
    @State private var dobDate = Calendar(identifier: .gregorian).date(byAdding: .year, value: -18, to: Date()) ?? Date()

    init(
        profile: ProfileUiState,
        errorMessage: String,
        countries: [CountryOption],
        bridge: TelefamCoreBridge,
        onDismiss: @escaping () -> Void,
        onUploadAvatarPhoto: @escaping (Data, String, Double, Double, Double, Double) -> Void,
        onCheckUsername: @escaping (String) -> Void,
        onSave: @escaping (String, String, String, String, String, String, String, String) -> Bool
    ) {
        self.profile = profile
        self.errorMessage = errorMessage
        self.countries = countries
        self.bridge = bridge
        self.onDismiss = onDismiss
        self.onUploadAvatarPhoto = onUploadAvatarPhoto
        self.onCheckUsername = onCheckUsername
        self.onSave = onSave

        _fullName = State(initialValue: profile.fullName)
        _username = State(initialValue: profile.username)
        _phoneIso = State(initialValue: profile.phoneCountryIso)
        _phoneNumber = State(initialValue: profile.phoneNumber)
        _gender = State(initialValue: profile.gender)
        _dob = State(initialValue: profile.dateOfBirth)
        _location = State(initialValue: profile.location)
        _bio = State(initialValue: profile.bio)
    }

    private var dialCode: String {
        countries.first(where: { $0.iso2.caseInsensitiveCompare(phoneIso) == .orderedSame })?.dialCode ?? profile.phoneDialCode
    }

    private var lockedUntilFormatted: String {
        guard profile.nextUsernameChangeAtEpochSec > 0 else { return "" }
        let date = Date(timeIntervalSince1970: TimeInterval(profile.nextUsernameChangeAtEpochSec))
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US")
        f.dateFormat = "MMM d, yyyy"
        return f.string(from: date)
    }

    var body: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 12) {
                    PhotosPicker(selection: $selectedAvatarItem, matching: .images) {
                        ZStack {
                            Circle()
                                .stroke(TelefamColors.crimson, lineWidth: 2)
                                .frame(width: 88, height: 88)
                            if !profile.avatarUrl.isEmpty {
                                CachedRemoteImage(urlString: profile.avatarUrl, bridge: bridge) {
                                    LucideIcon(kind: .camera, size: 28, color: TelefamColors.crimson)
                                }
                                .frame(width: 84, height: 84)
                                .clipShape(Circle())
                            } else {
                                LucideIcon(kind: .camera, size: 28, color: TelefamColors.crimson)
                            }
                            if profile.photoUploading {
                                ProgressView().tint(TelefamColors.crimson)
                            }
                        }
                    }
                    .accessibilityIdentifier("edit_avatar_button")

                    editFieldLabel(kind: .user, title: TelefamStrings.fullNameLabel)
                    TextField("", text: Binding(
                        get: { fullName },
                        set: { fullName = String($0.prefix(64)) }
                    ))
                    .padding(.horizontal, 14)
                    .frame(height: 50)
                    .background(palette.surface)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(palette.outline, lineWidth: 1))
                    .foregroundColor(palette.textPrimary)
                    .accessibilityIdentifier("edit_full_name_input")

                    HStack {
                        editFieldLabel(kind: .atSign, title: TelefamStrings.usernameLabel)
                        Spacer()
                        if !profile.canChangeUsername && !lockedUntilFormatted.isEmpty {
                            HStack(spacing: 4) {
                                LucideIcon(kind: .lock, size: 13, color: palette.textSecondary)
                                Text("\(TelefamStrings.profileUsernameLockedUntil) \(lockedUntilFormatted)")
                                    .font(.system(size: 11))
                                    .foregroundColor(palette.textSecondary)
                            }
                            .accessibilityIdentifier("edit_username_locked_badge")
                        } else if profile.canChangeUsername && username != profile.username {
                            if profile.usernameChecking {
                                ProgressView().scaleEffect(0.75).tint(TelefamColors.crimson)
                            } else if profile.usernameChecked {
                                HStack(spacing: 4) {
                                    if profile.usernameAvailable {
                                        LucideIcon(kind: .checkCircle, size: 14, color: TelefamColors.success)
                                    }
                                    Text(profile.usernameStatusMsg)
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundColor(profile.usernameAvailable ? TelefamColors.success : TelefamColors.crimson)
                                }
                                .accessibilityIdentifier("edit_username_status_text")
                            }
                        }
                    }

                    TextField("", text: Binding(
                        get: { username },
                        set: { newVal in
                            if profile.canChangeUsername {
                                let clean = String(newVal.lowercased().prefix(24))
                                username = clean
                                if clean != profile.username {
                                    onCheckUsername(clean)
                                }
                            }
                        }
                    ))
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .disabled(!profile.canChangeUsername)
                    .padding(.horizontal, 14)
                    .frame(height: 50)
                    .background(palette.surface)
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(profile.canChangeUsername ? palette.outline : palette.outline.opacity(0.5), lineWidth: 1)
                    )
                    .foregroundColor(profile.canChangeUsername ? palette.textPrimary : palette.textSecondary)
                    .accessibilityIdentifier("edit_username_input")

                    editFieldLabel(kind: .phone, title: TelefamStrings.phoneLabel)
                    HStack(spacing: 0) {
                        Button { showCountrySheet = true } label: {
                            HStack(spacing: 6) {
                                CountryFlagView(iso2: phoneIso)
                                Text(dialCode)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(palette.textPrimary)
                                LucideIcon(kind: .chevronDown, size: 16, color: palette.textSecondary)
                            }
                            .padding(.horizontal, 12)
                            .frame(height: 52)
                        }
                        Rectangle().fill(palette.outline).frame(width: 1, height: 28)
                        TextField("", text: Binding(
                            get: { phoneNumber },
                            set: { phoneNumber = String($0.prefix(20)) }
                        ))
                        .keyboardType(.phonePad)
                        .padding(.horizontal, 12)
                        .foregroundColor(palette.textPrimary)
                        .accessibilityIdentifier("edit_phone_input")
                    }
                    .frame(height: 52)
                    .background(palette.surface)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(palette.outline, lineWidth: 1))

                    editFieldLabel(kind: .users, title: TelefamStrings.genderLabel)
                    Menu {
                        ForEach(["Male", "Female", "Non-binary", "Prefer not to say"], id: \.self) { option in
                            Button(option) { gender = option }
                        }
                    } label: {
                        HStack {
                            Text(gender.isEmpty ? TelefamStrings.genderHint : gender)
                                .foregroundColor(gender.isEmpty ? palette.textSecondary : palette.textPrimary)
                            Spacer()
                            LucideIcon(kind: .chevronDown, size: 18, color: palette.textSecondary)
                        }
                        .padding(.horizontal, 14)
                        .frame(height: 52)
                        .background(palette.surface)
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(palette.outline, lineWidth: 1))
                    }

                    editFieldLabel(kind: .calendar, title: TelefamStrings.dobLabel)
                    Button { showDobSheet = true } label: {
                        HStack {
                            Text(dob.isEmpty ? TelefamStrings.dobHint : dob)
                                .foregroundColor(dob.isEmpty ? palette.textSecondary : palette.textPrimary)
                            Spacer()
                            LucideIcon(kind: .chevronDown, size: 18, color: palette.textSecondary)
                        }
                        .padding(.horizontal, 14)
                        .frame(height: 52)
                        .background(palette.surface)
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(palette.outline, lineWidth: 1))
                    }

                    editFieldLabel(kind: .mapPin, title: TelefamStrings.locationLabel)
                    TextField("", text: Binding(
                        get: { location },
                        set: { location = String($0.prefix(80)) }
                    ))
                    .padding(.horizontal, 14)
                    .frame(height: 50)
                    .background(palette.surface)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(palette.outline, lineWidth: 1))
                    .foregroundColor(palette.textPrimary)
                    .accessibilityIdentifier("edit_location_input")

                    editFieldLabel(kind: .messageSquare, title: TelefamStrings.bioLabel)
                    ZStack(alignment: .bottomTrailing) {
                        TextEditor(text: Binding(
                            get: { bio },
                            set: { bio = String($0.prefix(150)) }
                        ))
                        .scrollContentBackground(.hidden)
                        .foregroundColor(palette.textPrimary)
                        .font(.system(size: 15))
                        .frame(minHeight: 84)
                        .padding(8)
                        .padding(.bottom, 18)
                        .accessibilityIdentifier("edit_bio_input")

                        Text("\(bio.count)/150")
                            .font(.system(size: 12))
                            .foregroundColor(palette.textSecondary)
                            .padding(12)
                    }
                    .background(palette.surface)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(palette.outline, lineWidth: 1))

                    if !errorMessage.isEmpty {
                        Text(errorMessage)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(TelefamColors.crimson)
                            .multilineTextAlignment(.center)
                            .accessibilityIdentifier("edit_profile_error_text")
                    }

                    Button {
                        let ok = onSave(fullName, username, phoneIso, phoneNumber, gender, dob, location, bio)
                        if ok { onDismiss() }
                    } label: {
                        Text(TelefamStrings.profileSave)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(Capsule().fill(TelefamColors.crimson))
                    }
                    .disabled(profile.photoUploading)
                    .padding(.top, 6)
                    .accessibilityIdentifier("edit_profile_save_button")
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
            }
            .background(palette.surface.ignoresSafeArea())
            .navigationTitle(TelefamStrings.profileEditProfile)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: onDismiss) {
                        LucideIcon(kind: .xMark, size: 20, color: palette.textSecondary)
                            .frame(width: 44, height: 44)
                    }
                }
            }
        }
        .presentationDetents([.large])
        .sheet(isPresented: $showCountrySheet) {
            CountryPickerSheet(countries: countries) { chosen in
                phoneIso = chosen.iso2
                showCountrySheet = false
            }
        }
        .sheet(isPresented: $showDobSheet) {
            DobPickerSheet(selectedDate: $dobDate) { formatted in
                dob = formatted
                showDobSheet = false
            } onCancel: {
                showDobSheet = false
            }
        }
        .onChange(of: selectedAvatarItem) { item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self) {
                    let mime = ProfileSetupView.detectImageMime(data)
                    await MainActor.run {
                        pendingAvatarCrop = (data, mime)
                        selectedAvatarItem = nil
                    }
                }
            }
        }
        .fullScreenCover(isPresented: Binding(
            get: { pendingAvatarCrop != nil },
            set: { if !$0 { pendingAvatarCrop = nil } }
        )) {
            if let (data, mime) = pendingAvatarCrop {
                PhotoCropSheet(
                    imageData: data,
                    mimeType: mime,
                    aspectRatio: 1.0,
                    isCircularOverlay: true,
                    onCancel: { pendingAvatarCrop = nil },
                    onApplyCrop: { cx, cy, cw, ch in
                        pendingAvatarCrop = nil
                        onUploadAvatarPhoto(data, mime, cx, cy, cw, ch)
                    }
                )
            }
        }
    }

    private func editFieldLabel(kind: LucideIconKind, title: String) -> some View {
        HStack(spacing: 6) {
            LucideIcon(kind: kind, size: 16, color: TelefamColors.crimson)
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(palette.textPrimary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct FollowListSheet: View {
    let initialTab: String
    let contacts: [ContactItem]
    let bridge: TelefamCoreBridge
    let onDismiss: () -> Void
    let onToggleFollow: (ContactItem) -> Void

    @Environment(\.telefamPalette) private var palette
    @State private var activeTab: String

    init(
        initialTab: String,
        contacts: [ContactItem],
        bridge: TelefamCoreBridge,
        onDismiss: @escaping () -> Void,
        onToggleFollow: @escaping (ContactItem) -> Void
    ) {
        self.initialTab = initialTab
        self.contacts = contacts
        self.bridge = bridge
        self.onDismiss = onDismiss
        self.onToggleFollow = onToggleFollow
        _activeTab = State(initialValue: initialTab)
    }

    private var displayed: [ContactItem] {
        if activeTab == "followers" {
            return contacts.filter { $0.followsMe }
        }
        return contacts.filter { $0.iFollow }
    }

    var body: some View {
        VStack(spacing: 14) {
            HStack(spacing: 10) {
                ForEach([
                    ("following", TelefamStrings.profileFollowing),
                    ("followers", TelefamStrings.profileFollowers)
                ], id: \.0) { key, title in
                    let selected = (activeTab == key)
                    Button {
                        withAnimation(.spring(response: 0.25, dampingFraction: 0.85)) {
                            activeTab = key
                        }
                    } label: {
                        Text(title)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(selected ? .white : palette.textPrimary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 42)
                            .background(Capsule().fill(selected ? TelefamColors.crimson : palette.surfaceVariant))
                    }
                    .accessibilityIdentifier("follow_sheet_tab_\(key)")
                }
            }
            .padding(.horizontal, 18)
            .padding(.top, 16)

            if displayed.isEmpty {
                Spacer()
                Text(TelefamStrings.contactsEmpty)
                    .font(.system(size: 14))
                    .foregroundColor(palette.textSecondary)
                    .accessibilityIdentifier("follow_sheet_empty")
                Spacer()
            } else {
                ScrollView(showsIndicators: false) {
                    LazyVStack(spacing: 12) {
                        ForEach(displayed) { contact in
                            ContactRowView(
                                contact: contact,
                                bridge: bridge,
                                palette: palette
                            ) {
                                onToggleFollow(contact)
                            }
                            .accessibilityIdentifier("follow_sheet_row_\(contact.userId)")
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 18)
                }
            }
        }
        .background(palette.surface.ignoresSafeArea())
        .presentationDetents([.medium, .large])
    }
}
