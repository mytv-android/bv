<script>
'use strict';
/* ---------- mockup building blocks (dp units) ---------- */
const DEV = { phone: [360, 780], phoneL: [780, 360], tablet: [1200, 800], tv: [960, 540] };
const NAV = [['home', '首页'], ['shorts', '刷刷'], ['movie', '影视'], ['dynamic', '动态'], ['person', '我的']];
const sb = light => `<div class="m-sb" style="${light ? 'color:#fff' : ''}"><span class="num">9:41</span><span style="display:flex;gap:6px;align-items:center;font-size:11px">5G <i></i></span></div>`;
const fAttr = o => o && o.f ? ' data-f' : '';
function vcard(v, o = {}) {
  return `<div class="m-vc${o.f ? ' f' : ''}${o.lg ? ' lg' : ''}"${fAttr(o)} style="${o.w ? `width:${o.w}px;flex:none;` : ''}">
    <div class="m-cv">${img(v.c)}${o.stat ? `<span class="stat">▶ ${v.v}　弹 ${v.dm}</span>` : ''}<span class="dur">${v.d}</span>${o.prog ? `<span class="prog" style="width:${o.prog}%"></span>` : ''}</div>
    <div class="tt m-t2">${v.t}</div>${o.noMeta ? '' : `<div class="m-meta"><span class="m-t1">${v.up}</span><span>${v.v}播放</span></div>`}</div>`;
}
function poster(p, o = {}) {
  return `<div class="m-vc${o.f ? ' f' : ''}"${fAttr(o)} style="${o.w ? `width:${o.w}px;flex:none;` : ''}"><div class="m-cv poster">${img(p.c, 'S')}<span class="badge">${p.r}</span></div><div class="tt m-t1">${p.t}</div><div class="m-meta">${p.s}</div></div>`;
}
const listRow = v => `<div style="display:flex;gap:10px;min-width:0"><div class="m-cv" style="width:128px;flex:none;border-radius:12px">${img(v.c)}<span class="dur">${v.d}</span></div><div class="m-col" style="gap:6px;min-width:0"><div class="m-t2" style="font-size:13px;font-weight:500">${v.t}</div><div class="m-meta">${v.up} · ${v.v}播放</div></div></div>`;
const nbar = (active, dark) => `<div class="m-nbar${dark ? ' dark' : ''}"${dark ? ' style="color:#fff"' : ''}>${NAV.map(([i, l]) => `<div class="${l === active ? 'on' : ''}"${dark && l !== active ? ' style="color:rgb(255 255 255 / .7)"' : ''}><span class="pi">${ic(i)}</span>${l}</div>`).join('')}</div>`;
const rail = (active, wide) => `<div class="m-rail${wide ? ' wide' : ''}">${wide ? '' : `<div class="it"><span class="pi">${ic('menu')}</span></div>`}<div class="fab">${ic('search')}${wide ? '搜索' : ''}</div>${NAV.map(([i, l]) => `<div class="it${l === active ? ' on' : ''}"><span class="pi">${ic(i)}</span>${l}</div>`).join('')}<div class="sp"></div><div class="it"><span class="pi">${ic('settings')}</span>设置</div></div>`;
const tvrail = active => `<div class="m-tvrail"><div class="m-av p" style="width:36px;height:36px">阿</div><div style="height:10px"></div>${[['search', '搜索'], ...NAV].map(([i, l]) => `<div class="it f${l === active ? ' on' : ''}" data-f title="${l}">${ic(i)}<span class="lb">${l}</span></div>`).join('')}<div class="sp"></div><div class="it f" data-f title="设置">${ic('settings')}<span class="lb">设置</span></div></div>`;
const tvTop = (chips, active) => `<div class="m-abs" style="left:96px;top:24px;right:48px;display:flex;justify-content:space-between;align-items:center;z-index:2"><div class="m-chips" style="overflow:visible">${chips.map(c => `<span class="m-chip f${c === active ? ' on' : ''}" data-f>${c}</span>`).join('')}</div><div class="num" style="font:600 15px var(--font-display);color:var(--on-surface-variant)">20:46</div></div>`;
const tvhint = t => `<div class="m-tvhint">${t || '方向键移动焦点 · 确认键打开'}</div>`;
function wavy(frac, w, col = 'var(--primary)', rest = 'rgb(255 255 255 / .35)') {
  const x1 = frac * w; let d = 'M0 6';
  for (let x = 2; x <= x1; x += 2) d += `L${x} ${(6 + Math.sin(x / 18 * 2 * Math.PI) * 2.6).toFixed(2)}`;
  return `<svg class="m-wavy" viewBox="0 0 ${w} 12" style="width:${w}px"><path d="${d}" stroke="${col}" stroke-width="3.5"/><path d="M${(x1 + 8).toFixed(0)} 6H${w - 4}" stroke="${rest}" stroke-width="3.5"/><circle cx="${w - 2}" cy="6" r="2" fill="${col}"/></svg>`;
}
const comment = (n, name, txt, like) => `<div style="display:flex;gap:10px;min-width:0"><div class="m-av s" style="width:30px;height:30px;font-size:12px">${n}</div><div class="m-col" style="gap:3px;min-width:0"><b style="font-size:12px;color:var(--on-surface-variant)">${name}</b><div style="font-size:13px;line-height:1.5">${txt}</div><div class="m-meta">${ic('like')} ${like}　回复</div></div></div>`;
const COMMENTS = [['鹿', '鹿鸣呦呦', '22:47 那一段极光直接炸开，耳机里风声都起鸡皮疙瘩了', '3.1万'], ['青', '青山不改', '请问 UP 用的什么相机？夜景噪点控制得太好了', '8721'], ['橘', '橘子汽水', '去年九月去的，一次都没看到，今年再冲', '4402'], ['K', 'Kp指数观察员', '那天 Kp 到了 7，冰岛全境可见，运气真的好', '1980']];

