package com.qq.tarkovhealthfxlab.client.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class InjuryAudioVolumeTest {
    @Test
    void ringingSliderScalesOnlyItsInputChannel() {
        assertEquals(0.0F, InjuryAudioVolume.ringing(0.6F, 0.0D), 0.0001F);
        assertEquals(0.3F, InjuryAudioVolume.ringing(0.6F, 0.5D), 0.0001F);
        assertEquals(0.6F, InjuryAudioVolume.ringing(0.6F, 1.0D), 0.0001F);
    }

    @Test
    void malformedValuesAreSafelyClamped() {
        assertEquals(0.0F, InjuryAudioVolume.ringing(-1.0F, 0.5D), 0.0001F);
        assertEquals(0.6F, InjuryAudioVolume.ringing(0.6F, 2.0D), 0.0001F);
        assertEquals(0.0F, InjuryAudioVolume.ringing(0.6F, -2.0D), 0.0001F);
    }
}
