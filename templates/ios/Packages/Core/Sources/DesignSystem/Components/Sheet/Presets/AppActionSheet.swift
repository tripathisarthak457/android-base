import SwiftUI

extension View {
    /// Presents `actions` while `isPresented` is true: "What would you like to do with this?"
    ///
    /// It is the system's confirmation dialog: an action sheet with Cancel apart on a phone, and on
    /// an iPad a popover pointing at the view it is applied to, so apply it to the control that
    /// opens it. An action runs and then the dialog closes.
    public func appActionSheet(
        isPresented: Binding<Bool>,
        title: Text? = nil,
        message: Text? = nil,
        actions: [AppSheetAction]
    ) -> some View {
        confirmationDialog(
            title ?? Text(verbatim: ""),
            isPresented: isPresented,
            titleVisibility: title == nil ? .hidden : .visible
        ) {
            ForEach(actions.indices, id: \.self) { index in
                let action = actions[index]
                Button(role: action.role, action: action.action) {
                    action.label
                }
            }
        } message: {
            if let message {
                message
            }
        }
    }
}

#Preview("Action sheet") {
    @Previewable @State var isPresented = true
    Button {
        isPresented = true
    } label: {
        Text(verbatim: "Show the actions")
    }
    .appActionSheet(
        isPresented: $isPresented,
        title: Text(verbatim: "Groceries"),
        actions: [
            AppSheetAction(Text(verbatim: "Share")) {},
            AppSheetAction(Text(verbatim: "Rename")) {},
            AppSheetAction(Text(verbatim: "Delete List"), role: .destructive) {},
        ]
    )
    .appTheme()
}
