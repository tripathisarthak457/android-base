package httpapi

import (
	"net/http"
	"net/netip"
	"strings"

	"github.com/base-android/generator-api/internal/config"
)

// clientAddr is the client's address under the configured proxy policy.
func clientAddr(r *http.Request, policy config.ProxyPolicy) netip.Addr {
	if policy.Hops > 0 {
		var hops []string
		for _, header := range r.Header.Values("X-Forwarded-For") {
			for _, hop := range strings.Split(header, ",") {
				hops = append(hops, strings.TrimSpace(hop))
			}
		}
		if len(hops) >= policy.Hops {
			if addr, err := netip.ParseAddr(hops[len(hops)-policy.Hops]); err == nil {
				return addr.Unmap()
			}
		}
		if policy.Vercel {
			if addr, err := netip.ParseAddr(strings.TrimSpace(r.Header.Get("X-Real-Ip"))); err == nil {
				return addr.Unmap()
			}
		}
	}
	if addrPort, err := netip.ParseAddrPort(r.RemoteAddr); err == nil {
		return addrPort.Addr().Unmap()
	}
	addr, _ := netip.ParseAddr(r.RemoteAddr)
	return addr.Unmap()
}

// clientKey is what rate limits and visitor counts are keyed on: an IPv4 address, or the /64 an
// IPv6 address belongs to. One subscriber gets a whole /64, so keying on the full address gives
// every client 2^64 allowances.
func clientKey(addr netip.Addr) string {
	switch {
	case !addr.IsValid():
		return "unknown"
	case addr.Is4():
		return addr.String()
	default:
		prefix, _ := addr.WithZone("").Prefix(64)
		return prefix.String()
	}
}

func (s *Server) clientKey(r *http.Request) string {
	return clientKey(clientAddr(r, s.cfg.TrustedProxy))
}

// siteHost is the host the visitor's browser addressed. Behind Vercel the request can reach this
// service through a rewrite, with the rewrite's destination as its Host and the site's own host in
// X-Forwarded-Host.
func (s *Server) siteHost(r *http.Request) string {
	if s.cfg.TrustedProxy.Vercel {
		forwarded, _, _ := strings.Cut(r.Header.Get("X-Forwarded-Host"), ",")
		if forwarded = strings.TrimSpace(forwarded); forwarded != "" {
			return forwarded
		}
	}
	return r.Host
}
