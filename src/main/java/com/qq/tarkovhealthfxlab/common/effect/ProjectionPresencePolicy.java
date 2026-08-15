package com.qq.tarkovhealthfxlab.common.effect;

/** Pure transition policy separating externally timed effects from authoritative projections. */
public final class ProjectionPresencePolicy {
    public enum Action {
        NONE,
        APPLY_EXTERNAL,
        CLEAR_EXTERNAL,
        RESTORE_PROJECTION
    }

    private ProjectionPresencePolicy() {
    }

    public static Action decide(boolean active, boolean wasActive, boolean projected) {
        if (active && !wasActive && !projected) return Action.APPLY_EXTERNAL;
        if (!active && wasActive && !projected) return Action.CLEAR_EXTERNAL;
        if (!active && projected) return Action.RESTORE_PROJECTION;
        return Action.NONE;
    }

    /** Reconciliation may restore ownership, but must never promote an external timed effect. */
    public static int retainOwnedProjectionMask(int previousProjectedMask, int desiredTruthMask) {
        return previousProjectedMask & desiredTruthMask;
    }

    public static boolean shouldRemoveEffect(
            boolean owned,
            boolean presentationChanged,
            boolean desiredAfter,
            boolean externalSourceTransition
    ) {
        return !desiredAfter
                && (owned || presentationChanged)
                && (owned || !externalSourceTransition);
    }
}
