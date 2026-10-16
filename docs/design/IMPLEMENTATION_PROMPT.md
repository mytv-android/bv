# BV Expressive 重设计 · 实现提示词

> 把本文件**全文**交给执行模型。它是一份施工图，不是建议。执行模型必须逐条照做，遇到本文没写到的情况，按「第 15 节 遇到问题时」处理，不要自行发挥设计。

---

## 0. 你的任务

你要在 Android 项目 `C:\PythonProject\bv`（哔哩哔哩第三方客户端 BV，Kotlin + Jetpack Compose）里，把现有的「手机版 `app/mobile`」和「电视版 `app/tv`」两套界面，重写为**一套自适应界面**，视觉和交互必须与设计稿**一模一样**。

设计稿是一个 HTML 页面：`docs/design/bv-expressive-redesign.html`（用 Chrome 打开）。它的源码在 `docs/design/_src/`：

| 源文件 | 内容 | 你怎么用 |
|---|---|---|
| `_src/5a-mocks.js` | 首页、刷刷、视频详情、播放器 的手机/平板/电视原型 | **每个界面的精确尺寸来源** |
| `_src/5b-mocks.js` | 搜索、影视、动态、我的 的原型 | 同上 |
| `_src/5c-mocks.js` | 设置、登录 的原型，以及每个界面的适配规则 `SCREENS[].rules` | 同上 + 行为规则 |
| `_src/2-mock.css` | 原型里所有 class 的样式（`.m-vc` 卡片、`.m-btn` 按钮、`.f.fx` 焦点等） | 组件的精确样式 |
| `_src/1-head.html` | 页面色彩令牌 | 只作参考，App 里颜色由 material-kolor 生成 |
| `_src/4-data.js` | 示例数据、形状、弹簧参数 | 示例数据**不要**写进 App |

**读原型源码的换算规则（必须遵守）：**
- 原型里所有 `px` 数值 = Compose 里的 `dp`（例：`width:196px` → `196.dp`）。
- 原型里所有 `font-size: Npx` = `N.sp`；`font: 800 34px/1.08` = `fontSize 34.sp, fontWeight W800, lineHeight 34*1.08 = 37.sp`。
- 原型里 `var(--primary)`、`var(--surface-container-high)` 等 = `MaterialTheme.colorScheme.primary`、`surfaceContainerHigh`（把短横线命名换成驼峰）。
- 原型画布尺寸：手机 `360×780dp`，手机横屏播放器 `780×360dp`，平板 `1200×800dp`，电视 `960×540dp`。
- `class="cinema"` 的区域 = 强制深色主题。`data-seed-cover` 的区域 = 用该视频封面颜色做局部主题（`CoverTheme`）。
- `class="f"` + `data-f` = 该元素在电视上可获得焦点；`data-start` = 进入页面时的初始焦点；`.fx` = 获得焦点时的样式。

---

## 1. 硬性规则（违反任何一条都算失败）

1. **禁止**在新代码里 import 任何 `androidx.tv.*`。最终要从所有 `build.gradle.kts` 删掉 `tv-material` 和 `tv-foundation`。
2. **禁止**升级任何现有依赖版本（Kotlin 2.2.0、Compose、material3 `1.5.0-alpha03`、AGP、media3、Coil 2.7 等全部保持不变）。设计文档里写的「升级 material3 到 1.5.0-beta01 / 引入 navigation3」**作废**，以本文件为准。原因：新版本会牵连 Kotlin 和 Compose 大版本升级。
3. 唯一新增的第三方依赖是 `com.materialkolor:material-kolor:3.0.1`。**不要用 4.x 或 5.x**：5.x 需要 Kotlin 2.4，4.1 需要 Kotlin 2.3，项目是 Kotlin 2.2.0，会编译失败。3.0.1 已在本项目实测可编译。
4. 导航使用项目已有的 `androidx.navigation:navigation-compose:2.9.2`，用 `@Serializable` 类型安全路由。
5. 不修改 `bili-api` 的已有接口签名；只允许**新增**（例如 `StoryRepository`、稍后再看的添加/删除、点赞投币的仓库封装）。
6. 不修改 `app/shared` 里已有 ViewModel 的公开行为；可以新增 ViewModel 和新增方法。
7. 所有界面文字使用字符串资源，沿用项目现有做法（默认 `values/strings.xml` + 现有的中文 values 目录；以 `app/mobile/src/main/res` 的目录结构为准）。本文给出的中文文案就是中文资源的值，一字不改。
8. 每完成第 13 节的一个步骤，必须编译通过（第 3 节命令）再进入下一步。编译不过不许继续。
9. 不要「顺手」重构与本任务无关的代码。不要删除旧代码，直到第 13 节步骤 9。
10. 原型里以下内容**只是演示**，不要做进 App：设备外框、原型里的假状态栏「9:41 / 5G」（App 用系统状态栏）、电视原型右下角的灰色提示「方向键移动焦点 · 确认键打开」之类的 `m-tvhint`、刷刷横屏示例下方的说明文字「横屏视频：居中 + 封面氛围光，横过来自动全屏」、所有示例标题和数字。

---

## 2. 设计稿要点速览（先读完再动手）

- 一个启动入口 `MainActivity`，同时声明 `LAUNCHER` 和 `LEANBACK_LAUNCHER`。
- 五个一级入口（顺序固定）：**首页 · 刷刷 · 影视 · 动态 · 我的**。另有「搜索」「设置」两个固定入口。
- 布局由 `DeviceProfile` 决定（第 6.4 节）：
  - 手机布局：宽度 < 840dp 且非电视。底部导航栏。
  - 平板布局：宽度 ≥ 840dp 且非电视。左侧导航轨（≥1200dp 展开为 220dp 带文字）。
  - 电视布局：电视设备（任意宽度）。左侧 76dp 图标导航轨，获得焦点时展开为 196dp。
- 颜色：全局主题由种子色（默认 B 站粉 `#FB7299`）用 material-kolor 生成，默认风格 `PaletteStyle.Vibrant`；视频详情、播放器、刷刷、电视影视页用**封面颜色**局部覆盖（`CoverTheme`）。
- 电视、播放器、刷刷固定深色（`cinema`）。
- 形状、弹簧动画只在按下 / 聚焦 / 选中 / 加载时出现。

---

## 3. 构建环境（本机特有，照抄）

在 Git Bash 里执行：

```bash
export JAVA_HOME="C:/Users/ldm/jdk-dl/jdk21"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="C:/Users/ldm/AppData/Local/Android/Sdk"
cd /c/PythonProject/bv
./gradlew --no-daemon -I "C:/Users/ldm/jdk-dl/mirror-init.gradle" :app:assembleDefaultDebug
```

- 只编译界面模块时用：`:app:ui:compileDebugKotlin`（`app:ui` 是库模块，没有 flavor）。
- 不要用 Android Studio 自带的 JBR（JDK 25 会让 Kotlin 编译器崩溃）。
- Maven Central 必须走 `-I mirror-init.gradle`，否则下载失败。
- 如果报 `No matching variant of project :libs:ffmpegDecoder`，执行 `git submodule update --init --recursive`。
- APK 输出在 `app/build/outputs/apk/default/debug/`。

---

## 4. 已验证的依赖与 API

以下 API 已在本项目（Kotlin 2.2.0 + material3 1.5.0-alpha03 + material-kolor 3.0.1）里实际编译通过，**直接用，不要换成别的写法**：

```kotlin
// 文件头统一加：
@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

// material-kolor
import com.materialkolor.DynamicMaterialExpressiveTheme   // (seedColor, motionScheme, isDark, isAmoled, style, contrastLevel, specVersion, shapes, typography, animate, content)
import com.materialkolor.PaletteStyle                     // TonalSpot, Neutral, Vibrant, Expressive, Rainbow, FruitSalad, Monochrome, Fidelity, Content
import com.materialkolor.dynamiccolor.ColorSpec           // ColorSpec.SpecVersion.SPEC_2025
import com.materialkolor.ktx.themeColors                  // ImageBitmap.themeColors(fallback = Color.Unspecified): List<Color>
import com.materialkolor.ktx.harmonize                    // Color.harmonize(other: Color): Color

// material3 Expressive（包名 androidx.compose.material3）
MotionScheme.expressive(); MotionScheme.standard()
MaterialTheme.motionScheme.fastSpatialSpec<Float>(); .defaultSpatialSpec<Dp>(); .slowSpatialSpec<Float>()
MaterialTheme.motionScheme.fastEffectsSpec<Float>(); .defaultEffectsSpec<Float>(); .slowEffectsSpec<Color>()
ShortNavigationBar { ShortNavigationBarItem(selected, onClick, icon = {}, label = {}) }
WideNavigationRail(state = rememberWideNavigationRailState(), header = {}) { WideNavigationRailItem(selected, onClick, icon = {}, label = {}, railExpanded = false) }
LoadingIndicator(); ContainedLoadingIndicator()
LinearWavyProgressIndicator(progress = { 0.5f })
MaterialShapes.Cookie9Sided / Cookie4Sided / Sunny / Clover4Leaf / SoftBurst / Pentagon / Pill / Heart / Flower / Gem / Circle
MaterialShapes.Cookie9Sided.toShape()                     // RoundedPolygon -> Shape
ToggleButton(checked, onCheckedChange, shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()) { }
ButtonGroupDefaults.connectedMiddleButtonShapes(); ButtonGroupDefaults.connectedTrailingButtonShapes()
HorizontalFloatingToolbar(expanded = true) { }; VerticalFloatingToolbar(expanded = true) { }
androidx.compose.material3.pulltorefresh.PullToRefreshBox(isRefreshing, onRefresh) { }   // 注意包名
androidx.compose.material3.adaptive.currentWindowAdaptiveInfo()

// foundation / ui
androidx.compose.foundation.gestures.LocalBringIntoViewSpec
androidx.compose.foundation.gestures.BringIntoViewSpec
androidx.compose.ui.focus.focusRestorer                    // Modifier.focusRestorer()
androidx.compose.ui.focus.focusProperties                  // Modifier.focusProperties { onEnter = { }; onExit = { } }
androidx.compose.ui.platform.LocalInputModeManager         // .current.inputMode == InputMode.Keyboard
```

`connectedMiddleButtonShapes` / `connectedTrailingButtonShapes` 与 Leading 是同一组 API，若编译报找不到，改用 `ToggleButtonDefaults.shapes()` 并按第 6.3 节手动给圆角。

---

## 5. 模块结构

### 5.1 新建 `app:ui` 模块

1. `settings.gradle.kts` 在 `include(":app:tv")` 下一行加 `include(":app:ui")`。
2. 新建 `app/ui/build.gradle.kts`，内容**完全复制** `app/mobile/build.gradle.kts`，只改两处：
   - `namespace = AppConfiguration.appId + ".appui"`
   - `dependencies` 改为：
     ```kotlin
     dependencies {
         implementation(project(":app:shared"))
         implementation(project(":player:core"))
         implementation(project(":player:shared"))
         implementation(androidx.media3.common)
         implementation(androidx.media3.exoplayer)
         implementation(libs.logging)
         debugImplementation(androidx.compose.ui.tooling)
     }
     ```
3. 新建 `app/ui/src/main/AndroidManifest.xml`（先只写 `<manifest />`，第 8.1 节再填）、`app/ui/consumer-rules.pro`、`app/ui/proguard-rules.pro`（空文件）。
4. `app/build.gradle.kts` 的 `dependencies` 加 `implementation(project(":app:ui"))`（暂时保留 mobile 和 tv）。
5. `gradle/libs.versions.toml`：
   - `[versions]` 加 `materialKolor = "3.0.1"`
   - `[libraries]` 加 `material-kolor = { module = "com.materialkolor:material-kolor", version.ref = "materialKolor" }`
6. `app/shared/build.gradle.kts` 的 `dependencies` 在 `api(androidx.compose.material3)` 下一行加 `api(libs.material.kolor)`（`ThemePrefs` 在 shared 里要用 `PaletteStyle`）。

### 5.2 `app:ui` 包结构（照建）

```
app/ui/src/main/kotlin/dev/aaa1115910/bv/ui/
  MainActivity.kt
  navigation/   Routes.kt  BvNavHost.kt  AppShell.kt  TopLevel.kt
  designsystem/
    theme/      BvTheme.kt  CoverTheme.kt  BvType.kt  BvDimens.kt
    adaptive/   DeviceProfile.kt
    focus/      BvFocus.kt  PivotBringIntoViewSpec.kt  BvKey.kt
    component/  VideoCard.kt  PosterCard.kt  SectionHeader.kt  BvTabRow.kt  BvChip.kt
                ActionButtonGroup.kt  SegmentedRow.kt  BvAvatar.kt  CinemaScrim.kt
                Ambient.kt  TvBackdrop.kt  BvSearchField.kt  CommentItem.kt  WavyProgress.kt
                BvListItem.kt  ColorTile.kt  RankBadge.kt
    nav/        PhoneNavBar.kt  TabletRail.kt  TvRail.kt
  feature/
    home/ shorts/ video/ player/ search/ pgc/ dynamic/ me/ settings/ login/
    space/ tag/ favorite/ history/ toview/ following/ accounts/ timeline/ pgcindex/ tools/
```

每个 `feature/xxx/` 里放 `XxxScreen.kt`（入口，按 `DeviceProfile.layout` 分派）以及 `XxxPhone.kt`、`XxxTablet.kt`、`XxxTv.kt` 三个布局文件。三个布局共享同一个 ViewModel 实例。

---

## 6. 设计系统（逐字照抄代码，数值不许改）

### 6.1 主题偏好（放在 `app/shared`）

新建 `app/shared/src/main/kotlin/dev/aaa1115910/bv/entity/ThemePrefs.kt`：

```kotlin
package dev.aaa1115910.bv.entity

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

enum class ThemeSource { Wallpaper, Custom, Content }      // 系统壁纸 / 自选种子色 / 跟随内容
enum class DarkMode { System, Light, Dark, PureBlack }     // 跟随系统 / 浅色 / 深色 / 影院纯黑

object BvSeeds {
    val Pink = Color(0xFFFB7299)       // B 站粉（默认）
    val Sky = Color(0xFF23ADE5)        // 天蓝
    val Amber = Color(0xFFF2A53A)      // 琥珀
    val Teal = Color(0xFF1FA88C)       // 青绿
    val Violet = Color(0xFF8C6FF7)     // 紫罗兰
    val Vermilion = Color(0xFFE5484D)  // 朱红
    val presets = listOf(Pink, Sky, Amber, Teal, Violet, Vermilion)
}

data class ThemePrefs(
    val source: ThemeSource = ThemeSource.Custom,
    val seed: Color = BvSeeds.Pink,
    val contentSeed: Color? = null,                 // 「跟随内容」时使用：最近一次播放视频的封面色
    val style: PaletteStyle = PaletteStyle.Vibrant,
    val contrast: Double = 0.0,                     // 0.0 标准 / 0.5 中 / 1.0 高
    val darkMode: DarkMode = DarkMode.System,
    val coverTheme: Boolean = true,                 // 详情页使用封面颜色
    val reduceMotion: Boolean = false,              // 减少动效
)
```

在 `util/Prefs.kt` 的 `PrefKeys` 里新增（照现有 `prefThemeTypeKey` / `prefThemeTypeRequest` 的写法）：

