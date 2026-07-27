package io.github.ximu.autofish.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoFishConfigTest {
    @Test
    void normalizesUnsafeValues() {
        AutoFishConfig config = new AutoFishConfig();
        config.recastDelayMs = -5;
        config.clearLagRegex = null;

        config.normalize();

        assertEquals(AutoFishConfig.CURRENT_VERSION, config.version);
        assertEquals(500, config.recastDelayMs);
        assertEquals("", config.clearLagRegex);
    }

    @Test
    void clampsMaximumDelayAndRegexLength() {
        AutoFishConfig config = new AutoFishConfig();
        config.recastDelayMs = 9999;
        config.clearLagRegex = "x".repeat(600);

        config.normalize();

        assertEquals(5000, config.recastDelayMs);
        assertEquals(512, config.clearLagRegex.length());
    }

    @Test
    void copyPreservesUpstreamOptions() {
        AutoFishConfig config = new AutoFishConfig();
        config.multiRod = true;
        config.noBreak = true;
        config.persistentMode = true;
        config.useSoundDetection = true;
        config.forceMultiplayerDetection = true;
        config.runWhilePaused = false;

        AutoFishConfig copy = config.copy();

        assertTrue(copy.multiRod);
        assertTrue(copy.noBreak);
        assertTrue(copy.persistentMode);
        assertTrue(copy.useSoundDetection);
        assertTrue(copy.forceMultiplayerDetection);
        assertFalse(copy.requireSneaking);
        assertFalse(copy.runWhilePaused);
    }

    @Test
    void runsWhilePausedByDefault() {
        assertTrue(new AutoFishConfig().runWhilePaused);
    }
}
