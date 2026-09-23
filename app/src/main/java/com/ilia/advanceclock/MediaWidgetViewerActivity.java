package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.InputStream;
import java.util.Locale;

public final class MediaWidgetViewerActivity extends Activity {
    private static final long HIDE_DELAY_MS = 3000L;
    private static final int SEEK_STEP_MS = 10_000;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private FrameLayout root;
    private LinearLayout controls;
    private SeekBar seekBar;
    private ImageButton playPause;
    private TextView positionLabel;
    private TextView durationLabel;
    private VideoView videoView;
    private MediaPlayer audioPlayer;

    private Uri uri;
    private String mime = "";
    private String name = "";
    private boolean prepared;
    private boolean userSeeking;

    private final Runnable hideControls = () -> {
        if (controls == null) return;
        controls.animate()
                .alpha(0f)
                .setDuration(220L)
                .withEndAction(() -> controls.setVisibility(View.INVISIBLE))
                .start();
    };

    private final Runnable updateProgress = new Runnable() {
        @Override public void run() {
            if (prepared && !userSeeking) {
                int duration = duration();
                int position = position();
                seekBar.setMax(Math.max(1, duration));
                seekBar.setProgress(Math.max(0, Math.min(duration, position)));
                positionLabel.setText(formatTime(position));
                durationLabel.setText(formatTime(duration));
                updatePlayIcon();
            }
            handler.postDelayed(this, 300L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String uriValue = getIntent().getStringExtra(
                MediaWidgetActionReceiver.EXTRA_URI);
        mime = getIntent().getStringExtra(
                MediaWidgetActionReceiver.EXTRA_MIME);
        name = getIntent().getStringExtra(
                MediaWidgetActionReceiver.EXTRA_NAME);

        if (uriValue == null || uriValue.trim().isEmpty()) {
            finish();
            return;
        }

        uri = Uri.parse(uriValue);
        if (mime == null) mime = "";
        if (name == null) name = "";

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        if (mime.toLowerCase(Locale.ROOT).startsWith("image/")) {
            showImage();
        } else if (mime.toLowerCase(Locale.ROOT).startsWith("video/")) {
            showVideo();
            addControls();
        } else if (mime.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            showAudio();
            addControls();
        } else {
            finish();
            return;
        }

        root.setOnClickListener(v -> {
            if (controls != null) {
                showControlsTemporarily();
            }
        });
    }

    private void showImage() {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAdjustViewBounds(true);
        root.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        new Thread(() -> {
            Bitmap bitmap = decodeHighQuality(uri);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (bitmap != null) image.setImageBitmap(bitmap);
            });
        }).start();
    }

    private void showVideo() {
        videoView = new VideoView(this);
        videoView.setVideoURI(uri);
        videoView.setOnClickListener(
                v -> showControlsTemporarily());
        videoView.setOnPreparedListener(mp -> {
            prepared = true;
            videoView.start();
            showControlsTemporarily();
        });
        videoView.setOnCompletionListener(mp -> {
            updatePlayIcon();
            showControlsTemporarily();
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            finish();
            return true;
        });
        root.addView(videoView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void showAudio() {
        ImageView art = new ImageView(this);
        art.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        art.setPadding(dp(24), dp(24), dp(24), dp(140));
        art.setImageResource(R.drawable.ic_note);
        root.addView(art, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        new Thread(() -> {
            Bitmap bitmap = embeddedArt();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (bitmap != null) art.setImageBitmap(bitmap);
            });
        }).start();

        try {
            audioPlayer = new MediaPlayer();
            audioPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
            audioPlayer.setDataSource(this, uri);
            audioPlayer.setOnPreparedListener(mp -> {
                prepared = true;
                mp.start();
                showControlsTemporarily();
            });
            audioPlayer.setOnCompletionListener(mp -> {
                updatePlayIcon();
                showControlsTemporarily();
            });
            audioPlayer.setOnErrorListener((mp, what, extra) -> {
                finish();
                return true;
            });
            audioPlayer.prepareAsync();
        } catch (Exception ignored) {
            finish();
        }
    }

    private void addControls() {
        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(14), dp(10), dp(14), dp(12));
        controls.setBackgroundResource(R.drawable.bg_media_controls);
        controls.setAlpha(1f);

        FrameLayout.LayoutParams controlsLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        controlsLp.setMargins(dp(10), dp(10), dp(10), dp(12));
        root.addView(controls, controlsLp);

        if (!name.trim().isEmpty()) {
            TextView title = new TextView(this);
            title.setText(name);
            title.setTextColor(Color.WHITE);
            title.setTextSize(13);
            title.setMaxLines(1);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            title.setGravity(Gravity.START);
            controls.addView(title, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(30)));
        }

