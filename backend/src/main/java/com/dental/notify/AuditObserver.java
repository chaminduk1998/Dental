package com.dental.notify;

import com.dental.dao.DAOFactory;
import com.dental.model.AuditLog;

/** Concrete observer that writes every appointment event into the audit trail. */
public class AuditObserver implements AppointmentObserver {

    @Override
    public void onAppointmentEvent(AppointmentEvent event) {
        var a = event.getAppointment();
        if (a == null) {
            return;
        }
        String action = switch (event.getType()) {
            case CREATED -> "CREATE";
            case RESCHEDULED -> "RESCHEDULE";
            case CANCELLED -> "CANCEL";
            case COMPLETED -> "COMPLETE";
            case REMINDER -> "REMIND";
        };
        String details = a.getPatientName() + " with " + a.getDentistName()
                + " on " + a.getApptDate() + " " + a.getApptTime();
        DAOFactory.auditLogs().insert(
                new AuditLog(a.getCreatedBy(), action, "APPOINTMENT", a.getAppointmentNo(), details));
    }
}
