package com.ilia.advanceclock;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class TimeToolPresetStoreTest {
    @Test public void multipleNamedTimersAndPhotosSurviveReload() throws Exception {
        JSONObject first = new JSONObject().put("id", "a").put("label", "چای")
                .put("duration", 180000L).put("image1", "content://photos/1")
                .put("image2", "content://photos/2");
        String saved = TimeToolPresetStore.save("[]", first);
        saved = TimeToolPresetStore.save(saved, new JSONObject().put("id", "b")
                .put("label", "Study").put("duration", 1500000L));
        JSONArray restored = TimeToolPresetStore.read(saved);
        assertEquals(2, restored.length());
        assertEquals("چای", restored.getJSONObject(0).getString("label"));
        assertEquals(180000L, restored.getJSONObject(0).getLong("duration"));
        assertEquals("content://photos/1", restored.getJSONObject(0).getString("image1"));
        assertEquals("content://photos/2", restored.getJSONObject(0).getString("image2"));
    }

    @Test public void editingDoesNotDuplicateOrOverwriteAnotherItem() throws Exception {
        String saved = TimeToolPresetStore.save("[]", new JSONObject().put("id", "a").put("mode", 0));
        saved = TimeToolPresetStore.save(saved, new JSONObject().put("id", "b").put("mode", 2).put("target", 1800000000000L));
        saved = TimeToolPresetStore.save(saved, new JSONObject().put("id", "a").put("mode", 1).put("duration", 60000L));
        JSONArray restored = TimeToolPresetStore.read(saved);
        assertEquals(2, restored.length());
        assertEquals(1, restored.getJSONObject(0).getInt("mode"));
        assertEquals(1800000000000L, restored.getJSONObject(1).getLong("target"));
    }

    @Test public void deletingOneItemPreservesTheOthers() throws Exception {
        String saved = TimeToolPresetStore.save("[]", new JSONObject().put("id", "a"));
        saved = TimeToolPresetStore.save(saved, new JSONObject().put("id", "b"));
        JSONArray restored = TimeToolPresetStore.read(TimeToolPresetStore.delete(saved, "a"));
        assertEquals(1, restored.length());
        assertEquals("b", restored.getJSONObject(0).getString("id"));
    }

    @Test public void attachmentsAndBackgroundChoiceSurviveSavingAndReplay() throws Exception {
        String attachments = "[{\"kind\":\"url\",\"value\":\"https://example.com\"},"
                + "{\"kind\":\"app\",\"value\":\"com.example.app\"},"
                + "{\"kind\":\"file\",\"value\":\"content://documents/1\"}]";
        JSONObject config = new JSONObject().put("id", "media").put("attachments", attachments)
                .put("background", true).put("image1", "content://photos/cropped")
                .put("source1", "content://photos/original");
        JSONObject restored = TimeToolPresetStore.read(TimeToolPresetStore.save("[]", config)).getJSONObject(0);
        assertTrue(restored.getBoolean("background"));
        assertEquals(3, new JSONArray(restored.getString("attachments")).length());
        assertEquals("content://photos/original", restored.getString("source1"));
    }

    @Test public void invalidRecordsDoNotBreakTheSavedList() {
        assertEquals(0, TimeToolPresetStore.read("broken").length());
        assertEquals(1, TimeToolPresetStore.read("[null,3,{}, {\"id\":\"valid\"}]").length());
    }
    @Test public void displayShowsRunningThenClosestThenExpiredConfigurations() throws Exception {
        String saved = "[]";
        saved = TimeToolPresetStore.save(saved, new JSONObject().put("id", "expired").put("dateMode", true).put("target", 500));
        saved = TimeToolPresetStore.save(saved, new JSONObject().put("id", "later").put("dateMode", true).put("target", 5000));
        saved = TimeToolPresetStore.save(saved, new JSONObject().put("id", "soon").put("duration", 1000));
        saved = TimeToolPresetStore.save(saved, new JSONObject().put("id", "running").put("duration", 9000));
        JSONArray items = TimeToolPresetStore.ordered(saved, false, 1000, "running");
        assertEquals("running", items.getJSONObject(0).getString("id"));
        assertEquals("soon", items.getJSONObject(1).getString("id"));
        assertEquals("later", items.getJSONObject(2).getString("id"));
        assertEquals("expired", items.getJSONObject(3).getString("id"));
    }

    @Test public void matchingDurationsPreferNewestAndStopwatchDatesRemainFixed() throws Exception {
        String saved = TimeToolPresetStore.save("[]", new JSONObject().put("id", "old").put("mode", 1).put("duration", 1000).put("savedAt", 10));
        saved = TimeToolPresetStore.save(saved, new JSONObject().put("id", "new").put("mode", 1).put("duration", 1000).put("savedAt", 20));
        saved = TimeToolPresetStore.save(saved, new JSONObject().put("id", "past").put("mode", 2).put("target", 500).put("applyDate", false));
        JSONArray items = TimeToolPresetStore.ordered(saved, true, 1000, "");
        assertEquals("new", items.getJSONObject(0).getString("id"));
        assertEquals("old", items.getJSONObject(1).getString("id"));
        assertEquals("past", items.getJSONObject(2).getString("id"));
    }

}
