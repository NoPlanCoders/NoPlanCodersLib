package com.noplancoders.nplib.client.gui;

import net.minecraft.Util;

/**
 * UIアニメーション用の時間関数とイージング。
 * フレームレートに依存しないよう、すべて実時間（ミリ秒）基準で計算する。
 */
public final class NPAnim {

    private NPAnim() {}

    /** 0〜1 をゆっくり往復する値（ボタンの呼吸アニメーション等）。speed はラジアン/秒 */
    public static float breathe(float speed) {
        return 0.5f + 0.5f * (float) Math.sin(Util.getMillis() / 1000.0 * speed);
    }

    /** 呼吸アニメーション用の拡大率。amplitude=0.03 なら 1.00〜1.03 の間で揺れる */
    public static float pulseScale(float speed, float amplitude) {
        return 1f + amplitude * breathe(speed);
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    public static float easeOutCubic(float t) {
        float k = 1f - clamp01(t);
        return 1f - k * k * k;
    }

    public static float easeInOutCubic(float t) {
        float k = clamp01(t);
        return k < 0.5f ? 4f * k * k * k : 1f - (float) Math.pow(-2f * k + 2f, 3) / 2f;
    }

    /** 少し行き過ぎて戻るイージング（ポップアップ表示向け） */
    public static float easeOutBack(float t) {
        float k = clamp01(t) - 1f;
        float c = 1.70158f;
        return 1f + (c + 1f) * k * k * k + c * k * k;
    }

    public static float clamp01(float t) {
        return Math.max(0f, Math.min(1f, t));
    }

    /**
     * 目標値へ滑らかに追従する値。ホバー拡大・フェードなど、状態が切り替わるたびに
     * 途中から逆再生したいアニメーションに使う。
     *
     * <pre>{@code
     * private final NPAnim.Smooth hover = new NPAnim.Smooth(0.12f);
     * // render 内
     * float t = hover.update(isHovered ? 1f : 0f);
     * NPGui.scaled(g, cx, cy, 1f + 0.08f * t, () -> ...);
     * }</pre>
     */
    public static final class Smooth {
        private final float durationSec;
        private float value;
        private long lastMillis = -1;

        /** @param durationSec 0→1 まで変化するのにかかる秒数 */
        public Smooth(float durationSec) {
            this.durationSec = Math.max(0.001f, durationSec);
        }

        public float update(float target) {
            long now = Util.getMillis();
            if (lastMillis >= 0) {
                float step = (now - lastMillis) / 1000f / durationSec;
                if (value < target) value = Math.min(target, value + step);
                else if (value > target) value = Math.max(target, value - step);
            }
            lastMillis = now;
            return value;
        }

        public float value() { return value; }

        public void snap(float v) {
            value = v;
            lastMillis = -1;
        }
    }
}
