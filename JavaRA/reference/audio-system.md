# JavaRA 音频系统参考

## 架构总览

```
Game code (Weapon, Combat, GUI)
        │
        ▼
SoundManager (singleton)      ← 游戏引擎统一调用入口
        │
        ├── playMusic(name)       → Soundly 播放背景音乐
        ├── stopMusic()           → Soundly 停止当前音乐
        ├── playSfxAt(name, pos)  → Soundly 播放带位置的效果音
        └── isPlaying(name)       → Soundly 查询播放状态
        │
        ▼
Soundly (singleton)           ← 中央音频引擎，缓存 + 生命周期管理
        │
        ├── playSound(path, vol, loop)  → 创建/复用 MediaPlayer
        ├── stopSound(path)             → 停止并释放
        ├── stopAllSounds()             → 紧急停止所有
        ├── setSoundPosition(path, x, y) → 3D 空间音量衰减
        └── isPlaying(path)             → 查询状态
        │
        ▼
JavaFX MediaPlayer            ← 实际播放器
```

## 数据流

### 运行时播放 (当前工作方式)
```
SoundManager.playSfxAt("explosion", position)
  → Soundly.playSound("assets/sfx/explosion.wav", volume, loop=false)
    → JavaFX MediaPlayer(Media("file:assets/sfx/explosion.wav"))
      → player.play()
```

### 完整提取流程 (MIX → AUD → WAV)
```
.mix 文件 (Westwood 游戏包)
  → MixExtractor.parse()         解析 MIX 目录
  → MixExtractor.extract(name)   按文件名提取原始字节
  → AudToWavConverter.convert()  解码 IMA ADPCM → PCM → 写 WAV 头
  → .wav 文件 (可用于 JavaFX 播放)
```

## 核心类详解

### SoundManager (`src/cr0s/javara/resources/SoundManager.java`)
| 方法 | 作用 |
|------|------|
| `getInstance()` | 获取单例 |
| `playMusic(name)` | 背景音乐，同一时间只播一首 |
| `stopMusic()` | 停止背景音乐 |
| `playSfxAt(name, pos)` | 在 `Pos` 位置播放音效，自动计算音量 |
| `isPlaying(name)` | 检查是否正在播放 |

分层设计: MUSIC (0)、SFX (1)、UNIT (2)、SPEECH (3)

### Soundly (`src/soundly/Soundly.java`)
- `playSound(filePath, volume, loop)` → 返回 MediaPlayer
- `stopSound(filePath)` → 停止并 dispose
- `setSoundPosition(filePath, x, y)` → 按距离衰减音量
  - 公式: `volume = max(0, min(1, 1 - distance/500))`
- 自动清理: `setOnEndOfMedia` 在播放完成后释放 MediaPlayer

### XSound (`src/soundly/XSound.java`)
- 单个 MediaPlayer 的封装
- `setPosition(x, y)` 实现空间音频
- `setLooping(boolean)` 控制循环
- 在 ResourceManager 中用于缓存加载的声音

### MixExtractor (`src/soundly/MixExtractor.java`)
- 解析 MIX 文件格式
- 支持 v1/v2 MIX 头
- `extract(name)` → `byte[]` 原始数据

### AudToWavConverter (`src/soundly/AudToWavConverter.java`)
- 解压 Westwood IMA ADPCM 编码
- 写标准 RIFF WAV 文件
- 输出: `byte[]` 完整 WAV 数据，可直接写文件

## 生成的文件

`make_wav.sh` 脚本将提取流程串联:
```
for each .mix:
  mix -> list entries -> extract .aud -> decode -> .wav

输出目录: assets/extracted/
```
