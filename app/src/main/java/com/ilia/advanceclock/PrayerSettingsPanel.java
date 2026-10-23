package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
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
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

final class PrayerSettingsPanel {
    private static final int REQ_LOCATION = 740;
    private static final int REQ_NOTIFICATIONS = 741;
    private static final int REQ_HORIZON = 742;
    private static final int REQ_ADHAN_AUDIO = 743;

    private Button locationButton;
    private TextView locationStatus;
    private TextView locationAccessStatus;
    private Button locationPermissionAction;
    private Button locationGpsAction;
    private Button locationRetryAction;
    private int locationAccessErrorRes;
    private TextView adhanScheduleStatus;
    private LinearLayout horizonList;
    private final Handler locationHandler = new Handler(Looper.getMainLooper());
    private CancellationSignal locationCancellation;
    private Runnable locationTimeout;
    private LocationManager activeLocationManager;
    private LocationListener activeLocationListener;
    private int locationRequestGeneration;
    private QiblaLocationClient qiblaLocationClient;
    private int activeAdhanType = -1;
    private LinearLayout activeMuezzinList;
    private Switch adhanFeatureSwitch;
    private Switch masterAdhanSwitch;
    private Switch allAdhanSwitch;
    private final Switch[] adhanTypeSwitches = new Switch[5];
    private boolean syncingAdhanSwitches;
    private LinearLayout activeDetails;
    private ImageView activeDetailsArrow;
    private final java.util.Map<Integer, TextView> summaries = new java.util.HashMap<>();

    private final Activity host;

