<script>
'use strict';
/* ---------- SEARCH ---------- */
const HOT = ['冰岛极光 Kp7', '葬送的芙莉莲 第二季 第 8 话', '撒哈拉银河', '洪崖洞航拍', '自由潜蓝洞', '火山口熔岩湖'];
const hotList = n => HOT.slice(0, n).map((h, i) => `<div style="display:flex;gap:12px;align-items:center;font-size:13px;min-width:0"><span class="num" style="width:22px;height:22px;border-radius:8px;display:grid;place-items:center;font-weight:800;font-size:12px;${i < 3 ? `background:var(${['--primary', '--tertiary', '--secondary'][i]});color:var(${['--on-primary', '--on-tertiary', '--on-secondary'][i]})` : 'color:var(--on-surface-variant)'}">${i + 1}</span><span class="m-t1">${h}</span></div>`).join('');
const SUG = ['极光 4K', '极光 冰岛 自驾', '极光 延时摄影', '极光 预报 Kp 指数'];
function searchPhone() {
  return sb() + `<div style="padding:6px 12px 8px;display:flex;gap:6px;align-items:center"><span class="m-ib">${ic('back')}</span><div class="m-search m-fill" style="background:var(--surface-container-highest)"><span style="color:var(--on-surface)">极光<b style="color:var(--primary);font-weight:400">|</b></span><span class="m-ib" style="width:36px;height:36px">${ic('mic')}</span></div></div>
  <div style="display:flex;flex-direction:column;padding:0 4px">${SUG.map(s => `<div class="m-li" style="padding:10px 16px">${ic('search')}<span class="tx"><b style="font-weight:400">${s.replace('极光', '<b>极光</b>')}</b></span></div>`).join('')}</div>
  <div style="padding:12px 16px;display:flex;flex-direction:column;gap:12px"><div class="m-h">搜索历史 <small>清除</small></div><div class="m-chips" style="flex-wrap:wrap">${['芙莉莲', '冰岛', 'Compose 弹幕', '自由潜'].map(c => `<span class="m-chip">${c}</span>`).join('')}</div>
  <div class="m-h" style="margin-top:6px">大家都在搜</div><div style="display:grid;grid-template-columns:1fr 1fr;gap:14px 12px">${hotList(6)}</div></div>`;
}
function searchTablet() {
  return `<div style="display:flex;height:100%">${rail('')}<div class="m-fill m-col" style="padding:20px 28px 0 8px;gap:18px;overflow:hidden">
    <div style="display:flex;gap:16px;align-items:center"><div class="m-search" style="width:560px;background:var(--surface-container-highest)">${ic('search')}<span style="color:var(--on-surface)">极光</span><span class="m-ib">${ic('mic')}</span></div><div class="m-chips"><span class="m-chip on">综合</span><span class="m-chip">视频</span><span class="m-chip">番剧</span><span class="m-chip">影视</span><span class="m-chip">UP 主</span></div></div>
    <div style="display:flex;gap:16px;align-items:center;padding:16px;border-radius:24px;background:var(--surface-container)"><div class="m-av p lg">阿</div><div class="m-col m-fill" style="gap:4px"><b style="font-size:16px">旅行的阿树</b><span class="m-meta">粉丝 58.2万 · 216 个视频 · 极光、冰岛、北欧自驾</span></div><span class="m-btn fill">${ic('add')}关注</span><div class="m-row" style="gap:10px">${[0, 5, 3].map(i => `<div class="m-cv" style="width:120px">${img(i)}</div>`).join('')}</div></div>
    <div class="m-h">视频 <small>按播放量 ›</small></div>
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px">${[0, 2, 5, 6].map(i => vcard(VIDS[i], { stat: 1 })).join('')}</div>
    <div class="m-h">影视</div><div style="display:flex;gap:16px">${[1, 3].map(i => `<div style="display:flex;gap:14px;padding:12px;border-radius:20px;background:var(--surface-container-low);width:420px">${poster(PGCS[i], { w: 96 })}<div class="m-col" style="gap:6px"><b>${PGCS[i].t}</b><span class="m-meta">${PGCS[i].s}</span><span class="m-btn fill" style="height:34px;margin-top:8px;align-self:flex-start">${ic('play')}立即观看</span></div></div>`).join('')}</div>
  </div></div>`;
}
function searchTv() {
  const keys = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890'.split('');
  return `${tvrail('搜索')}<div class="m-abs" style="left:96px;top:27px;width:300px;display:flex;flex-direction:column;gap:12px;z-index:2">
    <div class="m-search" style="background:var(--surface-container-highest);height:52px">${ic('search')}<span style="color:var(--on-surface);font-size:16px">JIGUANG<b style="color:var(--primary);font-weight:400">|</b></span></div>
    <div style="display:grid;grid-template-columns:repeat(6,1fr);gap:6px">${keys.map((k, i) => `<div class="m-key f" data-f${i === 7 ? ' data-start' : ''}>${k}</div>`).join('')}</div>
    <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:6px"><div class="m-key f" data-f>空格</div><div class="m-key f" data-f>删除</div><div class="m-key f" data-f>清空</div></div>
    <div class="m-btn t f" data-f style="justify-content:flex-start">${ic('mic')}按住遥控器语音键说话</div></div>
  <div class="m-abs" style="left:430px;top:27px;width:190px;display:flex;flex-direction:column;gap:4px;z-index:2"><div class="m-eyebrow" style="padding:4px 12px 8px">联想</div>${SUG.map(s => `<div class="m-li f" data-f style="padding:9px 12px"><span class="tx"><b style="font-weight:500;font-size:13px">${s}</b></span></div>`).join('')}</div>
  <div class="m-abs" style="left:650px;top:27px;width:262px;display:flex;flex-direction:column;gap:12px;z-index:2"><div class="m-eyebrow">热搜</div>${hotList(6)}</div>
  <div class="m-abs" style="left:430px;right:0;top:330px;z-index:2"><div class="m-h" style="font-size:15px">“极光”的结果</div><div class="m-row tvrow" style="gap:16px;overflow:hidden;padding:12px 48px 12px 4px">${[0, 2, 5, 6, 3].map(i => vcard(VIDS[i], { w: 180, f: 1, noMeta: true })).join('')}</div></div>` + tvhint();
}

