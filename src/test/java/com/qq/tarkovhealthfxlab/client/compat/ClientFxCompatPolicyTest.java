package com.qq.tarkovhealthfxlab.client.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientFxCompatPolicyTest {
    @Test
    void installedKnownVersionsOpenAllReflectionGates() {
        ClientFxCompatPolicy.Selection selection = ClientFxCompatPolicy.select(
                mod("1.8.2"),
                mod("1.1-1.20.1"),
                mod("0.2.3.0-forge")
        );
        assertTrue(selection.enhancedVisuals());
        assertTrue(selection.cameraOverhaul());
        assertTrue(selection.explosionOverhaul());
    }

    @Test
    void missingModsSelectOnlySafeBuiltInFallbacks() {
        ClientFxCompatPolicy.Selection selection = ClientFxCompatPolicy.select(
                ClientFxCompatPolicy.ModCandidate.missing(),
                ClientFxCompatPolicy.ModCandidate.missing(),
                ClientFxCompatPolicy.ModCandidate.missing()
        );
        assertFalse(selection.enhancedVisuals());
        assertFalse(selection.cameraOverhaul());
        assertFalse(selection.explosionOverhaul());
    }

    @Test
    void unknownApiVersionsKeepReflectionClosed() {
        ClientFxCompatPolicy.Selection selection = ClientFxCompatPolicy.select(
                mod("2.0.0"),
                mod("1.2-1.20.1"),
                mod("0.2.4.0-forge")
        );
        assertFalse(selection.enhancedVisuals());
        assertFalse(selection.cameraOverhaul());
        assertFalse(selection.explosionOverhaul());
    }

    @Test
    void explosionSuppressionRequiresBothExactAdapterAndAnalgesia() {
        ClientFxCompatPolicy.Selection supported = new ClientFxCompatPolicy.Selection(
                false, false, true);
        assertFalse(ClientFxCompatPolicy.shouldSuppressExplosion(supported, false));
        assertTrue(ClientFxCompatPolicy.shouldSuppressExplosion(supported, true));
        assertFalse(ClientFxCompatPolicy.shouldSuppressExplosion(
                new ClientFxCompatPolicy.Selection(false, false, false), true));
    }

    @Test
    void rendererDisabledByCreativeModeReturnsScreenOwnershipToFallback() {
        assertFalse(ClientFxCompatPolicy.shouldUseLegacyScreenEffects(true, true));
        assertTrue(ClientFxCompatPolicy.shouldUseLegacyScreenEffects(true, false));
        assertTrue(ClientFxCompatPolicy.shouldUseLegacyScreenEffects(false, false));
    }

    private static ClientFxCompatPolicy.ModCandidate mod(String version) {
        return new ClientFxCompatPolicy.ModCandidate(true, version);
    }
}
