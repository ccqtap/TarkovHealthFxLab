package com.qq.tarkovhealthfxlab.common.health;

public record DamageApplication(BodyPart originalPart, BodyPart appliedPart, float amount,
                                long seed, boolean redirected) {
}
