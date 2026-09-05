package com.dental.pricing;

/** 10% off for returning patients (3 or more previous visits). */
public class LoyaltyPricing implements PricingStrategy {

    private static final double RATE = 0.10;

    @Override
    public String code() { return "LOYALTY"; }

    @Override
    public String label() { return "Loyalty (10%)"; }

    @Override
    public String description() { return "10% off for returning patients with 3 or more visits"; }

    @Override
    public double discountFor(double subTotal) {
        return SeniorCitizenPricing.round(subTotal * RATE);
    }
}
