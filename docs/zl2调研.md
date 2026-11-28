# ZalithLauncher2（zl2）调研：键位映射 与 渲染器

> 调研目的：评估把 zl2（Minecraft Java 启动器，GPL-3.0，Jetpack Compose）的能力移植进 ModBox（纯 Compose 应用，无游戏进程）的可行性与范围。
> 源仓库：`/root/workspace/upstream/ZalithLauncher2`（只读）。
> 本次实际移植范围见文末「已移植清单」。

---

## 0. 结论速览

- **渲染器（renderer）不是「UI 里的渲染引擎」，而是一组 native GL 翻译/注入方案**：zl2 用 PojavLauncher 的 JNI 内核在 Android 上跑真 MC（JVM + LWJGL/GLFW），渲染器负责把游戏请求的桌面 OpenGL 翻译到手机可用后端（ES3/Vulkan/软渲染）。**这一整套依赖 native 内核 + 游戏 JVM 进程，ModBox 当前没有宿主，整体不搬**（符合需求）。
- 但 zl2 渲染器里有**可复用的纯 Kotlin「配置层 + 选择 UI + 线程/生命周期模型」**：`RendererInterface` 抽象、`Renderers` 注册表、`RendererConfigDsl`/`RendererConfigStore`（稀疏 JSON 配置 + 版本级覆盖 + 默认值解析）、`RendererSettingsScreen` 选择/配置 UI。这些是 ModBox 未来「接游戏进程」时可直接套用的骨架。
- **键位映射（InputMap）核心是纯 Kotlin/Java 的键码映射 + Compose UI**，可移植；本次已把「键位配置 + 持久化 + 重新绑定对话框（含组合键/长按捕获）」移植为独立可用的 `keymap` 包。

---

## 1. zl2 渲染器到底在做什么

### 1.1 架构位置

zl2 的渲染链路：

```
UI(Compose) ──选择/配置──▶ RendererSettings / RendererConfigStore(JSON)
                                    │
GameLaunchFlow ──loadRenderer()──▶ Renderers.setCurrentRenderer(id)
                                    │  解析配置 → LoadedRenderer
GameLauncher.initRendererConfig() ─▶ prepareRendererLaunchConfig()
                                    │  得到 {环境变量, dlopen库列表, 主渲染库, EGL}
VMActivity(游戏宿主 Activity)
   └─ TextureView ──onSurfaceTextureAvailable──▶ ZLBridge.setupBridgeWindow(Surface)
                                                    │ (JNI → egl_bridge.c)
                                                    └─ EGL 建窗口/swapchain
                                                       + dlopen 渲染库(libgl4es / zink / osmesa ...)
                                                       + 注入 LD_LIBRARY_PATH
                                                       + 启游戏 JVM（JRE 内嵌 jre-17/21/25）
   游戏画面: GL → (渲染器翻译) → EGL Surface → TextureView
```

关键点：
- **游戏跑在内嵌 JRE 里**（assets 带 jre-8/17/21/25），走真实 LWJGL 3.3.3/3.4.1 的 GLFW，而不是 Android 的 SurfaceView 直接画。
- **`LWJGL` 模块**（`LWJGL/3.3.3`、`LWJGL/3.4.1`、`LWJGL/patches`）是把 LWJGL 各子模块 jar **合并/打补丁**成能在 Android 上加载的 fat jar 的 Gradle 工程（`java` 插件，产出 jar，不是 Android 库）。它只是「把 LWJGL 装进游戏 JVM」的载体，不是渲染器本身。
- **真正「翻译渲染」的是各 renderer 声明的 `.so`**，由 `RendererInterface` 暴露。

### 1.2 `RendererInterface`（`game/renderer/RendererInterface.kt`）

每个渲染器是一个 `object`，声明：

