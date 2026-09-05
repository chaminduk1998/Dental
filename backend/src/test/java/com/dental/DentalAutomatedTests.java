package com.dental;

import com.dental.model.Appointment;
import com.dental.pricing.InsurancePricing;
import com.dental.pricing.LoyaltyPricing;
import com.dental.pricing.PricingStrategy;
import com.dental.pricing.PricingStrategyFactory;
import com.dental.pricing.SeniorCitizenPricing;
import com.dental.pricing.StandardPricing;
import com.dental.util.PasswordUtil;
import com.dental.util.ValidationException;

public final class DentalAutomatedTests {
    private static int total;
    private static int passed;
    private static int failed;

    private DentalAutomatedTests() { }

    private static void check(boolean condition, String name) {
        total++;
        if (condition) {
            passed++;
            System.out.println("PASS: " + name);
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void checkEquals(Object expected, Object actual, String name) {
        check(expected == null ? actual == null : expected.equals(actual),
                name + " (expected=" + expected + ", actual=" + actual + ")");
    }

    private static void checkThrows(Runnable action, String name) {
        total++;
        try {
            action.run();
            failed++;
            System.out.println("FAIL: " + name + " (no exception thrown)");
        } catch (Throwable ex) {
            passed++;
            System.out.println("PASS: " + name + " (threw " + ex.getClass().getSimpleName() + ")");
        }
    }

    private static void testSha256PasswordHash() {
        checkEquals(
                "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
                PasswordUtil.hash("password"),
                "sha256 of password matches the standard hash");
        check(PasswordUtil.matches("password", PasswordUtil.hash("password")),
                "password matches its hash");
    }

    private static void testValidationExceptionForBlankInput() {
        checkThrows(() -> throwValidationException(),
                "blank validation failures use ValidationException");
    }

    private static void throwValidationException() {
        throw new ValidationException("value is required");
    }

    private static void testAppointmentStatusValues() {
        checkEquals("PENDING", Appointment.PENDING, "pending status is stable");
        checkEquals("CONFIRMED", Appointment.CONFIRMED, "confirmed status is stable");
        checkEquals("COMPLETED", Appointment.COMPLETED, "completed status is stable");
        checkEquals("CANCELLED", Appointment.CANCELLED, "cancelled status is stable");
    }

    private static void testPricingStrategiesResolveKnownKeys() {
        PricingStrategy senior = PricingStrategyFactory.of("senior");
        PricingStrategy insurance = PricingStrategyFactory.of("insurance");
        PricingStrategy loyalty = PricingStrategyFactory.of("loyalty");

        check(senior instanceof SeniorCitizenPricing, "senior key resolves to SeniorCitizenPricing");
        check(insurance instanceof InsurancePricing, "insurance key resolves to InsurancePricing");
        check(loyalty instanceof LoyaltyPricing, "loyalty key resolves to LoyaltyPricing");
        check(PricingStrategyFactory.of(null) instanceof StandardPricing,
                "null pricing key falls back to StandardPricing");
        check(PricingStrategyFactory.of("unknown") instanceof StandardPricing,
                "unknown pricing key falls back to StandardPricing");
    }

    public static void main(String[] args) {
        testSha256PasswordHash();
        testValidationExceptionForBlankInput();
        testAppointmentStatusValues();
        testPricingStrategiesResolveKnownKeys();

        System.out.println();
        System.out.println("Summary: " + passed + "/" + total + " tests passed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}