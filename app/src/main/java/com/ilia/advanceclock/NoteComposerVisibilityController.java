package com.ilia.advanceclock;

import android.app.Activity;
import android.view.View;

import java.util.WeakHashMap;

/**
 * Keeps the inline note composer consistent with the selected home layout.
 *
 * Rules:
 * - Inline/no-plus layout: always show the composer.
 * - Compact/+ layout: show the composer only while there are no notes yet.
 * - The legacy inline sketch card is always hidden; drawing lives in its modal.
 */
final class NoteComposerVisibilityController {
    private static final WeakHashMap<Activity, Boolean> INSTALLED = new WeakHashMap<>();

    private NoteComposerVisibilityController() {}

    static void apply(Activity activity) {
        if (!(activity instanceof MainActivity)) return;

        View composer = activity.findViewById(R.id.note_composer_card);
        View sketchCard = activity.findViewById(R.id.note_sketch_card);
        View noteList = activity.findViewById(R.id.noforget_list);
        if (composer == null) return;

        if (sketchCard != null && sketchCard.getVisibility() != View.GONE) {
            sketchCard.setVisibility(View.GONE);
        }

        boolean compact = AppSettings.clockLayoutMode(activity)
                == AppSettings.CLOCK_LAYOUT_CALENDAR_FIRST;
        boolean hasNotes = !new NoForgetStore(activity).all().isEmpty();
        boolean shouldShowComposer = !compact || !hasNotes;
        int wanted = shouldShowComposer ? View.VISIBLE : View.GONE;
        if (composer.getVisibility() != wanted) composer.setVisibility(wanted);

        if (noteList != null && !Boolean.TRUE.equals(INSTALLED.get(activity))) {
            INSTALLED.put(activity, true);
            noteList.addOnLayoutChangeListener((v, left, top, right, bottom,
                                                 oldLeft, oldTop, oldRight, oldBottom) ->
                    apply(activity));
        }
    }

    static void forget(Activity activity) {
        INSTALLED.remove(activity);
    }
}
