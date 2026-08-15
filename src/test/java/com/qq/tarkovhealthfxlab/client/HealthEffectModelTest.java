package com.qq.tarkovhealthfxlab.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HealthEffectModelTest {
    @Test
    void outputRemainsRestrainedAcrossFullCycle() {
        EffectChannels critical = new EffectChannels(1.0D, 1.0D, 1.0D, 1.0D, 1, 2);
        for (int sample = 0; sample <= 600; sample++) {
            HealthFxFrame frame = HealthEffectModel.evaluate(
                    critical, sample / 60.0D, true, true, 1.0D, false, false, 1.0D);
            assertRange(frame.bloodAlpha(), 0.0D, 0.28D);
            assertRange(frame.fractureAlpha(), 0.0D, 0.28D);
            assertRange(frame.painAlpha(), 0.0D, 0.28D);
            assertRange(frame.vignetteAlpha(), 0.0D, 0.28D);
            assertRange(frame.heartbeatVolume(), 0.0D, 1.0D);
            assertRange(frame.breathVolume(), 0.0D, 1.0D);
            assertTrue(Math.abs(frame.cameraPitch()) <= 0.22D + 1.0E-9D);
            assertTrue(Math.abs(frame.cameraRoll()) <= 0.28D + 1.0E-9D);
        }
    }

    @Test
    void reducedMotionCompletelyDisablesCameraOffsets() {
        HealthFxFrame frame = HealthEffectModel.evaluate(
                new EffectChannels(0.0D, 1.0D, 0.8D, 0.0D, 0, 1),
                2.1D, true, true, 1.0D, false, true, 1.0D);
        assertEquals(0.0D, frame.cameraPitch());
        assertEquals(0.0D, frame.cameraRoll());
    }

    @Test
    void painCreatesSlightCameraInstabilityWhileStandingStill() {
        HealthFxFrame frame = HealthEffectModel.evaluate(
                new EffectChannels(0.0D, 0.0D, 0.8D, 0.0D, 0, 0),
                1.37D, false, true, 1.0D, false, false, 0.6D);
        assertTrue(Math.abs(frame.cameraPitch()) > 1.0E-5D);
        assertTrue(Math.abs(frame.cameraRoll()) > 1.0E-5D);
        assertTrue(Math.abs(frame.cameraPitch()) < 0.06D);
        assertTrue(Math.abs(frame.cameraRoll()) < 0.05D);
    }

    @Test
    void lowModeScalesScreenAlphasButNotStatusState() {
        EffectChannels channels = new EffectChannels(1.0D, 0.5D, 0.7D, 0.4D, 1, 1);
        HealthFxFrame full = HealthEffectModel.evaluate(
                channels, 3.2D, false, true, 1.0D, false, false, 0.5D);
        HealthFxFrame low = HealthEffectModel.evaluate(
                channels, 3.2D, false, true, 1.0D, true, false, 0.5D);
        assertEquals(full.bloodAlpha() * 0.55D, low.bloodAlpha(), 1.0E-9D);
        assertEquals(full.fractureAlpha() * 0.55D, low.fractureAlpha(), 1.0E-9D);
        assertEquals(full.painAlpha() * 0.55D, low.painAlpha(), 1.0E-9D);
    }

    private static void assertRange(double value, double minimum, double maximum) {
        assertTrue(value >= minimum && value <= maximum,
                () -> value + " outside [" + minimum + ", " + maximum + "]");
    }
}
