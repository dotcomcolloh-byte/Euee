import Combine
import SwiftUI

struct OwnerProfileView: View {
    @ObservedObject var viewModel: TelefamViewModel
    let onBack: () -> Void

    @Environment(\.telefamPalette) private var palette
    @State private var showEditProfileSheet = false
    @State private var showEditBackgroundSheet = false
    @State private var followListInitialTab: String?
    @State private var showShareSheet = false

    private var state: TelefamUiState { viewModel.uiState }
    private var profile: ProfileUiState { viewModel.uiState.profile }

    private var followingCount: Int {
        viewModel.allCachedContacts.isEmpty ? profile.followingCount : viewModel.allCachedContacts.filter { $0.iFollow }.count
    }

    private var followersCount: Int {
        viewModel.allCachedContacts.isEmpty ? profile.followersCount : viewModel.allCachedContacts.filter { $0.followsMe }.count
    }

    private var displayName: String {
        if !profile.fullName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return profile.fullName
        }
        if !profile.username.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return profile.username
        }
        return TelefamStrings.appName
    }

    private var safeTopInset: CGFloat {
        for scene in UIApplication.shared.connectedScenes {
            if let windowScene = scene as? UIWindowScene,
               let win = windowScene.windows.first(where: { $0.isKeyWindow }) ?? windowScene.windows.first {
                return win.safeAreaInsets.top
            }
        }
        return 0
    }

    var body: some View {
        let topPad = max(safeTopInset, 12)
        let bannerHeight: CGFloat = 178 + topPad

        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                ZStack(alignment: .top) {
                    OwnerProfileBackgroundBanner(
                        displayName: displayName,
                        backgrounds: profile.backgroundPhotos,
                        bridge: viewModel.bridge,
                        isDark: palette.isDark,
                        isUploading: profile.backgroundUploading,
                        topInset: topPad,
                        bannerHeight: bannerHeight,
                        onBack: onBack,
                        onSubscribers: { viewModel.onComingSoon(TelefamStrings.profileSubscribers) },
                        onSettings: { viewModel.onComingSoon(TelefamStrings.profileSettings) },
                        onMenu: { viewModel.onComingSoon(TelefamStrings.profileMenu) },
                        onEditBackground: {
                            viewModel.onClearError()
                            showEditBackgroundSheet = true
                        }
                    )

                    Button {
                        viewModel.onClearError()
                        showEditProfileSheet = true
                    } label: {
                        ZStack {
                            Circle()
                                .fill(
                                    LinearGradient(
                                        colors: [Color(hex: 0xE91E63), TelefamColors.crimson, Color(hex: 0xFF5722)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                            Circle()
                                .fill(palette.background)
                                .padding(2.5)

                            if !profile.avatarUrl.isEmpty {
                                CachedRemoteImage(urlString: profile.avatarUrl, bridge: viewModel.bridge) {
                                    initialsText
                                }
                                .frame(width: 91, height: 91)
                                .clipShape(Circle())
                            } else {
                                initialsText
                            }
                        }
                        .frame(width: 96, height: 96)
                    }
                    .padding(.top, bannerHeight - 52)
                    .accessibilityIdentifier("profile_owner_avatar")
                }

                VStack(spacing: 0) {
                    Spacer().frame(height: 10)

                    Text("@\(profile.username.isEmpty ? "user" : profile.username)")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(palette.textPrimary)
                        .accessibilityIdentifier("profile_username_text")

                    Spacer().frame(height: 18)

                    HStack(spacing: 0) {
                        profileStatItem(
                            count: followingCount,
                            label: TelefamStrings.profileFollowing,
                            onTap: { followListInitialTab = "following" }
                        )
                        .frame(maxWidth: .infinity)
                        .accessibilityIdentifier("profile_following_stat")

                        Rectangle()
                            .fill(palette.outline.opacity(0.55))
                            .frame(width: 1, height: 26)

                        profileStatItem(
                            count: followersCount,
                            label: TelefamStrings.profileFollowers,
                            onTap: { followListInitialTab = "followers" }
                        )
                        .frame(maxWidth: .infinity)
                        .accessibilityIdentifier("profile_followers_stat")

                        Rectangle()
                            .fill(palette.outline.opacity(0.55))
                            .frame(width: 1, height: 26)

                        profileStatItem(
                            count: profile.likesCount,
                            label: TelefamStrings.profileLikes,
                            onTap: nil
                        )
                        .frame(maxWidth: .infinity)
                        .accessibilityIdentifier("profile_likes_stat")
                    }
                    .padding(.horizontal, 24)

                    Spacer().frame(height: 18)

                    HStack(spacing: 12) {
                        Button {
                            viewModel.onClearError()
                            showEditProfileSheet = true
                        } label: {
                            HStack(spacing: 8) {
                                LucideIcon(kind: .pencil, size: 16, color: .white)
                                Text(TelefamStrings.profileEditProfile)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(.white)
                                    .lineLimit(1)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(Capsule().fill(TelefamColors.crimson))
                        }
                        .accessibilityIdentifier("profile_edit_button")

                        Button {
                            showShareSheet = true
                        } label: {
                            HStack(spacing: 8) {
                                LucideIcon(kind: .share2, size: 16, color: palette.textPrimary)
                                Text(TelefamStrings.profileShareProfile)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(palette.textPrimary)
                                    .lineLimit(1)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(Capsule().fill(palette.surface))
                            .overlay(Capsule().stroke(palette.outline, lineWidth: 1))
                        }
                        .accessibilityIdentifier("profile_share_button")
                    }
                    .padding(.horizontal, 24)

                    if !profile.location.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        Spacer().frame(height: 14)
                        HStack(spacing: 5) {
                            LucideIcon(kind: .mapPin, size: 15, color: TelefamColors.crimson)
                            Text(profile.location)
                                .font(.system(size: 14))
                                .foregroundColor(palette.textSecondary)
                                .lineLimit(1)
                        }
                        .padding(.horizontal, 24)
                        .accessibilityIdentifier("profile_location_text")
                    }

                    if !profile.bio.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        Spacer().frame(height: 8)
                        Text(profile.bio)
                            .font(.system(size: 13))
                            .foregroundColor(palette.textSecondary)
                            .multilineTextAlignment(.center)
                            .lineLimit(3)
                            .padding(.horizontal, 32)
                            .accessibilityIdentifier("profile_bio_text")
                    }

                    Spacer().frame(height: 16)

                    profileContentTabsRow

                    if state.profileContentTab == .posts {
                        HStack {
                            Button {
                                viewModel.onComingSoon("Filter")
                            } label: {
                                HStack(spacing: 4) {
                                    Text(TelefamStrings.profileAllPosts)
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundColor(palette.textPrimary)
                                    LucideIcon(kind: .chevronDown, size: 16, color: palette.textPrimary)
                                }
                                .frame(minHeight: 48)
                            }

                            Spacer()

                            Button {
                                viewModel.onComingSoon("Filter")
                            } label: {
                                LucideIcon(kind: .slidersHorizontal, size: 18, color: palette.textPrimary)
                                    .frame(width: 48, height: 48)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 6)
                    }

                    Group {
                        if state.profileContentTab == .posts {
                            Text(TelefamStrings.profileNoPosts)
                        } else {
                            Text(TelefamStrings.comingSoon)
                        }
                    }
                    .font(.system(size: 15))
                    .foregroundColor(palette.textSecondary)
                    .frame(maxWidth: .infinity, minHeight: 180)
                    .padding(.vertical, 40)
                    .accessibilityIdentifier("profile_tab_empty_text")
                    .transition(.opacity)
                    .animation(.spring(response: 0.28, dampingFraction: 0.85), value: state.profileContentTab)
                }
                .frame(maxWidth: 600)
                .frame(maxWidth: .infinity)
            }
        }
        .ignoresSafeArea(edges: .top)
        .background(palette.background.ignoresSafeArea())
        .sheet(isPresented: $showEditBackgroundSheet, onDismiss: { viewModel.onClearError() }) {
            EditBackgroundSheet(
                backgrounds: profile.backgroundPhotos,
                bridge: viewModel.bridge,
                isUploading: profile.backgroundUploading,
                errorMessage: state.errorMessage,
                onDismiss: {
                    viewModel.onClearError()
                    showEditBackgroundSheet = false
                },
                onUploadBackground: { data, mime, cx, cy, cw, ch in
                    viewModel.onUploadBackgroundPhoto(data: data, mimeType: mime, cropX: cx, cropY: cy, cropW: cw, cropH: ch)
                },
                onDeleteBackground: { id in
                    viewModel.onDeleteBackgroundPhoto(id)
                }
            )
        }
        .sheet(isPresented: $showEditProfileSheet, onDismiss: { viewModel.onClearError() }) {
            EditProfileSheet(
                profile: profile,
                errorMessage: state.errorMessage,
                countries: viewModel.countries,
                bridge: viewModel.bridge,
                onDismiss: {
                    viewModel.onClearError()
                    showEditProfileSheet = false
                },
                onUploadAvatarPhoto: { data, mime, cx, cy, cw, ch in
                    viewModel.onUploadCroppedAvatar(data: data, mimeType: mime, cropX: cx, cropY: cy, cropW: cw, cropH: ch)
                },
                onCheckUsername: { un in
                    viewModel.onCheckUsername(un)
                },
                onSave: { fn, un, iso, phone, gender, dob, loc, bio in
                    viewModel.onSaveEditedProfile(
                        fullName: fn,
                        username: un,
                        phoneCountryIso: iso,
                        phoneNumber: phone,
                        gender: gender,
                        dateOfBirth: dob,
                        location: loc,
                        bio: bio
                    )
                }
            )
        }
        .sheet(isPresented: Binding(
            get: { followListInitialTab != nil },
            set: { if !$0 { followListInitialTab = nil } }
        )) {
            if let tab = followListInitialTab {
                FollowListSheet(
                    initialTab: tab,
                    contacts: viewModel.allCachedContacts,
                    bridge: viewModel.bridge,
                    onDismiss: { followListInitialTab = nil },
                    onToggleFollow: { contact in viewModel.onToggleFollowContact(contact) }
                )
            }
        }
        .sheet(isPresented: $showShareSheet) {
            ActivityShareSheet(items: ["https://telefam.company/@\(profile.username.isEmpty ? "user" : profile.username)"])
        }
    }

    private var initialsText: some View {
        let trimmed = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        let initials = trimmed.isEmpty ? "TF" : String(trimmed.prefix(2)).uppercased()
        return Text(initials)
            .font(.system(size: 26, weight: .semibold))
            .foregroundColor(TelefamColors.crimson)
    }

    @ViewBuilder
    private func profileStatItem(count: Int, label: String, onTap: (() -> Void)?) -> some View {
        let content = VStack(spacing: 2) {
            Text("\(count)")
                .font(.system(size: 19, weight: .bold))
                .foregroundColor(palette.textPrimary)
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(palette.textSecondary)
                .lineLimit(1)
        }
        .frame(minHeight: 48)
        .padding(.vertical, 4)

        if let onTap {
            Button(action: onTap) { content }
        } else {
            content
        }
    }

    private var profileContentTabsRow: some View {
        let items: [(ProfileContentTab, String, LucideIconKind, String)] = [
            (.posts, "posts", .layoutGrid, "\(TelefamStrings.profileTabPosts) \(profile.postsCount)"),
            (.saved, "saved", .bookmark, TelefamStrings.profileTabSaved),
            (.reposts, "reposts", .repeat2, TelefamStrings.profileTabReposts),
            (.locked, "locked", .lock, TelefamStrings.profileTabLocked)
        ]

        return VStack(spacing: 0) {
            HStack(spacing: 0) {
                ForEach(items, id: \.0) { tabEnum, key, iconKind, label in
                    let selected = (state.profileContentTab == tabEnum)
                    let tint = selected ? palette.textPrimary : palette.textSecondary

                    Button {
                        withAnimation(.spring(response: 0.25, dampingFraction: 0.85)) {
                            viewModel.onSelectProfileContentTab(key)
                        }
                    } label: {
                        VStack(spacing: 0) {
                            Spacer(minLength: 2)
                            HStack(spacing: 5) {
                                LucideIcon(kind: iconKind, size: 16, color: tint)
                                Text(label)
                                    .font(.system(size: 13, weight: selected ? .semibold : .medium))
                                    .foregroundColor(tint)
                                    .lineLimit(1)
                            }
                            Spacer(minLength: 2)
                            Rectangle()
                                .fill(selected ? TelefamColors.crimson : Color.clear)
                                .frame(height: 2)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                    }
                    .accessibilityIdentifier("profile_tab_\(key)")
                }
            }
            Rectangle().fill(palette.outline.opacity(0.35)).frame(height: 1)
        }
    }
}

private struct OwnerProfileBackgroundBanner: View {
    let displayName: String
    let backgrounds: [BackgroundPhotoItem]
    let bridge: TelefamCoreBridge
    let isDark: Bool
    let isUploading: Bool
    let topInset: CGFloat
    let bannerHeight: CGFloat
    let onBack: () -> Void
    let onSubscribers: () -> Void
    let onSettings: () -> Void
    let onMenu: () -> Void
    let onEditBackground: () -> Void

    @State private var activeIndex: Int = 0
    @State private var kenBurnsExpanded: Bool = false

    private let carouselTimer = Timer.publish(every: 5.0, on: .main, in: .common).autoconnect()

    var body: some View {
        let fallbackGradient = isDark
            ? [Color(hex: 0x0E0F12), Color(hex: 0x1D1F26), Color(hex: 0x2B2D36)]
            : [Color(hex: 0x1A1B1E), Color(hex: 0x2D2F34), Color(hex: 0x565961)]

        ZStack {
            LinearGradient(colors: fallbackGradient, startPoint: .top, endPoint: .bottom)

            if !backgrounds.isEmpty {
                let safeIdx = min(max(activeIndex, 0), backgrounds.count - 1)
                let currentBg = backgrounds[safeIdx]
                CachedRemoteImage(urlString: currentBg.imageUrl, bridge: bridge) {
                    Color.clear
                }
                .id(currentBg.id)
                .scaleEffect(kenBurnsExpanded ? 1.07 : 1.0)
                .transition(.opacity.combined(with: .move(edge: .trailing)))
                .animation(.easeInOut(duration: 0.7), value: activeIndex)
            }

            LinearGradient(
                colors: [Color.black.opacity(0.48), Color.black.opacity(0.14), Color.black.opacity(0.42)],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack {
                HStack(spacing: 4) {
                    bannerIconButton(kind: .arrowLeft, label: TelefamStrings.cancel, action: onBack)
                        .accessibilityIdentifier("profile_back_button")

                    Text(displayName)
                        .font(.system(size: 19, weight: .bold))
                        .foregroundColor(.white)
                        .lineLimit(1)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 6)
                        .accessibilityIdentifier("profile_display_name")

                    bannerIconButton(kind: .crown, label: TelefamStrings.profileSubscribers, action: onSubscribers)
                        .accessibilityIdentifier("profile_subscribers_button")
                    bannerIconButton(kind: .settings, label: TelefamStrings.profileSettings, action: onSettings)
                        .accessibilityIdentifier("profile_settings_button")
                    bannerIconButton(kind: .menu, label: TelefamStrings.profileMenu, action: onMenu)
                        .accessibilityIdentifier("profile_menu_button")
                }
                .padding(.horizontal, 10)
                .padding(.top, topInset + 4)

                Spacer()

                HStack(alignment: .bottom) {
                    let count = max(backgrounds.count, 1)
                    HStack(spacing: 5) {
                        ForEach(0..<count, id: \.self) { idx in
                            let selected = (idx == (activeIndex % count))
                            Capsule()
                                .fill(selected ? Color.white : Color.white.opacity(0.45))
                                .frame(width: selected ? 18 : 6, height: 6)
                                .animation(.spring(response: 0.35, dampingFraction: 0.72), value: activeIndex)
                                .onTapGesture {
                                    if backgrounds.count > 1 {
                                        withAnimation(.easeInOut(duration: 0.5)) {
                                            activeIndex = idx
                                        }
                                    }
                                }
                        }
                    }
                    .padding(.leading, 16)
                    .padding(.bottom, 16)
                    .accessibilityIdentifier("profile_bg_indicators")

                    Spacer()

                    Button(action: onEditBackground) {
                        HStack(spacing: 6) {
                            if isUploading {
                                ProgressView().scaleEffect(0.7).tint(.white)
                            } else {
                                LucideIcon(kind: .camera, size: 15, color: .white)
                            }
                            Text(TelefamStrings.profileEditBackground)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(.white)
                                .lineLimit(1)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(Capsule().fill(Color.black.opacity(0.62)))
                        .overlay(Capsule().stroke(Color.white.opacity(0.18), lineWidth: 1))
                        .frame(minHeight: 48)
                    }
                    .padding(.trailing, 10)
                    .padding(.bottom, 6)
                    .accessibilityIdentifier("profile_edit_background_button")
                }
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: bannerHeight)
        .clipped()
        .accessibilityIdentifier("profile_background_banner")
        .onAppear {
            withAnimation(.easeInOut(duration: 5.0).repeatForever(autoreverses: true)) {
                kenBurnsExpanded = true
            }
        }
        .onReceive(carouselTimer) { _ in
            if backgrounds.count > 1 {
                withAnimation(.easeInOut(duration: 0.7)) {
                    activeIndex = (activeIndex + 1) % backgrounds.count
                }
            } else {
                activeIndex = 0
            }
        }
    }

    private func bannerIconButton(kind: LucideIconKind, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(Color.black.opacity(0.45))
                    .frame(width: 36, height: 36)
                LucideIcon(kind: kind, size: 18, color: .white)
            }
            .frame(width: 48, height: 48)
        }
        .accessibilityLabel(label)
    }
}

private struct ActivityShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
