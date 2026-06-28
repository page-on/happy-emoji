import SwiftUI

struct ContentView: View {
    @StateObject private var state = AppState()

    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // 步骤指示器
                StepIndicator(state: state)

                // 内容区
                ScrollView {
                    VStack(spacing: 0) {
                        currentStepView
                            .padding(.horizontal, 12)
                    }
                    .frame(maxWidth: .infinity, minHeight: contentMinHeight)
                }

                // 底部按钮
                actionBar
                    .padding(.horizontal, 12)
                    .padding(.bottom, 8)
            }
        }
        // Toast 浮层
        .overlay(alignment: .bottom) {
            if let msg = state.toastMessage {
                ToastView(message: msg, isError: state.toastIsError)
                    .padding(.bottom, 80)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .animation(.easeInOut, value: state.toastMessage)
            }
        }
        .animation(.easeInOut(duration: 0.25), value: state.step)
    }

    // MARK: - 当前步骤视图
    @ViewBuilder
    private var currentStepView: some View {
        switch (state.step, state.mediaType) {
        // Step 0: 素材选择
        case (0, _):
            MediaPicker(state: state)

        // Step 1: 视频裁剪 / 连拍排序 / 照片→跳过
        case (1, .video):
            VideoTrimmer(state: state)
        case (1, .burst):
            BurstEditor(state: state)
        case (1, .photo):
            TextEditorView(state: state)  // 照片直接到文字

        // Step 2: 文字编辑
        case (2, .burst), (2, .photo), (2, _):
            TextEditorView(state: state)

        // Step 3: 导出
        case (_, _):
            ExportView(state: state)
        }
    }

    // MARK: - 底部操作栏
    private var actionBar: some View {
        HStack(spacing: 8) {
            // 上一步按钮
            if state.step > 0 {
                Button {
                    withAnimation { state.step -= 1 }
                } label: {
                    Text("上一步")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppTheme.accent)
                        .padding(.vertical, 12)
                        .frame(maxWidth: .infinity)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(AppTheme.accent, lineWidth: 1)
                        )
                }
            }

            // 下一步 / 导出按钮
            Button {
                handleNext()
            } label: {
                Text(nextButtonLabel)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.vertical, 12)
                    .frame(maxWidth: .infinity)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(canProceed ? AppTheme.accent : AppTheme.border)
                    )
            }
            .disabled(!canProceed)
        }
        .padding(.vertical, 10)
    }

    // MARK: - 辅助计算
    private var contentMinHeight: CGFloat {
        let screenH = UIScreen.main.bounds.height
        return screenH - 120  // minus step indicator + action bar + safe areas
    }

    private var canProceed: Bool {
        switch (state.step, state.mediaType) {
        case (0, _):
            switch state.mediaType {
            case .video: return state.videoURL != nil
            case .photo: return state.photoImage != nil
            case .burst: return !state.burstImages.isEmpty
            }
        case (1, .burst):
            return !state.burstImages.isEmpty
        default:
            return true
        }
    }

    private var nextButtonLabel: String {
        if state.isExporting { return "处理中..." }
        let isPhoto = state.mediaType == .photo
        let isBurst = state.mediaType == .burst

        switch state.step {
        case 0:
            return isBurst ? "下一步：排序编辑" :
                   isPhoto ? "下一步：添加文字" : "下一步：裁剪时间"
        case 1:
            return state.mediaType == .burst ? "下一步：添加文字" : "下一步：添加文字"
        case 2:
            return state.mediaType == .photo ? "导出图片" : "导出 GIF"
        default:
            return "完成"
        }
    }

    private func handleNext() {
        guard canProceed else {
            state.showToast("请先选择素材", error: true)
            return
        }

        let isPhoto = state.mediaType == .photo
        let isBurst = state.mediaType == .burst

        // 计算总步数
        let totalSteps = isPhoto ? 3 : (isBurst ? 4 : 4)
        let maxStep = totalSteps - 1

        if state.step < maxStep {
            withAnimation { state.step += 1 }
            // 照片模式跳过 step 1
            if isPhoto && state.step == 1 {
                state.step = 2
            }
        } else {
            // 最后一步：导出
            if state.mediaType == .photo {
                // 由 ExportView 处理
            }
        }
    }
}

// MARK: - Toast 组件
struct ToastView: View {
    let message: String
    let isError: Bool

    var body: some View {
        Text(message)
            .font(.system(size: 14, weight: .semibold))
            .foregroundColor(.white)
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(isError ? AppTheme.error : AppTheme.success)
            )
            .shadow(radius: 10)
    }
}

#Preview {
    ContentView()
}
