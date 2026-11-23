import SwiftUI

struct TelefamLogoView: View {
    var size: CGFloat = 86

    var body: some View {
        Canvas { context, canvasSize in
            let scaleX = canvasSize.width / 96.0
            let scaleY = canvasSize.height / 96.0

            let circleRect = CGRect(x: 4 * scaleX, y: 4 * scaleY, width: 88 * scaleX, height: 88 * scaleY)
            context.fill(Path(ellipseIn: circleRect), with: .color(TelefamColors.crimson))

            var plane = Path()
            func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint {
                CGPoint(x: x * scaleX, y: y * scaleY)
            }
            plane.move(to: p(68.5, 29.5))
            plane.addCurve(to: p(70.2, 31.2), control1: p(69.6, 29.1), control2: p(70.6, 30.1))
            plane.addLine(to: p(59.5, 64.8))
            plane.addCurve(to: p(56.8, 65.3), control1: p(59.1, 66.0), control2: p(57.6, 66.2))
            plane.addLine(to: p(47.5, 55.3))
            plane.addLine(to: p(40.5, 61.3))
            plane.addCurve(to: p(38.7, 60.4), control1: p(39.8, 61.9), control2: p(38.6, 61.4))
            plane.addLine(to: p(39.6, 50.9))
            plane.addLine(to: p(63.2, 34.6))
            plane.addLine(to: p(37.3, 47.8))
            plane.addLine(to: p(28.9, 43.8))
            plane.addCurve(to: p(29.1, 41.1), control1: p(27.7, 43.2), control2: p(27.8, 41.5))
            plane.addLine(to: p(68.5, 29.5))
            plane.closeSubpath()

            context.fill(plane, with: .color(.white), style: FillStyle(eoFill: true))
        }
        .frame(width: size, height: size)
        .accessibilityLabel(TelefamStrings.appName)
    }
}

struct AppleLogoView: View {
    var size: CGFloat = 20
    var color: Color = TelefamColors.lightTextPrimary

    var body: some View {
        Canvas { context, canvasSize in
            let w = canvasSize.width / 24.0
            let h = canvasSize.height / 24.0
            func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * w, y: y * h) }

            var leaf = Path()
            leaf.move(to: pt(15.2, 2.0))
            leaf.addCurve(to: pt(12.2, 5.4), control1: pt(13.6, 2.1), control2: pt(12.1, 3.5))
            leaf.addCurve(to: pt(15.3, 2.1), control1: pt(13.8, 5.5), control2: pt(15.3, 4.1))
            leaf.closeSubpath()

            var bodyPath = Path()
            bodyPath.move(to: pt(18.7, 12.6))
            bodyPath.addCurve(to: pt(20.9, 9.6), control1: pt(18.7, 10.6), control2: pt(20.4, 9.7))
            bodyPath.addCurve(to: pt(17.1, 7.6), control1: pt(19.7, 7.8), control2: pt(17.8, 7.5))
            bodyPath.addCurve(to: pt(13.7, 8.4), control1: pt(15.5, 7.4), control2: pt(14.1, 8.4))
            bodyPath.addCurve(to: pt(10.1, 7.6), control1: pt(13.2, 8.4), control2: pt(11.9, 7.5))
            bodyPath.addCurve(to: pt(5.3, 12.5), control1: pt(7.2, 7.6), control2: pt(5.3, 9.8))
            bodyPath.addCurve(to: pt(8.5, 21.8), control1: pt(5.3, 16.5), control2: pt(6.7, 21.8))
            bodyPath.addCurve(to: pt(11.3, 20.9), control1: pt(9.8, 21.8), control2: pt(10.4, 20.9))
            bodyPath.addCurve(to: pt(14.3, 21.8), control1: pt(12.3, 20.9), control2: pt(12.9, 21.8))
            bodyPath.addCurve(to: pt(19.9, 17.5), control1: pt(16.1, 21.8), control2: pt(17.3, 20.2))
            bodyPath.addCurve(to: pt(18.7, 12.6), control1: pt(18.6, 16.8), control2: pt(18.7, 14.3))
            bodyPath.closeSubpath()

            context.fill(leaf, with: .color(color))
            context.fill(bodyPath, with: .color(color))
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

struct CountryFlagView: View {
    let iso2: String

