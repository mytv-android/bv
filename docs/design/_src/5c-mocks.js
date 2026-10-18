<script>
'use strict';
/* ---------- SETTINGS ---------- */
const SET_CATS = [['palette', '外观与主题', '种子色、配色风格、深色模式', 'primary'], ['play', '播放', '默认画质、编码、音质、倍速', 'tertiary'], ['comment', '弹幕', '类型、透明度、区域、防挡', 'secondary'], ['cc', '字幕', '字号、背景、底部边距', 'primary'], ['wifi', '网络与代理', '代理服务器、官方 CDN、测速', 'tertiary'], ['shield', '账号与安全', '多账号、用户锁、无痕模式', 'secondary'], ['storage', '存储', '图片缓存、日志', 'primary'], ['info', '关于', '版本、更新、解码器信息', 'tertiary']];
const catRow = ([i, t, s, c], on, f) => `<div class="m-li${on ? ' on' : ''}${f ? ' f' : ''}"${f ? ' data-f' : ''}${f && on ? ' data-start' : ''}><span class="ic" style="background:var(--${c}-container);color:var(--on-${c}-container);clip-path:var(--shape-cookie9);border-radius:50%">${ic(i)}</span><span class="tx"><b>${t}</b><span>${s}</span></span></div>`;
const swatches = () => `<div style="display:flex;gap:10px;align-items:center">${['#FB7299', '#23ADE5', '#F2A53A', '#5BB974', '#9B7BF0'].map((c, i) => `<span style="width:${i ? 32 : 36}px;height:${i ? 32 : 36}px;background:${c};border-radius:${i ? '50%' : '30%'};${i ? '' : 'transform:rotate(45deg);box-shadow:0 0 0 2px var(--surface-container-low),0 0 0 4px var(--on-surface)'}"></span>`).join('')}<span class="m-ib t">${ic('add')}</span></div>`;
const segRow = (items, on) => `<div class="m-bg">${items.map((t, i) => `<div class="${i === on ? 'on' : ''}" style="font-size:12px;padding:10px 4px">${t}</div>`).join('')}</div>`;
const appearance = f => `<div class="m-col" style="gap:8px"><b style="font-size:13px;color:var(--on-surface-variant)">主题来源</b>${segRow(['系统壁纸', '自选种子色', '跟随内容'], 1)}</div>
  <div class="m-col" style="gap:8px"><b style="font-size:13px;color:var(--on-surface-variant)">种子色</b>${swatches()}</div>
  <div class="m-col" style="gap:8px"><b style="font-size:13px;color:var(--on-surface-variant)">配色风格</b><div class="m-chips" style="flex-wrap:wrap">${['TonalSpot', 'Vibrant', 'Expressive', 'Fidelity', 'Content', 'Monochrome'].map((t, i) => `<span class="m-chip${i === 1 ? ' on' : ''}${f ? ' f' : ''}"${f ? ' data-f' : ''}>${t}</span>`).join('')}</div></div>
  <div class="m-col" style="gap:8px"><div style="display:flex;justify-content:space-between;font-size:13px"><b style="color:var(--on-surface-variant)">对比度</b><span>标准</span></div><div class="m-sl"><span class="a" style="width:30%"></span><span class="h"></span><span class="b"></span></div></div>
  <div class="m-col" style="gap:8px"><b style="font-size:13px;color:var(--on-surface-variant)">深色模式</b>${segRow(['跟随系统', '浅色', '深色', '影院纯黑'], 0)}</div>
  <div style="display:flex;justify-content:space-between;align-items:center"><div class="m-col"><b style="font-size:14px">详情页使用封面颜色</b><span class="m-meta">详情、播放器、刷刷随视频变色</span></div><span class="m-sw on"></span></div>
  <div style="display:flex;justify-content:space-between;align-items:center"><div class="m-col"><b style="font-size:14px">减少动效</b><span class="m-meta">关闭形状变形和弹簧回弹</span></div><span class="m-sw"></span></div>`;
