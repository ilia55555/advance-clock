package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class PrayerSettingsActivity extends Activity {
    private static final int REQ_LOCATION = 740;
    private static final int REQ_NOTIFICATIONS = 741;
    private static final int REQ_HORIZON = 742;
    private static final int REQ_ADHAN_AUDIO = 743;

    private Button locationButton;
    private TextView locationStatus;
    private TextView adhanScheduleStatus;
    private LinearLayout horizonList;
    private final Handler locationHandler = new Handler(Looper.getMainLooper());
    private CancellationSignal locationCancellation;
    private Runnable locationTimeout;
    private LocationManager activeLocationManager;
    private LocationListener activeLocationListener;
    private int locationRequestGeneration;
    private int activeAdhanType = -1;
    private LinearLayout activeMuezzinList;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(12), dp(18), dp(24));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText(AppString.get(R.string.runtime_text_0076));
        title.setTextSize(24);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(top);

        LinearLayout masterCard = card();
        TextView masterTitle = text(
                AppString.get(R.string.runtime_text_0086),
                18,
                AppSettings.textPrimary(this));
        masterTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        masterCard.addView(masterTitle);

        Switch master = toggle(
                AppString.get(R.string.runtime_text_0351),
                AppSettings.adhanEnabled(this));
        master.setOnCheckedChangeListener((button, checked) -> {
            AppSettings.setAdhanEnabled(this, checked);
            setResult(RESULT_OK);
        });
        masterCard.addView(master);
        root.addView(masterCard, cardParams());

        LinearLayout locationCard = card();
        TextView locationTitle = text(
                AppString.get(R.string.runtime_text_0352),
                17,
                AppSettings.textPrimary(this));
        locationTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        locationCard.addView(locationTitle);

        locationStatus = text("", 12, AppSettings.textSecondary(this));
        locationStatus.setPadding(0, dp(4), 0, dp(8));
        locationCard.addView(locationStatus);

        Button searchLocation = fieldButton(AppString.get(R.string.runtime_text_0353));
        searchLocation.setOnClickListener(v -> startActivityForResult(
                new Intent(this, PrayerLocationSearchActivity.class), REQ_HORIZON));
        locationCard.addView(searchLocation, new LinearLayout.LayoutParams(-1, dp(52)));

        locationButton = fieldButton(AppString.get(R.string.runtime_text_0089));
        horizonList = new LinearLayout(this);
        horizonList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams horizonParams = new LinearLayout.LayoutParams(-1, -2);
        horizonParams.topMargin = dp(8);
        locationCard.addView(horizonList, horizonParams);

        root.addView(locationCard, cardParams());

        LinearLayout azanCard = card();
        TextView azanTitle = text(AppString.get(R.string.runtime_text_0342), 17, AppSettings.textPrimary(this));
        azanTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        azanCard.addView(azanTitle);
        addAdhanSettingRow(azanCard, AppString.get(R.string.runtime_text_0343), 899);
        addAdhanSettingRow(azanCard, AppString.get(R.string.runtime_text_0078), AdhanScheduler.FAJR);
        addAdhanSettingRow(azanCard, AppString.get(R.string.runtime_text_0079), AdhanScheduler.DHUHR);
        addAdhanSettingRow(azanCard, AppString.get(R.string.runtime_text_0082), AdhanScheduler.ASR);
        addAdhanSettingRow(azanCard, AppString.get(R.string.runtime_text_0080), AdhanScheduler.MAGHRIB);
        addAdhanSettingRow(azanCard, AppString.get(R.string.runtime_text_0081), AdhanScheduler.ISHA);

        adhanScheduleStatus = text(
                "",
                11,
                AppSettings.textSecondary(this));
        adhanScheduleStatus.setPadding(0, dp(6), 0, 0);
        adhanScheduleStatus.setOnClickListener(v -> resolveAdhanScheduleStatus());
        azanCard.addView(adhanScheduleStatus);

        root.addView(azanCard, cardParams());
        Button preview = fieldButton(AppString.get(R.string.runtime_text_0096));
        preview.setOnClickListener(v -> previewAdhan());
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(54)));

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        boolean permissionGranted = PermissionHelper.exactAlarmsGranted(this);
        if (permissionGranted
                && AdhanScheduler.status(this) == AdhanScheduler.Status.SCHEDULE_FAILED) {
            AdhanScheduler.rescheduleAll(this);
        }
        refresh();
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private void refresh() {
        refreshAdhanScheduleStatus();
        boolean hasLocation = AppSettings.prayerLocationSet(this);
        if (hasLocation) {
            String label = AppSettings.prayerLocationLabel(this);
            locationStatus.setText(
                    label
                            + "\n"
                            + coordinateText(
                            AppSettings.prayerLatitude(this),
                            AppSettings.prayerLongitude(this)));
            locationButton.setText(AppString.get(R.string.runtime_text_0372));
        } else {
            locationStatus.setText(AppString.get(R.string.runtime_text_0097));
            locationButton.setText(AppString.get(R.string.runtime_text_0089));
        }
        renderHorizons();

    }

    private void renderHorizons() {
        if (horizonList == null) return;
        horizonList.removeAllViews();
        List<AppSettings.PrayerHorizon> horizons = AppSettings.prayerHorizons(this);
        if (horizons.isEmpty()) {
            TextView empty = text(AppString.get(R.string.runtime_text_0354), 12,
                    AppSettings.textSecondary(this));
            empty.setPadding(dp(10), dp(14), dp(10), dp(14));
            horizonList.addView(empty);
            return;
        }

        for (AppSettings.PrayerHorizon horizon : horizons) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            row.setBackgroundResource(R.drawable.bg_field);
            row.setPadding(dp(12), 0, dp(8), 0);

            TextView label = text(horizon.label, 13, AppSettings.textPrimary(this));
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(label, new LinearLayout.LayoutParams(0, dp(54), 1f));

            boolean isPrimary = AppSettings.isPrimaryPrayerHorizon(
                    this, horizon.latitude, horizon.longitude);
            Button primary = fieldButton(isPrimary ? AppString.get(R.string.runtime_text_0356) : AppString.get(R.string.runtime_text_0565));
            primary.setGravity(Gravity.CENTER);
            primary.setEnabled(!isPrimary);
            primary.setContentDescription(
                    isPrimary ? AppString.get(R.string.runtime_text_0358) + horizon.label
                            : AppString.get(R.string.runtime_text_0566) + horizon.label + AppString.get(R.string.runtime_text_0567));
            primary.setOnClickListener(v -> {
                AppSettings.setPrimaryPrayerHorizon(
                        this,
                        horizon.latitude,
                        horizon.longitude,
                        horizon.label,
                        horizon.timeZoneId);
                setResult(RESULT_OK);
                refresh();
            });
            LinearLayout.LayoutParams primaryParams =
                    new LinearLayout.LayoutParams(dp(94), dp(44));
            primaryParams.setMarginStart(dp(6));
            row.addView(primary, primaryParams);

            Button remove = new Button(this);
            remove.setText("−");
            remove.setAllCaps(false);
            remove.setTextSize(22);
            remove.setGravity(Gravity.CENTER);
            remove.setPadding(0, 0, 0, dp(2));
            remove.setTextColor(0xFFD32F2F);
            remove.setBackground(deleteHorizonBackground(false));
            remove.setContentDescription(AppString.get(R.string.runtime_text_0509) + horizon.label);

            final boolean[] armed = {false};
            remove.setOnClickListener(v -> {
                if (!armed[0]) {
                    armed[0] = true;
                    remove.setTextColor(0xFFFFFFFF);
                    remove.setBackground(deleteHorizonBackground(true));
                    return;
                }
                AppSettings.removePrayerHorizon(
                        this, horizon.latitude, horizon.longitude);
                setResult(RESULT_OK);
                refresh();
            });

            LinearLayout.LayoutParams removeParams =
                    new LinearLayout.LayoutParams(dp(44), dp(44));
            removeParams.setMarginStart(dp(6));
            row.addView(remove, removeParams);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(58));
            params.bottomMargin = dp(6);
            horizonList.addView(row, params);
        }
    }

    private GradientDrawable deleteHorizonBackground(boolean armed) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(6));
        background.setStroke(dp(1), 0xFFD32F2F);
        background.setColor(armed ? 0xFFD32F2F : 0x00000000);
        return background;
    }

    private void updateAdhanSetting(Runnable update, boolean enabled) {
        update.run();
        refreshAdhanScheduleStatus();
        if (enabled && AdhanScheduler.status(this) != AdhanScheduler.Status.SCHEDULED) {
            resolveAdhanScheduleStatus();
        }
    }

    private void refreshAdhanScheduleStatus() {
        if (adhanScheduleStatus == null) return;
        AdhanScheduler.Status status = AdhanScheduler.status(this);
        String message;
        switch (status) {
            case DISABLED:
                message = AppString.get(R.string.runtime_text_0366);
                break;
            case LOCATION_MISSING:
                message = AppString.get(R.string.runtime_text_0362);
                break;
            case EXACT_PERMISSION_MISSING:
                message = AppString.get(R.string.runtime_text_0363);
                break;
            case NOTIFICATION_PERMISSION_MISSING:
                message = AppString.get(R.string.runtime_text_0364);
                break;
            case SCHEDULE_FAILED:
                message = AppString.get(R.string.runtime_text_0365);
                break;
            case SCHEDULED:
            default:
                message = AppString.get(R.string.runtime_text_0355);
                break;
        }
        adhanScheduleStatus.setText(message);
        boolean problem = status != AdhanScheduler.Status.SCHEDULED
                && status != AdhanScheduler.Status.DISABLED;
        adhanScheduleStatus.setTextColor(problem
                ? 0xFFC44C4C : AppSettings.textSecondary(this));
        adhanScheduleStatus.setClickable(problem);
    }

    private void resolveAdhanScheduleStatus() {
        AdhanScheduler.Status status = AdhanScheduler.status(this);
        if (status == AdhanScheduler.Status.LOCATION_MISSING) {
            requestPreciseLocation();
        } else if (status == AdhanScheduler.Status.EXACT_PERMISSION_MISSING) {
            try {
                startActivity(new Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {
            }
        } else if (status == AdhanScheduler.Status.NOTIFICATION_PERMISSION_MISSING
                && Build.VERSION.SDK_INT >= 33) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_NOTIFICATIONS);
        } else if (status == AdhanScheduler.Status.SCHEDULE_FAILED) {
            AdhanScheduler.rescheduleAll(this);
            refreshAdhanScheduleStatus();
        }
    }

    private void addAdhanSettingRow(LinearLayout parent, String title, int type) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(dp(10), dp(4), dp(10), dp(4));
        row.setBackgroundResource(R.drawable.bg_field);

        TextView name = text(title, 13, AppSettings.textPrimary(this));
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setSingleLine(true);
        row.addView(name, new LinearLayout.LayoutParams(dp(88), dp(58)));

        TextView summary = text(adhanSettingSummary(type), 10,
                AppSettings.textSecondary(this));
        summary.setSingleLine(true);
        summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(summary, new LinearLayout.LayoutParams(0, dp(58), 1f));

        Button configure = fieldButton(AppString.get(R.string.runtime_text_0001));
        configure.setGravity(Gravity.CENTER);
        configure.setOnClickListener(v -> showAdhanSettingsDialog(title, type));
        LinearLayout.LayoutParams configureParams =
                new LinearLayout.LayoutParams(dp(76), dp(44));
        configureParams.setMarginStart(dp(6));
        row.addView(configure, configureParams);

        Switch enabled = new Switch(this);
        enabled.setChecked(adhanTypeEnabled(type));
        enabled.setContentDescription(AppString.get(R.string.runtime_text_0350) + title);
        enabled.setOnCheckedChangeListener((button, checked) ->
                updateAdhanSetting(() -> setAdhanTypeEnabled(type, checked), checked));
        row.addView(enabled, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(66));
        rowParams.topMargin = dp(7);
        parent.addView(row, rowParams);
    }

    private String adhanSettingSummary(int type) {
        if (type == 899) {
            String firstSound = AppSettings.adhanSoundUri(this, AdhanScheduler.FAJR);
            int firstVolume = AppSettings.adhanVolume(this, AdhanScheduler.FAJR);
            boolean same = true;
            for (int item = AdhanScheduler.DHUHR; item <= AdhanScheduler.ISHA; item++) {
                if (!firstSound.equals(AppSettings.adhanSoundUri(this, item))
                        || firstVolume != AppSettings.adhanVolume(this, item)) {
                    same = false;
                    break;
                }
            }
            return same
                    ? SoundLibrary.name(this, firstSound) + " • " + firstVolume + AppString.get(R.string.runtime_text_0568)
                    : AppString.get(R.string.runtime_text_0344);
        }
        return SoundLibrary.name(this, AppSettings.adhanSoundUri(this, type))
                + " • " + AppSettings.adhanVolume(this, type) + AppString.get(R.string.runtime_text_0568);
    }

    private boolean adhanTypeEnabled(int type) {
        if (type == 899) return AppSettings.adhanEnabled(this)
                && AppSettings.fajrAdhanEnabled(this)
                && AppSettings.dhuhrAdhanEnabled(this)
                && AppSettings.asrAdhanEnabled(this)
                && AppSettings.maghribAdhanEnabled(this)
                && AppSettings.ishaAdhanEnabled(this);
        if (type == AdhanScheduler.FAJR) return AppSettings.fajrAdhanEnabled(this);
        if (type == AdhanScheduler.DHUHR) return AppSettings.dhuhrAdhanEnabled(this);
        if (type == AdhanScheduler.ASR) return AppSettings.asrAdhanEnabled(this);
        if (type == AdhanScheduler.MAGHRIB) return AppSettings.maghribAdhanEnabled(this);
        return AppSettings.ishaAdhanEnabled(this);
    }

    private void setAdhanTypeEnabled(int type, boolean enabled) {
        if (type == 899) {
            AppSettings.setAdhanEnabled(this, enabled);
            AppSettings.setFajrAdhanEnabled(this, enabled);
            AppSettings.setDhuhrAdhanEnabled(this, enabled);
            AppSettings.setAsrAdhanEnabled(this, enabled);
            AppSettings.setMaghribAdhanEnabled(this, enabled);
            AppSettings.setIshaAdhanEnabled(this, enabled);
            return;
        }
        if (enabled && !AppSettings.adhanEnabled(this)) {
            AppSettings.setAdhanEnabled(this, true);
        }
        if (type == AdhanScheduler.FAJR) AppSettings.setFajrAdhanEnabled(this, enabled);
        else if (type == AdhanScheduler.DHUHR) AppSettings.setDhuhrAdhanEnabled(this, enabled);
        else if (type == AdhanScheduler.ASR) AppSettings.setAsrAdhanEnabled(this, enabled);
        else if (type == AdhanScheduler.MAGHRIB) AppSettings.setMaghribAdhanEnabled(this, enabled);
        else AppSettings.setIshaAdhanEnabled(this, enabled);
    }

    private void showAdhanSettingsDialog(String title, int type) {
        Dialog dialog = new Dialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(10), dp(18), dp(18));
        root.setBackgroundColor(AppSettings.background(this));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView heading = text(AppString.get(R.string.runtime_text_0569) + title, 20, AppSettings.textPrimary(this));
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        header.addView(heading, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(header);

        LinearLayout output = card();
        TextView outputTitle = text(AppString.get(R.string.runtime_text_0345), 16,
                AppSettings.textPrimary(this));
        outputTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        output.addView(outputTitle);

        int initialVolume = AppSettings.adhanVolume(
                this, type == 899 ? AdhanScheduler.FAJR : type);
        TextView volumeLabel = text(AppString.get(R.string.runtime_text_0348) + initialVolume + AppString.get(R.string.runtime_text_0568), 13,
                AppSettings.textSecondary(this));
        volumeLabel.setPadding(0, dp(8), 0, 0);
        output.addView(volumeLabel);
        SeekBar volume = new SeekBar(this);
        volume.setMax(100);
        volume.setProgress(initialVolume);
        volume.setContentDescription(AppString.get(R.string.runtime_text_0349) + title);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser) {
                volumeLabel.setText(AppString.get(R.string.runtime_text_0348) + progress + AppString.get(R.string.runtime_text_0568));
                if (!fromUser) return;
                if (type == 899) {
                    for (int item = AdhanScheduler.FAJR;
                            item <= AdhanScheduler.ISHA; item++) {
                        AppSettings.setAdhanVolume(PrayerSettingsActivity.this, item, progress);
                    }
                } else {
                    AppSettings.setAdhanVolume(PrayerSettingsActivity.this, type, progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        output.addView(volume, new LinearLayout.LayoutParams(-1, dp(48)));

        Switch fullscreenUnlocked = toggle(AppString.get(R.string.runtime_text_0210),
                AppSettings.adhanFullscreenUnlocked(this));
        fullscreenUnlocked.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setAdhanFullscreenUnlocked(this, checked));
        Switch fullscreenLocked = toggle(AppString.get(R.string.runtime_text_0211),
                AppSettings.adhanFullscreenLocked(this));
        fullscreenLocked.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setAdhanFullscreenLocked(this, checked));
        Switch notification = toggle(AppString.get(R.string.runtime_text_0093), AppSettings.adhanNotification(this));
        notification.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setAdhanNotification(this, checked));
        Switch vibrate = toggle(AppString.get(R.string.runtime_text_0094), AppSettings.adhanVibrate(this));
        vibrate.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setAdhanVibrate(this, checked));
        Switch sound = toggle(AppString.get(R.string.runtime_text_0095), AppSettings.adhanSound(this));
        sound.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setAdhanSound(this, checked));
        output.addView(fullscreenUnlocked);
        output.addView(fullscreenLocked);
        output.addView(notification);
        output.addView(vibrate);
        output.addView(sound);
        root.addView(output, cardParams());

        LinearLayout muezzin = card();
        TextView muezzinTitle = text(AppString.get(R.string.runtime_text_0346), 16, AppSettings.textPrimary(this));
        muezzinTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        muezzin.addView(muezzinTitle);
        Button upload = fieldButton(AppString.get(R.string.runtime_text_0347));
        upload.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        upload.setPadding(dp(16), 0, dp(16), 0);
        upload.setOnClickListener(v -> uploadAdhanAudio(type));
        LinearLayout.LayoutParams uploadParams = new LinearLayout.LayoutParams(-1, dp(50));
        uploadParams.topMargin = dp(7);
        muezzin.addView(upload, uploadParams);

        ScrollView soundScroll = new ScrollView(this);
        soundScroll.setFillViewport(true);
        soundScroll.setClipToOutline(true);
        soundScroll.setBackground(soundListBackground());
        soundScroll.setPadding(dp(8), dp(8), dp(8), dp(8));
        activeMuezzinList = new LinearLayout(this);
        activeMuezzinList.setOrientation(LinearLayout.VERTICAL);
        activeMuezzinList.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        soundScroll.addView(activeMuezzinList, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams soundScrollParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        soundScrollParams.topMargin = dp(10);
        muezzin.addView(soundScroll, soundScrollParams);
        root.addView(muezzin, new LinearLayout.LayoutParams(-1, 0, 1f));
        activeAdhanType = type;
        renderMuezzinList(type);

        dialog.setContentView(root);
        dialog.setOnDismissListener(value -> {
            activeAdhanType = -1;
            activeMuezzinList = null;
            recreate();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(-1, -1);
            dialog.getWindow().setStatusBarColor(AppSettings.primaryColor(this));
            dialog.getWindow().setNavigationBarColor(AppSettings.background(this));
        }
        AppSettings.applyFullscreenInsets(root);
    }

    private void renderMuezzinList(int type) {
        if (activeMuezzinList == null) return;
        activeMuezzinList.removeAllViews();
        String selected = AppSettings.adhanSoundUri(
                this, type == 899 ? AdhanScheduler.FAJR : type);
        for (SoundLibrary.Sound item : SoundLibrary.all(this)) {
            boolean checked = item.uri.equals(selected);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), 0, dp(12), 0);
            row.setBackground(soundRowBackground(checked));
            TextView name = text(item.name, 14, checked
                    ? AppSettings.primaryColor(this) : AppSettings.textPrimary(this));
            if (checked) name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.addView(name, new LinearLayout.LayoutParams(0, -1, 1f));
            ImageView check = new ImageView(this);
            check.setImageResource(R.drawable.ic_md_check);
            check.setColorFilter(AppSettings.primaryColor(this), PorterDuff.Mode.SRC_IN);
            check.setVisibility(checked ? View.VISIBLE : View.INVISIBLE);
            check.setContentDescription(checked ? AppString.get(R.string.runtime_text_0341) : null);
            row.addView(check, new LinearLayout.LayoutParams(dp(24), dp(24)));
            row.setOnClickListener(v -> {
                setAdhanSound(type, item.uri);
                renderMuezzinList(type);
                setResult(RESULT_OK);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(54));
            params.bottomMargin = dp(6);
            activeMuezzinList.addView(row, params);
        }
    }

    private void setAdhanSound(int type, String uri) {
        if (type == 899) {
            for (int item = AdhanScheduler.FAJR; item <= AdhanScheduler.ISHA; item++) {
                AppSettings.setAdhanSoundUri(this, item, uri);
            }
        } else {
            AppSettings.setAdhanSoundUri(this, type, uri);
        }
    }

    private void uploadAdhanAudio(int type) {
        activeAdhanType = type;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("audio/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_ADHAN_AUDIO);
    }

    private GradientDrawable soundListBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(AppSettings.surface(this));
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), AppSettings.field(this));
        return background;
    }

    private GradientDrawable soundRowBackground(boolean checked) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(checked ? (AppSettings.primaryColor(this) & 0x00FFFFFF) | 0x18000000
                : AppSettings.field(this));
        background.setCornerRadius(dp(9));
        if (checked) background.setStroke(dp(1), AppSettings.primaryColor(this));
        return background;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADHAN_AUDIO) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null
                    || activeAdhanType == -1) return;
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            SoundLibrary.add(this, uri);
            setAdhanSound(activeAdhanType, uri.toString());
            renderMuezzinList(activeAdhanType);
            setResult(RESULT_OK);
            return;
        }
        if (requestCode == REQ_HORIZON) {
            if (resultCode == RESULT_OK && data != null
                    && data.getBooleanExtra("requestGps", false)) requestPreciseLocation();
            refresh();
            return;
        }
        if ((requestCode < 800 || requestCode > 804) && requestCode != 899
                || resultCode != RESULT_OK || data == null) return;
        String uri = data.getStringExtra(SoundPickerActivity.EXTRA_URI);
        if (requestCode == 899) {
            for (int type = AdhanScheduler.FAJR; type <= AdhanScheduler.ISHA; type++)
                AppSettings.setAdhanSoundUri(this, type, uri);
        } else AppSettings.setAdhanSoundUri(this, requestCode - 800, uri);
        recreate();
    }

    private void previewAdhan() {
        Intent service = AdhanSoundService.startIntent(this, AdhanScheduler.FAJR);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
        else startService(service);
        startActivity(new Intent(this, AdhanRingActivity.class)
                .putExtra("adhanType", AdhanScheduler.FAJR));
    }

    private void requestPreciseLocation() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQ_LOCATION);
            return;
        }
        fetchCurrentLocation();
    }

    @Override public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS) {
            refreshAdhanScheduleStatus();
            return;
        }
        if (requestCode != REQ_LOCATION) return;

        boolean granted = false;
        for (int value : grantResults) {
            if (value == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }
        if (granted) {
            fetchCurrentLocation();
        } else {
            LogoToast.makeText(
                    this,
                    AppString.get(R.string.runtime_text_0570),
                    Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("deprecation")
    private void fetchCurrentLocation() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager manager =
                (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            LogoToast.makeText(this, AppString.get(R.string.runtime_text_0367), Toast.LENGTH_SHORT).show();
            return;
        }

        String provider = null;
        try {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                provider = LocationManager.GPS_PROVIDER;
            } else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                provider = LocationManager.NETWORK_PROVIDER;
            }
        } catch (Exception ignored) {}

        if (provider == null) {
            LogoToast.makeText(
                    this,
                    AppString.get(R.string.runtime_text_0368),
                    Toast.LENGTH_LONG).show();
            return;
        }

        locationButton.setEnabled(false);
        locationButton.setText(AppString.get(R.string.runtime_text_0098));

        final String selectedProvider = provider;
        final int requestGeneration = ++locationRequestGeneration;
        activeLocationManager = manager;
        locationTimeout = () -> {
            if (requestGeneration != locationRequestGeneration) return;
            cancelLocationRequest();
            locationButton.setEnabled(true);
            refresh();
            LogoToast.makeText(
                    this,
                    AppString.get(R.string.runtime_text_0571),
                    Toast.LENGTH_LONG).show();
        };
        locationHandler.postDelayed(locationTimeout, 20_000L);
        if (Build.VERSION.SDK_INT >= 30) {
            locationCancellation = new CancellationSignal();
            try {
                manager.getCurrentLocation(
                        selectedProvider,
                        locationCancellation,
                        command -> runOnUiThread(command),
                        location -> {
                            if (requestGeneration != locationRequestGeneration) return;
                            clearLocationTimeout();
                            handleLocation(location, requestGeneration);
                        });
            } catch (RuntimeException error) {
                failLocationRequest(requestGeneration);
            }
            return;
        }

        activeLocationListener = new LocationListener() {
                    @Override public void onLocationChanged(Location location) {
                        if (requestGeneration != locationRequestGeneration) return;
                        handleLocation(location, requestGeneration);
                    }

                    @Override public void onStatusChanged(
                            String provider, int status, Bundle extras) {}

                    @Override public void onProviderEnabled(String provider) {}

                    @Override public void onProviderDisabled(String provider) {}
                };
        try {
            manager.requestSingleUpdate(
                    selectedProvider, activeLocationListener, Looper.getMainLooper());
        } catch (RuntimeException error) {
            failLocationRequest(requestGeneration);
        }
    }

    private void failLocationRequest(int requestGeneration) {
        if (requestGeneration != locationRequestGeneration) return;
        cancelLocationRequest();
        locationButton.setEnabled(true);
        refresh();
        LogoToast.makeText(
                this,
                AppString.get(R.string.runtime_text_0369),
                Toast.LENGTH_LONG).show();
    }

    private void handleLocation(Location location, int requestGeneration) {
        clearLocationTimeout();
        if (location == null) {
            locationButton.setEnabled(true);
            refresh();
            LogoToast.makeText(
                    this,
                    AppString.get(R.string.runtime_text_0370),
                    Toast.LENGTH_LONG).show();
            return;
        }

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        String fallback = coordinateText(lat, lon);
        AppSettings.setPrayerLocation(this, lat, lon, fallback);
        locationButton.setEnabled(true);
        refresh();
        setResult(RESULT_OK);

        new Thread(() -> resolveLocationName(
                lat, lon, fallback, requestGeneration)).start();
    }

    private void clearLocationTimeout() {
        if (locationTimeout != null) locationHandler.removeCallbacks(locationTimeout);
        locationTimeout = null;
        locationCancellation = null;
        if (activeLocationManager != null && activeLocationListener != null) {
            activeLocationManager.removeUpdates(activeLocationListener);
        }
        activeLocationManager = null;
        activeLocationListener = null;
    }

    private void cancelLocationRequest() {
        locationRequestGeneration++;
        if (locationCancellation != null) locationCancellation.cancel();
        clearLocationTimeout();
    }

    @Override protected void onDestroy() {
        cancelLocationRequest();
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    private void resolveLocationName(
            double lat, double lon, String fallback, int requestGeneration) {
        String label = fallback;
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);
                String locality = firstNotEmpty(
                        a.getLocality(),
                        a.getSubAdminArea(),
                        a.getAdminArea(),
                        a.getCountryName());
                if (locality != null) label = locality;
            }
        } catch (IOException | RuntimeException ignored) {}

        final String finalLabel = label;
        runOnUiThread(() -> {
            if (requestGeneration != locationRequestGeneration || isFinishing()
                    || (Build.VERSION.SDK_INT >= 17 && isDestroyed())) {
                return;
            }
            AppSettings.setPrayerLocation(this, lat, lon, finalLabel);
            refresh();
        });
    }

    private String firstNotEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }

    private String coordinateText(double lat, double lon) {
        return String.format(
                Locale.US,
                AppString.get(R.string.runtime_text_0572),
                lat,
                lon);
    }

    private Switch toggle(String value, boolean checked) {
        Switch control = new Switch(this);
        control.setText(value);
        control.setTextColor(AppSettings.textPrimary(this));
        control.setTextSize(14);
        control.setChecked(checked);
        control.setPadding(0, dp(3), 0, dp(3));
        return control;
    }

    private Button fieldButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(AppSettings.primaryColor(this));
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setBackgroundResource(R.drawable.bg_field);
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        return params;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
