# 合调 (InTune)

中文 | [English](README.en.md)

简洁的 Android 应用项目示例（包名 `com.zzy.intune`）。本仓库包含可直接构建安装的 APK，并提供发布与签名的示例配置。

## 下载
- 最新版本与历史版本请见 [Releases](https://github.com/brassface/InTune/releases)
- 直接下载 APK：在对应 Release 的 Assets 中获取 `InTune_*.apk`

## 安装
- 在手机上开启“允许安装未知来源应用”
- 将 APK 拷贝到手机后点击安装

## 兼容性
- 最低版本：Android 5.0（API 21）
- 目标版本：API 30（`targetSdkVersion = 30`）

## 本地构建
```bash
# 克隆代码（SSH）
git clone git@github.com:brassface/InTune.git
cd InTune

# 调试构建（未签名）
./gradlew assembleDebug

# 发布构建（签名，需先配置签名信息，见下）
./gradlew assembleRelease
```

生成产物位置：
- 调试包：`app/build/outputs/apk/debug/app-debug.apk`
- 发布包：`app/build/outputs/apk/release/app-release.apk`

## 签名配置（发布构建）
发布构建依赖 Gradle 签名配置。推荐将敏感信息写入“用户级” `gradle.properties`，避免提交到仓库。
（Windows 常见路径：`C:\Users\<你>\.gradle\gradle.properties`；如设置了 `GRADLE_USER_HOME`，也会读取该目录下的 `gradle.properties`。）

1) 准备 keystore（若尚无）：
```bash
# 示例（注意替换路径、别名与密码）
keytool -genkey -v -keystore /path/to/your.keystore -alias release \
  -keyalg RSA -keysize 2048 -validity 36500
```

2) 在本机 `~/.gradle/gradle.properties`（Windows: `C:\Users\<你>\.gradle\gradle.properties`）中加入：
```properties
RELEASE_STORE_FILE=/absolute/path/to/your.keystore
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=release
RELEASE_KEY_PASSWORD=your_key_password
```

3) 本项目的 `app/build.gradle.kts` 中已引用上述属性：
```kotlin
android {
    signingConfigs {
        create("release") {
            val store = project.findProperty("RELEASE_STORE_FILE") as String?
            if (store != null) {
                storeFile = file(store)
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
            }
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}
```

### 签名配置图示
按下列顺序查看截图（位于仓库根目录）：

![签名步骤1](./1.jpg)

![签名步骤2](./2.jpg)

![签名步骤3](./3.jpg)

![签名步骤4](./4.jpg)

![签名步骤5](./5.jpg)

完成后执行：
```bash
./gradlew clean assembleRelease
```

## 版本与发布
- 版本名在 `app/build.gradle.kts` 的 `defaultConfig.versionName`
- 发布到 GitHub：
  1. `git tag vX.Y.Z && git push origin vX.Y.Z`
  2. 前往 [Releases](https://github.com/brassface/InTune/releases/new) 创建 Release，上传 APK 作为附件

## 贡献
欢迎提交 Issue / PR 改进文档与功能。


