package com.qq.tarkovhealthfxlab.compat.tacz;

import com.qq.tarkovhealthfxlab.compat.tacz.ArmRecoilTuning.RecoilMultipliers;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.modifier.ParameterizedCache;
import com.tacz.guns.api.modifier.ParameterizedCachePair;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Direct TaCZ 1.1.8 adapter, packaged only by the withIntegrations profile. */
public final class TaCZClientCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaCZClientCompat.class);
    private static volatile InjuryCompatState injuryState = InjuryCompatState.NONE;
    private static volatile ArmRecoilTuning tuning = ArmRecoilTuning.DEFAULT;
    private static boolean registered;
    private static UUID lastPlayerId;
    private static InjuryCompatState.ArmInjuryState lastState;
    private static Item lastItem;
    private static CompoundTag lastTag;
    private static boolean warnedUnsupportedTransform;
    private static boolean warnedEventFailure;

    private TaCZClientCompat() {
    }

    public static synchronized void install(InjuryCompatState state, ArmRecoilTuning newTuning) {
        injuryState = state == null ? InjuryCompatState.NONE : state;
        tuning = newTuning == null ? ArmRecoilTuning.DEFAULT : newTuning;
        resetFingerprint();
        if (!registered) {
            MinecraftForge.EVENT_BUS.addListener(TaCZClientCompat::onAttachmentProperty);
            MinecraftForge.EVENT_BUS.addListener(TaCZClientCompat::onClientTick);
            registered = true;
            LOGGER.info("TaCZ 1.1.8 injury recoil compatibility enabled");
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            resetFingerprint();
            return;
        }

        ItemStack gunStack = player.getMainHandItem();
        InjuryCompatState.ArmInjuryState currentState = injuryState.safeArmInjuries(player.getUUID());
        Item currentItem = gunStack.getItem();
        CompoundTag currentTag = gunStack.getTag() == null ? null : gunStack.getTag().copy();
        boolean changed = !player.getUUID().equals(lastPlayerId)
                || !currentState.equals(lastState)
                || currentItem != lastItem
                || !Objects.equals(currentTag, lastTag);
        if (!changed) {
            return;
        }

        lastPlayerId = player.getUUID();
        lastState = currentState;
        lastItem = currentItem;
        lastTag = currentTag;
        // Public TaCZ rebuild entry point. It returns immediately for non-guns.
        // Every rebuild starts from the gun + attachment data, so removing an
        // injury restores the unmodified cache instead of dividing old values.
        try {
            AttachmentPropertyManager.postChangeEvent(player, gunStack);
        } catch (LinkageError | RuntimeException exception) {
            LOGGER.warn("TaCZ recoil cache refresh failed; injury recoil is disabled for this update", exception);
        }
    }

    private static void onAttachmentProperty(AttachmentPropertyEvent event) {
        try {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || event.getGunItem() != player.getMainHandItem()) {
                return;
            }

            RecoilMultipliers multipliers = tuning.multipliers(injuryState.safeArmInjuries(player.getUUID()));
            if (multipliers.isIdentity()) {
                return;
            }

            ParameterizedCachePair<Float, Float> recoil = event.getCacheProperty().getCache(GunProperties.RECOIL);
            if (recoil == null) {
                return;
            }
            ParameterizedCachePair<Float, Float> scaled = scaleRecoil(recoil, multipliers);
            if (scaled != null) {
                event.getCacheProperty().setCache(GunProperties.RECOIL, scaled);
            }
        } catch (LinkageError | RuntimeException exception) {
            if (!warnedEventFailure) {
                warnedEventFailure = true;
                LOGGER.warn("TaCZ recoil event API is incompatible; preserving the original recoil", exception);
            }
        }
    }

    private static ParameterizedCachePair<Float, Float> scaleRecoil(
            ParameterizedCachePair<Float, Float> recoil,
            RecoilMultipliers multipliers
    ) {
        RecoilScaleMath.Parameters pitch = scaleParameters(recoil.left(), multipliers.vertical());
        RecoilScaleMath.Parameters yaw = scaleParameters(recoil.right(), multipliers.horizontal());
        if (!pitch.supported() || !yaw.supported()) {
            if (!warnedUnsupportedTransform) {
                warnedUnsupportedTransform = true;
                LOGGER.warn("A TaCZ recoil script is non-affine; preserving its original recoil instead of replacing it");
            }
            return null;
        }

        Modifier pitchModifier = modifier(pitch);
        Modifier yawModifier = modifier(yaw);
        float pitchDefault = finiteFloat(recoil.left().getDefaultValue(), multipliers.vertical());
        float yawDefault = finiteFloat(recoil.right().getDefaultValue(), multipliers.horizontal());
        return ParameterizedCachePair.of(
                List.of(pitchModifier),
                List.of(yawModifier),
                pitchDefault,
                yawDefault
        );
    }

    private static RecoilScaleMath.Parameters scaleParameters(
            ParameterizedCache<Float> cache,
            double multiplier
    ) {
        return RecoilScaleMath.scaledParameters(
                RecoilScaleMath.samples(cache.eval(0.0D), cache.eval(0.5D), cache.eval(1.0D)),
                multiplier
        );
    }

    private static Modifier modifier(RecoilScaleMath.Parameters parameters) {
        Modifier modifier = new Modifier();
        modifier.setAddend(parameters.addend());
        modifier.setPercent(parameters.percentDelta());
        return modifier;
    }

    private static float finiteFloat(Float value, double multiplier) {
        if (value == null || !Float.isFinite(value) || !Double.isFinite(multiplier)) {
            return 0.0F;
        }
        return (float) (value * multiplier);
    }

    private static void resetFingerprint() {
        lastPlayerId = null;
        lastState = null;
        lastItem = null;
        lastTag = null;
    }
}
