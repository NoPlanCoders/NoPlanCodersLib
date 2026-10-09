package com.noplancoders.nplib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * NPNetwork に登録するパケットの共通インターフェース。
 * デコードは各パケットのコンストラクタ（FriendlyByteBuf を受け取るもの）で行う想定。
 */
public interface NPPacket {

    void encode(FriendlyByteBuf buf);

    /**
     * メインスレッド上で呼ばれる処理。enqueueWork / setPacketHandled は NPNetwork 側で行うため不要。
     * サーバー宛てパケットでは {@code ctx.getSender()} で送信元プレイヤーを取得できる。
     */
    void handle(NetworkEvent.Context ctx);
}
