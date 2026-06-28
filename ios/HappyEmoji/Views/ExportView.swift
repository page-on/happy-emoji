import SwiftUI
import ImageIO
import MobileCoreServices
import UniformTypeIdentifiers

struct ExportView: View {
    @ObservedObject var state: AppState
    @State private var previewImage: UIImage?
    @State private var showingShareSheet = false
    @State private var shareURL: URL?

    var body: some View {
        VStack(spacing: 6) {
            // 预览
            ZStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(Color.black)

                if let img = previewImage {
                    Image(uiImage: img)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                } else if state.mediaType == .photo, let photo = state.photoImage {
                    renderPreview(photo: photo)
                        .aspectRatio(contentMode: .fit)
                } else {
                    VStack(spacing: 12) {
                        if state.isExporting {
                            ProgressView()
                                .scaleEffect(1.5)
                                .tint(AppTheme.accent)
                            Text(state.progressText)
                                .font(.system(size: 14))
                                .foregroundColor(AppTheme.textSecondary)
                            ProgressBar(progress: state.progress)
                                .frame(width: 200, height: 6)
                        } else {
                            Text("📦")
                                .font(.system(size: 48))
                            Text("点击下方按钮生成")
                                .font(.system(size: 14))
                                .foregroundColor(AppTheme.textMuted)
                        }
                    }
                }
            }
            .aspectRatio(1, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 10))