    var body: some View {
        Canvas { context, canvasSize in
            let w = canvasSize.width
            let h = canvasSize.height
            switch iso2.uppercased() {
            case "KE":
                context.fill(Path(CGRect(x: 0, y: 0, width: w, height: h * 0.28)), with: .color(Color(hex: 0x111111)))
                context.fill(Path(CGRect(x: 0, y: h * 0.28, width: w, height: h * 0.08)), with: .color(.white))
                context.fill(Path(CGRect(x: 0, y: h * 0.36, width: w, height: h * 0.28)), with: .color(Color(hex: 0xBB161D)))
                context.fill(Path(CGRect(x: 0, y: h * 0.64, width: w, height: h * 0.08)), with: .color(.white))
                context.fill(Path(CGRect(x: 0, y: h * 0.72, width: w, height: h * 0.28)), with: .color(Color(hex: 0x106B2E)))
                context.fill(Path(ellipseIn: CGRect(x: w * 0.41, y: h * 0.25, width: w * 0.18, height: h * 0.50)), with: .color(Color(hex: 0xBB161D)))
            case "US", "CA":
                context.fill(Path(CGRect(x: 0, y: 0, width: w, height: h)), with: .color(Color(hex: 0xB22234)))
                for i in stride(from: 1, through: 5, by: 2) {
                    context.fill(Path(CGRect(x: 0, y: h * CGFloat(i) / 7.0, width: w, height: h / 7.0)), with: .color(.white))
                }
                context.fill(Path(CGRect(x: 0, y: 0, width: w * 0.45, height: h * 0.55)), with: .color(Color(hex: 0x3C3B6E)))
            case "GB":
                context.fill(Path(CGRect(x: 0, y: 0, width: w, height: h)), with: .color(Color(hex: 0x012169)))
                context.fill(Path(CGRect(x: w * 0.40, y: 0, width: w * 0.20, height: h)), with: .color(.white))
                context.fill(Path(CGRect(x: 0, y: h * 0.36, width: w, height: h * 0.28)), with: .color(.white))
                context.fill(Path(CGRect(x: w * 0.44, y: 0, width: w * 0.12, height: h)), with: .color(Color(hex: 0xC8102E)))
                context.fill(Path(CGRect(x: 0, y: h * 0.42, width: w, height: h * 0.16)), with: .color(Color(hex: 0xC8102E)))
            case "NG":
                context.fill(Path(CGRect(x: 0, y: 0, width: w / 3.0, height: h)), with: .color(Color(hex: 0x008751)))
                context.fill(Path(CGRect(x: w / 3.0, y: 0, width: w / 3.0, height: h)), with: .color(.white))
                context.fill(Path(CGRect(x: 2.0 * w / 3.0, y: 0, width: w / 3.0, height: h)), with: .color(Color(hex: 0x008751)))
            default:
                context.fill(Path(CGRect(x: 0, y: 0, width: w, height: h)), with: .color(Color(hex: 0xE5E7EB)))
                context.fill(Path(CGRect(x: 0, y: 0, width: w, height: h * 0.33)), with: .color(TelefamColors.crimson))
                context.fill(Path(CGRect(x: 0, y: h * 0.67, width: w, height: h * 0.33)), with: .color(Color(hex: 0x1F2937)))
            }
        }
        .frame(width: 24, height: 16)
        .clipShape(RoundedRectangle(cornerRadius: 2))
    }
}

enum LucideIconKind {
    case mail, arrowLeft, camera, plus, user, users, atSign, phone, calendar, mapPin
    case messageSquare, chevronDown, search, bell, moreVertical, sun, moon, monitor
    case messages, layers, messagePlus, checkCircle, userPlus, userCheck, crown
    case settings, menu, pencil, share2, layoutGrid, bookmark, repeat2, lock
    case slidersHorizontal, trash2, xMark
}

struct LucideIcon: View {
    let kind: LucideIconKind
    var size: CGFloat = 20
    var color: Color = .primary
    var lineWidth: CGFloat = 2.0

    var body: some View {
        Canvas { context, canvasSize in
            let s = min(canvasSize.width, canvasSize.height) / 24.0
            func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * s, y: y * s) }
            let strokeStyle = StrokeStyle(lineWidth: lineWidth * s, lineCap: .round, lineJoin: .round)

            func stroke(_ path: Path) {
                context.stroke(path, with: .color(color), style: strokeStyle)
            }
            func fill(_ path: Path) {
                context.fill(path, with: .color(color))
            }

