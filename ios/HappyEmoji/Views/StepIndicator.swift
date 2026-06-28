import SwiftUI

struct StepIndicator: View {
    @ObservedObject var state: AppState

    var body: some View {
        HStack(spacing: 6) {
            ForEach(Array(state.steps.enumerated()), id: \.offset) { i, _ in
                Circle()
                    .fill(stepColor(i))
                    .frame(width: 26, height: 26)
                    .overlay(
                        Text("\(i + 1)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(i == state.step ? .white : AppTheme.textMuted)
                    )
                    .shadow(color: i == state.step ? AppTheme.accent.opacity(0.5) : .clear, radius: 12)

                if i < state.steps.count - 1 {
                    Rectangle()
                        .fill(i < state.step ? AppTheme.success : AppTheme.surface)
                        .frame(height: 2)
                        .frame(maxWidth: 40)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    private func stepColor(_ i: Int) -> Color {
        if i == state.step { return AppTheme.accent }
        if i < state.step { return AppTheme.success }
        return AppTheme.surface
    }
}
