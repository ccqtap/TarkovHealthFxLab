package com.qq.tarkovhealthfxlab.client.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientFxProviderStatusTest {
    @Test
    void namesEveryApprovedProviderIncludingExternalOnlyIntegrations() {
        ClientFxProviderStatus status = new ClientFxProviderStatus(
                "EnhancedVisuals 1.8.2", "EnhancedVisuals 1.8.2", "Camera Overhaul 1.1",
                "Explosion Overhaul 0.2.3.0", "external", "absent");

        assertTrue(status.visualProviderDisplay().contains("EnhancedVisuals"));
        assertTrue(status.visualProviderDisplay().contains("Camera"));
        assertTrue(status.visualProviderDisplay().contains("Explosion"));
        assertTrue(status.externalProviderDisplay().contains("TaCZ: external"));
        assertTrue(status.externalProviderDisplay().contains("LR Tactical: absent"));
    }
}
