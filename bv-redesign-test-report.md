# BV Expressive 重设计 · 三端实机测试报告

- **被测版本**：`BV_971_0.3.0.r971.4c71315e.alpha_default_universal.apk`（CI 预发布 `alpha-r971`，对应 `develop` HEAD `4c71315e`）
- **获取方式**：`gh release download alpha-r971 --repo mytv-android/bv`（未本地编译）
- **对照依据**：`docs/design/bv-expressive-redesign.html`（浏览器交互稿）+ `docs/design/IMPLEMENTATION_PROMPT.md` 第 14.2/14.3 节验收清单
- **设备**（全程只开一台虚拟机）：
  - 手机 / 平板：`Phone_AI`（Pixel 7，android-34）→ `wm size 1080x2340 + density 480`（360×780dp）；`wm size 2392x1600 + density 320`（1196×800dp）
  - 电视：`Television_1080p`（android-31，1920×1080 @320 → 960×540dp）
- **截图证据**：`.bvtest/shots/*.png`（该目录已加入 `.git/info/exclude`，不进 git）

---

## 复测更新（2026-10-16 晚）

被测版本：`alpha-r973`（`402b0304`，含 P0/P1 修复与 baseline profile）。

| 项 | 状态 | 证据 |
|---|---|---|
| P0 详情 / 播放器被风控 `-352` | ✅ 已修（`getVideoDetail` 补 `Referer`） | 手机：详情页正常打开（简介/评论、UP 行、点赞投币收藏稍后分享、相关推荐）、内嵌播放器 + 弹幕、横屏全屏播放器顶栏（返回·标题·字幕·弹开·更多）与底栏（暂停·下一集·时间·1.0×·480P·退出全屏）与设计一致 —— `v1/v2/v3`；电视：详情页同设计稿（Eyebrow + 标题 + UP 行 + 播放/操作按钮行 + 合集/相关推荐）—— `tvD` |
| P1 电视首页焦点死锁 | ✅ 已修（标签行按 ↓ 重新把焦点交给内容） | 修复前：在「热门」标签上按 ↓ 画面 **0 像素**变化；修复后：**93.7 万像素**变化，焦点进入网格，确认键可打开详情 —— `tvB/tvC/tvD` |
| baseline profile | ✅ 已加入并验证生效 | 见下 |

**baseline profile 的做法**（全程未本地编译）：取 CI 的 alpha APK 装到模拟器 → 按 首页（滚动、换标签）→ 分区 → 影视 → 我的 → 搜索 → 刷刷翻页 走一遍 CUJ → `adb shell cmd package dump-profiles` 采样 ART profile → 用 CI 的 `mapping.txt` 把 ART 文本 profile 还原成源符号 HRF（12,422 条，HS 标记）→ 落到 `app/src/main/baselineProfiles/baseline-prof.txt`，`app` 增加 `androidx.profileinstaller`。

验证结果：
- r973 的 APK 里确实带上 `assets/dexopt/baseline.prof`（18,180 B）+ `baseline.profm`（2,150 B）
- 设备 logcat 出现 `ProfileInstaller: Installing profile for dev.aaa1115910.bv`；随后 `cmd package compile -m speed-profile` 使 `Dexopt state` 变为 `status=speed-profile`（ART 已按该 profile 编译）
- 冷启动 A/B（r973 带 profile vs r972 不带，各 4 次 `am start -W`）：≈600ms vs ≈590ms，**模拟器上无可测差异**——该指标被网络/首屏数据主导，收益量化需要在真机上跑 Macrobenchmark

P2/P3 各项（视频深链、电视热门用网格、轮播 eyebrow 缺分区名/评分、搜索框缺麦克风等）本轮未处理。

---

## 结论摘要

三端形态与设计稿整体一致：一个 `MainActivity`、`DeviceProfile` 驱动的手机 / 平板 / 电视三套布局、material-kolor 种子色实时换肤、Expressive 形状与弹簧、电视自建焦点系统（导航轨聚焦展开、枢轴滚动、焦点放大描边光晕）都已落地并且可交互。

但存在 **1 个阻断级缺陷** 和 **1 个电视端焦点死锁**，另有若干小偏差。

