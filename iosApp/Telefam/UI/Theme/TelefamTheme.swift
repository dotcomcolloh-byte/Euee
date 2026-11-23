import SwiftUI

enum TelefamColors {
    static let crimson = Color(hex: 0xC80E18)
    static let crimsonDark = Color(hex: 0x9F0A12)
    static let crimsonLight = Color(hex: 0xE53935)
    static let blush = Color(hex: 0xFDF2F2)
    static let success = Color(hex: 0x16A34A)
    static let errorRed = Color(hex: 0xDC2626)

    static let lightBg = Color(hex: 0xFFFFFF)
    static let lightSurface = Color(hex: 0xFFFFFF)
    static let lightSurfaceVariant = Color(hex: 0xF5F6F8)
    static let lightOutline = Color(hex: 0xE0E2E7)
    static let lightTextPrimary = Color(hex: 0x1A1C1E)
    static let lightTextSecondary = Color(hex: 0x6B7280)

    static let darkBg = Color(hex: 0x121214)
    static let darkSurface = Color(hex: 0x1C1D22)
    static let darkSurfaceVariant = Color(hex: 0x25262C)
    static let darkOutline = Color(hex: 0x33353D)
    static let darkTextPrimary = Color(hex: 0xF3F4F6)
    static let darkTextSecondary = Color(hex: 0x9CA3AF)
}

struct TelefamPalette {
    let isDark: Bool
    let background: Color
    let surface: Color
    let surfaceVariant: Color
    let outline: Color
    let textPrimary: Color
    let textSecondary: Color
    let primary: Color
    let error: Color

    static func resolve(themeMode: ThemeMode, systemColorScheme: ColorScheme) -> TelefamPalette {
        let isDark: Bool
        switch themeMode {
        case .system: isDark = (systemColorScheme == .dark)
        case .dark: isDark = true
        case .light: isDark = false
        }
        return TelefamPalette(
            isDark: isDark,
            background: isDark ? TelefamColors.darkBg : TelefamColors.lightBg,
            surface: isDark ? TelefamColors.darkSurface : TelefamColors.lightSurface,
            surfaceVariant: isDark ? TelefamColors.darkSurfaceVariant : TelefamColors.lightSurfaceVariant,
            outline: isDark ? TelefamColors.darkOutline : TelefamColors.lightOutline,
            textPrimary: isDark ? TelefamColors.darkTextPrimary : TelefamColors.lightTextPrimary,
            textSecondary: isDark ? TelefamColors.darkTextSecondary : TelefamColors.lightTextSecondary,
            primary: TelefamColors.crimson,
            error: TelefamColors.errorRed
        )
    }
}

extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: alpha)
    }
}

private struct TelefamPaletteKey: EnvironmentKey {
    static let defaultValue = TelefamPalette.resolve(themeMode: .system, systemColorScheme: .light)
}

extension EnvironmentValues {
    var telefamPalette: TelefamPalette {
        get { self[TelefamPaletteKey.self] }
        set { self[TelefamPaletteKey.self] = newValue }
    }
}
