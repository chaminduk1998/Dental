package com.dental.web;

import com.dental.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

/**
 * {@code /api/auth/login}, {@code /api/auth/logout}, {@code /api/auth/me}.
 */
public class AuthHandler extends ApiHandler {

    public AuthHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected void route(HttpExchange exchange) throws Exception {
        String[] seg = HttpUtil.pathSegments(exchange, "/api/auth");
        String action = seg.length > 0 ? seg[0] : "";

        switch (action) {
            case "login" -> {
                if (!isMethod(exchange, "POST")) { methodNotAllowed(exchange); return; }
                JSONObject body = HttpUtil.readJson(exchange);
                AuthService.Session session = authService.login(
                        HttpUtil.str(body, "username"), HttpUtil.str(body, "password"));
                HttpUtil.setCookie(exchange, HttpUtil.SESSION_COOKIE, session.token, 2 * 60 * 60);
                ok(exchange, new JSONObject().put("user", session.user.toJson()));
            }
            case "logout" -> {
                if (!isMethod(exchange, "POST")) { methodNotAllowed(exchange); return; }
                authService.logout(HttpUtil.cookie(exchange, HttpUtil.SESSION_COOKIE));
                HttpUtil.clearCookie(exchange, HttpUtil.SESSION_COOKIE);
                ok(exchange, message("Signed out"));
            }
            case "me" -> {
                if (!isMethod(exchange, "GET")) { methodNotAllowed(exchange); return; }
                AuthService.Session s = requireSession(exchange);
                ok(exchange, new JSONObject().put("user", s.user.toJson()));
            }
            default -> HttpUtil.sendError(exchange, 404, "Unknown auth endpoint");
        }
    }
}
