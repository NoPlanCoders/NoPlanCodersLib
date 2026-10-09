package com.noplancoders.nplib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * SimpleChannel の作成・パケット登録・送信をまとめたラッパー。
 *
 * <pre>{@code
 * public static final NPNetwork NET = NPNetwork.create(MyMod.MOD_ID);
 *
 * // FMLCommonSetupEvent などで
 * NET.toServer(SelectCardPacket.class, SelectCardPacket::new)
 *    .toClient(SyncDeckPacket.class, SyncDeckPacket::new);
 *
 * NET.sendTo(player, new SyncDeckPacket(...));
 * }</pre>
 */
public final class NPNetwork {

    private final SimpleChannel channel;
    private int nextId = 0;

    private NPNetwork(SimpleChannel channel) {
        this.channel = channel;
    }

    /** チャンネル名 {@code <modId>:main}、プロトコルバージョン "1" で作成する */
    public static NPNetwork create(String modId) {
        return create(new ResourceLocation(modId, "main"), "1");
    }

    /** プロトコルバージョンが一致しないクライアント/サーバーは接続を拒否される */
    public static NPNetwork create(ResourceLocation channelName, String protocolVersion) {
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                channelName,
                () -> protocolVersion,
                protocolVersion::equals,
                protocolVersion::equals
        );
        return new NPNetwork(channel);
    }

    /** クライアント → サーバー のパケットを登録する（逆方向の受信は破棄される） */
    public <T extends NPPacket> NPNetwork toServer(Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        return register(type, decoder, NetworkDirection.PLAY_TO_SERVER);
    }

    /** サーバー → クライアント のパケットを登録する（逆方向の受信は破棄される） */
    public <T extends NPPacket> NPNetwork toClient(Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        return register(type, decoder, NetworkDirection.PLAY_TO_CLIENT);
    }

    private <T extends NPPacket> NPNetwork register(Class<T> type, Function<FriendlyByteBuf, T> decoder,
                                                    NetworkDirection direction) {
        channel.registerMessage(nextId++, type, NPPacket::encode, decoder,
                (T msg, Supplier<NetworkEvent.Context> ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> msg.handle(ctx));
                    ctx.setPacketHandled(true);
                },
                Optional.of(direction));
        return this;
    }

    public void sendToServer(NPPacket packet) {
        channel.sendToServer(packet);
    }

    public void sendTo(ServerPlayer player, NPPacket packet) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void sendToAll(NPPacket packet) {
        channel.send(PacketDistributor.ALL.noArg(), packet);
    }

    /** エンティティを追跡しているプレイヤー全員に送る（プレイヤー自身にも送る場合は includeSelf=true） */
    public void sendToTracking(Entity entity, NPPacket packet, boolean includeSelf) {
        PacketDistributor.PacketTarget target = includeSelf
                ? PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity)
                : PacketDistributor.TRACKING_ENTITY.with(() -> entity);
        channel.send(target, packet);
    }

    /** 上記で足りない場合の生チャンネル */
    public SimpleChannel channel() {
        return channel;
    }
}