| 字段 | 作用 |
|---|---|
| `getRendererId()` / `getUniqueIdentifier()` | 稳定 id / 全局唯一 UUID（配置命名空间用） |
| `getRendererName()` / `getRendererSummary()` | 展示名 / 说明 |
| `getMinMCVersion()` / `getMaxMCVersion()` | 兼容的 MC 版本区间（可随配置调整） |
| `getRendererEnv(): Lazy<Map>` | 注入游戏 JVM 的**环境变量**（如 `LIBGL_GL`、`LIBGL_ES`、驱动路径） |
| `getDlopenLibrary(): Lazy<List>` | 启动前要 dlopen 的辅助库 |
| `getRendererLibrary()` | **主渲染 `.so`**（如 `libgl4es_114.so`、`libng_gl4es.so`） |
| `getRendererEGL()` | 指定 EGL 实现 |
| `mesaBased` / `needsZinkDriverOverride` / `supportsSfpewByDefault()` | Mesa/zink/SFPEW 兼容层开关 |

内置渲染器（`renderers/`）及其实质：

| 渲染器 | 库 | 本质 |
|---|---|---|
| `NGGL4ESRenderer`（"Krypton Wrapper"） | `libng_gl4es.so` | OpenGL ES 3 翻译桌面 GL，最通用 |
| `GL4ESRenderer` | `libgl4es_114.so` | 经典 GL4ES（桌面 GL→ES2） |
| `KopperZinkRenderer` | zink(Mesa) | 桌面 GL→Vulkan（Turnip 驱动） |
| `MobileGluesRenderer` | MobileGlues | 移动端 GL 胶水 |
| `VirGLRenderer` | VirGL | GPU 间 GL 重定向 |
| `FreedrenoRenderer` / `PanfrostRenderer` | 各自 Mesa 驱动 | Adreno /  Mali 专用后端 |

`ConfigurableRenderer : RendererInterface` 额外带 `configSpec`（声明式配置规范，见下）。

### 1.3 配置模型（`game/renderer/config/`）——**这是最值得复用的部分**

- `rendererConfigSpec { ... }` DSL：`switch/choice/int/float/directory...` 声明每个渲染器的可配项，每项有 `id/title/description/default` 与 `ConfigTarget`（落到**环境变量**还是**json 字段**）。
- `RendererConfigStore`（稀疏存储接口）+ `GlobalRendererConfigStore` / `VersionRendererConfigStore`：
  - 只存「用户显式改过的值」，默认值不入库；
  - **全局配置 + 版本级覆盖** 两层，`resolveRendererConfig()` 按优先级（版本覆盖 > 全局 > 规范默认）解析；
  - 带**旧键迁移**（`migrateLegacy`）与**清理失效键**（`removeStaleEntries`），用 MMKV/JSON 持久化；
  - 配置变更通过 `RendererConfigChanges.revision`（`mutableIntStateOf`）驱动 UI 刷新——**线程模型 = Compose 的 `mutableXStateOf` + `@WorkerThread` 读写**，没有额外线程。
- `LoadedRenderer` = 渲染器 + 已解析配置的绑定，版本兼容边界可依赖配置结果。

### 1.4 渲染器选择/配置 UI（`ui/screens/content/settings/RendererSettingsScreen.kt`）

- 用 `FlowRow`/`Card`/自定义 `TitleAndSummary`/`IntInputField`/`SimpleAlertDialog` 渲染；
- 从 `Renderers.getRenderers()` 拿列表 → 选中项写入 `RendererSettings.renderer`（stringSetting）；
- 选中 `ConfigurableRenderer` 时，按其 `configSpec` 动态生成配置项行，读写 `Global/VersionRendererConfigStore`；
- 依赖：`Plugin`/`DriverPluginManager`（渲染器**插件**可从 GitHub/云加载）、`MMKV`、字符串资源。

### 1.5 宿主 Activity 的接入（`ui/activities/VMActivity.kt`）

- 游戏画面是一个 **`TextureView`**（`AndroidView` 嵌入 Compose）；
- `onSurfaceTextureAvailable` → `ZLBridge.setupBridgeWindow(nativeSurface)`（JNI，把 ANativeWindow 交给 `egl_bridge.c` 建 EGL 上下文/swapchain，并 dlopen 渲染库、注入环境变量、拉起游戏 JVM）；
- `onSurfaceTextureDestroyed` → `ZLBridge.releaseBridgeWindow()`；
- 输入回灌：虚拟按键/鼠标产生的 GLFW 键码经 `ZLBridge.sendInputData(...)` → C 侧 `input_bridge_v3.c` → 游戏。

---