/* ---------- HOME ---------- */
function homePhone() {
  return sb() + `<div style="display:flex;gap:10px;align-items:center;padding:6px 16px 10px"><div class="m-av p" style="width:40px;height:40px">阿</div><div class="m-search m-fill">${ic('search')}<span>冰岛极光 4K</span><span class="m-ib" style="width:36px;height:36px">${ic('mic')}</span></div></div>
  <div class="m-tabs"><div class="m-tab on">推荐</div><div class="m-tab">热门</div><div class="m-tab">分区</div></div>
  <div style="padding:14px 16px 0;display:flex;gap:8px"><div style="flex:1;position:relative;height:196px;border-radius:28px;overflow:hidden">${img(0, 'W')}<div class="m-scrim-b"></div><div class="m-abs" style="left:16px;bottom:14px;right:16px;color:#fff"><div class="m-eyebrow" style="color:#fff;opacity:.85">今日精选</div><div class="m-t2" style="font:800 18px/1.25 var(--font-display);margin-top:4px">冰岛 14 天环岛自驾，追到了今年最强的一场极光</div></div></div><div style="width:52px;height:196px;border-radius:20px;overflow:hidden;flex:none">${img(6, 'P')}</div></div>
  <div class="m-h" style="padding:16px 16px 0">继续观看 <small>全部</small></div>
  <div class="m-row" style="padding:10px 16px 0;gap:10px;overflow:hidden">${vcard(VIDS[2], { w: 150, prog: 62, noMeta: true })}${vcard(VIDS[3], { w: 150, prog: 30, noMeta: true })}${vcard(VIDS[5], { w: 150, prog: 80, noMeta: true })}</div>
  <div class="m-h" style="padding:14px 16px 0">为你推荐</div>
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;padding:10px 16px">${[4, 6, 1, 7].map(i => vcard(VIDS[i])).join('')}</div>` + nbar('首页');
}
function homeTablet() {
  return `<div style="display:flex;height:100%">${rail('首页')}<div class="m-fill m-col" style="padding:20px 28px 0 8px;gap:18px;overflow:hidden">
    <div style="display:flex;align-items:center;gap:16px"><div class="m-search" style="width:440px;flex:none">${ic('search')}<span>搜索视频、番剧、UP 主</span><span class="m-ib">${ic('mic')}</span></div><div class="m-chips m-fill"><span class="m-chip on">推荐</span><span class="m-chip">热门</span><span class="m-chip">分区</span><span class="m-chip">动画</span><span class="m-chip">知识</span><span class="m-chip">美食</span><span class="m-chip">户外</span></div><div class="m-av p" style="width:40px;height:40px">阿</div></div>
    <div style="display:flex;gap:12px;height:300px;flex:none"><div style="width:110px;border-radius:28px;overflow:hidden;flex:none">${img(6, 'P')}</div>
      <div class="m-fill" style="position:relative;border-radius:28px;overflow:hidden">${img(0, 'W')}<div class="m-scrim-b"></div><div class="m-abs" style="left:28px;bottom:24px;right:28px;color:#fff;display:flex;flex-direction:column;gap:8px"><div class="m-eyebrow" style="color:#fff;opacity:.85">今日精选 · 纪录</div><div style="font:800 34px/1.1 var(--font-display);font-variation-settings:'wdth' 115">冰岛 14 天环岛，追到今年最强极光</div><div style="font-size:13px;opacity:.85">旅行的阿树 · 186.4万播放 · 4K HDR · 18:42</div><div style="display:flex;gap:10px;margin-top:6px"><span class="m-btn fill">${ic('play')}播放</span><span class="m-btn t">${ic('later')}稍后再看</span></div></div></div>
      <div style="width:110px;border-radius:28px;overflow:hidden;flex:none">${img(1, 'P')}</div></div>
    <div class="m-h">继续观看 <small>全部</small></div>
    <div class="m-row" style="gap:16px;overflow:hidden;flex:none">${[2, 3, 5, 4, 7].map((i, k) => vcard(VIDS[i], { w: 196, prog: [62, 30, 80, 45, 15][k], noMeta: true })).join('')}</div>
    <div class="m-h">为你推荐</div>
    <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:16px">${[4, 6, 1, 7, 3].map(i => vcard(VIDS[i])).join('')}</div>
  </div></div>`;
}
function homeTv() {
  return `<div class="m-backdrop">${img(0, 'W')}</div>${tvrail('首页')}${tvTop(['推荐', '热门', '分区', '关注'], '推荐')}
  <div class="m-abs" style="left:96px;top:74px;width:430px;display:flex;flex-direction:column;gap:8px;z-index:2">
    <div class="m-eyebrow">今日精选 · 纪录</div><div class="m-display">冰岛 14 天环岛，<br>追到今年最强极光</div>
    <div class="m-meta" style="font-size:12px;gap:10px;color:var(--on-surface)"><span class="m-chip on" style="padding:1px 8px;font-size:11px">4K HDR</span>186.4万播放 · 18:42 · 旅行的阿树</div>
    <div class="m-t2" style="font-size:13px;line-height:1.6;color:var(--on-surface-variant)">从雷克雅未克出发，沿 1 号公路逆时针绕岛一圈。黑沙滩、冰河湖、蓝冰洞，最后在东部峡湾等到了 Kp 7 的极光。</div>
    <div style="display:flex;gap:12px;margin-top:6px"><div class="m-btn fill lg f" data-f data-start>${ic('play')}播放</div><div class="m-btn t lg f" data-f>${ic('later')}稍后再看</div><div class="m-ib t f" data-f style="width:52px;height:52px">${ic('like')}</div></div>
  </div>
  <div class="m-abs" style="left:96px;right:0;top:340px;z-index:2"><div class="m-h" style="padding-right:48px;font-size:15px">为你推荐 <small style="color:var(--on-surface-variant)">按 ↓ 查看更多行</small></div>
    <div class="m-row tvrow" style="gap:18px;overflow:hidden;padding:12px 48px 16px 6px">${[1, 2, 3, 4, 5, 6, 7].map(i => vcard(VIDS[i], { w: 180, f: 1, noMeta: true })).join('')}</div></div>` + tvhint();
}

