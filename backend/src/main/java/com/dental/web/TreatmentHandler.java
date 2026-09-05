package com.dental.web;

import com.dental.model.Treatment;
import com.dental.service.AuthService;
import com.dental.service.TreatmentService;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/** {@code /api/treatments} - public read for booking forms, admin-only writes. */
public class TreatmentHandler extends ApiHandler {

    private final TreatmentService service = new TreatmentService();

    public TreatmentHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected void route(HttpExchange exchange) throws Exception {
        requireSession(exchange);
        String[] seg = HttpUtil.pathSegments(exchange, "/api/treatments");

        if (seg.length == 0) {
            if (isMethod(exchange, "GET")) {
                Map<String, String> q = HttpUtil.queryParams(exchange);
                boolean activeOnly = !"false".equalsIgnoreCase(q.get("activeOnly"));
                List<Treatment> list = service.list(activeOnly);
                JSONArray arr = new JSONArray();
                list.forEach(t -> arr.put(t.toJson()));
                ok(exchange, arr);
            } else if (isMethod(exchange, "POST")) {
                requireAdmin(exchange);
                created(exchange, service.create(fromJson(HttpUtil.readJson(exchange))).toJson());
            } else {
                methodNotAllowed(exchange);
            }
            return;
        }

        int id = HttpUtil.intOr(seg[0], 0);
        switch (exchange.getRequestMethod().toUpperCase()) {
            case "GET" -> ok(exchange, service.get(id).toJson());
            case "PUT" -> {
                requireAdmin(exchange);
                ok(exchange, service.update(id, fromJson(HttpUtil.readJson(exchange))).toJson());
            }
            case "DELETE" -> {
                requireAdmin(exchange);
                service.delete(id);
                ok(exchange, message("Treatment deleted"));
            }
            default -> methodNotAllowed(exchange);
        }
    }

    private Treatment fromJson(JSONObject body) {
        Treatment t = new Treatment();
        t.setTreatmentType(HttpUtil.str(body, "treatmentType"));
        t.setBaseCost(HttpUtil.doubleField(body, "baseCost", 0));
        t.setDurationMin((int) HttpUtil.doubleField(body, "durationMin", 30));
        t.setActive(body.optBoolean("active", true));
        return t;
    }
}