function setPhone() {
  return sb() + `<div style="padding:6px 20px 12px"><b style="font:800 26px var(--font-display)">设置</b></div><div style="padding:0 8px;display:flex;flex-direction:column;gap:2px">${SET_CATS.map(c => catRow(c)).join('')}</div>`;
}
function setTablet() {
  return `<div style="display:flex;height:100%;padding:20px 24px;gap:20px"><div class="m-col" style="width:380px;flex:none;gap:2px"><div style="display:flex;gap:8px;align-items:center;padding:0 8px 12px"><span class="m-ib">${ic('back')}</span><b style="font:800 24px var(--font-display)">设置</b></div>${SET_CATS.map((c, i) => catRow(c, i === 0)).join('')}</div>
    <div class="m-fill" style="border-radius:28px;background:var(--surface-container-low);padding:26px 30px;display:flex;flex-direction:column;gap:22px;overflow:hidden"><b style="font:800 24px var(--font-display)">外观与主题</b>${appearance()}</div></div>`;
}
function setTv() {
  return `<div style="position:absolute;inset:0;display:flex;padding:27px 48px;gap:24px"><div class="m-col" style="width:300px;flex:none;gap:2px"><b style="font:800 24px var(--font-display);padding:0 12px 12px">设置</b>${SET_CATS.slice(0, 7).map((c, i) => catRow(c, i === 0, 1)).join('')}</div>
    <div class="m-fill" style="border-radius:28px;background:var(--surface-container);padding:22px 26px;display:flex;flex-direction:column;gap:16px;overflow:hidden"><b style="font:800 20px var(--font-display)">外观与主题</b>${appearance(1)}
    <div class="m-col" style="gap:8px"><div style="display:flex;justify-content:space-between;font-size:13px"><b style="color:var(--on-surface-variant)">界面缩放（电视）</b><span class="num">960dp 基准 · 100%</span></div><div class="m-sl"><span class="a" style="width:50%"></span><span class="h"></span><span class="b"></span></div></div></div></div>` + tvhint('← 返回分类 · → 进入详情');
}

/* ---------- LOGIN ---------- */
function qrGrid(seed) {
  const r = rng(seed), n = 21, cells = [];
  const finder = (x, y) => [[0, 0], [n - 7, 0], [0, n - 7]].some(([fx, fy]) => { const dx = x - fx, dy = y - fy; if (dx < 0 || dy < 0 || dx > 6 || dy > 6) return false; return dx === 0 || dy === 0 || dx === 6 || dy === 6 || (dx > 1 && dx < 5 && dy > 1 && dy < 5); });
  const inFinderZone = (x, y) => (x < 8 && y < 8) || (x > n - 9 && y < 8) || (x < 8 && y > n - 9);
  for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) cells.push(inFinderZone(x, y) ? finder(x, y) : r() > .52);
  return `<div class="m-qr">${cells.map(on => `<i${on ? '' : ' class="o"'}></i>`).join('')}</div>`;
}
const field = (label, val, extra = '') => `<div style="display:flex;flex-direction:column;gap:4px;padding:8px 16px;border-radius:16px 16px 4px 4px;background:var(--surface-container-highest);box-shadow:inset 0 -2px 0 var(--primary)"><span style="font-size:11px;color:var(--primary)">${label}</span><div style="display:flex;justify-content:space-between;align-items:center"><span class="num" style="font-size:16px">${val}</span>${extra}</div></div>`;
function loginPhone() {
  return sb() + `<div style="padding:40px 24px;display:flex;flex-direction:column;gap:18px"><div class="m-av p lg" style="width:64px;height:64px">B</div><b style="font:800 28px/1.2 var(--font-display)">登录哔哩哔哩</b><p style="font-size:14px;color:var(--on-surface-variant);margin:0">登录后同步历史、收藏和追番，三台设备进度一致。</p>
  ${field('手机号', '+86 138 0013 8000')}${field('验证码', '6 2 9 _ _ _', '<span style="font-size:12px;color:var(--primary);font-weight:600">52 秒后重发</span>')}
  <span class="m-btn fill lg" style="width:100%">登录</span><div style="display:flex;align-items:center;gap:10px;color:var(--on-surface-variant);font-size:12px"><span class="m-fill" style="height:1px;background:var(--outline-variant)"></span>或<span class="m-fill" style="height:1px;background:var(--outline-variant)"></span></div>
  <span class="m-btn t lg" style="width:100%">${ic('qr')}在另一台设备上扫码</span><span class="m-btn o" style="width:100%">导入电视上已登录的账号</span></div>`;
}
const loginWide = tv => `<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:${tv ? 56 : 80}px;padding:0 48px"><div class="m-col" style="align-items:center;gap:16px;width:${tv ? 260 : 340}px"><div style="width:${tv ? 230 : 300}px">${qrGrid(7)}</div><b style="font-size:${tv ? 15 : 17}px">用哔哩哔哩 App 扫码登录</b><span class="m-meta" style="font-size:12px">二维码 3 分钟后刷新 · 已扫码请在手机上确认</span></div>
  <div class="m-col" style="gap:${tv ? 12 : 16}px;width:${tv ? 320 : 400}px"><b style="font:800 ${tv ? 26 : 32}px/1.2 var(--font-display)">登录哔哩哔哩</b>${['打开哔哩哔哩 App，点右上角扫一扫', '对准左侧二维码', '在手机上点「确认登录」'].map((t, i) => `<div style="display:flex;gap:12px;align-items:center;font-size:14px"><span class="m-av ${i === 1 ? 's' : 'p'}" style="width:30px;height:30px;font-size:13px">${i + 1}</span>${t}</div>`).join('')}
  <div style="height:1px;background:var(--outline-variant);margin:6px 0"></div><span class="m-btn t${tv ? ' f' : ''}"${tv ? ' data-f data-start' : ''} style="align-self:flex-start">${ic('phone')}改用手机号和验证码</span><span class="m-btn o${tv ? ' f' : ''}"${tv ? ' data-f' : ''} style="align-self:flex-start">${ic('person')}切换到已保存的账号</span></div></div>`;
