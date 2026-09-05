package com.dental.pricing;

/** 15% concession for patients aged 60 and over. */
public class SeniorCitizenPricing implements PricingStrategy {

    private static final double RATE = 0.15;

    @Override
    public String code() { return "SENIOR"; }

    @Override
    public String label() { return "Senior Citizen (15%)"; }

    @Override
    public String description() { return "15% concession for patients aged 60 and above"; }

    @Override
    public double discountFor(double subTotal) {
        return round(subTotal * RATE);
    }

    static double round(double v) {
        return Math.round(v * 100d) / 100d;
    }
}