| 级别 | 问题 | 影响 |
|---|---|---|
| P0 | 视频详情接口被 B 站风控拒绝（缺 `Referer`），页面直接抛原始异常 | 详情页 / 播放器 / 点赞投币收藏稍后再看 / 选集 / 评论 **三端全部不可用** |
| P1 | 电视首页切到「热门」「分区」后按 ↓ 无法进入内容网格 | 电视端这部分内容无法浏览、也进不去详情 |
| P2 | 视频深链（`b23.tv`、`bilibili.com/video`）未实现，清单里只有 `bugvideo://qrtoken` | 从浏览器/其它 App 点视频链接打不开 |
| P3 | 若干视觉/文案偏差（见下） | 与设计稿不完全一致 |

---

## P0 · 详情页与播放器全端不可用（你遇到的"视频播放不了"）

**现象**：从任意列表点进视频，详情页显示一段英文异常文本 + 「重试」按钮：

```
Illegal input: Fields [View, Tags] are required for type with serial name
'dev.aaa1115910.biliapi.http.entity.video.VideoDetail', but they were missing at path: $.data
```

手机、电视都一样（电视端截图 `tv26-detail.png`）。详情进不去 → 播放器也进不去。

**根因**：`BiliHttpApi.getVideoDetail()` 调用的是 WBI 接口 `/x/web-interface/wbi/view/detail`，但**没有带 `Referer` 请求头**，B 站风控直接返回：

```json
{"code":-352,"message":"风控校验失败","ttl":1,"data":{"v_voucher":"voucher_..."}}
```

`data` 里只有 `v_voucher`，没有 `View`/`Tags` → kotlinx.serialization 抛异常 → 页面把异常原文显示出来。

**对照实验**（本机 curl + 自算 WBI 签名，与 App 用同一个 web UA）：

| 请求 | 结果 |
|---|---|
| 不带 Referer（模拟 App 现状） | `code=-352 风控校验失败` |
| 带 `Referer: https://www.bilibili.com/` | `code=0`，`data` 含 `View/Card/Tags/Reply/Related` |
| 旧接口 `/x/web-interface/view/detail`（非 wbi，不带 Referer） | `code=0` 正常 |
| 无关变量：`buvid3`、`buvid4`、空 `SESSDATA`、假 `SESSDATA` | 都不影响结论 |

**引入点**：`/x/web-interface/view` → `/x/web-interface/wbi/view/detail` 的切换发生在 `1d8369e4`（2026-10-15「合并上游 PR 与 Frost819/newBV、fantasytyx/bv 的可用修复」），已包含在 HEAD 里。也就是说这是**相对 alpha-r943 的回归**；其它 fork（`fantasy/develop`）的同一函数是带 `header("Referer", "https://www.bilibili.com/")` 的，还额外有 `gaia_vtoken` / `checkForVVoucher`（滑块验证）处理，这两部分没有合并过来。

**修法**（一行，与同文件其它调用一致，`bili-api/.../http/BiliHttpApi.kt:185`）：

```kotlin
suspend fun getVideoDetail(...): BiliResponse<VideoDetail> = client.get("/x/web-interface/wbi/view/detail") {
    parameter("aid", av)
    parameter("bvid", bv)
    header("referer", "https://www.bilibili.com")   // ← 补这一行
    sessData?.let { header("Cookie", "SESSDATA=$sessData;") }
}.body()
```

**附带建议**：错误态直接把 Kotlin 异常文本打到屏幕上对用户不友好，`LoadingState` 建议换成"加载失败，请重试"这类文案（`app/ui/.../designsystem/component/LoadingState.kt`）。

---

## P1 · 电视首页「热门 / 分区」焦点死锁

**复现**：电视首页 → 焦点在顶栏标签上切到「热门」→ 按 ↓ 。**画面像素完全不变**（连续两帧 diff = 0 像素），焦点无法进入内容网格；按确认键也无反应。「分区」同样（diff = 0 像素）。

**根因**：`app/ui/.../feature/home/HomeTv.kt`
- L109 `var entryPending by remember { mutableStateOf(true) }`，只在进入页面时为 `true`；
- L219 标签获得焦点时 `if (it) entryPending = false`；
- L681-688 `TvGridContent` 用 `LaunchedEffect(cards.isNotEmpty()) { if (!entryPending) return@LaunchedEffect; ... }`。

