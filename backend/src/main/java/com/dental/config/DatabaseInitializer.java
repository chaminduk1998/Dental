package com.dental.config;

import com.dental.util.PasswordUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the schema and the seed rows on first start so the system can be run
 * straight after checkout. Every statement is idempotent, so running it again
 * on an existing database changes nothing.
 *
 * <p>Mirrors {@code database/schema.sql}.</p>
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() { }

    public static void initialise() {
        AppConfig cfg = AppConfig.get();
        String schema = cfg.get("db.name");

        // 1. the database itself
        try (Connection c = DatabaseConnection.serverConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + schema + "` "
                    + "DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot create/reach database `" + schema + "`: " + e.getMessage(), e);
        }

        // 2. tables
        try (Connection c = DatabaseConnection.getInstance().getConnection(); Statement st = c.createStatement()) {
            for (String ddl : TABLES) {
                st.executeUpdate(ddl);
            }
            seed(c, st);
        } catch (SQLException e) {
            throw new IllegalStateException("Schema initialisation failed: " + e.getMessage(), e);
        }
        System.out.println("[db] schema `" + schema + "` ready");
    }

    private static void seed(Connection c, Statement st) throws SQLException {
        if (isEmpty(st, "users")) {
            String admin = PasswordUtil.hash("admin123");
            String staff = PasswordUtil.hash("staff123");
            String recep = PasswordUtil.hash("reception123");
            st.executeUpdate("INSERT INTO users (username,password,role,full_name) VALUES "
                    + "('admin','" + admin + "','ADMIN','System Administrator'),"
                    + "('kamal','" + staff + "','STAFF','Kamal Jayasinghe'),"
                    + "('nadeeka','" + recep + "','STAFF','Nadeeka Silva')");
            System.out.println("[db] seeded users (admin/admin123)");
        }
        if (isEmpty(st, "dentists")) {
            st.executeUpdate("INSERT INTO dentists (name,specialization,consultation_fee) VALUES "
                    + "('Dr. Anura Bandara','General Dentistry',1500.00),"
                    + "('Dr. Shanika Perera','Orthodontics',2500.00),"
                    + "('Dr. Ruwan Fernando','Oral Surgery',3000.00),"
                    + "('Dr. Malini Gunawardena','Pedodontics',2000.00)");
        }
        if (isEmpty(st, "treatments")) {
            st.executeUpdate("INSERT INTO treatments (treatment_type,base_cost,duration_min) VALUES "
                    + "('Dental Check-up',1000.00,20),"
                    + "('Scaling & Polishing',4500.00,45),"
                    + "('Tooth Filling',3500.00,40),"
                    + "('Tooth Extraction',5000.00,30),"
                    + "('Root Canal Treatment',15000.00,90),"
                    + "('Braces Fitting',45000.00,120),"
                    + "('Teeth Whitening',12000.00,60),"
                    + "('Denture Fitting',25000.00,75)");
        }
    }

    private static boolean isEmpty(Statement st, String table) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private static final String[] TABLES = {

        """
        CREATE TABLE IF NOT EXISTS users (
          id         INT AUTO_INCREMENT PRIMARY KEY,
          username   VARCHAR(50)  NOT NULL UNIQUE,
          password   CHAR(64)     NOT NULL,
          role       VARCHAR(20)  NOT NULL DEFAULT 'STAFF',
          full_name  VARCHAR(100) NOT NULL,
          active     TINYINT(1)   NOT NULL DEFAULT 1,
          created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB
        """,

        """
        CREATE TABLE IF NOT EXISTS patients (
          id         INT AUTO_INCREMENT PRIMARY KEY,
          name       VARCHAR(100) NOT NULL,
          address    VARCHAR(255),
          contact_no VARCHAR(20),
          email      VARCHAR(120),
          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
          INDEX idx_patient_name (name),
          INDEX idx_patient_contact (contact_no)
        ) ENGINE=InnoDB
        """,

        """
        CREATE TABLE IF NOT EXISTS dentists (
          id               INT AUTO_INCREMENT PRIMARY KEY,
          name             VARCHAR(100)  NOT NULL,
          specialization   VARCHAR(100),
          consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
          active           TINYINT(1)    NOT NULL DEFAULT 1
        ) ENGINE=InnoDB
        """,

        """
        CREATE TABLE IF NOT EXISTS treatments (
          id             INT AUTO_INCREMENT PRIMARY KEY,
          treatment_type VARCHAR(100)  NOT NULL,
          base_cost      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
          duration_min   INT           NOT NULL DEFAULT 30,
          active         TINYINT(1)    NOT NULL DEFAULT 1
        ) ENGINE=InnoDB
        """,

        """
        CREATE TABLE IF NOT EXISTS appointments (
          id             INT AUTO_INCREMENT PRIMARY KEY,
          appointment_no VARCHAR(20) NOT NULL UNIQUE,
          patient_id     INT NOT NULL,
          dentist_id     INT NOT NULL,
          treatment_id   INT NOT NULL,
          appt_date      DATE NOT NULL,
          appt_time      TIME NOT NULL,
          status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
          notes          VARCHAR(255),
          created_by     INT,
          created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
          updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          CONSTRAINT fk_appt_patient   FOREIGN KEY (patient_id)   REFERENCES patients(id),
          CONSTRAINT fk_appt_dentist   FOREIGN KEY (dentist_id)   REFERENCES dentists(id),
          CONSTRAINT fk_appt_treatment FOREIGN KEY (treatment_id) REFERENCES treatments(id),
          INDEX idx_appt_date (appt_date),
          INDEX idx_appt_status (status)
        ) ENGINE=InnoDB
        """,

        """
        CREATE TABLE IF NOT EXISTS bills (
          id               INT AUTO_INCREMENT PRIMARY KEY,
          bill_no          VARCHAR(20) NOT NULL UNIQUE,
          appointment_id   INT NOT NULL UNIQUE,
          treatment_cost   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
          consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
          discount         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
          tax              DECIMAL(10,2) NOT NULL DEFAULT 0.00,
          total            DECIMAL(10,2) NOT NULL DEFAULT 0.00,
          pricing_strategy VARCHAR(40) NOT NULL DEFAULT 'STANDARD',
          payment_method   VARCHAR(20) NOT NULL DEFAULT 'CASH',
          issued_by        VARCHAR(50),
          issued_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT fk_bill_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id)
        ) ENGINE=InnoDB
        """,

        """
        CREATE TABLE IF NOT EXISTS notifications (
          id         INT AUTO_INCREMENT PRIMARY KEY,
          recipient  VARCHAR(150),
          channel    VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
          subject    VARCHAR(150),
          message    TEXT,
          status     VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB
        """,

        """
        CREATE TABLE IF NOT EXISTS audit_logs (
          id         INT AUTO_INCREMENT PRIMARY KEY,
          username   VARCHAR(50),
          action     VARCHAR(40),
          entity     VARCHAR(40),
          entity_ref VARCHAR(50),
          details    VARCHAR(255),
          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
          INDEX idx_audit_created (created_at)
        ) ENGINE=InnoDB
        """
    };
}
