package com.dental.web;

import com.dental.service.AuthService;
import com.dental.service.ReportService;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * {@code /api/reports}
 *
 * <pre>
 * GET /api/reports/summary                    dashboard KPI cards
 * GET /api/reports/appointments-trend?days=    appointments/day line chart
 * GET /api/reports/revenue-trend?days=         revenue/day line chart
 * GET /api/reports/top-treatments?limit=       most common treatments
 * GET /api/reports/dentist-workload            appointments+revenue per dentist
 * GET /api/reports/status-breakdown            appointment status donut
 * GET /api/reports/top-patients?limit=         most frequent patients
 * GET /api/reports/revenue?from=&to=           revenue report table
 * </pre>
 */
public class ReportHandler extends ApiHandler {

    private final ReportService service = new ReportService();

    public ReportHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected void route(HttpExchange exchange) throws Exception {
        requireSession(exchange);
        if (!isMethod(exchange, "GET")) {
            methodNotAllowed(exchange);
            return;
        }
        String[] seg = HttpUtil.pathSegments(exchange, "/api/reports");
        Map<String, String> q = HttpUtil.queryParams(exchange);
        String action = seg.length > 0 ? seg[0] : "";

        switch (action) {
            case "summary" -> ok(exchange, new JSONObject(service.dashboardSummary()));
            case "appointments-trend" -> ok(exchange, toArray(service.appointmentsTrend(HttpUtil.intOr(q.get("days"), 14))));
            case "revenue-trend" -> ok(exchange, toArray(service.revenueTrend(HttpUtil.intOr(q.get("days"), 14))));
            case "top-treatments" -> ok(exchange, toArray(service.topTreatments(HttpUtil.intOr(q.get("limit"), 8))));
            case "dentist-workload" -> ok(exchange, toArray(service.dentistWorkload()));
            case "status-breakdown" -> ok(exchange, toArray(service.statusBreakdown()));
            case "top-patients" -> ok(exchange, toArray(service.topPatients(HttpUtil.intOr(q.get("limit"), 8))));
            case "revenue" -> ok(exchange, toArray(service.revenueReport(q.get("from"), q.get("to"))));
            default -> HttpUtil.sendError(exchange, 404, "Unknown report endpoint");
        }
    }

    private JSONArray toArray(List<Map<String, Object>> rows) {
        JSONArray arr = new JSONArray();
        rows.forEach(r -> arr.put(new JSONObject(r)));
        return arr;
    }
}
