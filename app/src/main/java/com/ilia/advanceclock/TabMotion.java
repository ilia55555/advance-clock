package com.ilia.advanceclock;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.PathInterpolator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** A button-driven horizontal page transition; deliberately has no swipe/scroll handling. */
final class TabMotion {
    private final Map<String, View> panels;
    private final Set<String> warmed = new HashSet<>();
    private String selected;
    private int generation;

    TabMotion(Map<String, View> panels) { this.panels = panels; }

    void show(String tab, int direction, boolean animate) {
        String previous = selected;
        selected = tab;
        int token = ++generation;
        for (View panel : panels.values()) {
            panel.animate().cancel();
            panel.setTranslationX(0); panel.setAlpha(1);
        }
        View incoming = panels.get(tab), outgoing = panels.get(previous);
        if (incoming == null) return;
        warmed.add(tab);
        int width = incoming.getWidth();
        if (width <= 0 && incoming.getParent() instanceof View) width = ((View) incoming.getParent()).getWidth();
        boolean moving = animate && ValueAnimator.areAnimatorsEnabled() && outgoing != null
                && outgoing != incoming && width > 0;
        for (Map.Entry<String, View> entry : panels.entrySet()) {
            View panel = entry.getValue();
            panel.setVisibility(panel == incoming || (moving && panel == outgoing)
                    ? View.VISIBLE : warmed.contains(entry.getKey()) ? View.INVISIBLE : View.GONE);
            panel.setImportantForAccessibility(panel == incoming
                    ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
        if (!moving) return;
        float distance = Math.signum(direction) * width;
        incoming.setTranslationX(distance);
        incoming.postOnAnimation(() -> {
            if (token != generation) return;
            PathInterpolator curve = new PathInterpolator(.22f, .61f, .36f, 1f);
            outgoing.animate().translationX(-distance).setDuration(300).setInterpolator(curve).start();
            incoming.animate().translationX(0).setDuration(300).setInterpolator(curve)
                    .withEndAction(() -> { if (token == generation) finish(); }).start();
        });
    }

    void finish() {
        ++generation;
        for (Map.Entry<String, View> entry : panels.entrySet()) {
            View panel = entry.getValue();
            panel.animate().cancel(); panel.setTranslationX(0); panel.setAlpha(1);
            panel.setVisibility(entry.getKey().equals(selected) ? View.VISIBLE
                    : warmed.contains(entry.getKey()) ? View.INVISIBLE : View.GONE);
        }
    }
}
