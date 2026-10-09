package com.noplancoders.nplib.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

/**
 * 1人のプレイヤーが持つ全MOD分のデータ。nplib の Capability として1つだけアタッチされる。
 * データは初回アクセス時に生成し、まだ生成していない（または登録元MODが外された）データの NBT は
 * そのまま保持して、次回保存時に失われないようにしている。
 */
public final class PlayerDataContainer implements INBTSerializable<CompoundTag> {

    private final Map<ResourceLocation, INBTSerializable<CompoundTag>> values = new HashMap<>();
    private final Map<ResourceLocation, CompoundTag> pending = new HashMap<>();

    @SuppressWarnings("unchecked")
    <T extends INBTSerializable<CompoundTag>> T get(PlayerDataKey<T> key) {
        INBTSerializable<CompoundTag> value = values.get(key.id());
        if (value == null) {
            T created = key.create();
            CompoundTag saved = pending.remove(key.id());
            if (saved != null) created.deserializeNBT(saved);
            values.put(key.id(), created);
            return created;
        }
        return (T) value;
    }

    /**
     * プレイヤーのクローン時に旧データを移す。
     * 死亡時は keepOnDeath のキーのみ。未知のキー（登録元MODが無い）は安全側に倒して常に引き継ぐ。
     */
    void copyFrom(PlayerDataContainer old, boolean wasDeath) {
        CompoundTag all = old.serializeNBT();
        for (String name : all.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (id == null) continue;
            PlayerDataKey<?> key = PlayerDataKey.byId(id);
            if (wasDeath && key != null && !key.keepOnDeath()) continue;
            values.remove(id);
            pending.put(id, all.getCompound(name));
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        pending.forEach((id, data) -> tag.put(id.toString(), data.copy()));
        values.forEach((id, data) -> tag.put(id.toString(), data.serializeNBT()));
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        values.clear();
        pending.clear();
        for (String name : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (id != null) pending.put(id, tag.getCompound(name));
        }
    }
}
