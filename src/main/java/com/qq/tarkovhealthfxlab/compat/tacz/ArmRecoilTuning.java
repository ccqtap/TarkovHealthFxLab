package com.qq.tarkovhealthfxlab.compat.tacz;

/** Configurable injury-to-recoil policy. */
public record ArmRecoilTuning(
        double fractureHorizontalExcess,
        double fractureVerticalExcess,
        double blackenedHorizontalExcess,
        double blackenedVerticalExcess,
        double analgesiaExcessScale,
        double maximumMultiplier
) {
    public static final ArmRecoilTuning DEFAULT = new ArmRecoilTuning(
            0.15D,
            0.25D,
            0.30D,
            0.45D,
            0.50D,
            3.00D
    );

    public ArmRecoilTuning {
        fractureHorizontalExcess = nonNegative(fractureHorizontalExcess);
        fractureVerticalExcess = nonNegative(fractureVerticalExcess);
        blackenedHorizontalExcess = nonNegative(blackenedHorizontalExcess);
        blackenedVerticalExcess = nonNegative(blackenedVerticalExcess);
        analgesiaExcessScale = clamp(analgesiaExcessScale, 0.0D, 1.0D);
        maximumMultiplier = clamp(maximumMultiplier, 1.0D, 8.0D);
    }

    public RecoilMultipliers multipliers(InjuryCompatState.ArmInjuryState state) {
        InjuryCompatState.ArmInjuryState safeState = state == null
                ? InjuryCompatState.ArmInjuryState.HEALTHY
                : state;
        double horizontalExcess = safeState.fracturedArms() * fractureHorizontalExcess
                + safeState.blackenedArms() * blackenedHorizontalExcess;
        double verticalExcess = safeState.fracturedArms() * fractureVerticalExcess
                + safeState.blackenedArms() * blackenedVerticalExcess;
        if (safeState.analgesiaActive()) {
            horizontalExcess *= analgesiaExcessScale;
            verticalExcess *= analgesiaExcessScale;
        }
        return new RecoilMultipliers(
                clamp(1.0D + horizontalExcess, 1.0D, maximumMultiplier),
                clamp(1.0D + verticalExcess, 1.0D, maximumMultiplier)
        );
    }

    private static double nonNegative(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, value);
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record RecoilMultipliers(double horizontal, double vertical) {
        public static final RecoilMultipliers IDENTITY = new RecoilMultipliers(1.0D, 1.0D);

        public RecoilMultipliers {
            horizontal = sanitize(horizontal);
            vertical = sanitize(vertical);
        }

        public boolean isIdentity() {
            return Math.abs(horizontal - 1.0D) < 1.0E-6D
                    && Math.abs(vertical - 1.0D) < 1.0E-6D;
        }

        private static double sanitize(double value) {
            if (!Double.isFinite(value)) {
                return 1.0D;
            }
            return Math.max(0.0D, Math.min(8.0D, value));
        }
    }
}