/* ---------- PGC 影视 ---------- */
const PGC_TABS = ['番剧', '国创', '电影', '电视剧', '纪录片', '综艺'];
const DAYS = ['一', '二', '三', '四', '五', '六', '日'];
function pgcPhone() {
  return sb() + `<div style="display:flex;justify-content:space-between;align-items:center;padding:6px 16px 6px 20px"><b style="font:800 26px var(--font-display)">影视</b><span class="m-ib">${ic('search')}</span></div>
  <div class="m-chips" style="padding:0 16px">${PGC_TABS.map((t, i) => `<span class="m-chip${i === 0 ? ' on' : ''}">${t}</span>`).join('')}</div>
  <div style="display:flex;gap:8px;padding:14px 16px 0;height:290px"><div style="flex:1;position:relative;border-radius:28px;overflow:hidden">${img(8, 'S')}<div class="m-scrim-b"></div><div class="m-abs" style="left:14px;right:14px;bottom:14px;color:#fff"><div style="font:800 19px var(--font-display)">葬送的芙莉莲 第二季</div><div style="font-size:12px;opacity:.85">更新至第 8 话 · 每周五 23:00</div></div></div><div style="width:90px;border-radius:24px;overflow:hidden">${img(9, 'S')}</div><div style="width:34px;border-radius:16px;overflow:hidden">${img(4, 'S')}</div></div>
  <div class="m-h" style="padding:16px 16px 8px">新番时间表 <small>完整 ›</small></div>
  <div style="display:flex;gap:6px;padding:0 16px">${DAYS.map((d, i) => `<span style="flex:1;height:44px;border-radius:${i === 4 ? '16px' : '999px'};display:grid;place-items:center;font-weight:600;font-size:13px;${i === 4 ? 'background:var(--primary);color:var(--on-primary)' : 'background:var(--surface-container-high)'}">${d}</span>`).join('')}</div>
  <div style="display:flex;flex-direction:column;gap:10px;padding:12px 16px">${[0, 3].map(i => `<div style="display:flex;gap:12px;align-items:center"><span class="num" style="font-weight:700;color:var(--primary);width:44px">${['23:00', '23:30'][i ? 1 : 0]}</span><div style="width:44px;height:58px;border-radius:10px;overflow:hidden;flex:none">${img(PGCS[i].c, 'S')}</div><div class="m-col m-fill"><b style="font-size:14px">${PGCS[i].t}</b><span class="m-meta">第 ${i ? 12 : 8} 话</span></div><span class="m-btn t" style="height:32px;padding:0 12px;font-size:12px">追番</span></div>`).join('')}</div>` + nbar('影视');
}
function pgcTablet() {
  return `<div style="display:flex;height:100%">${rail('影视')}<div class="m-fill m-col" style="padding:20px 28px 0 8px;gap:18px;overflow:hidden">
    <div style="display:flex;align-items:center;gap:20px"><b style="font:800 28px var(--font-display)">影视</b><div class="m-chips">${PGC_TABS.map((t, i) => `<span class="m-chip${i === 0 ? ' on' : ''}">${t}</span>`).join('')}</div><div class="m-fill"></div><span class="m-btn t">${ic('tune')}索引</span><span class="m-btn t">${ic('star')}我的追番</span></div>
    <div style="display:flex;gap:20px;flex:none"><div style="flex:1;position:relative;height:320px;border-radius:28px;overflow:hidden">${img(8, 'W')}<div class="m-scrim-b"></div><div class="m-abs" style="left:28px;bottom:24px;color:#fff;display:flex;flex-direction:column;gap:8px"><div class="m-eyebrow" style="color:#fff">本季热播 · 9.8 分</div><div style="font:800 34px var(--font-display)">葬送的芙莉莲 第二季</div><div style="display:flex;gap:10px"><span class="m-btn fill">${ic('play')}看第 8 话</span><span class="m-btn t">${ic('add')}追番</span></div></div></div>
      <div style="width:300px;border-radius:28px;background:var(--surface-container);padding:18px;display:flex;flex-direction:column;gap:12px"><div class="m-h">今天更新 <small>周五</small></div>${[0, 3, 4].map(i => `<div style="display:flex;gap:10px;align-items:center"><div style="width:44px;height:58px;border-radius:10px;overflow:hidden;flex:none">${img(PGCS[i].c, 'S')}</div><div class="m-col"><b style="font-size:13px">${PGCS[i].t}</b><span class="m-meta">23:00 更新</span></div></div>`).join('')}</div></div>
    <div class="m-h">热门番剧</div><div style="display:grid;grid-template-columns:repeat(6,1fr);gap:16px">${PGCS.map(p => poster(p)).join('')}</div></div></div>`;
}
function pgcTv() {
  return `<div class="scope cinema" data-seed-cover="8" style="position:absolute;inset:0;background:var(--surface)"><div class="m-backdrop">${img(8, 'W')}</div>${tvrail('影视')}${tvTop(PGC_TABS, '番剧')}
  <div class="m-abs" style="left:96px;top:78px;width:420px;display:flex;flex-direction:column;gap:10px;z-index:2"><div class="m-eyebrow">本季热播 · 评分 9.8</div><div class="m-display">葬送的芙莉莲<br>第二季</div><div class="m-meta" style="font-size:12px">更新至第 8 话 · 每周五 23:00 · 奇幻 / 冒险</div>
    <div style="display:flex;gap:12px;margin-top:6px"><div class="m-btn fill lg f" data-f data-start>${ic('play')}看第 8 话</div><div class="m-btn t lg f" data-f>${ic('add')}追番</div></div>
    <div style="display:flex;gap:8px;margin-top:4px">${['时间表', '索引', '我的追番'].map(t => `<span class="m-chip f" data-f>${t}</span>`).join('')}</div></div>
  <div class="m-abs" style="left:96px;right:0;top:318px;z-index:2"><div class="m-h" style="font-size:15px">热门番剧</div><div class="m-row tvrow" style="gap:16px;overflow:hidden;padding:12px 48px 12px 4px">${PGCS.concat(PGCS.slice(0, 2)).map(p => poster(p, { w: 112, f: 1 })).join('')}</div></div></div>` + tvhint();
}

