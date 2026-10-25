# Compose 性能审计与修复记录

日期：2026-10-25  
基准提交：`c1552eb4`  
工作分支：`develop`

## 结论与验证边界

已检查应用界面、共享 Compose 组件、播放器界面与图片预览相关代码，并落实可局部修改的性能修复。保留已有功能、普通播放器的 SurfaceView/HDR 路径，以及基准提交中的全屏切换与双指手势修复。

用户已提交并推送 `851b2ce3a41356db1abd3a0f25502005995452ff`。[Alpha Build](https://github.com/mytv-android/bv/actions/runs/40191021655) 已成功，Alpha 与 Debug APK 均已构建并发布。已安装匹配提交的非调试 Alpha，完成下表中的虚拟机功能冒烟检查。核心检查通过，但电视菜单的系统返回行为未通过，不能称为全部回归通过。没有运行本地 Gradle，也没有测量性能提升。

| 检查 | 结果 |
| --- | --- |
| 改动中的 Kotlin 源码与构建脚本语法解析 | 72 个文件，0 个 PSI 语法错误 |
| 新增常用 API 的定向导入检查 | 未发现缺失导入 |
| 版本目录 TOML 解析 | 通过 |
| Compose 约束扩展、Lifecycle Flow API | 从本机已有依赖二进制核对接口存在 |
| `git diff --check` | 按仓库原有换行配置通过 |
| 新增回归测试 | 保存队列 7 项，日期缓存 2 项；仅编写与静态复查，未运行 |
| Android/Kotlin 类型检查与 Compose 编译器验证 | CI 的 Alpha/Debug 装配成功 |
| 下载的 Alpha APK | `1.0.0.r1044.851b2ce3.alpha`，版本号 1044，与本次提交匹配 |
| APK 签名和调试属性 | apksigner 验证通过，非 debuggable |
| 虚拟机安装与启动 | 干净 Android TV API 33 实例已安装 Alpha 1044，启动成功并加载首页内容 |
| 字幕设置保存 | 字号从 24 sp 调到 25 sp，切换分类与强制停止后重启均保留 25 sp，最后恢复 24 sp |
| 字幕遥控焦点 | 向下键从字号移到背景不透明度滑块，未改变背景与边距原值 |
| 其他设置分类 | 播放、弹幕、网络、账号、存储、关于页均已打开并显示真实内容 |
| 关于页返回恢复 | 解码器信息页打开后返回，原位置的解码器入口仍可见 |
| Telegram 二维码 | 显示和关闭通过，连续截图确认动画变化；未验证手机扫码 |
| 首页、视频详情与卡片焦点 | 推荐内容、热门网格和视频详情加载成功；热门标签向下进入首卡，向右移到不同卡片，已核对焦点边界与卡片内容 |
| 实际起播与弹幕 | 日志已出现 onReady/onPlay，截图可见视频画面和弹幕 |
| SurfaceView 路径 | 运行时 SurfaceFlinger 层列表确认保留 SurfaceView |
| 触控内嵌与全屏往返 | 强制平板布局下系统返回可回到内嵌画面；暂停后通过“全屏”和“退出全屏”按钮完成往返，视频帧与进度保留，期间无 ExoPlayer Init/Release；返回后恢复播放产生新的 onPlay |
| HDR | 虚拟机报告无支持的 HDR 类型，不能在此证明 HDR 播放正确 |
| 画幅切换 | 4:3、9:16 的选中状态与画面宽高变化可见，最后恢复默认 |
| 电视菜单返回 | 未通过：菜单打开时系统 Back 直接退回详情，触摸选项后和纯遥控均复现。控制层只有预览按键处理，缺少系统 BackHandler；与父提交比对确认相关逻辑未由本次提交引入，未运行父版本作对照 |
| 遥控暂停与恢复 | 等待实际起播后发送播放/暂停键，日志确认新的 onPause，随后确认新的 onPlay |
| 手机与平板设置布局 | 在同一 TV 实例切换强制手机和平板布局，字幕页均显示 24 sp、40%、12 dp，分类返回正常。这不等于实体手机验证 |
| 测试设置恢复 | 自动布局与字幕原值已在最终重启后再次确认；画幅此前已恢复默认，没有更改账号、代理或凭据 |
| 应用错误日志 | 最终重启前后 BV 进程日志未发现致命异常、设置保存失败或重复 Lazy 键异常，最终系统崩溃缓冲区为空。历史诊断中的崩溃属于 UIAutomator 连接超时，不是 BV |
| 帧耗时对比 | 未执行 |

语法检查使用 Kotlin PSI 解析器，不生成应用或 Kotlin 字节码。CI 随后执行了 `assembleDefaultAlpha assembleDefaultDebug`，日志记录 `BUILD SUCCESSFUL in 40m 47s`。工作流未执行新增单元测试，装配成功不等于单元测试通过。

发布版本：[alpha-r1044](https://github.com/mytv-android/bv/releases/tag/alpha-r1044)。下载产物的 SHA-256 为 `11f8959f3b6cde18cd2a15d55cc31582a47347d7a0cce5fca4f0f562e313ec29`。CI 后处理出现 Gradle 缓存上传服务不可用警告，但未影响 APK 构建、产物上传或发布。

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

这些测试未运行。现有 Alpha Build 只装配 APK，不会自动执行新增单元测试，不能把本次 APK 装配成功当作测试通过。

已完成的运行检查见开头结果表。原有 TV 虚拟机在 BV 启动之前出现系统界面与输入法无响应，安装超时；保留其数据，改用独立、干净的 Android TV API 33 实例完成安装和测试。该实例只用于本次 BV 验证，没有操作其他会话的设备或重启共享 ADB。宿主机负载明显，因此运行记录只用作功能证据，不作为性能基准。

验证结束后已恢复自动布局、字幕原值和默认画幅。专用模拟器进程优先级已恢复 Normal，随后正常退出；ADB 列表与进程查询均确认实例已停止，AVD 数据保留。验证阶段没有修改应用源码，没有再次提交或推送；仅此报告为新增的已跟踪工作区改动，原有 `libs` 无关改动保留。

本机证据保存在 Git 忽略的 `.bvtest/verification-851b2ce3/`，未随源码提交：

- [最终验证结果](../.bvtest/verification-851b2ce3/final-verification-results.json)
- [字幕持久化截图](../.bvtest/verification-851b2ce3/subtitle-persistence-evidence.jpg)
- [画幅切换截图](../.bvtest/verification-851b2ce3/aspect-ratio-evidence.jpg)
- [手机与平板设置布局](../.bvtest/verification-851b2ce3/forced-layout-settings-evidence.jpg)
- [内嵌与全屏往返截图](../.bvtest/verification-851b2ce3/fullscreen-roundtrip-evidence.jpg)
- [最终日志归类](../.bvtest/verification-851b2ce3/final-log-classification.json)
- [设置恢复记录](../.bvtest/verification-851b2ce3/final-setting-restoration.json)
- [专用模拟器清理记录](../.bvtest/verification-851b2ce3/environment-cleanup.json)

Alpha 1044 中已确认的未通过项：

- 电视画面菜单打开时，系统 Back 直接退回视频详情，而不是只关闭菜单。纯遥控和触摸选择画幅后均复现。
- TvController 对 Back 的处理只位于预览按键事件中，缺少面板打开时的系统 BackHandler。相关按键与导航逻辑在父提交中相同；未运行父版本，不声称做过旧 APK 对照。上述产物验证没有修改这段源码。

后续系统返回修复：

- 用户确认修复后，在 TvController 增加常驻的 `BackHandler(enabled = panelOpen && !showRemoteControlHint)`，复用既有 `closePanels()`。
- 系统返回只在菜单或选集面板打开时消费，沿用原有焦点恢复；遥控提示显示时不抢占返回。原有按下/抬起按键处理和无面板退出逻辑不变。
- 源码静态核对通过：Kotlin PSI 解析无语法错误，现有 Activity Compose 依赖含所需 BackHandler API。移除新增导入和拦截块后，文件与 HEAD 完全一致，确认原有遥控处理及焦点恢复未改。`git diff --check` 通过。
- 这是尚未提交的源码修复，不在上述 Alpha 1044 中。修复后的构建与设备回归尚未完成；核对最新 CI 仍是 `851b2ce3`，不能用旧产物的验证结果证明新修复通过。需要先提交、推送此修复，取得匹配的新 CI APK。没有运行本地 Gradle 或启动新设备测试。
- 已准备 [设备回归脚本](../.bvtest/verify_tv_menu_system_back.py)。要求明确的设备、AVD 和新版本号，在已起播的 TV 页面分别检查播放中与暂停状态；断言系统返回和菜单键均关闭菜单、调用原 closePanels、保持进程与 SurfaceView、不出现播放器 Init/Release 或播放状态改变。脚本拒绝把 Alpha 1044 当作修复验证。
- 脚本自身的 Python 语法、命令入口和模拟数据检查已通过：播放中、暂停中正常关闭样例可通过，模拟旧版返回后释放播放器的样例会被拒绝。这只验证检查脚本，不是 Android 应用运行测试。
- 回归还需覆盖：触摸改变菜单焦点后返回仍只关菜单；选集面板同样关闭；焦点回到打开面板前的入口；遥控提示层不被抢占；菜单键与方向键保持正常。

本次尚未覆盖的范围：

- 旧安装的升级路径、用户锁、启动主题切换、FPS 开关、真实慢存储压力与所有设置类别的手机/平板组合。
- Alpha 更新开关后立即检查更新、代理更改后启动网络功能、缓存设置后清理。
- 全部画幅组合、HDR、双指缩放、弹幕蒙版、实际字幕流、互动选集与历史提示的完整回归。
- 含重复音轨/编码值的运行时注入、首页分页与详情返回后的卡片焦点恢复。
- 图片缩放/拖动/返回、Shorts 横屏布局与封面淡出。
- Android 11 及以下快速切换二维码、实际扫码。Android 13 的显示、关闭与持续动画帧变化已验证。
- 真实发布配置下的冷启动、滚动、播放控制动画帧耗时与分配量对比。

`.claude/`、`artwork/` 与 `libs` 的无关工作区内容未纳入本次变更。基准中的 TouchGestures 修复未再次修改。
