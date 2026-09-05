package com.dental.notify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <b>Observer pattern - the subject.</b>
 *
 * <p>Keeps the list of observers and fans an {@link AppointmentEvent} out to all
 * of them. A single Singleton instance is shared by the whole application so any
 * service can subscribe or publish through it.</p>
 */
public final class AppointmentNotifier {

    private static final AppointmentNotifier INSTANCE = new AppointmentNotifier();

    private final List<AppointmentObserver> observers = new CopyOnWriteArrayList<>();

    private AppointmentNotifier() { }

    public static AppointmentNotifier get() {
        return INSTANCE;
    }

    public void subscribe(AppointmentObserver observer) {
        observers.add(observer);
    }

    public void unsubscribe(AppointmentObserver observer) {
        observers.remove(observer);
    }

    public void publish(AppointmentEvent event) {
        // isolate each observer so a failure in one (e.g. SMTP down) never
        // stops the others (e.g. the audit trail) from running
        List<RuntimeException> failures = new ArrayList<>();
        for (AppointmentObserver o : observers) {
            try {
                o.onAppointmentEvent(event);
            } catch (RuntimeException e) {
                failures.add(e);
                System.err.println("[notify] observer failed: " + e.getMessage());
            }
        }
    }
}
