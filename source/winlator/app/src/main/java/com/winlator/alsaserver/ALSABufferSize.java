package com.winlator.alsaserver;








public final class ALSABufferSize {

    public static final int DEFAULT_BURST = 256;

    private ALSABufferSize() {}


    public static int burstFrames(String property) {
        int burst;
        try {

            burst = Short.parseShort(property != null ? property.trim() : "");
        }
        catch (NumberFormatException e) {
            return DEFAULT_BURST;
        }
        return burst > 0 ? burst : DEFAULT_BURST;
    }









    public static int frames(int latencyMillis, int sampleRate, int burstFrames, int frameBytes) {
        int burst = burstFrames > 0 ? burstFrames : DEFAULT_BURST;

        int frames = Math.round(((latencyMillis * sampleRate) / 1000.0f) / (float)burst) * burst;
        if (frames < burst) frames = burst;
        if (frameBytes > 1 && frames % frameBytes != 0) frames += frameBytes - frames % frameBytes;
        return frames;
    }
}
