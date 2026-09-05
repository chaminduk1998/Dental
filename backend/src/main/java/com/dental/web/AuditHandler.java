package com.dental.web;

import com.dental.service.AuditService;
import com.dental.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;

import java.util.Map;

/** {@code /api/audit} - admin-only "who did what" trail. */
public class AuditHandler extends ApiHandler {

    private final AuditService service = new AuditService();

    public AuditHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected void route(HttpExchange exchange) throws Exception {
        requireAdmin(exchange);
        if (!isMethod(exchange, "GET")) {
            methodNotAllowed(exchange);
            return;
        }
        Map<String, String> q = HttpUtil.queryParams(exchange);
        JSONArray arr = new JSONArray();
        service.recent(HttpUtil.intOr(q.get("limit"), 200), q.get("entity")).forEach(a -> arr.put(a.toJson()));
        ok(exchange, arr);
    }
}
