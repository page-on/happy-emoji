import SwiftUI
import PhotosUI

struct BurstEditor: View {
    @ObservedObject var state: AppState
    @State private var showAddPicker = false
    @State private var addItems: [PhotosPickerItem] = []

    var body: some View {
        VStack(spacing: 10) {
            // 缩略图网格
            VStack(alignment: .leading, spacing: 6) {
                Text("帧排序（\(state.burstImages.count)/20 张）— 可拖拽排序，点击 × 删除")
                    .font(.system(size: 11))
                    .foregroundColor(AppTheme.textMuted)
                    .textCase(.uppercase)

                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6),
                                         count: min(4, max(3, state.burstImages.count))),
                          spacing: 6) {
                    ForEach(Array(state.burstImages.enumerated()), id: \.offset) { i, img in
                        ZStack(alignment: .topTrailing) {
                            Image(uiImage: img)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 72, height: 72)
                                .clipShape(RoundedRectangle(cornerRadius: 6))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 6)
                                        .stroke(AppTheme.border, lineWidth: 2)
                                )
                                .onDrag {
                                    NSItemProvider(object: String(i) as NSString)
                                }
                                .onDrop(of: [.text], delegate: FrameDropDelegate(
                                    index: i,
                                    images: $state.burstImages
                                ))

                            // 删除按钮
                            Button {
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    state.burstImages.remove(at: i)
                                }
                            } label: {
                                ZStack {
                                    Circle()
                                        .fill(Color.black.opacity(0.6))
                                        .frame(width: 18, height: 18)
                                    Text("×")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(.white)
                                }
                            }
                            .padding(2)
                        }
                    }
                }
            }
            .padding(10)
            .background(AppTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            // 添加更多
            if state.burstImages.count < 20 {
                Button {
                    showAddPicker = true
                } label: {
                    Text("+ 添加更多照片")
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

            // 帧间隔
            VStack(spacing: 4) {
                Text("帧间隔").font(.system(size: 11))
                    .foregroundColor(AppTheme.textMuted)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Slider(value: $state.frameDelay, in: 500...1500, step: 100) { _ in }
                    .tint(AppTheme.accent)

                HStack {
                    Text("500ms (2fps)")
                    Spacer()
                    Text("\(Int(state.frameDelay))ms")
                        .foregroundColor(AppTheme.accent)
                        .fontWeight(.bold)
                    Spacer()
                    Text("1500ms (0.7fps)")
                }
                .font(.system(size: 11))
                .foregroundColor(AppTheme.textMuted)
            }
            .padding(10)
            .background(AppTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            // 预估
            HStack {
                Spacer()
                Text(estimateText)
                    .font(.system(size: 11))
                    .foregroundColor(AppTheme.textMuted)
            }
        }
        .photosPicker(isPresented: $showAddPicker,
                       selection: $addItems,
                       max: 20 - state.burstImages.count,
                       matching: .images)
        .onChange(of: addItems) { _, items in
            guard !items.isEmpty else { return }
            Task {
                for item in items {
                    guard state.burstImages.count < 20,
                          let data = try? await item.loadTransferable(type: Data.self),
                          let img = UIImage(data: data) else { continue }
                    state.burstImages.append(img)
                }
                addItems = []
            }
        }
    }

    private var estimateText: String {
        let dur = Double(state.burstImages.count) * state.frameDelay / 1000
        let kb = state.burstImages.count * ExportQuality.medium.width * 18 / 1000
        return "预计 \(state.burstImages.count) 帧 / \(String(format: "%.1f", dur))s / 约 \(kb)KB"
    }
}

// MARK: - 拖拽代理
struct FrameDropDelegate: DropDelegate {
    let index: Int
    @Binding var images: [UIImage]

    func performDrop(info: DropInfo) -> Bool {
        guard let provider = info.itemProviders(for: [.text]).first else { return false }
        _ = provider.loadObject(ofClass: NSString.self) { str, _ in
            guard let s = str as? String, let fromIdx = Int(s) else { return }
            DispatchQueue.main.async {
                let moved = images.remove(at: fromIdx)
                images.insert(moved, at: index)
            }
        }
        return true
    }
}
