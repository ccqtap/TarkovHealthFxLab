package com.qq.tarkovhealthfxlab.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HealthFxSourceTest {
    @Test
    void cyclesThroughMockLabAndTarkovWithoutSkippingFallback() {
        assertEquals(HealthFxSource.LAB_SERVER, HealthFxSource.MOCK.next());
        assertEquals(HealthFxSource.TARKOV_LIVE, HealthFxSource.LAB_SERVER.next());
        assertEquals(HealthFxSource.MOCK, HealthFxSource.TARKOV_LIVE.next());
    }
}
