package com.ilia.advanceclock;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Subtle "tick" for wheel steps. The sample is synthesised once and always
 * played at the same fixed volume, however fast the wheel spins.
 */
final class WheelTickSound {
    private static final int RATE = 44100;
    private static final float VOLUME = 0.35f;
    private static final long MIN_GAP_MS = 30;

    private static SoundPool pool;
    private static int soundId;
    private static boolean loaded;
    private static long lastPlay;
    private static android.media.AudioManager audio;

    private WheelTickSound() {}

    static synchronized void prepare(Context context) {
        if (pool != null) return;
        audio = (android.media.AudioManager) context.getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        pool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((p, id, status) -> loaded = status == 0);
        try {
            File file = new File(context.getApplicationContext().getCacheDir(), "wheel_tick_v3.wav");
            if (!file.exists()) writeWav(file, synth());
            soundId = pool.load(file.getAbsolutePath(), 1);
        } catch (IOException ignored) {
            // Silent fallback; haptics still work.
        }
    }

    static synchronized void play() {
        if (pool == null || !loaded) return;
        // Stay quiet when the phone is silent/vibrate or media volume is muted.
        if (audio != null && (audio.getRingerMode() != android.media.AudioManager.RINGER_MODE_NORMAL
                || audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) == 0
                || audio.isStreamMute(android.media.AudioManager.STREAM_MUSIC))) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastPlay < MIN_GAP_MS) return;
        lastPlay = now;
        // One stream only, fixed volume: fast spins never stack into a louder sound.
        pool.play(soundId, VOLUME, VOLUME, 1, 0, 1f);
    }

    /** A clean, short "tick": a mid-high tone with a very fast decay, no noise. */
    private static short[] synth() {
        int n = RATE * 12 / 1000;
        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double attack = Math.min(1.0, t / 0.0003);
            double env = attack * Math.exp(-t / 0.0022);
            double v = Math.sin(2 * Math.PI * 3400 * t) * 0.8
                    + Math.sin(2 * Math.PI * 5200 * t) * 0.2;
            pcm[i] = (short) (v * env * 0.6 * 32767);
        }
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
