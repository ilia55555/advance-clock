package com.ilia.advanceclock;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.PathInterpolator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Shared horizontal page animation for tab buttons and swipe navigation. */
final class TabMotion {
    private final Map<String, View> panels;
    private final Set<String> warmed = new HashSet<>();
    private String selected;
    private int generation;
    private View blocker;
    private ValueAnimator animator;
    interface ProgressListener { void update(String from, String to, float progress); }
    private ProgressListener progressListener;

    void setProgressListener(ProgressListener listener) { progressListener = listener; }

    private void progress(String from, String to, float fraction) {
        if (progressListener != null) progressListener.update(from, to, fraction);
    }

    private void cancelAnimation() {
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
            animator = null;
        }
    }

    private void animatePages(String from, String to, View outgoing, View incoming,
            float distance, float start, float end, long duration, Runnable complete) {
        animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(duration);
        animator.setInterpolator(new PathInterpolator(.22f, .61f, .36f, 1f));
        animator.addUpdateListener(animation -> {
            float offset = (float) animation.getAnimatedValue();
            outgoing.setTranslationX(offset);
            incoming.setTranslationX(distance + offset);
            progress(from, to, -offset / distance);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                animator = null;
                complete.run();
            }
        });
        animator.start();
    }

    private void unblock() {
        if (blocker != null && blocker.getParent() instanceof android.view.ViewGroup)
            ((android.view.ViewGroup) blocker.getParent()).removeView(blocker);
        blocker = null;
    }

    TabMotion(Map<String, View> panels) { this.panels = panels; }

    void show(String tab, int direction, boolean animate) {
        String previous = selected;
        selected = tab;
        int token = ++generation;
        unblock();
        cancelAnimation();
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
        if (!moving) { progress(tab, tab, 1); return; }
        progress(previous, tab, 0);
        if (incoming.getParent() instanceof android.view.ViewGroup) {
            blocker = new View(incoming.getContext());
            blocker.setClickable(true);
            blocker.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            ((android.view.ViewGroup) incoming.getParent()).addView(blocker,
                    new android.view.ViewGroup.LayoutParams(-1, -1));
        }
        float distance = Math.signum(direction) * width;
        incoming.setTranslationX(distance);
        incoming.postOnAnimation(() -> {
            if (token != generation) return;
            animatePages(previous, tab, outgoing, incoming, distance, 0, -distance, 300,
                    () -> { if (token == generation) finish(); });
        });
    }

    private String dragTarget;
    private float dragDistance;
    private float dragOffset;

    boolean beginDrag(String target, float deltaX) {
        finish();
        View incoming = panels.get(target), outgoing = panels.get(selected);
        if (incoming == null || outgoing == null || incoming == outgoing) return false;
        int width = ((View) outgoing.getParent()).getWidth();
        if (width <= 0) return false;
        dragOffset = 0;
        dragTarget = target;
        dragDistance = deltaX < 0 ? width : -width;
        warmed.add(target);
        incoming.setVisibility(View.VISIBLE);
        incoming.setTranslationX(dragDistance);
        return true;
    }

    void drag(float deltaX) {
        if (dragTarget == null) return;
        dragOffset = dragDistance > 0 ? Math.max(-dragDistance, Math.min(0, deltaX))
                : Math.min(-dragDistance, Math.max(0, deltaX));
        panels.get(selected).setTranslationX(dragOffset);
        panels.get(dragTarget).setTranslationX(dragDistance + dragOffset);
        progress(selected, dragTarget, -dragOffset / dragDistance);
    }

    void release(boolean cancel, float velocity, Runnable committed) {
        if (dragTarget == null) return;
        final String target = dragTarget;
        boolean forwardFling = velocity * dragDistance < 0 && Math.abs(velocity) > 650;
        boolean backwardFling = velocity * dragDistance > 0 && Math.abs(velocity) > 650;
        boolean commit = !cancel && !backwardFling
                && (Math.abs(dragOffset) > Math.abs(dragDistance) * .35f || forwardFling);
        View outgoing = panels.get(selected), incoming = panels.get(target);
        int token = ++generation;
        float end = commit ? -dragDistance : 0;
        long duration = (long) (180 + 140 * Math.abs(end - dragOffset) / Math.abs(dragDistance));
        dragTarget = null;
        Runnable complete = () -> {
            if (token != generation) return;
            if (commit) selected = target;
            finish();
            if (commit) committed.run();
        };
        if (!ValueAnimator.areAnimatorsEnabled()) {
            complete.run();
            return;
        }
        animatePages(selected, target, outgoing, incoming, dragDistance, dragOffset, end,
                duration, complete);
    }

    void finish() {
        dragTarget = null;
        ++generation;
        cancelAnimation();
        unblock();
        progress(selected, selected, 1);
        for (Map.Entry<String, View> entry : panels.entrySet()) {
            View panel = entry.getValue();
            panel.animate().cancel(); panel.setTranslationX(0); panel.setAlpha(1);
            panel.setVisibility(entry.getKey().equals(selected) ? View.VISIBLE
                    : warmed.contains(entry.getKey()) ? View.INVISIBLE : View.GONE);
        }
    }
}
