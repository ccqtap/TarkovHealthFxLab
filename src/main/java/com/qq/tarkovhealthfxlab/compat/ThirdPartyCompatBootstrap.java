package com.qq.tarkovhealthfxlab.compat;

import com.qq.tarkovhealthfxlab.compat.lrtactical.MedicalActionSink;
import com.qq.tarkovhealthfxlab.compat.tacz.ArmRecoilTuning;
import com.qq.tarkovhealthfxlab.compat.tacz.InjuryCompatState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/** Loads optional direct adapters only when both their classes and target mods exist. */
public final class ThirdPartyCompatBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdPartyCompatBootstrap.class);
    private static final String TACZ_ADAPTER = "com.qq.tarkovhealthfxlab.compat.tacz.TaCZClientCompat";
    private static final String LR_ADAPTER = "com.qq.tarkovhealthfxlab.compat.lrtactical.LrTacticalCompat";
    private static volatile Availability lastAvailability = new Availability(false, false);
    private static volatile boolean installAttempted;

    private ThirdPartyCompatBootstrap() {
    }

    public static Availability install(InjuryCompatState injuryState, MedicalActionSink medicalSink) {
        return install(injuryState, ArmRecoilTuning.DEFAULT, medicalSink);
    }

    public static Availability install(
            InjuryCompatState injuryState,
            ArmRecoilTuning recoilTuning,
            MedicalActionSink medicalSink
    ) {
        InjuryCompatState safeState = injuryState == null ? InjuryCompatState.NONE : injuryState;
        ArmRecoilTuning safeTuning = recoilTuning == null ? ArmRecoilTuning.DEFAULT : recoilTuning;
        MedicalActionSink safeSink = medicalSink == null ? MedicalActionSink.IGNORE : medicalSink;

        boolean tacz = FMLEnvironment.dist == Dist.CLIENT
                && supportedInstalledVersion("tacz")
                && invokeInstall(
                TACZ_ADAPTER,
                new Class<?>[]{InjuryCompatState.class, ArmRecoilTuning.class},
                safeState,
                safeTuning
        );
        boolean lrTactical = supportedInstalledVersion("lrtactical")
                && invokeInstall(
                LR_ADAPTER,
                new Class<?>[]{MedicalActionSink.class},
                safeSink
        );
        Availability result = new Availability(tacz, lrTactical);
        lastAvailability = result;
        installAttempted = true;
        return result;
    }

    public static Availability availability() {
        return lastAvailability;
    }

    public static boolean installAttempted() {
        return installAttempted;
    }

    public static boolean supportedInstalledVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> supportedVersion(
                        modId, container.getModInfo().getVersion().toString()))
                .orElse(false);
    }

    public static String installedVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("");
    }

    static boolean supportedVersion(String modId, String version) {
        String clean = Objects.requireNonNullElse(version, "").trim();
        return switch (modId) {
            case "tacz" -> clean.equals("1.1.8-hotfix");
            case "lrtactical" -> clean.equals("0.4.1");
            default -> false;
        };
    }

    private static boolean invokeInstall(String className, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Class<?> adapter = Class.forName(className, true, ThirdPartyCompatBootstrap.class.getClassLoader());
            Method install = adapter.getMethod("install", parameterTypes);
            install.invoke(null, arguments);
            return true;
        } catch (ClassNotFoundException exception) {
            LOGGER.info("Optional compatibility adapter {} is not packaged; continuing without it", className);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            LOGGER.warn("Optional compatibility adapter {} rejected initialization; continuing without it", className, cause);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            LOGGER.warn("Optional compatibility adapter {} is incompatible; continuing without it", className, exception);
        }
        return false;
    }

    public record Availability(boolean tacz, boolean lrTactical) {
    }
}
