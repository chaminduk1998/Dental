package com.dental.dao;

/**
 * <b>Factory pattern.</b>
 *
 * <p>The service tier never calls {@code new UserDAO()} directly; it asks this
 * factory instead. Swapping MySQL for another store (or for a mock during
 * testing) means changing the factory only.</p>
 */
public final class DAOFactory {

    private static final UserDAO USER = new UserDAO();
    private static final PatientDAO PATIENT = new PatientDAO();
    private static final DentistDAO DENTIST = new DentistDAO();
    private static final TreatmentDAO TREATMENT = new TreatmentDAO();
    private static final AppointmentDAO APPOINTMENT = new AppointmentDAO();
    private static final BillDAO BILL = new BillDAO();
    private static final NotificationDAO NOTIFICATION = new NotificationDAO();
    private static final AuditLogDAO AUDIT = new AuditLogDAO();
    private static final ReportDAO REPORT = new ReportDAO();

    private DAOFactory() { }

    public static UserDAO users() { return USER; }

    public static PatientDAO patients() { return PATIENT; }

    public static DentistDAO dentists() { return DENTIST; }

    public static TreatmentDAO treatments() { return TREATMENT; }

    public static AppointmentDAO appointments() { return APPOINTMENT; }

    public static BillDAO bills() { return BILL; }

    public static NotificationDAO notifications() { return NOTIFICATION; }

    public static AuditLogDAO auditLogs() { return AUDIT; }

    public static ReportDAO reports() { return REPORT; }
}
