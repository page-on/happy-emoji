import SwiftUI

struct TextEditorView: View {
    @ObservedObject var state: AppState

    @State private var dragOffset: CGSize = .zero

    var body: some View {
        VStack(spacing: 6) {
            // 预览区（含文字叠加）
            previewWithText

            // 控制面板
            VStack(spacing: 6) {
                // 文字输入
                textInputGroup

                // 字体选择
                fontPickerGroup

                // 颜色选择
                colorPickerGroup

                // 文字大小
                sizeSliderGroup
            }
        }
    }

    // MARK: - 预览 + 文字叠加
    private var previewWithText: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(Color.black)

            // 素材预览
            if let image = state.photoImage, state.mediaType == .photo {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
            } else if state.mediaType == .burst, let first = state.burstImages.first {
                Image(uiImage: first)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
            } else if state.mediaType == .video {
                Rectangle()
                    .fill(Color.black)
                    .overlay(
                        Text("🎬")
                            .font(.system(size: 48))
                            .foregroundColor(AppTheme.textMuted)
                    )
                    .overlay(
                        Text("视频裁剪区")
                            .font(.system(size: 12))
                            .foregroundColor(AppTheme.textMuted)
                            .offset(y: 35)
                    )
            } else {
                Rectangle()
                    .fill(Color.black)
            }

            // 文字叠加层
            GeometryReader { geo in
                if !state.textContent.isEmpty {
                    Text(state.textContent)
                        .font(fontForText)
                        .fontWeight(state.textWeight == "900" ? .black : .bold)
                        .foregroundColor(state.textColor)
                        .multilineTextAlignment(.center)
                        .shadow(color: .black.opacity(0.7), radius: 4, x: 2, y: 2)
                        .position(
                            x: state.textX * geo.size.width + dragOffset.width,
                            y: state.textY * geo.size.height + dragOffset.height
                        )
                        .gesture(
                            DragGesture()
                                .onChanged { v in
                                    dragOffset = v.translation
                                }
                                .onEnded { v in
                                    let newX = (state.textX * geo.size.width + v.translation.width) / geo.size.width
                                    let newY = (state.textY * geo.size.height + v.translation.height) / geo.size.height
                                    state.textX = max(0.05, min(0.95, newX))
                                    state.textY = max(0.05, min(0.95, newY))
                                    dragOffset = .zero
                                }
                        )
                        .onTapGesture(count: 2) {
                            // 双击重置到中心
                            withAnimation(.easeInOut(duration: 0.3)) {
                                state.textX = 0.5
                                state.textY = 0.5
                            }
                        }
                }
            }
        }
        .aspectRatio(1, contentMode: .fit)
    }

    // MARK: - 文字输入
    private var textInputGroup: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("文字内容").font(.system(size: 11))
                .foregroundColor(AppTheme.textMuted)
                .textCase(.uppercase)

            TextField("输入文字...", text: $state.textContent)
                .padding(10)
                .background(AppTheme.background)
                .cornerRadius(6)
                .overlay(
                    RoundedRectangle(cornerRadius: 6)
                        .stroke(AppTheme.border, lineWidth: 1)
                )
                .foregroundColor(AppTheme.textPrimary)
                .font(.system(size: 14))
        }
        .padding(10)
        .background(AppTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - 字体选择
    private var fontPickerGroup: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("字体").font(.system(size: 11))
                .foregroundColor(AppTheme.textMuted)
                .textCase(.uppercase)

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 4),
                      spacing: 6) {
                ForEach(Array(AppFont.all.enumerated()), id: \.offset) { i, font in
                    Button {
                        state.textFont = font.value
                        state.textWeight = font.label == "粗黑搞怪" ? "900" :
                                           font.label == "萌萌圆体" ? "400" : "700"
                    } label: {
                        Text(font.label)
                            .font(.system(size: 12, weight: .medium))
                            .padding(.vertical, 6)
                            .frame(maxWidth: .infinity)
                            .background(
                                RoundedRectangle(cornerRadius: 6)
                                    .fill(state.textFont == font.value ? AppTheme.accent : Color.clear)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 6)
                                    .stroke(
                                        state.textFont == font.value ? AppTheme.accent : AppTheme.border,
                                        lineWidth: 1
                                    )
                            )
                            .foregroundColor(state.textFont == font.value ? .white : AppTheme.textPrimary)
                    }
                }
            }
        }
        .padding(10)
        .background(AppTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - 颜色选择
    private var colorPickerGroup: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("文字颜色").font(.system(size: 11))
                .foregroundColor(AppTheme.textMuted)
                .textCase(.uppercase)

            HStack(spacing: 8) {
                ForEach(Array(AppTheme.textColors.enumerated()), id: \.offset) { i, c in
                    Button {
                        state.textColorHex = c.hex
                        state.textColor = Color(hex: c.hex)
                    } label: {
                        Circle()
                            .fill(Color(hex: c.hex))
                            .frame(width: 28, height: 28)
                            .overlay(
                                Circle()
                                    .stroke(state.textColorHex == c.hex ? Color.white : Color.clear,
                                            lineWidth: 2)
                            )
                            .scaleEffect(state.textColorHex == c.hex ? 1.15 : 1.0)
                            .shadow(color: state.textColorHex == c.hex ? .white.opacity(0.4) : .clear,
                                    radius: 8)
                            .animation(.easeInOut(duration: 0.2), value: state.textColorHex)
                    }
                }
            }
        }
        .padding(10)
        .background(AppTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - 文字大小
    private var sizeSliderGroup: some View {
        VStack(spacing: 4) {
            HStack {
                Text("文字大小")
                    .font(.system(size: 11))
                    .foregroundColor(AppTheme.textMuted)
                Spacer()
                Text("\(Int(state.textSize))pt")
                    .font(.system(size: 11))
                    .foregroundColor(AppTheme.accent)
                    .fontWeight(.bold)
            }

            Slider(value: $state.textSize, in: 16...80, step: 1) { _ in }
                .tint(AppTheme.accent)

            HStack {
                Text("16")
                Spacer()
                Text("80")
            }
            .font(.system(size: 11))
            .foregroundColor(AppTheme.textMuted)
        }
        .padding(10)
        .background(AppTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - 字体解析
    private var fontForText: Font {
        let baseSize = state.textSize
        switch AppFont.all.first(where: { $0.value == state.textFont })?.label {
        case "粗黑搞怪": return .system(size: baseSize, design: .default).weight(.black)
        case "手写涂鸦": return .system(size: baseSize, design: .serif).weight(.bold)
        case "萌萌圆体": return .system(size: baseSize, design: .rounded).weight(.regular)
        case "圆体": return .system(size: baseSize, design: .rounded).weight(.bold)
        case "仿宋": return .system(size: baseSize, design: .serif).weight(.bold)
        case "楷体": return .system(size: baseSize, design: .serif).weight(.bold)
        default: return .system(size: baseSize, design: .default).weight(.bold)
        }
    }
}
