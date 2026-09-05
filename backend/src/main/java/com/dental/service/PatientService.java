package com.dental.service;

import com.dental.dao.AppointmentDAO;
import com.dental.dao.DAOFactory;
import com.dental.dao.PatientDAO;
import com.dental.model.Appointment;
import com.dental.model.Patient;
import com.dental.util.ValidationException;
import com.dental.util.Validator;

import java.util.List;
import java.util.Optional;

/** Business logic for managing patients and their treatment history. */
public class PatientService {

    private final PatientDAO patientDAO = DAOFactory.patients();
    private final AppointmentDAO appointmentDAO = DAOFactory.appointments();

    public List<Patient> list(String term) {
        return patientDAO.search(term);
    }

    public Patient get(int id) {
        return patientDAO.findById(id)
                .orElseThrow(() -> new ValidationException("Patient not found"));
    }

    public List<Appointment> history(int patientId) {
        get(patientId); // 404 if missing
        return appointmentDAO.findByPatient(patientId);
    }

    public Patient create(Patient p) {
        validate(p);
        p.setId(patientDAO.insert(p));
        return p;
    }

    public Patient update(int id, Patient p) {
        get(id);
        validate(p);
        p.setId(id);
        patientDAO.update(p);
        return p;
    }

    public void delete(int id) {
        get(id);
        patientDAO.delete(id);
    }

    /** Finds a matching existing patient or creates a new one - used by AppointmentService. */
    public Patient findOrCreate(String name, String address, String contactNo, String email) {
        Patient candidate = new Patient();
        candidate.setName(Validator.required(name, "Patient name"));
        candidate.setAddress(address);
        candidate.setContactNo(Validator.phone(contactNo));
        candidate.setEmail(Validator.optionalEmail(email));

        Optional<Patient> existing = patientDAO.findByNameAndContact(candidate.getName(), candidate.getContactNo());
        if (existing.isPresent()) {
            Patient p = existing.get();
            // keep contact details fresh
            p.setAddress(address == null || address.isBlank() ? p.getAddress() : address);
            p.setEmail(candidate.getEmail() == null ? p.getEmail() : candidate.getEmail());
            patientDAO.update(p);
            return p;
        }
        candidate.setId(patientDAO.insert(candidate));
        return candidate;
    }

    private void validate(Patient p) {
        p.setName(Validator.required(p.getName(), "Patient name"));
        p.setContactNo(Validator.phone(p.getContactNo()));
        p.setEmail(Validator.optionalEmail(p.getEmail()));
    }
}
