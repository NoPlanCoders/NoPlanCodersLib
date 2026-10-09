package com.noplancoders.nplib.util;

import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/** サーバー側でプレイヤーを扱う際の小物ユーティリティ */
public final class PlayerUtil {

    private PlayerUtil() {}

    /**
     * 指定した進捗をプレイヤーが達成済みか判定する。
     * advancementId が null の場合は制限なしとして true、未定義の進捗は安全側に倒して false を返す。
     */
    public static boolean hasAdvancement(ServerPlayer player, @Nullable ResourceLocation advancementId) {
        if (advancementId == null) return true;
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) return false;
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /** アクションバー（ホットバー上）にメッセージを表示する */
    public static void actionBar(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }
}