切标签时 `entryPending` 已经是 `false`，而 `cards.isNotEmpty()` 这个 key 也没变化 → 这段 effect 不会再跑 → 网格永远拿不到焦点。

**影响**：电视端（960dp 宽、只有遥控器）切到热门/分区后就卡死在标签行，`为你推荐` 之外的推荐内容全部够不到。

**建议**：切标签时把 `entryPending` 重置为 `true`（或在切换 chip 的分支里直接调用对应 grid 的 `FocusRequester.requestFocus()`）。

---

## 其它偏差

1. **视频深链未实现**（P2）：`app/ui/src/main/AndroidManifest.xml` 只注册了 `bugvideo://qrtoken`。设计 §8.1 要求在同一个 `MainActivity` 上再收 `b23.tv`、`bilibili.com/video/...`。目前从外部点视频链接不会打开 App。
2. **电视首页「热门」「关注」用 4 列网格**（P2，与设计稿的"行"不同）：`HomeTv.kt:673` 注释写明是有意沿用旧电视版。若以设计稿为准应改成 `TvRowPivot` 横向行；这条需要你拍板。
3. **轮播 eyebrow 少了后半句**（P3）：首页平板应为「今日精选 · {分区名}」，影视三端应为「本季热播 · 评分 {x}」，实现里只有「今日精选」「本季热播」——分区名/评分没接上。
4. **搜索框没有麦克风图标**（P3）：原型 `BvSearchField` 右侧有麦克风，实机只有放大镜。
5. **手机影视页竖海报轮播只有 1 张**（P3，非回归）：`PgcRepository.getCarousel()` 抓 `www.bilibili.com/anime` 的 `__INITIAL_STATE__`，按 `episodeId`/番剧链接过滤后只剩 1 条，与原型的多张示意不同。数据层问题，不属于本轮改造。
6. **手机横屏（780×360dp）导航变成左侧轨道 + 搜索 FAB**（P3，需拍板）：`IMPLEMENTATION_PROMPT` §2 写"< 840dp 用底部导航栏"，设计页「03 · 自适应框架」的宽度分级表写"Medium 600–839dp → WideNavigationRail 收起"。当前实现跟的是后者。
7. **电视顶栏时钟位置**：与设计一致（右侧 HH:mm），但页面上没有任何"当前时间/进度"以外的信息缺失，仅记录已核对。

---

## 已逐项核对通过的部分（附证据）

**手机（360×780dp）**
- 首页：头像 + 热搜占位搜索框 + 推荐/热门/分区；精选轮播（大图 + 右侧露边、CinemaScrim、「今日精选」+ 2 行标题）；热门页无轮播/无继续观看；分区页 31 个 BvChip + 2 列网格 —— `s01/c2/c3/c4`
- 搜索：自动弹键盘、热搜 2 列 + 名次徽章（1-3 上色）、联想列表（匹配段加粗）、提交后 综合/视频/番剧/影视/UP 主 五类齐全、UP 主列表带头像签名 —— `s1/s2/s3/s4/s5`
- 影视：标题 + 搜索按钮 + 6 个标签；竖海报轮播 + 「更新至第 N 话」；7 日时间表（今天=圆角 16 primary，其它全圆角）；列表行带「追番」按钮 —— `p1`
- 动态（未登录）：居中「登录后查看关注的 UP 主动态」+ 登录按钮 —— `y1`
- 我的（未登录）：头像 + 「登录后同步历史、收藏和追番」+ 登录；4 张彩色入口卡（历史/收藏夹/稍后再看/追番追剧）；列表卡（关注的 UP 主 / 账号与用户锁 0 个账号 / 设置 主题、播放、弹幕）—— `m1`
- 设置：8 个分类图标容器形状与配色符合规格表；外观与主题含 主题来源 / 6 预设色 + 自定义 / 6 种配色风格 / 对比度 Slider / 深色模式四态 / 详情页使用封面颜色 / 减少动效 —— `g1/g2/g3`
- **实时换肤**：改种子色 → 全 App 立即变蓝（`g4`）；切深色 → 全站深色（`g5`）；切影院纯黑 → 页面背景采样为纯黑 `(0,0,0)`、容器仍是 surfaceContainer —— `tb6`
- 刷刷：竖屏翻页（上滑换条，氛围光随封面变）、横屏自动进入全屏播放、长按 2× 出现「2× 倍速中」标签、波浪进度条、右侧操作列 + 底部 UP/标题/标签 —— `sh1/sh2/sh4/r2/r3`

