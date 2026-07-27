package io.github.ximu.autofish.client.config;

public final class AutoFishConfig {
    public static final int CURRENT_VERSION = 3;

    public int version = CURRENT_VERSION;
    public boolean enabled = true;
    public boolean multiRod = false;
    public boolean noBreak = false;
    public boolean persistentMode = false;
    public boolean useSoundDetection = false;
    public boolean forceMultiplayerDetection = false;
    public int recastDelayMs = 1500;
    public String clearLagRegex = "\\[ClearLag\\] Removed [0-9]+ Entities!";
    public boolean showHud = true;
    public boolean requireSneaking = false;
    public boolean runWhilePaused = true;

    public void normalize() {
        version = CURRENT_VERSION;
        recastDelayMs = clamp(recastDelayMs, 500, 5000);
        if (clearLagRegex == null) {
            clearLagRegex = "";
        } else if (clearLagRegex.length() > 512) {
            clearLagRegex = clearLagRegex.substring(0, 512);
        }
    }

    public AutoFishConfig copy() {
        AutoFishConfig copy = new AutoFishConfig();
        copy.enabled = enabled;
        copy.multiRod = multiRod;
        copy.noBreak = noBreak;
        copy.persistentMode = persistentMode;
        copy.useSoundDetection = useSoundDetection;
        copy.forceMultiplayerDetection = forceMultiplayerDetection;
        copy.recastDelayMs = recastDelayMs;
        copy.clearLagRegex = clearLagRegex;
        copy.showHud = showHud;
        copy.requireSneaking = requireSneaking;
        copy.runWhilePaused = runWhilePaused;
        return copy;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