```kotlin
val prefThemeSourceKey = intPreferencesKey("bv_theme_source")
val prefSeedColorKey = intPreferencesKey("bv_seed_color")
val prefContentSeedKey = intPreferencesKey("bv_content_seed")          // 0 表示没有
val prefPaletteStyleKey = intPreferencesKey("bv_palette_style")
val prefContrastKey = floatPreferencesKey("bv_contrast")
val prefDarkModeKey = intPreferencesKey("bv_dark_mode")
val prefCoverThemeKey = booleanPreferencesKey("bv_cover_theme")
val prefReduceMotionKey = booleanPreferencesKey("bv_reduce_motion")

val prefThemeSourceRequest = PreferenceRequest(prefThemeSourceKey, ThemeSource.Custom.ordinal)
val prefSeedColorRequest = PreferenceRequest(prefSeedColorKey, 0xFFFB7299.toInt())
val prefContentSeedRequest = PreferenceRequest(prefContentSeedKey, 0)
val prefPaletteStyleRequest = PreferenceRequest(prefPaletteStyleKey, PaletteStyle.Vibrant.ordinal)
val prefContrastRequest = PreferenceRequest(prefContrastKey, 0f)
val prefDarkModeRequest = PreferenceRequest(prefDarkModeKey, DarkMode.System.ordinal)
val prefCoverThemeRequest = PreferenceRequest(prefCoverThemeKey, true)
val prefReduceMotionRequest = PreferenceRequest(prefReduceMotionKey, false)
```

在 `object Prefs` 里新增：

```kotlin
val themePrefsFlow: Flow<ThemePrefs>
    get() = combine(
        dsm.getPreferenceFlow(PrefKeys.prefThemeSourceRequest),
        dsm.getPreferenceFlow(PrefKeys.prefSeedColorRequest),
        dsm.getPreferenceFlow(PrefKeys.prefPaletteStyleRequest),
        dsm.getPreferenceFlow(PrefKeys.prefContrastRequest),
        dsm.getPreferenceFlow(PrefKeys.prefDarkModeRequest),
    ) { source, seed, style, contrast, dark ->
        ThemePrefs(
            source = ThemeSource.entries[source],
            seed = Color(seed),
            style = PaletteStyle.entries[style],
            contrast = contrast.toDouble(),
            darkMode = DarkMode.entries[dark],
        )
    }
        .combine(dsm.getPreferenceFlow(PrefKeys.prefContentSeedRequest)) { p, c -> p.copy(contentSeed = if (c == 0) null else Color(c)) }
        .combine(dsm.getPreferenceFlow(PrefKeys.prefCoverThemeRequest)) { p, v -> p.copy(coverTheme = v) }
        .combine(dsm.getPreferenceFlow(PrefKeys.prefReduceMotionRequest)) { p, v -> p.copy(reduceMotion = v) }

suspend fun setThemeSource(v: ThemeSource) = dsm.editPreference(PrefKeys.prefThemeSourceKey, v.ordinal)
suspend fun setSeedColor(v: Color) = dsm.editPreference(PrefKeys.prefSeedColorKey, v.toArgb())
suspend fun setContentSeed(v: Color) = dsm.editPreference(PrefKeys.prefContentSeedKey, v.toArgb())
suspend fun setPaletteStyle(v: PaletteStyle) = dsm.editPreference(PrefKeys.prefPaletteStyleKey, v.ordinal)
suspend fun setContrast(v: Float) = dsm.editPreference(PrefKeys.prefContrastKey, v)
suspend fun setDarkMode(v: DarkMode) = dsm.editPreference(PrefKeys.prefDarkModeKey, v.ordinal)
suspend fun setCoverTheme(v: Boolean) = dsm.editPreference(PrefKeys.prefCoverThemeKey, v)
suspend fun setReduceMotion(v: Boolean) = dsm.editPreference(PrefKeys.prefReduceMotionKey, v)
```

如果 `dsm.editPreference` 不是 suspend 函数，就去掉 `suspend`，保持与文件里已有写法一致。

### 6.2 设备画像 `designsystem/adaptive/DeviceProfile.kt`

```kotlin
package dev.aaa1115910.bv.ui.designsystem.adaptive

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager

enum class WidthClass { Compact, Medium, Expanded, Large }
enum class BvInputMode { Touch, DPad }
enum class Distance { Near, Far }
enum class NavKind { Bar, Rail, RailExpanded, TvRail }
enum class DeviceLayout { Phone, Tablet, Tv }

@Immutable
data class DeviceProfile(
    val width: WidthClass,
    val input: BvInputMode,
    val distance: Distance,
) {
    val isTv: Boolean get() = distance == Distance.Far
    val navKind: NavKind
        get() = when {
            isTv -> NavKind.TvRail
            width >= WidthClass.Large -> NavKind.RailExpanded
            width >= WidthClass.Medium -> NavKind.Rail
            else -> NavKind.Bar
        }
    /** 界面布局：Compact/Medium 用手机布局，Expanded 及以上用平板布局，电视用电视布局 */
    val layout: DeviceLayout
        get() = when {
            isTv -> DeviceLayout.Tv
            width >= WidthClass.Expanded -> DeviceLayout.Tablet
            else -> DeviceLayout.Phone
        }
}

val LocalDeviceProfile = staticCompositionLocalOf {
    DeviceProfile(WidthClass.Compact, BvInputMode.Touch, Distance.Near)
}

fun Context.isTelevision(): Boolean {
    val ui = getSystemService(UiModeManager::class.java)
    return ui?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

@Composable
fun rememberDeviceProfile(): DeviceProfile {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val tv = remember(context) { context.isTelevision() }
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val width = when {
        widthDp < 600 -> WidthClass.Compact
        widthDp < 840 -> WidthClass.Medium
        widthDp < 1200 -> WidthClass.Expanded
        else -> WidthClass.Large
    }
    val input = if (tv || keyboard) BvInputMode.DPad else BvInputMode.Touch
    return remember(width, input, tv) {
        DeviceProfile(width, input, if (tv) Distance.Far else Distance.Near)
    }
}
```

说明：平板原型画布是 1200×800dp，但它画的是**收起**的导航轨（Expanded 布局）。验收时平板模拟器用 1196×800dp（见第 14 节），正好落在 Expanded。

### 6.3 尺寸、字号

`designsystem/theme/BvType.kt`（所有界面文字**只能**用这里的样式，颜色另外指定）：

```kotlin
package dev.aaa1115910.bv.ui.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object BvType {
    val Display = TextStyle(fontSize = 34.sp, lineHeight = 37.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.01).em) // 电视主标题
    val HeroTablet = TextStyle(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold)    // 平板横幅标题
    val DetailTv = TextStyle(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)      // 电视详情标题
    val PageTitle = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)     // 手机/电视页标题「影视」「动态」「设置」
    val PageTitleLarge = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold) // 平板页标题
    val PaneTitle = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)     // 右侧面板标题「历史记录」「外观与主题」
    val TitleLarge = TextStyle(fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold)    // 平板详情标题、电视刷刷标题
    val PanelTitle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold)    // 电视设置右侧标题、播放器标题、我的页昵称
    val HeroPhone = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)     // 手机精选标题、弹幕设置标题
    val Title = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)              // 手机详情标题
    val Section = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)            // 区块标题「继续观看」
    val Body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val BodyBold = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val CardTitle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)        // 卡片标题
    val Small = TextStyle(fontSize = 13.sp, lineHeight = 20.sp)
    val Label = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val Meta = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)                                              // 卡片副信息
    val Eyebrow = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em) // 「今日精选 · 纪录」
    val Badge = TextStyle(fontSize = 10.5.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold)        // 时长、评分角标
    val Button = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val ButtonLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
}
```

字体：全部用系统默认字体，不要打包额外字体文件。

`designsystem/theme/BvDimens.kt`：

```kotlin
package dev.aaa1115910.bv.ui.designsystem.theme

import androidx.compose.ui.unit.dp

object BvDimens {
    val PhoneGutter = 16.dp
    val TabletContentStart = 8.dp
    val TabletContentEnd = 28.dp
    val TabletContentTop = 20.dp
    val TabletSectionGap = 18.dp
    val TvSafeHorizontal = 48.dp
    val TvSafeVertical = 27.dp
    val TvRailWidth = 76.dp
    val TvRailExpandedWidth = 196.dp
    val TvContentStart = 96.dp        // 带导航轨的电视页面，内容左边距
    val CardCorner = 16.dp
    val CardCornerFocused = 22.dp
    val HeroCorner = 28.dp
    val PaneCorner = 28.dp
    val TileCorner = 24.dp
}
```

### 6.4 主题 `designsystem/theme/BvTheme.kt`

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.aaa1115910.bv.ui.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.dynamiccolor.ColorSpec
import dev.aaa1115910.bv.entity.*
import dev.aaa1115910.bv.ui.designsystem.adaptive.*
import dev.aaa1115910.bv.util.Prefs

val LocalThemePrefs = staticCompositionLocalOf { ThemePrefs() }
val LocalBvDark = staticCompositionLocalOf { false }

/** 应用根：提供设备画像、主题偏好、电视密度，然后套全局主题。只在 MainActivity.setContent 里调用一次。 */
@Composable
fun BvRoot(initialPrefs: ThemePrefs, content: @Composable () -> Unit) {
    val prefs by Prefs.themePrefsFlow.collectAsState(initial = initialPrefs)
    val profile = rememberDeviceProfile()
    val context = LocalContext.current
    val base = LocalDensity.current
    val tvDensity by Prefs.densityFlow.collectAsState(initial = context.resources.displayMetrics.widthPixels / 960f)
    val density = if (profile.isTv) Density(tvDensity, base.fontScale) else base
    CompositionLocalProvider(
        LocalDeviceProfile provides profile,
        LocalThemePrefs provides prefs,
        LocalDensity provides density,
    ) {
        BvTheme(content = content)
    }
}

