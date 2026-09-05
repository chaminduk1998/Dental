package com.dental.web;

import com.dental.model.Appointment;
import com.dental.service.AppointmentService;
import com.dental.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * {@code /api/appointments}
 *
 * <pre>
 * GET    /api/appointments?q=&status=&from=&to=&dentistId=      search / list
 * GET    /api/appointments/{id}                                 by numeric id
 * GET    /api/appointments/lookup/{appointmentNo}                "Display Appointment Details"
 * POST   /api/appointments                                      "Register New Appointment"
 * PUT    /api/appointments/{id}                                  reschedule / edit
 * PUT    /api/appointments/{id}/status  { "status": "..." }       change status
 * POST   /api/appointments/{id}/remind                           send reminder (Observer demo)
 * DELETE /api/appointments/{id}                                  cancel
 * </pre>
 */
public class AppointmentHandler extends ApiHandler {

    private final AppointmentService service = new AppointmentService();

    public AppointmentHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected void route(HttpExchange exchange) throws Exception {
        AuthService.Session session = requireSession(exchange);
        String[] seg = HttpUtil.pathSegments(exchange, "/api/appointments");

        if (seg.length == 0) {
            if (isMethod(exchange, "GET")) {
                Map<String, String> q = HttpUtil.queryParams(exchange);
                List<Appointment> list = service.search(
                        q.get("q"), q.get("status"), q.get("from"), q.get("to"),
                        HttpUtil.intOr(q.get("dentistId"), 0), HttpUtil.intOr(q.get("limit"), 300));
                JSONArray arr = new JSONArray();
                list.forEach(a -> arr.put(a.toJson()));
                ok(exchange, arr);
            } else if (isMethod(exchange, "POST")) {
                JSONObject body = HttpUtil.readJson(exchange);
                Appointment saved = service.register(
                        HttpUtil.str(body, "patientName"),
                        HttpUtil.str(body, "address"),
                        HttpUtil.str(body, "contactNo"),
                        HttpUtil.str(body, "email"),
                        HttpUtil.intField(body, "dentistId", 0),
                        HttpUtil.intField(body, "treatmentId", 0),
                        HttpUtil.str(body, "date"),
                        HttpUtil.str(body, "time"),
                        HttpUtil.str(body, "notes"),
                        session.user.getUsername());
                created(exchange, new JSONObject(saved.toJson().toMap())
                        .put("appointmentNo", saved.getAppointmentNo())
                        .put("status", "success"));
            } else {
                methodNotAllowed(exchange);
            }
            return;
        }

        if (seg.length == 2 && "lookup".equals(seg[0]) && isMethod(exchange, "GET")) {
            ok(exchange, service.getByNo(seg[1]).toJson());
            return;
        }

        int id = HttpUtil.intOr(seg[0], 0);

        if (seg.length == 1) {
            switch (exchange.getRequestMethod().toUpperCase()) {
                case "GET" -> ok(exchange, service.getById(id).toJson());
                case "PUT" -> {
                    JSONObject body = HttpUtil.readJson(exchange);
                    ok(exchange, service.reschedule(id,
                            HttpUtil.intField(body, "dentistId", 0),
                            HttpUtil.intField(body, "treatmentId", 0),
                            HttpUtil.str(body, "date"),
                            HttpUtil.str(body, "time"),
                            HttpUtil.str(body, "notes")).toJson());
                }
                case "DELETE" -> ok(exchange, service.cancel(id).toJson());
                default -> methodNotAllowed(exchange);
            }
            return;
        }

        if (seg.length == 2 && "status".equals(seg[1]) && isMethod(exchange, "PUT")) {
            JSONObject body = HttpUtil.readJson(exchange);
            ok(exchange, service.changeStatus(id, HttpUtil.str(body, "status")).toJson());
            return;
        }

        if (seg.length == 2 && "remind".equals(seg[1]) && isMethod(exchange, "POST")) {
            service.sendReminder(id);
            ok(exchange, message("Reminder queued"));
            return;
        }

        HttpUtil.sendError(exchange, 404, "Unknown appointment endpoint");
    }
}