## 2. 键位映射（InputMap）是怎么实现的

### 2.1 `InputMap` 模块（`com.movtery.inputmap`）

纯 Kotlin/Java，**无 native**，无 Compose。内容：

| 文件 | 作用 |
|---|---|
| `AWTInputEvent.java` | **Oracle 版权**的 AWT 键码常量副本（GPL-2-only + Classpath 例外），仅给 Java 侧用 |
| `keycodes/LwjglGlfwKeycode.java` | GLFW 键码常量（zlib 许可） |
| `keycodes/Lwjgl2Keycode.java` | LWJGL2 键码常量 |
| `keycodes/ControlEventKeycode.java` | 「字符串名 → GLFW 键码」映射（字符串表用） |
| `keycodes/ControlEventKeyName.java` | 「GLFW 键码 → 显示名」映射 |
| `keycodes/MinecraftKeyBindingMapper.java` | 「MC 键位资源名（`key.keyboard.w`）→ GLFW 键码」 |
| `keycodes/_MinecraftKeyBindingMapper.kt` | **MC 默认绑定常量**（WASD/快捷栏/聊天/疾跑…） |

> 结论：InputMap 模块本身是「键码字典」，真正的「键位 UI」在主 app。

### 2.2 主 app 的键位 UI

- `ui/control/Keyboard.kt`：**虚拟键盘** Compose 对话框，按 GLFW 键码网格排布，`isTapMode` 单击选键；
- `control_editor/edit_widget/KeyEventEdit.kt`：控件编辑器里的「按键事件」编辑（列出已绑定键、点 + 加键、删除），键名走 `ControlEventKeyName`；
- `control_editor/edit_widget/LauncherEventsEdit.kt`：启动器内事件绑定；
- 持久化：`setting/ControlSettings.kt`（`SettingsRegistry`：`physicalKeyImeCode` 等）+ `LayerController` 的 `ControlLayout`（控件布局 + 每控件的 `ClickEvent` 列表，Gson 序列化到文件）。

### 2.3 与 ModBox 的对应关系（本次移植策略）

| zl2 | ModBox 移植结果 |
|---|---|
| `LwjglGlfwKeycode` | `keymap/GlKeycodes.kt`（Int 常量，含 MOD_* 位） |
| `ControlEventKeyName` | `keymap/GlKeyNames.kt` |
| `_MinecraftKeyBindingMapper.kt`（默认绑定） | `keymap/KeymapConfig.kt` 的 `DefaultKeymap` |
| 「键位 = 动作 + 键码」概念 | `keymap/KeyBinding` |
| `ControlSettings`/布局持久化 | `keymap/KeymapRepository`（`files/keymap.json`） |
| `Keyboard` + `KeyEventEdit` | `keymap/KeymapScreen` + `KeymapBindDialog`（Compose，组合键/长按捕获） |
| （无） | `keymap/KeymapViewModel`（Hilt，最小解耦） |
| （无） | `keymap/AndroidKeyMapper`（物理键盘 `KeyEvent`→GLFW 键码 + MOD 位） |

> 注：zl2 的 `EfficientAndroidLWJGLKeycode`（`game/input/`）是「Android `KeyEvent.keyCode` → LWJGL 键码」的权威映射（本身修改自 PojavLauncher）。本次 `AndroidKeyMapper` 参照它重建了常用键的映射表。

---

## 3. 可复用 vs 不可复用（渲染器）

### ✅ 可复用（纯 Kotlin / Compose，无 native，无 MC 进程绑定）

1. **`RendererInterface` + `ConfigurableRenderer` 抽象**：定义「一个渲染后端 = id + 名称 + 版本区间 + 环境变量 + 库清单 + 可配规范」，与具体实现解耦。ModBox 将来接任何后端（哪怕只是「软渲染截图预览」）都能套用。
2. **`Renderers` 注册表**：单例注册/查询/切换当前渲染器，带 id 冲突检测。
3. **配置层 `config/*`**：`rendererConfigSpec` DSL、稀疏 `RendererConfigStore`、`resolveRendererConfig` 优先级解析、旧键迁移、`RendererConfigChanges`（`mutableIntStateOf` 驱动刷新）。**这是线程模型的最佳参考：读写在 `@WorkerThread`，状态用 Compose `State` 回主线程。**
4. **`RendererSettingsScreen` 的 UI 形态**：渲染器列表卡片 + 选中 + 按 `configSpec` 动态生成配置行。UI 结构可照搬，去掉 `Plugin`/`MMKV`/字符串资源即可。
5. **线程/生命周期模型**：`GameLaunchFlow`（suspend 协程串联 检查→加载→确认框）、`LoadedRenderer` 绑定、`VMActivity` 的 `SurfaceTextureListener` 生命周期（available/changed/destroyed 对应 setup/release）。

