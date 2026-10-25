# Compose 性能审计与修复记录

日期：2026-10-25  
基准提交：`c1552eb4`  
工作分支：`develop`

## 结论与验证边界

已检查应用界面、共享 Compose 组件、播放器界面与图片预览相关代码，并落实可局部修改的性能修复。保留已有功能、普通播放器的 SurfaceView/HDR 路径，以及基准提交中的全屏切换与双指手势修复。

本记录不是编译通过或性能提升证明。未运行本地 Gradle、应用编译、单元测试、设备测试或性能基准。未提交、未推送、未触发新的 CI。

| 检查 | 结果 |
| --- | --- |
| 改动中的 Kotlin 源码与构建脚本语法解析 | 72 个文件，0 个 PSI 语法错误 |
| 新增常用 API 的定向导入检查 | 未发现缺失导入 |
| 版本目录 TOML 解析 | 通过 |
| Compose 约束扩展、Lifecycle Flow API | 从本机已有依赖二进制核对接口存在 |
| `git diff --check` | 按仓库原有换行配置通过 |
| 新增回归测试 | 保存队列 7 项，日期缓存 2 项；仅编写与静态复查，未运行 |
| Android/Kotlin 类型检查与 Compose 编译器验证 | 未执行 |
| Alpha APK、设备回归、帧耗时对比 | 未执行 |

语法检查使用 Kotlin PSI 解析器，不生成应用或 Kotlin 字节码。它不能发现所有类型、重载、资源、Gradle 配置或 Compose 编译器错误。静态复查也不能替代运行验证。

发布流程停在验证边界：项目禁止本地构建；浏览器联网前置检查曾被停止，没有重试。当前 `develop` 同时是默认分支，提交工具要求默认分支先建分支，而项目要求不新建分支。本次未改变分支或绕过这些约束。

## 已落地的修复

### 状态、绘制与动画

- 播放器画幅、缩放、平移以及电视控制层动画边距延后到测量或放置阶段读取。保持唯一播放器调用点，不因全屏切换释放内核。
- 电视导航轨宽度与内边距在测量阶段读取。背景渐变与头像外轮廓按绘制尺寸缓存。
- FPS 数值和采样循环隔离在覆盖层，避免监控数值更新重新进入整页内容。
- 图片预览透明度在 graphicsLayer 内读取。尺寸派生不再通过多个冗余快照状态来回复制。手势临时计算值改为普通对象字段。
- Shorts 封面只在跨过移除阈值时改变组合结构，渐隐透明度仍由绘制层读取。
- 日期格式器复用，同时以语言和时区失效，保留系统设置改变后的日期输出。

主要入口：

- [BvPlayerScreen](../player/ui/src/main/kotlin/dev/aaa1115910/bv/player/ui/BvPlayerScreen.kt)
- [TvController](../player/ui/src/main/kotlin/dev/aaa1115910/bv/player/ui/tv/TvController.kt)
- [TvRail](../app/ui/src/main/kotlin/dev/aaa1115910/bv/ui/designsystem/nav/TvRail.kt)
- [FpsMonitor](../app/shared/src/main/kotlin/dev/aaa1115910/bv/component/FpsMonitor.kt)
- [ImageComposeOrigin](../app/ui/src/main/kotlin/dev/aaa1115910/bv/ui/imageviewer/viewer/ImageComposeOrigin.kt)

### 列表与布局

- 选集仓库从原地修改的普通可变列表改为完整、可观察的列表快照。全部已知发布调用改为一次替换，选集标签缓存能够观察更新。
- TV 首页按实际进入组合的条目转换卡片，不再在分页变化时把整个列表映射为卡片。
- 历史与稍后再看按条目计算展示进度。动态日期分组改为单次分组。
- 收藏夹、播放器选集、电视菜单和日志列表补充键或内容类型。电视菜单允许接口返回重复音轨/编码，不会因为相同值键崩溃。
- 播放进度缩略图与 Shorts 横屏视频的简单测量不再依赖 BoxWithConstraints。
- 仅尺寸观察改用 onSizeChanged；移除弹幕层无必要的外层 Box。

主要入口：

