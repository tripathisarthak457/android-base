// swift-tools-version: 6.2
import PackageDescription

// Interface code runs on the main actor unless it says otherwise, as in Xcode's app template.
let interfaceSettings: [SwiftSetting] = [
    .defaultIsolation(MainActor.self),
    .enableUpcomingFeature("InferIsolatedConformances"),
    .enableUpcomingFeature("NonisolatedNonsendingByDefault"),
]

// Everything else stays nonisolated, so decoding and storage work never queues behind a frame on
// the main actor. An async function runs on its caller's actor unless it is marked `@concurrent`.
let backgroundSettings: [SwiftSetting] = [
    .enableUpcomingFeature("NonisolatedNonsendingByDefault"),
]

let package = Package(
    name: "Core",
    defaultLocalization: "en",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "AppCore", targets: ["AppCore"]),
        .library(name: "DesignSystem", targets: ["DesignSystem"]),
        .library(name: "Navigation", targets: ["Navigation"]),
        // <opt:network>
        .library(name: "Networking", targets: ["Networking"]),
        // </opt:network>
        .library(name: "Persistence", targets: ["Persistence"]),
        // For test targets only, as the Android template's core:testing.
        .library(name: "AppTesting", targets: ["AppTesting"]),
    ],
    targets: [
        .target(
            name: "AppCore",
            swiftSettings: backgroundSettings
        ),
        .target(
            name: "DesignSystem",
            dependencies: ["AppCore"],
            resources: [.process("Resources")],
            swiftSettings: interfaceSettings
        ),
        .target(
            name: "Navigation",
            swiftSettings: interfaceSettings
        ),
        // <opt:network>
        .target(
            name: "Networking",
            dependencies: ["AppCore"],
            swiftSettings: backgroundSettings
        ),
        // </opt:network>
        .target(
            name: "Persistence",
            swiftSettings: backgroundSettings
        ),
        .target(
            name: "AppTesting",
            swiftSettings: backgroundSettings
        ),
        .testTarget(
            name: "AppCoreTests",
            dependencies: ["AppCore"],
            swiftSettings: backgroundSettings
        ),
        .testTarget(
            name: "DesignSystemTests",
            dependencies: ["DesignSystem", "AppTesting"],
            swiftSettings: interfaceSettings
        ),
        .testTarget(
            name: "NavigationTests",
            dependencies: ["Navigation"],
            swiftSettings: interfaceSettings
        ),
        // <opt:network>
        .testTarget(
            name: "NetworkingTests",
            dependencies: ["Networking", "AppCore"],
            swiftSettings: backgroundSettings
        ),
        // </opt:network>
        .testTarget(
            name: "PersistenceTests",
            dependencies: ["Persistence"],
            swiftSettings: backgroundSettings
        ),
        .testTarget(
            name: "AppTestingTests",
            dependencies: ["AppTesting"],
            swiftSettings: backgroundSettings
        ),
    ]
)
