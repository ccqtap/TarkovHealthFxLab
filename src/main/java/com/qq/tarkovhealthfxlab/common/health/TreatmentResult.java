package com.qq.tarkovhealthfxlab.common.health;

import java.util.Optional;

public record TreatmentResult(TreatedCondition condition, BodyPart part, boolean painCleared) {
    public enum TreatedCondition {
        NONE,
        FRACTURE,
        BLACKENED
    }

    public Optional<BodyPart> treatedPart() {
        return Optional.ofNullable(this.part);
    }

    public boolean changed() {
        return this.condition != TreatedCondition.NONE || this.painCleared;
    }
}