- [VideoInfoRepository](../app/shared/src/main/kotlin/dev/aaa1115910/bv/repository/VideoInfoRepository.kt)
- [HomeTv](../app/ui/src/main/kotlin/dev/aaa1115910/bv/ui/feature/home/HomeTv.kt)
- [PlayerCommon](../player/ui/src/main/kotlin/dev/aaa1115910/bv/player/ui/PlayerCommon.kt)
- [TvMenuPanel](../player/ui/src/main/kotlin/dev/aaa1115910/bv/player/ui/tv/TvMenuPanel.kt)

### 副作用、设置与线程

- 设置初值在 IO 线程加载，真实值就绪之前不展示可编辑的占位默认值。
- 滚动容器在真实内容就绪后挂载，避免加载指示器的短内容把恢复的滚动位置清零。TV 在等待期间记录进入右栏的请求，控件挂载后再请求焦点。
- 阻塞式偏好保存进入进程级串行 IO 队列。已接收的保存不会因为页面销毁或等待者取消而被取消。
- 播放器滑块对尚未开始的同键保存合并，避免每帧堆积磁盘写入。普通写入与读取屏障隔开合并范围，不能把屏障后的值替换到屏障之前。
- 设置滑块保留停止调节防抖与结束保存，并捕获最后一次事件值、在离开组合时补交未保存值。
- 新目的地在应用组合变更后等待已入队保存。更新弹窗、版本提示、缓存清理等依赖持久化结果的操作先等待保存。
- 启动主题和 FPS 初值放到后台读取。主题、布局和密度的 DataStore 流使用生命周期感知收集，并显式声明 Lifecycle Compose 依赖。
- 日志元数据与测速页凭据读取移至 IO。关注按钮读取已有认证仓库状态，不在组合中阻塞读取用户编号。
- 历史提示计时改为组合生命周期内的 delay。画幅动画不再每帧销毁并重新启动弹幕比例同步协程。
- 使用兼容当前 Compose 的 KeyedSideEffect 执行同步外部对象更新。焦点请求和依赖 AndroidView 挂载顺序的播放器启动仍保留 LaunchedEffect。

保存屏障只保证调用之前已接收的保存。它不承诺导航退出动画仍保留的页面上、尚未结束或尚未提交的手势已落盘。保存错误会记录日志并结束对应等待对象，不阻止后续写入；读取屏障不会把较早保存的失败重新抛出。

主要入口：

- [PreferenceWrites](../app/ui/src/main/kotlin/dev/aaa1115910/bv/ui/PreferenceWrites.kt)
- [PreferenceSnapshot](../app/ui/src/main/kotlin/dev/aaa1115910/bv/ui/feature/settings/PreferenceSnapshot.kt)
- [SettingsCommon](../app/ui/src/main/kotlin/dev/aaa1115910/bv/ui/feature/settings/SettingsCommon.kt)
- [SettingsTv](../app/ui/src/main/kotlin/dev/aaa1115910/bv/ui/feature/settings/SettingsTv.kt)
- [UpdateDialog](../app/ui/src/main/kotlin/dev/aaa1115910/bv/ui/feature/settings/UpdateDialog.kt)
- [KeyedSideEffect](../player/ui/src/main/kotlin/dev/aaa1115910/bv/player/ui/KeyedSideEffect.kt)

### 二维码与图形分配

- ZXing 编码在 Default 调度器执行。生成与帧循环放在同一个内容键控 effect 中，避免快速切换内容时旧任务清理标志导致新动画提前结束。
- 去重矩阵改为普通布尔数组，生成过程不再逐格写 Snapshot 状态。
- draw 阶段不再反向写入完成状态和背景动画状态。
- 非定位图案动画到终点后停止变化。Android 12 及以上定位图案的连续旋转、Lottie 背景和现有入场效果保留。
- 每个绘制器复用临时矩形。旧版 Android 上复用独立 Drawable，而不是每次绘制克隆。

主要入口：

- [MaterialShapeQr](../app/shared/src/main/kotlin/dev/aaa1115910/m3qrcode/MaterialShapeQr.kt)
- [MaterialShapeQrState](../app/shared/src/main/kotlin/dev/aaa1115910/m3qrcode/MaterialShapeQrState.kt)
- [MaterialShapeRenderer](../app/shared/src/main/kotlin/dev/aaa1115910/m3qrcode/MaterialShapeRenderer.kt)

## 审计覆盖与保留项

