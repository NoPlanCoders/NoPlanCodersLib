package com.noplancoders.nplib.data;

import com.noplancoders.nplib.NPLib;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** {@link PlayerDataContainer} を全プレイヤーにアタッチし、クローン時の引き継ぎと同期を行う */
public final class PlayerDataCapability {

    public static final Capability<PlayerDataContainer> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation CAP_ID = NPLib.id("player_data");

    private PlayerDataCapability() {}

    @Mod.EventBusSubscriber(modid = NPLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PlayerDataContainer.class);
        }
    }

    @Mod.EventBusSubscriber(modid = NPLib.MOD_ID)
    public static final class ForgeEvents {

        @SubscribeEvent
        public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                Provider provider = new Provider();
                event.addCapability(CAP_ID, provider);
                event.addListener(provider::invalidate);
            }
        }

        @SubscribeEvent
        public static void onClone(PlayerEvent.Clone event) {
            Player original = event.getOriginal();
            original.reviveCaps();
            original.getCapability(CAPABILITY).ifPresent(oldData ->
                    event.getEntity().getCapability(CAPABILITY).ifPresent(newData ->
                            newData.copyFrom(oldData, event.isWasDeath())));
            original.invalidateCaps();
        }

        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            syncAll(event.getEntity());
        }

        @SubscribeEvent
        public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            syncAll(event.getEntity());
        }

        @SubscribeEvent
        public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            syncAll(event.getEntity());
        }

        private static void syncAll(Player player) {
            if (player instanceof ServerPlayer sp) {
                for (PlayerDataKey<?> key : PlayerDataKey.all()) key.sync(sp);
            }
        }
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerDataContainer container = new PlayerDataContainer();
        private final LazyOptional<PlayerDataContainer> optional = LazyOptional.of(() -> container);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return container.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            container.deserializeNBT(nbt);
        }

        void invalidate() {
            optional.invalidate();
        }
    }
}
