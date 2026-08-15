package com.qq.tarkovhealthfxlab.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThirdPartyCompatBootstrapTest {
    @Test
    void directAdaptersAreLockedToTheAuditedVersions() {
        assertTrue(ThirdPartyCompatBootstrap.supportedVersion("tacz", "1.1.8-hotfix"));
        assertTrue(ThirdPartyCompatBootstrap.supportedVersion("lrtactical", "0.4.1"));
        assertFalse(ThirdPartyCompatBootstrap.supportedVersion("tacz", "1.1.9"));
        assertFalse(ThirdPartyCompatBootstrap.supportedVersion("lrtactical", "0.5.0"));
        assertFalse(ThirdPartyCompatBootstrap.supportedVersion("unknown", "1.0"));
    }
}