        LinearLayout timeline = new LinearLayout(this);
        timeline.setOrientation(LinearLayout.HORIZONTAL);
        timeline.setGravity(Gravity.CENTER_VERTICAL);
        timeline.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        positionLabel = timeLabel("00:00");
        timeline.addView(positionLabel, new LinearLayout.LayoutParams(
                dp(48), dp(34)));

        seekBar = new SeekBar(this);
        timeline.addView(seekBar, new LinearLayout.LayoutParams(
                0, dp(38), 1f));

        durationLabel = timeLabel("00:00");
        durationLabel.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        timeline.addView(durationLabel, new LinearLayout.LayoutParams(
                dp(48), dp(34)));

        controls.addView(timeline);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        buttons.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        Button back = controlButton("−10 ث");
        back.setOnClickListener(v -> {
            seekTo(position() - SEEK_STEP_MS);
            showControlsTemporarily();
        });
        buttons.addView(back, new LinearLayout.LayoutParams(
                dp(76), dp(46)));

        playPause = new ImageButton(this);
        playPause.setImageResource(R.drawable.ic_md_pause);
        playPause.setColorFilter(Color.WHITE);
        playPause.setBackgroundResource(R.drawable.bg_soft_button);
        playPause.setContentDescription("پخش یا مکث");
        playPause.setPadding(dp(11), dp(11), dp(11), dp(11));
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(
                dp(50), dp(46));
        playLp.setMargins(dp(12), 0, dp(12), 0);
        buttons.addView(playPause, playLp);
        playPause.setOnClickListener(v -> {
            togglePlayback();
            showControlsTemporarily();
        });

        Button forward = controlButton("+10 ث");
        forward.setOnClickListener(v -> {
            seekTo(position() + SEEK_STEP_MS);
            showControlsTemporarily();
        });
        buttons.addView(forward, new LinearLayout.LayoutParams(
                dp(76), dp(46)));

        controls.addView(buttons);

        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {
                        if (fromUser) {
                            positionLabel.setText(formatTime(progress));
                        }
                    }

                    @Override public void onStartTrackingTouch(SeekBar seekBar) {
                        userSeeking = true;
                        handler.removeCallbacks(hideControls);
                    }