/** 全局主题。forceDark = true 用于播放器、刷刷（原型里的 .cinema）。电视永远深色。 */
@Composable
fun BvTheme(forceDark: Boolean = false, content: @Composable () -> Unit) {
    val prefs = LocalThemePrefs.current
    val profile = LocalDeviceProfile.current
    val systemDark = isSystemInDarkTheme()
    val dark = forceDark || profile.isTv || when (prefs.darkMode) {
        DarkMode.System -> systemDark
        DarkMode.Light -> false
        DarkMode.Dark, DarkMode.PureBlack -> true
    }
    val context = LocalContext.current
    val seed = when {
        prefs.source == ThemeSource.Wallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            remember(context) { dynamicLightColorScheme(context).primary }
        prefs.source == ThemeSource.Content && prefs.contentSeed != null -> prefs.contentSeed
        else -> prefs.seed
    }
    CompositionLocalProvider(LocalBvDark provides dark) {
        DynamicMaterialExpressiveTheme(
            seedColor = seed,
            motionScheme = if (prefs.reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
            isDark = dark,
            isAmoled = dark && prefs.darkMode == DarkMode.PureBlack,
            style = prefs.style,
            contrastLevel = prefs.contrast,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            animate = true,
            content = content,
        )
    }
}
```

### 6.5 封面局部主题 `designsystem/theme/CoverTheme.kt`

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.aaa1115910.bv.ui.designsystem.theme

import android.content.Context
import android.util.LruCache
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.themeColors
import dev.aaa1115910.bv.entity.DarkMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CoverSeedCache {
    private val cache = LruCache<String, Int>(256)

    fun peek(url: String): Color? = cache.get(url)?.let { Color(it) }

    /** 取 112px 小图，后台线程量化取色，按 URL 缓存。失败返回 null。 */
    suspend fun get(context: Context, url: String): Color? {
        peek(url)?.let { return it }
        val request = ImageRequest.Builder(context).data(url).size(112).allowHardware(false).build()
        val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable ?: return null
        val color = withContext(Dispatchers.Default) {
            drawable.toBitmap().asImageBitmap().themeColors(fallback = Color.Unspecified).firstOrNull()
        } ?: return null
        if (!color.isSpecified) return null
        cache.put(url, color.toArgb())
        return color
    }
}

@Composable
fun rememberCoverSeed(url: String?): State<Color?> {
    val context = LocalContext.current
    return produceState(initialValue = url?.let { CoverSeedCache.peek(it) }, url) {
        value = url?.let { CoverSeedCache.get(context, it) }
    }
}

/**
 * 用封面颜色局部覆盖主题（原型里的 data-seed-cover）。
 * 结构永远不变：封面色没取到时用当前 primary 作种子，取到后颜色自动动画过渡。
 */
@Composable
fun CoverTheme(coverUrl: String?, forceDark: Boolean = false, content: @Composable () -> Unit) {
    val prefs = LocalThemePrefs.current
    val loaded by rememberCoverSeed(if (prefs.coverTheme) coverUrl else null)
    val dark = forceDark || LocalBvDark.current
    val seed = loaded ?: MaterialTheme.colorScheme.primary
    CompositionLocalProvider(LocalBvDark provides dark) {
        DynamicMaterialExpressiveTheme(
            seedColor = seed,
            motionScheme = MaterialTheme.motionScheme,
            isDark = dark,
            isAmoled = dark && prefs.darkMode == DarkMode.PureBlack,
            style = prefs.style,
            contrastLevel = prefs.contrast,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            shapes = MaterialTheme.shapes,
            typography = MaterialTheme.typography,
            animate = true,
            content = content,
        )
    }
}
```

**`CoverTheme` 必须用在：** 视频详情页整页、番剧详情页整页、播放器（`forceDark = true`）、刷刷的每一页（`forceDark = true`）、电视影视页（种子取当前精选番剧封面，`forceDark = true`）。电视首页**不用**。

**跟随内容：** `ThemeSource.Content` 时，每次开始播放一个视频，取该视频封面色调用 `Prefs.setContentSeed(color)`。

### 6.6 焦点系统 `designsystem/focus/BvFocus.kt`

规格（来自 `_src/2-mock.css` 里的 `.f.fx` 规则）：

| 元素 | 聚焦放大 | 描边 | 光晕 | 其他 |
|---|---|---|---|---|
| 视频卡 / 海报卡（只给**封面区域**描边） | 1.08 | 3dp primary，画在封面外侧 | primary 阴影 14dp | 封面圆角 16dp → 22dp（defaultSpatial 弹簧） |
| 按钮、图标按钮、标签、列表项、键盘键、电视导航轨项 | 1.10 | 3dp primary 外侧 | primary 阴影 14dp | 背景变 `inverseSurface`、内容色变 `inverseOnSurface`；filled primary 按钮保持 primary |
| 「我的」页 4 个彩色入口卡、电视刷刷的视频画面 | 1.05 | 3dp primary 外侧 | primary 阴影 16dp | — |

只有 `LocalDeviceProfile.current.input == BvInputMode.DPad` 时显示焦点效果；触摸时没有放大、没有描边。

```kotlin
package dev.aaa1115910.bv.ui.designsystem.focus

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.aaa1115910.bv.ui.designsystem.adaptive.BvInputMode
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import kotlin.math.min

/** 外侧 3dp 描边 + 光晕，带淡入淡出动画。 */
fun Modifier.focusRing(visible: Boolean, corner: Dp, glow: Dp = 14.dp): Modifier = composed {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, MaterialTheme.motionScheme.fastEffectsSpec(), label = "focusRing")
    val color = MaterialTheme.colorScheme.primary
    this
        .shadow(elevation = glow * alpha, shape = RoundedCornerShape(corner), clip = false, ambientColor = color, spotColor = color)
        .drawWithContent {
            drawContent()
            if (alpha > 0f) {
                val w = 3.dp.toPx()
                val r = min(corner.toPx() + w / 2, (min(size.width, size.height) + w) / 2)
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(-w / 2, -w / 2),
                    size = Size(size.width + w, size.height + w),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(w),
                )
            }
        }
}

/**
 * 通用焦点效果。给 M3 自带焦点的组件（Button、IconButton、Surface(onClick)…）用时 addFocusable = false；
 * 给普通 Box 用时 addFocusable = true。onFocused 用于「聚焦即选中」。
 */
fun Modifier.bvFocus(
    scale: Float = 1.1f,
    corner: Dp = 999.dp,
    glow: Dp = 14.dp,
    addFocusable: Boolean = false,
    onFocused: ((Boolean) -> Unit)? = null,
): Modifier = composed {
    val profile = LocalDeviceProfile.current
    var focused by remember { mutableStateOf(false) }
    val show = focused && profile.input == BvInputMode.DPad
    val s by animateFloatAsState(if (show) scale else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "bvFocusScale")
    this
        .zIndex(if (show) 1f else 0f)
        .graphicsLayer { scaleX = s; scaleY = s }
        .focusRing(show, corner, glow)
        .onFocusChanged { focused = it.hasFocus; onFocused?.invoke(it.hasFocus) }
        .then(if (addFocusable) Modifier.focusable() else Modifier)
}

/** 卡片用：给出是否显示焦点效果、以及封面圆角（16→22dp 弹簧）。 */
@Composable
fun rememberCardFocus(focused: Boolean): Pair<Boolean, Dp> {
    val show = focused && LocalDeviceProfile.current.input == BvInputMode.DPad
    val corner by animateDpAsState(if (show) 22.dp else 16.dp, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "cardCorner")
    return show to corner
}
```

**聚焦时变色**：每个可聚焦的按钮、标签、列表项组件内部用 `interactionSource.collectIsFocusedAsState()` 拿 focused，当 `focused && DPad` 时把 containerColor 换成 `inverseSurface`、contentColor 换成 `inverseOnSurface`（filled primary 按钮除外），用 `animateColorAsState(target, MaterialTheme.motionScheme.fastEffectsSpec())` 过渡。

### 6.7 电视滚动 `designsystem/focus/PivotBringIntoViewSpec.kt`

```kotlin
@file:OptIn(ExperimentalFoundationApi::class)

package dev.aaa1115910.bv.ui.designsystem.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile

/** 焦点项保持在容器 pivot 比例处：横向行 0.15，纵向列表 0.30。 */
class PivotBringIntoViewSpec(private val pivot: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        offset - containerSize * pivot
}

private val RowPivot = PivotBringIntoViewSpec(0.15f)
private val ColumnPivot = PivotBringIntoViewSpec(0.30f)

/** 只在电视上生效。LazyRow 外层套 TvRowPivot，LazyColumn 外层套 TvColumnPivot。 */
@Composable
fun TvRowPivot(content: @Composable () -> Unit) {
    if (LocalDeviceProfile.current.isTv) CompositionLocalProvider(LocalBringIntoViewSpec provides RowPivot, content = content) else content()
}

@Composable
fun TvColumnPivot(content: @Composable () -> Unit) {
    if (LocalDeviceProfile.current.isTv) CompositionLocalProvider(LocalBringIntoViewSpec provides ColumnPivot, content = content) else content()
}
```

- `LazyColumn` 外层套 `TvColumnPivot`，其中每一行的 `LazyRow` 外层**再**套 `TvRowPivot`（内层覆盖外层）。
- 若编译要求实现 `scrollAnimationSpec`，加 `override val scrollAnimationSpec: AnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 380f)`。
- **电视上每一个 `LazyRow`、`LazyVerticalGrid`、按钮行都必须加 `Modifier.focusRestorer().focusGroup()`**，从别的行回来时焦点回到上次的位置。

### 6.8 遥控器按键 `designsystem/focus/BvKey.kt`

```kotlin
package dev.aaa1115910.bv.ui.designsystem.focus

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*

enum class BvKey { Up, Down, Left, Right, Center, CenterLong, Menu, Back, ChannelUp, ChannelDown, PlayPause, FastForward, Rewind }

fun KeyEvent.toBvKey(): BvKey? {
    val native = nativeKeyEvent
    return when (native.keyCode) {
        AndroidKeyEvent.KEYCODE_DPAD_UP -> BvKey.Up
        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> BvKey.Down
        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> BvKey.Left
        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> BvKey.Right
        AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER ->
            if (native.isLongPress) BvKey.CenterLong else BvKey.Center
        763 -> BvKey.CenterLong   // 部分盒子的「长按确认」键码
        AndroidKeyEvent.KEYCODE_MENU -> BvKey.Menu
        AndroidKeyEvent.KEYCODE_BACK -> BvKey.Back
        AndroidKeyEvent.KEYCODE_CHANNEL_UP, AndroidKeyEvent.KEYCODE_PAGE_UP -> BvKey.ChannelUp
        AndroidKeyEvent.KEYCODE_CHANNEL_DOWN, AndroidKeyEvent.KEYCODE_PAGE_DOWN -> BvKey.ChannelDown
        AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, AndroidKeyEvent.KEYCODE_MEDIA_PLAY,
        AndroidKeyEvent.KEYCODE_MEDIA_PAUSE, AndroidKeyEvent.KEYCODE_SPACE -> BvKey.PlayPause
        AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> BvKey.FastForward
        AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> BvKey.Rewind
        else -> null
    }
}

/** 只处理 KeyDown；handler 返回 true 表示已消费。 */
fun Modifier.onBvKey(handler: (BvKey) -> Boolean): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) false else event.toBvKey()?.let(handler) ?: false
}
```

所有遥控器按键逻辑都通过 `onBvKey` 写，各界面里不要直接判断 keyCode。

### 6.9 动效令牌（只用这些，不要自己写 tween 时长）

| 用途 | 写法 |
|---|---|
| 焦点放大、按钮按压变形、点赞弹出 | `MaterialTheme.motionScheme.fastSpatialSpec()` |
| 卡片圆角变化、导航轨展开、抽屉 | `defaultSpatialSpec()` |
| 卡片 → 播放器转场、刷刷翻页回弹 | `slowSpatialSpec()` |
| 焦点描边出现、按钮颜色切换 | `fastEffectsSpec()` |
| 控制层淡入淡出、标签切换 | `defaultEffectsSpec()` |
| 主题色切换、氛围光渐变 | `slowEffectsSpec()` |

`ThemePrefs.reduceMotion = true` 时 `BvTheme` 已切到 `MotionScheme.standard()`；另外在点赞爱心、形状变形处检查 `reduceMotion`，为 true 时直接跳到终态。

---

## 7. 公共组件（`designsystem/component/`）

每个组件的规格都来自 `_src/2-mock.css`。表中「色」指 `MaterialTheme.colorScheme` 的角色。

### 7.1 `VideoCard`（原型 `.m-vc` + `vcard()`）

```kotlin
@Composable
fun VideoCard(
    data: VideoCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,              // 横向行里固定宽度；网格里传 null（填满格子）
    progress: Float? = null,        // 0..1，继续观看进度条
    showMeta: Boolean = true,       // UP 主 + 播放量
    showStats: Boolean = false,     // 封面底部「▶ 播放　弹 弹幕」渐变条
    onLongClick: (() -> Unit)? = null,
)
```

结构（从上到下，`Column(verticalArrangement = spacedBy(7.dp))`）：
1. 封面 `Box`：`fillMaxWidth().aspectRatio(16f / 10f)`，圆角 = `rememberCardFocus(focused).second`（16→22），`focusRing(show, corner)`，`clip`，背景 `surfaceContainerHigh`。
   - `AsyncImage(cover, ContentScale.Crop, fillMaxSize)`。
   - 时长角标：右下 6dp；内边距 水平 6dp 垂直 2dp；圆角 8dp；背景 `Color.Black.copy(alpha = .55f)`；文字 `BvType.Badge` 白色；数字等宽。
   - `showStats`：底部全宽，上内边距 18dp、左右 8dp、下 6dp，背景竖直渐变 透明→`Black α0.6`，文字 `BvType.Badge` 白色「▶ {play}　弹 {danmaku}」。
   - `progress != null`：底部 3dp 高、宽度 = progress 比例的条，颜色 `primary`。
2. 标题：`BvType.CardTitle`（列表行里 14sp 时用 `BvType.Body.copy(fontWeight = Medium)`），最多 2 行，省略号，色 `onSurface`。
3. `showMeta`：`Row(spacedBy(8.dp))`，`BvType.Meta`，色 `onSurfaceVariant`：UP 主名（1 行省略）+「{play}播放」。

焦点：整个 Column 用 `combinedClickable(interactionSource, indication = ripple(), onClick, onLongClick)`；`interactionSource.collectIsFocusedAsState()` 得 focused；Column 加 `zIndex` 和 `graphicsLayer { scale = if (show) 1.08 else 1 }`（`fastSpatialSpec` 动画）；**描边只加在封面 Box 上**。

长按菜单（手机/平板）：弹出 `DropdownMenu`：「从这里开始刷」→ `ShortsRoute(startAid = data.avid)`；「稍后再看」；「UP 主主页」。电视上长按确认键弹同样菜单。

### 7.2 `PosterCard`（原型 `poster()`）

同 `VideoCard`，区别：封面 `aspectRatio(3f / 4f)`；左上角评分角标：位置 6dp，背景 `primary`、文字 `onPrimary`、`BvType.Badge`、圆角 8；标题 1 行；副信息 `BvType.Meta`（如「番剧 · 更新至第 8 话」）。

### 7.3 `SectionHeader`（原型 `.m-h`）

`Row(SpaceBetween, CenterVertically)`：左标题 `BvType.Section` 色 `onSurface`；右侧可选动作文字 `BvType.Label` 色 `primary`（如「全部」「全部 ›」「完整 ›」），可点击（电视上可聚焦，`bvFocus`）。

### 7.4 `BvTabRow`（原型 `.m-tabs`）

`Row(spacedBy(22.dp))`，水平内边距 16dp，底部 1dp `outlineVariant` 分隔线。每个 Tab：`BvType.Body`，内边距上 10 下 9；未选中色 `onSurfaceVariant`；选中色 `primary` + `FontWeight.Bold`，下方指示条：高 3dp、宽度为文字宽度的 60%（左右各缩 20%）、上圆角 3dp、色 `primary`，指示条位置变化用 `defaultSpatialSpec` 动画。电视上 Tab 可聚焦，**聚焦即选中**。

### 7.5 `BvChip`（原型 `.m-chip`）

高度由内边距决定：水平 14dp、垂直 6dp；文字 `BvType.Small.copy(fontWeight = Medium)`。
- 未选中：圆角 10dp，1dp 内描边 `outlineVariant`，文字 `onSurfaceVariant`。
- 选中：圆角变为全圆（999dp，`defaultSpatialSpec` 动画），背景 `secondaryContainer`，文字 `onSecondaryContainer`，无描边。
- 电视：`bvFocus()`，聚焦变色规则见 6.6。

### 7.6 `ActionButtonGroup`（原型 `.m-bg` + `btnGroup()`，视频详情的点赞/投币/收藏/稍后/分享）

5 个等宽按钮，`Row(spacedBy(2.dp))`，每个 `weight(1f)`：
- 内容 `Column(CenterHorizontally, spacedBy(3.dp))`：图标 20dp + 文字 `BvType.Meta.copy(fontWeight = Medium)`；内边距 上 9 下 8 左右 4。
- 未选中：背景 `surfaceContainerHigh`，内容色 `onSurfaceVariant`；圆角：第一个 左 20 右 8，中间 8，最后一个 左 8 右 20。
- 选中（已点赞/已投币/已收藏）：背景 `primary`，内容色 `onPrimary`，四角都 20dp。圆角变化用 `fastSpatialSpec`。
- 实现：用 5 个 `ToggleButton`，`shapes` 分别取 `ButtonGroupDefaults.connectedLeadingButtonShapes()` / `connectedMiddleButtonShapes()` / `connectedTrailingButtonShapes()`，再用 `ToggleButtonDefaults.toggleButtonColors(containerColor = surfaceContainerHigh, contentColor = onSurfaceVariant, checkedContainerColor = primary, checkedContentColor = onPrimary)`；如果这些 shapes 无法给出上面的圆角，就自己写 `Surface(onClick, shape = animated RoundedCornerShape)`，**以上面的圆角数值为准**。
- 图标：点赞 `ThumbUp`、投币（自绘硬币图标，或 `Icons.Outlined.MonetizationOn`）、收藏 `Star`、稍后 `WatchLater`、分享 `Share`。
- 文字：点赞/投币/收藏显示数量，后两个显示「稍后」「分享」。

### 7.7 `SegmentedRow`（设置里的单选组，原型 `segRow()`）

与 7.6 同样的外观，但内容只有文字 `BvType.Label`，内边距垂直 10dp；只能单选。

### 7.8 `BvAvatar`

圆形头像图片，裁剪为 `MaterialShapes.Cookie9Sided.toShape()`。参数 `size: Dp`、`url: String?`、`fallbackText: String`。没有图片时：背景 `primaryContainer`、文字 `onPrimaryContainer`、字号 = size × 0.4、字重 Bold，显示名字首字。变体 `shape = Sunny`（用在评论头像、等级徽章，背景 `secondaryContainer`）。
- 尺寸表：首页顶栏 40；卡片作者行 26/32；详情 UP 主行 40（平板 44，电视 32）；刷刷操作列 46；「我的」头像 72；评论 30；动态 UP 主行 48。

### 7.9 `CinemaScrim`（原型 `.m-scrim-b`）

铺满父容器的两层渐变：
- 下：从底部 `Black α0.78` 到 40% 高度处透明。
- 上：从顶部 `Black α0.45` 到 22% 高度处透明。

### 7.10 `Ambient`（原型 `.m-ambient`，氛围光）

铺满父容器且四周各外扩 10%：封面图 `ContentScale.Crop`，`graphicsLayer { scaleX = 1.1f; scaleY = 1.1f; alpha = 0.55f }`，饱和度 1.4（`ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.4f) })`），模糊 48dp：
- API ≥ 31：`Modifier.blur(48.dp)`。
- API < 31：用项目里已有的 `coil.transform.BlurTransformation`，图片请求尺寸 64px，radius 25。
- 低内存设备（`ActivityManager.isLowRamDevice()`）不画氛围光，只画 `surface` 纯色。
- 封面切换时用 `Crossfade(animationSpec = MaterialTheme.motionScheme.slowEffectsSpec())`。

### 7.11 `TvBackdrop`（原型 `.m-backdrop`，电视全幅背景）

位置：右上角对齐，宽 = 父宽 74%，高 = 父高 76%（视频详情页用 70%）。内容：封面图 Crop。上面叠两层渐变（颜色用 `surface`，所以随主题变化）：
- 横向：x=0 处 `surface` 不透明 → x=30% 处 `surface α0.7` → x=62% 处透明。
- 纵向：底部往上 2% 处 `surface` 不透明 → 48% 处透明。
换图时 `Crossfade(slowEffectsSpec)`。

### 7.12 `BvSearchField`（原型 `.m-search`）

高 48dp（电视 52dp），全圆角，背景 `surfaceContainerHigh`（处于输入状态时 `surfaceContainerHighest`），左内边距 16 右 6，`Row(spacedBy(10.dp))`：搜索图标 24 色 `onSurfaceVariant` + 文字 `BvType.Body`（占位符色 `onSurfaceVariant`，输入文字色 `onSurface`）+ 右侧 36dp 麦克风图标按钮（语音搜索：`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`，不可用时隐藏）。

### 7.13 `CommentItem`（原型 `comment()`）

`Row(spacedBy(10.dp))`：`BvAvatar(30.dp, shape = Sunny)` + `Column(spacedBy(3.dp))`：名字 `BvType.Label.copy(fontWeight = Bold)` 色 `onSurfaceVariant`；正文 `BvType.Small`（行高 1.5）；底行 `BvType.Meta` 色 `onSurfaceVariant`：点赞图标 14dp + 点赞数 + 「回复」。表情、图片、楼中楼沿用 `app/mobile/.../component/reply/` 的现有实现，只换样式。

### 7.14 `WavyProgress`

`LinearWavyProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(12.dp), color = primary（详情内嵌播放器用 inversePrimary）, trackColor = Color.White.copy(alpha = .35f))`。暂停时改用普通 `LinearProgressIndicator`（拉直），切换用 `Crossfade(defaultEffectsSpec)`。可拖动的场合（播放器）在其上叠一个透明 `Slider` 处理拖动。

### 7.15 `BvListItem`（原型 `.m-li`）

`Row(spacedBy(14.dp), CenterVertically)`，内边距 水平 16 垂直 10，圆角 16。左侧 40dp 图标容器（圆角 12，或设置页用 Cookie9 形状）；中间 `Column(spacedBy(2.dp))`：标题 `BvType.BodyBold`、副标题 `BvType.Label.copy(fontWeight = Normal)` 色 `onSurfaceVariant` 1 行省略。选中态：背景 `secondaryContainer`、内容色 `onSecondaryContainer`。

### 7.16 `ColorTile`（「我的」页彩色入口卡，原型 `tile()`）

内边距 14，圆角 24，`Column(spacedBy(10.dp))`。参数 `role: Primary | Secondary | Tertiary`：背景 `{role}Container`、内容 `on{Role}Container`；图标容器 40dp 圆角 14 背景 `{role}`、图标色 `on{Role}`；标题 15sp Bold；副标题 12sp α0.8。

### 7.17 `RankBadge`（热搜名次）

22dp 方块，圆角 8，数字 12sp ExtraBold 等宽。第 1 名 `primary/onPrimary`，第 2 名 `tertiary/onTertiary`，第 3 名 `secondary/onSecondary`，之后无背景、色 `onSurfaceVariant`。

### 7.18 `LoadingState`

整页加载用 `ContainedLoadingIndicator()` 居中；列表底部加载更多用 `LoadingIndicator()`；下拉刷新用 `PullToRefreshBox`（指示器用默认的 Expressive 加载指示器）。删除所有旧的 M2 `pullRefresh`。

---

## 8. 应用外壳与导航

### 8.1 单入口 `MainActivity`

`app/ui/src/main/AndroidManifest.xml`：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-feature android:name="android.software.leanback" android:required="false" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
    <application android:banner="@drawable/tv_banner">
        <activity
            android:name="dev.aaa1115910.bv.ui.MainActivity"
            android:exported="true"
            android:resizeableActivity="true"
            android:theme="@style/Theme.BV.Main"
            android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboard|keyboardHidden|navigation|density|uiMode">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="bugvideo" android:host="qrtoken" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- `tv_banner`：把 `app/tv/src/main/res` 里电视版用的 banner 复制过来。
- `Theme.BV.Main`：复制 `app/mobile` 的 XML 主题（`Theme.Material3.DayNight.NoActionBar` + splash），不要强制横屏。
- 同时**删除** `app/mobile` 和 `app/tv` 两个旧 `MainActivity` 的 `<intent-filter>`（保留 activity 声明，类先不删），以及旧 `IntentHandlerActivity` 里 `bugvideo://qrtoken` 的 intent-filter。

`MainActivity.kt`：

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val initialPrefs = runBlocking { Prefs.themePrefsFlow.first() }   // 只读一次，避免首帧主题闪烁
        setContent {
            BvRoot(initialPrefs) {
                val nav = rememberNavController()
                AppShell(nav)
                DeepLinkEffect(intent, nav)   // 处理 bugvideo://qrtoken：弹出现有 QrTokenResult 的导入账号对话框
            }
        }
    }
}
```

- 原电视版启动时的「用户锁」：如果当前用户 `lock` 不为空，`AppShell` 之前先显示现有 `UnlockUserScreen` 的逻辑（重写样式：电视居中卡片），解锁后再进外壳。
- 原电视版首次播放的「遥控器说明」（RemoteControllerPanelDemo）改成播放器第一次打开时的覆盖层，只显示一次（复用现有的「已显示」偏好）。
- 去掉 `accompanist systemuicontroller` 的使用，改用 `enableEdgeToEdge()`。

### 8.2 路由 `navigation/Routes.kt`

```kotlin
@Serializable data object HomeRoute
@Serializable data class ShortsRoute(val startAid: Long? = null)
@Serializable data object PgcRoute
@Serializable data object DynamicRoute
@Serializable data object MeRoute
@Serializable data class SearchRoute(val query: String = "")
@Serializable data class VideoRoute(val aid: Long)
@Serializable data class SeasonRoute(val seasonId: Int? = null, val epId: Int? = null)
@Serializable data class PlayerRoute(/* 参数照抄旧 VideoPlayerV3Activity / 手机 VideoPlayerActivity 的 Intent extras，一个不少 */)
@Serializable data class SpaceRoute(val mid: Long)
@Serializable data class TagRoute(val tagId: Int)
@Serializable data class DynamicDetailRoute(val id: String)
@Serializable data class PgcIndexRoute(val type: Int)
@Serializable data object PgcTimelineRoute
@Serializable data object HistoryRoute
@Serializable data object FavoriteRoute
@Serializable data object ToViewRoute
@Serializable data object FollowingSeasonRoute
@Serializable data object FollowingRoute
@Serializable data object AccountsRoute
@Serializable data object UserLockRoute
@Serializable data object LoginRoute
@Serializable data class SettingsRoute(val category: String = "")   // "" = 分类列表；"appearance" "play" "danmaku" "subtitle" "network" "account" "storage" "about"
@Serializable data object LogsRoute
@Serializable data object MediaCodecRoute
@Serializable data object SpeedTestRoute
```

`navigation/TopLevel.kt`：

```kotlin
enum class TopLevel(val route: Any, @StringRes val label: Int, val icon: ImageVector, val iconSelected: ImageVector) {
    Home(HomeRoute, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),               // 首页
    Shorts(ShortsRoute(), R.string.nav_shorts, Icons.Outlined.Smartphone, Icons.Filled.Smartphone), // 刷刷（图标用竖屏手机 + 播放三角的自定义 ImageVector 更佳，见原型 ICONS.shorts 的 path）
    Pgc(PgcRoute, R.string.nav_pgc, Icons.Outlined.Movie, Icons.Filled.Movie),               // 影视
    Dynamic(DynamicRoute, R.string.nav_dynamic, Icons.Outlined.Subscriptions, Icons.Filled.Subscriptions), // 动态
    Me(MeRoute, R.string.nav_me, Icons.Outlined.Person, Icons.Filled.Person),                 // 我的
}
```

刷刷图标：用 `_src/4-data.js` 里 `ICONS.shorts` 的 SVG path 做成 `ImageVector`（`PathParser().parsePathString(path).toNodes()`），其他图标也可以照 `ICONS` 里的 path 做，保证和原型一模一样。

### 8.3 `AppShell`

```
val profile = LocalDeviceProfile.current
val current = 当前回退栈顶对应的 TopLevel（hasRoute 判断），不是一级页面时为 null
```

| NavKind | 结构 |
|---|---|
| `Bar` | `Box(fillMaxSize)`：`BvNavHost` 铺满；`current != null` 时 `PhoneNavBar` 叠在底部（`Alignment.BottomCenter`）。一级页面的滚动内容底部留出导航栏高度（通过 `LocalBottomBarPadding` 提供 = 64dp + navigationBars inset）。 |
| `Rail` / `RailExpanded` | `Row`：`TabletRail(expanded = navKind == RailExpanded)` + `BvNavHost(Modifier.weight(1f))`。非一级页面（详情等）时隐藏导航轨，内容全宽。 |
| `TvRail` | `Box`：`BvNavHost` 铺满；`current != null` 时 `TvRail` 叠在左侧（`Alignment.CenterStart`）。电视一级页面内容从 x=96dp 开始。 |

切换一级页面：`nav.navigate(route) { popUpTo(HomeRoute) { saveState = true }; launchSingleTop = true; restoreState = true }`。页面切换动画：`fadeIn(defaultEffectsSpec) + slideIn` 方向按导航顺序（沿用旧 `MobileMainScreen` 的做法）。

### 8.4 `PhoneNavBar`（原型 `.m-nbar`）

`ShortNavigationBar(containerColor = surfaceContainer)`，5 项（首页/刷刷/影视/动态/我的）。每项：图标 24dp 放在 56×32dp 全圆角指示器里，选中指示器 `secondaryContainer`、图标 `onSecondaryContainer`；标签 11sp，选中 `onSurface` Bold，未选中 `onSurfaceVariant`。
**在刷刷页时**：`containerColor = Color.Black.copy(alpha = .72f)`，未选中标签/图标 `Color.White.copy(alpha = .7f)`，选中标签白色，且导航栏叠在视频上（视频不让位）。

### 8.5 `TabletRail`（原型 `.m-rail`）

`WideNavigationRail(state = rememberWideNavigationRailState(if (expanded) Expanded else Collapsed), header = { … })`：
- 收起（96dp 宽）：顶部菜单图标按钮（点击展开为 `ModalWideNavigationRail`）；FAB：56×56dp、圆角 16、背景 `primaryContainer`、图标搜索 → `SearchRoute()`，上下外边距 8/16；然后 5 个一级入口（图标在 56×32 指示器里，标签 12sp 在下方，选中 `secondaryContainer` + Bold）；`Spacer(weight 1)`；底部「设置」→ `SettingsRoute()`。
- 展开（220dp 宽，`RailExpanded`）：FAB 变为扩展型（图标 + 「搜索」15sp SemiBold，左内边距 18）；每项横向排列（图标 + 标签 15sp），高 52dp，全圆角，选中整行 `secondaryContainer`。
- 顶部第一项在收起状态是菜单按钮，展开状态没有菜单按钮（直接从 FAB 开始）。

### 8.6 `TvRail`（原型 `.m-tvrail`，**自己实现，不用 WideNavigationRail**）

```
Column(
  width = animateDpAsState(if (hasFocus) 196.dp else 76.dp, defaultSpatialSpec),
  fillMaxHeight, padding(vertical = 27.dp, horizontal = if (hasFocus) 14.dp else 0.dp),
  background = if (hasFocus) surfaceContainer else 透明,
  阴影：hasFocus 时向右 48dp 模糊的黑色阴影（Modifier.shadow 或 drawBehind 画横向渐变 Black α0.7→透明）,
  horizontalAlignment = 收起时居中、展开时 Start,
  spacedBy(10.dp),
  Modifier.onFocusChanged { hasFocus = it.hasFocus }.focusRestorer().focusGroup()
)
```
内容依次：头像 36dp（`BvAvatar`，点击 → 我的）、10dp 间隔、搜索、首页、刷刷、影视、动态、我的、`Spacer(weight 1)`、设置。
每项：收起时 44×44dp 全圆角，只有图标；展开时高 44、宽填满、水平内边距 14、`Row(spacedBy(14.dp))` 图标 + 标签 14sp SemiBold。选中项背景 `secondaryContainer`、图标 `onSecondaryContainer`；聚焦项 `bvFocus()` 且背景 `inverseSurface`、内容 `inverseOnSurface`。按确认键才切换页面（不是聚焦即切换）。

**电视返回键（一级页面）：** 焦点不在导航轨时 → 把焦点移到导航轨当前选中项；焦点已在导航轨时 → 3 秒内再按一次退出，第一次按显示 Toast「再按一次退出」。

### 8.7 页面分派

每个 `XxxScreen` 都这样写：

```kotlin
@Composable
fun HomeScreen(nav: NavController) {
    val vm: RecommendViewModel = koinViewModel()   // ViewModel 在这里创建，三个布局共享
    when (LocalDeviceProfile.current.layout) {
        DeviceLayout.Phone -> HomePhone(...)
        DeviceLayout.Tablet -> HomeTablet(...)
        DeviceLayout.Tv -> HomeTv(...)
    }
}
```

---

## 9. 逐界面规格

**通用约定：**
- 每个界面写完后，打开设计页「06 · 界面」，选同一界面和设备，与模拟器截图逐块对比（方法见第 14 节）。
- 下面写的坐标如「left 96 / top 74」是相对屏幕左上角的 dp，用 `Box` + `Modifier.offset` 或 `padding` 实现；「Row spacedBy(12)」即 `Arrangement.spacedBy(12.dp)`。
- 「数据」一栏告诉你从哪里拿数据。**加载逻辑照抄对应的旧界面**（括号里给出了旧文件），只换界面。
- 原型中的示例标题、UP 主、数字都换成真实数据。
- 列表都要做「快到底部时加载更多」：最后可见项索引 > 总数 − 6 时调用 ViewModel 的加载更多方法（沿用旧界面的调用方式）。

### 9.1 首页（`homePhone` / `homeTablet` / `homeTv`）

**数据：** 推荐 `RecommendViewModel`，热门 `PopularViewModel`（旧：`app/mobile/.../screen/home/RcmdPage.kt`、`PopularPage.kt`）；分区 31 个 `Ugc*ViewModel`（旧：`app/tv/.../screens/main/UgcContent.kt` 与 `UgcRegionScaffold`）；继续观看 `HistoryViewModel` 前 10 条（未登录或为空时整个区块隐藏）；精选 = 推荐列表前 5 条；顶部搜索框占位词 = `SearchInputViewModel` 的第一个热搜词，取不到时用「搜索」。

**手机（Compact / Medium）：**
```
Column(fillMaxSize, background surface)
├─ 顶栏 Row(spacedBy 10, CenterVertically) padding(top = statusBar + 6, start 16, end 16, bottom 10)
│   ├─ BvAvatar(40)  → 点击打开「我的」（MeRoute）
│   └─ BvSearchField(weight 1)  占位词=热搜词，点击 → SearchRoute()
├─ BvTabRow: 推荐 / 热门 / 分区
└─ HorizontalPager(3 页)
    推荐页 = LazyVerticalGrid(columns = Fixed(2)（Medium 用 Fixed(3)），
             contentPadding(start 16, end 16, bottom = 导航栏高度 + 16), 水平/垂直间距 12)
      ├─ [整行] 精选轮播 padding(top 14)，高 196：
      │    HorizontalMultiBrowseCarousel(state = rememberCarouselState { 5 }, preferredItemWidth = 300.dp,
      │       itemSpacing = 8.dp, minSmallItemWidth = 40.dp, maxSmallItemWidth = 56.dp)
      │    每项：maskClip(RoundedCornerShape(28))，封面 Crop + CinemaScrim；
      │    左下 padding(16, 14) Column：Eyebrow「今日精选」白色 α0.85 + 4dp + 标题 BvType.HeroPhone 白色 最多 2 行
      ├─ [整行] SectionHeader「继续观看」+「全部」→ HistoryRoute，padding(top 16)
      ├─ [整行] LazyRow(spacedBy 10, padding top 10) VideoCard(width 150, progress, showMeta = false)
      ├─ [整行] SectionHeader「为你推荐」padding(top 14)
      └─ VideoCard × N（showMeta = true）
    热门页 = 同样的网格，没有精选和继续观看
    分区页 = 顶部 LazyRow 分区标签（BvChip，31 个，默认选第一个「动画」），下面同样的 2 列网格显示该分区视频
