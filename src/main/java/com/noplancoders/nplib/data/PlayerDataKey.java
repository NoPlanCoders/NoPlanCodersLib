package com.noplancoders.nplib.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * プレイヤーに紐づく保存データの種類。Capability を自前で書かずに、NBT保存・クローン時の引き継ぎ・
 * ログイン/リスポーン/次元移動時の同期を nplib に任せられる。
 *
 * <pre>{@code
 * public static final PlayerDataKey<DeckData> DECK = PlayerDataKey.builder(
 *         new ResourceLocation(MOD_ID, "deck"), DeckData::new)
 *     .keepOnDeath()
 *     .syncWith((player, deck) -> NET.sendTo(player, new SyncDeckPacket(deck)))
 *     .register();
 *
 * DeckData deck = DECK.get(player);
 * }</pre>
 *
 * キーはMODのクラス初期化時（static フィールド）に登録すること。
 */
public final class PlayerDataKey<T extends INBTSerializable<CompoundTag>> {

    private static final Map<ResourceLocation, PlayerDataKey<?>> REGISTRY = new ConcurrentHashMap<>();

    private final ResourceLocation id;
    private final Supplier<T> factory;
    private final boolean keepOnDeath;
    @Nullable
    private final BiConsumer<ServerPlayer, T> syncer;

    private PlayerDataKey(ResourceLocation id, Supplier<T> factory, boolean keepOnDeath,
                          @Nullable BiConsumer<ServerPlayer, T> syncer) {
        this.id = id;
        this.factory = factory;
        this.keepOnDeath = keepOnDeath;
        this.syncer = syncer;
    }

    public static <T extends INBTSerializable<CompoundTag>> Builder<T> builder(ResourceLocation id, Supplier<T> factory) {
        return new Builder<>(id, factory);
    }

    /**
     * プレイヤーのデータを取得する（未作成なら factory で作成）。
     * 通常のプレイヤーでは常に存在するが、Capability が無効化された後（削除済みエンティティ等）は例外を投げる。
     */
    public T get(Player player) {
        return getOptional(player).orElseThrow(() ->
                new IllegalStateException("nplib player data is not available for " + player.getName().getString()));
    }

    public Optional<T> getOptional(Player player) {
        return player.getCapability(PlayerDataCapability.CAPABILITY).resolve().map(container -> container.get(this));
    }

    /** syncWith で指定した同期処理を即座に実行する（データ変更後に呼ぶ） */
    public void sync(ServerPlayer player) {
        if (syncer != null) getOptional(player).ifPresent(data -> syncer.accept(player, data));
    }

    public ResourceLocation id() { return id; }

    public boolean keepOnDeath() { return keepOnDeath; }

    T create() { return factory.get(); }

    @Nullable
    static PlayerDataKey<?> byId(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    static Iterable<PlayerDataKey<?>> all() {
        return REGISTRY.values();
    }

    public static final class Builder<T extends INBTSerializable<CompoundTag>> {
        private final ResourceLocation id;
        private final Supplier<T> factory;
        private boolean keepOnDeath = false;
        @Nullable
        private BiConsumer<ServerPlayer, T> syncer;

        private Builder(ResourceLocation id, Supplier<T> factory) {
            this.id = id;
            this.factory = factory;
        }

        /** 死亡・リスポーン時もデータを引き継ぐ（エンド帰還などの死亡以外のクローンでは常に引き継ぐ） */
        public Builder<T> keepOnDeath() {
            this.keepOnDeath = true;
            return this;
        }

        /** ログイン・リスポーン・次元移動時、および {@link PlayerDataKey#sync} 呼び出し時に実行する同期処理 */
        public Builder<T> syncWith(BiConsumer<ServerPlayer, T> syncer) {
            this.syncer = syncer;
            return this;
        }

        public PlayerDataKey<T> register() {
            PlayerDataKey<T> key = new PlayerDataKey<>(id, factory, keepOnDeath, syncer);
            if (REGISTRY.putIfAbsent(id, key) != null) {
                throw new IllegalStateException("Duplicate nplib player data key: " + id);
            }
            return key;
        }
    }
}
