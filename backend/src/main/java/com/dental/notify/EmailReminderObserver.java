package com.dental.notify;

import com.dental.config.AppConfig;
import com.dental.dao.DAOFactory;
import com.dental.model.Appointment;
import com.dental.model.Notification;

/**
 * Concrete observer that turns booking events into a confirmation / reminder
 * message. Real SMTP sending is intentionally out of scope for a coursework
 * project with no mail server configured; instead every message is written to
 * the {@code notifications} table (status QUEUED) and the "Reminders" screen
 * lists them - this keeps the Observer pattern fully demonstrable without an
 * external dependency. If {@code mail.enabled=true} and SMTP settings are
 * supplied, {@link #dispatch} can be pointed at a real mail client.
 */
public class EmailReminderObserver implements AppointmentObserver {

    @Override
    public void onAppointmentEvent(AppointmentEvent event) {
        Appointment a = event.getAppointment();
        if (a == null || a.getPatientEmail() == null || a.getPatientEmail().isBlank()) {
            return; // no address on file - nothing to send
        }

        String subject;
        String body;
        switch (event.getType()) {
            case CREATED -> {
                subject = "Appointment Confirmed - " + a.getAppointmentNo();
                body = greet(a) + "Your appointment " + a.getAppointmentNo() + " with " + a.getDentistName()
                        + " for " + a.getTreatmentType() + " is booked on " + a.getApptDate()
                        + " at " + a.getApptTime() + ". " + signOff();
            }
            case RESCHEDULED -> {
                subject = "Appointment Rescheduled - " + a.getAppointmentNo();
                body = greet(a) + "Your appointment " + a.getAppointmentNo() + " has been moved to "
                        + a.getApptDate() + " at " + a.getApptTime() + ". " + signOff();
            }
            case CANCELLED -> {
                subject = "Appointment Cancelled - " + a.getAppointmentNo();
                body = greet(a) + "Your appointment " + a.getAppointmentNo() + " on " + a.getApptDate()
                        + " has been cancelled. Please contact us to rebook. " + signOff();
            }
            case COMPLETED -> {
                subject = "Thank you for your visit - " + a.getAppointmentNo();
                body = greet(a) + "Thank you for visiting us today for " + a.getTreatmentType() + ". " + signOff();
            }
            case REMINDER -> {
                subject = "Appointment Reminder - " + a.getAppointmentNo();
                body = greet(a) + "This is a reminder of your appointment with " + a.getDentistName()
                        + " on " + a.getApptDate() + " at " + a.getApptTime() + ". " + signOff();
            }
            default -> {
                return;
            }
        }

        Notification n = new Notification();
        n.setRecipient(a.getPatientEmail());
        n.setChannel("EMAIL");
        n.setSubject(subject);
        n.setMessage(body);
        n.setStatus(dispatch(a.getPatientEmail(), subject, body) ? "SENT" : "QUEUED");
        DAOFactory.notifications().insert(n);
    }

    private String greet(Appointment a) {
        return "Dear " + (a.getPatientName() == null ? "Patient" : a.getPatientName()) + ", ";
    }

    private String signOff() {
        return "- " + AppConfig.get().get("clinic.name") + ", " + AppConfig.get().get("clinic.phone");
    }

    /**
     * Placeholder send hook. Returns {@code true} (SENT) only when
     * {@code mail.enabled=true}; otherwise the message stays QUEUED and is
     * visible on the Reminders screen, which is sufficient for demoing the
     * Observer pattern without a live SMTP server.
     */
    private boolean dispatch(String to, String subject, String body) {
        return AppConfig.get().getBool("mail.enabled", false);
    }
}
