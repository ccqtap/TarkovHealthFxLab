package com.qq.tarkovhealthfxlab.common.health;

/** Pure movement rule used by the server attribute reconciler and unit tests. */
public final class MovementPenaltyCalculator {
    public static final double FRACTURED_LEG_PENALTY = 0.15D;
    public static final double BLACKENED_LEG_PENALTY = 0.25D;
    public static final double ANALGESIA_FRACTURE_FACTOR = 0.35D;
    public static final double MAXIMUM_PENALTY = 0.65D;

    private MovementPenaltyCalculator() {
    }

    /** Returns a negative MULTIPLY_TOTAL attribute amount. */
    public static double modifierAmount(int fracturedLegs, int blackenedLegs, boolean analgesia) {
        if (fracturedLegs < 0 || blackenedLegs < 0) {
            throw new IllegalArgumentException("injury counts must be non-negative");
        }
        double fracture = fracturedLegs * FRACTURED_LEG_PENALTY
                * (analgesia ? ANALGESIA_FRACTURE_FACTOR : 1.0D);
        double blackened = blackenedLegs * BLACKENED_LEG_PENALTY;
        return -Math.min(MAXIMUM_PENALTY, fracture + blackened);
    }
}
