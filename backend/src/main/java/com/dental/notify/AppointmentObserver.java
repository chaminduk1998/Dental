package com.dental.notify;

/**
 * <b>Observer pattern.</b>
 *
 * <p>Implemented by anything that must react when an appointment changes state
 * (a new booking, a reschedule, a cancellation, a reminder). The booking
 * service does not know or care who is listening - it just calls
 * {@link AppointmentNotifier#publish}.</p>
 */
public interface AppointmentObserver {

    void onAppointmentEvent(AppointmentEvent event);
}
