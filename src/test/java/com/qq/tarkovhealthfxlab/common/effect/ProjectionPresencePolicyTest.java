package com.qq.tarkovhealthfxlab.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectionPresencePolicyTest {
    @Test
    void externalEffectRiseAndExpiryWriteTruth() {
        assertEquals(ProjectionPresencePolicy.Action.APPLY_EXTERNAL,
                ProjectionPresencePolicy.decide(true, false, false));
        assertEquals(ProjectionPresencePolicy.Action.CLEAR_EXTERNAL,
                ProjectionPresencePolicy.decide(false, true, false));
    }

    @Test
    void missingAuthoritativeProjectionIsRestoredNotCleared() {
        assertEquals(ProjectionPresencePolicy.Action.RESTORE_PROJECTION,
                ProjectionPresencePolicy.decide(false, true, true));
        assertEquals(ProjectionPresencePolicy.Action.RESTORE_PROJECTION,
                ProjectionPresencePolicy.decide(false, false, true));
    }

    @Test
    void reconcileNeverPromotesAnExternalTimedEffect() {
        int externalDesiredBit = 1 << 4;
        int authoritativeBit = 1 << 2;
        int previousOwnership = authoritativeBit;
        int desiredTruth = authoritativeBit | externalDesiredBit;

        assertEquals(authoritativeBit,
                ProjectionPresencePolicy.retainOwnedProjectionMask(
                        previousOwnership, desiredTruth));
        assertEquals(0,
                ProjectionPresencePolicy.retainOwnedProjectionMask(0, externalDesiredBit));
    }

    @Test
    void authoritativeTreatmentRemovesAnExternallyStartedCondition() {
        assertEquals(true, ProjectionPresencePolicy.shouldRemoveEffect(
                false, true, false, false));
        assertEquals(false, ProjectionPresencePolicy.shouldRemoveEffect(
                false, true, false, true));
        assertEquals(true, ProjectionPresencePolicy.shouldRemoveEffect(
                true, true, false, true));
    }
}
