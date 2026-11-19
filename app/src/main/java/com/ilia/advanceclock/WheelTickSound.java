package com.ilia.advanceclock;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Subtle metallic "lock click" for wheel ticks. The sample is synthesised once
 * (noise through a band-pass body and a high-pass snap) and always played at
 * the same fixed volume, however fast the wheel spins.
 */
final class WheelTickSound {
    private static final int RATE = 44100;
    private static final float VOLUME = 0.35f;
    private static final long MIN_GAP_MS = 18;

    private static SoundPool pool;
    private static int soundId;
    private static boolean loaded;
    private static long lastPlay;

    private WheelTickSound() {}

    static synchronized void prepare(Context context) {
        if (pool != null) return;
        pool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((p, id, status) -> loaded = status == 0);
        try {
            File file = new File(context.getApplicationContext().getCacheDir(), "wheel_tick_v1.wav");
            if (!file.exists()) writeWav(file, synth());
            soundId = pool.load(file.getAbsolutePath(), 1);
        } catch (IOException ignored) {
            // Silent fallback; haptics still work.
        }
    }

    static synchronized void play() {
        if (pool == null || !loaded) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastPlay < MIN_GAP_MS) return;
        lastPlay = now;
        pool.play(soundId, VOLUME, VOLUME, 1, 0, 1f);
    }

    private static short[] synth() {
        int n = RATE * 30 / 1000;
        float[] out = new float[n];
        Random random = new Random(7);
        float[] body = biquad(bandPass(1800, 4f));
        float[] snap = biquad(highPass(5200, 0.8f));
        float b1 = 0, b2 = 0, a1 = 0, a2 = 0;
        float s1 = 0, s2 = 0, c1 = 0, c2 = 0;
        float peak = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            float noise = random.nextFloat() * 2f - 1f;
            float yb = body[0] * noise + body[1] * b1 + body[2] * b2 - body[3] * a1 - body[4] * a2;
            b2 = b1; b1 = noise; a2 = a1; a1 = yb;
            float ys = snap[0] * noise + snap[1] * s1 + snap[2] * s2 - snap[3] * c1 - snap[4] * c2;
            s2 = s1; s1 = noise; c2 = c1; c1 = ys;
            float v = yb * 0.9f * (float) Math.exp(-t / 0.007f)
                    + ys * 0.5f * (float) Math.exp(-t / 0.0018f);
            out[i] = v;
            peak = Math.max(peak, Math.abs(v));
        }
        short[] pcm = new short[n];
        float scale = peak > 0 ? 0.6f / peak : 0;
        for (int i = 0; i < n; i++) pcm[i] = (short) (out[i] * scale * 32767);
        return pcm;
    }

    /** Returns b0,b1,b2,a1,a2 normalised by a0. */
    private static float[] biquad(double[] c) {
        return new float[]{(float) (c[0] / c[3]), (float) (c[1] / c[3]), (float) (c[2] / c[3]),
                (float) (c[4] / c[3]), (float) (c[5] / c[3])};
    }

    private static double[] bandPass(double f, double q) {
        double w = 2 * Math.PI * f / RATE, alpha = Math.sin(w) / (2 * q);
        return new double[]{alpha, 0, -alpha, 1 + alpha, -2 * Math.cos(w), 1 - alpha};
    }

    private static double[] highPass(double f, double q) {
        double w = 2 * Math.PI * f / RATE, alpha = Math.sin(w) / (2 * q), cos = Math.cos(w);
        return new double[]{(1 + cos) / 2, -(1 + cos), (1 + cos) / 2, 1 + alpha, -2 * cos, 1 - alpha};
    }

    private static void writeWav(File file, short[] pcm) throws IOException {
        int dataLen = pcm.length * 2;
        byte[] b = new byte[44 + dataLen];
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buf.put("RIFF".getBytes()).putInt(36 + dataLen).put("WAVE".getBytes())
                .put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1)
                .putInt(RATE).putInt(RATE * 2).putShort((short) 2).putShort((short) 16)
                .put("data".getBytes()).putInt(dataLen);
        for (short s : pcm) buf.putShort(s);
        File tmp = new File(file.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(b);
        }
        if (!tmp.renameTo(file)) throw new IOException("rename failed");
    }
}
