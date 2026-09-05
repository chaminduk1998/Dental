package com.dental.web;

import com.dental.model.User;
import com.dental.service.AuthService;
import com.dental.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

/** {@code /api/users} - admin-only staff account management. */
public class UserHandler extends ApiHandler {

    private final UserService service = new UserService();

    public UserHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected void route(HttpExchange exchange) throws Exception {
        AuthService.Session session = requireAdmin(exchange);
        String[] seg = HttpUtil.pathSegments(exchange, "/api/users");

        if (seg.length == 0) {
            if (isMethod(exchange, "GET")) {
                JSONArray arr = new JSONArray();
                service.list().forEach(u -> arr.put(u.toJson()));
                ok(exchange, arr);
            } else if (isMethod(exchange, "POST")) {
                JSONObject body = HttpUtil.readJson(exchange);
                User u = fromJson(body);
                created(exchange, service.create(u, HttpUtil.str(body, "password")).toJson());
            } else {
                methodNotAllowed(exchange);
            }
            return;
        }

        int id = HttpUtil.intOr(seg[0], 0);
        switch (exchange.getRequestMethod().toUpperCase()) {
            case "GET" -> ok(exchange, service.get(id).toJson());
            case "PUT" -> {
                JSONObject body = HttpUtil.readJson(exchange);
                ok(exchange, service.update(id, fromJson(body), HttpUtil.str(body, "password")).toJson());
            }
            case "DELETE" -> {
                service.delete(id, session.user.getUsername());
                ok(exchange, message("User deleted"));
            }
            default -> methodNotAllowed(exchange);
        }
    }

    private User fromJson(JSONObject body) {
        User u = new User();
        u.setUsername(HttpUtil.str(body, "username"));
        u.setFullName(HttpUtil.str(body, "fullName"));
        u.setRole(HttpUtil.str(body, "role"));
        u.setActive(body.optBoolean("active", true));
        return u;
    }
}
