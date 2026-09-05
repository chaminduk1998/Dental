package com.dental.web;

import com.dental.model.Dentist;
import com.dental.service.AuthService;
import com.dental.service.DentistService;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/** {@code /api/dentists} - public read for booking forms, admin-only writes. */
public class DentistHandler extends ApiHandler {

    private final DentistService service = new DentistService();

    public DentistHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected void route(HttpExchange exchange) throws Exception {
        requireSession(exchange);
        String[] seg = HttpUtil.pathSegments(exchange, "/api/dentists");

        if (seg.length == 0) {
            if (isMethod(exchange, "GET")) {
                Map<String, String> q = HttpUtil.queryParams(exchange);
                boolean activeOnly = !"false".equalsIgnoreCase(q.get("activeOnly"));
                List<Dentist> list = service.list(activeOnly);
                JSONArray arr = new JSONArray();
                list.forEach(d -> arr.put(d.toJson()));
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
                ok(exchange, message("Dentist deleted"));
            }
            default -> methodNotAllowed(exchange);
        }
    }

    private Dentist fromJson(JSONObject body) {
        Dentist d = new Dentist();
        d.setName(HttpUtil.str(body, "name"));
        d.setSpecialization(HttpUtil.str(body, "specialization"));
        d.setConsultationFee(HttpUtil.doubleField(body, "consultationFee", 0));
        d.setActive(body.optBoolean("active", true));
        return d;
    }
}
