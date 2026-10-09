package com.noplancoders.nplib;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * NoPlanCoders 製MODの前提ライブラリ。
 * 単体ではゲーム内容を追加せず、依存MODに共通APIを提供する。
 */
@Mod(NPLib.MOD_ID)
public class NPLib {

    public static final String MOD_ID = "nplib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NPLib() {
        // プレイヤーデータCapabilityなどは @Mod.EventBusSubscriber により自動登録される
        LOGGER.info("NoPlanCoders Lib loaded");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
