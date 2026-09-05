package com.dental.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Small input-validation helpers shared by the service layer. */
public final class Validator {

    private static final Pattern PHONE = Pattern.compile("^[0-9+\\-\\s()]{7,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]{2,}$");

    private Validator() { }

    public static String required(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required");
        }
        return value.trim();
    }

    public static int positive(int value, String field) {
        if (value <= 0) {
            throw new ValidationException(field + " must be selected");
        }
        return value;
    }

    public static double notNegative(double value, String field) {
        if (value < 0) {
            throw new ValidationException(field + " cannot be negative");
        }
        return value;
    }

    public static String phone(String value) {
        String v = required(value, "Contact number");
        if (!PHONE.matcher(v).matches()) {
            throw new ValidationException("Contact number format is not valid");
        }
        return v;
    }

    /** Email is optional - blank is accepted, a malformed value is not. */
    public static String optionalEmail(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();
        if (!EMAIL.matcher(v).matches()) {
            throw new ValidationException("Email address format is not valid");
        }
        return v;
    }

    public static LocalDate date(String value, String field) {
        try {
            return LocalDate.parse(required(value, field));
        } catch (DateTimeParseException e) {
            throw new ValidationException(field + " must be in yyyy-MM-dd format");
        }
    }

    public static LocalTime time(String value, String field) {
        String v = required(value, field);
        try {
            return LocalTime.parse(v.length() == 5 ? v + ":00" : v);
        } catch (DateTimeParseException e) {
            throw new ValidationException(field + " must be in HH:mm format");
        }
    }
}
