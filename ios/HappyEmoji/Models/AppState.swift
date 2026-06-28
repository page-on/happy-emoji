import SwiftUI
import AVFoundation
import PhotosUI

enum MediaType: Equatable {
    case video, photo, burst
}

@MainActor
class AppState: ObservableObject {
    // MARK: - 步骤
    @Published var step: Int = 0
    @Published var mediaType: MediaType = .video

    // MARK: - 视频
    @Published var videoURL: URL?
    @Published var videoDuration: Double = 0
    @Published var trimStart: Double = 0
    @Published var trimEnd: Double = 0

    // MARK: - 照片
    @Published var photoURL: URL?
    @Published var photoImage: UIImage?

    // MARK: - 连拍
    @Published var burstImages: [UIImage] = []
    @Published var frameDelay: Double = 500  // ms

    // MARK: - 播放速度
    @Published var speed: Double = 1.0

    // MARK: - 文字
    @Published var textContent: String = ""
    @Published var textFont: String = "SimHei, Microsoft YaHei, sans-serif"
    @Published var textWeight: String = "700"
    @Published var textColor: Color = .white
    @Published var textColorHex: String = "FFFFFF"
    @Published var textSize: Double = 36
    @Published var textX: Double = 0.5  // 相对位置 0-1
    @Published var textY: Double = 0.5

    // MARK: - 导出
    @Published var qualityLevel: ExportQuality = .medium
    @Published var isExporting: Bool = false
    @Published var progress: Double = 0
    @Published var progressText: String = ""

    // MARK: - Toast
    @Published var toastMessage: String?
    @Published var toastIsError: Bool = false

    // MARK: - 步骤标签
    var steps: [String] {
        switch mediaType {
        case .photo: return ["选取素材", "添加文字", "预览导出"]
        case .burst: return ["选取素材", "排序编辑", "添加文字", "预览导出"]
        case .video: return ["选取素材", "裁剪时间", "添加文字", "预览导出"]
        }
    }

    // MARK: - 选中字体索引
    var selectedFontIndex: Int {
        AppFont.all.firstIndex(where: { $0.value == textFont }) ?? 0
    }

    // MARK: - 选中颜色索引
    var selectedColorIndex: Int {
        AppTheme.textColors.firstIndex(where: { $0.hex == textColorHex }) ?? 0
    }

    // MARK: - 工具方法
    func reset() {
        step = 0
        videoURL = nil
        videoDuration = 0
        trimStart = 0
        trimEnd = 0
        photoURL = nil
        photoImage = nil
        burstImages = []
        frameDelay = 500
        speed = 1.0
        textContent = ""
        textX = 0.5
        textY = 0.5
        textSize = 36
        progress = 0
        progressText = ""
        isExporting = false
    }

    func showToast(_ msg: String, error: Bool = false) {
        toastMessage = msg
        toastIsError = error
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.6) { [weak self] in
            self?.toastMessage = nil
        }
    }
}
