package com.dental.service;

import com.dental.dao.DAOFactory;
import com.dental.dao.DentistDAO;
import com.dental.model.Dentist;
import com.dental.util.ValidationException;
import com.dental.util.Validator;

import java.util.List;

/** Business logic for managing dentists (admin panel). */
public class DentistService {

    private final DentistDAO dao = DAOFactory.dentists();

    public List<Dentist> list(boolean activeOnly) {
        return activeOnly ? dao.findActive() : dao.findAll();
    }

    public Dentist get(int id) {
        return dao.findById(id).orElseThrow(() -> new ValidationException("Dentist not found"));
    }

    public Dentist create(Dentist d) {
        validate(d);
        d.setId(dao.insert(d));
        return d;
    }

    public Dentist update(int id, Dentist d) {
        get(id);
        validate(d);
        d.setId(id);
        dao.update(d);
        return d;
    }

    public void delete(int id) {
        get(id);
        dao.delete(id);
    }

    private void validate(Dentist d) {
        d.setName(Validator.required(d.getName(), "Dentist name"));
        d.setConsultationFee(Validator.notNegative(d.getConsultationFee(), "Consultation fee"));
    }
}