/* ---------- DYNAMIC ---------- */
const UPS = ['阿树', '沙漠鱼', '飞手', '蓝调', '植物所', '北境', '老王'];
const upRow = () => `<div style="display:flex;gap:14px;overflow:hidden">${UPS.map((u, i) => `<div class="m-col" style="align-items:center;gap:4px;flex:none;font-size:11px"><div class="m-av ${i % 2 ? 's' : 'p'}" style="width:48px;height:48px;${i === 0 ? 'box-shadow:0 0 0 2px var(--primary)' : ''}">${u[0]}</div>${u}</div>`).join('')}</div>`;
const dynCard = (k, sel) => {
  const v = VIDS[[0, 4, 6][k]];
  const head = `<div style="display:flex;gap:10px;align-items:center"><div class="m-av p">${v.up[0]}</div><div class="m-col"><b style="font-size:13px">${v.up}</b><span class="m-meta">${['2 小时前 · 投稿了视频', '昨天 21:30 · 动态', '10-12 · 投稿了视频'][k]}</span></div></div>`;
  const body = k === 1
    ? `<div style="font-size:13px;line-height:1.55">第 30 天，花终于全开了。顺便晒一下拍摄用的延时轨道，下期讲布光。</div><div style="display:grid;grid-template-columns:repeat(3,1fr);gap:4px">${[4, 11, 5].map(c => `<div class="m-cv" style="aspect-ratio:1;border-radius:10px">${img(c, 'S')}</div>`).join('')}</div>`
    : `<div class="m-t2" style="font-size:13px">${v.t}</div><div style="display:flex;gap:10px;padding:8px;border-radius:16px;background:var(--surface-container-high)"><div class="m-cv" style="width:120px;flex:none;border-radius:10px">${img(v.c)}<span class="dur">${v.d}</span></div><div class="m-col" style="gap:4px;font-size:11px;color:var(--on-surface-variant);justify-content:center">${v.v}播放<br>${v.dm}弹幕</div></div>`;
  return `<div style="display:flex;flex-direction:column;gap:10px;padding:14px;border-radius:24px;background:${sel ? 'var(--secondary-container)' : 'var(--surface-container-low)'}">${head}${body}<div class="m-meta" style="justify-content:space-around;font-size:12px"><span>${ic('share')} 转发</span><span>${ic('comment')} ${[312, 88, 640][k]}</span><span>${ic('like')} ${['1.2万', '2061', '3.4万'][k]}</span></div></div>`;
};
function dynPhone() {
  return sb() + `<div style="padding:6px 20px 10px"><b style="font:800 26px var(--font-display)">动态</b></div><div style="padding:0 16px 12px">${upRow()}</div><div style="display:flex;flex-direction:column;gap:12px;padding:0 12px">${dynCard(0)}${dynCard(1)}</div>` + nbar('动态');
}
function dynTablet() {
  return `<div style="display:flex;height:100%">${rail('动态')}<div class="m-col" style="width:470px;flex:none;padding:20px 8px 0;gap:14px;overflow:hidden"><b style="font:800 28px var(--font-display);padding-left:8px">动态</b>${upRow()}${dynCard(0, 1)}${dynCard(1)}</div>
    <div class="m-fill" style="margin:16px 20px 16px 12px;border-radius:28px;background:var(--surface-container-low);padding:22px;display:flex;flex-direction:column;gap:14px;overflow:hidden"><div style="display:flex;gap:10px;align-items:center"><div class="m-av p" style="width:44px;height:44px">阿</div><div class="m-col"><b>旅行的阿树</b><span class="m-meta">2 小时前 · 投稿了视频</span></div></div>
      <div class="m-video" style="height:300px;border-radius:24px">${img(0, 'W')}<div class="m-abs" style="left:50%;top:50%;width:64px;height:64px;margin:-32px;border-radius:22px;background:var(--primary);color:var(--on-primary);display:grid;place-items:center">${ic('play')}</div></div>
      <b style="font-size:16px">${VIDS[0].t}</b><div class="m-tabs" style="padding:0"><div class="m-tab on">评论 312</div><div class="m-tab">转发</div></div>${COMMENTS.slice(0, 3).map(c => comment(...c)).join('')}</div></div>`;
}
function dynTv() {
  return `${tvrail('动态')}<div class="m-abs" style="left:96px;top:27px;right:48px;display:flex;flex-direction:column;gap:16px;z-index:2"><div style="display:flex;justify-content:space-between;align-items:center"><b style="font:800 26px var(--font-display)">动态</b><div class="m-chips">${['全部', '视频', '图文'].map((c, i) => `<span class="m-chip f${i ? '' : ' on'}" data-f>${c}</span>`).join('')}</div></div>${upRow()}</div>
  <div class="m-abs" style="left:96px;right:0;top:170px;z-index:2"><div class="m-row tvrow" style="gap:18px;overflow:hidden;padding:12px 48px 12px 4px">${[0, 6, 2, 3, 5].map((i, k) => `<div class="m-vc f" data-f${k === 0 ? ' data-start' : ''} style="width:240px;flex:none"><div style="display:flex;gap:8px;align-items:center"><div class="m-av p" style="width:26px;height:26px;font-size:11px">${VIDS[i].up[0]}</div><span style="font-size:12px;font-weight:600">${VIDS[i].up}</span><span class="m-meta">· ${k + 1} 小时前</span></div><div class="m-cv">${img(VIDS[i].c)}<span class="dur">${VIDS[i].d}</span></div><div class="tt m-t2">${VIDS[i].t}</div></div>`).join('')}</div></div>` + tvhint();
}

