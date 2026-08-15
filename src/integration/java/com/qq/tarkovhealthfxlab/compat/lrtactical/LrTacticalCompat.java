package com.qq.tarkovhealthfxlab.compat.lrtactical;

import me.xjqsh.lrtactical.api.event.ConsumableUseEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Direct LR Tactical 0.4.1 adapter, packaged only by the withIntegrations profile. */
public final class LrTacticalCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(LrTacticalCompat.class);
    private static volatile MedicalActionSink sink = MedicalActionSink.IGNORE;
    private static boolean registered;
    private static boolean warnedCallbackFailure;

    private LrTacticalCompat() {
    }

    public static synchronized void install(MedicalActionSink newSink) {
        sink = newSink == null ? MedicalActionSink.IGNORE : newSink;
        if (!registered) {
            MinecraftForge.EVENT_BUS.addListener(LrTacticalCompat::onConsumableUse);
            registered = true;
            LOGGER.info("LR Tactical 0.4.1 medical compatibility enabled");
        }
    }

    private static void onConsumableUse(ConsumableUseEvent event) {
        if (!(event.getUser() instanceof ServerPlayer player)) {
            return;
        }
        String consumableId = event.getConsumableId().toString();
        LrConsumablePolicy.actionFor(consumableId).ifPresent(action -> {
            try {
                sink.onMedicalAction(
                        player,
                        new MedicalActionSink.MedicalActionRequest(action, consumableId, true)
                );
            } catch (LinkageError | RuntimeException exception) {
                if (!warnedCallbackFailure) {
                    warnedCallbackFailure = true;
                    LOGGER.warn("LR Tactical medical callback failed; preserving LR's normal completed use", exception);
                }
            }
        });
    }
}