**平板（1196×800dp）**
- 首页：左侧导航轨（顶部汉堡 + 搜索 FAB + 5 项一级 + 底部设置）、右上头像、`搜索视频、番剧、UP 主` 搜索框 + 推荐/热门/分区 + 4 个快捷分区 chips、居中大轮播（露出两侧、当前页带「今日精选/标题/UP · 播放 · 时长」+「▶ 播放」「稍后再看」）、`Adaptive(180dp)` 网格（此处 5 列）—— `tb1/tb2`
- 影视：标题 + 6 标签 + 索引/我的追番、横幅（本季热播/标题/看第 N 话/追番）+ 右侧「今天更新 周五」侧卡、热门番剧 6 列海报 —— `tb3`
- 我的 / 设置：列表-详情双栏，设置默认选中「外观与主题」—— `tb4/tb5`
- 尺寸切换（手机 ↔ 平板）时正在播放的视频不中断（`t1`→`t3` 同一视频继续播放）

**电视（960×540dp）**
- 首页：左侧 76dp 图标轨（聚焦展开为带文字的模态轨，`tv4`）、顶栏 推荐/热门/分区/关注 + 时钟、全幅背景 + 精选信息区（初始焦点在「▶ 播放」，`tv1`）、按 ↓ 进入「为你推荐」行、行内枢轴滚动（焦点停在左侧约 15%，`tv3`）
- 热门：4 列网格（`tv6`）
- 影视：6 标签 + 本季热播信息区 + 「看第 1276 话 / 追番」+ 时间表/索引/我的追番 + 热门番剧海报行（`tv28`）
- 搜索：拼音搜索框 + A–Z/0–9 六列键盘（初始焦点 A）+ 空格/删除/清空 + 语音按钮；中列搜索历史、右列热搜 6 条带名次徽章（`tv32`）
- 设置：列表-详情，左列分类聚焦即切换右栏（`tv30`）
- 刷刷：左侧播放队列（当前项 primary 描边）+ 中间视频框（初始焦点、放大 + 描边 + 光晕）+ 右侧 UP/标题/点赞数/5 个操作按钮 + 评论卡；评论面板 360dp 从右侧滑出（`tv20/tv23`）；**视频能正常解码播放**
- 电视端整体深色（`cinema`）成立

---

## 未覆盖 / 建议补测

1. **登录后的一切**：动态列表与动态详情、我的资料头、历史/收藏夹/稍后再看/追番/关注、详情页 点赞/投币/收藏/稍后再看/关注/分享、评论列表、选集。
2. **播放器控制层细节**：字幕显示与菜单、画质/编码/倍速/画面比例菜单、拖动手势、拖动预览缩略图、播放心跳——全部被 P0 阻断。
3. **电视端详情页与播放器**、电视端动态/我的：同样被 P0 阻断。
4. **减少动效**：开关存在且可切换，但没有做像素级验证（需要逐帧对比弹簧/形状变形）。
5. **真机 GPU 下的视频渲染**：本轮电视端用的是 `-gpu swiftshader_indirect`，刷刷首次进入时视频框出现过一次绿屏（未解码完成），之后正常；真机/硬件加速下建议复核一次。
6. **深链**：`b23.tv` / `bilibili.com/video` 未实现，无法用链接直达视频。

---

## 建议的下一步

1. 修 **P0**（一行 `Referer`）与 **P1**（切标签重置 `entryPending`），push 触发 CI 出新的 alpha 包。
2. 新包出来后我再跑一轮，把「详情 → 播放器 → 详情操作组 → 评论」这条链路补测完。
3. 需要覆盖登录后功能的话，我可以把登录页调出来，你用哔哩哔哩 App 扫一次码（二维码 3 分钟刷新）。
4. P2/P3 里有两处需要你拍板：电视首页「热门」是否改回横向行；手机横屏是否改回底部导航栏。
