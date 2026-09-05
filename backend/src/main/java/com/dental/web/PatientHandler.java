package com.dental.web;

import com.dental.model.Appointment;
import com.dental.model.Patient;
import com.dental.service.AuthService;
import com.dental.service.PatientService;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/** {@code /api/patients} - list/search, get, create, update, delete, history. */
public class PatientHandler extends ApiHandler {

    private final PatientService service = new PatientService();

    public PatientHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected void route(HttpExchange exchange) throws Exception {
        requireSession(exchange);
        String[] seg = HttpUtil.pathSegments(exchange, "/api/patients");

        if (seg.length == 0) {
            if (isMethod(exchange, "GET")) {
                Map<String, String> q = HttpUtil.queryParams(exchange);
                List<Patient> list = service.list(q.get("q"));
                JSONArray arr = new JSONArray();
                list.forEach(p -> arr.put(p.toJson()));
                ok(exchange, arr);
            } else if (isMethod(exchange, "POST")) {
                JSONObject body = HttpUtil.readJson(exchange);
                Patient p = fromJson(body);
                created(exchange, service.create(p).toJson());
            } else {
                methodNotAllowed(exchange);
            }
            return;
        }

        int id = HttpUtil.intOr(seg[0], 0);
        if (seg.length == 1) {
            switch (exchange.getRequestMethod().toUpperCase()) {
                case "GET" -> ok(exchange, service.get(id).toJson());
                case "PUT" -> {
                    JSONObject body = HttpUtil.readJson(exchange);
                    ok(exchange, service.update(id, fromJson(body)).toJson());
                }
                case "DELETE" -> {
                    service.delete(id);
                    ok(exchange, message("Patient deleted"));
                }
                default -> methodNotAllowed(exchange);
            }
            return;
        }

        if (seg.length == 2 && "history".equals(seg[1]) && isMethod(exchange, "GET")) {
            JSONArray arr = new JSONArray();
            List<Appointment> hist = service.history(id);
            hist.forEach(a -> arr.put(a.toJson()));
            ok(exchange, arr);
            return;
        }

        HttpUtil.sendError(exchange, 404, "Unknown patient endpoint");
    }

    private Patient fromJson(JSONObject body) {
        Patient p = new Patient();
        p.setName(HttpUtil.str(body, "name"));
        p.setAddress(HttpUtil.str(body, "address"));
        p.setContactNo(HttpUtil.str(body, "contactNo"));
        p.setEmail(HttpUtil.str(body, "email"));
        return p;
    }
}