/* ---------- SHORTS ---------- */
function shortSlide(s, landscape) {
  const media = landscape
    ? `<div class="m-ambient">${img(s.c, 'P')}</div><div class="m-abs" style="left:0;right:0;top:50%;transform:translateY(-60%);aspect-ratio:16/9">${img(s.c, 'W')}</div><div class="m-abs" style="left:0;right:0;top:calc(50% + 70px);text-align:center;font-size:11px;color:rgb(255 255 255 / .7)">横屏视频：居中 + 封面氛围光，横过来自动全屏</div>`
    : img(s.c, 'P');
  return `<div class="m-slide scope cinema" data-seed-cover="${s.c}">${media}<div class="m-scrim-b"></div>
    <div class="m-abs" style="top:30px;left:16px;right:12px;display:flex;justify-content:space-between;align-items:center;color:#fff"><b style="font:800 20px var(--font-display)">刷刷</b><span class="m-ib" style="color:#fff">${ic('search')}</span></div>
    <div class="m-acts m-abs" style="right:10px;bottom:150px"><div><div class="m-av p" style="width:46px;height:46px">${s.up[0]}</div></div><div><span class="b">${ic('like')}</span>${s.like}</div><div><span class="b">${ic('coin')}</span>投币</div><div><span class="b">${ic('star')}</span>${s.fav}</div><div><span class="b">${ic('comment')}</span>${s.cm}</div><div><span class="b">${ic('share')}</span>分享</div></div>
    <div class="m-abs" style="left:16px;right:76px;bottom:112px;color:#fff;display:flex;flex-direction:column;gap:6px"><b style="font-size:15px">@${s.up}</b><div class="m-t2" style="font-size:14px;line-height:1.45">${s.t}</div><div style="font-size:12px;opacity:.85">${s.tags}</div></div>
    <div class="m-abs" style="left:16px;right:16px;bottom:90px">${wavy(.42, 328)}</div></div>`;
}
function shortsPhone() {
  return `<div class="m-feed">${SHORTS.map((s, i) => shortSlide(s, i === 2)).join('')}</div><div class="m-abs" style="top:0;left:0;right:0;z-index:3">${sb(1)}</div>` + nbar('刷刷', 1);
}
function shortsTablet() {
  const s = SHORTS[0];
  return `<div style="display:flex;height:100%">${rail('刷刷')}<div class="m-fill" style="position:relative;overflow:hidden;background:#000"><div class="m-ambient">${img(s.c, 'P')}</div>
    <div class="m-abs scope cinema" data-seed-cover="${s.c}" style="left:50%;top:20px;height:760px;width:428px;margin-left:-214px;border-radius:28px;overflow:hidden">${img(s.c, 'P')}<div class="m-scrim-b"></div><div class="m-abs" style="left:20px;right:20px;bottom:30px;color:#fff;display:flex;flex-direction:column;gap:8px"><b>@${s.up}</b><div style="font-size:15px">${s.t}</div><div style="font-size:12px;opacity:.85">${s.tags}</div>${wavy(.42, 388)}</div></div>
    <div class="m-ftb v m-abs scope cinema" data-seed-cover="${s.c}" style="left:calc(50% + 232px);bottom:40px"><span class="m-ib" style="background:var(--primary);color:var(--on-primary)">${ic('like')}</span><span class="m-ib">${ic('coin')}</span><span class="m-ib">${ic('star')}</span><span class="m-ib">${ic('share')}</span></div>
    <div class="m-ftb v m-abs" style="left:calc(50% - 290px);top:50%;transform:translateY(-50%)"><span class="m-ib">${ic('up')}</span><span class="m-ib">${ic('down')}</span></div></div>
    <div style="width:380px;flex:none;background:var(--surface-container-low);padding:24px 20px;display:flex;flex-direction:column;gap:16px;overflow:hidden">
      <div style="display:flex;gap:12px;align-items:center"><div class="m-av p" style="width:44px;height:44px">夜</div><div class="m-col m-fill"><b>${s.up}</b><span class="m-meta">粉丝 23.4万 · 312 个视频</span></div><span class="m-btn fill" style="height:36px;padding:0 16px">${ic('add')}关注</span></div>
      <div style="font:700 17px/1.4 var(--font-body)">${s.t}</div><div class="m-meta">${s.like} 点赞 · ${s.cm} 评论 · 今天 05:12</div>
      <div class="m-tabs" style="padding:0"><div class="m-tab on">评论 ${s.cm}</div><div class="m-tab">相关</div></div>
      ${COMMENTS.map(c => comment(...c)).join('')}</div></div>`;
}
function shortsTv() {
  const s = SHORTS[0];
  return `<div class="m-ambient">${img(s.c, 'P')}</div><div class="m-abs" style="inset:0;background:color-mix(in srgb, var(--surface) 50%, transparent)"></div>${tvrail('刷刷')}
  <div class="m-abs" style="left:110px;top:60px;display:flex;flex-direction:column;gap:10px;z-index:2">${[11, 10, 0, 6].map((c, i) => `<div style="width:54px;height:96px;border-radius:12px;overflow:hidden;${i === 1 ? 'box-shadow:0 0 0 3px var(--primary)' : 'opacity:.55'}">${img(c, 'P')}</div>`).join('')}</div>
  <div class="m-abs scope cinema f g" data-f data-start data-seed-cover="${s.c}" style="left:300px;top:27px;width:273px;height:486px;border-radius:24px;overflow:hidden;z-index:2">${img(s.c, 'P')}<div class="m-scrim-b"></div><div class="m-abs" style="left:14px;right:14px;bottom:14px">${wavy(.42, 245)}</div></div>
  <div class="m-abs scope cinema" data-seed-cover="${s.c}" style="left:600px;top:60px;width:312px;display:flex;flex-direction:column;gap:12px;z-index:2">
    <div style="display:flex;gap:10px;align-items:center"><div class="m-av p" style="width:40px;height:40px">夜</div><div class="m-col m-fill"><b>${s.up}</b><span class="m-meta">粉丝 23.4万</span></div><span class="m-btn t f" data-f style="height:34px;padding:0 14px">${ic('add')}关注</span></div>
    <div style="font:800 22px/1.25 var(--font-display)">${s.t}</div><div class="m-meta" style="font-size:12px">${s.tags}　${s.like} 点赞</div>
    <div style="display:flex;gap:8px">${[['like', 1], ['coin'], ['star'], ['comment'], ['info']].map(([n, on]) => `<span class="m-ib f${on ? '' : ' t'}" data-f style="${on ? 'background:var(--primary);color:var(--on-primary);border-radius:16px' : ''}">${ic(n)}</span>`).join('')}</div>
    <div style="display:flex;flex-direction:column;gap:12px;padding:14px;border-radius:20px;background:color-mix(in srgb, var(--surface-container) 80%, transparent)">${COMMENTS.slice(0, 2).map(c => comment(...c)).join('')}</div>
  </div>` + tvhint('↑↓ 换一条 · 确认键暂停 · 菜单键评论');
}

