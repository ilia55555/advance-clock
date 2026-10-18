package com.ilia.advanceclock;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ReminderLayoutTest {
    @Test public void shortcutsStayAtExactScreenCenterOnPhone() {
        ReminderLayout.Placement p = ReminderLayout.centered(800, 180, 620, 164);
        assertEquals(400, p.top + p.height / 2);
        assertEquals(164, p.height);
    }
    @Test public void tabletsUseTheirActualScreenCenter() {
        ReminderLayout.Placement p = ReminderLayout.centered(1600, 240, 1350, 220);
        assertEquals(800, p.top + p.height / 2);
    }
    @Test public void longNotesAndLargeControlsNeverOverlapTheAttachments() {
        ReminderLayout.Placement p = ReminderLayout.centered(560, 180, 330, 220);
        assertTrue(p.top >= 180);
        assertTrue(p.top + p.height <= 330);
        assertEquals(150, p.height);
    }
    @Test public void headerGrowthMovesShortcutsOnlyAsMuchAsNeeded() {
        ReminderLayout.Placement p = ReminderLayout.centered(800, 340, 620, 200);
        assertEquals(340, p.top);
        assertEquals(200, p.height);
    }
}
