# 部署指南

## 本地开发部署

### 前置条件

- Android Studio 已安装
- JDK 17+ 已配置
- Android SDK 已下载

### 构建步骤

1. 打开 Android Studio
2. 导入项目文件夹
3. 等待 Gradle 同步完成
4. 连接 Android 设备或启动模拟器
5. 点击 Run 按钮构建并部署

### 命令行构建

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease

# 运行测试
./gradlew test
```

## 发布流程

### 1. 准备发布版本

```bash
# 生成签名密钥（首次需要）
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release-key

# 构建 Release 包
./gradlew assembleRelease
```

### 2. 上传到应用商店

- Google Play Store
- F-Droid
- 或直接分发 APK/AAB 文件

### 3. 版本管理

- 使用 Semantic Versioning (SemVer)
- 每次发布更新 CHANGELOG.md
- 创建 Git Tag

## CI/CD 配置

建议使用 GitHub Actions 进行自动化构建和测试。