/* 横屏视频的操作状态：视频铺满 960×540，底部信息叠在渐变上；导航轨已自动隐藏 */
const shortAct = ([i, label, on], size = 46, radius = 12) => `<div style="display:flex;flex-direction:column;align-items:center;gap:4px">
  <span style="width:${size}px;height:${size}px;display:grid;place-items:center;border-radius:${on ? 16 : '50%'};background:${on ? 'var(--primary)' : 'rgb(255 255 255 / .18)'};color:${on ? 'var(--on-primary)' : '#fff'};backdrop-filter:blur(8px);-webkit-backdrop-filter:blur(8px)">${ic(i)}</span>
  <span style="font:600 11px var(--font-display);color:#fff;text-shadow:0 1px 3px rgb(0 0 0 / .65)">${label}</span></div>`;

function shortsTvFull() {
  const s = SHORTS[2];
  return `<div class="m-abs" style="inset:0;overflow:hidden;background:#000">${img(s.c, 'W')}</div>
  <div class="m-scrim-b" style="background:linear-gradient(0deg, rgb(0 0 0 / .86), transparent 46%), linear-gradient(180deg, rgb(0 0 0 / .5), transparent 26%)"></div>
  <div class="m-abs" style="left:96px;right:400px;bottom:96px;display:flex;flex-direction:column;gap:10px;z-index:2;color:#fff">
    <div style="display:flex;gap:12px;align-items:center">
      <div class="m-av p" style="width:44px;height:44px">${s.up[0]}</div>
      <div style="display:flex;flex-direction:column;gap:2px;min-width:0">
        <b style="font:700 16px var(--font-body)">@${s.up}</b>
        <span style="font-size:12px;opacity:.85">粉丝 23.4万 · ${s.tags}</span>
      </div>
      <span class="m-btn fill" style="height:34px;padding:0 16px;margin-left:6px">${ic('add')}关注</span>
    </div>
    <div style="font:800 32px/1.2 var(--font-display);letter-spacing:-.01em;max-width:560px;text-shadow:0 2px 12px rgb(0 0 0 / .5)">${s.t}</div>
    <div style="display:flex;gap:14px;font-size:13px;opacity:.85">${s.like} 点赞 · ${s.cm} 评论 · 今天 05:12</div>
  </div>
  <div class="m-abs" style="right:48px;bottom:96px;display:flex;gap:16px;align-items:flex-start;z-index:2">
    ${[['like', s.like, 1], ['coin', s.fav, 0], ['star', s.fav, 0], ['comment', s.cm, 0], ['info', '详情', 0]].map(a => shortAct(a)).join('')}
  </div>
  <div class="m-abs" style="left:96px;right:48px;bottom:52px;z-index:2">${wavy(.42, 816)}</div>
  ${tvhint('↑↓ 换一条 · ← 呼出导航 · 长按 ← 快退 · 菜单键评论')}`;
}

