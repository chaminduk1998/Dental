package com.dental.web;

import com.dental.model.Bill;
import com.dental.pricing.PricingStrategy;
import com.dental.service.AuthService;
import com.dental.service.BillingService;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * {@code /api/bills}
 *
 * <pre>
 * GET  /api/bills                          recent bills
 * GET  /api/bills/strategies                available pricing strategies (Strategy pattern)
 * GET  /api/bills/by-appointment/{no}       receipt for an appointment reference
 * GET  /api/bills/{billNo}                  receipt by bill number
 * POST /api/bills/preview   { appointmentId, pricingCode }   live total preview
 * POST /api/bills           { appointmentId, pricingCode, paymentMethod }   issue + print
 * </pre>
 */
public class BillingHandler extends ApiHandler {

    private final BillingService service = new BillingService();

    public BillingHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected void route(HttpExchange exchange) throws Exception {
        AuthService.Session session = requireSession(exchange);
        String[] seg = HttpUtil.pathSegments(exchange, "/api/bills");

        if (seg.length == 0) {
            if (isMethod(exchange, "GET")) {
                JSONArray arr = new JSONArray();
                service.recent().forEach(b -> arr.put(b.toJson()));
                ok(exchange, arr);
            } else if (isMethod(exchange, "POST")) {
                JSONObject body = HttpUtil.readJson(exchange);
                Bill bill = service.issue(
                        HttpUtil.intField(body, "appointmentId", 0),
                        HttpUtil.str(body, "pricingCode"),
                        HttpUtil.str(body, "paymentMethod"),
                        session.user.getUsername());
                created(exchange, bill.toJson());
            } else {
                methodNotAllowed(exchange);
            }
            return;
        }

        if (seg.length == 1 && "strategies".equals(seg[0]) && isMethod(exchange, "GET")) {
            JSONArray arr = new JSONArray();
            for (PricingStrategy s : service.pricingOptions()) {
                arr.put(new JSONObject()
                        .put("code", s.code())
                        .put("label", s.label())
                        .put("description", s.description()));
            }
            ok(exchange, arr);
            return;
        }

        if (seg.length == 1 && "preview".equals(seg[0]) && isMethod(exchange, "POST")) {
            JSONObject body = HttpUtil.readJson(exchange);
            Bill preview = service.preview(
                    HttpUtil.intField(body, "appointmentId", 0), HttpUtil.str(body, "pricingCode"));
            ok(exchange, preview.toJson());
            return;
        }

        if (seg.length == 2 && "by-appointment".equals(seg[0]) && isMethod(exchange, "GET")) {
            ok(exchange, service.getByAppointmentNo(seg[1]).toJson());
            return;
        }

        if (seg.length == 1 && isMethod(exchange, "GET")) {
            ok(exchange, service.getByBillNo(seg[0]).toJson());
            return;
        }

        HttpUtil.sendError(exchange, 404, "Unknown billing endpoint");
    }
}
