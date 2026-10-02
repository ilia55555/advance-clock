package com.ilia.advanceclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public final class QiblaLocationClient {
    public interface Callback {
        void onLocation(Location location);
        void onError(int stringRes);
    }

    private final Context context;
    private final Handler handler =
            new Handler(Looper.getMainLooper());
    private final List<LocationListener> listeners =
            new ArrayList<>();

    private LocationManager manager;
    private Runnable timeout;
    private boolean finished;

    public QiblaLocationClient(Context context) {
        this.context = context;
    }

    @SuppressLint("MissingPermission")
    public void request(Callback callback) {
        cancel();
        finished = false;

        if (!QiblaUtils.hasLocationPermission(context)) {
            callback.onError(
                    R.string.location_access_permission_needed);
            return;
        }

        if (!QiblaUtils.isLocationEnabled(context)) {
            callback.onError(
                    R.string.location_access_gps_off);
            return;
        }

        manager =
                (LocationManager)
                        context.getSystemService(
                                Context.LOCATION_SERVICE);
        if (manager == null) {
            callback.onError(
                    R.string.runtime_text_0367);
            return;
        }

        Location best =
                QiblaUtils.bestKnownLocation(context);
        if (isUsable(best)) {
            callback.onLocation(best);
        }

        List<String> providers =
                enabledProviders(manager);
        if (providers.isEmpty()) {
            if (!isUsable(best)) {
                callback.onError(
                        R.string.location_access_gps_off);
            }
            return;
        }

        timeout = () -> {
            if (finished) return;
            Location fallback =
                    QiblaUtils.bestKnownLocation(context);
            if (isUsable(fallback)) {
                finishWithLocation(
                        callback,
                        fallback);
            } else {
                finishWithError(
                        callback,
                        R.string.runtime_text_0571);
            }
        };
        handler.postDelayed(
                timeout,
                20_000L);

        for (String provider : providers) {
            try {
                Location cached =
                        manager.getLastKnownLocation(provider);
                if (isUsable(cached)
                        && betterThan(cached, best)) {
                    best = cached;
                    callback.onLocation(cached);
                }
            } catch (RuntimeException ignored) {
            }

            final String selectedProvider = provider;
            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    manager.getCurrentLocation(
                            selectedProvider,
                            null,
                            context.getMainExecutor(),
                            location -> {
                                if (finished
                                        || !isUsable(location)) {
                                    return;
                                }
                                finishWithLocation(
                                        callback,
                                        location);
                            });
                } catch (RuntimeException ignored) {
                }
                continue;
            }

            LocationListener listener =
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(
                                Location location) {
                            if (finished
                                    || !isUsable(location)) {
                                return;
                            }
                            finishWithLocation(
                                    callback,
                                    location);
                        }

                        @Override public void onStatusChanged(
                                String provider,
                                int status,
                                Bundle extras) {}

                        @Override public void onProviderEnabled(
                                String provider) {}

                        @Override public void onProviderDisabled(
                                String provider) {}
                    };
            listeners.add(listener);
            try {
                manager.requestSingleUpdate(
                        selectedProvider,
                        listener,
                        Looper.getMainLooper());
            } catch (RuntimeException ignored) {
            }
        }
    }

    private List<String> enabledProviders(
            LocationManager manager) {
        List<String> result =
                new ArrayList<>();
        for (String provider : new String[]{
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER}) {
            try {
                if (manager.isProviderEnabled(provider)) {
                    result.add(provider);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return result;
    }

    private boolean isUsable(Location location) {
        if (location == null) return false;
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        return !Double.isNaN(latitude)
                && !Double.isNaN(longitude)
                && latitude >= -90d
                && latitude <= 90d
                && longitude >= -180d
                && longitude <= 180d;
    }

    private boolean betterThan(
            Location candidate,
            Location current) {
        if (!isUsable(candidate)) return false;
        if (!isUsable(current)) return true;

        long candidateAge =
                Math.max(
                        0L,
                        System.currentTimeMillis()
                                - candidate.getTime());
        long currentAge =
                Math.max(
                        0L,
                        System.currentTimeMillis()
                                - current.getTime());

        if (candidateAge + 30_000L < currentAge) {
            return true;
        }
        if (currentAge + 30_000L < candidateAge) {
            return false;
        }
        return candidate.hasAccuracy()
                && (!current.hasAccuracy()
                || candidate.getAccuracy()
                < current.getAccuracy());
    }

    private void finishWithLocation(
            Callback callback,
            Location location) {
        if (finished || !isUsable(location)) return;
        finished = true;
        QiblaUtils.remember(
                context,
                location.getLatitude(),
                location.getLongitude());
        cancelInternal();
        callback.onLocation(location);
    }

    private void finishWithError(
            Callback callback,
            int stringRes) {
        if (finished) return;
        finished = true;
        cancelInternal();
        callback.onError(stringRes);
    }

    public void cancel() {
        finished = true;
        cancelInternal();
    }

    private void cancelInternal() {
        if (timeout != null) {
            handler.removeCallbacks(timeout);
            timeout = null;
        }
        if (manager != null) {
            for (LocationListener listener : listeners) {
                try {
                    manager.removeUpdates(listener);
                } catch (RuntimeException ignored) {
                }
            }
        }
        listeners.clear();
        manager = null;
    }
}
