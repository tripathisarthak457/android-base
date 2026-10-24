import DesignSystem
import SampleData
import SwiftUI

/// A sample in a list: its title, and the start of its body.
struct SampleRow: View {
    let item: SampleItem

    @Environment(\.appTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.xxs) {
            Group {
                if item.title.isEmpty {
                    Text("Untitled", bundle: .module)
                } else {
                    Text(item.title)
                }
            }
            .font(theme.typography.headline)
            .foregroundStyle(theme.colors.contentPrimary)
            .lineLimit(2)
            Text(item.preview)
                .font(theme.typography.subheadline)
                .foregroundStyle(theme.colors.contentSecondary)
                .lineLimit(2)
        }
        .padding(.vertical, AppSpacing.xxs)
    }
}
