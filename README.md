# NoPlanCoders Lib (nplib)

**NoPlanCoders（NPC）製 Minecraft MOD の前提MOD（共通ライブラリ）です。**

単体ではゲーム内容を追加しません。NPC製MODで繰り返し書いている処理（パケット登録、プレイヤーデータの保存と同期、すりガラス風UIの描画）を共通APIとして提供します。

## 対応環境

- Minecraft 1.20.1
- Minecraft Forge 47.3.0 以上
- Java 17

## 提供する機能

| パッケージ | クラス | 内容 |
| --- | --- | --- |
| `network` | `NPNetwork`, `NPPacket` | SimpleChannel の作成、方向付きパケット登録、`enqueueWork`／`setPacketHandled` の自動化、送信ヘルパー |
| `data` | `PlayerDataKey` | Capability を書かずにプレイヤーデータを保存。クローン時の引き継ぎ、ログイン・リスポーン・次元移動時の同期も自動 |
| `client.gui` | `NPGui` | バイリニア補間でのテクスチャ描画、拡大描画、すりガラスパネル、進捗バー、中央揃えテキスト |
| `client.gui` | `NPAnim` | 呼吸アニメーション、イージング、ホバー用の補間値 `Smooth` |
| `client.gui` | `NPColors` | 白ベース＋CMYアクセントの共通配色と ARGB 操作 |
| `util` | `PlayerUtil` | 進捗の達成判定、アクションバー表示 |

## 依存MOD側の設定

### 1. ライブラリをローカル Maven に公開

```powershell
.\gradlew.bat publishToMavenLocal
```

### 2. 依存MODの `build.gradle`

```groovy
repositories {
    mavenLocal()
}

dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.3.0'
    implementation fg.deobf('com.noplancoders:nplib:1.0.0')
}
```

### 3. 依存MODの `mods.toml`

```toml
[[dependencies.idolcraft]]
    modId="nplib"
    mandatory=true
    versionRange="[1.0.0,2.0.0)"
    ordering="AFTER"
    side="BOTH"
```

プレイヤーは `nplib-x.y.z.jar` を依存MODと一緒に `mods` フォルダへ入れてください。

## 使い方

### ネットワーク

```java
public final class MyNetwork {
    public static final NPNetwork NET = NPNetwork.create(MyMod.MOD_ID);

    public static void register() { // FMLCommonSetupEvent の enqueueWork 内で呼ぶ
        NET.toServer(ResetDeckPacket.class, ResetDeckPacket::new)
           .toClient(SyncDeckPacket.class, SyncDeckPacket::new);
    }
}

public class ResetDeckPacket implements NPPacket {
    public ResetDeckPacket() {}
    public ResetDeckPacket(FriendlyByteBuf buf) {}

    @Override public void encode(FriendlyByteBuf buf) {}

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;
        // メインスレッドで実行される
    }
}

MyNetwork.NET.sendToServer(new ResetDeckPacket());
MyNetwork.NET.sendTo(serverPlayer, new SyncDeckPacket(deck));
```

### プレイヤーデータ

データクラスは `INBTSerializable<CompoundTag>` を実装します。

```java
public static final PlayerDataKey<DeckData> DECK = PlayerDataKey.builder(
        new ResourceLocation(MOD_ID, "deck"), DeckData::new)
    .keepOnDeath()                                           // 死亡しても消えない
    .syncWith((player, deck) -> NET.sendTo(player, new SyncDeckPacket(deck)))
    .register();

DeckData deck = DECK.get(player);
deck.addCard(...);
DECK.sync(serverPlayer); // 変更をクライアントへ送る
```

- すべてのMODのデータは nplib の1つの Capability（`nplib:player_data`）内に、キーIDごとに保存されます。
- 依存MODを外しても、そのMODのデータはNBTのまま保持されるので、入れ直せば復元されます。
- キーは static フィールドで登録してください（同じIDを2回登録すると例外になります）。

### GUI

```java
private final NPAnim.Smooth hover = new NPAnim.Smooth(0.12f);

@Override
public void render(GuiGraphics g, int mx, int my, float pt) {
    NPGui.backdrop(g, width, height);
    NPGui.blitSmooth(g, PANEL_TEX, x, y, 360, 320, 2160, 1620);

    float t = hover.update(NPGui.isHovered(mx, my, bx, by, bw, bh) ? 1f : 0f);
    float scale = 1f + 0.08f * NPAnim.easeOutCubic(t);
    NPGui.scaled(g, bx + bw / 2f, by + bh / 2f, scale, () ->
            NPGui.blitSmooth(g, BUTTON_TEX, bx, by, bw, bh, 702, 234));

    NPGui.progressBar(g, x + 20, y + 60, 200, 6, progress, NPColors.BAR_BG, NPColors.CYAN);
    NPGui.centeredText(g, font, "プロデューサーランク", width / 2, y + 12, NPColors.TEXT_TITLE);
}
```

`client.gui` パッケージはクライアント専用です。サーバー側のコードから参照しないでください。

## ビルド

```powershell
.\gradlew.bat build
```

成果物は `build/libs/` に出力されます。Gradle 8.8 は Java 25 では動かないため、`JAVA_HOME` を JDK 17 に向けて実行してください。

## ライセンス

MIT