└─ PhoneNavBar（外壳提供）
```
下拉刷新：每个页用 `PullToRefreshBox`。

**平板（Expanded+）：**
```
Row(fillMaxSize)
├─ TabletRail（外壳提供）
└─ Column(weight 1, padding(start 8, end 28, top 20), spacedBy 18)
    ├─ Row(spacedBy 16, CenterVertically)
    │   ├─ BvSearchField(width 440)  占位「搜索视频、番剧、UP 主」
    │   ├─ Row(weight 1, spacedBy 8) BvChip：推荐(选中) 热门 分区 动画 知识 美食 户外
    │   │    （后 4 个是快捷分区，点击 = 选中「分区」页并切到该分区）
    │   └─ BvAvatar(40)
    └─ 下面是一个 LazyVerticalGrid(columns = Adaptive(180.dp), 水平/垂直间距 16)，整行项依次为：
        ├─ 居中大图轮播，高 300：HorizontalPager(5 页, contentPadding(horizontal = 122.dp), pageSpacing = 12.dp)
        │    每页圆角 28，封面 Crop（用横图）；只有当前页显示文字层：
        │    CinemaScrim + 左下 padding(28, 24) Column(spacedBy 8)：
        │      Eyebrow「今日精选 · {分区名}」白 α0.85
        │      标题 BvType.HeroTablet 白色 最多 2 行
        │      「{UP} · {播放}播放 · {时长}」13sp 白 α0.85
        │      Row(spacedBy 10, top 6)：Button「▶ 播放」(高 40，primary) + FilledTonalButton「稍后再看」(secondaryContainer)
        │    两侧露出的上一页/下一页宽 110dp，同样圆角 28，不显示文字。每 6 秒自动翻页，用户触摸后暂停 10 秒。
        ├─ SectionHeader「继续观看」+「全部」
        ├─ LazyRow(spacedBy 16) VideoCard(width 196, progress, showMeta = false)
        ├─ SectionHeader「为你推荐」
        └─ VideoCard × N（showMeta = true）
