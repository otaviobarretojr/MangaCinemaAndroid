package com.mangacinema.reader;

/** Shared deterministic scene timing for player and exported motion comic. */
public final class SceneTiming {
    public static final long FADE_MS = 440L;
    private SceneTiming() { }

    public static long motionMillis(SceneDetector.Scene scene, float speed) {
        if (scene == null || !Float.isFinite(speed) || speed <= 0f)
            throw new IllegalArgumentException("Scene and speed must be valid");
        long base = 2750L + (long) (1800f * Math.min(1.6f,
                (scene.endY - scene.startY) / 1150f));
        return Math.max(220L, Math.round(base / speed));
    }

    public static int frames(long motionMillis, int fps, boolean hasPrevious) {
        if (fps < 1 || motionMillis <= 0) throw new IllegalArgumentException("Invalid video timing");
        long duration = motionMillis + (hasPrevious ? FADE_MS : 0L);
        return (int) Math.max(1, Math.round(duration * fps / 1000d));
    }
}
