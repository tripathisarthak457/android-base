import DesignSystem
import SwiftUI
import Testing

struct AppThemeTests {
    @Test(arguments: AppDesignStyle.allCases)
    func everyStyleResolvesInBothAppearances(style: AppDesignStyle) {
        let light = AppTheme(designStyle: style, motionStyle: .standard, colorScheme: .light, hapticsEnabled: true)
        let dark = AppTheme(designStyle: style, motionStyle: .standard, colorScheme: .dark, hapticsEnabled: true)
        #expect(light.colors.background != dark.colors.background)
        #expect(light.colors.contentPrimary != dark.colors.contentPrimary)
    }

    @Test func themesAreEqualWhenTheirChoicesAre() {
        let theme = AppTheme(designStyle: .utility, motionStyle: .calm, colorScheme: .dark, hapticsEnabled: false)
        #expect(theme == AppTheme(designStyle: .utility, motionStyle: .calm, colorScheme: .dark, hapticsEnabled: false))
        #expect(theme != AppTheme(designStyle: .utility, motionStyle: .calm, colorScheme: .light, hapticsEnabled: false))
    }
}
