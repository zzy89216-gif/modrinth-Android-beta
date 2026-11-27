# 部署与构建指南

> 本项目**构建全部走云端**（GitHub Actions），本地不安装 Android SDK。

## 一、CI 工作流

| 工作流 | 触发 | 作用 | 产物 |
|--------|------|------|------|
| `build.yml` | push 到 `main` | 构建 Debug APK | artifact `modrinth-android-debug`（保留 14 天） |
| `pr-check.yml` | Pull Request 到 `main` | 只跑 `compileDebugKotlin`，快速发现编译错误 | 无 |
| `release.yml` | push tag `v*` | 构建 Release APK + 生成 sha256 + 创建 GitHub Release（pre-release） | Release 附件 |

文件位置：`.github/workflows/`

## 二、取 APK

1. 打开 https://github.com/zzy89216-gif/modrinth-Android-beta/actions
2. 选最新一次成功的 `Build APK`
3. 页面底部 Artifacts → 下载 `modrinth-android-debug.zip`
4. 解压得到 `app-debug.apk`，传手机安装（需允许未知来源）

### 发正式 beta（打 tag）

```bash
git tag v0.1.0
git push origin v0.1.0
```
触发 `release.yml`：
- 构建 `assembleRelease`（当前为未签名 APK，beta 阶段先用 debug 安装验证）
- 产出 `modrinth-android-v0.1.0-release.apk` + `.sha256`
- 自动创建 Release（标记 pre-release）

> 正式发布前需配置签名：在仓库 Secrets 放 keystore（base64）与密码，并在 `app/build.gradle.kts` 增加 `signingConfigs`。

## 三、本地构建（一般不需要）

```bash
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```
要求：JDK 17、Android SDK（platforms;android-34、build-tools;34.0.0）。

## 四、排错：直接读 CI 日志（推荐）

网页看日志慢，用 API 更快：

```bash
TOKEN=<你的 GitHub Token>
REPO=zzy89216-gif/modrinth-Android-beta

# 1. 取最近一次运行 id 与状态
curl -s -H "Authorization: token $TOKEN" \
  "https://api.github.com/repos/$REPO/actions/runs?per_page=3" \
  | python3 -c "import json,sys;[print(r['id'],r['name'],r['status'],r['conclusion']) for r in json.load(sys.stdin)['workflow_runs']]"

# 2. 下载日志压缩包
RUN_ID=<上一步的 id>
curl -sL -H "Authorization: token $TOKEN" \
  "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/logs" -o /tmp/logs.zip

# 3. 解压并只看错误段
cd /tmp && unzip -o logs.zip >/dev/null && sed -n '/What went wrong/,/Try:/p' build/*.txt
```

## 五、常见构建错误速查

| 报错 | 原因 | 处理 |
|------|------|------|
| `repository 'Google' was added by build file` | 根 `allprojects.repositories` 与 settings 的 `FAIL_ON_PROJECT_REPOS` 冲突 | 删根项目 repositories 块 |
| `Unresolved reference: kapt` | 模块用了 `kapt()` 但没应用 kapt 插件 | plugins 加 `id("org.jetbrains.kotlin.kapt")` |
| `Unresolved reference: getDefaultProguardFile` | Groovy 风格调用，Kotlin 需括号 | `proguardFiles(getDefaultProguardFile(...), "...")` |
| `Script compilation error` | build.gradle.kts 语法/插件缺失 | 按行号修 |
| Compose 编译失败 + Kotlin 2.0 | 仍用 `composeOptions.kotlinCompilerExtensionVersion` | 改用 `org.jetbrains.kotlin.plugin.compose` |
| Setup Android 步骤失败 | `android-actions/setup-android@v3` 在 runner 上异常 | 直接删掉该步骤 |

## 六、检查本机环境（可选）

```bash
java -version        # 需要 17
./gradlew --version  # 确认 Gradle 8.7
```
