import SwiftUI

extension View {
    /// Presents `content` as the app's sheet while `isPresented` is true.
    ///
    /// It is the system's sheet, so the card stack behind it, the detent motion, the rubber band at
    /// the top, interactive dismissal, the keyboard and VoiceOver all behave as everywhere else on
    /// the device. The design style supplies its colours, and before iOS 26 its corner radius;
    /// iOS 26 sheets take the system's concentric corners.
    ///
    /// - Parameters:
    ///   - options: the detents, modality, dismissal, material, placement and grabber.
    ///   - selection: the detent the sheet rests at, one of `options.detents`. Dragging the sheet
    ///     changes it, and changing it moves the sheet.
    ///   - onDismiss: called once the sheet has gone, however it was closed.
    public func appSheet<Content: View>(
        isPresented: Binding<Bool>,
        options: AppSheetOptions = AppSheetOptions(),
        selection: Binding<AppSheetDetent>? = nil,
        onDismiss: (() -> Void)? = nil,
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        sheet(isPresented: isPresented, onDismiss: onDismiss) {
            content().appSheetPresentation(options, selection: selection)
        }
    }

    /// Presents a sheet for `item` while it is not nil, as `appSheet(isPresented:)` does. An item
    /// with another identity replaces the sheet with a sheet of its own.
    public func appSheet<Item: Identifiable, Content: View>(
        item: Binding<Item?>,
        options: AppSheetOptions = AppSheetOptions(),
        selection: Binding<AppSheetDetent>? = nil,
        onDismiss: (() -> Void)? = nil,
        @ViewBuilder content: @escaping (Item) -> Content
    ) -> some View {
        sheet(item: item, onDismiss: onDismiss) { item in
            content(item).appSheetPresentation(options, selection: selection)
        }
    }

    /// Gives content that something else presents as a sheet, such as an `.inspector` on a phone,
    /// the presentation `appSheet` gives its own. Where the content is not presented as a sheet,
    /// such as an inspector column in a wide window, the options do nothing.
    public func appSheetPresentation(
        _ options: AppSheetOptions = AppSheetOptions(),
        selection: Binding<AppSheetDetent>? = nil
    ) -> some View {
        modifier(AppSheetPresentation(options: options, selection: selection))
    }
}

private struct AppSheetPresentation: ViewModifier {
    let options: AppSheetOptions
    let selection: Binding<AppSheetDetent>?

    /// The content's ideal height, measured only when a detent fits it.
    @State private var contentHeight: CGFloat?

    /// Where the sheet rests when it opens at `initialDetent` and no `selection` is passed.
    @State private var restingDetent: AppSheetDetent?

    init(options: AppSheetOptions, selection: Binding<AppSheetDetent>?) {
        self.options = options
        self.selection = selection
        _restingDetent = State(initialValue: options.openingDetent)
    }

    func body(content: Content) -> some View {
        content
            .fixedSize(horizontal: false, vertical: options.fitsContent)
            .onGeometryChange(for: CGFloat.self) { proxy in
                proxy.size.height
            } action: { height in
                if options.fitsContent {
                    contentHeight = height
                }
            }
            // A fitted sheet dragged taller keeps its content at the top, where the grabber is.
            .frame(maxHeight: options.fitsContent ? .infinity : nil, alignment: .top)
            .modifier(Detents(
                detents: options.presentationDetents(contentHeight: contentHeight),
                selection: presentedSelection
            ))
            .presentationBackgroundInteraction(
                options.modality.backgroundInteraction(contentHeight: contentHeight)
            )
            .presentationContentInteraction(options.presentationContentInteraction)
            .presentationDragIndicator(options.grabberVisibility)
            .interactiveDismissDisabled(!options.dismissible)
            .environment(\.isAppSheetDismissible, options.dismissible)
            .modifier(AppSheetSurface(material: options.material))
            .modifier(AppSheetPlacementModifier(placement: options.placement))
    }