```

**电视：**
```
Box(fillMaxSize, background surface)  // 电视永远深色
├─ TvBackdrop(当前精选封面)
├─ TvRail（外壳）
├─ 顶栏 at (left 96, top 24, right 48)：Row(SpaceBetween)
│   ├─ Row(spacedBy 8) BvChip：推荐(选中) 热门 分区 关注   ← 聚焦即选中，切换下方内容
│   │    （「关注」= 动态里的视频投稿流，数据 DynamicViewModel 的视频类动态）
│   └─ 时钟「HH:mm」15sp SemiBold onSurfaceVariant，每分钟刷新
└─ TvColumnPivot { LazyColumn(fillMaxSize, contentPadding(top 74)) }：
    item 0 — 精选信息区 Column(padding(start 96), width 430, spacedBy 8)，整块高度固定 266dp：
       Eyebrow「今日精选 · {分区名}」色 primary
       标题 BvType.Display 最多 2 行
       Row(spacedBy 10)：画质标签（有 4K/HDR 信息时显示 BvChip 选中样式，padding 1×8，11sp）+「{播放}播放 · {时长} · {UP}」12sp onSurface
       简介 13sp 行高 21sp 最多 2 行 onSurfaceVariant（推荐数据没有简介时，用 VideoInfoRepository 按需取精选视频的简介；取不到就隐藏这一行）
       Row(spacedBy 12, top 6, focusRestorer)：
         Button「▶ 播放」高 52 水平内边距 28 文字 BvType.ButtonLarge —— 页面初始焦点
         FilledTonalButton「稍后再看」高 52
         FilledTonalIconButton 52×52 点赞图标（背景 surfaceContainerHighest）
       精选每 8 秒切到下一条（TvBackdrop 与文字一起 Crossfade），焦点在本区按钮上时不自动切换。
    item 1 — 「为你推荐」行：
       Row(padding(start 96, end 48), SpaceBetween)：SectionHeader 文本 15sp +「按 ↓ 查看更多行」12sp onSurfaceVariant
       TvRowPivot { LazyRow(contentPadding(start 102, end 48, top 12, bottom 16), spacedBy 18, focusRestorer + focusGroup)
          VideoCard(width 180, showMeta = false) }
    item 2 — 「热门」行（同上，数据 PopularViewModel）
    item 3..7 — 前 5 个分区各一行（标题为分区名）
```
按 ↓ 从按钮进入「为你推荐」行；继续 ↓ 依次进入下面的行，列表按 0.30 枢轴滚动。

### 9.2 刷刷（`shortsPhone` / `shortsTablet` / `shortsTv`）

**数据：** 新建 `ShortsViewModel`（见第 10 节）。每一项包含 aid、cid、标题、封面、UP 主（名字、mid、头像）、点赞/投币/收藏/评论数、标签、宽高（判断横竖屏）。

每一页都包在 `CoverTheme(cover, forceDark = true)` 里，所以每条视频的按钮、进度条颜色都不同。

**手机：**
```
Box(fillMaxSize, background Black)
├─ VerticalPager(state, beyondViewportPageCount = 1, key = { aid })  每页：
│   CoverTheme(cover, forceDark = true) { Box(fillMaxSize) {
│     竖屏视频：PlayerSurface 铺满（首帧前显示封面 Crop）
│     横屏视频：Ambient(封面) 铺满 + 16:9 视频全宽、垂直居中再上移自身高度的 10%
│     CinemaScrim
│     顶栏 at (top = statusBar + 2, start 16, end 12)：Row(SpaceBetween)：「刷刷」20sp ExtraBold 白色 + 搜索图标按钮(白)
│     右侧操作列 at (end 10, bottom 150) Column(spacedBy 16, CenterHorizontally)，每项 Column(spacedBy 3)：
│        BvAvatar(46)（点击 → SpaceRoute）
│        点赞：46dp 按钮，背景 White α0.16 + 背景模糊（API≥31 用 Modifier.blur 作用在背景层；低版本只用半透明），图标白；
│             已赞时背景 primary、图标 onPrimary，形状由圆形变为圆角 16 方形（fastSpatialSpec）；下方数量 11sp SemiBold 白色带阴影
│        投币（文字「投币」）、收藏（数量）、评论（数量）、分享（文字「分享」）同样式
│     底部信息 at (start 16, end 76, bottom 112) Column(spacedBy 6)：「@{UP}」15sp Bold 白；标题 14sp 行高 1.45 白 最多 2 行；标签 12sp 白 α0.85
│     WavyProgress at (start 16, end 16, bottom 90)，color primary，track White α0.35
│   } }
└─ PhoneNavBar（刷刷样式：黑 α0.72，叠在视频上）
```
交互：单击暂停/继续；双击点赞 + 在点击位置弹出 96dp 爱心（`MaterialShapes.Heart` 填 primary，或 Heart 图标），动画：0→1.15→1 缩放并轻微旋转，然后上移 60dp 淡出，总时长 900ms；长按 2 倍速、松手恢复，屏幕顶部中间显示「2× 倍速中」小标签；向左滑 → `SpaceRoute(mid)`；点标题 → `VideoRoute(aid)`；评论 → `ModalBottomSheet` 显示 `CommentViewModel` 列表；手机横过来 → 当前视频全屏横屏播放，竖回来恢复。

**平板：**
```
Row(fillMaxSize)
├─ TabletRail
├─ Box(weight 1, background Black)
│   ├─ Ambient(当前封面) 铺满
│   ├─ VerticalPager（同手机），每页内容：视频框居中，top 20 bottom 20，宽 = 高 × 9/16，圆角 28，clip
│   │     框内：视频 + CinemaScrim + 底部 Column(start/end 20, bottom 30, spacedBy 8)：「@UP」Bold、标题 15sp、标签 12sp α0.85、WavyProgress
│   ├─ VerticalFloatingToolbar(expanded = true) 位于视频框右侧 18dp、底部 40dp：
│   │     点赞(FilledIconButton primary) 投币 收藏 分享（IconButton）
│   └─ VerticalFloatingToolbar 位于视频框左侧 24dp、垂直居中：上一条 ↑、下一条 ↓
└─ 右侧信息栏 width 380, background surfaceContainerLow, padding(vertical 24, horizontal 20), Column(spacedBy 16)
    ├─ Row(spacedBy 12)：BvAvatar(44) + Column(weight 1)(名字 Bold + 「粉丝 X · N 个视频」Meta) + Button「+ 关注」高 36 内边距 16
    ├─ 标题 17sp Bold 行高 1.4
    ├─ Meta「{点赞} 点赞 · {评论} 评论 · {发布时间}」
    ├─ BvTabRow(水平内边距 0)：评论 {N} / 相关
    └─ LazyColumn(spacedBy 16) CommentItem × N
```
右侧栏也包在当前视频的 `CoverTheme`（非强制深色，跟随全局明暗）里。

**电视：**
```
Box(fillMaxSize)
├─ Ambient(当前封面) 铺满 + 叠一层 surface α0.5
├─ TvRail
├─ 播放队列 at (left 110, top 60) Column(spacedBy 10)：4 个缩略图 54×96 圆角 12（上一条、当前、下两条），
│     当前项外侧 3dp primary 描边，其余 alpha 0.55
├─ 视频框 at (left 300, top 27) 273×486，圆角 24，clip，可聚焦（bvFocus(scale = 1.05f, corner = 24.dp, glow = 16.dp, addFocusable = true)）—— 初始焦点
│     框内：视频 + CinemaScrim + WavyProgress(start/end/bottom 14)
└─ 信息区 at (left 600, top 60) width 312 Column(spacedBy 12)：
    ├─ Row(spacedBy 10)：BvAvatar(40) + Column(名字 Bold + 「粉丝 X」Meta) + FilledTonalButton「+ 关注」高 34 内边距 14（可聚焦）
    ├─ 标题 BvType.TitleLarge 行高 1.25
    ├─ Meta 12sp「{标签}　{点赞} 点赞」
    ├─ Row(spacedBy 8, focusRestorer)：40dp 图标按钮 × 5：点赞(已赞时 primary 背景 + 圆角 16)、投币、收藏、评论、详情(→ VideoRoute)
    └─ 评论卡 padding 14 圆角 20 背景 surfaceContainer α0.8，Column(spacedBy 12) 前 2 条 CommentItem
```
遥控器（焦点在视频框上时，用 `onBvKey`）：↑ / 频道+ 上一条；↓ / 频道− 下一条（翻页动画：新视频从 40dp 外滑入并淡入，`slowSpatialSpec`）；← → 快退快进 5 秒，连按加速（沿用旧电视播放器的加速逻辑）；确认暂停/继续；长按确认点赞；菜单键在右侧打开评论面板（宽 360 的侧边面板，样式同第 9.4 节播放器菜单）。→ 键从视频框移到右侧按钮（正常焦点搜索）。

### 9.3 视频详情（`detailPhone` / `detailTablet` / `detailTv`）

**数据：** `VideoDetailViewModel`、`CommentViewModel`（旧：`app/mobile/.../screen/VideoPlayerScreen.kt` 的信息/评论部分；电视 `app/tv/.../screens/VideoInfoScreen.kt` 的关注、收藏夹、分 P、合集、相关）。PGC 内容（`jumpToSeason` / redirect）跳到 `SeasonRoute`。
**新增功能：** 点赞 / 投币用 bili-api 已有的 `sendVideoLike` / `sendVideoCoin`（先 `check*` 拿状态）；投币弹出选择 1 枚 / 2 枚的对话框；收藏弹出收藏夹多选对话框（照抄电视版 FavoriteButton 的逻辑）；关注按钮可用（照抄电视版 UpButton）；稍后再看见第 10.3 节。
整页包在 `CoverTheme(cover)`（不强制深色）。

**手机：**
```
Column(fillMaxSize, background surface)
├─ 黑色区域 = 状态栏高度（视频上方）
├─ 内嵌播放器 16:9（宽 360 时高 203），背景黑：
│    CinemaScrim；左上返回 IconButton(白)，右上更多(白)；
│    底部 Row(start/end 12, bottom 8, spacedBy 10, 白色)：暂停 20dp | WavyProgress(weight 1, color inversePrimary) | 「06:12」11sp | 全屏 20dp
├─ Row：BvTabRow「简介」「评论 {N}」+ Spacer(weight 1) + 描边小标签「发弹幕」（padding 4×12, 12sp）
└─ HorizontalPager(简介 / 评论)
    简介 = LazyColumn(padding(vertical 14, horizontal 16), spacedBy 12)：
      ├─ Row(spacedBy 10)：BvAvatar(40) + Column(weight 1)(UP 名 14sp Bold + 「粉丝 X」Meta) + Button「+ 关注」高 36 内边距 16（已关注显示 FilledTonal「已关注」）
      ├─ 标题 BvType.Title 最多 2 行（点击展开全部标题 + 简介 + 标签）
      ├─ Meta「{播放}播放 · {弹幕}弹幕 · {MM-dd} · {BV号}」
      ├─ ActionButtonGroup
      ├─ SectionHeader「选集」+「{n}P ›」(点击打开分 P 底部抽屉，沿用旧 VideoPlayerPages 的 ModalBottomSheet)
      ├─ LazyRow(spacedBy 8) BvChip：每个分 P「P{n} {标题}」，当前播放的选中
      ├─ （有合集时）SectionHeader「合集 · {名称}」+ 同样的标签行
      ├─ SectionHeader「相关推荐」
      └─ 相关视频行 × N：Row(spacedBy 10)：封面 128 宽 16:10 圆角 12（含时长角标）+ Column(spacedBy 6)(标题 CardTitle 2 行 + Meta「{UP} · {播放}播放」)
    评论 = CommentViewModel 列表（最热/最新切换 BvChip），回复用 ReplySheetScaffold（沿用旧实现）
```
手机横屏 → 全屏播放器（第 9.4 节手机布局）。

**平板：**
```
Row(fillMaxSize, padding(vertical 20, horizontal 24), spacedBy 24, background surface)
├─ Column(width 720, spacedBy 14)
│   ├─ Row：返回 IconButton +「视频详情」15sp Bold
│   ├─ 内嵌播放器 720×405 圆角 28：底部控制 Row(start/end 20, bottom 14, spacedBy 12)：暂停 | WavyProgress(inversePrimary) | 「06:12 / 18:42」12sp | 全屏
│   ├─ 标题 BvType.TitleLarge
│   ├─ Row(spacedBy 12, CenterVertically)：BvAvatar(44) + Column(UP 名 Bold + Meta「粉丝 · 播放 · 日期」) + Button「+ 关注」高 36 + Spacer(weight 1) + ActionButtonGroup(width 330)
│   └─ LazyRow BvChip 分 P
└─ Column(weight 1, background surfaceContainerLow, 圆角 28, padding(vertical 16, horizontal 18), spacedBy 14)
    ├─ BvTabRow(水平内边距 0)：评论 {N} / 相关推荐
    ├─ Row BvChip：最热(选中) 最新
    └─ 列表（评论 CommentItem / 相关视频行）