const loginTablet = () => loginWide(false);
const loginTv = () => loginWide(true) + tvhint();

/* ---------- registry: screens × devices + adaptive rules ---------- */
const SCREENS = [
  { id: 'home', name: '首页', fn: { phone: homePhone, tablet: homeTablet, tv: homeTv },
    comps: ['AppBarWithSearch', 'SecondaryTabRow', 'HorizontalMultiBrowseCarousel', 'HorizontalCenteredHeroCarousel', 'ShortNavigationBar', 'WideNavigationRail'],
    rules: { phone: ['顶部是头像 + 搜索框（AppBarWithSearch），下面是推荐 / 热门 / 分区三个标签。', '精选用多尺寸轮播：一张大图露出下一张的边缘，暗示可以横滑。', '「继续观看」横向一行，卡片底部带进度条；推荐流 2 列。'],
             tablet: ['左侧导航轨，搜索做成导航轨顶部的 FAB。', '精选改为居中大图轮播，左右两张竖图露边。', '推荐流 4–5 列，随宽度增减；分区标签横向铺开不折叠。'],
             tv: ['全幅背景：当前精选的封面占右侧 74%，左和下渐隐到背景色。', '焦点在「播放」按钮上；向下进入内容行，行内枢轴滚动，焦点保持在左侧 15% 处。', '导航轨默认只有图标，焦点移进去时展开为带文字的模态导航轨。'] } },
  { id: 'shorts', name: '刷刷', fn: { phone: shortsPhone, tablet: shortsTablet, tv: shortsTv },
    comps: ['VerticalPager', 'VerticalFloatingToolbar', 'LinearWavyProgressIndicator', 'MaterialShapes.Heart', 'ModalBottomSheet'],
    rules: { phone: ['全屏竖向翻页，底部导航变为半透明深色，不打断画面。', '右侧操作列：头像（带关注）、点赞、投币、收藏、评论、分享；点赞后按钮由圆形变为圆角方形。', '横屏视频居中显示，上下用封面模糊后的氛围光填满。'],
             tablet: ['视频保持 9:16 居中，左右是氛围光；右侧固定显示信息和评论（SupportingPane）。', '竖向浮动工具栏放操作，左侧上一条 / 下一条按钮方便鼠标和触控笔。'],
             tv: ['视频居中，左侧是播放队列缩略图，右侧是信息面板。', '↑↓ 换条，← → 快退快进；→ 移到右侧操作按钮。', '背景整屏氛围光，颜色来自当前视频封面，换条时颜色用弹簧动画过渡。'] } },
  { id: 'shortsFull', name: '刷刷 · 横屏全屏', fn: { phone: shortsPhone, tablet: shortsTablet, tv: shortsTvFull },
    comps: ['LinearWavyProgressIndicator', 'MaterialShapes.Heart'],
    rules: { phone: ['此原型针对电视横屏；手机形态见「刷刷」。'],
             tablet: ['此原型针对电视横屏；平板形态见「刷刷」。'],
             tv: ['横屏视频铺满 960×540 画面，不再缩在竖屏框里，也不再占用右侧信息区。', '标题、UP 主、5 个操作按钮、进度条和呼出导航提示都叠在画面底部。', '播放 3 秒无操作后全部隐藏，只剩画面；按任意方向键唤回信息，← 或手势额外唤出导航轨。横屏不显示播放队列，↑↓ 直接换条。'] } },
  { id: 'shortsFullIdle', name: '刷刷 · 横屏全屏（3 秒后）', fn: { phone: shortsPhone, tablet: shortsTablet, tv: shortsTvFullIdle },
    comps: ['LinearWavyProgressIndicator', 'MaterialShapes.Heart'],
    rules: { phone: ['此原型针对电视横屏；手机形态见「刷刷」。'],
             tablet: ['此原型针对电视横屏；平板形态见「刷刷」。'],
             tv: ['3 秒无操作后的状态：导航轨、标题、UP 主、操作按钮、进度条和提示全部隐藏，只剩视频画面。', '按任意方向键唤回底部信息；按 ← 或用手势同时唤出导航轨。', '暂停时信息保持显示。'] } },
  { id: 'shortsFullPortrait', name: '刷刷 · 竖屏全高', fn: { phone: shortsPhone, tablet: shortsTablet, tv: shortsTvFullPortrait },
    comps: ['LinearWavyProgressIndicator', 'MaterialShapes.Heart'],
    rules: { phone: ['此原型针对电视竖屏视频；手机形态见「刷刷」。'],
             tablet: ['此原型针对电视竖屏视频；平板形态见「刷刷」。'],
             tv: ['竖屏视频按屏幕高度铺满（304×540），上下不留框，左右用封面氛围光填满。', '左侧播放队列、右侧信息与操作按钮叠在氛围光上；进度条在画面底部。', '竖屏只隐藏导航轨，标题/操作/进度条常驻；← 或手势呼出导航，↑↓ 换条。'] } },
  { id: 'detail', name: '视频详情', fn: { phone: detailPhone, tablet: detailTablet, tv: detailTv },
    comps: ['ButtonGroup', 'ToggleButton', 'SupportingPaneScaffold', 'FilterChip', 'LinearWavyProgressIndicator'],
    rules: { phone: ['整页用封面颜色做局部主题（CoverTheme）。', '点赞 / 投币 / 收藏 / 稍后 / 分享是一组相连按钮（ButtonGroup），选中项变成胶囊形。', '简介和评论用标签切换；评论的回复用底部抽屉。'],
             tablet: ['左侧播放器 + 信息 + 选集，右侧评论和相关推荐。', '旋转为竖屏（Medium）时右栏收起，退化为手机布局。'],
             tv: ['沉浸式背景，焦点默认在「继续 P2 · 06:12」。', '选集和相关推荐做成两行，与首页行为一致。', 'UP 主按钮进入 UP 主空间，收藏按钮弹出收藏夹选择对话框。'] } },
  { id: 'player', name: '播放器', fn: { phone: playerPhone, tablet: playerTablet, tv: playerTv },
    comps: ['LinearWavyProgressIndicator', 'Slider (Expressive)', 'Switch', 'SideSheet', 'LoadingIndicator'],
    rules: { phone: ['横屏全屏：顶部标题和字幕 / 弹幕开关，底部波浪进度条、倍速、画质、选集。', '拖动进度时上方显示预览缩略图（videoshot）。', '暂停时中间按钮是圆角方形，播放时变回圆形。'],
             tablet: ['菜单从右侧滑出为侧边面板，不遮挡画面主体。', '弹幕设置用 Expressive 滑块，开关就在面板标题旁。'],
             tv: ['控制层只在按键后出现 3 秒；长按确认键或菜单键打开右侧菜单。', '菜单三栏：分类 / 项目 / 选项，沿用现有 MenuController 的数据结构。'] } },
  { id: 'search', name: '搜索', fn: { phone: searchPhone, tablet: searchTablet, tv: searchTv },
    comps: ['ExpandedFullScreenSearchBar', 'DockedSearchBar', 'FilterChip', 'ListItem'],
    rules: { phone: ['全屏搜索：输入时显示联想，空闲时显示历史和热搜。', '热搜前三名用 primary / tertiary / secondary 三种强调色。'],
             tablet: ['结果页顶部一排分类：综合、视频、番剧、影视、UP 主。', '综合结果把匹配的 UP 主和影视单独成块，视频 4 列。'],
             tv: ['左侧屏幕键盘（拼音首字母），中间联想，右侧热搜。', '下方实时出结果，按 ↓ 直接进入结果行。', '支持遥控器语音键。'] } },
  { id: 'pgc', name: '影视', fn: { phone: pgcPhone, tablet: pgcTablet, tv: pgcTv },
    comps: ['HorizontalMultiBrowseCarousel', 'ButtonGroup', 'FilterChip', 'Card'],
    rules: { phone: ['一级入口，包含番剧、国创、电影、电视剧、纪录片、综艺。', '竖海报轮播 + 一周时间表，今天的日期按钮是圆角方形。'],
             tablet: ['横幅 + 「今天更新」侧栏，下方海报 6 列。', '索引和我的追番放在标题栏右侧。'],
             tv: ['整页用当前推荐番剧的封面颜色做局部主题。', '时间表、索引、我的追番做成焦点可达的标签。'] } },
  { id: 'dynamic', name: '动态', fn: { phone: dynPhone, tablet: dynTablet, tv: dynTv },
    comps: ['ListDetailPaneScaffold', 'Card', 'MaterialShapes (头像)', 'ImageViewer'],
    rules: { phone: ['顶部关注 UP 主头像行（Cookie 形状），下面是视频、图文混排的动态流。', '点图片进入现有的全屏图片查看器。'],
             tablet: ['列表-详情：左侧动态流，右侧选中动态的内容和评论。'],
             tv: ['横向卡片行，只展示视频动态和图文动态的封面，评论在详情里看。'] } },
  { id: 'me', name: '我的', fn: { phone: mePhone, tablet: meTablet, tv: meTv },
    comps: ['Card', 'ListItem', 'ListDetailPaneScaffold', 'MaterialShapes (等级徽章)'],
    rules: { phone: ['头像、等级、关注 / 粉丝 / 硬币；四个彩色入口卡片分别用 primary、tertiary、secondary 容器色。', '「稍后再看」补齐（手机端现在是空操作）。'],
             tablet: ['列表-详情：左侧入口列表，右侧直接展开历史 / 收藏等内容。'],
             tv: ['入口卡片可聚焦；账号切换和用户锁放在顶部。'] } },
  { id: 'settings', name: '设置', fn: { phone: setPhone, tablet: setTablet, tv: setTv },
    comps: ['ListDetailPaneScaffold', 'ButtonGroup', 'Slider', 'Switch'],
    rules: { phone: ['八个分类，图标容器用 Cookie 形状并轮换三种强调色。', '合并两端的设置项：手机端从 3 类补到 8 类。'],
             tablet: ['列表-详情，「外观与主题」包含主题来源、种子色、配色风格、对比度、深色模式。'],
             tv: ['同样的列表-详情，← 回分类、→ 进详情；额外有界面缩放（以 960dp 为基准）。'] } },
  { id: 'login', name: '登录', fn: { phone: loginPhone, tablet: loginTablet, tv: loginTv },
    comps: ['TextField', 'Button', 'MaterialShapes 二维码（现有 m3qrcode）'],
    rules: { phone: ['默认短信登录，下方提供「在另一台设备上扫码」和导入电视账号。'],
             tablet: ['默认二维码登录，右侧是步骤说明和改用短信的入口。'],
             tv: ['与平板同一布局，二维码更大；焦点默认在「改用手机号」。'] } },
];
</script>