                    @Override public void onStopTrackingTouch(SeekBar seekBar) {
                        userSeeking = false;
                        seekTo(seekBar.getProgress());
                        showControlsTemporarily();
                    }
                });

        showControlsTemporarily();
        handler.post(updateProgress);
    }

    private TextView timeLabel(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.WHITE);
        text.setTextSize(11);
        text.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return text;
    }

    private Button controlButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setMinWidth(0);
        button.setPadding(dp(6), 0, dp(6), 0);
        button.setBackgroundResource(R.drawable.bg_soft_button);
        return button;
    }

    private void showControlsTemporarily() {
        if (controls == null) return;
        handler.removeCallbacks(hideControls);
        controls.setVisibility(View.VISIBLE);
        controls.animate().cancel();
        controls.animate().alpha(1f).setDuration(120L).start();
        handler.postDelayed(hideControls, HIDE_DELAY_MS);
    }

    private void togglePlayback() {
        if (!prepared) return;
        try {
            if (isPlaying()) {
                pause();
            } else {
                play();
            }
            updatePlayIcon();
        } catch (Exception ignored) {
        }
    }

    private boolean isPlaying() {
        if (videoView != null) return videoView.isPlaying();
        return audioPlayer != null && audioPlayer.isPlaying();
    }

    private void play() {
        if (videoView != null) {
            videoView.start();
        } else if (audioPlayer != null) {
            audioPlayer.start();
        }
    }

    private void pause() {
        if (videoView != null) {
            videoView.pause();
        } else if (audioPlayer != null) {
            audioPlayer.pause();
        }
    }

    private int position() {
        try {
            if (videoView != null) return videoView.getCurrentPosition();
            if (audioPlayer != null && prepared) return audioPlayer.getCurrentPosition();
        } catch (Exception ignored) {
        }
        return 0;
    }

    private int duration() {
        try {
            if (videoView != null && prepared) return Math.max(0, videoView.getDuration());
            if (audioPlayer != null && prepared) return Math.max(0, audioPlayer.getDuration());
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void seekTo(int targetMs) {
        if (!prepared) return;
        int clamped = Math.max(0, Math.min(duration(), targetMs));
        try {
            if (videoView != null) {
                videoView.seekTo(clamped);
            } else if (audioPlayer != null) {
                audioPlayer.seekTo(clamped);
            }
        } catch (Exception ignored) {
        }
    }

    private void updatePlayIcon() {
        if (playPause == null) return;
        playPause.setImageResource(
                isPlaying()
                        ? R.drawable.ic_md_pause
                        : R.drawable.ic_md_play);
    }

    private Bitmap embeddedArt() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            byte[] data = retriever.getEmbeddedPicture();
            if (data == null || data.length == 0) return null;
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception ignored) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private Bitmap decodeHighQuality(Uri imageUri) {
        try {
            int targetWidth = Math.max(
                    getResources().getDisplayMetrics().widthPixels * 2,
                    1080);
            int targetHeight = Math.max(
                    getResources().getDisplayMetrics().heightPixels * 2,
                    1920);

            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.Source source =
                        ImageDecoder.createSource(
                                getContentResolver(),
                                imageUri);
                return ImageDecoder.decodeBitmap(
                        source,
                        (decoder, info, src) -> {
                            int width = info.getSize().getWidth();
                            int height = info.getSize().getHeight();
                            float scale = Math.min(
                                    1f,
                                    Math.min(
                                            (float) targetWidth / Math.max(1, width),
                                            (float) targetHeight / Math.max(1, height)));
                            if (scale < 1f) {
                                decoder.setTargetSize(
                                        Math.max(1, Math.round(width * scale)),
                                        Math.max(1, Math.round(height * scale)));
                            }
                            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        });
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream first =
                         getContentResolver().openInputStream(imageUri)) {
                if (first == null) return null;
                BitmapFactory.decodeStream(first, null, bounds);
            }

            int sample = 1;
            while (bounds.outWidth / sample > targetWidth
                    || bounds.outHeight / sample > targetHeight) {
                sample *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, sample);
            try (InputStream second =
                         getContentResolver().openInputStream(imageUri)) {
                if (second == null) return null;
                return BitmapFactory.decodeStream(second, null, options);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatTime(int millis) {
        int totalSeconds = Math.max(0, millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes >= 60) {
            int hours = minutes / 60;
            minutes %= 60;
            return String.format(
                    Locale.getDefault(),
                    "%d:%02d:%02d",
                    hours,
                    minutes,
                    seconds);
        }
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds);
    }

    @Override protected void onPause() {
        super.onPause();
        if (prepared && isPlaying()) {
            pause();
            updatePlayIcon();
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (videoView != null) {
            try {
                videoView.stopPlayback();
            } catch (Exception ignored) {
            }
        }
        if (audioPlayer != null) {
            try {
                audioPlayer.release();
            } catch (Exception ignored) {
            }
            audioPlayer = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density);
    }
}
