import SwiftUI

// MARK: - 配色 — 完全对应 demo.html
struct AppTheme {
    static let background = Color(hex: "1a1a2e")
    static let surface = Color(hex: "2a2a3e")
    static let accent = Color(hex: "7c4dff")
    static let accentHover = Color(hex: "6a3de8")
    static let success = Color(hex: "4caf50")
    static let error = Color(hex: "f44336")
    static let textPrimary = Color(hex: "eeeeee")
    static let textSecondary = Color(hex: "aaaaaa")
    static let textMuted = Color(hex: "888888")
    static let border = Color(hex: "444444")
    static let overlayMask = Color.black.opacity(0.55)
}

// MARK: - Hex Color 扩展
extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex)
        _ = scanner.scanString("#")
        var rgb: UInt64 = 0
        scanner.scanHexInt64(&rgb)
        self.init(
            red: Double((rgb >> 16) & 0xFF) / 255.0,
            green: Double((rgb >> 8) & 0xFF) / 255.0,
            blue: Double(rgb & 0xFF) / 255.0
        )
    }
}

// MARK: - 字体配置 — 完全对应 demo.html fonts 数组
struct AppFont: Identifiable {
    let id = UUID()
    let label: String
    let value: String
    let weight: Font.Weight
}

extension AppFont {
    static let all: [AppFont] = [
        AppFont(label: "黑体", value: "SimHei, Microsoft YaHei, sans-serif", weight: .bold),
        AppFont(label: "楷体", value: "KaiTi, STKaiti, serif", weight: .bold),
        AppFont(label: "仿宋", value: "FangSong, STFangsong, serif", weight: .bold),
        AppFont(label: "默认", value: "Arial, sans-serif", weight: .bold),
        AppFont(label: "粗黑搞怪", value: "Impact, Arial Black, Noto Sans SC, sans-serif", weight: .black),
        AppFont(label: "手写涂鸦", value: "Caveat, Comic Sans MS, STXingkai, cursive", weight: .bold),
        AppFont(label: "萌萌圆体", value: "ZCOOL KuaiLe, Comic Neue, PingFang SC, sans-serif", weight: .regular),
        AppFont(label: "圆体", value: "Yuanti SC, PingFang SC, sans-serif", weight: .bold),
    ]

    var swiftUIFont: Font {
        switch label {
        case "粗黑搞怪": return .system(.body, design: .default).weight(.black)
        case "手写涂鸦": return .system(.body, design: .serif).weight(.bold)
        case "萌萌圆体": return .system(.body, design: .rounded).weight(.regular)
        case "圆体": return .system(.body, design: .rounded).weight(.bold)
        case "仿宋": return .system(.body, design: .serif).weight(.bold)
        case "楷体": return .system(.body, design: .serif).weight(.bold)
        default: return .system(.body, design: .default).weight(.bold)
        }
    }
}

// MARK: - 文字颜色预设 — 对应 demo.html colors 数组
extension AppTheme {
    static let textColors: [(label: String, hex: String)] = [
        ("白", "FFFFFF"),
        ("黑", "000000"),
        ("红", "FF1744"),
        ("黄", "FFD600"),
        ("蓝", "2979FF"),
        ("绿", "00E676"),
    ]
}

// MARK: - GIF 导出质量 — 对应 demo.html QUALITY_MAP
enum ExportQuality: CaseIterable, Identifiable {
    case low, medium, high

    var id: Self { self }

    var label: String {
        switch self {
        case .low: return "流畅"
        case .medium: return "标准"
        case .high: return "高清"
        }
    }

    var width: Int {
        switch self {
        case .low: return 360
        case .medium: return 480
        case .high: return 640
        }
    }

    var gifQuality: Int {
        switch self {
        case .low: return 20
        case .medium: return 10
        case .high: return 3
        }
    }
}
