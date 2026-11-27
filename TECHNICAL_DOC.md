# 技术文档

## 一、模块与依赖关系

```
app (com.android.application)
 ├── core    (android.library)
 ├── domain  (kotlin.jvm 纯 JVM，无 Android 依赖)
 └── data    (android.library)
      ├── domain
      └── core
```

依赖方向：`app → data → domain`，`domain` 不反向依赖任何层。

| 模块 | 职责 | 关键类 |
|------|------|--------|
| `app` | Compose UI、Navigation、ViewModel、应用级 DI | `MainActivity`、`AppNavGraph`、`SearchViewModel`、`AppUseCaseModule` |
| `data` | Retrofit 接口、DTO、Repository 实现、Hilt 模块 | `ModrinthApiImpl`、`ProjectResponse`、`ProjectRepositoryImpl`、`DataModule` |
| `domain` | 领域模型、仓库接口、用例 | `Project`、`ProjectRepository`、`GetProjectUseCase`、`SearchProjectsUseCase` |
| `core` | 预留通用组件 | — |

## 二、架构分层

```
UI (Compose Screen)
  ↓ 收集 StateFlow
ViewModel (HiltViewModel + viewModelScope)
  ↓ 调用
UseCase (domain)
  ↓ 调用接口
ProjectRepository (domain 接口)
  ↑ 实现
ProjectRepositoryImpl (data)  →  ModrinthApiImpl (Retrofit)
```

- ViewModel 只依赖 UseCase，不直接接触 Retrofit
- `domain` 保持纯 Kotlin/JVM，便于单测

## 三、依赖注入（Hilt）

| 模块 | 提供 |
|------|------|
| `DataModule`（data） | `ModrinthApiImpl`（Retrofit 单例）、`ProjectRepository` ← `ProjectRepositoryImpl`（`@Binds`） |
| `AppUseCaseModule`（app） | `GetProjectUseCase`、`SearchProjectsUseCase` |

注意：
- `DataModule` 必须是 `abstract class` + `companion object` 放 `@Provides`（object 不能有 abstract 方法）
- 模块必须标 `@InstallIn(SingletonComponent::class)`
- 使用 kapt 的模块**必须应用** `org.jetbrains.kotlin.kapt` 插件（app、data 均已应用）
- `MainActivity` 需 `@AndroidEntryPoint`

## 四、版本矩阵（必须保持一致）

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | CI 用 temurin 17 |
| Gradle | 8.7 | wrapper 固定 |
| AGP | 8.6.0 | 根 `build.gradle.kts` |
| Kotlin | 2.0.0 | android / jvm / kapt 同版本 |
| Compose 编译器 | `org.jetbrains.kotlin.plugin.compose` 2.0.0 | Kotlin 2.0 起必须用插件，不能用 `composeOptions` |
| Compose BOM | 2024.09.00 | |
| Hilt | 2.52 | |
| Retrofit | 2.11.0 | |
| OkHttp | 4.12.0 | |
| minSdk / target / compile | 26 / 34 / 34 | |

## 五、网络层

基础地址：`https://api.modrinth.com/`

```kotlin
// data/di/DataModule.kt
Retrofit.Builder()
    .baseUrl("https://api.modrinth.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(ModrinthApiImpl::class.java)
```

已定义的接口（`ModrinthApiImpl`）：

| 方法 | 路径 | 返回（当前代码假设） |
|------|------|----------------------|
| `getProject` | `GET v1/project/{project_id}` | `ProjectResponse` |
| `searchProjects` | `GET v1/project/search` | `List<ProjectResponse>` ⚠️ 实际是 `{hits:[]}` |
| `getCategories` | `GET v1/category` | `List<String>` ⚠️ 实际是对象/对象数组 |

> ⚠️ 这两处解析与真实响应不符，运行时会导致解析失败（Repository 内已 catch 返回空）。见 HANDOFF。

## 六、数据流示例：搜索

```
用户输入 → SearchScreen 点击搜索
  → SearchViewModel.search(query)
    → SearchProjectsUseCase.execute(query)
      → ProjectRepositoryImpl.searchProjects()
        → ModrinthApiImpl.searchProjects()  (Retrofit)
          → mapToProject() 转换 DTO → 领域模型
    → 更新 _projects / _isLoading / _error (StateFlow)
  → Compose 重组渲染列表 / 加载态 / 错误提示
```

## 七、CI 与构建

详见 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)。
构建全部在 GitHub Actions 云端完成，本地不安装 Android SDK。

## 八、已知技术债

1. 搜索结果与分类响应模型未对齐真实 API
2. 无本地缓存层（Room 未接入）
3. 无图片加载（Coil 未接入）
4. 无分页与下拉刷新
5. `core` 模块为空
6. 无单元测试
