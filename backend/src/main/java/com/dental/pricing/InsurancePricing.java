package com.dental.pricing;

/** 40% of the bill is settled by the patient's insurer. */
public class InsurancePricing implements PricingStrategy {

    private static final double COVERED = 0.40;

    @Override
    public String code() { return "INSURANCE"; }

    @Override
    public String label() { return "Insurance Cover (40%)"; }

    @Override
    public String description() { return "40% of the bill is claimed from the patient's insurer"; }

    @Override
    public double discountFor(double subTotal) {
        return SeniorCitizenPricing.round(subTotal * COVERED);
    }
}
