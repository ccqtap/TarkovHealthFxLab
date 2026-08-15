package com.qq.tarkovhealthfxlab.common.health;

import com.qq.tarkovhealthfxlab.TarkovHealthFxLab;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class HeadDamageRedirectSavedData extends SavedData {
    private static final String DATA_NAME = TarkovHealthFxLab.MODID + "_head_redirect";

    private boolean enabled;
    private DamageApplication last;

    public static HeadDamageRedirectSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                HeadDamageRedirectSavedData::load,
                HeadDamageRedirectSavedData::new,
                DATA_NAME
        );
    }

    public static HeadDamageRedirectSavedData load(CompoundTag tag) {
        HeadDamageRedirectSavedData result = new HeadDamageRedirectSavedData();
        result.enabled = tag.getBoolean("enabled");
        if (tag.contains("last")) {
            CompoundTag last = tag.getCompound("last");
            try {
                result.last = new DamageApplication(
                        BodyPart.valueOf(last.getString("original")),
                        BodyPart.valueOf(last.getString("applied")),
                        last.getFloat("amount"),
                        last.getLong("seed"),
                        last.getBoolean("redirected")
                );
            } catch (IllegalArgumentException ignored) {
                result.last = null;
            }
        }
        return result;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            setDirty();
        }
    }

    public DamageApplication resolveAndRecord(BodyPart original, float amount, long seed) {
        this.last = HeadDamageRedirector.resolve(this.enabled, original, amount, seed);
        setDirty();
        return this.last;
    }

    public DamageApplication last() {
        return this.last;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("enabled", this.enabled);
        if (this.last != null) {
            CompoundTag lastTag = new CompoundTag();
            lastTag.putString("original", this.last.originalPart().name());
            lastTag.putString("applied", this.last.appliedPart().name());
            lastTag.putFloat("amount", this.last.amount());
            lastTag.putLong("seed", this.last.seed());
            lastTag.putBoolean("redirected", this.last.redirected());
            tag.put("last", lastTag);
        }
        return tag;
    }
}
