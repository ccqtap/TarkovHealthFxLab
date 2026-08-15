package com.qq.tarkovhealthfxlab.common.health;

/** Ordinary healing is deliberately unable to recover a blackened limb. */
public enum HealingSource {
    ORDINARY(false),
    REPAIR(true),
    REGENERATION(true);

    private final boolean unlocksBlackenedLimb;

    HealingSource(boolean unlocksBlackenedLimb) {
        this.unlocksBlackenedLimb = unlocksBlackenedLimb;
    }

    public boolean unlocksBlackenedLimb() {
        return this.unlocksBlackenedLimb;
    }
}
