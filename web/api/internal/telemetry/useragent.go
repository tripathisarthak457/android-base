package telemetry

import (
	"regexp"
	"strings"
)

// Agent is a user agent reduced to three short enums. The raw string is never stored: with the
// version numbers and build ids it carries, it narrows a visitor down far more than counting
// browsers needs.
type Agent struct {
	Browser     string
	OS          string
	DeviceClass string
}

// Device classes. "unknown" rather than a guess for agents that name no platform at all.
const (
	DeviceMobile  = "mobile"
	DeviceTablet  = "tablet"
	DeviceDesktop = "desktop"
	DeviceUnknown = "unknown"
)

var (
	Browsers      = []string{"chrome", "safari", "firefox", "edge", "opera", "samsung", "other"}
	OSFamilies    = []string{"android", "ios", "windows", "macos", "chromeos", "linux", "other"}
	DeviceClasses = []string{DeviceMobile, DeviceTablet, DeviceDesktop, DeviceUnknown}
)

// botPattern matches crawlers, link unfurlers, uptime monitors and headless browsers. curl, wget and
// HTTP libraries are not bots: they are how people script the generator, which is the cli client.
var botPattern = regexp.MustCompile(`(?i)bot\b|bot/|crawl|spider|slurp|facebookexternalhit|embedly|` +
	`headlesschrome|lighthouse|pagespeed|pingdom|uptime|monitor|phantomjs|selenium|puppeteer|playwright|preview`)

// IsBot reports whether a user agent belongs to a crawler or an automated browser.
func IsBot(userAgent string) bool {
	return userAgent != "" && botPattern.MatchString(userAgent)
}

// ClassifyAgent reduces a user agent to browser family, OS family and device class. secCHUAMobile is
// the `Sec-CH-UA-Mobile` client hint ("?1" or "?0"); when a Chromium browser sends it, it is more
// reliable than the user agent, which Chrome has frozen to a generic value.
func ClassifyAgent(userAgent, secCHUAMobile string) Agent {
	agent := Agent{Browser: browserFamily(userAgent), OS: osFamily(userAgent)}
	fromAgent := deviceClass(userAgent, agent.OS)

	switch {
	case secCHUAMobile == "?1":
		agent.DeviceClass = DeviceMobile
	// The hint has no tablet value: an Android tablet in Chrome sends "?0".
	case secCHUAMobile == "?0" && fromAgent != DeviceTablet:
		agent.DeviceClass = DeviceDesktop
	default:
		agent.DeviceClass = fromAgent
	}
	return agent
}

func browserFamily(ua string) string {
	switch {
	case strings.Contains(ua, "Edg/") || strings.Contains(ua, "EdgA/") || strings.Contains(ua, "EdgiOS/"):
		return "edge"
	case strings.Contains(ua, "OPR/") || strings.Contains(ua, "Opera"):
		return "opera"
	case strings.Contains(ua, "SamsungBrowser/"):
		return "samsung"
	case strings.Contains(ua, "Firefox/") || strings.Contains(ua, "FxiOS/"):
		return "firefox"
	case strings.Contains(ua, "Chrome/") || strings.Contains(ua, "CriOS/") || strings.Contains(ua, "Chromium/"):
		return "chrome"
	case strings.Contains(ua, "Safari/") && strings.Contains(ua, "Version/"):
		return "safari"
	default:
		return "other"
	}
}

func osFamily(ua string) string {
	switch {
	case strings.Contains(ua, "Android"):
		return "android"
	case strings.Contains(ua, "iPhone") || strings.Contains(ua, "iPad") || strings.Contains(ua, "iPod"):
		return "ios"
	case strings.Contains(ua, "Windows"):
		return "windows"
	case strings.Contains(ua, "CrOS"):
		return "chromeos"
	case strings.Contains(ua, "Macintosh") || strings.Contains(ua, "Mac OS X"):
		return "macos"
	case strings.Contains(ua, "Linux") || strings.Contains(ua, "X11"):
		return "linux"
	default:
		return "other"
	}
}

func deviceClass(ua, os string) string {
	switch {
	case strings.Contains(ua, "iPad") || strings.Contains(ua, "Tablet"):
		return DeviceTablet
	// Android phones put "Mobile" in the user agent; Android tablets leave it out.
	case os == "android" && !strings.Contains(ua, "Mobile"):
		return DeviceTablet
	case strings.Contains(ua, "Mobi") || os == "ios" || os == "android":
		return DeviceMobile
	case os == "windows" || os == "macos" || os == "linux" || os == "chromeos":
		return DeviceDesktop
	default:
		return DeviceUnknown
	}
}
