package com.qq.tarkovhealthfxlab.common.health;

import com.qq.tarkovhealthfxlab.TarkovHealthFxLab;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public final class PlayerInjuryStore {
    public static final String ROOT_KEY = TarkovHealthFxLab.MODID + ".injury_v2";
    static final String STATE_KEY = "state";
    public static final String PROJECTED_MASK_KEY = "projected_effects";
    public static final String OBSERVED_MASK_KEY = "observed_effects";
    public static final String SPECIAL_OBSERVED_MASK_KEY = "observed_special_effects";

    private PlayerInjuryStore() {
    }

    public static InjuryState get(Player player) {
        CompoundTag root = root(player, false);
        return root == null || !root.contains(STATE_KEY, Tag.TAG_COMPOUND)
                ? new InjuryState()
                : InjuryStateCodec.decode(root.getCompound(STATE_KEY));
    }

    public static void put(Player player, InjuryState state) {
        root(player, true).put(STATE_KEY, InjuryStateCodec.encode(state));
    }

    public static int getMarker(Player player, String key) {
        CompoundTag root = root(player, false);
        return root == null || !root.contains(key, Tag.TAG_ANY_NUMERIC) ? 0 : root.getInt(key);
    }

    public static void setMarker(Player player, String key, int marker) {
        root(player, true).putInt(key, marker);
    }

    public static CompoundTag copyRoot(Player player) {
        CompoundTag root = root(player, false);
        return root == null ? new CompoundTag() : root.copy();
    }

    public static void replaceRoot(Player player, CompoundTag root) {
        player.getPersistentData().put(ROOT_KEY, root.copy());
    }

    public static void clear(Player player) {
        player.getPersistentData().remove(ROOT_KEY);
    }

    private static CompoundTag root(Player player, boolean create) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return persistent.getCompound(ROOT_KEY);
    }
}
