package com.noplancoders.nplib.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

/**
 * NPC製MODで共通して使うGUI描画ヘルパー（白ベース＋すりガラス風のカスタムUI向け）。
 * クライアント専用のため、サーバー側コードから参照しないこと。
 */
@OnlyIn(Dist.CLIENT)
public final class NPGui {

    private NPGui() {}

    /**
     * テクスチャをバインドし、線形（バイリニア）フィルタを適用する。
     * Minecraftは既定でニアレストネイバーを使うため、高解像度で作って縮小表示するパネルの
     * 角丸・影がギザギザになるのを防ぐ。ドット絵アイコンには使わないこと（ぼやけるため）。
     */
    public static void bindSmooth(ResourceLocation texture) {
        RenderSystem.setShaderTexture(0, texture);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    /**
     * テクスチャ全体（texW x texH の実ピクセル）を (x, y, w, h) に縮小描画する。
     * 高解像度テクスチャを使ったパネル・ボタン用。
     */
    public static void blitSmooth(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int texW, int texH) {
        bindSmooth(tex);
        g.blit(tex, x, y, w, h, 0f, 0f, texW, texH, texW, texH);
    }

    /** {@link #blitSmooth} に乗算色（ARGB）を掛けて描画する。ホバー時の明るさ変化などに使う */
    public static void blitSmoothTinted(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h,
                                        int texW, int texH, int argb) {
        RenderSystem.setShaderColor(NPColors.red(argb) / 255f, NPColors.green(argb) / 255f,
                NPColors.blue(argb) / 255f, NPColors.alpha(argb) / 255f);
        RenderSystem.enableBlend();
        blitSmooth(g, tex, x, y, w, h, texW, texH);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** (cx, cy) を中心に scale 倍した状態で draw を実行する（ホバー拡大・呼吸アニメーション用） */
    public static void scaled(GuiGraphics g, float cx, float cy, float scale, Runnable draw) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0);
        pose.scale(scale, scale, 1f);
        pose.translate(-cx, -cy, 0);
        draw.run();
        pose.popPose();
    }

    /** 半透明の背景幕（画面全体） */
    public static void backdrop(GuiGraphics g, int screenW, int screenH) {
        g.fill(0, 0, screenW, screenH, NPColors.BACKDROP);
    }

    /**
     * テクスチャを使わない簡易すりガラスパネル。角を1px欠いた矩形＋上辺ハイライト＋外枠で描く。
     * 本格的な見た目が必要な場合は高解像度テクスチャ＋{@link #blitSmooth}を使う。
     */
    public static void glassPanel(GuiGraphics g, int x, int y, int w, int h) {
        roundedRect(g, x - 1, y - 1, w + 2, h + 2, NPColors.PANEL_BORDER);
        roundedRect(g, x, y, w, h, NPColors.PANEL_GLASS);
        g.fill(x + 2, y + 1, x + w - 2, y + 2, NPColors.PANEL_HIGHLIGHT);
    }

    /** 角を1px欠いた矩形（小さいサイズで角丸っぽく見える） */
    public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x + 1, y, x + w - 1, y + h, argb);
        g.fill(x, y + 1, x + 1, y + h - 1, argb);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    /** 進捗バー。progress は 0〜1 にクランプされる */
    public static void progressBar(GuiGraphics g, int x, int y, int w, int h, float progress, int bgArgb, int fillArgb) {
        roundedRect(g, x, y, w, h, bgArgb);
        int fw = Math.round(w * Math.max(0f, Math.min(1f, progress)));
        if (fw >= 2) roundedRect(g, x, y, fw, h, fillArgb);
    }

    /** 影なしで中央揃えの文字列を描く（白ベースUIでは影があると汚く見えるため） */
    public static void centeredText(GuiGraphics g, Font font, Component text, int cx, int y, int argb) {
        g.drawString(font, text, cx - font.width(text) / 2, y, argb, false);
    }

    public static void centeredText(GuiGraphics g, Font font, String text, int cx, int y, int argb) {
        g.drawString(font, text, cx - font.width(text) / 2, y, argb, false);
    }

    public static boolean isHovered(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
