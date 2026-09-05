package com.dental;

import com.dental.config.AppConfig;
import com.dental.config.DatabaseConnection;
import com.dental.config.DatabaseInitializer;
import com.dental.notify.AppointmentNotifier;
import com.dental.notify.AuditObserver;
import com.dental.notify.EmailReminderObserver;
import com.dental.service.AuthService;
import com.dental.web.AppointmentHandler;
import com.dental.web.AuditHandler;
import com.dental.web.AuthHandler;
import com.dental.web.BillingHandler;
import com.dental.web.DentistHandler;
import com.dental.web.NotificationHandler;
import com.dental.web.PatientHandler;
import com.dental.web.PublicHandler;
import com.dental.web.ReportHandler;
import com.dental.web.StaticFileHandler;
import com.dental.web.TreatmentHandler;
import com.dental.web.UserHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Application entry point.
 *
 * <p>Boots the 3-tier Dental Surgery Appointment Management System:</p>
 * <ul>
 *   <li>Data tier   - JDBC / MySQL, initialised automatically (schema + seed data)</li>
 *   <li>Logic tier  - {@code com.dental.service.*}, backed by DAOs and the
 *       Strategy / Observer / Factory / Singleton patterns</li>
 *   <li>Presentation tier - a JDK {@link HttpServer} exposing a JSON REST API
 *       under {@code /api/*} and serving the HTML/CSS/JS frontend</li>
 * </ul>
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.get();

        System.out.println("============================================================");
        System.out.println(" " + config.get("clinic.name") + " - Appointment Management System");
        System.out.println("============================================================");

        // ---- Data tier ----
        if (config.getBool("db.autoInit", true)) {
            System.out.println("[startup] checking database schema...");
            DatabaseInitializer.initialise();
        }
        if (!DatabaseConnection.getInstance().testConnection()) {
            System.err.println("[startup] WARNING: could not open a connection to MySQL. "
                    + "Check db.host/db.user/db.password in config.properties.");
        } else {
            System.out.println("[startup] database connection OK (" + config.get("db.name") + ")");
        }

        // ---- Observer pattern wiring ----
        AppointmentNotifier.get().subscribe(new EmailReminderObserver());
        AppointmentNotifier.get().subscribe(new AuditObserver());

        // ---- Logic tier shared services ----
        AuthService authService = new AuthService();

        // ---- Presentation tier ----
        int port = config.getInt("server.port", 8080);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));

        server.createContext("/api/auth", new AuthHandler(authService));
        server.createContext("/api/public", new PublicHandler(authService));
        server.createContext("/api/patients", new PatientHandler(authService));
        server.createContext("/api/dentists", new DentistHandler(authService));
        server.createContext("/api/treatments", new TreatmentHandler(authService));
        server.createContext("/api/appointments", new AppointmentHandler(authService));
        server.createContext("/api/bills", new BillingHandler(authService));
        server.createContext("/api/reports", new ReportHandler(authService));
        server.createContext("/api/users", new UserHandler(authService));
        server.createContext("/api/notifications", new NotificationHandler(authService));
        server.createContext("/api/audit", new AuditHandler(authService));

        Path webRoot = Path.of(config.get("web.root", "src/main/resources/web"));
        server.createContext("/", new StaticFileHandler(webRoot));

        server.start();

        System.out.println("[startup] web root      : " + webRoot.toAbsolutePath());
        System.out.println("[startup] server running : http://localhost:" + port + "/");
        System.out.println("[startup] default login  : admin / admin123");
        System.out.println("============================================================");
    }
}
