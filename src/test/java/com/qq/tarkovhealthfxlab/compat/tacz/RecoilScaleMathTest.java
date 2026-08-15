package com.qq.tarkovhealthfxlab.compat.tacz;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoilScaleMathTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void scalesIdentityTransform() {
        var parameters = RecoilScaleMath.scaledParameters(
                RecoilScaleMath.samples(0.0D, 0.5D, 1.0D),
                1.25D
        );

        assertTrue(parameters.supported());
        assertEquals(1.25D, parameters.evaluate(1.0D), EPSILON);
        assertEquals(1.875D, parameters.evaluate(1.5D), EPSILON);
    }

    @Test
    void preservesExistingAffineAttachmentTransformBeforeScaling() {
        // Existing TaCZ attachment transform: f(x) = (x + 0.2) * 0.8.
        var samples = RecoilScaleMath.samples(0.16D, 0.56D, 0.96D);

        var parameters = RecoilScaleMath.scaledParameters(samples, 1.5D);

        assertTrue(parameters.supported());
        assertEquals(((1.25D + 0.2D) * 0.8D) * 1.5D, parameters.evaluate(1.25D), EPSILON);
    }

    @Test
    void rejectsNonlinearScriptInsteadOfDestroyingItsBehavior() {
        var parameters = RecoilScaleMath.scaledParameters(
                RecoilScaleMath.samples(0.0D, 0.25D, 1.0D),
                1.5D
        );

        assertFalse(parameters.supported());
    }
}
