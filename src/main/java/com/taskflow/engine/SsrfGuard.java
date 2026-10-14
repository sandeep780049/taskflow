package com.taskflow.engine;

import com.taskflow.common.ApiException;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Guards the HTTP_REQUEST handler against SSRF: only http(s) URLs, ports
 * >=1024 by default, and every resolved address must be public (no loopback,
 * link-local including cloud metadata endpoints, site-local, or multicast).
 */
public final class SsrfGuard {

    private static final List<String> ALLOWED_SCHEMES = List.of("http", "https");

    private SsrfGuard() {
    }

    public static URI validate(String rawUrl) {
        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (Exception e) {
            throw ApiException.badRequest("payload.url is not a valid URL");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw ApiException.badRequest("payload.url scheme must be http or https");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw ApiException.badRequest("payload.url must contain a host");
        }
        if (uri.getUserInfo() != null) {
            throw ApiException.badRequest("payload.url must not embed credentials");
        }
        int port = uri.getPort();
        if (port != -1 && port < 1024) {
            throw ApiException.badRequest("payload.url port must be >= 1024");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                requirePublic(address);
            }
        } catch (UnknownHostException e) {
            throw ApiException.badRequest("payload.url host is not resolvable: " + host);
        }
        return uri;
    }

    private static void requirePublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            throw ApiException.badRequest("payload.url host resolves to a private/reserved address (SSRF blocked)");
        }
    }
}
