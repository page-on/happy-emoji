import SwiftUI
import AVKit

struct VideoTrimmer: View {
    @ObservedObject var state: AppState
    @State private var player: AVPlayer?
    @State private var isPreviewing = false

    private let maxTrimDuration: Double = 10

    var body: some View {
        VStack(spacing: 6) {
            // 视频预览
            ZStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(Color.black)

                if let url = state.videoURL {
                    VideoPlayer(player: player)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                } else {
                    VStack(spacing: 8) {
                        Text("🎬").font(.system(size: 48))
                        Text("视频未加载").font(.system(size: 14))
                            .foregroundColor(AppTheme.textMuted)
                    }
                }
            }
            .aspectRatio(1, contentMode: .fit)

            // 裁剪控制
            VStack(spacing: 8) {
                // 时间标签
                HStack {
                    Text("起始: **\(formatTime(state.trimStart))**").font(.system(size: 13))
                    Spacer()
                    Text("时长: **\(String(format: "%.1f", state.trimEnd - state.trimStart))s**")
                        .font(.system(size: 13)).foregroundColor(AppTheme.accent)
                    Spacer()
                    Text("结束: **\(formatTime(state.trimEnd))**").font(.system(size: 13))
                }
                .foregroundColor(AppTheme.textPrimary)

                // 裁剪滑块
                trimSlider
                    .padding(.vertical, 8)

                // 倍速选择
                speedPicker

                // 帧数预估
                HStack {
                    Spacer()
                    Text("预计 \(estimatedFrames) 帧 / 约 \(estimatedSize) KB")
                        .font(.system(size: 11))
                        .foregroundColor(AppTheme.textMuted)
                }
            }
            .padding(10)
            .background(AppTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            // 预览按钮
            previewButton
        }
        .onAppear { setupPlayer() }
    }

    // MARK: - 裁剪滑块
    private var trimSlider: some View {
        let duration = max(state.videoDuration, 1)
        let startPercent = state.trimStart / duration
        let endPercent = state.trimEnd / duration

        return VStack(spacing: 0) {
            ZStack(alignment: .leading) {
                // 轨道底色
                RoundedRectangle(cornerRadius: 4)
                    .fill(AppTheme.background)
                    .frame(height: 48)

                // 选中范围高亮
                RoundedRectangle(cornerRadius: 0)
                    .fill(AppTheme.accent.opacity(0.15))
                    .overlay(
                        Rectangle()
                            .strokeBorder(AppTheme.accent, style: StrokeStyle(lineWidth: 3))
                    )
                    .frame(width: max(CGFloat(endPercent - startPercent) * UIScreen.main.bounds.width * 0.85, 0),
                           height: 48)
                    .offset(x: CGFloat(startPercent) * UIScreen.main.bounds.width * 0.85)

                // 左遮罩
                Rectangle()
                    .fill(AppTheme.overlayMask)
                    .frame(width: max(CGFloat(startPercent) * UIScreen.main.bounds.width * 0.85, 0),
                           height: 48)

                // 右遮罩
                HStack { Spacer() }
                    .frame(width: max(CGFloat(1 - endPercent) * UIScreen.main.bounds.width * 0.85, 0),
                           height: 48)
                    .background(AppTheme.overlayMask)
                    .offset(x: CGFloat(endPercent) * UIScreen.main.bounds.width * 0.85)

                // 起始手柄
                handle
                    .offset(x: CGFloat(startPercent) * UIScreen.main.bounds.width * 0.85 - 10)
                    .gesture(
                        DragGesture()
                            .onChanged { v in
                                let ratio = max(0, min(v.location.x / (UIScreen.main.bounds.width * 0.85), 1))
                                var newStart = ratio * state.videoDuration
                                newStart = max(0, min(newStart, state.trimEnd - 0.1))
                                if state.trimEnd - newStart <= maxTrimDuration {
                                    state.trimStart = newStart
                                } else {
                                    state.trimStart = state.trimEnd - maxTrimDuration
                                }
                            }
                    )

                // 结束手柄
                handle
                    .offset(x: CGFloat(endPercent) * UIScreen.main.bounds.width * 0.85 - 10)
                    .gesture(
                        DragGesture()
                            .onChanged { v in
                                let ratio = max(0, min(v.location.x / (UIScreen.main.bounds.width * 0.85), 1))
                                var newEnd = ratio * state.videoDuration
                                newEnd = max(state.trimStart + 0.1, min(newEnd, state.videoDuration))
                                if newEnd - state.trimStart <= maxTrimDuration {
                                    state.trimEnd = newEnd
                                } else {
                                    state.trimEnd = state.trimStart + maxTrimDuration
                                }
                            }
                    )
            }
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    private var handle: some View {
        RoundedRectangle(cornerRadius: 4)
            .fill(AppTheme.accent)
            .frame(width: 20, height: 56)
            .shadow(color: AppTheme.accent.opacity(0.5), radius: 6)
            .overlay(
                RoundedRectangle(cornerRadius: 1)
                    .fill(Color.white.opacity(0.6))
                    .frame(width: 2, height: 18)
            )
    }

    // MARK: - 倍速
    private var speedPicker: some View {
        let speeds: [Double] = [0.25, 0.5, 1.0, 1.5, 2.0]
        return HStack(spacing: 6) {
            Text("倍速:")
                .font(.system(size: 11))
                .foregroundColor(AppTheme.textSecondary)
            ForEach(speeds, id: \.self) { s in
                Button {
                    state.speed = s
                } label: {
                    Text(speedLabel(s))
                        .font(.system(size: 12, weight: .medium))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 5)
                        .background(
                            RoundedRectangle(cornerRadius: 6)
                                .fill(state.speed == s ? AppTheme.accent : Color.clear)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 6)
                                .stroke(state.speed == s ? AppTheme.accent : AppTheme.border, lineWidth: 1)
                        )
                        .foregroundColor(state.speed == s ? .white : AppTheme.textPrimary)
                }
            }
        }
    }

    // MARK: - 预览按钮
    private var previewButton: some View {
        Button {
            previewSegment()
        } label: {
            Text("▶ 预览片段")
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(AppTheme.accent)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(AppTheme.accent, lineWidth: 1)
                )
        }
    }

    // MARK: - Helper
    private func setupPlayer() {
        guard let url = state.videoURL else { return }
        player = AVPlayer(url: url)
        if state.trimEnd == 0 {
            state.trimStart = 0
            state.trimEnd = min(maxTrimDuration, state.videoDuration)
        }
    }

    private func previewSegment() {
        guard let player, let item = player.currentItem else { return }
        let start = CMTime(seconds: state.trimStart, preferredTimescale: 600)
        let end = CMTime(seconds: state.trimEnd, preferredTimescale: 600)
        player.seek(to: start)
        player.play()
        // stop at end
        player.addBoundaryTimeObserver(forTimes: [NSValue(time: end)], queue: .main) {
            player.pause()
            player.seek(to: start)
        }
    }

    private func formatTime(_ sec: Double) -> String {
        let m = Int(sec) / 60
        let s = sec.truncatingRemainder(dividingBy: 60)
        return "\(m):\(String(format: "%04.1f", s))"
    }

    private func speedLabel(_ s: Double) -> String {
        switch s {
        case 0.25: return "1/4x"
        case 0.5: return "1/2x"
        case 1.0: return "原速"
        default: return "\(s)x"
        }
    }

    private var estimatedFrames: Int {
        let dur = state.trimEnd - state.trimStart
        return max(1, Int(dur * 8 / state.speed))
    }

    private var estimatedSize: Int {
        estimatedFrames * 5  // rough estimate
    }
}
