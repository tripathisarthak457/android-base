import AppCore
import SwiftUI

/// A failed load, in words for its `AppError`, with a button to try again.
///
/// Use it when there is no content to keep on screen. When a refresh fails over content that is
/// still worth showing, keep the content and show a toast instead.
public struct AppFailureView: View {
    private let error: AppError
    private let retry: (() -> Void)?

    public init(_ error: AppError, retry: (() -> Void)? = nil) {
        self.error = error
        self.retry = retry
    }

    public var body: some View {
        AppStateView(title, systemImage: systemImage, message: message) {
            if let retry {
                Button(action: retry) {
                    Text("Try Again", bundle: .module)
                }
                .buttonStyle(.app(.tinted))
            }
        }
    }

    private var title: Text {
        switch error {
        case .offline: Text("You're Offline", bundle: .module)
        case .timeout: Text("No Response", bundle: .module)
        case .http(let failure) where failure.status >= 500: Text("Server Problem", bundle: .module)
        case .http: Text("Request Failed", bundle: .module)
        case .decode, .storage, .unexpected, .cancelled: Text("Something Went Wrong", bundle: .module)
        }
    }

    private var message: Text {
        switch error {
        case .offline:
            Text("Check your connection, then try again.", bundle: .module)
        case .timeout:
            Text("The server took too long to answer. Try again in a moment.", bundle: .module)
        case .http(let failure):
            if let message = failure.message, !message.isEmpty {
                Text(message)
            } else if failure.status >= 500 {
                Text("The server couldn't handle the request. Try again in a moment.", bundle: .module)
            } else {
                Text("The server turned the request down.", bundle: .module)
            }
        case .decode:
            Text("The app received something it couldn't read.", bundle: .module)
        case .storage:
            Text("Saved data couldn't be read or written.", bundle: .module)
        case .unexpected, .cancelled:
            Text("Try again. If it keeps happening, restart the app.", bundle: .module)
        }
    }

    private var systemImage: String {
        switch error {
        case .offline: "wifi.slash"
        case .timeout: "clock.badge.exclamationmark"
        case .http, .decode, .storage, .unexpected, .cancelled: "exclamationmark.triangle"
        }
    }
}

#Preview("Failures") {
    TabView {
        AppFailureView(.offline) {}
        AppFailureView(.http(HTTPFailure(status: 503))) {}
        AppFailureView(.decode("keyNotFound")) {}
    }
    .tabViewStyle(.page)
    .appTheme()
}
