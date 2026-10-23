import SwiftUI

/// The button that closes the sheet it is in: the one `appSheetHeader(_:)` shows, for a header with
/// items of its own. Place it in `.cancellationAction`.
///
/// Like a swipe down, it asks the content's dismiss guards first and closes the sheet only when none
/// is enabled. It shows nothing in a sheet that is not `dismissible`, which people cannot close, and
/// outside a sheet the app presented.
public struct AppSheetCloseButton: View {
    @Environment(\.appSheetGuards) private var guards
    @Environment(\.isAppSheetDismissible) private var isDismissible
    @Environment(\.dismiss) private var dismiss

    public init() {}

    public var body: some View {
        if let guards, isDismissible {
            if #available(iOS 26, *) {
                // The close role brings the system's symbol and spoken label, drawn as glass.
                Button(role: .close) {
                    close(asking: guards)
                }
            } else {
                Button {
                    close(asking: guards)
                } label: {
                    Text("Close", bundle: .module)
                }
            }
        }
    }

    private func close(asking guards: AppSheetGuards) {
        if !guards.attemptDismissal() {
            dismiss()
        }
    }
}