            switch kind {
            case .mail:
                var p = Path()
                p.addRoundedRect(in: CGRect(x: 2 * s, y: 4 * s, width: 20 * s, height: 16 * s), cornerSize: CGSize(width: 2 * s, height: 2 * s))
                p.move(to: pt(22, 6)); p.addLine(to: pt(12, 13)); p.addLine(to: pt(2, 6))
                stroke(p)
            case .arrowLeft:
                var p = Path()
                p.move(to: pt(19, 12)); p.addLine(to: pt(5, 12))
                p.move(to: pt(12, 19)); p.addLine(to: pt(5, 12)); p.addLine(to: pt(12, 5))
                stroke(p)
            case .camera:
                var p = Path()
                p.move(to: pt(14.5, 4)); p.addLine(to: pt(9.5, 4)); p.addLine(to: pt(7, 7)); p.addLine(to: pt(4, 7))
                p.addQuadCurve(to: pt(2, 9), control: pt(2, 7))
                p.addLine(to: pt(2, 18))
                p.addQuadCurve(to: pt(4, 20), control: pt(2, 20))
                p.addLine(to: pt(20, 20))
                p.addQuadCurve(to: pt(22, 18), control: pt(22, 20))
                p.addLine(to: pt(22, 9))
                p.addQuadCurve(to: pt(20, 7), control: pt(22, 7))
                p.addLine(to: pt(17, 7)); p.closeSubpath()
                p.addEllipse(in: CGRect(x: 8.5 * s, y: 10 * s, width: 7 * s, height: 7 * s))
                stroke(p)
            case .plus:
                var p = Path()
                p.move(to: pt(12, 5)); p.addLine(to: pt(12, 19))
                p.move(to: pt(5, 12)); p.addLine(to: pt(19, 12))
                stroke(p)
            case .user:
                var p = Path()
                p.move(to: pt(20, 21)); p.addLine(to: pt(20, 19))
                p.addCurve(to: pt(16, 15), control1: pt(20, 16.79), control2: pt(18.21, 15))
                p.addLine(to: pt(8, 15))
                p.addCurve(to: pt(4, 19), control1: pt(5.79, 15), control2: pt(4, 16.79))
                p.addLine(to: pt(4, 21))
                p.addEllipse(in: CGRect(x: 8 * s, y: 3 * s, width: 8 * s, height: 8 * s))
                stroke(p)
            case .users:
                var p = Path()
                p.move(to: pt(16, 21)); p.addLine(to: pt(16, 19))
                p.addCurve(to: pt(13, 16), control1: pt(16, 17.34), control2: pt(14.66, 16))
                p.addLine(to: pt(5, 16))
                p.addCurve(to: pt(2, 19), control1: pt(3.34, 16), control2: pt(2, 17.34))
                p.addLine(to: pt(2, 21))
                p.addEllipse(in: CGRect(x: 5.5 * s, y: 4 * s, width: 7 * s, height: 7 * s))
                p.move(to: pt(22, 21)); p.addLine(to: pt(22, 19))
                p.addCurve(to: pt(19.7, 16.08), control1: pt(22, 17.6), control2: pt(21.04, 16.4))
                p.move(to: pt(16, 4.13))
                p.addCurve(to: pt(16, 11.87), control1: pt(19.6, 5.1), control2: pt(19.6, 10.9))
                stroke(p)
            case .atSign:
                var p = Path()
                p.addEllipse(in: CGRect(x: 8 * s, y: 8 * s, width: 8 * s, height: 8 * s))
                p.move(to: pt(16, 8)); p.addLine(to: pt(16, 13))
                p.addCurve(to: pt(19, 16), control1: pt(16, 14.66), control2: pt(17.34, 16))
                p.addCurve(to: pt(22, 12), control1: pt(20.66, 16), control2: pt(22, 14.5))
                p.addArc(center: pt(12, 12), radius: 10 * s, startAngle: .degrees(0), endAngle: .degrees(305), clockwise: true)
                stroke(p)
            case .phone:
                var p = Path()
                p.move(to: pt(22, 16.92)); p.addLine(to: pt(22, 19.92))
                p.addCurve(to: pt(19.98, 21.84), control1: pt(22, 21.02), control2: pt(21.08, 21.92))
                p.addCurve(to: pt(2.86, 4.72), control1: pt(10.86, 21.1), control2: pt(3.6, 13.84))
                p.addCurve(to: pt(4.78, 2.7), control1: pt(2.78, 3.62), control2: pt(3.68, 2.7))
                p.addLine(to: pt(7.78, 2.7))
                p.addCurve(to: pt(9.64, 4.26), control1: pt(8.7, 2.7), control2: pt(9.48, 3.36))
                p.addLine(to: pt(10.22, 7.5))
                p.addCurve(to: pt(9.39, 9.33), control1: pt(10.35, 8.22), control2: pt(10.02, 8.95))
                p.addLine(to: pt(7.9, 10.22))
                p.addCurve(to: pt(14.48, 16.8), control1: pt(9.35, 12.92), control2: pt(11.78, 15.35))
                p.addLine(to: pt(15.37, 15.31))
                p.addCurve(to: pt(17.2, 14.48), control1: pt(15.75, 14.68), control2: pt(16.48, 14.35))
                p.addLine(to: pt(20.44, 15.06))
                p.addCurve(to: pt(22, 16.92), control1: pt(21.34, 15.22), control2: pt(22, 16.0))
                p.closeSubpath()
                stroke(p)
            case .calendar:
                var p = Path()
                p.addRoundedRect(in: CGRect(x: 3 * s, y: 4 * s, width: 18 * s, height: 18 * s), cornerSize: CGSize(width: 2 * s, height: 2 * s))
                p.move(to: pt(16, 2)); p.addLine(to: pt(16, 6))
                p.move(to: pt(8, 2)); p.addLine(to: pt(8, 6))
                p.move(to: pt(3, 10)); p.addLine(to: pt(21, 10))
                stroke(p)
            case .mapPin:
                var p = Path()
                p.move(to: pt(20, 10))
                p.addCurve(to: pt(12, 22), control1: pt(20, 16), control2: pt(12, 22))
                p.addCurve(to: pt(4, 10), control1: pt(12, 22), control2: pt(4, 16))
                p.addCurve(to: pt(12, 2), control1: pt(4, 5.58), control2: pt(7.58, 2))
                p.addCurve(to: pt(20, 10), control1: pt(16.42, 2), control2: pt(20, 5.58))
                p.closeSubpath()
                p.addEllipse(in: CGRect(x: 9 * s, y: 7 * s, width: 6 * s, height: 6 * s))
                stroke(p)
            case .messageSquare:
                var p = Path()
                p.move(to: pt(21, 15))
                p.addQuadCurve(to: pt(19, 17), control: pt(21, 17))
                p.addLine(to: pt(7, 17)); p.addLine(to: pt(3, 21)); p.addLine(to: pt(3, 5))
                p.addQuadCurve(to: pt(5, 3), control: pt(3, 3))
                p.addLine(to: pt(19, 3))
                p.addQuadCurve(to: pt(21, 5), control: pt(21, 3))
                p.closeSubpath()
                stroke(p)
            case .chevronDown:
                var p = Path()
                p.move(to: pt(6, 9)); p.addLine(to: pt(12, 15)); p.addLine(to: pt(18, 9))
                stroke(p)
            case .search:
                var p = Path()
                p.addEllipse(in: CGRect(x: 3 * s, y: 3 * s, width: 16 * s, height: 16 * s))
                p.move(to: pt(21, 21)); p.addLine(to: pt(16.65, 16.65))
                stroke(p)
            case .bell:
                var p = Path()
                p.move(to: pt(18, 8))
                p.addCurve(to: pt(12, 2), control1: pt(18, 4.69), control2: pt(15.31, 2))
                p.addCurve(to: pt(6, 8), control1: pt(8.69, 2), control2: pt(6, 4.69))
                p.addCurve(to: pt(3, 17), control1: pt(6, 15), control2: pt(3, 17))
                p.addLine(to: pt(21, 17))
                p.addCurve(to: pt(18, 8), control1: pt(21, 17), control2: pt(18, 15))
                p.move(to: pt(13.73, 21))
                p.addCurve(to: pt(10.27, 21), control1: pt(12.9, 22.3), control2: pt(11.1, 22.3))
                stroke(p)
            case .moreVertical:
                var p = Path()
                p.addEllipse(in: CGRect(x: 10.5 * s, y: 4 * s, width: 3 * s, height: 3 * s))
                p.addEllipse(in: CGRect(x: 10.5 * s, y: 10.5 * s, width: 3 * s, height: 3 * s))
                p.addEllipse(in: CGRect(x: 10.5 * s, y: 17 * s, width: 3 * s, height: 3 * s))
                fill(p)
            case .sun:
                var p = Path()
                p.addEllipse(in: CGRect(x: 8 * s, y: 8 * s, width: 8 * s, height: 8 * s))
                p.move(to: pt(12, 2)); p.addLine(to: pt(12, 4))
                p.move(to: pt(12, 20)); p.addLine(to: pt(12, 22))
                p.move(to: pt(4.93, 4.93)); p.addLine(to: pt(6.34, 6.34))
                p.move(to: pt(17.66, 17.66)); p.addLine(to: pt(19.07, 19.07))
                p.move(to: pt(2, 12)); p.addLine(to: pt(4, 12))
                p.move(to: pt(20, 12)); p.addLine(to: pt(22, 12))
                p.move(to: pt(6.34, 17.66)); p.addLine(to: pt(4.93, 19.07))
                p.move(to: pt(19.07, 4.93)); p.addLine(to: pt(17.66, 6.34))
                stroke(p)
            case .moon:
                var p = Path()
                p.addArc(center: pt(12, 12), radius: 9 * s, startAngle: .degrees(10), endAngle: .degrees(260), clockwise: false)
                p.addCurve(to: pt(21, 12.79), control1: pt(12.2, 10.6), control2: pt(15.2, 14.3))
                p.closeSubpath()
                stroke(p)
            case .monitor:
                var p = Path()
                p.addRoundedRect(in: CGRect(x: 2 * s, y: 3 * s, width: 20 * s, height: 14 * s), cornerSize: CGSize(width: 2 * s, height: 2 * s))
                p.move(to: pt(8, 21)); p.addLine(to: pt(16, 21))
                p.move(to: pt(12, 17)); p.addLine(to: pt(12, 21))
                stroke(p)
            case .messages:
                var p = Path()
                p.move(to: pt(14, 9)); p.addQuadCurve(to: pt(12, 11), control: pt(14, 11))
                p.addLine(to: pt(6, 11)); p.addLine(to: pt(2, 15)); p.addLine(to: pt(2, 4))
                p.addQuadCurve(to: pt(4, 2), control: pt(2, 2))
                p.addLine(to: pt(12, 2)); p.addQuadCurve(to: pt(14, 4), control: pt(14, 2)); p.closeSubpath()
                p.move(to: pt(18, 9)); p.addLine(to: pt(20, 9)); p.addQuadCurve(to: pt(22, 11), control: pt(22, 9))
                p.addLine(to: pt(22, 22)); p.addLine(to: pt(18, 18)); p.addLine(to: pt(11, 18))
                p.addQuadCurve(to: pt(9, 16), control: pt(9, 18)); p.addLine(to: pt(9, 15))
                stroke(p)
            case .layers:
                var p = Path()
                p.move(to: pt(12, 2)); p.addLine(to: pt(2, 7)); p.addLine(to: pt(12, 12)); p.addLine(to: pt(22, 7)); p.closeSubpath()
                p.move(to: pt(2, 12)); p.addLine(to: pt(12, 17)); p.addLine(to: pt(22, 12))
                p.move(to: pt(2, 17)); p.addLine(to: pt(12, 22)); p.addLine(to: pt(22, 17))
                stroke(p)
            case .messagePlus:
                var p = Path()
                p.move(to: pt(21, 15)); p.addQuadCurve(to: pt(19, 17), control: pt(21, 17))
                p.addLine(to: pt(7, 17)); p.addLine(to: pt(3, 21)); p.addLine(to: pt(3, 5))
                p.addQuadCurve(to: pt(5, 3), control: pt(3, 3)); p.addLine(to: pt(19, 3))
                p.addQuadCurve(to: pt(21, 5), control: pt(21, 3)); p.closeSubpath()
                p.move(to: pt(12, 7)); p.addLine(to: pt(12, 13))
                p.move(to: pt(9, 10)); p.addLine(to: pt(15, 10))
                stroke(p)
            case .checkCircle:
                var p = Path()
                p.addEllipse(in: CGRect(x: 2 * s, y: 2 * s, width: 20 * s, height: 20 * s))
                p.move(to: pt(9, 12)); p.addLine(to: pt(11, 14)); p.addLine(to: pt(15, 10))
                stroke(p)
            case .userPlus:
                var p = Path()
                p.move(to: pt(16, 21)); p.addLine(to: pt(16, 19))
                p.addCurve(to: pt(12, 15), control1: pt(16, 16.79), control2: pt(14.21, 15))
                p.addLine(to: pt(5, 15))
                p.addCurve(to: pt(1, 19), control1: pt(2.79, 15), control2: pt(1, 16.79))
                p.addLine(to: pt(1, 21))
                p.addEllipse(in: CGRect(x: 4.5 * s, y: 3 * s, width: 8 * s, height: 8 * s))
                p.move(to: pt(20, 8)); p.addLine(to: pt(20, 14))
                p.move(to: pt(17, 11)); p.addLine(to: pt(23, 11))
                stroke(p)
            case .userCheck:
                var p = Path()
                p.move(to: pt(16, 21)); p.addLine(to: pt(16, 19))
                p.addCurve(to: pt(12, 15), control1: pt(16, 16.79), control2: pt(14.21, 15))
                p.addLine(to: pt(5, 15))
                p.addCurve(to: pt(1, 19), control1: pt(2.79, 15), control2: pt(1, 16.79))
                p.addLine(to: pt(1, 21))
                p.addEllipse(in: CGRect(x: 4.5 * s, y: 3 * s, width: 8 * s, height: 8 * s))
                p.move(to: pt(17, 11)); p.addLine(to: pt(19, 13)); p.addLine(to: pt(23, 9))
                stroke(p)
            case .crown:
                var p = Path()
                p.move(to: pt(2, 19)); p.addLine(to: pt(22, 19))
                p.move(to: pt(3, 15)); p.addLine(to: pt(2, 6)); p.addLine(to: pt(7.5, 10.5))
                p.addLine(to: pt(12, 4)); p.addLine(to: pt(16.5, 10.5)); p.addLine(to: pt(22, 6))
                p.addLine(to: pt(21, 15)); p.closeSubpath()
                stroke(p)
            case .settings:
                var p = Path()
                p.addEllipse(in: CGRect(x: 9 * s, y: 9 * s, width: 6 * s, height: 6 * s))
                p.addEllipse(in: CGRect(x: 3.5 * s, y: 3.5 * s, width: 17 * s, height: 17 * s))
                p.move(to: pt(12, 1.5)); p.addLine(to: pt(12, 4))
                p.move(to: pt(12, 20)); p.addLine(to: pt(12, 22.5))
                p.move(to: pt(1.5, 12)); p.addLine(to: pt(4, 12))
                p.move(to: pt(20, 12)); p.addLine(to: pt(22.5, 12))
                p.move(to: pt(4.6, 4.6)); p.addLine(to: pt(6.4, 6.4))
                p.move(to: pt(17.6, 17.6)); p.addLine(to: pt(19.4, 19.4))
                p.move(to: pt(19.4, 4.6)); p.addLine(to: pt(17.6, 6.4))
                p.move(to: pt(6.4, 17.6)); p.addLine(to: pt(4.6, 19.4))
                stroke(p)
            case .menu:
                var p = Path()
                p.move(to: pt(4, 6)); p.addLine(to: pt(20, 6))
                p.move(to: pt(4, 12)); p.addLine(to: pt(20, 12))
                p.move(to: pt(4, 18)); p.addLine(to: pt(20, 18))
                stroke(p)
            case .pencil:
                var p = Path()
                p.move(to: pt(17, 3)); p.addLine(to: pt(21, 7))
                p.addLine(to: pt(7.5, 20.5)); p.addLine(to: pt(2, 22)); p.addLine(to: pt(3.5, 16.5)); p.closeSubpath()
                stroke(p)
            case .share2:
                var p = Path()
                p.addEllipse(in: CGRect(x: 15 * s, y: 3 * s, width: 6 * s, height: 6 * s))
                p.addEllipse(in: CGRect(x: 3 * s, y: 9 * s, width: 6 * s, height: 6 * s))
                p.addEllipse(in: CGRect(x: 15 * s, y: 15 * s, width: 6 * s, height: 6 * s))
                p.move(to: pt(8.59, 13.51)); p.addLine(to: pt(15.42, 17.49))
                p.move(to: pt(15.41, 6.51)); p.addLine(to: pt(8.59, 10.49))
                stroke(p)
            case .layoutGrid:
                var p = Path()
                p.addRect(CGRect(x: 3 * s, y: 3 * s, width: 7 * s, height: 7 * s))
                p.addRect(CGRect(x: 14 * s, y: 3 * s, width: 7 * s, height: 7 * s))
                p.addRect(CGRect(x: 14 * s, y: 14 * s, width: 7 * s, height: 7 * s))
                p.addRect(CGRect(x: 3 * s, y: 14 * s, width: 7 * s, height: 7 * s))
                stroke(p)
            case .bookmark:
                var p = Path()
                p.move(to: pt(19, 21)); p.addLine(to: pt(12, 16)); p.addLine(to: pt(5, 21)); p.addLine(to: pt(5, 5))
                p.addQuadCurve(to: pt(7, 3), control: pt(5, 3))
                p.addLine(to: pt(17, 3))
                p.addQuadCurve(to: pt(19, 5), control: pt(19, 3))
                p.closeSubpath()
                stroke(p)
            case .repeat2:
                var p = Path()
                p.move(to: pt(2, 9)); p.addLine(to: pt(5, 6)); p.addLine(to: pt(8, 9))
                p.move(to: pt(5, 6)); p.addLine(to: pt(5, 15))
                p.addQuadCurve(to: pt(8, 18), control: pt(5, 18))
                p.addLine(to: pt(14, 18))
                p.move(to: pt(22, 15)); p.addLine(to: pt(19, 18)); p.addLine(to: pt(16, 15))
                p.move(to: pt(19, 18)); p.addLine(to: pt(19, 9))
                p.addQuadCurve(to: pt(16, 6), control: pt(19, 6))
                p.addLine(to: pt(10, 6))
                stroke(p)
            case .lock:
                var p = Path()
                p.addRoundedRect(in: CGRect(x: 3 * s, y: 11 * s, width: 18 * s, height: 11 * s), cornerSize: CGSize(width: 2 * s, height: 2 * s))
                p.move(to: pt(7, 11)); p.addLine(to: pt(7, 7))
                p.addArc(center: pt(12, 7), radius: 5 * s, startAngle: .degrees(180), endAngle: .degrees(0), clockwise: false)
                p.addLine(to: pt(17, 11))
                stroke(p)
            case .slidersHorizontal:
                var p = Path()
                p.move(to: pt(21, 4)); p.addLine(to: pt(14, 4))
                p.move(to: pt(10, 4)); p.addLine(to: pt(3, 4))
                p.move(to: pt(21, 12)); p.addLine(to: pt(12, 12))
                p.move(to: pt(8, 12)); p.addLine(to: pt(3, 12))
                p.move(to: pt(21, 20)); p.addLine(to: pt(16, 20))
                p.move(to: pt(12, 20)); p.addLine(to: pt(3, 20))
                p.move(to: pt(14, 2)); p.addLine(to: pt(14, 6))
                p.move(to: pt(8, 10)); p.addLine(to: pt(8, 14))
                p.move(to: pt(16, 18)); p.addLine(to: pt(16, 22))
                stroke(p)
            case .trash2:
                var p = Path()
                p.move(to: pt(3, 6)); p.addLine(to: pt(21, 6))
                p.move(to: pt(19, 6)); p.addLine(to: pt(19, 20))
                p.addQuadCurve(to: pt(17, 22), control: pt(19, 22))
                p.addLine(to: pt(7, 22))
                p.addQuadCurve(to: pt(5, 20), control: pt(5, 22))
                p.addLine(to: pt(5, 6))
                p.move(to: pt(8, 6)); p.addLine(to: pt(8, 4))
                p.addQuadCurve(to: pt(10, 2), control: pt(8, 2))
                p.addLine(to: pt(14, 2))
                p.addQuadCurve(to: pt(16, 4), control: pt(16, 2))
                p.addLine(to: pt(16, 6))
                p.move(to: pt(10, 11)); p.addLine(to: pt(10, 17))
                p.move(to: pt(14, 11)); p.addLine(to: pt(14, 17))
                stroke(p)
            case .xMark:
                var p = Path()
                p.move(to: pt(18, 6)); p.addLine(to: pt(6, 18))
                p.move(to: pt(6, 6)); p.addLine(to: pt(18, 18))
                stroke(p)
            }
        }
        .frame(width: size, height: size)
    }
}
