import SwiftUI
import PhotosUI
import AVKit

struct MediaPicker: View {
    @ObservedObject var state: AppState

    @State private var showVideoPicker = false
    @State private var showPhotoPicker = false
    @State private var showBurstPicker = false

    @State private var selectedVideoItem: PhotosPickerItem?
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var selectedBurstItems: [PhotosPickerItem] = []

    var body: some View {
        VStack(spacing: 6) {
            // 预览区域
            previewArea
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .aspectRatio(1, contentMode: .fit)
                .padding(.bottom, 8)

            // 选择按钮区
            VStack(spacing: 6) {
                HStack(spacing: 10) {
                    uploadCard(icon: "🎬", title: "选取视频", hint: "MP4 / MOV") {
                        state.mediaType = .video; state.reset()
                        showVideoPicker = true
                    }
                    uploadCard(icon: "🖼", title: "选取照片", hint: "JPG / PNG") {
                        state.mediaType = .photo; state.reset()
                        showPhotoPicker = true
                    }
                    uploadCard(icon: "📸", title: "连拍GIF", hint: "多选照片") {
                        state.mediaType = .burst; state.reset()
                        showBurstPicker = true
                    }
                }

                // 已选择信息
                if hasMedia {
                    mediaInfoCard
                }
            }
        }
        // 视频选择
        .photosPicker(isPresented: $showVideoPicker,
                       selection: $selectedVideoItem,
                       matching: .videos)
        .onChange(of: selectedVideoItem) { _, item in
            guard let item else { return }
            loadVideo(from: item)
        }
        // 照片选择
        .photosPicker(isPresented: $showPhotoPicker,
                       selection: $selectedPhotoItem,
                       matching: .images)
        .onChange(of: selectedPhotoItem) { _, item in
            guard let item else { return }
            loadPhoto(from: item)
        }
        // 连拍选择
        .photosPicker(isPresented: $showBurstPicker,
                       selection: $selectedBurstItems,
                       max: 20,
                       matching: .images)
        .onChange(of: selectedBurstItems) { _, items in
            guard !items.isEmpty else { return }
            loadBurst(from: items)
        }
    }

    // MARK: - 预览区
    @ViewBuilder
    private var previewArea: some View {
        ZStack {
            AppTheme.background

            if let photoImage = state.photoImage, state.mediaType == .photo {
                Image(uiImage: photoImage)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
            } else if state.mediaType == .burst, let first = state.burstImages.first {
                Image(uiImage: first)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .overlay(Text("+\(state.burstImages.count - 1)").font(.title).foregroundColor(.white).shadow(radius: 4))
            } else {
                emptyState
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Text(iconForType)
                .font(.system(size: 64))
                .padding(.bottom, 4)
            Text(hintForType)
                .font(.system(size: 14))
                .foregroundColor(AppTheme.textSecondary)
        }
    }

    // MARK: - 上传卡片
    private func uploadCard(icon: String, title: String, hint: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Text(icon).font(.system(size: 28))
                Text(title).font(.system(size: 14)).foregroundColor(AppTheme.textPrimary)
                Text(hint).font(.system(size: 12)).foregroundColor(AppTheme.textMuted)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .strokeBorder(style: StrokeStyle(lineWidth: 2, dash: [6, 4]))
                    .foregroundColor(AppTheme.border)
            )
        }
    }

    // MARK: - 已选择信息
    private var mediaInfoCard: some View {
        VStack(spacing: 4) {
            Text("已选择").font(.system(size: 11, weight: .medium))
                .foregroundColor(AppTheme.textMuted)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(mediaName)
                .font(.system(size: 13))
                .foregroundColor(AppTheme.textPrimary)
        }
        .padding(10)
        .background(AppTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - 加载方法
    private func loadVideo(from item: PhotosPickerItem) {
        Task {
            guard let data = try? await item.loadTransferable(type: Data.self) else { return }
            let tmpURL = FileManager.default.temporaryDirectory
                .appendingPathComponent("emoji_video_\(Date().timeIntervalSince1970).mp4")
            try? data.write(to: tmpURL)
            state.videoURL = tmpURL

            let asset = AVAsset(url: tmpURL)
            let duration = try? await asset.load(.duration)
            state.videoDuration = duration?.seconds ?? 0

            if state.videoDuration > 30 {
                state.showToast("视频过长（\(String(format: "%.1f", state.videoDuration))s），请选择 30 秒以内的视频", error: true)
                state.videoURL = nil; state.videoDuration = 0
            }
        }
    }

    private func loadPhoto(from item: PhotosPickerItem) {
        Task {
            guard let data = try? await item.loadTransferable(type: Data.self),
                  let img = UIImage(data: data) else { return }
            state.photoImage = img
        }
    }

    private func loadBurst(from items: [PhotosPickerItem]) {
        let addCount = min(items.count, 20 - state.burstImages.count)
        if addCount < items.count {
            state.showToast("最多选择 20 张照片，已自动截取前 20 张", error: true)
        }
        Task {
            for i in 0..<addCount {
                guard let data = try? await items[i].loadTransferable(type: Data.self),
                      let img = UIImage(data: data) else { continue }
                state.burstImages.append(img)
            }
        }
    }

    // MARK: - 计算属性
    private var hasMedia: Bool {
        switch state.mediaType {
        case .video: return state.videoURL != nil
        case .photo: return state.photoImage != nil
        case .burst: return !state.burstImages.isEmpty
        }
    }

    private var iconForType: String {
        switch state.mediaType {
        case .video: return "🎬"
        case .photo: return "🖼"
        case .burst: return "📸"
        }
    }

    private var hintForType: String {
        if !hasMedia { return "点击下方按钮选择素材" }
        return mediaName
    }

    private var mediaName: String {
        switch state.mediaType {
        case .video:
            let dur = state.videoDuration
            let m = Int(dur) / 60
            let s = dur.truncatingRemainder(dividingBy: 60)
            return "视频 (\(m):\(String(format: "%04.1f", s)))"
        case .photo:
            return "单张照片"
        case .burst:
            return "连拍模式 · 已选\(state.burstImages.count)张"
        }
    }
}