/* 竖屏视频：顶天立地（304×540 全高），左侧队列、右侧信息叠在氛围光上 */
function shortsTvFullPortrait() {
  const s = SHORTS[0];
  return `<div class="m-ambient" style="opacity:.65">${img(s.c, 'P')}</div>
  <div class="m-abs" style="inset:0;background:color-mix(in srgb, var(--surface) 58%, transparent)"></div>
  <div class="m-abs" style="left:300px;top:0;width:304px;height:540px;overflow:hidden;background:#000;z-index:2">
    ${img(s.c, 'P')}
    <div class="m-abs" style="inset:0;background:linear-gradient(0deg, rgb(0 0 0 / .72), transparent 34%)"></div>
    <div class="m-abs" style="left:14px;right:14px;bottom:14px">${wavy(.42, 276)}</div>
  </div>
  <div class="m-abs" style="left:110px;top:60px;display:flex;flex-direction:column;gap:10px;z-index:2">
    ${[11, 10, 0, 6].map((c, i) => `<div style="width:54px;height:96px;border-radius:12px;overflow:hidden;${i === 2 ? 'box-shadow:0 0 0 3px var(--primary)' : 'opacity:.55'}">${img(c, 'P')}</div>`).join('')}
  </div>
  <div class="m-abs" style="right:48px;top:60px;width:280px;display:flex;flex-direction:column;gap:12px;z-index:2;color:#fff">
    <div style="display:flex;gap:10px;align-items:center">
      <div class="m-av p" style="width:44px;height:44px">${s.up[0]}</div>
      <div style="display:flex;flex-direction:column;gap:2px;min-width:0">
        <b style="font:700 16px var(--font-body)">@${s.up}</b>
        <span style="font-size:12px;opacity:.85">粉丝 23.4万 · ${s.tags}</span>
      </div>
      <span class="m-btn fill" style="height:34px;padding:0 14px">${ic('add')}关注</span>
    </div>
    <div style="font:800 26px/1.25 var(--font-display);text-shadow:0 2px 12px rgb(0 0 0 / .5)">${s.t}</div>
    <div style="font-size:13px;opacity:.85">${s.like} 点赞 · ${s.cm} 评论 · 今天 05:12</div>
    <div style="display:flex;gap:8px">${[['like', s.like, 1], ['coin', s.fav, 0], ['star', s.fav, 0], ['comment', s.cm, 0], ['info', '详情', 0]].map(a => shortAct(a, 44)).join('')}</div>
  </div>
  ${tvhint('↑↓ 换一条 · ← 呼出导航 · 菜单键评论')}`;
}

