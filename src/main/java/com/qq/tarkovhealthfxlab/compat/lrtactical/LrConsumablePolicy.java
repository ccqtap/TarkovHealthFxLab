package com.qq.tarkovhealthfxlab.compat.lrtactical;

import java.util.Locale;
import java.util.Optional;

/** Exact ID mapping; item display names and base item IDs are intentionally ignored. */
public final class LrConsumablePolicy {
    public static final String BLOOD_PACK = "lrtactical:blood_pack";
    public static final String IBUPROFEN = "lrtactical:ibuprofen";

    private LrConsumablePolicy() {
    }

    public static Optional<MedicalActionSink.MedicalAction> actionFor(String consumableId) {
        if (consumableId == null) {
            return Optional.empty();
        }
        return switch (consumableId.trim().toLowerCase(Locale.ROOT)) {
            case BLOOD_PACK -> Optional.of(MedicalActionSink.MedicalAction.REPAIR);
            case IBUPROFEN -> Optional.of(MedicalActionSink.MedicalAction.ANALGESIA);
            default -> Optional.empty();
        };
    }
}
