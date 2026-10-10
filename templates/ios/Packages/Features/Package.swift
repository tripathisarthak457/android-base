// swift-tools-version: 6.2
import PackageDescription

// The graph mirrors the Android template's: a feature depends on its own data target and on Core,
// a data target never depends on a feature, and features never depend on each other.

// Feature targets are interface code: main actor by default, as in Xcode's app template.
let featureSettings: [SwiftSetting] = [
    .defaultIsolation(MainActor.self),
    .enableUpcomingFeature("InferIsolatedConformances"),
    .enableUpcomingFeature("NonisolatedNonsendingByDefault"),
]

// Data targets stay nonisolated, so decoding and storage never run on the main actor unless a
// caller is waiting there for a result. Work that must leave the caller's actor is `@concurrent`.
let dataSettings: [SwiftSetting] = [
    .enableUpcomingFeature("NonisolatedNonsendingByDefault"),
]

let package = Package(
    name: "Features",
    defaultLocalization: "en",
    platforms: [.iOS(.v17)],
    products: [
        // <opt:sample>
        .library(name: "SampleFeature", targets: ["SampleFeature"]),
        // </opt:sample>
        // <opt:settings>
        .library(name: "SettingsFeature", targets: ["SettingsFeature"]),
        // </opt:settings>
        // <opt:onboarding>
        .library(name: "OnboardingFeature", targets: ["OnboardingFeature"]),
        // </opt:onboarding>
        // <generated:feature-products>
    ],
    dependencies: [
        .package(path: "../Core"),
    ],
    targets: [
        // <opt:sample>
        .target(
            name: "SampleData",
            dependencies: [
                .product(name: "AppCore", package: "Core"),
                .product(name: "Networking", package: "Core"),
            ],
            swiftSettings: dataSettings
        ),
        .target(
            name: "SampleFeature",
            dependencies: [
                "SampleData",
                .product(name: "AppCore", package: "Core"),
                .product(name: "DesignSystem", package: "Core"),
                .product(name: "Navigation", package: "Core"),
                .product(name: "Networking", package: "Core"),
            ],
            resources: [.process("Resources")],
            swiftSettings: featureSettings
        ),
        .testTarget(
            name: "SampleDataTests",
            dependencies: [
                "SampleData",
                .product(name: "AppCore", package: "Core"),
                .product(name: "Networking", package: "Core"),
                // <opt:database>
                .product(name: "Persistence", package: "Core"),
                // </opt:database>
            ],
            swiftSettings: dataSettings
        ),
        .testTarget(
            name: "SampleFeatureTests",
            dependencies: [
                "SampleFeature",
                "SampleData",
                .product(name: "AppCore", package: "Core"),
                .product(name: "DesignSystem", package: "Core"),
                .product(name: "Networking", package: "Core"),
            ],
            swiftSettings: featureSettings
        ),
        // </opt:sample>
        // <opt:settings>
        .target(
            name: "SettingsFeature",
            dependencies: [
                .product(name: "AppCore", package: "Core"),
                .product(name: "DesignSystem", package: "Core"),
                .product(name: "Navigation", package: "Core"),
                .product(name: "Persistence", package: "Core"),
            ],
            resources: [.process("Resources")],
            swiftSettings: featureSettings
        ),
        .testTarget(
            name: "SettingsFeatureTests",
            dependencies: [
                "SettingsFeature",
                .product(name: "Persistence", package: "Core"),
            ],
            swiftSettings: featureSettings
        ),
        // </opt:settings>
        // <opt:onboarding>
        .target(
            name: "OnboardingFeature",
            dependencies: [
                .product(name: "DesignSystem", package: "Core"),
                .product(name: "Persistence", package: "Core"),
            ],
            resources: [.process("Resources")],
            swiftSettings: featureSettings
        ),
        .testTarget(
            name: "OnboardingFeatureTests",
            dependencies: [
                "OnboardingFeature",
                .product(name: "Persistence", package: "Core"),
            ],
            swiftSettings: featureSettings
        ),
        // </opt:onboarding>
        // <generated:feature-targets>
    ]
)