### ❌ 不可复用（native / 与 MC 进程绑定）

1. **`LWJGL` 模块**：把 LWJGL jar 合并/打补丁塞进游戏 JVM 的 Gradle 工程，无宿主无意义。
2. **`bridge/`（ZLBridge/ZLNativeInvoker/NativeLibraryLoader）+ `jni/`**：EGL 建窗、dlopen 渲染库、注入 `LD_LIBRARY_PATH`、拉起游戏 JVM、AWT/输入桥、退出钩子——全部 native。
3. **各 `*.so` 渲染库**（gl4es/zink/osmesa/virgl…）与 Mesa 驱动、Turnip Vulkan 驱动。
4. **`VMActivity` 中把游戏画面接到 `TextureView` + `setupBridgeWindow` 的那段**：依赖 native。
5. **内嵌 JRE（jre-8/17/21/25）** 与真实 MC 启动流程。
6. **`LayerController` 的运行时控件**（把手势映射成游戏 GLFW 输入）：虽然它是纯 Kotlin Compose，但**运行时强依赖**把事件灌进游戏进程（`EventHandler`/`PointerEventBus`），没有游戏进程无法生效。**本次不搬运行时**；其**数据模型**（`ClickEvent`/`ControlLayout`/`ButtonStyle`）是纯数据类，将来做「控件布局编辑器」时可参考，但本次范围外。

---

## 4. 移植路线图

### Phase A（本次已完成）：键位 UI 可用
- [x] `keymap` 包：`GlKeycodes`/`GlKeyNames`/`KeymapConfig`(+`DefaultKeymap`)/`AndroidKeyMapper`/`KeymapRepository`/`KeymapViewModel`/`KeymapScreen`(含 `KeymapBindDialog`)。
- [x] 键捕获：Compose `onKeyEvent`（物理键盘，支持 Ctrl/Shift/Alt/Win **组合键**）+ 修饰键芯片 + **长按动作**标记 + 虚拟按键（无键盘设备兜底）。
- [x] 持久化：`context.filesDir/keymap.json`，原子写（临时文件+rename）。
- 遗留：未接入导航（`NavGraph`/`Screen` 属禁改文件），调用方需自行 `composable { KeymapScreen(onBack=…) }`。

### Phase B（未来接游戏进程时）：渲染器怎么接
1. **先只搬「配置骨架」**（不动 native）：把 `RendererInterface`/`Renderers`/`RendererConfigDsl`/`RendererConfigStore` 移植为 `renderer` 包，`RendererSettingsScreen` 做成独立 Composable。此时 ModBox 仍无游戏，仅能**选/存**渲染器配置（为将来做数据准备）。
2. **接入宿主**：新增一个游戏宿主 Activity/Fragment，内嵌 `TextureView`，复刻 `VMActivity` 的 `SurfaceTextureListener` 生命周期（available→setup、destroyed→release）。
3. **引入 native**：按需引入 PojavLauncher/zl2 的 `jni`（EGL 桥、dlopen 钩子）与所选渲染器的 `.so`。**这一步必须带宿主进程**，且涉及 GPL native 代码，需单独评审。
4. **渲染器插件**（zl2 支持从 GitHub/云加载渲染器插件）：作为最后可选能力，依赖 `game/plugin` 体系，暂不建议。

> 决策建议：Phase B 的 1、2 是纯 Kotlin，可在 ModBox 有「要跑游戏」的明确需求前**不预支**；3、4 属于重 native 工程，应与「移植 PojavLauncher 内核」合并成一个独立立项。

---

## 5. 已移植清单（文件对文件）

