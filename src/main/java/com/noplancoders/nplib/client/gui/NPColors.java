package com.noplancoders.nplib.client.gui;

/**
 * NPC製MOD共通のUIカラー（白ベース＋CMYアクセント）と ARGB 操作ユーティリティ。
 * 色定数はすべて ARGB（0xAARRGGBB）。
 */
public final class NPColors {

    private NPColors() {}

    // ── テキスト ──
    public static final int TEXT_TITLE = 0xFF3A3550;
    public static final int TEXT_LABEL = 0xFF6B6880;
    public static final int TEXT_SUB   = 0xFF8A87A0;

    // ── アクセント（CMY）──
    public static final int CYAN    = 0xFF3AB2EC;
    public static final int MAGENTA = 0xFFF26098;
    public static final int YELLOW  = 0xFFF5C33B;

    // ── 状態 ──
    public static final int POSITIVE = 0xFF33A65B;
    public static final int DISABLED = 0xFF9A97AC;

    // ── 面 ──
    public static final int BACKDROP        = 0x88283050;
    public static final int PANEL_GLASS     = 0xE6F8F9FD;
    public static final int PANEL_BORDER    = 0x66FFFFFF;
    public static final int PANEL_HIGHLIGHT = 0xCCFFFFFF;
    public static final int ROW_BG          = 0xFFF6F8FD;
    public static final int ROW_DISABLED    = 0xFFECEEF5;
    public static final int BAR_BG          = 0xFFE4E7F2;

    public static int alpha(int argb) { return argb >>> 24; }
    public static int red(int argb)   { return (argb >> 16) & 0xFF; }
    public static int green(int argb) { return (argb >> 8) & 0xFF; }
    public static int blue(int argb)  { return argb & 0xFF; }

    public static int argb(int a, int r, int g, int b) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    /** アルファだけ差し替える（alpha は 0〜1） */
    public static int withAlpha(int argb, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255);
        return (argb & 0x00FFFFFF) | a << 24;
    }

    /** 2色を t（0〜1）で線形補間する */
    public static int lerp(int from, int to, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        return argb(
                Math.round(alpha(from) + (alpha(to) - alpha(from)) * k),
                Math.round(red(from) + (red(to) - red(from)) * k),
                Math.round(green(from) + (green(to) - green(from)) * k),
                Math.round(blue(from) + (blue(to) - blue(from)) * k));
    }

    /** RGB に係数を掛ける（1より大きいと明るく、小さいと暗く）。アルファは維持 */
    public static int shade(int argb, float factor) {
        return argb(alpha(argb),
                Math.min(255, Math.round(red(argb) * factor)),
                Math.min(255, Math.round(green(argb) * factor)),
                Math.min(255, Math.round(blue(argb) * factor)));
    }
}
