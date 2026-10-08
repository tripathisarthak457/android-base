package telemetry

import "testing"

func TestClassifyAgent(t *testing.T) {
	cases := []struct {
		name, ua, mobileHint string
		want                 Agent
	}{
		{"Chrome on Android", "Mozilla/5.0 (Linux; Android 14; Redmi Note 10 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36", "",
			Agent{"chrome", "android", DeviceMobile}},
		{"Chrome on an Android tablet", "Mozilla/5.0 (Linux; Android 14; SM-X710) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36", "?0",
			Agent{"chrome", "android", DeviceTablet}},
		{"Safari on iPhone", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1", "",
			Agent{"safari", "ios", DeviceMobile}},
		{"Safari on iPad", "Mozilla/5.0 (iPad; CPU OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1", "",
			Agent{"safari", "ios", DeviceTablet}},
		{"Chrome on iOS", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/141.0.0.0 Mobile/15E148 Safari/604.1", "",
			Agent{"chrome", "ios", DeviceMobile}},
		{"Safari on a Mac", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15", "",
			Agent{"safari", "macos", DeviceDesktop}},
		{"Edge on Windows", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0", "?0",
			Agent{"edge", "windows", DeviceDesktop}},
		{"Firefox on Linux", "Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0", "",
			Agent{"firefox", "linux", DeviceDesktop}},
		{"Opera on Windows", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 OPR/115.0.0.0", "",
			Agent{"opera", "windows", DeviceDesktop}},
		{"Samsung Internet", "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/26.0 Chrome/122.0.0.0 Mobile Safari/537.36", "",
			Agent{"samsung", "android", DeviceMobile}},
		{"Chrome on a Chromebook", "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36", "",
			Agent{"chrome", "chromeos", DeviceDesktop}},
		// Chrome's reduced user agent says Android and "K", but the hint still says mobile.
		{"reduced UA with a mobile hint", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36", "?1",
			Agent{"chrome", "android", DeviceMobile}},
		{"curl", "curl/8.5.0", "", Agent{"other", "other", DeviceUnknown}},
		{"nothing", "", "", Agent{"other", "other", DeviceUnknown}},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			if got := ClassifyAgent(c.ua, c.mobileHint); got != c.want {
				t.Fatalf("got %+v, want %+v", got, c.want)
			}
		})
	}
}

func TestIsBot(t *testing.T) {
	bots := []string{
		"Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
		"Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)",
		"facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
		"Slackbot-LinkExpanding 1.0 (+https://api.slack.com/robots)",
		"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/141.0.0.0 Safari/537.36",
		"Mozilla/5.0 (Linux; Android 11; moto g power (2022)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36 Chrome-Lighthouse",
		"Mozilla/5.0 (compatible; UptimeRobot/2.0; http://www.uptimerobot.com/)",
	}
	for _, ua := range bots {
		if !IsBot(ua) {
			t.Errorf("not a bot: %s", ua)
		}
	}
	people := []string{
		"Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1",
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36",
		"curl/8.5.0",
		"python-requests/2.32.3",
		"",
	}
	for _, ua := range people {
		if IsBot(ua) {
			t.Errorf("not a person or a script: %s", ua)
		}
	}
}