### 键位（新包 `app/src/main/java/com/modrinth/android/keymap/`）
| 目标文件 | 来源（zl2） | 说明 |
|---|---|---|
| `GlKeycodes.kt` | `InputMap/.../LwjglGlfwKeycode.java` | GLFW 键码 + MOD_* 位（zlib） |
| `GlKeyNames.kt` | `InputMap/.../ControlEventKeyName.java` | 键码→显示名 |
| `KeymapConfig.kt` | `InputMap/.../_MinecraftKeyBindingMapper.kt` + 键位概念 | 数据模型 + MC 默认键位 |
| `AndroidKeyMapper.kt` | `ZalithLauncher/.../game/input/EfficientAndroidLWJGLKeycode.java` | Android KeyEvent→GLFW（重建映射表） |
| `KeymapRepository.kt` | （新写，参照 `ControlSettings`/布局持久化） | `keymap.json` 持久化 |
| `KeymapViewModel.kt` | （新写，参照 `SearchViewModel` 的 Hilt 写法） | 加载/改绑/重置 |
| `KeymapScreen.kt` | `ui/control/Keyboard.kt` + `control_editor/edit_widget/KeyEventEdit.kt` | 列表 + 重新绑定对话框（组合键/长按） |

### 通用组件（`app/src/main/java/com/modrinth/android/ui/components/`）
| 目标目录 | 来源 | 文件数 |
|---|---|---|
| `cardgrid/` | `CardGrid` 模块（engine/model/state/ui） | 11 |
| `colorpicker/` | `ColorPicker` 模块 | 9 |

> 每个文件头加 `// Ported from ZalithLauncher2 (GPL-3.0), see NOTICE`；包名 `com.movtery.*` → `com.modrinth.android.*`；未逐字保留 GPL 文件头（按要求换成 NOTICE 指向）。

### 文档
- `docs/zl2调研.md`（本文）
- `NOTICE`（仓库根）：登记移植的 GPL 文件清单，指向源仓库 `LICENSE`。

### 未移植（按规则排除）
- `LWJGL`/`bridge`/`jni`/内嵌 JRE（native，无宿主）。
- `LayerController` 运行时控件（强依赖游戏进程）。
- `Guide`/`Terracotta`/`PetLib`（与键位/渲染器无关）。

---

## 6. 遗留问题 / 风险

1. **Gson 依赖**：需求说 Repository 用 Gson，但 app 模块当前未声明 Gson（data 模块的 `converter-gson` 是 `implementation`，不外泄）。为不新增未审批依赖，`KeymapRepository` 改用 Android 自带 `org.json`。若坚持 Gson，需在 `app/build.gradle.kts` 加 `com.google.code.gson:gson`（本次禁改该文件，故未加）。
2. **Compose BOM 版本差**：zl2 用 `compose-bom 2026.09.00`（Material **Expressive**），ModBox 用 `2024.09.00`。已移植的 `CardGrid` 原实现从 `MaterialTheme.motionScheme`（Expressive 专属）取动效参数，已改为 `CardGridState` 内置弹簧默认值（见 `ui/CardGrid.kt` 注释）。升级 BOM 后可恢复。
3. **未接导航**：`KeymapScreen` 未挂进 `NavGraph`（禁改），调用方自行 `composable`。
4. **未编译验证**：本机无 Android SDK，无法跑 `gradlew assembleDebug`；已逐一核对 import/版本 API（`FilterChip`/`Surface(onClick)`/`keypad` 等均为 BOM 2024.09.00 稳定 API），但**需在 Android Studio 实际编译一次**确认。
5. **GLFW 键码覆盖**：`AndroidKeyMapper` 重建了常用键（字母/数字/符号/功能/方向/修饰/F1-12/小键盘）。特殊键（`KEYCODE_HEADSETHOOK`、方向之外的 dpad、部分 IME 键）未映射，返回 null；若将来接手柄，需补全 `LwjglGlfwKeycode`/`ControlEventKeycode` 的全量表。
6. **CardGrid 依赖 `androidx.activity.compose.BackHandler`**：app 已依赖 `activity-compose`，无问题；但 `CardGrid` 还用了 `LocalHapticFeedback`、`PointerEventTimeoutCancellationException`（均为 ui 稳定 API）。
