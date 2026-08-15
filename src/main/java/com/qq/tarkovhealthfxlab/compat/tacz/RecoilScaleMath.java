package com.qq.tarkovhealthfxlab.compat.tacz;

/** Pure math used to preserve TaCZ's ordinary affine attachment modifiers. */
public final class RecoilScaleMath {
    private static final double EPSILON = 1.0E-6D;

    private RecoilScaleMath() {
    }

    public static AffineSamples samples(double atZero, double atHalf, double atOne) {
        return new AffineSamples(atZero, atHalf, atOne);
    }

    public static Parameters scaledParameters(AffineSamples samples, double injuryMultiplier) {
        if (samples == null || !samples.isFinite() || !Double.isFinite(injuryMultiplier)) {
            return Parameters.UNSUPPORTED;
        }
        double slope = samples.atOne() - samples.atZero();
        double expectedHalf = samples.atZero() + slope * 0.5D;
        double tolerance = 1.0E-5D * Math.max(1.0D, Math.max(Math.abs(samples.atZero()), Math.abs(samples.atOne())));
        if (Math.abs(samples.atHalf() - expectedHalf) > tolerance || Math.abs(slope) < EPSILON) {
            return Parameters.UNSUPPORTED;
        }

        double scaledSlope = slope * injuryMultiplier;
        if (Math.abs(scaledSlope) < EPSILON) {
            return Parameters.UNSUPPORTED;
        }
        // TaCZ evaluates one Modifier as (x + addend) * (1 + percent).
        double addend = samples.atZero() / slope;
        double percentDelta = scaledSlope - 1.0D;
        return new Parameters(addend, percentDelta, true);
    }

    public record AffineSamples(double atZero, double atHalf, double atOne) {
        public boolean isFinite() {
            return Double.isFinite(atZero) && Double.isFinite(atHalf) && Double.isFinite(atOne);
        }
    }

    public record Parameters(double addend, double percentDelta, boolean supported) {
        public static final Parameters UNSUPPORTED = new Parameters(0.0D, 0.0D, false);

        public double evaluate(double input) {
            return (input + addend) * (1.0D + percentDelta);
        }
    }
}
