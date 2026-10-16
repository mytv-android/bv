<script>
'use strict';
const card = (icon, title, body, cls = '') => `<div class="card ${cls}"><h3><span class="ico">${ic(icon)}</span>${title}</h3>${body}</div>`;
const P = t => `<p>${t}</p>`;
const UL = items => `<ul class="clean">${items.map(i => `<li>${i}</li>`).join('')}</ul>`;

const CONTENT = {
  principleCards: [
    card('tv', '一套界面，三种距离', P('布局看窗口宽度，交互看输入方式，字号看观看距离。不再按「手机版 / 电视版」分叉，同一个 Composable 在三块屏幕上各自成立。')),
    card('palette', '内容决定颜色', P('全局用种子色生成主题，进到某个视频就换成它封面的颜色。颜色丰富但不杂乱，因为所有颜色都来自同一套色调算法。'), 't'),
    card('movie', '画面优先', P('封面全幅、播放时界面退场、氛围光延伸画面边缘。电视、播放器和刷刷固定深色，深色模式另有「影院纯黑」。'), 's'),
    card('explore', '动效有物理感，也有节制', P('只在按下、聚焦、选中、加载时出现形状变形和弹簧回弹，参数统一取自 <code>MotionScheme.expressive()</code>。系统「移除动画」开启时全部降级为淡入淡出。')),
    card('tune', '焦点和触摸同等重要', P('每个可点的东西都能被方向键走到，并且有清楚的焦点样式。焦点路径是设计出来的，不靠默认搜索碰运气。'), 't'),
    card('info', '功能三端一致', P('手机缺的番剧、分区、稍后再看、字幕，电视缺的评论、动态详情，这次全部补齐。差异只在布局，不在功能。'), 's'),
  ],
  axisCards: [
    card('tablet', '尺寸 · WindowSizeClass', P('决定导航形态、分栏数量、网格列数。用 <code>currentWindowAdaptiveInfo()</code>，统一到一个入口，替换现在混用的两套 API。') + UL(['Compact：底部导航栏，单栏', 'Medium / Expanded：导航轨，双栏', 'Large 及以上：展开的导航轨，更多列'])),
    card('tune', '输入 · InputMode', P('决定焦点是否可见、手势还是按键。根据最后一次输入事件的来源实时切换。') + UL(['Touch：无焦点框，手势优先', 'DPad：焦点放大 + 描边，按键映射', 'Pointer：悬停预览、右键菜单、滚轮翻页']), 't'),
    card('tv', '距离 · Distance', P('决定字号、安全边距和最小焦点目标。由 <code>UiModeManager</code> 和 <code>FEATURE_LEANBACK</code> 判断，投屏到电视的平板也可以在设置里手动切换。') + UL(['Near：标准 M3 字号', 'Far：正文 16sp 起，四周留 48 × 27dp 过扫描安全区', '沿用 widthPx / 960 的密度换算，4K 和 1080p 布局一致']), 's'),
  ],
  colorRules: [
    card('palette', '三层主题', UL(['系统壁纸色（Android 12+，可选）', '用户种子色，默认 B 站粉 #FB7299', '内容种子：详情、播放器、刷刷用封面色局部覆盖'])),
    card('storage', '取色不卡顿', UL(['只取 112px 小图，关闭硬件位图', '在 Dispatchers.Default 计算，按 URL 放进 LruCache', '列表滚动停下后预取下一屏，进详情时已就绪']), 't'),
    card('explore', '颜色过渡', UL(['<code>animate = true</code>：主题切换时颜色角色逐个补间', '刷刷换条时氛围光用 slowEffects 弹簧过渡', '减少动效时直接切换']), 's'),
    card('tune', '和谐色', UL(['31 个分区各有标志色，经 <code>Color.harmonize()</code> 向 primary 靠拢', '点赞 / 投币 / 收藏的语义色同样处理', '任何种子色下都不刺眼']), ''),
  ],
  shortsCards: [
    card('dynamic', '数据源', P('在 bili-api 新增 <code>StoryRepository</code>，调用 App 端竖屏流接口（<code>/x/v2/feed/index/story</code>，以当前 aid 为游标，需要 access_key）。接口不可用或未登录时，退回推荐流并优先排竖屏稿件。接口参数需要在实现时抓包核实。')),
    card('play', '播放器池', P('同时只保留 3 个 ExoPlayer：上一条、当前、下一条，Surface 复用。用 media3 的 <code>DefaultPreloadManager</code> 预加载后两条的前几秒。翻页停稳 150ms 后才开始播放，快速划过时不发请求。'), 't'),
    card('shorts', '页面结构', P('<code>VerticalPager</code>（<code>beyondViewportPageCount = 1</code>，<code>key = bvid</code>）+ <code>ShortsViewModel</code> 分页加载。播放心跳、历史上报复用 <code>VideoPlayerV3ViewModel</code> 的逻辑。'), 's'),
    card('add', '入口', UL(['底部导航第二项 / 导航轨第二项', '任意视频卡长按菜单「从这里开始刷」', '竖屏视频在详情页点全屏，直接进入刷刷'])),
    card('movie', '横屏视频', P('不裁切：居中显示，上下用封面模糊填满；手机横过来自动切成全屏播放，竖回来继续刷。平板和电视上横屏视频直接用横向布局。'), 't'),
    card('wifi', '流量与默认值', UL(['移动网络默认 720P，只预加载 1 条', 'Wi-Fi 和电视用设置里的默认画质', '弹幕默认关闭，可在操作列打开']), 's'),
  ],
  tvCards: [
    card('tune', '焦点只在需要时出现', P('<code>bvFocus()</code> 读 <code>LocalDeviceProfile.input</code>：触摸时不放大不描边，方向键或键盘一按立刻出现。平板接遥控器、电视接鼠标都能正常用。')),
    card('history', '焦点记忆与进出规则', P('每个行和网格加 <code>focusRestorer()</code>；一级页面用 <code>rememberSaveable</code> 记住最后焦点。用 <code>focusProperties { onEnter / onExit }</code> 替换现在的 tabMoved 延迟标志和「最后一张卡吞掉右键」的写法。'), 't'),
    card('explore', '枢轴滚动', P('通过 <code>LocalBringIntoViewSpec</code> 提供一个枢轴：横向行里焦点保持在 15% 处，纵向保持在 30% 处，滚动用 defaultSpatial 弹簧。不需要 tv-foundation 的 TvLazyRow。'), 's'),
    card('menu', '按键集中处理', P('现在 player 里有 75 处 <code>onPreviewKeyEvent</code>。统一成一张 <code>KeyMap</code>：确认长按（兼容 keycode 763）、菜单、频道 ±、媒体键，各界面只声明动作。')),
    card('tv', '10 英尺排版', UL(['过扫描安全区 48 × 27dp', '正文 16sp、标题 22sp、展示字 32–40sp（960dp 画布）', '焦点目标不小于 48dp，行间距 ≥ 24dp']), 't'),
    card('check', '可测试', P('焦点路径写成 Compose UI 测试：<code>performKeyInput { pressKey(Key.DirectionDown) }</code> 后断言焦点节点，防止回归。'), 's'),
  ],
  motionRows: () => MOTION.map(m => `<tr><td><code>${m.name}</code></td><td class="num">damping ${m.z} · stiffness ${m.k}</td><td>${m.use}</td></tr>`).join(''),
  compRows: [
    ['ShortNavigationBar / WideNavigationRail', '应用外壳', '按 DeviceProfile 自动切换；电视用收起的导航轨，焦点进入时展开为模态'],
    ['AppBarWithSearch / ExpandedFullScreenSearchBar', '首页顶部、搜索', '手机全屏展开搜索，平板用 Docked 形式'],
    ['ButtonGroup + ToggleButton', '视频详情操作区、设置里的单选', '点赞、投币、收藏连成一组，选中项变胶囊形'],
    ['HorizontalFloatingToolbar / VerticalFloatingToolbar', '刷刷操作列、平板播放控制', '悬浮在画面上，不占布局'],
    ['FloatingActionButtonMenu', '动态页、收藏夹', '发动态、新建收藏夹等次要操作收进一个 FAB'],
    ['LoadingIndicator / ContainedLoadingIndicator', '首屏、下拉刷新、播放缓冲', '替换线性进度条和旧的 pullRefresh'],
    ['LinearWavyProgressIndicator', '播放进度、刷刷进度', '播放中显示波浪，暂停时拉直'],
    ['HorizontalMultiBrowseCarousel / CenteredHeroCarousel', '首页精选、影视海报', '手机多尺寸露边，平板居中大图'],
    ['MaterialShapes', '头像、等级徽章、设置图标、点赞反馈', '形状本身也是信息：头像用 Cookie，徽章用 Sunny'],
    ['ListDetailPaneScaffold / SupportingPaneScaffold', '动态、我的、设置 / 视频详情', '大屏双栏，小屏自动退回单栏'],
    ['Flexible TopAppBar（Large / Medium）', 'UP 主空间、收藏夹、历史', '滚动时标题从大字收为单行'],
  ].map(([a, b, c]) => `<tr><td><code>${a}</code></td><td>${b}</td><td>${c}</td></tr>`).join(''),
  routeRows: [
    ['MainActivity（手机） / MainActivity（电视）', 'MainActivity + Home', '一个 Activity，两个启动类别；电视的用户锁解锁页作为启动时的覆盖层'],
    ['VideoPlayerActivity / VideoInfoActivity / VideoPlayerV3Activity', 'Video(bvid) · Player(bvid, cid)', '手机和平板详情页内嵌播放器；电视从详情页进全屏 Player'],
    ['SeasonInfoActivity', 'Season(seasonId | epId)', '番剧详情，选集和季度选择器沿用电视版逻辑'],
    ['SearchInputActivity / SearchResultActivity', 'Search(query?)', '输入和结果是同一路由的两个状态'],
    ['DynamicDetailActivity', 'Dynamic(id)', '大屏是列表-详情的右栏'],
    ['UserSpaceActivity / UpInfoActivity', 'Space(mid)', ''],
    ['History / Favorite / ToView / FollowingSeason / Follow(ing)User', 'Me.History / Me.Favorite / Me.ToView / Me.Bangumi / Me.Following', '「我的」下的子路由'],
    ['TagActivity', 'Tag(id)', ''],
    ['PgcIndexActivity / AnimeTimelineActivity', 'Pgc.Index(type) / Pgc.Timeline', ''],
    ['LoginActivity ×2', 'Login', '大屏默认扫码，手机默认短信'],
    ['UserSwitchActivity / UserLockSettingsActivity', 'Accounts / Accounts.Lock', ''],
    ['SettingsActivity ×2 / LogsActivity / MediaCodecActivity / SpeedTestActivity', 'Settings(category)', '后三个成为设置的子页面'],
    ['RemoteControllerPanelDemoActivity', '覆盖层', '电视首次播放时显示一次'],
    ['IntentHandlerActivity / QrTokenResultActivity', '深链 → MainActivity', '<code>bugvideo://qrtoken</code> 进入后弹出导入账号对话框'],
    ['（新增）', 'Shorts(startBvid?)', '刷刷'],
  ].map(([a, b, c]) => `<tr><td>${a}</td><td><code>${b}</code></td><td>${c}</td></tr>`).join(''),
  phases: [
    ['地基', '2 周', '新建 <code>app:ui</code>；BvTheme + material-kolor；DeviceProfile；bvFocus 与 KeyMap；应用外壳和导航。旧入口暂时保留，设置里可切回。'],
    ['核心链路', '3 周', '首页、视频详情、播放器（合并 player:mobile 与 player:tv 的控制层，补上手机端字幕和心跳）、搜索。'],
    ['刷刷', '2 周', 'StoryRepository、VerticalPager + PreloadManager、三端交互和氛围光。'],
    ['补齐其余界面', '3 周', '影视、分区、动态、我的、设置、登录；补齐两端各自缺失的功能，接入点赞和投币。'],
    ['收尾', '1–2 周', '删除 <code>app:mobile</code>、<code>app:tv</code>、<code>player:mobile</code>、<code>player:tv</code> 和 androidx.tv 依赖；在手机、折叠屏、平板、电视盒子上回归；建立启动和滑动帧率基线。'],
  ].map(([t, d, b], i) => `<div class="card" style="grid-template-columns:auto minmax(0,1fr);column-gap:16px"><span class="num" style="grid-row:span 2;width:44px;height:44px;display:grid;place-items:center;background:var(--primary);color:var(--on-primary);font-weight:800;font-size:18px;clip-path:var(--shape-cookie9)">P${i}</span><h3 style="font-size:17px">${t} <span class="pill o">${d}</span></h3><p>${b}</p></div>`).join(''),
  risks: [
    ['部分 Expressive API 仍是实验性', 'LoadingIndicator、MaterialShapes 在 1.5.0-alpha19 撤回了转正。全部包在 designsystem 里，业务代码不直接 opt-in，升级时只改一处。'],
    ['低端电视盒子性能', '模糊依赖 RenderEffect（API 31+）。低于 31 用 Coil 预先模糊的小图；<code>isLowRamDevice</code> 时关闭氛围光和形状变形。'],
    ['封面取色开销', '只在详情、播放器、刷刷取色；小图 + 后台线程 + 缓存；列表卡片不取色。'],
    ['Prefs 的 runBlocking 读取', '主题偏好改为 Flow 收集，首帧用上次缓存的种子色，避免启动阻塞。'],
    ['story 接口需要 App 鉴权', 'Web 登录的账号可能拿不到，回退到推荐流；接口字段在实现时抓包确认。'],
    ['焦点回归', '旧电视代码有 57 处 FocusRequester、21 处 focusRestorer。迁移时按界面写焦点路径测试，逐页替换。'],
  ].map(([t, b]) => `<div class="card" style="gap:6px"><h3 style="font-size:16px">${t}</h3><p>${b}</p></div>`).join(''),
  modules: `<div style="display:grid;grid-template-columns:minmax(0,1fr) auto minmax(0,1fr);gap:12px;align-items:center">
    <div style="display:grid;gap:8px">${['app（空壳）', 'app:mobile', 'app:tv', 'player:mobile', 'player:tv'].map((m, i) => `<div style="padding:10px 14px;border-radius:14px;background:var(--surface-container-high);font:500 13px var(--font-mono);${i ? 'text-decoration:line-through;color:var(--on-surface-variant)' : ''}">${m}</div>`).join('')}</div>
    <svg viewBox="0 0 24 24" width="28" height="28" style="fill:var(--primary)" aria-hidden="true"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/></svg>
    <div style="display:grid;gap:8px"><div style="padding:10px 14px;border-radius:14px;background:var(--primary-container);color:var(--on-primary-container);font:500 13px var(--font-mono)">app<br><span style="font-family:var(--font-body);font-size:12px">单 Activity、清单、flavor</span></div><div style="padding:10px 14px;border-radius:14px;background:var(--primary);color:var(--on-primary);font:500 13px var(--font-mono)">app:ui（新）<br><span style="font-family:var(--font-body);font-size:12px">designsystem / adaptive / feature/*</span></div><div style="padding:10px 14px;border-radius:14px;background:var(--tertiary);color:var(--on-tertiary);font:500 13px var(--font-mono)">player:ui（新）<br><span style="font-family:var(--font-body);font-size:12px">统一控制层与菜单</span></div></div>
  </div>
  <p class="muted" style="font-size:14px;margin-top:12px">不变：<code>app:shared</code>（ViewModel、Prefs、Room）、<code>player:core</code>、<code>player:shared</code>、<code>bili-api</code>。<code>app:ui</code> 内按 <code>designsystem/</code>（主题、形状、动效、焦点、组件）、<code>adaptive/</code>（DeviceProfile、外壳）、<code>feature/home|shorts|video|player|search|pgc|dynamic|me|settings|login</code> 分包。</p>`,
};
</script>