```
宽度回到 Medium（平板竖屏）时自动使用手机布局。

**电视：**
```
Box(fillMaxSize, background surface)    // CoverTheme(cover, forceDark = true)
├─ Ambient(封面)
├─ TvBackdrop(封面, 高度 70%)
├─ 信息区 at (left 48, top 30) width 520, Column(spacedBy 10)：
│   ├─ Eyebrow「{分区} · {标签} · {n}P」色 primary
│   ├─ 标题 BvType.DetailTv 最多 2 行
│   ├─ Row(spacedBy 10)：BvAvatar(32) + UP 名 13sp Bold + Meta「粉丝 X · {播放}播放 · {点赞}点赞 · {投币}投币」
│   ├─ 简介 13sp 行高 21sp 最多 2 行 onSurfaceVariant（确认键打开全文对话框，沿用旧电视 VideoDescription）
│   └─ Row(spacedBy 10, top 4, focusRestorer)：
│        Button「▶ 继续 P{n} · {mm:ss}」（无历史时「▶ 播放」）高 52 内边距 28 ButtonLarge —— 初始焦点
│        48dp 图标按钮 × 5：点赞（已赞 primary 背景圆角 16）、投币、收藏、稍后再看、UP 主（→ SpaceRoute）
└─ 下方行 at (left 48, top 300) TvColumnPivot { LazyColumn }：
    ├─ SectionHeader「选集」15sp + TvRowPivot { LazyRow(spacedBy 14, padding top 10 bottom 12, end 48) 分 P 卡片 width 170：
    │      封面 16:10（用视频封面）+ 时长角标 + 已看部分进度条；标题 1 行「P{n} {标题}」 }
    ├─ （有合集时）合集行，同上
    └─ SectionHeader「相关推荐」+ VideoCard 行（width 180, showMeta = false）
```

### 9.4 播放器（`playerPhone` / `playerTablet` / `playerTv`）

**数据与内核：** 新建 `player:ui` 模块（第 11 节），合并 `player/mobile` 与 `player/tv` 的控制层；ViewModel 仍是 `VideoPlayerV3ViewModel`。整个播放器包在 `BvTheme(forceDark = true) { CoverTheme(cover, forceDark = true) { … } }`。
**补齐手机端缺失：** 字幕显示（`BottomSubtitle`）、字幕菜单、播放心跳上报（取消 `player/mobile/.../BvPlayer.kt` 里被注释掉的心跳）、画面比例、弹幕蒙版。

**手机（横屏全屏，780×360）：**
```
Box(fillMaxSize, Black)
├─ 视频 + 弹幕层（akdanmaku，沿用）+ 字幕
├─ 控制层（单击出现，3 秒无操作淡出，defaultEffectsSpec）：
│   CinemaScrim
│   顶栏 at (top 12, start 16, end 16) Row(spacedBy 10, CenterVertically, 白色)：
│     返回 | 标题「P{n} {分P名} · {视频标题}」14sp Bold 1 行 weight 1 | 字幕图标 | 小标签「弹 开」（开=secondaryContainer 全圆角，关=白色描边）点击切换弹幕 | 更多
│   中央大按钮 72dp：暂停中 = 圆角 24 的方形、背景 primaryContainer、图标播放；播放中 = 圆形、图标暂停。形状变化 fastSpatialSpec。
│   底部 Column(start/end 20, bottom 12, spacedBy 6, 白色)：
│     WavyProgress（可拖动）
│     Row(spacedBy 12, 12sp)：暂停 | 下一集 | 「08:30 / 18:42」 | Spacer | 描边标签「选集」「1.0×」(白 α0.4 描边) | 选中标签「1080P 高码率」 | 退出全屏
├─ 拖动进度时：在拖动位置正上方显示预览缩略图 140 宽 16:10 圆角 14，白色 2dp 描边，图内底部居中时间 11sp SemiBold（videoshot，沿用旧逻辑）
└─ 菜单（画质 / 倍速 / 选集 / 弹幕 / 字幕 / 更多）一律从右侧滑出侧边面板（见平板）
```
手势沿用旧 `BvPlayerController`：单击显示/隐藏；双击暂停；长按 2 倍速；横滑快进；左半竖滑亮度、右半竖滑音量。

**平板：** 控制层同手机（底部控制区宽 800）。侧边面板：
```
Box at (top 16, end 16, bottom 16) width 340, 圆角 28, background surfaceContainer, padding 22, Column(spacedBy 20)
例：弹幕设置
├─ Row(SpaceBetween)：「弹幕设置」BvType.HeroPhone + Switch(开)
├─ FlowRow BvChip 多选：滚动 顶部 底部 彩色 高级
├─ 4 个滑块，每个 Column(spacedBy 8)：Row(SpaceBetween)(名称 13sp + 数值 13sp onSurfaceVariant 等宽) + Slider
│    不透明度 80% / 显示区域 1/2 屏 / 字号 100% / 速度 适中
│    Slider 用 M3 Expressive 样式：轨道高 16dp，已选段 primary（左圆角 8 右 3），未选段 secondaryContainer（左 3 右 8），手柄 4×36dp primary
└─ Row(SpaceBetween)：「智能防挡（人像蒙版）」14sp + Switch
```
其他菜单同样的面板：画质（单选列表：4K 超清 / 1080P 高码率 / 1080P 高清 / 720P …）、编码、音质、倍速（0.5–3.0 滑块 + 常用值标签）、选集（分 P 列表）、字幕（轨道单选 + 字号/背景/底部边距滑块）、更多（播放模式：单集 / 顺序 / 单集循环 / 列表循环；画面比例）。内容来自旧 `DashMenu`、`DanmakuMenu`、`SpeedMenu`、`VideoListMenu`、`MoreMenu` 和电视 `MenuController` 的合并。

**电视：**
```
Box(fillMaxSize, Black)
├─ 视频 + 弹幕 + 字幕
├─ 控制层（任意方向键/确认键后出现，3 秒后淡出；按键逻辑照抄旧 player/tv VideoPlayerController 的表格）：
│   CinemaScrim
│   左上 at (48, 27)：「P{n} {分P名}」BvType.PanelTitle 白 + 「{视频标题} · {UP}」12sp 白 α0.8
│   底部 at (start 48, end 340 —— 菜单打开时；菜单关闭时 end 48, bottom 27) Column(spacedBy 10, 白)：
│     Row(SpaceBetween) 12sp SemiBold 等宽：当前时间 / 总时长
│     WavyProgress
│     Row(spacedBy 10, focusRestorer)：44dp 图标按钮(FilledTonal) × 6：暂停 | 下一集 | 选集 | 评论 | 字幕 | 设置
└─ 菜单面板（长按确认或菜单键打开）at (top 16, end 16, bottom 16) width 300 圆角 28 background surfaceContainer：
    Row：
    ├─ Column(width 96, background surfaceContainerHigh, padding(vertical 16, horizontal 8), spacedBy 4)：分类 画面 / 弹幕 / 字幕 / 其他（BvListItem 居中 13sp，选中 secondaryContainer）
    └─ Column(weight 1, padding(vertical 16, horizontal 10), spacedBy 2)：
         Eyebrow「分辨率」(padding 0×12 下 6) + 选项 BvListItem（选中显示 ✓ 并 secondaryContainer）…
         Eyebrow「编码」+ 选项 …
```
菜单内容沿用旧 `MenuController`：画面（分辨率、编码、画面比例、倍速、音质）、弹幕（类型开关、大小、透明度、区域、蒙版）、字幕（轨道、字号、背景透明度、底部边距）、其他（播放模式）。

### 9.5 搜索（`searchPhone` / `searchTablet` / `searchTv`）

**数据：** `SearchInputViewModel`（联想、热搜、历史）、`SearchResultViewModel`（结果；**四类都要做**：视频、番剧、影视、UP 主。旧手机版只做了视频，电视版 `SearchResultScreen` 四类都有，照抄电视版的数据逻辑）。

**手机：**
```
Column(fillMaxSize)
├─ Row(padding(top = statusBar + 6, start 12, end 12, bottom 8), spacedBy 6)：返回 IconButton + BvSearchField(weight 1, 输入状态背景 surfaceContainerHighest, 自动弹键盘)
├─ 有输入时：联想列表，每项 BvListItem(padding 10×16)：搜索图标 + 文字（与输入匹配的部分加粗）
└─ 无输入时：Column(padding(12, 16), spacedBy 12)
    ├─ SectionHeader「搜索历史」+「清除」
    ├─ FlowRow(spacedBy 8) BvChip（未选中样式）历史词
    ├─ SectionHeader「大家都在搜」(top 6)
    └─ 2 列网格(水平 12, 垂直 14)：Row(spacedBy 12)：RankBadge + 热搜词 13sp 1 行
提交后：顶部同一搜索框 + BvChip 分类行（综合 视频 番剧 影视 UP 主）+ 结果列表（视频用相关视频行样式；番剧/影视用横向卡片：海报 96 宽 + 标题 + Meta + Button「立即观看」；UP 主用 BvListItem + 关注按钮）
```

**平板：**
```
Row
├─ TabletRail（不选中任何项）
└─ Column(weight 1, padding(start 8, end 28, top 20), spacedBy 18)
    ├─ Row(spacedBy 16)：BvSearchField(width 560, 背景 surfaceContainerHighest) + BvChip：综合(选中) 视频 番剧 影视 UP 主
    ├─ 「综合」结果第一块：最匹配的 UP 主卡 Row(padding 16, 圆角 24, background surfaceContainer, spacedBy 16)：
    │    BvAvatar(72) + Column(weight 1)(名字 16sp Bold + Meta「粉丝 · 视频数 · 签名」) + Button「+ 关注」+ 3 个最新视频封面(120 宽 16:10 圆角 16)
    ├─ SectionHeader「视频」+「按播放量 ›」（点击切换排序，沿用旧搜索结果的排序参数）
    ├─ 4 列网格 VideoCard(showStats = true)
    ├─ SectionHeader「影视」
    └─ Row(spacedBy 16) 影视卡 width 420：Row(padding 12, 圆角 20, background surfaceContainerLow, spacedBy 14)：PosterCard(width 96) + Column(spacedBy 6)(标题 Bold + Meta + Button「▶ 立即观看」高 34)
```

**电视：**
```
Box
├─ TvRail（选中「搜索」）
├─ 左列 at (left 96, top 27) width 300 Column(spacedBy 12)：
│   ├─ BvSearchField(高 52, 显示已输入字母 16sp + primary 光标「|」)
│   ├─ 键盘网格 6 列 spacedBy 6：A–Z、1–9、0，每键高 34 圆角 10 背景 surfaceContainerHigh 14sp SemiBold 等宽，bvFocus —— 初始焦点在「A」
│   ├─ 3 列：空格 / 删除 / 清空（同样式）
│   └─ FilledTonalButton「🎤 按住遥控器语音键说话」（左对齐，图标用 Mic），点击启动语音识别
├─ 中列 at (left 430, top 27) width 190：Eyebrow「联想」(padding 4×12 下 8) + BvListItem × N（13sp Medium，可聚焦）
├─ 右列 at (left 650, top 27) width 262 Column(spacedBy 12)：Eyebrow「热搜」+ 6 行(RankBadge + 词)
└─ 结果行 at (left 430, top 330)：SectionHeader「“{关键词}”的结果」15sp + TvRowPivot { LazyRow(spacedBy 16, padding top 12 bottom 12, end 48) VideoCard(width 180, showMeta = false) }
     输入变化 500ms 防抖后刷新结果；按 ↓ 进入结果行；在结果行按确认进入详情；按菜单键打开「完整结果」页（同平板布局的电视版：4 列网格 + 顶部分类标签）
```
电视键盘输入的是拼音首字母，B 站接口支持拼音联想，直接把字母串作为关键词。

### 9.6 影视（`pgcPhone` / `pgcTablet` / `pgcTv`）

**数据：** `PgcAnimeViewModel`、`PgcGuoChuangViewModel`、`PgcMovieViewModel`、`PgcTvViewModel`、`PgcDocumentaryViewModel`、`PgcVarietyViewModel`（旧：`app/tv/.../screens/main/PgcContent.kt` 与 `PgcScaffold`）；时间表 `SeasonRepository.getTimeline`（旧：`AnimeTimelineScreen`）；索引 `PgcIndexRoute`；我的追番 `FollowingSeasonRoute`。

**手机：**
```
Column
├─ Row(padding(top = statusBar + 6, start 20, end 16, bottom 6), SpaceBetween)：「影视」BvType.PageTitle + 搜索 IconButton
├─ LazyRow(padding 水平 16, spacedBy 8) BvChip：番剧(选中) 国创 电影 电视剧 纪录片 综艺
└─ LazyColumn(bottom 留导航栏)
    ├─ 海报轮播 padding(top 14, 水平 16) 高 290：HorizontalMultiBrowseCarousel(preferredItemWidth = 220.dp, itemSpacing = 8.dp, minSmallItemWidth = 34.dp, maxSmallItemWidth = 90.dp)
    │    每项竖海报 3:4 Crop，maskClip 圆角 28；大项显示 CinemaScrim + 左下(14,14)：标题 19sp ExtraBold 白 + 「更新至第 N 话 · 每周五 23:00」12sp 白 α0.85
    ├─ SectionHeader「新番时间表」+「完整 ›」→ PgcTimelineRoute，padding(16, 16, 8)
    ├─ Row(水平 16, spacedBy 6)：7 个等宽按钮「一 二 三 四 五 六 日」高 44 13sp SemiBold：
    │    今天 = 背景 primary、文字 onPrimary、圆角 16；其他 = 背景 surfaceContainerHigh、全圆角；点击切换下方列表（选中项变为圆角 16 的 primary，fastSpatialSpec）
    ├─ 当日更新列表 Column(padding 12×16, spacedBy 10)，每项 Row(spacedBy 12, CenterVertically)：
    │    时间「23:00」width 44 Bold primary 等宽 | 海报 44×58 圆角 10 | Column(weight 1)(标题 14sp Bold + Meta「第 N 话」) | FilledTonalButton「追番」高 32 内边距 12 12sp（已追显示「已追」）
    └─ 各分区 feed 行（沿用 PgcScaffold 的 feed 数据）：SectionHeader + LazyRow PosterCard(width 112)
```
切换顶部标签时，换成对应 ViewModel 的数据；电影/电视剧/纪录片/综艺没有时间表，隐藏时间表区块。

**平板：**
```
Row
├─ TabletRail（选中「影视」）
└─ Column(weight 1, padding(start 8, end 28, top 20), spacedBy 18)
    ├─ Row(spacedBy 20, CenterVertically)：「影视」BvType.PageTitleLarge + BvChip × 6 + Spacer(weight 1) + FilledTonalButton「⚙ 索引」+ FilledTonalButton「★ 我的追番」
    ├─ Row(spacedBy 20, 高 320)：
    │   ├─ 横幅 weight 1 圆角 28：横图 Crop + CinemaScrim + 左下(28,24) Column(spacedBy 8)：Eyebrow「本季热播 · {评分} 分」白 + 标题 BvType.HeroTablet 白 + Row(spacedBy 10)(Button「▶ 看第 N 话」+ FilledTonalButton「+ 追番」)
    │   └─ 侧卡 width 300 圆角 28 background surfaceContainer padding 18 Column(spacedBy 12)：SectionHeader「今天更新」+「周X」，3 行（海报 44×58 + 标题 13sp Bold + Meta「HH:mm 更新」）
    ├─ SectionHeader「热门番剧」
    └─ 6 列网格 PosterCard