/* 横屏播放 3 秒后：导航轨、标题/操作/进度条和提示全部隐藏，只剩画面 */
function shortsTvFullIdle() {
  const s = SHORTS[2];
  return `<div class="m-abs" style="inset:0;overflow:hidden;background:#000">${img(s.c, 'W')}</div>`;
}

/* ---------- VIDEO DETAIL ---------- */
const ACTIONS = [['like', '3.2万', 1], ['coin', '8961'], ['star', '1.4万'], ['later', '稍后'], ['share', '分享']];
const btnGroup = () => `<div class="m-bg">${ACTIONS.map(([i, l, on]) => `<div class="${on ? 'on' : ''}">${ic(i)}${l}</div>`).join('')}</div>`;
const EPS = ['P1 雷克雅未克', 'P2 黑沙滩', 'P3 冰河湖', 'P4 蓝冰洞', 'P5 东部峡湾'];
function detailPhone() {
  const v = VIDS[0];
  return `<div class="scope" data-seed-cover="0" style="height:100%;background:var(--surface)"><div style="background:#000;color:#fff">${sb(1)}</div>
  <div class="m-video" style="height:203px">${img(0)}<div class="m-scrim-b"></div><span class="m-abs m-ib" style="left:4px;top:2px;color:#fff">${ic('back')}</span><span class="m-abs m-ib" style="right:4px;top:2px;color:#fff">${ic('more')}</span><div class="m-abs" style="left:12px;right:12px;bottom:8px;display:flex;align-items:center;gap:10px;color:#fff"><span style="width:20px">${ic('pause')}</span><div class="m-fill">${wavy(.33, 230, 'var(--inverse-primary)')}</div><span class="num" style="font-size:11px">06:12</span><span style="width:20px">${ic('full')}</span></div></div>
  <div class="m-tabs"><div class="m-tab on">简介</div><div class="m-tab">评论 2341</div><div class="m-fill"></div><span class="m-chip" style="margin:5px 0;padding:4px 12px;font-size:12px">发弹幕</span></div>
  <div style="padding:14px 16px;display:flex;flex-direction:column;gap:12px">
    <div style="display:flex;gap:10px;align-items:center"><div class="m-av p" style="width:40px;height:40px">阿</div><div class="m-col m-fill"><b style="font-size:14px">${v.up}</b><span class="m-meta">粉丝 58.2万</span></div><span class="m-btn fill" style="height:36px;padding:0 16px">${ic('add')}关注</span></div>
    <div style="font:700 16px/1.45 var(--font-body)" class="m-t2">${v.t}</div>
    <div class="m-meta">${v.v}播放 · ${v.dm}弹幕 · 10-02 · BV1xK4y1C7Ae</div>
    ${btnGroup()}
    <div class="m-h">选集 <small>5P ›</small></div><div class="m-chips">${EPS.map((e, i) => `<span class="m-chip${i === 0 ? ' on' : ''}">${e}</span>`).join('')}</div>
    <div class="m-h">相关推荐</div>${[3, 5, 6].map(i => listRow(VIDS[i])).join('')}
  </div></div>`;
}
function detailTablet() {
  const v = VIDS[0];
  return `<div class="scope" data-seed-cover="0" style="height:100%;display:flex;gap:24px;padding:20px 24px;background:var(--surface)">
    <div class="m-col" style="width:720px;flex:none;gap:14px"><div style="display:flex;align-items:center;gap:8px"><span class="m-ib">${ic('back')}</span><b style="font-size:15px">视频详情</b></div>
      <div class="m-video" style="height:405px;border-radius:28px">${img(0, 'W')}<div class="m-scrim-b"></div><div class="m-abs" style="left:20px;right:20px;bottom:14px;display:flex;align-items:center;gap:12px;color:#fff"><span>${ic('pause')}</span><div class="m-fill">${wavy(.33, 560, 'var(--inverse-primary)')}</div><span class="num" style="font-size:12px">06:12 / 18:42</span><span>${ic('full')}</span></div></div>
      <div style="font:800 22px/1.3 var(--font-display)">${v.t}</div>
      <div style="display:flex;gap:12px;align-items:center"><div class="m-av p" style="width:44px;height:44px">阿</div><div class="m-col"><b>${v.up}</b><span class="m-meta">粉丝 58.2万 · ${v.v}播放 · 10-02</span></div><span class="m-btn fill" style="height:36px;margin-left:8px">${ic('add')}关注</span><div class="m-fill"></div><div style="width:330px">${btnGroup()}</div></div>
      <div class="m-chips">${EPS.map((e, i) => `<span class="m-chip${i === 0 ? ' on' : ''}">${e}</span>`).join('')}</div></div>
    <div class="m-col m-fill" style="background:var(--surface-container-low);border-radius:28px;padding:16px 18px;gap:14px;overflow:hidden"><div class="m-tabs" style="padding:0"><div class="m-tab on">评论 2341</div><div class="m-tab">相关推荐</div></div><div class="m-chips"><span class="m-chip on">最热</span><span class="m-chip">最新</span></div>${COMMENTS.map(c => comment(...c)).join('')}${listRow(VIDS[3])}</div></div>`;
}
function detailTv() {
  const v = VIDS[0];
  return `<div class="scope cinema" data-seed-cover="0" style="position:absolute;inset:0;background:var(--surface)"><div class="m-ambient">${img(0, 'W')}</div><div class="m-backdrop" style="height:70%">${img(0, 'W')}</div>
  <div class="m-abs" style="left:48px;top:30px;width:520px;display:flex;flex-direction:column;gap:10px;z-index:2">
    <div class="m-eyebrow">纪录 · 旅行 · 5P</div><div class="m-display m-t2" style="font-size:26px">${v.t}</div>
    <div style="display:flex;gap:10px;align-items:center"><div class="m-av p" style="width:32px;height:32px">阿</div><b style="font-size:13px">${v.up}</b><span class="m-meta">粉丝 58.2万 · ${v.v}播放 · 3.2万点赞 · 8961 投币</span></div>
    <div class="m-t2" style="font-size:13px;line-height:1.6;color:var(--on-surface-variant)">从雷克雅未克出发，沿 1 号公路逆时针绕岛一圈。全程 4K HDR 拍摄，第 5P 是东部峡湾等到的 Kp 7 极光。</div>
    <div style="display:flex;gap:10px;margin-top:4px;align-items:center"><div class="m-btn fill lg f" data-f data-start>${ic('play')}继续 P2 · 06:12</div>${[['like', 1], ['coin'], ['star'], ['later'], ['person']].map(([n, on]) => `<span class="m-ib f t" data-f style="width:48px;height:48px;${on ? 'background:var(--primary);color:var(--on-primary);border-radius:16px' : ''}">${ic(n)}</span>`).join('')}</div>
  </div>
  <div class="m-abs" style="left:48px;right:0;top:300px;z-index:2"><div class="m-h" style="font-size:15px">选集</div><div class="m-row tvrow" style="gap:14px;overflow:hidden;padding:10px 48px 12px 4px">${EPS.map((e, i) => `<div class="m-vc f" data-f style="width:170px;flex:none"><div class="m-cv">${img([0, 1, 5, 3, 0][i])}<span class="dur">${['03:40', '04:12', '03:55', '04:31', '02:24'][i]}</span>${i === 1 ? '<span class="prog" style="width:33%"></span>' : ''}</div><div class="tt m-t1">${e}</div></div>`).join('')}</div>
  <div class="m-h" style="font-size:15px;margin-top:2px">相关推荐</div></div></div>` + tvhint();
}