/* ---------- ME 我的 ---------- */
const TILES = [['history', '历史记录', '今天看了 6 个', 'primary'], ['star', '收藏夹', '12 个收藏夹', 'tertiary'], ['later', '稍后再看', '9 个待看', 'secondary'], ['movie', '追番追剧', '3 部更新', 'primary']];
const tile = ([i, t, s, c]) => `<div style="padding:14px;border-radius:24px;background:var(--${c}-container);color:var(--on-${c}-container);display:flex;flex-direction:column;gap:10px"><span style="width:40px;height:40px;display:grid;place-items:center;border-radius:14px;background:var(--${c});color:var(--on-${c})">${ic(i)}</span><b style="font-size:15px">${t}</b><span style="font-size:12px;opacity:.8">${s}</span></div>`;
const profileHead = () => `<div style="display:flex;gap:16px;align-items:center"><div class="m-av p lg">阿</div><div class="m-col" style="gap:6px"><div style="display:flex;gap:8px;align-items:center"><b style="font:800 20px var(--font-display)">阿树不吃香菜</b><span class="m-av s" style="width:30px;height:30px;font-size:11px">6</span></div><div class="m-meta" style="font-size:12px">关注 312 · 粉丝 1.2万 · 硬币 486</div></div></div>`;
function mePhone() {
  return sb() + `<div style="padding:16px 20px;display:flex;flex-direction:column;gap:18px">${profileHead()}<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">${TILES.map(tile).join('')}</div>
  <div class="m-h">继续观看 <small>全部</small></div><div class="m-row" style="gap:10px;overflow:hidden">${[2, 3, 5].map((i, k) => vcard(VIDS[i], { w: 150, prog: [62, 30, 80][k], noMeta: true })).join('')}</div>
  <div style="display:flex;flex-direction:column;border-radius:24px;background:var(--surface-container-low);padding:6px">${[['person', '关注的 UP 主', '312 位'], ['shield', '账号与用户锁', '2 个账号'], ['settings', '设置', '主题、播放、弹幕']].map(([i, t, s]) => `<div class="m-li"><span class="ic" style="background:var(--surface-container-highest)">${ic(i)}</span><span class="tx"><b>${t}</b><span>${s}</span></span></div>`).join('')}</div></div>` + nbar('我的');
}
function meTablet() {
  return `<div style="display:flex;height:100%">${rail('我的')}<div class="m-col" style="width:340px;flex:none;padding:24px 12px;gap:18px">${profileHead()}<div class="m-col" style="gap:4px">${[['history', '历史记录', '今天看了 6 个', 1], ['star', '收藏夹', '12 个收藏夹'], ['later', '稍后再看', '9 个待看'], ['movie', '追番追剧', '3 部更新'], ['person', '关注', '312 位 UP 主'], ['shield', '账号与用户锁', '2 个账号']].map(([i, t, s, on]) => `<div class="m-li${on ? ' on' : ''}"><span class="ic" style="${on ? 'background:var(--primary);color:var(--on-primary)' : 'background:var(--surface-container-high)'}">${ic(i)}</span><span class="tx"><b>${t}</b><span>${s}</span></span></div>`).join('')}</div></div>
    <div class="m-fill" style="margin:16px 20px 16px 4px;border-radius:28px;background:var(--surface-container-low);padding:24px;display:flex;flex-direction:column;gap:16px;overflow:hidden"><div style="display:flex;justify-content:space-between;align-items:center"><b style="font:800 24px var(--font-display)">历史记录</b><div class="m-chips"><span class="m-chip on">全部</span><span class="m-chip">视频</span><span class="m-chip">番剧</span></div></div>
      <div class="m-h">今天</div><div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px">${[2, 3, 5, 0].map((i, k) => vcard(VIDS[i], { prog: [62, 30, 80, 100][k] })).join('')}</div><div class="m-h">昨天</div><div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px">${[6, 7, 1, 4].map((i, k) => vcard(VIDS[i], { prog: [100, 45, 12, 70][k] })).join('')}</div></div></div>`;
}
function meTv() {
  return `${tvrail('我的')}<div class="m-abs" style="left:96px;top:27px;right:48px;display:flex;gap:20px;align-items:center;z-index:2">${profileHead()}<div class="m-fill"></div><span class="m-btn t f" data-f>${ic('person')}切换账号</span><span class="m-btn t f" data-f>${ic('shield')}用户锁</span></div>
  <div class="m-abs" style="left:96px;right:48px;top:118px;display:grid;grid-template-columns:repeat(4,1fr);gap:14px;z-index:2">${TILES.map((t, i) => `<div class="f g" data-f${i === 0 ? ' data-start' : ''} style="border-radius:24px">${tile(t)}</div>`).join('')}</div>
  <div class="m-abs" style="left:96px;right:0;top:300px;z-index:2"><div class="m-h" style="font-size:15px">继续观看</div><div class="m-row tvrow" style="gap:16px;overflow:hidden;padding:12px 48px 12px 4px">${[2, 3, 5, 0, 6, 7].map((i, k) => vcard(VIDS[i], { w: 190, f: 1, prog: [62, 30, 80, 15, 50, 90][k], noMeta: true })).join('')}</div></div>` + tvhint();
}
</script>
