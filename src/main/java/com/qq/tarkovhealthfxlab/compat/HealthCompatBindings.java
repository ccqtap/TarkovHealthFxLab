package com.qq.tarkovhealthfxlab.compat;

import com.qq.tarkovhealthfxlab.common.health.BodyPart;
import com.qq.tarkovhealthfxlab.common.health.HealthRuleService;
import com.qq.tarkovhealthfxlab.common.health.InjuryState;
import com.qq.tarkovhealthfxlab.compat.lrtactical.MedicalCompatTuning;
import com.qq.tarkovhealthfxlab.compat.tacz.ArmRecoilTuning;
import com.qq.tarkovhealthfxlab.compat.tacz.InjuryCompatState;
import com.qq.tarkovhealthfxlab.network.ClientInjuryState;

import java.util.UUID;

/** Connects optional-mod adapters to the v2 health authority without exposing either API to the other. */
public final class HealthCompatBindings {
    private HealthCompatBindings() {
    }

    public static ThirdPartyCompatBootstrap.Availability install() {
        return install(ArmRecoilTuning.DEFAULT, MedicalCompatTuning.DEFAULT);
    }

    public static ThirdPartyCompatBootstrap.Availability install(
            ArmRecoilTuning recoilTuning,
            MedicalCompatTuning medicalTuning
    ) {
        MedicalCompatTuning safeMedicalTuning = medicalTuning == null
                ? MedicalCompatTuning.DEFAULT
                : medicalTuning;
        return ThirdPartyCompatBootstrap.install(
                HealthCompatBindings::clientArmState,
                recoilTuning,
                (player, request) -> {
                    switch (request.action()) {
                        case REPAIR -> {
                            HealthRuleService.applyRepair(player);
                            HealthRuleService.consumeActiveRegenerationTreatment(player);
                        }
                        case ANALGESIA -> HealthRuleService.applyAnalgesia(
                                player,
                                safeMedicalTuning.ibuprofenAnalgesiaDurationTicks()
                        );
                    }
                    // LR applies remove_effects before ConsumableUseEvent.
                    // Re-project from truth in the same server tick so a broad
                    // third-party removal can never become an injury clear.
                    HealthRuleService.reconcileAuthoritativeEffects(player);
                }
        );
    }

    static InjuryCompatState.ArmInjuryState clientArmState(UUID playerId) {
        ClientInjuryState.Snapshot snapshot = ClientInjuryState.get(playerId);
        return armState(snapshot.state(), snapshot.analgesia());
    }

    static InjuryCompatState.ArmInjuryState armState(InjuryState state, boolean analgesia) {
        int blackened = countBlackened(state, BodyPart.LEFT_ARM) + countBlackened(state, BodyPart.RIGHT_ARM);
        int fractureOnly = countFractureOnly(state, BodyPart.LEFT_ARM)
                + countFractureOnly(state, BodyPart.RIGHT_ARM);
        return new InjuryCompatState.ArmInjuryState(fractureOnly, blackened, analgesia);
    }

    private static int countBlackened(InjuryState state, BodyPart arm) {
        return state.isBlackened(arm) ? 1 : 0;
    }

    private static int countFractureOnly(InjuryState state, BodyPart arm) {
        return state.isFractured(arm) && !state.isBlackened(arm) ? 1 : 0;
    }
}
