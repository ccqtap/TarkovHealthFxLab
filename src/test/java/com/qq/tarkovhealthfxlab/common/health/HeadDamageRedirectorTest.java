package com.qq.tarkovhealthfxlab.common.health;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HeadDamageRedirectorTest {
    @Test
    void disabledOrNonHeadDamageKeepsOriginalPart() {
        assertEquals(BodyPart.HEAD, HeadDamageRedirector.resolve(false, BodyPart.HEAD, 12.5F, 4L).appliedPart());
        assertEquals(BodyPart.LEFT_ARM, HeadDamageRedirector.resolve(true, BodyPart.LEFT_ARM, 12.5F, 4L).appliedPart());
    }

    @Test
    void enabledHeadDamageIsDeterministicAndPreservesAmount() {
        DamageApplication first = HeadDamageRedirector.resolve(true, BodyPart.HEAD, 19.25F, 987654L);
        DamageApplication second = HeadDamageRedirector.resolve(true, BodyPart.HEAD, 19.25F, 987654L);

        assertEquals(first, second);
        assertNotEquals(BodyPart.HEAD, first.appliedPart());
        assertEquals(19.25F, first.amount());
        assertTrue(first.redirected());
    }

    @Test
    void chooserCanReachEveryNonHeadTarget() {
        Set<BodyPart> reached = new HashSet<>();
        for (long seed = 0; seed < 10_000 && reached.size() < 6; seed++) reached.add(HeadDamageRedirector.choose(seed));
        assertEquals(new HashSet<>(HeadDamageRedirector.TARGETS), reached);
    }
}
