# Modrinth Android Beta

Modrinth Minecraft 模组平台的 Android 客户端移植项目。

## 项目概述

本项目旨在将 Modrinth 的功能移植到 Android 平台，让用户可以在移动设备上浏览、下载和管理 Minecraft 模组。

## 快速开始

```bash
# 克隆项目
git clone https://github.com/zzy89216-gif/modrinth-Android-beta.git

# 使用 Android Studio 打开项目
# 或使用命令行构建
./gradlew assembleDebug
```

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose
- **架构**: MVVM + 清洁架构
- **网络**: Retrofit + OkHttp
- **依赖注入**: Hilt
- **本地存储**: Room + DataStore

## 目录结构

```
modrinth-Android-beta/
├── app/                  # 主应用模块
├── core/                 # 核心共享模块
├── domain/               # 领域层
├── data/                 # 数据层
└── build.gradle.kts      # 根项目构建配置
```

## 许可

MIT 许可 - 详见 [LICENSE](LICENSE) 文件
