package com.qq.tarkovhealthfxlab.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HealthFxAssetTest {
    private static final String ROOT = "/assets/tarkov_health_fx_lab/";

    @Test
    void overlayTexturesHaveAlphaAndLeaveTheAimCenterClear() throws IOException {
        for (String name : List.of("bleeding_border.png", "fracture_stress.png", "pain_periphery.png")) {
            try (InputStream stream = resource("textures/gui/health_fx/" + name)) {
                BufferedImage image = ImageIO.read(stream);
                assertNotNull(image, name);
                assertEquals(1254, image.getWidth(), name);
                assertEquals(1254, image.getHeight(), name);
                assertTrue(image.getColorModel().hasAlpha(), name + " must retain transparency");

                int visible = 0;
                int samples = 0;
                for (int y = image.getHeight() * 3 / 10; y < image.getHeight() * 7 / 10; y += 3) {
                    for (int x = image.getWidth() * 3 / 10; x < image.getWidth() * 7 / 10; x += 3) {
                        samples++;
                        if (((image.getRGB(x, y) >>> 24) & 0xFF) > 8) visible++;
                    }
                }
                assertTrue(visible <= samples / 200,
                        name + " obstructs more than 0.5% of the central aiming area");
            }
        }
    }

    @Test
    void suppliedStatusIconsArePackagedAtNativeSizeWithTransparency() throws IOException {
        for (String name : List.of(
                "bleeding.png",
                "fracture.png",
                "pain.png",
                "painkiller.png",
                "blackened_left_arm.png",
                "blackened_right_arm.png",
                "blackened_left_leg.png",
                "blackened_right_leg.png")) {
            try (InputStream stream = resource("textures/gui/status/" + name)) {
                BufferedImage image = ImageIO.read(stream);
                assertNotNull(image, name);
                assertEquals(18, image.getWidth(), name);
                assertEquals(18, image.getHeight(), name);
                assertTrue(image.getColorModel().hasAlpha(), name + " must retain transparency");

                boolean transparentPixel = false;
                for (int y = 0; y < image.getHeight() && !transparentPixel; y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        if (((image.getRGB(x, y) >>> 24) & 0xFF) == 0) {
                            transparentPixel = true;
                            break;
                        }
                    }
                }
                assertTrue(transparentPixel, name + " must not contain the exported checkerboard background");
            }
        }
    }

    @Test
    void mobEffectIconCopiesCoverAllSuppliedEffectTypes() throws IOException {
        for (String name : List.of(
                "light_bleeding.png",
                "heavy_bleeding.png",
                "pain.png",
                "fracture.png",
                "blackened_left_arm.png",
                "blackened_right_arm.png",
                "blackened_left_leg.png",
                "blackened_right_leg.png",
                "analgesia.png")) {
            try (InputStream stream = resource("textures/mob_effect/" + name)) {
                BufferedImage image = ImageIO.read(stream);
                assertNotNull(image, name);
                assertEquals(18, image.getWidth(), name);
                assertEquals(18, image.getHeight(), name);
            }
        }
    }

    @Test
    void originalAudioAssetsArePackagedAsOggVorbisContainers() throws IOException {
        for (String name : List.of(
                "bleed_pulse.ogg",
                "fracture_onset.ogg",
                "fracture_step.ogg",
                "pain_sting.ogg",
                "pain_breath.ogg",
                "relief.ogg")) {
            try (InputStream stream = resource("sounds/health_fx/" + name)) {
                byte[] header = stream.readNBytes(4);
                assertEquals("OggS", new String(header, java.nio.charset.StandardCharsets.US_ASCII), name);
            }
        }
    }

    private static InputStream resource(String relative) {
        InputStream stream = HealthFxAssetTest.class.getResourceAsStream(ROOT + relative);
        assertNotNull(stream, relative);
        return stream;
    }
}
