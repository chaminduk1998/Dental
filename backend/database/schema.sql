-- =====================================================================
--  Dental Surgery Appointment Management System
--  MySQL 8 schema + seed data
--
--  The application creates all of this automatically when
--  db.autoInit=true in config.properties, so running this script by
--  hand is optional.  Use it if you prefer to set the database up
--  yourself (phpMyAdmin / MySQL Workbench / SQLTools).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS dental_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE dental_db;

-- ---------------------------------------------------------------------
-- 1. users  -  staff accounts used for login
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE,
  password    CHAR(64)     NOT NULL COMMENT 'SHA-256 hex digest',
  role        VARCHAR(20)  NOT NULL DEFAULT 'STAFF' COMMENT 'ADMIN | STAFF',
  full_name   VARCHAR(100) NOT NULL,
  active      TINYINT(1)   NOT NULL DEFAULT 1,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. patients
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patients (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  address     VARCHAR(255),
  contact_no  VARCHAR(20),
  email       VARCHAR(120),
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_patient_name (name),
  INDEX idx_patient_contact (contact_no)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. dentists
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dentists (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  name             VARCHAR(100)   NOT NULL,
  specialization   VARCHAR(100),
  consultation_fee DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
  active           TINYINT(1)     NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. treatments
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS treatments (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  treatment_type VARCHAR(100)  NOT NULL,
  base_cost      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  duration_min   INT           NOT NULL DEFAULT 30,
  active         TINYINT(1)    NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. appointments
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointments (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  appointment_no VARCHAR(20) NOT NULL UNIQUE,
  patient_id     INT NOT NULL,
  dentist_id     INT NOT NULL,
  treatment_id   INT NOT NULL,
  appt_date      DATE NOT NULL,
  appt_time      TIME NOT NULL,
  status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                 COMMENT 'PENDING | CONFIRMED | COMPLETED | CANCELLED',
  notes          VARCHAR(255),
  created_by     INT,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_appt_patient   FOREIGN KEY (patient_id)   REFERENCES patients(id),
  CONSTRAINT fk_appt_dentist   FOREIGN KEY (dentist_id)   REFERENCES dentists(id),
  CONSTRAINT fk_appt_treatment FOREIGN KEY (treatment_id) REFERENCES treatments(id),
  INDEX idx_appt_date (appt_date),
  INDEX idx_appt_status (status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. bills
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bills (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  bill_no          VARCHAR(20) NOT NULL UNIQUE,
  appointment_id   INT NOT NULL UNIQUE,
  treatment_cost   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  discount         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  tax              DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  total            DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  pricing_strategy VARCHAR(40)   NOT NULL DEFAULT 'STANDARD',
  payment_method   VARCHAR(20)   NOT NULL DEFAULT 'CASH',
  issued_by        VARCHAR(50),
  issued_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_bill_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. notifications  -  produced by the Observer pattern
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  recipient   VARCHAR(150),
  channel     VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
  subject     VARCHAR(150),
  message     TEXT,
  status      VARCHAR(20) NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED | SENT | FAILED',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 8. audit_logs
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50),
  action      VARCHAR(40),
  entity      VARCHAR(40),
  entity_ref  VARCHAR(50),
  details     VARCHAR(255),
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_created (created_at)
) ENGINE=InnoDB;

-- =====================================================================
--  SEED DATA
--  Passwords are SHA-256 hex digests:
--    admin   / admin123
--    kamal   / staff123
--    nadeeka / reception123
-- =====================================================================
INSERT IGNORE INTO users (username, password, role, full_name) VALUES
 ('admin',   '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN', 'System Administrator'),
 ('kamal',   '10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6', 'STAFF', 'Kamal Jayasinghe'),
 ('nadeeka', '5145dba3b6bda2d610d2c5c435a1c2481eefd3146b6a7e004ad73f794386e031', 'STAFF', 'Nadeeka Silva');

INSERT IGNORE INTO dentists (id, name, specialization, consultation_fee) VALUES
 (1, 'Dr. Anura Bandara',   'General Dentistry',  1500.00),
 (2, 'Dr. Shanika Perera',  'Orthodontics',       2500.00),
 (3, 'Dr. Ruwan Fernando',  'Oral Surgery',       3000.00),
 (4, 'Dr. Malini Gunawardena', 'Pedodontics',     2000.00);

INSERT IGNORE INTO treatments (id, treatment_type, base_cost, duration_min) VALUES
 (1, 'Dental Check-up',      1000.00, 20),
 (2, 'Scaling & Polishing',  4500.00, 45),
 (3, 'Tooth Filling',        3500.00, 40),
 (4, 'Tooth Extraction',     5000.00, 30),
 (5, 'Root Canal Treatment',15000.00, 90),
 (6, 'Braces Fitting',      45000.00,120),
 (7, 'Teeth Whitening',     12000.00, 60),
 (8, 'Denture Fitting',     25000.00, 75);
