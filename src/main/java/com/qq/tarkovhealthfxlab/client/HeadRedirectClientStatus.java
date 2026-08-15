package com.qq.tarkovhealthfxlab.client;

import java.util.Locale;
import java.util.Objects;

/** Small client cache fed by the server command feedback shown in normal chat. */
public final class HeadRedirectClientStatus {
    private static State state = State.UNKNOWN;
    private static String lastRecord = "none";

    private HeadRedirectClientStatus() {
    }

    public static State state() {
        return state;
    }

    public static String lastRecord() {
        return lastRecord;
    }

    public static boolean accept(String message) {
        String text = Objects.requireNonNullElse(message, "").trim();
        if (text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        boolean recognized = false;
        int switchIndex = lower.indexOf("head_redirect=");
        if (switchIndex >= 0) {
            String value = lower.substring(switchIndex + "head_redirect=".length());
            state = value.startsWith("true") ? State.ENABLED
                    : value.startsWith("false") ? State.DISABLED : State.UNKNOWN;
            recognized = true;
        }
        int lastIndex = lower.indexOf("last:");
        if (lastIndex >= 0) {
            lastRecord = text.substring(lastIndex + "last:".length()).trim();
            recognized = true;
        } else if (lower.startsWith("seed=") && lower.contains("original=")
                && lower.contains("target=")) {
            lastRecord = text;
            recognized = true;
        } else if (lower.contains("last=none")) {
            lastRecord = "none";
            recognized = true;
        }
        return recognized;
    }

    public static void reset() {
        state = State.UNKNOWN;
        lastRecord = "none";
    }

    public enum State {
        UNKNOWN,
        ENABLED,
        DISABLED
    }
}