    PrayerSettingsPanel(Activity host, LinearLayout root) {
        this.host = host;
        qiblaLocationClient = new QiblaLocationClient(host);
        adhanFeatureSwitch = toggle(AppString.get(R.string.runtime_text_0361), AppSettings.adhanEnabled(host));
        adhanFeatureSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (syncingAdhanSwitches) return;
            AppSettings.setAdhanEnabled(host, checked);
            host.setResult(Activity.RESULT_OK);
            refresh();
            AdvanceClockApplication.refreshOpenActivities(host, false);
        });
        root.addView(adhanFeatureSwitch, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout masterCard = card();
        masterAdhanSwitch = toggle(
                AppString.get(R.string.runtime_text_0351),
                AppSettings.showPrayerTimes(host));
        masterAdhanSwitch.setTextSize(18);
        masterAdhanSwitch.setTextColor(AppSettings.textPrimary(host));
        masterAdhanSwitch.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        masterAdhanSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (syncingAdhanSwitches) return;
            AppSettings.setShowPrayerTimes(host, checked);
            host.setResult(Activity.RESULT_OK);
        });
        masterCard.addView(masterAdhanSwitch);
        root.addView(masterCard, cardParams());

        LinearLayout locationCard = card();
        TextView locationTitle = text(
                AppString.get(R.string.runtime_text_0352),
                17,
                AppSettings.textPrimary(host));
        locationTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        locationCard.addView(locationTitle);

        locationStatus = text("", 12, AppSettings.textSecondary(host));
        locationStatus.setPadding(0, dp(4), 0, dp(8));
        locationCard.addView(locationStatus);

        TextView accessTitle = text(
                AppString.get(R.string.location_access_title),
                14,
                AppSettings.textPrimary(host));
        accessTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        accessTitle.setPadding(0, dp(6), 0, dp(3));
        locationCard.addView(accessTitle);

        locationAccessStatus = text(
                "",
                12,
                AppSettings.textSecondary(host));
        locationAccessStatus.setPadding(0, 0, 0, dp(6));
        locationCard.addView(locationAccessStatus);

        locationPermissionAction =
                fieldButton("");
        locationPermissionAction.setVisibility(View.GONE);
        locationPermissionAction.setOnClickListener(v ->
                handleLocationPermissionAction());
        locationCard.addView(
                locationPermissionAction,
                new LinearLayout.LayoutParams(-1, dp(48)));

        locationGpsAction =
                fieldButton(
                        AppString.get(
                                R.string.location_access_enable_gps));
        locationGpsAction.setVisibility(View.GONE);
        locationGpsAction.setOnClickListener(v ->
                openLocationSettings());
        LinearLayout.LayoutParams gpsActionParams =
                new LinearLayout.LayoutParams(-1, dp(48));
        gpsActionParams.topMargin = dp(6);
        locationCard.addView(
                locationGpsAction,
                gpsActionParams);

        locationRetryAction =
                fieldButton(
                        AppString.get(
                                R.string.location_access_retry));
        locationRetryAction.setVisibility(View.GONE);
        locationRetryAction.setOnClickListener(v -> {
            locationAccessErrorRes = 0;
            requestPreciseLocation();
        });
        LinearLayout.LayoutParams retryActionParams =
                new LinearLayout.LayoutParams(-1, dp(48));
        retryActionParams.topMargin = dp(6);
        locationCard.addView(
                locationRetryAction,
                retryActionParams);

        Button searchLocation = fieldButton(AppString.get(R.string.runtime_text_0353));
        searchLocation.setOnClickListener(v -> host.startActivityForResult(
                new Intent(host, PrayerLocationSearchActivity.class), REQ_HORIZON));
        LinearLayout.LayoutParams searchParams =
                new LinearLayout.LayoutParams(-1, dp(52));
        searchParams.topMargin = dp(8);
        locationCard.addView(searchLocation, searchParams);

        locationButton = fieldButton(AppString.get(R.string.runtime_text_0089));
        locationButton.setOnClickListener(v -> {
            locationAccessErrorRes = 0;
            requestPreciseLocation();
        });
        LinearLayout.LayoutParams currentLocationParams =
                new LinearLayout.LayoutParams(-1, dp(52));
        currentLocationParams.topMargin = dp(6);
        locationCard.addView(locationButton, currentLocationParams);

        horizonList = new LinearLayout(host);
        horizonList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams horizonParams = new LinearLayout.LayoutParams(-1, -2);
        horizonParams.topMargin = dp(8);
        locationCard.addView(horizonList, horizonParams);

        root.addView(locationCard, cardParams());

        LinearLayout qiblaDisplayCard = card();
        TextView qiblaDisplayTitle = text(
                AppString.get(
                        R.string.adhan_qibla_display_title),
                17,
                AppSettings.textPrimary(host));
        qiblaDisplayTitle.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD);
        qiblaDisplayCard.addView(qiblaDisplayTitle);

        Spinner qiblaDisplayMode = new Spinner(host);
        ArrayAdapter<String> qiblaDisplayAdapter =
                new ArrayAdapter<>(
                        host,
                        android.R.layout.simple_spinner_item,
                        new String[]{
                                AppString.get(
                                        R.string.adhan_qibla_display_none),
                                AppString.get(
                                        R.string.adhan_qibla_display_button),
                                AppString.get(
                                        R.string.adhan_qibla_display_background)
                        });
        qiblaDisplayAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        qiblaDisplayMode.setAdapter(qiblaDisplayAdapter);
        qiblaDisplayMode.setLayoutDirection(
                AppSettings.layoutDirection(host));
        qiblaDisplayMode.setBackgroundResource(
                R.drawable.bg_field);
        qiblaDisplayMode.setSelection(
                AppSettings.adhanQiblaDisplayMode(host));
        qiblaDisplayMode.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        if (AppSettings.adhanQiblaDisplayMode(
                                host)
                                == position) {
                            return;
                        }
                        AppSettings.setAdhanQiblaDisplayMode(
                                host,
                                position);
                        host.setResult(Activity.RESULT_OK);
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {}
                });
        LinearLayout.LayoutParams qiblaDisplayParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(54));
        qiblaDisplayParams.topMargin = dp(8);
        qiblaDisplayCard.addView(
                qiblaDisplayMode,
                qiblaDisplayParams);
        root.addView(
                qiblaDisplayCard,
                cardParams());

        LinearLayout azanCard = card();
        TextView azanTitle = text(AppString.get(R.string.runtime_text_0342), 17, AppSettings.textPrimary(host));
        azanTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        azanCard.addView(azanTitle);
        addAdhanSettingRow(azanCard, AppString.get(R.string.runtime_text_0343), 899);
        addAdhanSettingRow(azanCard, AppString.get(R.string.runtime_text_0078), AdhanScheduler.FAJR);
        addAdhanSettingRow(azanCard, AppString.get(R.string.runtime_text_0079), AdhanScheduler.DHUHR);
        addAdhanSettingRow(azanCard, AppString.get(R.string.adhan_asr_name), AdhanScheduler.ASR);
        addAdhanSettingRow(azanCard, AppString.get(R.string.runtime_text_0080), AdhanScheduler.MAGHRIB);
        addAdhanSettingRow(azanCard, AppString.get(R.string.adhan_isha_name), AdhanScheduler.ISHA);

        adhanScheduleStatus = text(
                "",
                11,
                AppSettings.textSecondary(host));
        adhanScheduleStatus.setPadding(0, dp(6), 0, 0);
        adhanScheduleStatus.setOnClickListener(v -> resolveAdhanScheduleStatus());
        azanCard.addView(adhanScheduleStatus);

        root.addView(azanCard, cardParams());

        Button preview = fieldButton(AppString.get(R.string.runtime_text_0096));
        preview.setOnClickListener(v -> previewAdhan());
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(54)));

        refresh();
    }

    void onResume() {

        if (QiblaUtils.hasLocationPermission(host)
                && QiblaUtils.isLocationEnabled(host)
                && QiblaUtils.bestKnownLocation(host) != null) {
            locationAccessErrorRes = 0;
        }

        boolean permissionGranted = PermissionHelper.exactAlarmsGranted(host);
        if (permissionGranted
                && AdhanScheduler.status(host) == AdhanScheduler.Status.SCHEDULE_FAILED) {
            AdhanScheduler.rescheduleAll(host);
        }
        refresh();
    }


    private void refresh() {
        refreshAdhanScheduleStatus();
        boolean hasLocation = AppSettings.prayerLocationSet(host);
        if (hasLocation) {
            String label = AppSettings.prayerLocationLabel(host);
            locationStatus.setText(
                    label
                            + "\n"
                            + coordinateText(
                            AppSettings.prayerLatitude(host),
                            AppSettings.prayerLongitude(host)));
            locationButton.setText(AppString.get(R.string.runtime_text_0372));
        } else {
            locationStatus.setText(AppString.get(R.string.runtime_text_0097));
            locationButton.setText(AppString.get(R.string.runtime_text_0089));
        }
        refreshLocationAccessUi();
        renderHorizons();
        syncAdhanSwitches();
        refreshSummaries();

    }

    private void renderHorizons() {
        if (horizonList == null) return;
        horizonList.removeAllViews();
        List<AppSettings.PrayerHorizon> horizons = AppSettings.prayerHorizons(host);
        if (horizons.isEmpty()) {
            TextView empty = text(AppString.get(R.string.runtime_text_0354), 12,
                    AppSettings.textSecondary(host));
            empty.setPadding(dp(10), dp(14), dp(10), dp(14));
            horizonList.addView(empty);
            return;
        }

        for (AppSettings.PrayerHorizon horizon : horizons) {
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutDirection(AppSettings.layoutDirection(host));
            row.setBackgroundResource(R.drawable.bg_field);
            row.setPadding(dp(12), 0, dp(8), 0);

            TextView label = text(horizon.label, 13, AppSettings.textPrimary(host));
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(label, new LinearLayout.LayoutParams(0, dp(54), 1f));

            boolean isPrimary = AppSettings.isPrimaryPrayerHorizon(
                    host, horizon.latitude, horizon.longitude);
            Button primary = fieldButton(isPrimary ? AppString.get(R.string.runtime_text_0356) : AppString.get(R.string.runtime_text_0565));
            primary.setGravity(Gravity.CENTER);
            primary.setEnabled(!isPrimary);
            primary.setContentDescription(
                    isPrimary ? AppString.get(R.string.runtime_text_0358) + horizon.label
                            : AppString.get(R.string.runtime_text_0566) + horizon.label + AppString.get(R.string.runtime_text_0567));
            primary.setOnClickListener(v -> {
                AppSettings.setPrimaryPrayerHorizon(
                        host,
                        horizon.latitude,
                        horizon.longitude,
                        horizon.label,
                        horizon.timeZoneId);
                host.setResult(Activity.RESULT_OK);
                refresh();
            });
            LinearLayout.LayoutParams primaryParams =
                    new LinearLayout.LayoutParams(dp(94), dp(44));
            primaryParams.setMarginStart(dp(6));
            row.addView(primary, primaryParams);

            Button remove = new Button(host);
            remove.setText(R.string.ui_symbol_minus);
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
                        host, horizon.latitude, horizon.longitude);
                host.setResult(Activity.RESULT_OK);
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
        if (enabled && AdhanScheduler.status(host) != AdhanScheduler.Status.SCHEDULED) {
            resolveAdhanScheduleStatus();
        }
    }

    private void refreshAdhanScheduleStatus() {
        if (adhanScheduleStatus == null) return;
        AdhanScheduler.Status status = AdhanScheduler.status(host);
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
                ? 0xFFC44C4C : AppSettings.textSecondary(host));
        adhanScheduleStatus.setClickable(problem);
    }

    private void resolveAdhanScheduleStatus() {
        AdhanScheduler.Status status = AdhanScheduler.status(host);
        if (status == AdhanScheduler.Status.LOCATION_MISSING) {
            requestPreciseLocation();
        } else if (status == AdhanScheduler.Status.EXACT_PERMISSION_MISSING) {
            try {
                host.startActivity(new Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + host.getPackageName())));
            } catch (Exception ignored) {
            }
        } else if (status == AdhanScheduler.Status.NOTIFICATION_PERMISSION_MISSING
                && Build.VERSION.SDK_INT >= 33) {
            host.requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_NOTIFICATIONS);
        } else if (status == AdhanScheduler.Status.SCHEDULE_FAILED) {
            AdhanScheduler.rescheduleAll(host);
            refreshAdhanScheduleStatus();
        }
    }

    private void addAdhanSkipSection(LinearLayout root, int type) {
        LinearLayout skipCard = card();

        TextView title = text(
                AppString.get(R.string.adhan_skip_title),
                16,
                AppSettings.textPrimary(host));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        skipCard.addView(title);

        TextView description = text(
                AppString.get(R.string.adhan_skip_description),
                12,
                AppSettings.textSecondary(host));
        description.setPadding(0, dp(4), 0, dp(8));
        skipCard.addView(description);

        boolean mixedSkipMode = type == 899
                && !AppSettings.allAdhanSkipModesMatch(host);
        java.util.ArrayList<String> modeItems = new java.util.ArrayList<>();
        modeItems.add(AppString.get(R.string.adhan_skip_mode_off));
        modeItems.add(AppString.get(R.string.adhan_skip_mode_weekdays));
        modeItems.add(AppString.get(R.string.adhan_skip_mode_dates));
        if (mixedSkipMode) {
            modeItems.add(AppString.get(R.string.runtime_text_0344));
        }

        Spinner mode = new Spinner(host);
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(
                host,
                android.R.layout.simple_spinner_item,
                modeItems);
        modeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mode.setAdapter(modeAdapter);
        mode.setLayoutDirection(AppSettings.layoutDirection(host));
        mode.setSelection(
                mixedSkipMode
                        ? 3
                        : AppSettings.adhanSkipMode(host, type));
        skipCard.addView(mode, new LinearLayout.LayoutParams(-1, dp(54)));

        ScrollView optionsScroll = new ScrollView(host);
        optionsScroll.setFillViewport(true);
        LinearLayout options = new LinearLayout(host);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setLayoutDirection(AppSettings.layoutDirection(host));
        optionsScroll.addView(options, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams optionsParams =
                new LinearLayout.LayoutParams(-1, -2);
        optionsParams.topMargin = dp(8);
        skipCard.addView(optionsScroll, optionsParams);

        mode.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        if (type == 899 && position > AppSettings.ADHAN_SKIP_DATES) {
                            renderAdhanSkipOptions(options, mode, type);
                            return;
                        }
                        boolean mixed = type == 899
                                && !AppSettings.allAdhanSkipModesMatch(
                                        host);
                        if (mixed || AppSettings.adhanSkipMode(
                                host, type) != position) {
                            AppSettings.setAdhanSkipMode(
                                    host, type, position);
                            host.setResult(Activity.RESULT_OK);
                        }
                        renderAdhanSkipOptions(options, mode, type);
                    }

                    @Override public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {}
                });

        renderAdhanSkipOptions(options, mode, type);
        root.addView(skipCard, cardParams());
    }

    private void renderAdhanSkipOptions(
            LinearLayout options, Spinner mode, int type) {
        options.removeAllViews();
        int selectedMode = mode.getSelectedItemPosition();
        View optionsContainer = (View) options.getParent();

        if (type == 899 && selectedMode > AppSettings.ADHAN_SKIP_DATES) {
            optionsContainer.setVisibility(View.VISIBLE);
            TextView mixed = text(
                    AppString.get(R.string.runtime_text_0344),
                    13,
                    AppSettings.textSecondary(host));
            mixed.setPadding(dp(8), dp(12), dp(8), dp(12));
            options.addView(mixed);
            return;
        }

        optionsContainer.setVisibility(
                selectedMode == AppSettings.ADHAN_SKIP_NONE ? View.GONE : View.VISIBLE);

        if (selectedMode == AppSettings.ADHAN_SKIP_WEEKDAYS) {
            if (type == 899 && !AppSettings.allAdhanSkipWeekdaysMatch(host)) {
                TextView mixed = text(
                        AppString.get(R.string.runtime_text_0344),
                        12,
                        AppSettings.textSecondary(host));
                mixed.setPadding(dp(8), dp(4), dp(8), dp(8));
                options.addView(mixed);
            }
            int[] days = {
                    java.util.Calendar.SATURDAY,
                    java.util.Calendar.SUNDAY,
                    java.util.Calendar.MONDAY,
                    java.util.Calendar.TUESDAY,
                    java.util.Calendar.WEDNESDAY,
                    java.util.Calendar.THURSDAY,
                    java.util.Calendar.FRIDAY
            };
            int[] labels = {
                    R.string.runtime_text_0180,
                    R.string.runtime_text_0181,
                    R.string.runtime_text_0182,
                    R.string.runtime_text_0552,
                    R.string.runtime_text_0184,
                    R.string.runtime_text_0185,
                    R.string.runtime_text_0186
            };

            for (int i = 0; i < days.length; i++) {
                final int day = days[i];
                Switch skip = toggle(
                        AppString.get(labels[i]),
                        AppSettings.adhanSkipWeekday(host, type, day));
                skip.setOnCheckedChangeListener((button, checked) -> {
                    AppSettings.setAdhanSkipWeekday(
                            host, type, day, checked);
                    host.setResult(Activity.RESULT_OK);
                });
                options.addView(
                        skip,
                        new LinearLayout.LayoutParams(-1, dp(48)));
            }
            return;
        }

        if (selectedMode != AppSettings.ADHAN_SKIP_DATES) return;

        if (type == 899 && !AppSettings.allAdhanSkipDatesMatch(host)) {
            TextView mixed = text(
                    AppString.get(R.string.runtime_text_0344),
                    12,
                    AppSettings.textSecondary(host));
            mixed.setPadding(dp(8), dp(4), dp(8), dp(8));
            options.addView(mixed);
        }

        Button addDate = fieldButton(
                AppString.get(R.string.adhan_skip_add_date));
        addDate.setOnClickListener(v -> CalendarPickerDialog.showDateAny(
                host,
                System.currentTimeMillis(),
                AppSettings.defaultCalendar(host),
                (picked, calendarType) -> {
                    AppSettings.addAdhanSkipDate(host, type, picked);
                    host.setResult(Activity.RESULT_OK);
                    renderAdhanSkipOptions(options, mode, type);
                }));
        options.addView(
                addDate,
                new LinearLayout.LayoutParams(-1, dp(52)));

        java.util.ArrayList<String> dates = new java.util.ArrayList<>(
                AppSettings.adhanSkipDates(host, type));
        java.util.Collections.sort(dates);

        if (dates.isEmpty()) {
            TextView empty = text(
                    AppString.get(R.string.adhan_skip_no_dates),
                    12,
                    AppSettings.textSecondary(host));
            empty.setPadding(dp(8), dp(12), dp(8), dp(6));
            options.addView(empty);
            return;
        }

        for (String key : dates) {
            long millis = AppSettings.adhanSkipDateMillis(host, key);
            if (millis <= 0L) continue;

            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutDirection(AppSettings.layoutDirection(host));
            row.setBackgroundResource(R.drawable.bg_field);
            row.setPadding(dp(12), 0, dp(8), 0);

            TextView date = text(
                    CalendarUtils.formatDate(
                            millis,
                            AppSettings.defaultCalendar(host),
                            AppSettings.prayerTimeZone(host)),
                    13,
                    AppSettings.textPrimary(host));
            row.addView(date, new LinearLayout.LayoutParams(0, dp(52), 1f));

            Button remove = fieldButton(
                    AppString.get(R.string.adhan_skip_remove_date));
            remove.setGravity(Gravity.CENTER);
            remove.setTextColor(0xFFD32F2F);
            remove.setOnClickListener(v -> {
                AppSettings.removeAdhanSkipDate(
                        host, type, key);
                host.setResult(Activity.RESULT_OK);
                renderAdhanSkipOptions(options, mode, type);
            });
            LinearLayout.LayoutParams removeParams =
                    new LinearLayout.LayoutParams(dp(106), dp(42));
            removeParams.setMarginStart(dp(6));
            row.addView(remove, removeParams);

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(-1, dp(56));
            rowParams.topMargin = dp(6);
            options.addView(row, rowParams);
        }
    }

    private void addAdhanSettingRow(LinearLayout parent, String title, int type) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(AppSettings.layoutDirection(host));
        row.setPadding(dp(10), dp(4), dp(10), dp(4));
        row.setBackgroundResource(R.drawable.bg_field);

        LinearLayout labels = new LinearLayout(host);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(title, 14, AppSettings.textPrimary(host));
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labels.addView(name, new LinearLayout.LayoutParams(-1, -2));
        TextView summary = text(adhanSettingSummary(type), 11, AppSettings.textSecondary(host));
        summaries.put(type, summary);
        labels.addView(summary, new LinearLayout.LayoutParams(-1, -2));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout details = new LinearLayout(host);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setVisibility(View.GONE);
        labels.setPadding(0, dp(10), dp(6), dp(10));
        labels.setFocusable(true);
        labels.setBackgroundResource(R.drawable.bg_soft_button);
        labels.setContentDescription(title + "، " + AppString.get(R.string.runtime_text_0001));
        ImageView expand = new ImageView(host);
        labels.setOnClickListener(v -> {
            boolean opening = details.getVisibility() != View.VISIBLE;
            if (activeDetails != null) activeDetails.setVisibility(View.GONE);
            if (activeDetailsArrow != null) {
                activeDetailsArrow.animate().cancel();
                activeDetailsArrow.animate().rotation(0f).setDuration(180).start();
            }
            activeDetailsArrow = null;
            activeDetails = null;
            activeMuezzinList = null;
            activeAdhanType = -1;
            if (opening) {
                details.removeAllViews();
                showAdhanSettings(title, type, details);
                details.setVisibility(View.VISIBLE);
                activeDetails = details;
                activeDetailsArrow = expand;
                expand.animate().cancel();
                expand.animate().rotation(180f).setDuration(180).start();
            }
        });
        expand.setImageResource(R.drawable.ic_settings_chevron_down);
        expand.setColorFilter(AppSettings.primaryColor(host));
        expand.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        expand.setOnClickListener(v -> labels.performClick());
        row.addView(expand, new LinearLayout.LayoutParams(dp(32), dp(48)));

        Switch enabled = new Switch(host);
        enabled.setChecked(adhanTypeEnabled(type));
        enabled.setContentDescription(AppString.get(R.string.runtime_text_0350) + title);
        if (type == 899) {
            allAdhanSwitch = enabled;
        } else if (type >= AdhanScheduler.FAJR && type <= AdhanScheduler.ISHA) {
            adhanTypeSwitches[type] = enabled;
        }
        enabled.setOnCheckedChangeListener((button, checked) -> {
            if (syncingAdhanSwitches) return;
            updateAdhanSetting(() -> setAdhanTypeEnabled(type, checked), checked);
            syncAdhanSwitches();
        });
        row.addView(enabled, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(7);
        parent.addView(row, rowParams);
        parent.addView(details, new LinearLayout.LayoutParams(-1, -2));
    }

    private void syncAdhanSwitches() {
        syncingAdhanSwitches = true;
        try {
            adhanFeatureSwitch.setChecked(AppSettings.adhanEnabled(host));
            if (masterAdhanSwitch != null) {
                masterAdhanSwitch.setChecked(AppSettings.showPrayerTimes(host));
            }
            for (int type = AdhanScheduler.FAJR; type <= AdhanScheduler.ISHA; type++) {
                Switch item = adhanTypeSwitches[type];
                if (item != null) item.setChecked(adhanTypeEnabled(type));
            }
            if (allAdhanSwitch != null) {
                allAdhanSwitch.setChecked(adhanTypeEnabled(899));
            }
        } finally {
            syncingAdhanSwitches = false;
        }
    }

    private void refreshSummaries() {
        for (java.util.Map.Entry<Integer, TextView> entry : summaries.entrySet())
            entry.getValue().setText(adhanSettingSummary(entry.getKey()));
    }

    private String adhanSettingSummary(int type) {
        if (type == 899) {
            String firstSound = AppSettings.adhanSoundUri(host, AdhanScheduler.FAJR);
            int firstVolume = AppSettings.adhanVolume(host, AdhanScheduler.FAJR);
            boolean same = true;
            for (int item = AdhanScheduler.DHUHR; item <= AdhanScheduler.ISHA; item++) {
                if (!firstSound.equals(AppSettings.adhanSoundUri(host, item))
                        || firstVolume != AppSettings.adhanVolume(host, item)) {
                    same = false;
                    break;
                }
            }
            return same
                    ? SoundLibrary.name(host, firstSound) + " • " + firstVolume + AppString.get(R.string.runtime_text_0568)
                    : AppString.get(R.string.runtime_text_0344);
        }
        return SoundLibrary.name(host, AppSettings.adhanSoundUri(host, type))
                + " • " + AppSettings.adhanVolume(host, type) + AppString.get(R.string.runtime_text_0568);
    }

    private boolean adhanTypeEnabled(int type) {
        if (type == 899) return AppSettings.adhanEnabled(host)
                && AppSettings.fajrAdhanEnabled(host)
                && AppSettings.dhuhrAdhanEnabled(host)
                && AppSettings.asrAdhanEnabled(host)
                && AppSettings.maghribAdhanEnabled(host)
                && AppSettings.ishaAdhanEnabled(host);
        if (type == AdhanScheduler.FAJR) return AppSettings.fajrAdhanEnabled(host);
        if (type == AdhanScheduler.DHUHR) return AppSettings.dhuhrAdhanEnabled(host);
        if (type == AdhanScheduler.ASR) return AppSettings.asrAdhanEnabled(host);
        if (type == AdhanScheduler.MAGHRIB) return AppSettings.maghribAdhanEnabled(host);
        return AppSettings.ishaAdhanEnabled(host);
    }

    private void setAdhanTypeEnabled(int type, boolean enabled) {
        if (type == 899) {
            AppSettings.setAdhanEnabled(host, enabled);
            AppSettings.setFajrAdhanEnabled(host, enabled);
            AppSettings.setDhuhrAdhanEnabled(host, enabled);
            AppSettings.setAsrAdhanEnabled(host, enabled);
            AppSettings.setMaghribAdhanEnabled(host, enabled);
            AppSettings.setIshaAdhanEnabled(host, enabled);
            return;
        }
        if (enabled && !AppSettings.adhanEnabled(host)) {
            AppSettings.setAdhanEnabled(host, true);
        }
        if (type == AdhanScheduler.FAJR) AppSettings.setFajrAdhanEnabled(host, enabled);
        else if (type == AdhanScheduler.DHUHR) AppSettings.setDhuhrAdhanEnabled(host, enabled);
        else if (type == AdhanScheduler.ASR) AppSettings.setAsrAdhanEnabled(host, enabled);
        else if (type == AdhanScheduler.MAGHRIB) AppSettings.setMaghribAdhanEnabled(host, enabled);
        else AppSettings.setIshaAdhanEnabled(host, enabled);
    }

    private void showAdhanSettings(String title, int type, LinearLayout root) {
        LinearLayout output = card();
        TextView outputTitle = text(AppString.get(R.string.runtime_text_0345), 16,
                AppSettings.textPrimary(host));
        outputTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        output.addView(outputTitle);

        boolean mixedVolume = type == 899
                && !AppSettings.allAdhanVolumesMatch(host);
        int initialVolume = AppSettings.adhanVolume(host, type);
        TextView volumeLabel = text(
                mixedVolume
                        ? AppString.get(R.string.runtime_text_0344)
                        : AppString.get(R.string.runtime_text_0348)
                                + initialVolume
                                + AppString.get(R.string.runtime_text_0568),
                13,
                AppSettings.textSecondary(host));
        volumeLabel.setPadding(0, dp(8), 0, 0);
        output.addView(volumeLabel);

        SeekBar volume = new SeekBar(host);
        volume.setMax(100);
        volume.setProgress(initialVolume);
        volume.setContentDescription(
                AppString.get(R.string.runtime_text_0349) + title);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                AppSettings.setAdhanVolume(
                        host, type, progress);
                refreshSummaries();
                volumeLabel.setText(
                        AppString.get(R.string.runtime_text_0348)
                                + progress
                                + AppString.get(R.string.runtime_text_0568));
                host.setResult(Activity.RESULT_OK);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        output.addView(volume, new LinearLayout.LayoutParams(-1, dp(48)));

        Switch fullscreenUnlocked = toggle(
                AppString.get(R.string.runtime_text_0210),
                AppSettings.adhanFullscreenUnlocked(host, type));
        fullscreenUnlocked.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setAdhanFullscreenUnlocked(host, type, checked));

        Switch fullscreenLocked = toggle(
                AppString.get(R.string.runtime_text_0211),
                AppSettings.adhanFullscreenLocked(host, type));
        fullscreenLocked.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setAdhanFullscreenLocked(host, type, checked));

        Switch notification = toggle(
                AppString.get(R.string.runtime_text_0093),
                AppSettings.adhanNotification(host, type));
        notification.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setAdhanNotification(host, type, checked));

        Switch vibrate = toggle(
                AppString.get(R.string.runtime_text_0094),
                AppSettings.adhanVibrate(host, type));
        vibrate.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setAdhanVibrate(host, type, checked));

        Switch sound = toggle(
                AppString.get(R.string.runtime_text_0095),
                AppSettings.adhanSound(host, type));
        sound.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setAdhanSound(host, type, checked));
        output.addView(fullscreenUnlocked);
        output.addView(fullscreenLocked);
        output.addView(notification);
        output.addView(vibrate);
        output.addView(sound);
        root.addView(output, cardParams());

        addAdhanSkipSection(root, type);

        LinearLayout muezzin = card();
        TextView muezzinTitle = text(AppString.get(R.string.runtime_text_0346), 16, AppSettings.textPrimary(host));
        muezzinTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        muezzin.addView(muezzinTitle);
        Button upload = fieldButton(AppString.get(R.string.runtime_text_0347));
        upload.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        upload.setPadding(dp(16), 0, dp(16), 0);
        upload.setOnClickListener(v -> uploadAdhanAudio(type));
        LinearLayout.LayoutParams uploadParams = new LinearLayout.LayoutParams(-1, dp(50));
        uploadParams.topMargin = dp(7);
        muezzin.addView(upload, uploadParams);

        ScrollView soundScroll = new ScrollView(host);
        soundScroll.setFillViewport(true);
        soundScroll.setClipToOutline(true);
        soundScroll.setBackground(soundListBackground());
        soundScroll.setPadding(dp(8), dp(8), dp(8), dp(8));
        activeMuezzinList = new LinearLayout(host);
        activeMuezzinList.setOrientation(LinearLayout.VERTICAL);
        activeMuezzinList.setLayoutDirection(AppSettings.layoutDirection(host));
        soundScroll.addView(activeMuezzinList, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams soundScrollParams = new LinearLayout.LayoutParams(-1, -2);
        soundScrollParams.topMargin = dp(10);
        muezzin.addView(soundScroll, soundScrollParams);
        root.addView(muezzin, new LinearLayout.LayoutParams(-1, -2));
        activeAdhanType = type;
        renderMuezzinList(type);

    }

    private void renderMuezzinList(int type) {
        refreshSummaries();
        if (activeMuezzinList == null) return;
        activeMuezzinList.removeAllViews();

        String selected;
        boolean mixed = type == 899 && !AppSettings.allAdhanSoundsMatch(host);
        if (mixed) {
            selected = "";
            TextView mixedLabel = text(
                    AppString.get(R.string.runtime_text_0344),
                    13,
                    AppSettings.textSecondary(host));
            mixedLabel.setPadding(dp(10), dp(8), dp(10), dp(10));
            activeMuezzinList.addView(
                    mixedLabel,
                    new LinearLayout.LayoutParams(-1, -2));
        } else {
            selected = type == 899
                    ? AppSettings.commonAdhanSoundUri(host)
                    : AppSettings.adhanSoundUri(host, type);
            if (selected == null) selected = "";
        }

        for (SoundLibrary.Sound item : SoundLibrary.all(host)) {
            boolean checked = item.uri.equals(selected);
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(AppSettings.layoutDirection(host));
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), 0, dp(12), 0);
            row.setBackground(soundRowBackground(checked));
            TextView name = text(item.name, 14, checked
                    ? AppSettings.primaryColor(host) : AppSettings.textPrimary(host));
            if (checked) name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.addView(name, new LinearLayout.LayoutParams(0, -1, 1f));
            ImageView check = new ImageView(host);
            check.setImageResource(R.drawable.ic_md_check);
            check.setColorFilter(AppSettings.primaryColor(host), PorterDuff.Mode.SRC_IN);
            check.setVisibility(checked ? View.VISIBLE : View.INVISIBLE);
            check.setContentDescription(checked ? AppString.get(R.string.runtime_text_0341) : null);
            row.addView(check, new LinearLayout.LayoutParams(dp(24), dp(24)));
            row.setOnClickListener(v -> {
                setAdhanSound(type, item.uri);
                renderMuezzinList(type);
                host.setResult(Activity.RESULT_OK);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(54));
            params.bottomMargin = dp(6);
            activeMuezzinList.addView(row, params);
        }
    }

    private void setAdhanSound(int type, String uri) {
        if (type == 899) {
            for (int item = AdhanScheduler.FAJR; item <= AdhanScheduler.ISHA; item++) {
                AppSettings.setAdhanSoundUri(host, item, uri);
            }
        } else {
            AppSettings.setAdhanSoundUri(host, type, uri);
        }
    }

    private void uploadAdhanAudio(int type) {
        activeAdhanType = type;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("audio/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        host.startActivityForResult(intent, REQ_ADHAN_AUDIO);
    }

    private GradientDrawable soundListBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(AppSettings.surface(host));
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), AppSettings.field(host));
        return background;
    }

    private GradientDrawable soundRowBackground(boolean checked) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(checked ? (AppSettings.primaryColor(host) & 0x00FFFFFF) | 0x18000000
                : AppSettings.field(host));
        background.setCornerRadius(dp(9));
        if (checked) background.setStroke(dp(1), AppSettings.primaryColor(host));
        return background;
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_ADHAN_AUDIO) {
            if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null
                    || activeAdhanType == -1) return;
            Uri uri = data.getData();
            try {
                host.getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            SoundLibrary.add(host, uri);
            setAdhanSound(activeAdhanType, uri.toString());
            renderMuezzinList(activeAdhanType);
            host.setResult(Activity.RESULT_OK);
            return;
        }
        if (requestCode == REQ_HORIZON) {
            if (resultCode == Activity.RESULT_OK && data != null
                    && data.getBooleanExtra("requestGps", false)) requestPreciseLocation();
            refresh();
            return;
        }
        if ((requestCode < 800 || requestCode > 804) && requestCode != 899
                || resultCode != Activity.RESULT_OK || data == null) return;
        String uri = data.getStringExtra(SoundPickerActivity.EXTRA_URI);
        if (requestCode == 899) {
            for (int type = AdhanScheduler.FAJR; type <= AdhanScheduler.ISHA; type++)
                AppSettings.setAdhanSoundUri(host, type, uri);
        } else AppSettings.setAdhanSoundUri(host, requestCode - 800, uri);
        host.recreate();
    }

    private void previewAdhan() {
        Intent service = AdhanSoundService.startIntent(host, AdhanScheduler.FAJR);
        if (Build.VERSION.SDK_INT >= 26) host.startForegroundService(service);
        else host.startService(service);
        host.startActivity(new Intent(host, AdhanRingActivity.class)
                .putExtra("adhanType", AdhanScheduler.FAJR));
    }

    private void requestPreciseLocation() {
        if (!QiblaUtils.hasLocationPermission(host)) {
            requestLocationPermission();
            return;
        }

        if (!QiblaUtils.isLocationEnabled(host)) {
            locationAccessErrorRes = 0;
            refreshLocationAccessUi();
            return;
        }

        fetchCurrentLocation();
    }

    private void requestLocationPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            fetchCurrentLocation();
            return;
        }

        if (QiblaUtils.locationPermissionBlocked(host)) {
            openAppPermissionSettings();
            return;
        }

        QiblaUtils.markLocationPermissionRequested(host);
        host.requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQ_LOCATION);
    }

    private void handleLocationPermissionAction() {
        if (QiblaUtils.locationPermissionBlocked(host)) {
            openAppPermissionSettings();
        } else {
            requestLocationPermission();
        }
    }

    private void openAppPermissionSettings() {
        try {
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + host.getPackageName()));
            host.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void openLocationSettings() {
        try {
            host.startActivity(
                    new Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } catch (Exception ignored) {
        }
    }

    private void refreshLocationAccessUi() {
        if (locationAccessStatus == null) return;

        boolean permission =
                QiblaUtils.hasLocationPermission(host);
        boolean gpsEnabled =
                QiblaUtils.isLocationEnabled(host);
        boolean blocked =
                QiblaUtils.locationPermissionBlocked(host);
        boolean requested =
                QiblaUtils.locationPermissionWasRequested(host);

        locationPermissionAction.setVisibility(View.GONE);
        locationGpsAction.setVisibility(View.GONE);
        locationRetryAction.setVisibility(View.GONE);

        if (!permission) {
            locationAccessStatus.setText(
                    AppString.get(
                            blocked
                                    ? R.string.location_access_permission_blocked
                                    : requested
                                    ? R.string.location_access_permission_denied
                                    : R.string.location_access_permission_needed));
            locationAccessStatus.setTextColor(0xFFC44C4C);

            locationPermissionAction.setText(
                    AppString.get(
                            blocked
                                    ? R.string.location_access_open_app_settings
                                    : requested
                                    ? R.string.location_access_request_again
                                    : R.string.location_access_grant));
            locationPermissionAction.setVisibility(View.VISIBLE);

            if (!gpsEnabled) {
                locationGpsAction.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (!gpsEnabled) {
            locationAccessStatus.setText(
                    AppString.get(
                            R.string.location_access_gps_off));
            locationAccessStatus.setTextColor(0xFFC44C4C);
            locationGpsAction.setVisibility(View.VISIBLE);
            return;
        }

        if (QiblaUtils.bestKnownLocation(host) != null) {
            locationAccessErrorRes = 0;
        }

        if (locationAccessErrorRes != 0) {
            locationAccessStatus.setText(
                    AppString.get(locationAccessErrorRes));
            locationAccessStatus.setTextColor(0xFFC44C4C);
            locationRetryAction.setVisibility(View.VISIBLE);
            return;
        }

        locationAccessStatus.setText(
                AppString.get(
                        R.string.location_access_ready));
        locationAccessStatus.setTextColor(
                AppSettings.textSecondary(host));
    }

    void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
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
            locationAccessErrorRes = 0;
            fetchCurrentLocation();
        } else {
            locationAccessErrorRes = 0;
            refreshLocationAccessUi();
        }
    }

    private void fetchCurrentLocation() {
        if (!QiblaUtils.hasLocationPermission(host)) {
            refreshLocationAccessUi();
            return;
        }

        if (!QiblaUtils.isLocationEnabled(host)) {
            locationAccessErrorRes = 0;
            refreshLocationAccessUi();
            return;
        }

        if (qiblaLocationClient == null) {
            qiblaLocationClient =
                    new QiblaLocationClient(host);
        }

        final int requestGeneration =
                ++locationRequestGeneration;

        locationAccessErrorRes = 0;
        locationButton.setEnabled(false);
        locationButton.setText(
                AppString.get(
                        R.string.runtime_text_0098));

        if (locationAccessStatus != null) {
            locationAccessStatus.setText(
                    AppString.get(
                            R.string.runtime_text_0098));
            locationAccessStatus.setTextColor(
                    AppSettings.textSecondary(host));
            locationRetryAction.setVisibility(View.GONE);
        }

        qiblaLocationClient.request(
                new QiblaLocationClient.Callback() {
                    @Override
                    public void onLocation(
                            Location location) {
                        if (requestGeneration
                                != locationRequestGeneration) {
                            return;
                        }
                        handleLocation(
                                location,
                                requestGeneration);
                    }

                    @Override
                    public void onError(
                            int stringRes) {
                        if (requestGeneration
                                != locationRequestGeneration) {
                            return;
                        }

                        locationButton.setEnabled(true);

                        Location fallback =
                                QiblaUtils.bestKnownLocation(
                                        host);
                        if (fallback != null
                                && QiblaUtils.hasLocationPermission(
                                host)
                                && QiblaUtils.isLocationEnabled(
                                host)) {
                            locationAccessErrorRes = 0;
                        } else {
                            locationAccessErrorRes =
                                    stringRes;
                        }

                        refresh();
                        refreshLocationAccessUi();
                    }
                });
    }

    private void handleLocation(
            Location location,
            int requestGeneration) {
        if (location == null
                || requestGeneration
                != locationRequestGeneration) {
            return;
        }

        locationAccessErrorRes = 0;

        double lat =
                location.getLatitude();
        double lon =
                location.getLongitude();

        QiblaUtils.remember(
                host,
                lat,
                lon);

        String fallback =
                coordinateText(
                        lat,
                        lon);

        AppSettings.setPrayerLocation(
                host,
                lat,
                lon,
                fallback);

        locationButton.setEnabled(true);
        refresh();
        host.setResult(Activity.RESULT_OK);

        new Thread(() ->
                resolveLocationName(
                        lat,
                        lon,
                        fallback,
                        requestGeneration))
                .start();
    }

    private void cancelLocationRequest() {
        locationRequestGeneration++;
        if (qiblaLocationClient != null) {
            qiblaLocationClient.cancel();
        }
    }

    void onDestroy() {
        cancelLocationRequest();
    }

    @SuppressWarnings("deprecation")
    private void resolveLocationName(
            double lat, double lon, String fallback, int requestGeneration) {
        String label = fallback;
        try {
            Geocoder geocoder = new Geocoder(host, Locale.getDefault());
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
        host.runOnUiThread(() -> {
            if (requestGeneration != locationRequestGeneration || host.isFinishing()
                    || (Build.VERSION.SDK_INT >= 17 && host.isDestroyed())) {
                return;
            }
            AppSettings.setPrayerLocation(host, lat, lon, finalLabel);
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
        Switch control = new Switch(host);
        control.setText(value);
        control.setTextColor(AppSettings.textPrimary(host));
        control.setTextSize(14);
        control.setChecked(checked);
        control.setPadding(0, dp(10), 0, dp(10));
        control.setMinHeight(dp(48));
        return control;
    }

    private Button fieldButton(String value) {
        Button button = new Button(host);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(AppSettings.primaryColor(host));
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setBackgroundResource(R.drawable.bg_field);
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(AppSettings.layoutDirection(host));
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
        TextView view = new TextView(host);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * host.getResources().getDisplayMetrics().density);
    }
}