```

**电视：** 整页 `CoverTheme(当前精选封面, forceDark = true)`。
```
Box
├─ TvBackdrop(精选横图)
├─ TvRail（选中「影视」）
├─ 顶栏 (left 96, top 24, right 48)：BvChip × 6（聚焦即选中）+ 时钟
├─ 信息区 at (left 96, top 78) width 420 Column(spacedBy 10)：
│   Eyebrow「本季热播 · 评分 {x}」｜ 标题 BvType.Display 2 行 ｜ Meta 12sp「更新至第 N 话 · 每周五 23:00 · {风格标签}」
│   Row(spacedBy 12, top 6)：Button「▶ 看第 N 话」高 52（初始焦点）+ FilledTonalButton「+ 追番」高 52
│   Row(spacedBy 8, top 4)：BvChip「时间表」「索引」「我的追番」（可聚焦，确认进入对应页面）
└─ 行 at (left 96, top 318) TvColumnPivot { LazyColumn }：SectionHeader「热门番剧」+ TvRowPivot { LazyRow(spacedBy 16, padding top 12, end 48) PosterCard(width 112) }，下面接各 feed 行
```

### 9.7 动态（`dynPhone` / `dynTablet` / `dynTv`）

**数据：** `DynamicViewModel`、`DynamicDetailViewModel`、`CommentViewModel`（旧：`app/mobile/.../screen/home/DynamicScreen.kt`、`DynamicDetailScreen.kt`、`component/home/dynamic/DynamicItem.kt` 支持的所有类型：视频、图文、文字、转发、直播、PGC）。顶部 UP 主头像行用关注列表（`FollowViewModel` 前 20 个，按最近更新排序；接口不支持排序就按原顺序）。

**动态卡 `DynamicCard`：** Column(padding 14, 圆角 24, background surfaceContainerLow（选中时 secondaryContainer）, spacedBy 10)：
- 头部 Row(spacedBy 10)：BvAvatar(32) + Column(名字 13sp Bold + Meta「{时间} · 投稿了视频 / 动态」)
- 视频类：标题 13sp 2 行 + 内嵌卡 Row(padding 8, 圆角 16, background surfaceContainerHigh, spacedBy 10)(封面 120 宽 16:10 圆角 10 带时长 + Column(Meta「{播放}播放」「{弹幕}弹幕」))
- 图文类：正文 13sp 行高 1.55 + 3 列方图网格(spacedBy 4, 圆角 10)，点图片进入现有图片查看器
- 底部 Row(SpaceAround) 12sp onSurfaceVariant：转发 / 评论 {N} / 点赞 {N}（图标 14dp），点赞可用（沿用旧逻辑，若接口不存在则仅显示）

**手机：** Column：「动态」BvType.PageTitle(padding top=statusBar+6, start 20, bottom 10) → UP 主头像行 LazyRow(padding 水平 16 下 12, spacedBy 14)：每项 Column(spacedBy 4, 11sp)：BvAvatar(48，奇数项用 Sunny 形状) + 名字；第一项「全部」外圈 2dp primary（选中的筛选项都加这个外圈）；点头像 = 只看该 UP 主动态 → LazyColumn(水平 12, spacedBy 12) DynamicCard。点卡片 → 视频类进 `VideoRoute`，其他进 `DynamicDetailRoute`。

**平板：** Row：TabletRail | 左列 width 470 Column(padding(top 20, 水平 8), spacedBy 14)：标题 PageTitleLarge(start 8) + 头像行 + LazyColumn DynamicCard（选中项 secondaryContainer）| 右栏 weight 1 margin(16, 20, 16, 12) 圆角 28 background surfaceContainerLow padding 22 Column(spacedBy 14)：头部 Row(BvAvatar 44 + 名字/时间) + 媒体区高 300 圆角 24（视频：封面 + 居中 64dp 播放按钮 圆角 22 primary；点击进入 VideoRoute）+ 标题 16sp Bold + BvTabRow「评论 {N}」「转发」+ CommentItem 列表。

**电视：** TvRail | 顶部 (left 96, top 27, right 48)：Row(SpaceBetween)(「动态」BvType.PageTitle + BvChip 全部(选中) 视频 图文) + 下方头像行 | 内容 at (left 96, top 170) TvColumnPivot { LazyColumn }：按日期分组（今天 / 昨天 / 更早），每组一个 SectionHeader + TvRowPivot { LazyRow(spacedBy 18, end 48) 动态电视卡 width 240：Column(spacedBy 7)：Row(BvAvatar 26 + 名字 12sp SemiBold + Meta「· N 小时前」) + 封面 16:10（焦点效果同 VideoCard）+ 标题 2 行 }；第一张卡初始焦点。

### 9.8 我的（`mePhone` / `meTablet` / `meTv`）

**数据：** `UserViewModel`（头像、昵称、等级、关注/粉丝/硬币）、`HistoryViewModel`、`FavoriteViewModel`、`ToViewViewModel`、`FollowingSeasonViewModel`、`FollowViewModel`、`UserSwitchViewModel`。未登录时头像区换成：Row(BvAvatar 72 占位 +「登录后同步历史、收藏和追番」+ Button「登录」→ LoginRoute)。

**资料头 `ProfileHeader`：** Row(spacedBy 16)：BvAvatar(72) + Column(spacedBy 6)：Row(spacedBy 8)(昵称 BvType.PanelTitle + 等级徽章 30dp Sunny 形状 secondaryContainer 数字 11sp) + Meta 12sp「关注 {n} · 粉丝 {n} · 硬币 {n}」。

**手机：** Column(padding(top=statusBar+16, 水平 20), spacedBy 18)：ProfileHeader → 2×2 ColorTile(spacedBy 10)：历史记录(Primary,「今天看了 N 个」) 收藏夹(Tertiary,「N 个收藏夹」) 稍后再看(Secondary,「N 个待看」) 追番追剧(Primary,「N 部更新」) → SectionHeader「继续观看」+「全部」→ LazyRow VideoCard(width 150, progress, showMeta=false) → 列表卡 Column(圆角 24, background surfaceContainerLow, padding 6)：BvListItem(图标容器背景 surfaceContainerHighest)「关注的 UP 主 / {n} 位」「账号与用户锁 / {n} 个账号」「设置 / 主题、播放、弹幕」。

**平板：** Row：TabletRail | 左列 width 340 Column(padding(24, 12), spacedBy 18)：ProfileHeader + BvListItem × 6（历史记录、收藏夹、稍后再看、追番追剧、关注、账号与用户锁；选中项 secondaryContainer 且图标容器 primary 背景 onPrimary 图标）| 右栏 margin(16, 20, 16, 4) 圆角 28 surfaceContainerLow padding 24 Column(spacedBy 16)：Row(SpaceBetween)(BvType.PaneTitle「历史记录」+ BvChip 全部 视频 番剧) + 按日期分组（今天 / 昨天 / 更早）的 4 列 VideoCard 网格（progress）。右栏内容随左侧选择切换为收藏夹（顶部收藏夹 BvChip 切换）、稍后再看、追番（PosterCard 6 列）、关注（UP 主 2 列列表）、账号管理。

**电视：** TvRail | 顶部 Row at (left 96, top 27, right 48, spacedBy 20)：ProfileHeader + Spacer + FilledTonalButton「切换账号」+ FilledTonalButton「用户锁」| 4 个 ColorTile at (left 96, right 48, top 118) 4 列 spacedBy 14，各包一层 `bvFocus(scale = 1.05f, corner = 24.dp, glow = 16.dp)` 且可点击 —— 第一个初始焦点 | 「继续观看」行 at (left 96, top 300)：SectionHeader + LazyRow VideoCard(width 190, progress, showMeta=false)。

### 9.9 设置（`setPhone` / `setTablet` / `setTv`）

**分类（顺序、图标、颜色固定）：**

| # | 标题 | 副标题 | 图标 | 图标容器颜色 | 内容来源 |
|---|---|---|---|---|---|
| 1 | 外观与主题 | 种子色、配色风格、深色模式 | Palette | primaryContainer | 新增（见下） |
| 2 | 播放 | 默认画质、编码、音质、倍速 | PlayArrow | tertiaryContainer | 旧手机 `PlayContent` + 电视的分辨率/编码/音频/倍速/播放模式/FFmpeg 音频 |
| 3 | 弹幕 | 类型、透明度、区域、防挡 | ChatBubble | secondaryContainer | Prefs 里弹幕相关键 |
| 4 | 字幕 | 字号、背景、底部边距 | Subtitles | primaryContainer | Prefs 里字幕相关键 |
| 5 | 网络与代理 | 代理服务器、官方 CDN、测速 | Wifi | tertiaryContainer | 电视 `NetworkSetting` + API 类型(Web/App) + 测速入口 |
| 6 | 账号与安全 | 多账号、用户锁、无痕模式 | Shield | secondaryContainer | 账号切换、用户锁、无痕模式 |
| 7 | 存储 | 图片缓存、日志 | Storage | primaryContainer | 电视 `StorageSetting` + 日志入口 |
| 8 | 关于 | 版本、更新、解码器信息 | Info | tertiaryContainer | 版本/更新/GitHub/alpha 更新/Firebase/FPS/设备信息/解码器信息/崩溃测试 |

分类项 = BvListItem，图标容器裁剪为 `MaterialShapes.Cookie9Sided.toShape()`。

**「外观与主题」详情（三端相同内容，Column(spacedBy 22；电视 16)）：**
1. 「主题来源」SegmentedRow：系统壁纸 / 自选种子色 / 跟随内容（Android 12 以下隐藏「系统壁纸」）
2. 「种子色」Row(spacedBy 10)：6 个预设色圆点 32dp（`BvSeeds.presets`）；当前选中的那个 36dp、圆角 30%、旋转 45°、外加 2dp 背景色间隙 + 2dp onSurface 外圈（`defaultSpatialSpec` 动画）；最后一个「+」tonal 图标按钮打开自定义取色对话框（色相滑块 0–360 + 预览，HSV 饱和度 0.7 明度 0.95）
3. 「配色风格」FlowRow BvChip：TonalSpot Vibrant(默认选中) Expressive Fidelity Content Monochrome
4. 「对比度」Row(SpaceBetween)(标签 + 当前值「标准/中/高」) + Slider(3 档：0 / 0.5 / 1.0)
5. 「深色模式」SegmentedRow：跟随系统 / 浅色 / 深色 / 影院纯黑（电视上整项隐藏，电视永远深色）
6. Row(SpaceBetween)：Column(「详情页使用封面颜色」14sp Bold + Meta「详情、播放器、刷刷随视频变色」) + Switch
7. Row(SpaceBetween)：Column(「减少动效」+ Meta「关闭形状变形和弹簧回弹」) + Switch
8. （仅电视）「界面缩放（电视）」Row(标签 + 「960dp 基准 · {百分比}」) + Slider（写 `Prefs.density`，沿用旧电视 UI 设置的范围）
每一项修改立即调用 `Prefs.setXxx`，整个 App 颜色带动画实时变化。

**手机：** 列表页：标题「设置」BvType.PageTitle(padding top=statusBar+6, start 20, bottom 12) + Column(padding 水平 8, spacedBy 2) 8 个分类；点分类 → `SettingsRoute(category)` 进入详情页（LargeFlexibleTopAppBar 标题为分类名 + 上面的内容，水平内边距 16）。

**平板：** Row(padding(20, 24), spacedBy 20)：左列 width 380（顶部 Row：返回 IconButton + 「设置」BvType.PaneTitle，padding 0×8 下 12；分类列表，选中项 secondaryContainer）| 右栏 weight 1 圆角 28 background surfaceContainerLow padding(26, 30) Column(spacedBy 22)：BvType.PaneTitle 分类名 + 内容（可滚动）。默认选中「外观与主题」。

**电视：** Row(padding(27, 48), spacedBy 24)：左列 width 300（「设置」BvType.PaneTitle padding 0×12 下 12 + 分类 BvListItem 可聚焦，初始焦点在选中项）| 右栏 weight 1 圆角 28 background surfaceContainer padding(22, 26) Column(spacedBy 16)：BvType.PanelTitle 分类名 + 内容。在右栏按 ← 回到左列；在左列按 → 进右栏第一项；左列聚焦即切换右栏内容。

### 9.10 登录（`loginPhone` / `loginTablet` / `loginTv`）

**数据：** `SmsLoginViewModel`（含 Geetest 验证，沿用）、`AppQrLoginViewModel`；二维码用现有 `m3qrcode` 的 Material 形状二维码组件，放在白底圆角 24、内边距 14 的容器里。

**手机：** Column(padding(top=statusBar+40, 水平 24), spacedBy 18)：
1. 应用图标 64dp（裁剪 Cookie9）
2. 「登录哔哩哔哩」28sp ExtraBold 行高 1.2
3. 「登录后同步历史、收藏和追番，三台设备进度一致。」14sp onSurfaceVariant
4. 手机号输入框、验证码输入框：TextField（filled 样式：上圆角 16 下圆角 4，背景 surfaceContainerHighest，底部 2dp primary 指示线，标签 11sp primary，输入 16sp 等宽）；验证码框尾部文字按钮「获取验证码 / {n} 秒后重发」12sp SemiBold primary
5. Button「登录」全宽 高 52 ButtonLarge
6. Row：分隔线 +「或」12sp onSurfaceVariant + 分隔线
7. FilledTonalButton「在另一台设备上扫码」全宽 高 52（二维码图标）→ 弹对话框显示二维码（AppQrLoginViewModel）
8. OutlinedButton「导入电视上已登录的账号」全宽 → 说明对话框（电视端「账号与安全」里生成的 `bugvideo://qrtoken` 二维码，用手机扫码导入，沿用现有流程）

**平板 / 电视（同一布局，括号内为电视数值）：** Row(居中, spacedBy 80 (56), 水平内边距 48)：
- 左 Column(width 340 (260), CenterHorizontally, spacedBy 16)：二维码 300 (230) + 「用哔哩哔哩 App 扫码登录」17sp (15sp) Bold + Meta 12sp「二维码 3 分钟后刷新 · 已扫码请在手机上确认」
- 右 Column(width 400 (320), spacedBy 16 (12))：「登录哔哩哔哩」32sp (26sp) ExtraBold；3 个步骤行 Row(spacedBy 12)(编号头像 30dp：1、3 用 Cookie9 primaryContainer，2 用 Sunny secondaryContainer + 14sp 文字)：「打开哔哩哔哩 App，点右上角扫一扫」「对准左侧二维码」「在手机上点「确认登录」」；1dp 分隔线；FilledTonalButton「📱 改用手机号和验证码」（电视初始焦点）；OutlinedButton「👤 切换到已保存的账号」。图标用 Material 图标，不要用 emoji。

### 9.11 原型里没有画的页面（统一模板）

| 页面 | 路由 | 数据（照抄旧界面） |
|---|---|---|
| UP 主空间 | SpaceRoute | `UserSpaceViewModel`（旧手机 UserSpaceScreen / 电视 UpSpaceScreen） |
| Tag 页 | TagRoute | `TagViewModel` |
| 历史 / 收藏 / 稍后再看 / 追番 / 关注 | Me 子路由 | 对应 ViewModel |
| 新番时间表 | PgcTimelineRoute | `SeasonRepository.getTimeline` |
| 番剧索引 | PgcIndexRoute | `PgcIndexViewModel` + 筛选对话框 |
| 账号管理 / 用户锁 | AccountsRoute / UserLockRoute | `UserSwitchViewModel` / 旧 `UserLockSettingsScreen`（方向键序列密码，电视专用；手机上用 4 位数字密码输入，存同一字段） |
| 日志 / 解码器信息 / 测速 | Logs / MediaCodec / SpeedTest | 旧电视对应界面 |
| 番剧详情 | SeasonRoute | `SeasonViewModel`（旧 SeasonInfoScreen），布局完全套用 9.3 视频详情：分 P 行换成剧集行（卡片标题「第 N 话 {标题}」），多季时在标题下加 BvChip 季度选择行 |
| 动态详情（手机） | DynamicDetailRoute | `DynamicDetailViewModel`，布局 = 9.7 平板右栏内容全屏化 |

