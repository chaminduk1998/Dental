package com.dental.pricing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Factory pattern</b> that hands out the concrete {@link PricingStrategy}
 * selected on the billing screen. Unknown codes fall back to STANDARD, so a bad
 * request can never leave a bill without a price rule.
 */
public final class PricingStrategyFactory {

    private static final Map<String, PricingStrategy> REGISTRY = new LinkedHashMap<>();

    static {
        register(new StandardPricing());
        register(new SeniorCitizenPricing());
        register(new InsurancePricing());
        register(new LoyaltyPricing());
    }

    private PricingStrategyFactory() { }

    private static void register(PricingStrategy s) {
        REGISTRY.put(s.code(), s);
    }

    public static PricingStrategy of(String code) {
        if (code == null) {
            return REGISTRY.get("STANDARD");
        }
        return REGISTRY.getOrDefault(code.trim().toUpperCase(), REGISTRY.get("STANDARD"));
    }

    /** Every strategy, in the order the drop-down should show them. */
    public static List<PricingStrategy> all() {
        return List.copyOf(REGISTRY.values());
    }
}