| 类别 | 检查与处理 |
| --- | --- |
| 状态、快照、稳定性 | 延后高频读取、去除冗余状态、修正选集可观察性。没有添加集合稳定性注解或 ImmutableList。 |
| 布局与测量 | 调整高频播放器、导航轨、缩略图和 Shorts 测量。保留确有容器约束需求的低频页面。 |
| Lazy 布局 | 检查数据身份、内容类型、全列表转换和分组。没有把普通 Row/Column 当作 Lazy 布局处理。 |
| 动画 | 调整可直接延后到绘制/测量的读取。已有正确的 graphicsLayer 和 Lottie lambda 不当作缺陷。 |
| 副作用、生命周期、线程 | 设置 IO、串行保存、读取屏障、协程键、焦点/播放顺序、生命周期收集。 |
| 图形与自定义绘制 | 复用 QR 绘制对象，缓存渐变/轮廓，保留原图预览和现有图片加载方案。 |
| Modifier.Node | 当前源码未发现需要迁移的 Modifier.composed。没有为增加 Node 数量而改写已有节点实现。 |

生产前提已经核对：应用 release/alpha/r8Test 开启 R8；已有 baseline-prof.txt 与 ProfileInstaller。没有用 debug 构建做性能测量。当前项目的 Compose 版本不能使用较新版本的 computedStateOf 或带 keys 的内置 SideEffect。本次未修改全局集合稳定性配置。

以下区域未进行架构性重写：

- 搜索折叠顶栏仍通过尺寸回调计算内容偏移。修复需要一起改变结果列表、输入法与系统栏的测量关系，并做视觉回归，不能用固定高度替代。
- 认证仓库、搜索状态、播放器与 Shorts 内核初始化仍有必须在请求或内核创建前取得的同步偏好。统一异步初始化需要明确的就绪状态和调用契约，不能用临时默认值替代真实认证或播放配置。
- 图片查看器仍保留容器尺寸与图片默认尺寸的初始化握手。未重写低频/无生产调用的 Canvas 与 transform 注册接口。
- QR 的形状扫描和 Android Drawable 组装仍在主线程，因为它们修改活动绘制数据。本次仅将独立编码计算移出主线程。
- 未重新生成基线配置文件，也未新增宏基准模块。没有运行轨迹证明时，不宣称已有基线已覆盖所有新路径。

未使用 SubcomposeAsyncImage，未切换普通播放器为 TextureView，未删减二维码连续动画或公开日期输出。没有把 SnapshotStateList.toList() 描述为必然的全列表复制，也没有将未指定 contentType 的同质列表一概判定为不可复用。

## 回归测试源码与运行验收范围

新增测试源码：

- [PreferenceWriteQueueTest](../app/ui/src/test/kotlin/dev/aaa1115910/bv/ui/PreferenceWriteQueueTest.kt)：FIFO、同键合并、普通写入屏障、读取屏障、执行中追加保存、失败后继续、取消等待者不取消保存。
- [VideoCardDataTest](../app/shared/src/test/kotlin/dev/aaa1115910/bv/entity/carddata/VideoCardDataTest.kt)：同一线程跨时区与语言变化的日期输出。

这些测试未运行。现有 Alpha Build 只装配 APK，不会自动执行新增单元测试，不能将未来 APK 构建成功当作测试通过。

待运行的验收范围：

- 非调试 Alpha APK 的启动主题、用户锁、布局模式与 FPS 开关。
- 手机/平板/TV 设置所有分类、返回后的滚动位置、慢存储下的 TV 焦点和快速保存后导航。
- Alpha 更新开关后立即检查更新、代理更改后启动网络功能、缓存设置后清理。
- 播放器所有画幅、内嵌/全屏切换、HDR、双指缩放、弹幕蒙版、字幕、互动选集、暂停/恢复与历史提示。
- TV 菜单含重复音轨/编码值时的渲染，首页分页与焦点恢复。
- 图片缩放/拖动/返回、Shorts 横屏布局与封面淡出。
- Android 11 及以下快速切换二维码，以及 Android 12 及以上持续定位图案动画。
- 真实发布配置下的冷启动、滚动、播放控制动画帧耗时与分配量对比。

`.claude/`、`artwork/` 与 `libs` 的无关工作区内容未纳入本次变更。基准中的 TouchGestures 修复未再次修改。
