import SwiftUI

/// The walkthrough's pages, in order. The words are placeholders for the three things a new user
/// most needs to hear; rewrite them for your app.
enum OnboardingPage: Int, CaseIterable, Identifiable {
    case welcome
    case benefit
    case reassurance

    var id: Int {
        rawValue
    }

    var systemImage: String {
        switch self {
        case .welcome: "hand.wave"
        case .benefit: "bell.badge"
        case .reassurance: "lock.shield"
        }
    }

    var title: Text {
        switch self {
        case .welcome: Text("Welcome", bundle: .module)
        case .benefit: Text("Stay in the loop", bundle: .module)
        case .reassurance: Text("Your data is yours", bundle: .module)
        }
    }

    var message: Text {
        switch self {
        case .welcome: Text("A short sentence about what this app does for the person reading it.", bundle: .module)
        case .benefit: Text("What they will get out of it, phrased as a benefit rather than a feature.", bundle: .module)
        case .reassurance:
            Text("The reassurance that removes whatever hesitation they have about signing up.", bundle: .module)
        }
    }
}
