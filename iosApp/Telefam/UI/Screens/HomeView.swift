import SwiftUI

struct HomeView: View {
    @ObservedObject var viewModel: TelefamViewModel
    @Environment(\.telefamPalette) private var palette
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    private var state: TelefamUiState { viewModel.uiState }

    var body: some View {
        VStack(spacing: 0) {
            Group {
                switch state.homeTab {
                case .contacts:
                    ContactsView(viewModel: viewModel)
                        .transition(.opacity.combined(with: .move(edge: .trailing)))
                case .profile:
                    OwnerProfileView(viewModel: viewModel) {
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.85)) {
                            viewModel.onSelectHomeTab(.chats)
                        }
                    }
                    .transition(.opacity.combined(with: .move(edge: .trailing)))
                case .chats:
                    chatsHomeContent
                        .transition(.opacity.combined(with: .move(edge: .leading)))
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .animation(.spring(response: 0.28, dampingFraction: 0.85), value: state.homeTab)

            bottomNavigationBar
        }
        .background(palette.background.ignoresSafeArea())
    }

    private var chatsHomeContent: some View {
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                Button {
                    viewModel.onComingSoon("Search")
                } label: {
                    HStack(spacing: 8) {
                        LucideIcon(kind: .search, size: 18, color: palette.textSecondary)
                        Text(TelefamStrings.searchHint)
                            .font(.system(size: 14))
                            .foregroundColor(palette.textSecondary)
                            .lineLimit(1)
                        Spacer()
                    }
                    .padding(.horizontal, 14)
                    .frame(height: 48)
                    .background(palette.isDark ? Color(hex: 0x1F2026) : Color.white)
                    .cornerRadius(24)
                    .overlay(
                        RoundedRectangle(cornerRadius: 24)
                            .stroke(palette.isDark ? Color(hex: 0x2D2E35) : Color.white.opacity(0.6), lineWidth: 1)
                    )
                }
                .accessibilityIdentifier("home_search_bar")

                ThemeToggleChip(themeMode: state.themeMode, isDark: palette.isDark) {
                    viewModel.onCycleTheme()
                }
                .accessibilityIdentifier("home_theme_toggle")

                Button {
                    viewModel.onComingSoon("Notifications")
                } label: {
                    LucideIcon(kind: .bell, size: 21, color: .white)
                        .frame(width: 48, height: 48)
                }
                .accessibilityIdentifier("home_notifications_button")

                Button {
                    viewModel.onComingSoon("Menu")
                } label: {
                    LucideIcon(kind: .moreVertical, size: 21, color: .white)
                        .frame(width: 48, height: 48)
                }
                .accessibilityIdentifier("home_more_button")
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(TelefamColors.crimson.ignoresSafeArea(edges: .top))

            ZStack {
                palette.background

                VStack(spacing: 0) {
                    ZStack {
                        Circle()
                            .fill(palette.isDark ? Color(hex: 0x2B1215) : Color(hex: 0xFDECEC))
                            .frame(width: 72, height: 72)
                        LucideIcon(kind: .messages, size: 30, color: TelefamColors.crimson)
                    }

                    Spacer().frame(height: 18)

                    Text(TelefamStrings.startConversation)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(palette.textPrimary)

                    Spacer().frame(height: 6)

                    Text(TelefamStrings.startConversationSubtitle)
                        .font(.system(size: 13))
                        .foregroundColor(palette.textSecondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: 360)

                    Spacer().frame(height: 22)

                    Button {
                        viewModel.onComingSoon("Start chat")
                    } label: {
                        HStack(spacing: 8) {
                            LucideIcon(kind: .messagePlus, size: 18, color: .white)
                            Text(TelefamStrings.startChat)
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(.white)
                        }
                        .padding(.horizontal, 22)
                        .frame(height: 48)
                        .background(Capsule().fill(TelefamColors.crimson))
                    }
                    .accessibilityIdentifier("start_chat_button")
                }
                .padding(.horizontal, 32)
            }
            .clipShape(RoundedCornersShape(topLeft: 24, topRight: 24))
            .background(TelefamColors.crimson)
        }
    }

    private var bottomNavigationBar: some View {
        let chatsColor = (state.homeTab == .chats) ? TelefamColors.crimson : palette.textSecondary
        let contactsColor = (state.homeTab == .contacts) ? TelefamColors.crimson : palette.textSecondary
        let profileColor = (state.homeTab == .profile) ? TelefamColors.crimson : palette.textSecondary

        return VStack(spacing: 0) {
            Rectangle().fill(palette.outline.opacity(0.4)).frame(height: 1)

            HStack(spacing: 0) {
                Button { viewModel.onSelectHomeTab(.chats) } label: {
                    VStack(spacing: 4) {
                        LucideIcon(kind: .messages, size: 22, color: chatsColor)
                        Text(TelefamStrings.navChats)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(chatsColor)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                }
                .accessibilityIdentifier("nav_chats")

                Button { viewModel.onSelectHomeTab(.contacts) } label: {
                    VStack(spacing: 4) {
                        LucideIcon(kind: .users, size: 22, color: contactsColor)
                        Text(TelefamStrings.navContacts)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(contactsColor)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                }
                .accessibilityIdentifier("nav_contacts")

                Button { viewModel.onComingSoon("Post") } label: {
                    VStack(spacing: 2) {
                        ZStack {
                            Circle()
                                .fill(TelefamColors.crimson)
                                .frame(width: 46, height: 46)
                                .shadow(color: Color.black.opacity(0.18), radius: 6, x: 0, y: 3)
                            LucideIcon(kind: .plus, size: 22, color: .white, lineWidth: 2.4)
                        }
                        Text(TelefamStrings.navPost)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(palette.textSecondary)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                    .offset(y: -8)
                }
                .accessibilityIdentifier("nav_post")

                Button { viewModel.onComingSoon("Feeds") } label: {
                    VStack(spacing: 4) {
                        LucideIcon(kind: .layers, size: 22, color: palette.textSecondary)
                        Text(TelefamStrings.navFeeds)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(palette.textSecondary)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                }
                .accessibilityIdentifier("nav_feeds")

                Button { viewModel.onSelectHomeTab(.profile) } label: {
                    VStack(spacing: 4) {
                        if !state.profile.avatarUrl.isEmpty {
                            CachedRemoteImage(urlString: state.profile.avatarUrl, bridge: viewModel.bridge) {
                                defaultProfileTabAvatar(profileColor: profileColor)
                            }
                            .frame(width: 24, height: 24)
                            .clipShape(Circle())
                            .overlay(
                                Circle()
                                    .stroke(TelefamColors.crimson, lineWidth: state.homeTab == .profile ? 2 : 1)
                            )
                        } else {
                            defaultProfileTabAvatar(profileColor: profileColor)
                        }
                        Text(TelefamStrings.navProfile)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(profileColor)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                }
                .accessibilityIdentifier("nav_profile")
            }
            .frame(maxWidth: horizontalSizeClass == .regular ? 680 : .infinity)
            .frame(height: 68)
            .frame(maxWidth: .infinity)
        }
        .background(palette.surface.ignoresSafeArea(edges: .bottom))
    }

    private func defaultProfileTabAvatar(profileColor: Color) -> some View {
        ZStack {
            Circle()
                .stroke(
                    state.homeTab == .profile ? TelefamColors.crimson : profileColor.opacity(0.7),
                    lineWidth: state.homeTab == .profile ? 1.5 : 1.2
                )
            LucideIcon(kind: .user, size: 15, color: profileColor)
        }
        .frame(width: 24, height: 24)
    }
}
