import SwiftUI

struct ContactsView: View {
    @ObservedObject var viewModel: TelefamViewModel
    @Environment(\.telefamPalette) private var palette

    private var state: TelefamUiState { viewModel.uiState }

    private let tabs: [(ContactsTab, String, String)] = [
        (.all, "all", TelefamStrings.contactsTabAll),
        (.suggestions, "suggestions", TelefamStrings.contactsTabSuggestions),
        (.followers, "followers", TelefamStrings.contactsTabFollowers),
        (.following, "following", TelefamStrings.contactsTabFollowing),
        (.friends, "friends", TelefamStrings.contactsTabFriends)
    ]

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 10) {
                HStack(spacing: 8) {
                    HStack(spacing: 10) {
                        LucideIcon(kind: .search, size: 18, color: palette.textSecondary)
                        TextField(
                            "",
                            text: Binding(
                                get: { state.contactsSearchQuery },
                                set: { viewModel.onContactsSearchChanged($0) }
                            ),
                            prompt: Text(TelefamStrings.contactsSearchHint).foregroundColor(palette.textSecondary)
                        )
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .foregroundColor(palette.textPrimary)
                        .font(.system(size: 14))
                        .accessibilityIdentifier("contacts_search_input")
                    }
                    .padding(.horizontal, 14)
                    .frame(height: 48)
                    .background(palette.isDark ? Color(hex: 0x1F2026) : Color.white)
                    .cornerRadius(24)
                    .overlay(
                        RoundedRectangle(cornerRadius: 24)
                            .stroke(palette.isDark ? Color(hex: 0x2D2E35) : Color.white.opacity(0.7), lineWidth: 1)
                    )

                    ThemeToggleChip(themeMode: state.themeMode, isDark: palette.isDark) {
                        viewModel.onCycleTheme()
                    }
                    .accessibilityIdentifier("contacts_theme_toggle")
                }
                .padding(.horizontal, 12)
                .padding(.top, 10)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(tabs, id: \.0) { tabEnum, key, label in
                            let selected = (state.contactsTab == tabEnum)
                            Button {
                                withAnimation(.spring(response: 0.28, dampingFraction: 0.82)) {
                                    viewModel.onSelectContactsTab(key)
                                }
                            } label: {
                                Text(label)
                                    .font(.system(size: 13, weight: selected ? .semibold : .medium))
                                    .foregroundColor(selected ? TelefamColors.lightTextPrimary : .white)
                                    .padding(.horizontal, 14)
                                    .frame(height: 36)
                                    .background(Capsule().fill(selected ? Color.white : Color.clear))
                            }
                            .frame(minHeight: 44)
                            .accessibilityIdentifier("contacts_tab_\(key)")
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.bottom, 6)
                }
            }
            .background(TelefamColors.crimson.ignoresSafeArea(edges: .top))

            ScrollView(showsIndicators: false) {
                LazyVStack(alignment: .leading, spacing: 12) {
                    Text(sectionTitle)
                        .font(.system(size: 12, weight: .semibold))
                        .tracking(0.6)
                        .foregroundColor(palette.textSecondary)
                        .padding(.bottom, 4)
                        .accessibilityIdentifier("contacts_section_header")

                    if viewModel.filteredContacts.isEmpty {
                        VStack(spacing: 10) {
                            LucideIcon(kind: .users, size: 34, color: palette.textSecondary.opacity(0.65))
                            Text(TelefamStrings.contactsEmpty)
                                .font(.system(size: 14))
                                .foregroundColor(palette.textSecondary)
                                .accessibilityIdentifier("contacts_empty_text")
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 56)
                    } else {
                        ForEach(viewModel.filteredContacts) { contact in
                            ContactRowView(
                                contact: contact,
                                bridge: viewModel.bridge,
                                palette: palette
                            ) {
                                withAnimation(.spring(response: 0.3, dampingFraction: 0.72)) {
                                    viewModel.onToggleFollowContact(contact)
                                }
                            }
                            .transition(.opacity.combined(with: .move(edge: .bottom)))
                        }
                    }
                }
                .frame(maxWidth: 600)
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .frame(maxWidth: .infinity)
            }
            .background(palette.background)
        }
    }

    private var sectionTitle: String {
        switch state.contactsTab {
        case .all: return TelefamStrings.contactsTabAll.uppercased()
        case .suggestions: return TelefamStrings.contactsTabSuggestions.uppercased()
        case .followers: return TelefamStrings.contactsTabFollowers.uppercased()
        case .following: return TelefamStrings.contactsTabFollowing.uppercased()
        case .friends: return TelefamStrings.contactsTabFriends.uppercased()
        }
    }
}

struct ContactRowView: View {
    let contact: ContactItem
    let bridge: TelefamCoreBridge
    let palette: TelefamPalette
    let onToggleFollow: () -> Void

