package com.dental.service;

import com.dental.dao.DAOFactory;
import com.dental.dao.TreatmentDAO;
import com.dental.model.Treatment;
import com.dental.util.ValidationException;
import com.dental.util.Validator;

import java.util.List;

/** Business logic for managing treatment types and their base cost (admin panel). */
public class TreatmentService {

    private final TreatmentDAO dao = DAOFactory.treatments();

    public List<Treatment> list(boolean activeOnly) {
        return activeOnly ? dao.findActive() : dao.findAll();
    }

    public Treatment get(int id) {
        return dao.findById(id).orElseThrow(() -> new ValidationException("Treatment not found"));
    }

    public Treatment create(Treatment t) {
        validate(t);
        t.setId(dao.insert(t));
        return t;
    }

    public Treatment update(int id, Treatment t) {
        get(id);
        validate(t);
        t.setId(id);
        dao.update(t);
        return t;
    }

    public void delete(int id) {
        get(id);
        dao.delete(id);
    }

    private void validate(Treatment t) {
        t.setTreatmentType(Validator.required(t.getTreatmentType(), "Treatment type"));
        t.setBaseCost(Validator.notNegative(t.getBaseCost(), "Base cost"));
        if (t.getDurationMin() <= 0) {
            t.setDurationMin(30);
        }
    }
}