/* ---------- PLAYER ---------- */
const danmaku = [['那一段极光直接炸开', 40, 60], ['前方高能', 90, 300], ['这是 KP7 吗', 140, 120], ['UP 主的相机是什么', 60, 480], ['好想去冰岛', 180, 420]];
const dmHtml = sc => danmaku.map(([t, y, x]) => `<span class="m-dm" style="top:${y * sc}px;left:${x * sc}px">${t}</span>`).join('');
function playerPhone() {
  return `<div class="scope cinema" data-seed-cover="0" style="position:absolute;inset:0;background:#000"><div class="m-video" style="position:absolute;inset:0">${img(0, 'W')}</div><div class="m-scrim-b"></div>${dmHtml(1)}
  <div class="m-abs" style="top:12px;left:16px;right:16px;display:flex;align-items:center;gap:10px;color:#fff"><span>${ic('back')}</span><b class="m-fill m-t1" style="font-size:14px">P2 黑沙滩 · 冰岛 14 天环岛自驾</b><span>${ic('cc')}</span><span class="m-chip on" style="padding:3px 10px;font-size:12px">弹 开</span><span>${ic('more')}</span></div>
  <div class="m-abs" style="left:50%;top:50%;width:72px;height:72px;margin:-46px 0 0 -36px;border-radius:24px;background:var(--primary-container);color:var(--on-primary-container);display:grid;place-items:center">${ic('play')}</div>
  <div class="m-abs" style="left:300px;bottom:78px;width:140px;border-radius:14px;overflow:hidden;box-shadow:0 0 0 2px #fff">${img(1)}<div style="position:absolute;bottom:4px;width:100%;text-align:center;color:#fff;font:600 11px var(--font-display)">08:30</div></div>
  <div class="m-abs" style="left:20px;right:20px;bottom:12px;display:flex;flex-direction:column;gap:6px;color:#fff">${wavy(.45, 740)}<div style="display:flex;align-items:center;gap:12px;font-size:12px"><span>${ic('pause')}</span><span>${ic('next')}</span><span class="num">08:30 / 18:42</span><div class="m-fill"></div><span class="m-chip" style="color:#fff;box-shadow:inset 0 0 0 1px rgb(255 255 255 / .4);padding:3px 10px">选集</span><span class="m-chip" style="color:#fff;box-shadow:inset 0 0 0 1px rgb(255 255 255 / .4);padding:3px 10px">1.0×</span><span class="m-chip on" style="padding:3px 10px">1080P 高码率</span><span>${ic('full')}</span></div></div></div>`;
}
function playerTablet() {
  const sl = (name, val, pct) => `<div class="m-col" style="gap:8px"><div style="display:flex;justify-content:space-between;font-size:13px"><span>${name}</span><span class="num" style="color:var(--on-surface-variant)">${val}</span></div><div class="m-sl"><span class="a" style="width:${pct}%"></span><span class="h"></span><span class="b"></span></div></div>`;
  return `<div class="scope cinema" data-seed-cover="0" style="position:absolute;inset:0;background:#000"><div class="m-video" style="position:absolute;inset:0">${img(0, 'W')}</div><div class="m-scrim-b"></div>${dmHtml(1.6)}
  <div class="m-abs" style="left:28px;right:28px;bottom:20px;display:flex;flex-direction:column;gap:8px;color:#fff;width:800px">${wavy(.45, 800)}<div style="display:flex;gap:14px;align-items:center;font-size:13px"><span>${ic('pause')}</span><span>${ic('next')}</span><span class="num">08:30 / 18:42</span></div></div>
  <div class="m-abs" style="top:16px;right:16px;bottom:16px;width:340px;border-radius:28px;background:var(--surface-container);padding:22px;display:flex;flex-direction:column;gap:20px;color:var(--on-surface)">
    <div style="display:flex;justify-content:space-between;align-items:center"><b style="font:800 18px var(--font-display)">弹幕设置</b><span class="m-sw on"></span></div>
    <div class="m-chips" style="flex-wrap:wrap"><span class="m-chip on">滚动</span><span class="m-chip on">顶部</span><span class="m-chip">底部</span><span class="m-chip on">彩色</span><span class="m-chip">高级</span></div>
    ${sl('不透明度', '80%', 70)}${sl('显示区域', '1/2 屏', 45)}${sl('字号', '100%', 50)}${sl('速度', '适中', 55)}
    <div style="display:flex;justify-content:space-between;align-items:center;font-size:14px"><span>智能防挡（人像蒙版）</span><span class="m-sw on"></span></div></div></div>`;
}
function playerTv() {
  const opt = (t, on) => `<div class="m-li f${on ? ' on' : ''}" data-f style="padding:8px 12px">${on ? ic('check') : '<span style="width:24px"></span>'}<span class="tx"><b style="font-size:13px">${t}</b></span></div>`;
  return `<div class="scope cinema" data-seed-cover="0" style="position:absolute;inset:0;background:#000"><div class="m-video" style="position:absolute;inset:0">${img(0, 'W')}</div><div class="m-scrim-b"></div>
  <div class="m-abs" style="left:48px;top:27px;color:#fff"><div style="font:800 20px var(--font-display)">P2 黑沙滩</div><div style="font-size:12px;opacity:.8">冰岛 14 天环岛自驾 · 旅行的阿树</div></div>
  <div class="m-abs" style="left:48px;right:340px;bottom:27px;display:flex;flex-direction:column;gap:10px;color:#fff"><div style="display:flex;justify-content:space-between;font:600 12px var(--font-display)" class="num"><span>08:30</span><span>18:42</span></div>${wavy(.45, 572)}
    <div style="display:flex;gap:10px">${[['pause'], ['next'], ['dynamic'], ['comment'], ['cc'], ['tune', 1]].map(([n, on]) => `<span class="m-ib f t" data-f${on ? ' data-start' : ''} style="width:44px;height:44px">${ic(n)}</span>`).join('')}</div></div>
  <div class="m-abs" style="top:16px;right:16px;bottom:16px;width:300px;border-radius:28px;background:var(--surface-container);display:flex;color:var(--on-surface);overflow:hidden">
    <div class="m-col" style="width:96px;padding:16px 8px;gap:4px;background:var(--surface-container-high)">${['画面', '弹幕', '字幕', '其他'].map((t, i) => `<div class="m-li f${i === 0 ? ' on' : ''}" data-f style="padding:8px 10px;justify-content:center;font-size:13px">${t}</div>`).join('')}</div>
    <div class="m-col m-fill" style="padding:16px 10px;gap:2px"><div class="m-eyebrow" style="padding:0 12px 6px">分辨率</div>${opt('4K 超清')}${opt('1080P 高码率', 1)}${opt('1080P 高清')}${opt('720P')}<div class="m-eyebrow" style="padding:10px 12px 6px">编码</div>${opt('HEVC', 1)}${opt('AV1')}</div></div></div>` + tvhint('确认键暂停 · 长按确认 / 菜单键打开设置');
}
</script>
