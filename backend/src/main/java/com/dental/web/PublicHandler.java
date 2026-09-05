package com.dental.web;

import com.dental.model.Appointment;
import com.dental.model.Dentist;
import com.dental.model.Treatment;
import com.dental.service.AppointmentService;
import com.dental.service.AuthService;
import com.dental.service.DentistService;
import com.dental.service.TreatmentService;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * {@code /api/public} - the only endpoints reachable without a staff session.
 * Backs the "Book an Appointment" form on the public home page: a visitor can
 * see the active dentists/treatments and submit a booking request, which is
 * saved as a {@code PENDING} appointment for staff to confirm.
 *
 * <pre>
 * GET  /api/public/dentists
 * GET  /api/public/treatments
 * POST /api/public/appointments
 * </pre>
 */
public class PublicHandler extends ApiHandler {

    private final DentistService dentistService = new DentistService();
    private final TreatmentService treatmentService = new TreatmentService();
    private final AppointmentService appointmentService = new AppointmentService();

    public PublicHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected void route(HttpExchange exchange) throws Exception {
        String[] seg = HttpUtil.pathSegments(exchange, "/api/public");

        if (seg.length == 1 && "dentists".equals(seg[0]) && isMethod(exchange, "GET")) {
            JSONArray arr = new JSONArray();
            for (Dentist d : dentistService.list(true)) {
                arr.put(new JSONObject()
                        .put("id", d.getId())
                        .put("name", d.getName())
                        .put("specialization", d.getSpecialization() == null ? "" : d.getSpecialization())
                        .put("consultationFee", d.getConsultationFee()));
            }
            ok(exchange, arr);
            return;
        }

        if (seg.length == 1 && "treatments".equals(seg[0]) && isMethod(exchange, "GET")) {
            JSONArray arr = new JSONArray();
            for (Treatment t : treatmentService.list(true)) {
                arr.put(new JSONObject()
                        .put("id", t.getId())
                        .put("treatmentType", t.getTreatmentType())
                        .put("baseCost", t.getBaseCost())
                        .put("durationMin", t.getDurationMin()));
            }
            ok(exchange, arr);
            return;
        }

        if (seg.length == 1 && "appointments".equals(seg[0]) && isMethod(exchange, "POST")) {
            JSONObject body = HttpUtil.readJson(exchange);
            String notes = HttpUtil.str(body, "notes");
            String tagged = "Booked online via website."
                    + (notes != null && !notes.isBlank() ? " " + notes.trim() : "");

            Appointment saved = appointmentService.register(
                    HttpUtil.str(body, "patientName"),
                    HttpUtil.str(body, "address"),
                    HttpUtil.str(body, "contactNo"),
                    HttpUtil.str(body, "email"),
                    HttpUtil.intField(body, "dentistId", 0),
                    HttpUtil.intField(body, "treatmentId", 0),
                    HttpUtil.str(body, "date"),
                    HttpUtil.str(body, "time"),
                    tagged,
                    null);

            created(exchange, new JSONObject()
                    .put("appointmentNo", saved.getAppointmentNo())
                    .put("dentistName", saved.getDentistName())
                    .put("treatmentType", saved.getTreatmentType())
                    .put("date", saved.getApptDate())
                    .put("time", saved.getApptTime())
                    .put("status", "success"));
            return;
        }

        HttpUtil.sendError(exchange, 404, "Unknown endpoint");
    }
}
