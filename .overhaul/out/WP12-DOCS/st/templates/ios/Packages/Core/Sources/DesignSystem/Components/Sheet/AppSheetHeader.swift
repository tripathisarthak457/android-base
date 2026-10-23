import SwiftUI

extension View {
    /// The sheet's title row: `title`, inline, with a button that closes the sheet. The button asks
    /// the content's dismiss guards first, as a swipe does.
    ///
    /// Apply it last, to the sheet's root content. It puts the content in a `NavigationStack`, so
    /// the content can push screens of its own and VoiceOver names the sheet by the title. A stack
    /// has no height of its own to fit, so a `.fitContent` sheet carries its title in its content.
    public func appSheetHeader(_ title: Text) -> some View {
        appSheetHeader(title) {
            ToolbarItem(placement: .cancellationAction) {
                AppSheetCloseButton()
            }
        }
    }

    /// The sheet's title row with `items` in place of the close button: Cancel in
    /// `.cancellationAction` and Done in `.confirmationAction`, drawn as glass from iOS 26. Keep
    /// `AppSheetCloseButton` in `.cancellationAction` to close the sheet as the default header does.
    public func appSheetHeader<Items: ToolbarContent>(
        _ title: Text,
        @ToolbarContentBuilder items: () -> Items
    ) -> some View {
        let items = items()
        let root = navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                items
            }
        return NavigationStack {
            root
        }
    }
}

#Preview("Header, footer and confirmation") {
    @Previewable @State var isPresented = true
    @Previewable @State var name = "Groceries"
    Button {
        isPresented = true
    } label: {
        Text(verbatim: "Rename the list")
    }
    .appSheet(isPresented: $isPresented, options: AppSheetOptions(detents: [.medium, .large], initialDetent: .large)) {
        Form {
            TextField(text: $name) {
                Text(verbatim: "Name")
            }
        }
        .appSheetDismissConfirmation(isEnabled: name != "Groceries") {
            isPresented = false
        }
        .appSheetFooter {
            Button {
                isPresented = false
            } label: {
                Text(verbatim: "Save")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.app(.filled, size: .large))
        }
        .appSheetHeader(Text(verbatim: "Rename List"))
    }
    .appTheme()
}
