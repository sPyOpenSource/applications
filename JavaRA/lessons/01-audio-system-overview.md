# 第 1 课：音频系统概览

## 1. 声音从哪来？

JavaRA 是 Red Alert 的 Java 复刻。原始的 Red Alert 声音文件储存在 `.mix` 文件里，这是一种 Westwood 公司自研的打包格式（类似 .zip）。

```
sounds.mix  →  里面包含  →  dog.au, dog2.au, explodes.au, ...
speech.mix  →  里面包含  →  nod1.au, nod2.au, ...
```

## 2. 系统架构 (3 层)

```
Layer 1: SoundManager        ← 游戏逻辑调用（Weapon, Combat, GUI）
Layer 2: Soundly / XSound    ← 音频引擎（缓存、生命周期管理）
Layer 3: JavaFX MediaPlayer  ← 实际播放
```

### Layer 1: SoundManager
文件: `src/cr0s/javara/resources/SoundManager.java`

这是游戏代码唯一直接调用的类。它封装了两个播放层:

```java
// 背景音乐 - 同一时间只能有一首
manager.playMusic("main_theme");   // GUI.java:145 在游戏开始时调用
manager.stopMusic();

// 音效 - 可以叠加，带位置
manager.playSfxAt("explosion", new Pos(x, y));  // Weapon.java:109
```

为什么需要分层？因为不同的播放有不同的优先级和混音规则：
- MUSIC = 0（背景音乐，独占）
- SFX = 1（音效，可叠加）
- UNIT = 2（单位语音）
- SPEECH = 3（语音播报）

### Layer 2: Soundly 
文件: `src/soundly/Soundly.java`

单例，中央音频引擎。负责：

1. **缓存** Media 对象（Media 是不可变的，可以复用）
2. **管理** MediaPlayer 生命周期（创建、播放、释放）
3. **空间音频**：按位置自动计算音量

```java
public MediaPlayer playSound(String filePath, float volume, boolean loop) {
    // 1. 获取/缓存 Media
    Media media = getMedia(filePath);
    
    // 2. 创建新的 MediaPlayer
    MediaPlayer player = new MediaPlayer(media);
    player.setVolume(volume * 100);
    player.setCycleCount(loop ? INDEFINITE : 1);
    
    // 3. 播放完成后自动释放
    player.setOnEndOfMedia(() -> {
        player.dispose();
        activePlayers.remove(filePath);
    });
    
    player.play();
    activePlayers.put(filePath, player);
    return player;
}
```

关键设计决策：每个 `playSound` 调用都创建**新的** MediaPlayer。旧的同路径声音如果还在播，先停止再替换。播放完成后通过 `setOnEndOfMedia` 自动 `dispose()`，避免内存泄露。

### Layer 3: XSound
文件: `src/soundly/XSound.java`

单个 MediaPlayer 的封装。ResouceManager 用它来缓存从 .mix 加载的声音。

```java
XSound sound = new XSound("assets/sfx/explosion.wav");
sound.setVolume(0.8f);
sound.setLooping(false);
sound.play();
```

## 3. 空间音频

当游戏单位在战场上发出声音，声音的音量应该随着距离变化。这就是 `setPosition` 做的事情：

```java
public void setPosition(double x, double y) {
    double distance = Math.sqrt(x * x + y * y);
    double volume = Math.max(0, Math.min(1, 1 - distance / 500));
    player.setVolume(volume * 100);
}
```

公式：`volume = 1 - distance / 500`

- 距离 0 → 音量 100%
- 距离 250 → 音量 50%
- 距离 500 → 音量 0%

SoundManager 在调用 `playSfxAt` 时会自动传递位置：

```java
public void playSfxAt(String name, Pos position) {
    Soundly.get().playSound("assets/sfx/" + name + ".wav", 1.0f, false);
    // 实际还会调用 setSoundPosition 设置空间位置
}
```

## 4. 当前状态

目前 SoundManager 和 Soundly 使用 **直接文件路径** 播放 .wav 文件（需提前从 .mix 提取）。ResourceManager 中有一段从 .mix 加载 → XSound 的代码，但被注释掉了。

课后练习：阅读 `ResourceManager.loadSound()` (第 165-199 行)，看能不能修复它。
