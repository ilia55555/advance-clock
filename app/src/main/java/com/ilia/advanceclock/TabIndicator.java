package com.ilia.advanceclock;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import java.util.Map;

/** Draws the underline at the page's exact progress, including subpixel positions. */
final class TabIndicator {
    private final ViewGroup bar;
    private final Map<String, View> markers;
    private final Drawable background;
    private String from, to;
    private float fraction;
    private final Drawable underline = new Drawable() {
        @Override public void draw(Canvas canvas) {
            View start = markers.get(from), end = markers.get(to);
            if (start == null || end == null) return;
            Rect a = bounds(start), b = bounds(end);
            float left = blend(a.left, b.left), top = blend(a.top, b.top);
            float width = blend(a.width(), b.width()), height = blend(a.height(), b.height());
            if (a.width() <= 0 || a.height() <= 0) return;
            int save = canvas.save();
            canvas.translate(left, top);
            canvas.scale(width / a.width(), height / a.height());
            background.setBounds(0, 0, a.width(), a.height());
            background.draw(canvas);
            canvas.restoreToCount(save);
        }
        @Override public void setAlpha(int alpha) { background.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter filter) { background.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    };

    TabIndicator(ViewGroup bar, Map<String, View> markers) {
        this.bar = bar;
        this.markers = markers;
        Drawable source = markers.values().iterator().next().getBackground();
        background = source.getConstantState() != null
                ? source.getConstantState().newDrawable().mutate() : source.mutate();
        for (View marker : markers.values()) marker.setVisibility(View.INVISIBLE);
        bar.getOverlay().add(underline);
        bar.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> redraw());
        for (View marker : markers.values())
            marker.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> redraw());
    }

    void update(String from, String to, float fraction) {
        this.from = from;
        this.to = to;
        this.fraction = Math.max(0, Math.min(1, fraction));
        redraw();
    }

    private Rect bounds(View marker) {
        Rect rect = new Rect(0, 0, marker.getWidth(), marker.getHeight());
        bar.offsetDescendantRectToMyCoords(marker, rect);
        return rect;
    }

    private void redraw() {
        underline.setBounds(0, 0, bar.getWidth(), bar.getHeight());
        underline.invalidateSelf();
    }

    private float blend(int start, int end) {
        return start + (end - start) * fraction;
    }
}
