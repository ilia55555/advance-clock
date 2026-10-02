package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;

public final class QiblaUtils {
    private static final double KAABA_LATITUDE = 21.422487;
    private static final double KAABA_LONGITUDE = 39.826206;

    private static final String PREFS = "qibla_location";
    private static final String KEY_HAS = "has_location";
    private static final String KEY_LAT = "latitude";
    private static final String KEY_LON = "longitude";
    private static final String KEY_PERMISSION_REQUESTED =
            "location_permission_requested";

    private QiblaUtils() {}

    public static float trueHeading(
            float magneticHeading,
            Location location) {
        if (location == null) {
            return normalize(magneticHeading);
        }

        try {
            GeomagneticField field =
                    new GeomagneticField(
                            (float) location.getLatitude(),
                            (float) location.getLongitude(),
                            (float) location.getAltitude(),
                            System.currentTimeMillis());
            return normalize(
                    magneticHeading
                            + field.getDeclination());
        } catch (RuntimeException ignored) {
            return normalize(magneticHeading);
        }
    }

    private static float normalize(float value) {
        float result = value % 360f;
        if (result < 0f) result += 360f;
        return result;
    }

    public static double bearing(
            double latitude,
            double longitude) {
        double lat = Math.toRadians(latitude);
        double lonDifference = Math.toRadians(
                KAABA_LONGITUDE - longitude);
        double kaabaLat = Math.toRadians(
                KAABA_LATITUDE);

        double y =
                Math.sin(lonDifference)
                        * Math.cos(kaabaLat);
        double x =
                Math.cos(lat)
                        * Math.sin(kaabaLat)
                        - Math.sin(lat)
                        * Math.cos(kaabaLat)
                        * Math.cos(lonDifference);

        return (Math.toDegrees(
                Math.atan2(y, x))
                + 360d) % 360d;
    }

    public static void remember(
            Context context,
            double latitude,
            double longitude) {
        context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_HAS, true)
                .putString(
                        KEY_LAT,
                        Double.toString(latitude))
                .putString(
                        KEY_LON,
                        Double.toString(longitude))
                .apply();
    }

    public static Location bestKnownLocation(
            Context context) {
        Location live = lastKnownLocation(context);
        if (live != null) {
            remember(
                    context,
                    live.getLatitude(),
                    live.getLongitude());
            return live;
        }

        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_HAS, false)) {
            try {
                Location stored =
                        new Location("qibla-cache");
                stored.setLatitude(
                        Double.parseDouble(
                                prefs.getString(
                                        KEY_LAT,
                                        "0")));
                stored.setLongitude(
                        Double.parseDouble(
                                prefs.getString(
                                        KEY_LON,
                                        "0")));
                return stored;
            } catch (Exception ignored) {
            }
        }

        if (AppSettings.prayerLocationSet(context)) {
            Location prayer =
                    new Location("prayer-location");
            prayer.setLatitude(
                    AppSettings.prayerLatitude(context));
            prayer.setLongitude(
                    AppSettings.prayerLongitude(context));
            return prayer;
        }

        return null;
    }

    public static void markLocationPermissionRequested(
            Context context) {
        context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .edit()
                .putBoolean(
                        KEY_PERMISSION_REQUESTED,
                        true)
                .apply();
    }

    public static boolean locationPermissionWasRequested(
            Context context) {
        return context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .getBoolean(
                        KEY_PERMISSION_REQUESTED,
                        false);
    }

    public static boolean locationPermissionBlocked(
            Activity activity) {
        if (Build.VERSION.SDK_INT < 23
                || hasLocationPermission(activity)
                || !locationPermissionWasRequested(activity)) {
            return false;
        }

        return !activity.shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION)
                && !activity.shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public static boolean isLocationEnabled(
            Context context) {
        LocationManager manager =
                (LocationManager)
                        context.getSystemService(
                                Context.LOCATION_SERVICE);
        if (manager == null) {
            return false;
        }

        try {
            if (Build.VERSION.SDK_INT >= 28) {
                return manager.isLocationEnabled();
            }
            return manager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)
                    || manager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean hasLocationPermission(
            Context context) {
        return Build.VERSION.SDK_INT < 23
                || context.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("MissingPermission")
    private static Location lastKnownLocation(
            Context context) {
        if (!hasLocationPermission(context)) {
            return null;
        }

        LocationManager manager =
                (LocationManager)
                        context.getSystemService(
                                Context.LOCATION_SERVICE);
        if (manager == null) {
            return null;
        }

        Location best = null;
        for (String provider : new String[]{
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER}) {
            try {
                Location value =
                        manager.getLastKnownLocation(provider);
                if (value == null) continue;
                if (best == null
                        || value.getTime() > best.getTime()
                        || value.getAccuracy()
                        < best.getAccuracy()) {
                    best = value;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return best;
    }
}
