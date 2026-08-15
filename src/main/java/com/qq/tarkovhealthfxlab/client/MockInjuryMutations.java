package com.qq.tarkovhealthfxlab.client;

import java.util.Objects;

/** Pure mock-only mutations shared by the F8 controls and their tests. */
public final class MockInjuryMutations {
    private MockInjuryMutations() {
    }

    public static BodyEffect setPartHealthRatio(
            BodyRegion region,
            BodyEffect before,
            double ratio
    ) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(before, "before");
        double safe = Math.max(0.0D, Math.min(1.0D, ratio));
        BodyEffect updated = before.withCurrentHealth(before.maximumHealth() * safe);
        if ((region.isArm() || region.isLeg()) && updated.currentHealth() <= 0.0D) {
            updated = updated.withBlackened(true);
        }
        return updated;
    }
}
