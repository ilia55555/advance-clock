package com.ilia.advanceclock;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public class MediaWidgetAuthorizationTest {
    private final Context app = RuntimeEnvironment.getApplication();
    private void saved(int id, String uri) {
        AppString.init(app);
        AppWidgetProviderInfo info = new AppWidgetProviderInfo();
        info.provider = new ComponentName(app, MediaWidgetProvider.class);
        Shadows.shadowOf(AppWidgetManager.getInstance(app)).putWidgetInfo(id, info);
        MediaWidgetPrefs.save(app, id, Collections.singletonList(new MediaWidgetPrefs.Item(uri, "Saved name", "audio/mpeg")));
    }
    @Test public void invalidWidgetUnselectedUriAndRemoteUriAreRejected() {
        saved(42, "content://documents/selected");
        assertNull(MediaWidgetPrefs.authorizedItem(app, -1, "content://documents/selected"));
        assertNull(MediaWidgetPrefs.authorizedItem(app, 99, "content://documents/selected"));
        assertNull(MediaWidgetPrefs.authorizedItem(app, 42, "content://documents/different"));
        saved(43, "https://example.invalid/audio");
        assertNull(MediaWidgetPrefs.authorizedItem(app, 43, "https://example.invalid/audio"));
    }
    @Test public void receiverUsesStoredMetadataAndRejectsCraftedCommands() {
        saved(42, "content://documents/selected");
        final Intent[] started = {null};
        Context capture = new ContextWrapper(app) {
            @Override public ComponentName startForegroundService(Intent intent) {
                started[0] = intent; return intent.getComponent();
            }
        };
        MediaWidgetActionReceiver receiver = new MediaWidgetActionReceiver();
        Intent command = new Intent(MediaWidgetActionReceiver.ACTION_TOGGLE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 42)
                .putExtra(MediaWidgetActionReceiver.EXTRA_URI, "content://documents/selected")
                .putExtra(MediaWidgetActionReceiver.EXTRA_MIME, "application/vnd.android.package-archive")
                .putExtra(MediaWidgetActionReceiver.EXTRA_NAME, "forged.apk");
        receiver.onReceive(capture, command);
        assertNotNull(started[0]);
        assertEquals("audio/mpeg", started[0].getStringExtra(MediaWidgetActionReceiver.EXTRA_MIME));
        assertEquals("Saved name", started[0].getStringExtra(MediaWidgetActionReceiver.EXTRA_NAME));
        started[0] = null;
        command.putExtra(MediaWidgetActionReceiver.EXTRA_URI, "https://example.invalid/unselected");
        receiver.onReceive(capture, command);
        assertNull(started[0]);
        command.removeExtra(AppWidgetManager.EXTRA_APPWIDGET_ID);
        receiver.onReceive(capture, command);
        assertNull(started[0]);
    }
    @Test public void playbackServiceIndependentlyRejectsUnselectedCommands() {
        AppString.init(app);
        MediaWidgetPlaybackService service = org.robolectric.Robolectric.buildService(MediaWidgetPlaybackService.class).create().get();
        Intent command = new Intent(MediaWidgetPlaybackService.ACTION_TOGGLE)
                .putExtra(MediaWidgetActionReceiver.EXTRA_URI, "https://example.invalid/forged");
        service.onStartCommand(command, 0, 1);
        assertNull(org.robolectric.util.ReflectionHelpers.getField(service, "player"));
        service.onDestroy();
    }
    @Test public void anotherWidgetProviderCannotAuthorizeMediaCommands() {
        saved(42, "content://documents/selected");
        AppWidgetProviderInfo other = new AppWidgetProviderInfo();
        other.provider = new ComponentName(app, ClockWidgetProvider.class);
        Shadows.shadowOf(AppWidgetManager.getInstance(app)).putWidgetInfo(42, other);
        assertNull(MediaWidgetPrefs.authorizedItem(app, 42, "content://documents/selected"));
    }

}
