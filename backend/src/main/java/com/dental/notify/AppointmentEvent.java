package com.dental.notify;

import com.dental.model.Appointment;

/** Payload passed to every {@link AppointmentObserver} when something happens to a booking. */
public class AppointmentEvent {

    public enum Type { CREATED, RESCHEDULED, CANCELLED, COMPLETED, REMINDER }

    private final Type type;
    private final Appointment appointment;

    public AppointmentEvent(Type type, Appointment appointment) {
        this.type = type;
        this.appointment = appointment;
    }

    public Type getType() { return type; }

    public Appointment getAppointment() { return appointment; }
}
