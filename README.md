# VitualHub

VitualHub 是一个 Android 虚拟定位与路径模拟应用，基于 Kotlin + Jetpack Compose 构建。

## 功能概览

- 虚拟定位注入（Mock Location）
- 地图选点与路径绘制
- 路径插值模拟（线性 / Bezier / Catmull-Rom / B 样条）
- 历史记录与预设路线

## 环境要求

- Android Studio (推荐最新稳定版)
- JDK 17
- Android SDK（以项目 `compileSdk` 配置为准）

## 快速开始

1. 克隆项目
2. 在 `app/src/main/AndroidManifest.xml` 中将
   - `YOUR_AMAP_API_KEY` 替换为你自己的高德地图 Key
3. 使用 Android Studio 打开并同步 Gradle
4. 运行应用到真机或模拟器

## 构建命令

```bash
./gradlew assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

## 免责声明

本项目仅用于学习与测试用途。请遵守当地法律法规及平台政策，不得用于非法用途。

## License

本项目采用 MIT License，详见 `LICENSE`。

