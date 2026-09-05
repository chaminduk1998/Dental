package com.dental.pricing;

/** Full price - the default concrete strategy. */
public class StandardPricing implements PricingStrategy {

    @Override
    public String code() { return "STANDARD"; }

    @Override
    public String label() { return "Standard"; }

    @Override
    public String description() { return "Full price, no concession applied"; }

    @Override
    public double discountFor(double subTotal) { return 0d; }
}
