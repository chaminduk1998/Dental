package com.dental.pricing;

/**
 * <b>Strategy pattern.</b>
 *
 * <p>The billing service knows it has to work out a discount, but not how.
 * Each concession the surgery offers is a separate implementation, so a new
 * scheme is a new class rather than another {@code if} in the service.</p>
 */
public interface PricingStrategy {

    /** Stable code stored on the bill row. */
    String code();

    /** Human readable name printed on the receipt. */
    String label();

    /** Short explanation shown in the UI. */
    String description();

    /**
     * @param subTotal treatment cost + consultation fee
     * @return the discount amount (never larger than {@code subTotal})
     */
    double discountFor(double subTotal);
}
