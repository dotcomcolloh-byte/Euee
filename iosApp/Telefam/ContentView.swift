import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = TelefamViewModel()
    @Environment(\.colorScheme) private var systemColorScheme

    private var activeColorScheme: ColorScheme {
        switch viewModel.uiState.themeMode {
        case .system: return systemColorScheme
        case .light: return .light
        case .dark: return .dark
        }
    }

    private var palette: TelefamPalette {
        TelefamPalette.resolve(
            themeMode: viewModel.uiState.themeMode,
            systemColorScheme: systemColorScheme
        )
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Group {
                switch viewModel.uiState.screen {
                case .authEmail, .authOtp:
                    AuthView(viewModel: viewModel)
                        .transition(.opacity.combined(with: .scale(scale: 0.98)))
                case .profileSetup:
                    ProfileSetupView(viewModel: viewModel)
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                case .home:
                    HomeView(viewModel: viewModel)
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                }
            }
            .animation(.spring(response: 0.32, dampingFraction: 0.86), value: viewModel.uiState.screen)

            if !viewModel.uiState.noticeMessage.isEmpty {
                NoticeBannerToast(message: viewModel.uiState.noticeMessage) {
                    viewModel.onClearNotice()
                }
                .padding(.bottom, 86)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .zIndex(10)
            }

            if !viewModel.uiState.errorMessage.isEmpty && viewModel.uiState.screen == .home {
                NoticeBannerToast(message: viewModel.uiState.errorMessage, isError: true) {
                    viewModel.onClearError()
                }
                .padding(.bottom, 86)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .zIndex(11)
            }
        }
        .environment(\.telefamPalette, palette)
        .preferredColorScheme(viewModel.uiState.screen == .home ? (viewModel.uiState.themeMode == .system ? nil : activeColorScheme) : .light)
    }
}

private struct NoticeBannerToast: View {
    let message: String
    var isError: Bool = false
    let onDismiss: () -> Void

    var body: some View {
        Text(message)
            .font(.system(size: 13, weight: .semibold))
            .foregroundColor(.white)
            .padding(.horizontal, 18)
            .padding(.vertical, 11)
            .background(Capsule().fill(isError ? TelefamColors.crimson : Color.black.opacity(0.86)))
            .shadow(color: Color.black.opacity(0.22), radius: 8, x: 0, y: 4)
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.4) {
                    onDismiss()
                }
            }
    }
}