    var body: some View {
        let avatarBg = palette.isDark ? Color(hex: 0x1A2D3D) : Color(hex: 0xE7EFF7)
        let avatarFg = palette.isDark ? Color(hex: 0x8CC4FF) : Color(hex: 0x1D4E7A)

        HStack(spacing: 12) {
            if !contact.avatarUrl.isEmpty {
                CachedRemoteImage(urlString: contact.avatarUrl, bridge: bridge) {
                    initialsAvatar(bg: avatarBg, fg: avatarFg)
                }
                .frame(width: 48, height: 48)
                .clipShape(Circle())
            } else {
                initialsAvatar(bg: avatarBg, fg: avatarFg)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(contact.fullName.isEmpty ? contact.username : contact.fullName)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(palette.textPrimary)
                    .lineLimit(1)
                Text("@\(contact.username) \u{00B7} \(contact.countryName)")
                    .font(.system(size: 13))
                    .foregroundColor(palette.textSecondary)
                    .lineLimit(1)
            }

            Spacer(minLength: 8)

            ContactRelationshipButton(contact: contact, palette: palette, onTap: onToggleFollow)
        }
        .padding(.vertical, 4)
        .accessibilityIdentifier("contact_row_\(contact.userId)")
    }

    private func initialsAvatar(bg: Color, fg: Color) -> some View {
        let raw = contact.fullName.trimmingCharacters(in: .whitespacesAndNewlines)
        let initials = String((raw.isEmpty ? contact.username : raw).prefix(2)).uppercased()
        return ZStack {
            Circle().fill(bg)
            Text(initials)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(fg)
        }
        .frame(width: 48, height: 48)
    }
}

struct ContactRelationshipButton: View {
    let contact: ContactItem
    let palette: TelefamPalette
    let onTap: () -> Void

    var body: some View {
        let isFriend = contact.isFriend || (contact.iFollow && contact.followsMe)
        let isFollowingOnly = contact.iFollow && !contact.followsMe
        let isFollowBack = !contact.iFollow && contact.followsMe

        let label: String = {
            if isFriend { return TelefamStrings.contactsActionFriends }
            if isFollowingOnly { return TelefamStrings.contactsActionFollowing }
            if isFollowBack { return TelefamStrings.contactsActionFollowBack }
            return TelefamStrings.contactsActionFollow
        }()

        let iconKind: LucideIconKind = {
            if isFriend { return .users }
            if isFollowingOnly { return .userCheck }
            return .userPlus
        }()

        let containerColor: Color = {
            if isFriend { return TelefamColors.success.opacity(palette.isDark ? 0.15 : 0.09) }
            if isFollowingOnly { return palette.isDark ? Color(hex: 0x1F2026) : Color(hex: 0xF3F4F6) }
            return TelefamColors.crimson.opacity(palette.isDark ? 0.14 : 0.08)
        }()

        let borderColor: Color = {
            if isFriend { return TelefamColors.success }
            if isFollowingOnly { return palette.isDark ? Color(hex: 0x3E4049) : Color(hex: 0xD1D5DB) }
            return TelefamColors.crimson
        }()

        let contentColor: Color = {
            if isFriend { return palette.isDark ? Color(hex: 0x4ADE80) : TelefamColors.success }
            if isFollowingOnly { return palette.textPrimary }
            return palette.isDark ? Color(hex: 0xFF6B73) : TelefamColors.crimson
        }()

        Button(action: onTap) {
            HStack(spacing: 6) {
                LucideIcon(kind: iconKind, size: 15, color: contentColor)
                Text(label)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(contentColor)
                    .lineLimit(1)
            }
            .padding(.horizontal, 14)
            .frame(height: 36)
            .background(Capsule().fill(containerColor))
            .overlay(Capsule().stroke(borderColor, lineWidth: 1))
            .frame(minHeight: 48)
        }
        .scaleEffect(isFriend ? 1.02 : 1.0)
        .animation(.spring(response: 0.3, dampingFraction: 0.65), value: isFriend)
        .accessibilityIdentifier("contact_action_\(contact.userId)")
    }
}

struct ThemeToggleChip: View {
    let themeMode: ThemeMode
    let isDark: Bool
    let onTap: () -> Void

    var body: some View {
        let label: String = {
            switch themeMode {
            case .system: return "System"
            case .light: return "Light"
            case .dark: return "Dark"
            }
        }()
        let icon: LucideIconKind = {
            switch themeMode {
            case .system: return .monitor
            case .light: return .sun
            case .dark: return .moon
            }
        }()
        let bg = isDark ? Color(hex: 0x22181A) : Color(hex: 0xA70B14)
        let border = isDark ? Color(hex: 0x4B2428) : Color.white.opacity(0.25)

        Button(action: onTap) {
            HStack(spacing: 6) {
                LucideIcon(kind: icon, size: 16, color: isDark ? TelefamColors.crimson : .white)
                Text(label)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 12)
            .frame(height: 40)
            .background(Capsule().fill(bg))
            .overlay(Capsule().stroke(border, lineWidth: 1))
            .frame(minHeight: 48)
        }
    }
}
