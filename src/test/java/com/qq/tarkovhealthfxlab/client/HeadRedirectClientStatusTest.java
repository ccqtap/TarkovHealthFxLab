package com.qq.tarkovhealthfxlab.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HeadRedirectClientStatusTest {
    @AfterEach
    void reset() {
        HeadRedirectClientStatus.reset();
    }

    @Test
    void acceptsStatusAndLatestRedirectFeedback() {
        assertTrue(HeadRedirectClientStatus.accept(
                "head_redirect=true, last: seed=7 original=head target=left_arm damage=20.0 redirected=true"));
        assertEquals(HeadRedirectClientStatus.State.ENABLED, HeadRedirectClientStatus.state());
        assertEquals("seed=7 original=head target=left_arm damage=20.0 redirected=true",
                HeadRedirectClientStatus.lastRecord());

        assertTrue(HeadRedirectClientStatus.accept("head_redirect=false"));
        assertEquals(HeadRedirectClientStatus.State.DISABLED, HeadRedirectClientStatus.state());
        assertFalse(HeadRedirectClientStatus.accept("ordinary server message"));
    }

    @Test
    void acceptsStandaloneTestRecord() {
        assertTrue(HeadRedirectClientStatus.accept(
                "seed=9 original=head target=stomach damage=12.0 redirected=true"));
        assertEquals("seed=9 original=head target=stomach damage=12.0 redirected=true",
                HeadRedirectClientStatus.lastRecord());
    }
}