模板：
- **手机：** `LargeFlexibleTopAppBar`（滚动收起为单行，标题 = 页面名）+ 内容（视频 2 列网格 / 海报 3 列网格 / 列表），水平内边距 16，间距 12。
- **平板：** 外壳不显示导航轨时左上返回按钮 + BvType.PageTitleLarge 标题（padding 20/24）+ `Adaptive(180.dp)` 网格（海报 `Adaptive(120.dp)`），间距 16。
- **电视：** 标题 BvType.PageTitle at (48, 27)；内容 `LazyVerticalGrid(Fixed(4)` 视频 / `Fixed(6)` 海报`)`，contentPadding(start 48, end 48, top 80, bottom 27)，间距 18，`focusRestorer`，第一项初始焦点。

---

## 10. 刷刷的数据与播放

### 10.1 `StoryRepository`（在 `bili-api` 新增）

1. 仿照 `RecommendVideoRepository` 调用 App 端推荐流 `/x/v2/feed/index` 的方式（同样的 App 签名、`access_key`、公共参数），新增请求 `GET https://app.bilibili.com/x/v2/feed/index/story`。参数：`aid`（上一页最后一个视频的 aid，首次为空或传起始视频 aid）、`fnval=272`、`fnver=0`、`fourk=1`、`qn=80`、`pull`（首次 true，之后 false）、`display_id`（从 1 递增）。
2. **先把原始 JSON 打到日志里看一遍**，再写 `@Serializable` 数据类（`Json { ignoreUnknownKeys = true }`）。需要的字段：每项的 aid、cid、标题、封面、UP 主（名字、mid、头像）、点赞/投币/收藏/评论数、时长、宽高或 `dimension`（判断横竖屏）、标签。字段名以实际返回为准。
3. 未登录、接口报错、返回为空时：**回退到推荐流**（`RecommendVideoRepository`），把竖屏视频排在前面（推荐数据里没有宽高时全部当横屏处理）。
4. 新增 `ShortsItem` 数据类放在 `app/shared/entity/`，两种数据源都转换成它。

### 10.2 `ShortsViewModel`（在 `app/shared/viewmodel/` 新增，`@KoinViewModel`）

- `items: SnapshotStateList<ShortsItem>`，`loadMore()`，`currentIndex`。
- 进入时如果有 `startAid`，先用该视频（取视频详情转换为 ShortsItem）作为第一条，然后接 story 流。
- 剩余不足 3 条时自动 `loadMore()`。
- 点赞/投币/收藏/关注调用与视频详情相同的仓库方法。

### 10.3 稍后再看的添加 / 删除

`ToViewRepository` 目前只读。在 bili-api 里新增：`POST https://api.bilibili.com/x/v2/history/toview/add`（表单参数 `aid`、`csrf` = bili_jct）和 `POST https://api.bilibili.com/x/v2/history/toview/del`（`aid`、`csrf`）。用现有 `BiliHttpApi` 的 Web 登录 cookie 发送。

### 10.4 播放器池

- `ShortsPlayerPool` 持有 **3 个**播放器实例（复用 `player:core` 的 `ExoMediaPlayer`）：上一条、当前、下一条。
- 翻到第 i 页：i 用中间实例播放；i+1 用下一实例 `prepare()` 但不播放（静音、暂停）；i−1 保留暂停。超出范围的实例 `stop()` 后复用。
- 播放地址：把 `VideoPlayerV3ViewModel` 里根据 aid/cid 取播放地址、选择画质/编码的代码**抽成**一个可复用的 `PlayUrlResolver`（放 `app/shared`），刷刷和播放器都用它。不要复制粘贴两份。
- 页面停稳（`pagerState.isScrollInProgress == false`）150ms 后才开始播放当前页。
- 移动网络下画质上限 720P、只预准备下一条；Wi-Fi 和电视用用户设置的默认画质。
- 刷刷默认关闭弹幕。
- 心跳 / 历史上报调用与播放器相同的上报方法。
- 页面不可见（切走、退到后台）时全部暂停并释放下一条之外的实例。

---

## 11. 播放器合并：新建 `player:ui`

1. `settings.gradle.kts` 加 `include(":player:ui")`；`player/ui/build.gradle.kts` 复制 `player/mobile/build.gradle.kts`，namespace 改为 `dev.aaa1115910.bv.player.ui`；`dependencies` 取 `player/mobile` 和 `player/tv` 两个文件 `dependencies` 的并集，**去掉** `androidx.compose.tv.foundation` 和 `androidx.compose.tv.material` 两行。
2. 把 `player/mobile/src/main/kotlin/.../player/mobile/` 和 `player/tv/src/main/kotlin/.../player/tv/` 的代码复制进 `player/ui/src/main/kotlin/dev/aaa1115910/bv/player/ui/`，分别放在 `touch/` 和 `tv/` 子包。
3. 新的入口 `BvPlayerScreen(...)`：参数是两个旧 `BvPlayer` 参数的并集；内部 `if (LocalDeviceProfile.current.isTv) TvController(...) else TouchController(...)`。`LocalDeviceProfile` 在 `app:ui`，所以 `player:ui` 改为接收一个 `isTv: Boolean` 参数，由 `app:ui` 传入（避免循环依赖）。
4. 把 `tv/` 子包里所有 `androidx.tv.material3.*` 换掉（规则见第 12 节），把两个控制层的样式改成第 9.4 节规格。
5. 菜单数据合并成一份：`PlayerMenuState`（分辨率、编码、音质、倍速、画面比例、弹幕类型/大小/透明度/区域/蒙版、字幕轨道/字号/背景/边距、播放模式），触摸端侧边面板和电视端三栏面板都读写它。
6. 手机端补上 `BottomSubtitle` 和字幕菜单，打开心跳上报。
7. `app:ui` 依赖 `player:ui`，`PlayerRoute` 的页面调用 `BvPlayerScreen`。

---

## 12. 去掉 androidx.tv

迁移每个界面时按下表替换（统计的是现有代码里的 import 次数，用来估工作量）：

| androidx.tv.material3 | 次数 | 替换 |
|---|---|---|
| `Text` `Icon` `MaterialTheme` `LocalContentColor` `ProvideTextStyle` `LocalTextStyle` | 157 | `androidx.compose.material3` 同名 API，只改 import |
| `Surface` `ClickableSurfaceDefaults` `SurfaceDefaults` `Border` `Glow` | 53 | `androidx.compose.material3.Surface(onClick = …)` + `Modifier.bvFocus(…)`；原来的 `scale` / `border` / `glow` 参数一律删除，由 bvFocus 统一提供 |
| `Button` `OutlinedButton` `IconButton` `ButtonDefaults` `OutlinedButtonDefaults` | 29 | material3 同名按钮 + `bvFocus()` + 第 6.6 节聚焦变色 |
| `TabRow` `Tab` `TabRowScope` | 11 | `BvTabRow`（聚焦即选中） |
| `ListItem` `DenseListItem` `ListItemDefaults` | 11 | `BvListItem` |
| `NavigationDrawer` `NavigationDrawerItem` `DrawerValue` `rememberDrawerState` `NavigationDrawerScope` | 8 | `TvRail`（第 8.6 节） |
| `Typography` `darkColorScheme` `lightColorScheme` | 6 | 删除，由 `BvTheme` 提供 |
| `Switch` `Checkbox` `RadioButton` `FilterChip` `SuggestionChip` `Card` `CardDefaults` | 15 | material3 同名组件 + `bvFocus()`；Chip 用 `BvChip`，Card 用 `VideoCard` / `PosterCard` |
| `CarouselDefaults.IndicatorRow` | 1 | 自绘：一排 6dp 圆点，当前项变成 18×6 的胶囊，颜色 primary / onSurfaceVariant α0.4 |
| `ExperimentalTvMaterial3Api` | 6 | 删除 |

旧代码里的焦点写法也要换：
- `tabMoved` 之类的延迟标志 → 删除，用 `focusProperties { onEnter / onExit }` 和 `focusRestorer()`。
- 「最后一张卡吞掉 → 键」→ 删除，行尾自然停止（`focusGroup` 内找不到下一个就不动）。
- `Modifier.focusedBorder` / `focusedScale`（`app/shared/.../util/ModifierExtends.kt`）→ `bvFocus`。
- 自己写的 `onPreviewKeyEvent` → `onBvKey`。

最后一步（第 13 节步骤 9）：从 `app/shared/build.gradle.kts` 删除 `api(androidx.compose.tv.foundation)`、`api(androidx.compose.tv.material)`；从 `gradle/androidx.versions.toml` 删除 `compose-tv`、`compose-tv-foundation` 版本和库定义；全局搜索 `androidx.tv` 必须为 0 条结果。`app/shared/.../ui/theme/Theme.kt` 里的旧 `BVTheme` 和 `app/shared` 里另外 5 个引用 tv 的文件（`Typography`、`ModifierExtends`、`UpdateDialog`、`FpsMonitor`、`DevelopingTip`）一并改写或删除。

---

## 13. 实施顺序（严格按顺序；每步结束必须编译通过并自测）

| 步骤 | 内容 | 完成标准 |
|---|---|---|
| 1 | 第 5 节建模块、加 material-kolor；第 6 节全部设计系统代码；第 7 节全部公共组件 | `:app:ui:compileDebugKotlin` 通过 |
| 2 | 第 8 节：新 `MainActivity`、清单、路由、`AppShell`、三种导航；一级页面先放占位文字 | 手机、平板、电视三个模拟器都能启动并切换 5 个一级页面；电视导航轨聚焦展开 196dp |
| 3 | 9.1 首页三端 | 与设计页「首页」三端截图对比，结构一致 |
| 4 | 9.3 视频详情 + 9.4 播放器 + 第 11 节 `player:ui` | 能从首页点进详情并播放；手机能全屏；电视菜单能用；字幕在手机上显示 |
| 5 | 9.5 搜索 | 四类结果都能显示 |
| 6 | 第 10 节 + 9.2 刷刷 | 三端能连续刷，翻页流畅，每条颜色不同；电视 ↑↓ 换条 |
| 7 | 9.6 影视、9.7 动态、9.8 我的 | 三端对比通过 |
| 8 | 9.9 设置、9.10 登录、9.11 其余页面、用户锁、深链 | 设置里改种子色，整个 App 实时变色 |
| 9 | 删除旧代码：`app/mobile`、`app/tv`、`player/mobile`、`player/tv` 四个模块（`settings.gradle.kts` 去掉 include，`app/build.gradle.kts` 只依赖 `:app:ui`），第 12 节删 androidx.tv 依赖 | 全局搜 `androidx.tv` 为 0；`:app:assembleDefaultDebug` 通过；三端回归 |

每完成一步，用 git 提交一次，提交信息格式：`feat(redesign): 步骤 N - {内容}`。

---

## 14. 验收方法

### 14.1 三个模拟器（尺寸必须与原型画布一致）

```bash
# 手机：360×780dp
adb -s <手机模拟器> shell wm size 1080x2340
adb -s <手机模拟器> shell wm density 480

# 平板：1196×800dp（Expanded，收起导航轨，对应原型 1200×800 画布）
adb -s <平板模拟器> shell wm size 2392x1600
adb -s <平板模拟器> shell wm density 320

# 电视：用 Android TV 1080p 模拟器（1920×1080，density 320 → 960×540dp）
# App 的电视密度偏好默认 widthPx/960，保持默认

# 截图
adb -s <设备> exec-out screencap -p > shot_<界面>_<设备>.png
```

手机播放器横屏验收：把手机模拟器旋转为横屏（780×360dp）。

### 14.2 对比

1. 用 Chrome 打开 `docs/design/bv-expressive-redesign.html`，到「06 · 界面」，选界面和设备。
2. 把模拟器截图和原型放在一起看，逐项检查：
   - 区块的顺序、位置、大小（误差 ≤ 4dp）
   - 圆角（16 / 20 / 24 / 28）、间距（8 / 10 / 12 / 14 / 16 / 18 / 20 / 24）
   - 字号和字重（对照 BvType）
   - 颜色角色（primary / secondaryContainer / surfaceContainer… 用对了没有）。**具体色值不需要与原型一致**：原型用的是 2021 规范的浏览器算法，App 用 material-kolor 的 SPEC_2025，色调会有细微差别。
   - 电视：初始焦点位置、焦点样式（放大 + 外侧描边 + 光晕 + 卡片圆角变化）、行内滚动时焦点停在左侧 15% 处
3. 刷刷和封面取色：确认每条视频的点赞按钮、进度条颜色随封面变化；详情页颜色与首页不同。
4. 设置 → 外观与主题：改种子色、风格、深色模式，全 App 颜色带动画切换。
5. 电视全程只用方向键 + 确认 + 返回 + 菜单键，能走到每一个可操作元素，没有焦点丢失（焦点消失或跳到看不见的位置）。
6. 手机/平板全程触摸操作，界面上**没有**任何焦点放大或描边。

### 14.3 必须通过的检查清单

- [ ] 手机桌面和电视桌面各只有一个 BV 图标，都启动同一个 `MainActivity`
- [ ] 折叠屏展开/折叠、平板分屏拖动窗口时，布局切换且正在播放的视频不中断
- [ ] 平板接蓝牙键盘按方向键，立即出现焦点效果；摸一下屏幕，焦点效果消失
- [ ] 10 个界面 × 3 端与原型一致
- [ ] 刷刷三端可用，横屏视频有氛围光
- [ ] 视频详情：点赞、投币、收藏、稍后再看、关注、分享都能用
- [ ] 手机端能看番剧、分区、稍后再看、字幕；电视端能看评论、动态详情
- [ ] 系统「移除动画」或 App「减少动效」开启后，没有弹簧和形状变形
- [ ] `androidx.tv` 搜索结果为 0，旧四个模块已删除

---

## 15. 遇到问题时

| 情况 | 做法 |
|---|---|
| 本文给的代码编译报错 | 只做**最小修改**让它通过（改 import、补 opt-in、调整参数名），不改变行为和数值；在提交信息里写明改了什么 |
| 某个 M3 Expressive 组件在 1.5.0-alpha03 里不存在或参数不同 | 用基础组件（`Surface`、`Box`、`Row`）按本文数值手写一个外观相同的版本，不要升级 material3 |
| 本文和原型源码数值冲突 | 以**原型源码**（`_src/*.js`、`_src/2-mock.css`）为准 |
| 本文和设计文档（HTML 正文）冲突 | 以**本文**为准（本文已针对当前工具链做过调整） |
| 原型和本文都没写到的细节 | 照搬旧界面里对应功能的行为，样式用最接近的公共组件；不要发明新样式 |
| 接口字段与本文描述不同 | 以接口实际返回为准，先打日志确认 |
| 需要新增依赖 | 停下来，不要加；除 material-kolor 3.0.1 外不允许新增依赖 |
| 编译超时或 Gradle 下载失败 | 检查是否用了第 3 节的 JDK 21 和 `-I mirror-init.gradle` |
| 不确定某个界面该用哪个旧 ViewModel | 在 `app/mobile` 和 `app/tv` 里搜该界面的旧 Screen，看它 `koinViewModel()` 了什么 |
