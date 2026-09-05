package com.dental.web;

import com.dental.service.AuthService;
import com.dental.service.NotificationService;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;

/** {@code /api/notifications} - the queued reminder/confirmation messages (Observer pattern output). */
public class NotificationHandler extends ApiHandler {

    private final NotificationService service = new NotificationService();

    public NotificationHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected void route(HttpExchange exchange) throws Exception {
        requireSession(exchange);
        if (!isMethod(exchange, "GET")) {
            methodNotAllowed(exchange);
            return;
        }
        JSONArray arr = new JSONArray();
        service.recent().forEach(n -> arr.put(n.toJson()));
        ok(exchange, arr);
    }
}
