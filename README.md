# 合调 (InTune)

中文 | [English](README.en.md)

简洁的 Android 应用项目示例（包名 `com.zzy.intune`）。本仓库包含可直接构建安装的 APK，并提供发布的示例配置。

## 下载
- 最新版本与历史版本请见 [Releases](https://github.com/brassface/InTune/releases)
- 直接下载 APK：在对应 Release 的 Assets 中获取 `InTune_*.apk`

## 安装
- 在手机上开启“允许安装未知来源应用”
- 将 APK 拷贝到手机后点击安装

## 兼容性
- 最低版本：Android 5.0（API 21）
- 目标版本：API 30（`targetSdkVersion = 30`）

## 环境与工具版本
- JDK：1.8.0_361（Java 8）
- Gradle：6.7.1（使用仓库自带 wrapper）
- Android SDK：`compileSdkVersion = 30`
- `minSdkVersion = 21`
- `targetSdkVersion = 30`

检查本地版本（可选）：
```bash
./gradlew -v      # 查看 Gradle 版本与 JDK
java -version     # 查看 JDK 版本
```

## 本地构建
```bash
# 克隆代码（SSH）
git clone git@github.com:brassface/InTune.git
cd InTune

# 调试构建（未签名）
./gradlew assembleDebug
```

生成产物位置：
- 调试包：`app/build/outputs/apk/debug/app-debug.apk`

 

## 版本与发布
- 版本名在 `app/build.gradle.kts` 的 `defaultConfig.versionName`
- 发布到 GitHub：
  1. `git tag vX.Y.Z && git push origin vX.Y.Z`
  2. 前往 [Releases](https://github.com/brassface/InTune/releases/new) 创建 Release，上传 APK 作为附件

## 贡献
欢迎提交 Issue / PR 改进文档与功能。


## 演示截图

> 以下为应用实机运行截图（位于仓库根目录）。

![PIC1](./PIC1.jpg)

![PIC2](./PIC2.jpg)

![PIC3](./PIC3.jpg)

![PIC4](./PIC4.jpg)

![PIC5](./PIC5.jpg)