            // 质量控制
            VStack(spacing: 4) {
                Text("导出质量").font(.system(size: 11))
                    .foregroundColor(AppTheme.textMuted)
                    .textCase(.uppercase)
                    .frame(maxWidth: .infinity, alignment: .leading)

                HStack(spacing: 8) {
                    ForEach(ExportQuality.allCases) { q in
                        Button {
                            state.qualityLevel = q
                        } label: {
                            Text(q.label)
                                .font(.system(size: 14, weight: .medium))
                                .padding(.vertical, 10)
                                .frame(maxWidth: .infinity)
                                .background(
                                    RoundedRectangle(cornerRadius: 8)
                                        .fill(state.qualityLevel == q ? AppTheme.accent : Color.clear)
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(state.qualityLevel == q ? AppTheme.accent : AppTheme.border,
                                                lineWidth: 1)
                                )
                                .foregroundColor(state.qualityLevel == q ? .white : AppTheme.textPrimary)
                        }
                    }
                }
            }
            .padding(10)
            .background(AppTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            // 导出状态
            if !state.progressText.isEmpty && !state.isExporting {
                VStack(spacing: 4) {
                    Text("✅ \(state.progressText)")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(AppTheme.success)
                }
                .padding(10)
                .background(AppTheme.surface)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
        .sheet(isPresented: $showingShareSheet) {
            if let url = shareURL {
                ShareSheet(items: [url])
            }
        }
    }

    // MARK: - 预览渲染
    @ViewBuilder
    private func renderPreview(photo: UIImage) -> some View {
        Canvas { ctx, size in
            // draw photo
            let scale = min(size.width / photo.size.width, size.height / photo.size.height)
            let dw = photo.size.width * scale
            let dh = photo.size.height * scale
            ctx.draw(
                Image(uiImage: photo),
                in: CGRect(x: (size.width - dw) / 2, y: (size.height - dh) / 2, width: dw, height: dh)
            )

            // draw text
            if !state.textContent.isEmpty {
                let fs = state.textSize * (size.width / 420)
                let text = Text(state.textContent)
                    .font(.system(size: fs, weight: .bold))
                    .foregroundColor(state.textColor)
                let tx = state.textX * size.width
                let ty = state.textY * size.height
                ctx.draw(text, at: CGPoint(x: tx, y: ty))
            }
        }
    }

    // MARK: - 导出图片
    func exportImage() {
        state.isExporting = true
        state.progressText = "正在合成图片..."
        state.progress = 0

        guard let photo = state.photoImage else {
            state.showToast("素材未加载", error: true)
            state.isExporting = false
            return
        }

        let targetW = state.qualityLevel.width
        let aspect = photo.size.height / photo.size.width
        let w = CGFloat(targetW)
        let h = w * aspect

        let renderer = UIGraphicsImageRenderer(size: CGSize(width: w, height: h))
        let img = renderer.image { ctx in
            let c = ctx.cgContext
            c.setFillColor(UIColor.black.cgColor)
            c.fill(CGRect(x: 0, y: 0, width: w, height: h))

            let scale = min(w / photo.size.width, h / photo.size.height)
            let dw = photo.size.width * scale, dh = photo.size.height * scale
            photo.draw(in: CGRect(x: (w - dw) / 2, y: (h - dh) / 2, width: dw, height: dh))

            if !state.textContent.isEmpty {
                let fontSize = state.textSize * (w / 420)
                let attrs: [NSAttributedString.Key: Any] = [
                    .font: UIFont.systemFont(ofSize: fontSize, weight: .bold),
                    .foregroundColor: UIColor(state.textColor),
                ]
                let size = state.textContent.size(withAttributes: attrs)
                let tx = state.textX * w - size.width / 2
                let ty = state.textY * h - size.height / 2

                // shadow
                c.setShadow(offset: CGSize(width: 2, height: 2), blur: 4,
                            color: UIColor.black.withAlphaComponent(0.7))
                state.textContent.draw(at: CGPoint(x: tx, y: ty), withAttributes: attrs)
            }
        }

        state.progress = 100
        state.progressText = "图片生成完成！"
        previewImage = img
        state.isExporting = false

        // save to temp
        let tmpURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("emoji_\(Date().timeIntervalSince1970).png")
        if let data = img.pngData() {
            try? data.write(to: tmpURL)
            shareURL = tmpURL
            showingShareSheet = true
        }
    }

    // MARK: - 导出 GIF (简化版，用于演示)
    func exportGIF() {
        state.isExporting = true
        state.progressText = "正在生成GIF..."
        state.progress = 0

        // GIF 生成需要 iOS ImageIO
        // 这里先做简化版：导出单帧预览
        let targetW = state.qualityLevel.width

        guard let photo = state.photoImage else {
            state.showToast("素材未加载", error: true)
            state.isExporting = false
            return
        }

        let aspect = photo.size.height / photo.size.width
        let w = CGFloat(targetW)
        let h = w * aspect

        let gifURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("emoji_\(Date().timeIntervalSince1970).gif")

        guard let destination = CGImageDestinationCreateWithURL(
            gifURL as CFURL, UTType.gif.identifier as CFString, 1, nil
        ) else {
            state.showToast("GIF 创建失败", error: true)
            state.isExporting = false
            return
        }

        let gifProperties: [CFString: Any] = [
            kCGImagePropertyGIFDictionary: [
                kCGImagePropertyGIFLoopCount: 0,
                kCGImagePropertyGIFDelayTime: 0.5
            ]
        ]
        CGImageDestinationSetProperties(destination, gifProperties as CFDictionary)

        // 生成一帧
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: w, height: h))
        let frame = renderer.image { ctx in
            photo.draw(in: CGRect(x: 0, y: 0, width: w, height: h))
            if !state.textContent.isEmpty {
                let fs = state.textSize * (w / 420)
                let attrs: [NSAttributedString.Key: Any] = [
                    .font: UIFont.systemFont(ofSize: fs, weight: .bold),
                    .foregroundColor: UIColor(state.textColor)
                ]
                let size = state.textContent.size(withAttributes: attrs)
                state.textContent.draw(
                    at: CGPoint(x: state.textX * w - size.width / 2,
                                 y: state.textY * h - size.height / 2),
                    withAttributes: attrs
                )
            }
        }

        if let cgImage = frame.cgImage {
            CGImageDestinationAddImage(destination, cgImage, nil)
        }
        CGImageDestinationFinalize(destination)

        state.progress = 100
        state.progressText = "GIF 已生成！"
        state.isExporting = false

        shareURL = gifURL
        showingShareSheet = true
    }
}

// MARK: - 进度条组件
struct ProgressBar: View {
    let progress: Double  // 0-100

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3)
                    .fill(AppTheme.surface)
                RoundedRectangle(cornerRadius: 3)
                    .fill(AppTheme.accent)
                    .frame(width: geo.size.width * progress / 100)
                    .animation(.easeInOut(duration: 0.3), value: progress)
            }
        }
    }
}

// MARK: - 分享 Sheet
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
