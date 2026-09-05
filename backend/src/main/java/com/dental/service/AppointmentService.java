package com.dental.service;

import com.dental.dao.AppointmentDAO;
import com.dental.dao.DAOFactory;
import com.dental.dao.DentistDAO;
import com.dental.dao.TreatmentDAO;
import com.dental.model.Appointment;
import com.dental.model.Patient;
import com.dental.notify.AppointmentEvent;
import com.dental.notify.AppointmentNotifier;
import com.dental.util.ValidationException;
import com.dental.util.Validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Business logic for registering, searching, rescheduling and cancelling
 * appointments. This is the class the "Register New Appointment" and
 * "Display Appointment Details" screens ultimately call into.
 */
public class AppointmentService {

    private final AppointmentDAO appointmentDAO = DAOFactory.appointments();
    private final DentistDAO dentistDAO = DAOFactory.dentists();
    private final TreatmentDAO treatmentDAO = DAOFactory.treatments();
    private final PatientService patientService = new PatientService();

    public List<Appointment> search(String term, String status, String from, String to, int dentistId, int limit) {
        return appointmentDAO.search(term, status, from, to, dentistId, limit <= 0 ? 300 : limit);
    }

    public Appointment getByNo(String appointmentNo) {
        String no = Validator.required(appointmentNo, "Appointment number");
        return appointmentDAO.findByAppointmentNo(no)
                .orElseThrow(() -> new ValidationException("No appointment found with reference " + no));
    }

    public Appointment getById(int id) {
        return appointmentDAO.findById(id)
                .orElseThrow(() -> new ValidationException("Appointment not found"));
    }

    /**
     * Registers a new appointment: validates the input, finds-or-creates the
     * patient, checks the dentist/treatment exist and the slot is free, then
     * saves the booking and publishes a CREATED event to every observer
     * (email confirmation + audit log).
     */
    public Appointment register(String patientName, String address, String contactNo, String email,
                                 int dentistId, int treatmentId, String date, String time,
                                 String notes, String createdBy) {

        Validator.positive(dentistId, "Dentist");
        Validator.positive(treatmentId, "Treatment");
        LocalDate apptDate = Validator.date(date, "Appointment date");
        LocalTime apptTime = Validator.time(time, "Appointment time");

        if (apptDate.isBefore(LocalDate.now())) {
            throw new ValidationException("Appointment date cannot be in the past");
        }

        dentistDAO.findById(dentistId).orElseThrow(() -> new ValidationException("Selected dentist does not exist"));
        treatmentDAO.findById(treatmentId).orElseThrow(() -> new ValidationException("Selected treatment does not exist"));

        if (appointmentDAO.isSlotTaken(dentistId, apptDate.toString(), pad(apptTime), 0)) {
            throw new ValidationException("This dentist already has an appointment at that date and time");
        }

        Patient patient = patientService.findOrCreate(patientName, address, contactNo, email);

        Appointment a = new Appointment();
        a.setAppointmentNo(appointmentDAO.nextAppointmentNo());
        a.setPatientId(patient.getId());
        a.setDentistId(dentistId);
        a.setTreatmentId(treatmentId);
        a.setApptDate(apptDate.toString());
        a.setApptTime(pad(apptTime));
        a.setStatus(Appointment.PENDING);
        a.setNotes(notes);
        a.setCreatedBy(createdBy);

        int id = appointmentDAO.insert(a);
        Appointment saved = appointmentDAO.findById(id)
                .orElseThrow(() -> new ValidationException("Appointment could not be reloaded after saving"));

        AppointmentNotifier.get().publish(new AppointmentEvent(AppointmentEvent.Type.CREATED, saved));
        return saved;
    }

    /** Reschedules / edits an existing appointment (dentist, treatment, date, time, notes). */
    public Appointment reschedule(int id, int dentistId, int treatmentId, String date, String time, String notes) {
        Appointment existing = getById(id);
        if (Appointment.CANCELLED.equals(existing.getStatus()) || Appointment.COMPLETED.equals(existing.getStatus())) {
            throw new ValidationException("A " + existing.getStatus().toLowerCase() + " appointment cannot be edited");
        }

        Validator.positive(dentistId, "Dentist");
        Validator.positive(treatmentId, "Treatment");
        LocalDate apptDate = Validator.date(date, "Appointment date");
        LocalTime apptTime = Validator.time(time, "Appointment time");

        dentistDAO.findById(dentistId).orElseThrow(() -> new ValidationException("Selected dentist does not exist"));
        treatmentDAO.findById(treatmentId).orElseThrow(() -> new ValidationException("Selected treatment does not exist"));

        if (appointmentDAO.isSlotTaken(dentistId, apptDate.toString(), pad(apptTime), id)) {
            throw new ValidationException("This dentist already has an appointment at that date and time");
        }

        existing.setDentistId(dentistId);
        existing.setTreatmentId(treatmentId);
        existing.setApptDate(apptDate.toString());
        existing.setApptTime(pad(apptTime));
        existing.setNotes(notes);
        if (Appointment.PENDING.equals(existing.getStatus())) {
            existing.setStatus(Appointment.CONFIRMED);
        }
        appointmentDAO.update(existing);

        Appointment saved = getById(id);
        AppointmentNotifier.get().publish(new AppointmentEvent(AppointmentEvent.Type.RESCHEDULED, saved));
        return saved;
    }

    public Appointment changeStatus(int id, String status) {
        Appointment existing = getById(id);
        String target = Validator.required(status, "Status").toUpperCase();
        if (!List.of(Appointment.PENDING, Appointment.CONFIRMED, Appointment.COMPLETED, Appointment.CANCELLED)
                .contains(target)) {
            throw new ValidationException("Unknown status: " + status);
        }
        if (Appointment.CANCELLED.equals(existing.getStatus())) {
            throw new ValidationException("A cancelled appointment cannot change status");
        }
        appointmentDAO.updateStatus(id, target);
        Appointment saved = getById(id);

        AppointmentEvent.Type type = switch (target) {
            case Appointment.CANCELLED -> AppointmentEvent.Type.CANCELLED;
            case Appointment.COMPLETED -> AppointmentEvent.Type.COMPLETED;
            default -> null;
        };
        if (type != null) {
            AppointmentNotifier.get().publish(new AppointmentEvent(type, saved));
        }
        return saved;
    }

    public Appointment cancel(int id) {
        return changeStatus(id, Appointment.CANCELLED);
    }

    public void sendReminder(int id) {
        Appointment a = getById(id);
        if (Appointment.CANCELLED.equals(a.getStatus()) || Appointment.COMPLETED.equals(a.getStatus())) {
            throw new ValidationException("Cannot send a reminder for a " + a.getStatus().toLowerCase() + " appointment");
        }
        AppointmentNotifier.get().publish(new AppointmentEvent(AppointmentEvent.Type.REMINDER, a));
    }

    private String pad(LocalTime t) {
        return t.toString().length() == 5 ? t + ":00" : t.toString();
    }
}