    /// `selection`, or the detent the sheet opened at, in SwiftUI's terms; nil leaves SwiftUI to
    /// open the sheet at its smallest detent. A fitted detent is presented at the content's measured
    /// height, so the mapping is made again on every read.
    private var presentedSelection: Binding<PresentationDetent>? {
        guard let detent = selection ?? Binding($restingDetent) else { return nil }
        return Binding {
            detent.wrappedValue.presentationDetent(contentHeight: contentHeight)
        } set: { presented in
            if let presentedDetent = options.detent(presentedAs: presented, contentHeight: contentHeight) {
                detent.wrappedValue = presentedDetent
            }
        }
    }
}

private struct Detents: ViewModifier {
    let detents: Set<PresentationDetent>
    let selection: Binding<PresentationDetent>?

    @ViewBuilder
    func body(content: Content) -> some View {
        if let selection {
            content.presentationDetents(detents, selection: selection)
        } else {
            content.presentationDetents(detents)
        }
    }
}

private struct AppSheetPlacementModifier: ViewModifier {
    let placement: AppSheetPlacement

    @ViewBuilder
    func body(content: Content) -> some View {
        // presentationPlacement exists only in the iOS 27 SDK, so only the Swift that SDK ships
        // with (6.4) compiles the call; `#available` alone fails to build against an older SDK.
        #if compiler(>=6.4)
        if #available(iOS 27, *) {
            switch placement {
            case .automatic:
                content
            case .center:
                content.presentationPlacement(.center)
            case .leading:
                content.presentationPlacement(.leading)
            case .trailing:
                content.presentationPlacement(.trailing)
            }
        } else {
            content
        }
        #else
        content
        #endif
    }
}

private struct AppSheetSurface: ViewModifier {
    let material: AppSheetMaterial

    @Environment(\.appTheme) private var theme

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26, *) {
            switch material {
            case .automatic, .glass:
                // The system's sheet is Liquid Glass and turns opaque as it grows to full height;
                // a background of the app's own would replace both.
                content
            case .solid, .frosted:
                content.presentationBackground(background)
            }
        } else {
            content
                .presentationBackground(background)
                .presentationCornerRadius(theme.shapes.radiusSheet)
        }
    }

    private var background: AnyShapeStyle {
        switch material {
        case .automatic, .solid:
            AnyShapeStyle(theme.colors.surfaceElevated)
        case .frosted:
            AnyShapeStyle(.regularMaterial)
        case .glass:
            AnyShapeStyle(.ultraThinMaterial)
        }
    }
}

#Preview("Detents") {
    @Previewable @State var isPresented = true
    @Previewable @State var detent = AppSheetDetent.medium
    Button {
        isPresented = true
    } label: {
        Text(verbatim: "Show the sheet")
    }
    .appSheet(
        isPresented: $isPresented,
        options: AppSheetOptions(detents: [.medium, .large]),
        selection: $detent
    ) {
        Text(verbatim: detent == .medium ? "Drag the grabber up." : "Drag the grabber down.")
            .padding(AppSpacing.gutter)
    }
    .appTheme()
}

#Preview("Fitted, frosted") {
    @Previewable @State var isPresented = true
    Button {
        isPresented = true
    } label: {
        Text(verbatim: "Show the sheet")
    }
    .appSheet(isPresented: $isPresented, options: AppSheetOptions(detents: [.fitContent], material: .frosted)) {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            Text(verbatim: "Delete this list?")
                .font(.headline)
            Text(verbatim: "Its items go with it.")
            Button(role: .destructive) {
                isPresented = false
            } label: {
                Text(verbatim: "Delete")
            }
            .buttonStyle(.app(.destructive))
        }
        .padding(AppSpacing.gutter)
    }
    .appTheme(designStyle: .playful)
}

#Preview("Non-modal") {
    @Previewable @State var taps = 0
    Button {
        taps += 1
    } label: {
        Text(verbatim: "Tapped \(taps) times behind the sheet")
    }
    .appSheet(
        isPresented: .constant(true),
        options: AppSheetOptions(
            detents: [.fraction(0.15), .medium, .large],
            modality: .nonModal(upThrough: .medium),
            dismissible: false
        )
    ) {
        List(0..<30, id: \.self) { row in
            Text(verbatim: "Row \(row)")
        }
    }
    .appTheme()
}
