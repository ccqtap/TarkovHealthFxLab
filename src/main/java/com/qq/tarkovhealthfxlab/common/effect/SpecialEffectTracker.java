package com.qq.tarkovhealthfxlab.common.effect;

/** Pure edge/consumption state for one-shot treatment effects. */
final class SpecialEffectTracker {
    private static final int REPAIR = 1;
    private static final int REGENERATION = 1 << 1;
    private static final int ANALGESIA = 1 << 2;
    private static final int REPAIR_USED = 1 << 3;
    private static final int REGENERATION_USED = 1 << 4;

    private SpecialEffectTracker() {
    }

    static int carryActive(int previous, boolean repair, boolean regeneration, boolean analgesia) {
        int current = (repair ? REPAIR : 0)
                | (regeneration ? REGENERATION : 0)
                | (analgesia ? ANALGESIA : 0);
        if (repair && repairUsed(previous)) current |= REPAIR_USED;
        if (regeneration && regenerationUsed(previous)) current |= REGENERATION_USED;
        return current;
    }

    static boolean repairUsed(int marker) {
        return (marker & REPAIR_USED) != 0;
    }

    static boolean regenerationUsed(int marker) {
        return (marker & REGENERATION_USED) != 0;
    }

    static int markRepairUsed(int marker) {
        return marker | REPAIR | REPAIR_USED;
    }

    static int markRegenerationUsed(int marker) {
        return marker | REGENERATION | REGENERATION_USED;
    }

    static int rearmRepair(int marker) {
        return marker & ~REPAIR_USED;
    }

    static int rearmRegeneration(int marker) {
        return marker & ~REGENERATION_USED;
    }
}
