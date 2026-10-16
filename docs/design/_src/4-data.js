<script>
'use strict';
/* ---------- icons (Material Symbols paths, 24dp grid) ---------- */
const ICONS = {
  home: 'M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z',
  search: 'M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',
  person: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z',
  play: 'M8 5v14l11-7z',
  pause: 'M6 19h4V5H6v14zm8-14v14h4V5h-4z',
  like: 'M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z',
  star: 'M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z',
  heart: 'M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z',
  comment: 'M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z',
  share: 'M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11A2.99 2.99 0 0 0 21 5c0-1.66-1.34-3-3-3s-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81A2.99 2.99 0 0 0 3 12a2.99 2.99 0 0 0 5.04 2.19l7.12 4.16c-.05.21-.08.43-.08.65a2.92 2.92 0 1 0 2.92-2.92z',
  settings: 'M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.48.48 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96a.49.49 0 0 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.49.49 0 0 0-.12-.61l-2.01-1.58zM12 15.6A3.6 3.6 0 1 1 12 8.4a3.6 3.6 0 0 1 0 7.2z',
  history: 'M13 3a9 9 0 0 0-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42A8.95 8.95 0 0 0 13 21a9 9 0 0 0 0-18zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z',
  dynamic: 'M20 8H4V6h16v2zm-2-6H6v2h12V2zm4 10v8c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2v-8c0-1.1.9-2 2-2h16c1.1 0 2 .9 2 2zm-6 4l-6-3.27v6.53L16 16z',
  movie: 'M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z',
  shorts: 'M8 2h8a3 3 0 0 1 3 3v14a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V5a3 3 0 0 1 3-3zm0 2a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V5a1 1 0 0 0-1-1H8zm2 4.5l5 3.5-5 3.5v-7z',
  menu: 'M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z',
  back: 'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z',
  more: 'M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z',
  next: 'M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z',
  full: 'M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z',
  cc: 'M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 12h4v2H4v-2zm10 6H4v-2h10v2zm6 0h-4v-2h4v2zm0-4H10v-2h10v2z',
  later: 'M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm4.2 14.2L11 13V7h1.5v5.2l4.5 2.7-.8 1.3z',
  mic: 'M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z',
  qr: 'M3 11h8V3H3v8zm2-6h4v4H5V5zM3 21h8v-8H3v8zm2-6h4v4H5v-4zM13 3v8h8V3h-8zm6 6h-4V5h4v4zm0 10h2v2h-2zm-6-6h2v2h-2zm2 2h2v2h-2zm-2 2h2v2h-2zm2 2h2v2h-2zm2-2h2v2h-2zm0-4h2v2h-2zm2 2h2v2h-2z',
  tv: 'M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.1-.9-2-2-2zm0 14H3V5h18v12z',
  phone: 'M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z',
  tablet: 'M21 4H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h18c1.1 0 1.99-.9 1.99-2L23 6c0-1.1-.9-2-2-2zm-2 14H5V6h14v12z',
  tune: 'M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z',
  palette: 'M12 3a9 9 0 0 0 0 18c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm3-4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm5 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm3 4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z',
  up: 'M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z',
  down: 'M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z',
  check: 'M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z',
  add: 'M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z',
  explore: 'M12 10.9c-.61 0-1.1.49-1.1 1.1s.49 1.1 1.1 1.1c.61 0 1.1-.49 1.1-1.1s-.49-1.1-1.1-1.1zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm2.19 12.19L6 18l3.81-8.19L18 6l-3.81 8.19z',
  coin: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 2a8 8 0 1 1 0 16 8 8 0 0 1 0-16zm-1 3v1.1A3 3 0 0 0 8.5 11c0 1.7 1.4 2.3 2.8 2.7 1.2.3 1.7.6 1.7 1.2 0 .6-.6 1-1.5 1-1 0-1.6-.5-1.7-1.2H8.2c.1 1.4 1.1 2.4 2.8 2.7V18h2v-.6c1.6-.3 2.6-1.3 2.6-2.6 0-1.8-1.5-2.4-2.9-2.8-1.1-.3-1.6-.5-1.6-1.1 0-.5.5-.9 1.3-.9.8 0 1.3.4 1.4 1h1.7c-.1-1.3-1-2.3-2.5-2.6V7h-2z',
  shield: 'M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z',
  storage: 'M2 20h20v-4H2v4zm2-3h2v2H4v-2zM2 4v4h20V4H2zm4 3H4V5h2v2zm-4 7h20v-4H2v4zm2-3h2v2H4v-2z',
  wifi: 'M1 9l2 2c4.97-4.97 13.03-4.97 18 0l2-2C16.93 2.93 7.08 2.93 1 9zm8 8l3 3 3-3a4.24 4.24 0 0 0-6 0zm-4-4l2 2a7.07 7.07 0 0 1 10 0l2-2C15.14 9.14 8.87 9.14 5 13z',
  info: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z',
};
const ic = (n, cls = 'i') => `<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true"><path d="${ICONS[n]}"/></svg>`;

/* ---------- example content ---------- */
const VIDS = [
  { t: '【4K HDR】冰岛 14 天环岛自驾，追到了今年最强的一场极光', up: '旅行的阿树', v: '186.4万', dm: '1.2万', d: '18:42', c: 0, tag: '纪录' },
  { t: '撒哈拉的一夜：躺在沙丘上看银河是什么体验', up: '沙漠里的鱼', v: '52.1万', dm: '3412', d: '12:07', c: 1, tag: '旅行' },
  { t: '赛博山城：凌晨三点的洪崖洞航拍', up: '山城飞手', v: '98.7万', dm: '8906', d: '06:31', c: 2, tag: '航拍' },
  { t: '深海 200 米｜自由潜水员眼中的蓝洞', up: '蓝调潜水日志', v: '33.9万', dm: '2077', d: '21:15', c: 3, tag: '运动' },
  { t: '雨林延时摄影：一株兰花开放的 30 天', up: '植物观察所', v: '21.3万', dm: '1309', d: '08:50', c: 4, tag: '知识' },
  { t: '零下 30 度的雪原上，我们搭了一座冰屋', up: '北境日记', v: '64.0万', dm: '5520', d: '15:28', c: 5, tag: '户外' },
  { t: '夜访火山口：离熔岩湖只有 300 米', up: '地球表面观察员', v: '120.5万', dm: '9881', d: '11:46', c: 6, tag: '科普' },
  { t: '麦浪翻滚的七月，跟着收割机跑完一整天', up: '种地的小王', v: '17.6万', dm: '740', d: '09:12', c: 7, tag: '三农' },
];
const PGCS = [
  { t: '葬送的芙莉莲 第二季', s: '番剧 · 更新至第 8 话', r: '9.8', c: 8 },
  { t: '地球脉动 III', s: '纪录片 · 全 8 集', r: '9.9', c: 3 },
  { t: '沙丘：第二部', s: '电影 · 2024', r: '9.4', c: 1 },
  { t: '间谍过家家 第三季', s: '番剧 · 已完结', r: '9.6', c: 9 },
  { t: '中国奇谭', s: '国创 · 全 8 集', r: '9.5', c: 4 },
  { t: '风味人间 4', s: '纪录片 · 全 8 集', r: '9.6', c: 7 },
];
const SHORTS = [
  { t: '凌晨四点，城市还没醒，我已经跑完 10 公里', up: '夜跑的猫', like: '12.3万', cm: '2341', fav: '8902', tags: '#夜跑 #城市', c: 10 },
  { t: '樱花雨里的一分钟，手机竖着拍才对', up: '春天收集者', like: '8.7万', cm: '1105', fav: '6023', tags: '#樱花 #氛围感', c: 11 },
  { t: '极光在头顶炸开的那一秒', up: '旅行的阿树', like: '31.6万', cm: '5872', fav: '2.1万', tags: '#极光 #冰岛', c: 0 },
  { t: '火山脚下煮泡面，汤是硫磺味的', up: '地球表面观察员', like: '6.2万', cm: '987', fav: '3311', tags: '#火山 #露营', c: 6 },
  { t: '潜入蓝洞的最后 10 米', up: '蓝调潜水日志', like: '9.9万', cm: '1648', fav: '7450', tags: '#自由潜 #深海', c: 3 },
];

/* ---------- procedural cover art ---------- */
const PAL = [
  { sky: ['#06142e', '#0d3b4f'], sun: '#bff7ff', aur: '#3cf2a6', layers: ['#0e2a3c', '#071521'], stars: 1 },
  { sky: ['#2b1055', '#ff7e5f'], sun: '#ffd27f', layers: ['#c2563a', '#8a3324'], dune: 1 },
  { sky: ['#12002e', '#6b1f86'], sun: '#ff3fa4', layers: ['#1d0f3b', '#0c0620'], city: 1, stars: 1 },
  { sky: ['#01223f', '#1f8fe0'], sun: '#a9e4ff', layers: ['#0563a8', '#013a6b'], sea: 1, rays: 1 },
  { sky: ['#0b3d2e', '#8fd694'], sun: '#f2ffd9', layers: ['#2b7a4b', '#145232'], fog: 1 },
  { sky: ['#8fb8f0', '#eaf4ff'], sun: '#ffffff', layers: ['#d3e4f6', '#a9c4e0'] },
  { sky: ['#1a0505', '#7a1c0b'], sun: '#ff6a00', layers: ['#3a0d06', '#140302'], lava: 1, stars: 1 },
  { sky: ['#5aa7e0', '#f6d98b'], sun: '#fff3c4', layers: ['#e2b04a', '#b9822a'], dune: 1 },
  { sky: ['#9fc4ee', '#efe6ff'], sun: '#fffdf2', layers: ['#7f9fd3', '#5b78b0'], fog: 1 },
  { sky: ['#1f2a44', '#e05a6b'], sun: '#ffe1a8', layers: ['#2d3a5c', '#151c30'], city: 1 },
  { sky: ['#06222a', '#11727a'], sun: '#ffb86b', layers: ['#0b3b44', '#03161b'], city: 1, stars: 1 },
  { sky: ['#ffc2d4', '#fff1f5'], sun: '#ffffff', layers: ['#f59ab8', '#d9668f'], petals: 1 },
];
function rng(seed) { let a = seed >>> 0; return () => { a = (a + 0x6D2B79F5) | 0; let t = Math.imul(a ^ (a >>> 15), 1 | a); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; }; }
const coverCache = new Map();
function drawCover(ctx, w, h, idx) {
  const p = PAL[idx % PAL.length], r = rng(idx * 97 + 13);
  let g = ctx.createLinearGradient(0, 0, 0, h); g.addColorStop(0, p.sky[0]); g.addColorStop(1, p.sky[1]);
  ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
  if (p.stars) { ctx.fillStyle = 'rgba(255,255,255,.8)'; for (let i = 0; i < 70; i++) { const s = r() * 1.4 + .3; ctx.globalAlpha = r() * .8 + .2; ctx.fillRect(r() * w, r() * h * .55, s, s); } ctx.globalAlpha = 1; }
  if (p.aur) { for (let k = 0; k < 3; k++) { ctx.beginPath(); const y0 = h * (.18 + k * .09); ctx.moveTo(0, y0); for (let x = 0; x <= w; x += 8) ctx.lineTo(x, y0 + Math.sin(x / w * 5 + k) * h * .06); ctx.lineWidth = h * (.08 - k * .015); ctx.strokeStyle = p.aur; ctx.globalAlpha = .38 - k * .08; ctx.filter = 'blur(6px)'; ctx.stroke(); } ctx.filter = 'none'; ctx.globalAlpha = 1; }
  const sx = w * (.25 + r() * .5), sy = h * (.3 + r() * .15), sr = Math.min(w, h) * (.09 + r() * .06);
  g = ctx.createRadialGradient(sx, sy, 0, sx, sy, sr * 4); g.addColorStop(0, p.sun); g.addColorStop(.25, p.sun + '88'); g.addColorStop(1, p.sun + '00');
  ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
  ctx.fillStyle = p.sun; ctx.beginPath(); ctx.arc(sx, sy, sr, 0, 7); ctx.fill();
  if (p.rays) { ctx.globalAlpha = .18; ctx.fillStyle = p.sun; for (let i = 0; i < 5; i++) { ctx.beginPath(); const x = w * (.1 + i * .2); ctx.moveTo(x, 0); ctx.lineTo(x + w * .05, 0); ctx.lineTo(x + w * .2, h); ctx.lineTo(x + w * .1, h); ctx.fill(); } ctx.globalAlpha = 1; }
  p.layers.forEach((col, li) => {
    const base = h * (.55 + li * .17);
    ctx.fillStyle = col; ctx.beginPath(); ctx.moveTo(0, h);
    if (p.city) { let x = 0; while (x < w) { const bw = w * (.04 + r() * .07), bh = h * (.12 + r() * .3) * (li ? .8 : 1.2); ctx.lineTo(x, base - bh); ctx.lineTo(x + bw, base - bh); x += bw; } ctx.lineTo(w, h); ctx.fill();
      ctx.fillStyle = li ? '#ffd166' : '#4cc9f0'; for (let i = 0; i < 60; i++) { ctx.globalAlpha = .5 + r() * .5; ctx.fillRect(r() * w, base - r() * h * .3, 2, 3); } ctx.globalAlpha = 1; return; }
    const f1 = 2 + r() * 3, f2 = 5 + r() * 6, ph = r() * 6, amp = h * (p.dune || p.sea ? .05 : .12);
    for (let x = 0; x <= w; x += 4) ctx.lineTo(x, base - amp * (Math.sin(x / w * f1 + ph) * .7 + Math.sin(x / w * f2 + ph * 2) * .3) - (p.sea ? 0 : amp * .6));
    ctx.lineTo(w, h); ctx.fill();
  });
  if (p.lava) { g = ctx.createRadialGradient(w * .5, h, 0, w * .5, h, h * .7); g.addColorStop(0, 'rgba(255,120,0,.85)'); g.addColorStop(1, 'rgba(255,60,0,0)'); ctx.fillStyle = g; ctx.fillRect(0, 0, w, h); }
  if (p.fog) { g = ctx.createLinearGradient(0, h * .45, 0, h * .8); g.addColorStop(0, 'rgba(255,255,255,0)'); g.addColorStop(.5, 'rgba(255,255,255,.35)'); g.addColorStop(1, 'rgba(255,255,255,0)'); ctx.fillStyle = g; ctx.fillRect(0, 0, w, h); }
  if (p.petals) { for (let i = 0; i < 40; i++) { ctx.fillStyle = r() > .5 ? '#ff8fb1' : '#ffe0ea'; ctx.globalAlpha = .7; ctx.beginPath(); ctx.ellipse(r() * w, r() * h, 3 + r() * 3, 2 + r() * 2, r() * 3, 0, 7); ctx.fill(); } ctx.globalAlpha = 1; }
  g = ctx.createRadialGradient(w / 2, h / 2, Math.min(w, h) * .35, w / 2, h / 2, Math.max(w, h) * .75); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, 'rgba(0,0,0,.35)');
  ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
}
const SIZES = { L: [320, 200], P: [216, 384], S: [240, 320], W: [640, 300] };
function cover(idx, kind = 'L') {
  const key = idx + kind;
  if (coverCache.has(key)) return coverCache.get(key);
  const [w, h] = SIZES[kind], cv = document.createElement('canvas'); cv.width = w; cv.height = h;
  drawCover(cv.getContext('2d'), w, h, idx);
  const url = cv.toDataURL('image/jpeg', .86); coverCache.set(key, url); return url;
}
const img = (idx, kind) => `<img src="${cover(idx, kind)}" alt="">`;

/* ---------- MaterialShapes approximations: polar radius functions ---------- */
const N_PTS = 144;
const SHAPES = {
  Circle: () => 1,
  Cookie9Sided: t => .9 + .1 * Math.cos(9 * t),
  Cookie4Sided: t => .82 + .18 * Math.pow(Math.abs(Math.cos(2 * t)), .6),
  Sunny: t => .86 + .14 * Math.cos(8 * t),
  Clover4Leaf: t => .58 + .42 * Math.pow(Math.abs(Math.cos(2 * t)), .5),
  SoftBurst: t => .8 + .2 * Math.pow((Math.cos(10 * t) + 1) / 2, 1.6),
  Pentagon: t => { const n = 5, a = ((t + Math.PI / 2) % (2 * Math.PI / n) + 2 * Math.PI / n) % (2 * Math.PI / n) - Math.PI / n; return .78 * (Math.cos(Math.PI / n) / Math.cos(a)) * .82 + .18; },
  Pill: t => { const a = 1, b = .62, p = 3; return Math.pow(Math.pow(Math.abs(Math.cos(t) / a), p) + Math.pow(Math.abs(Math.sin(t) / b), p), -1 / p); },
  Flower: t => .74 + .26 * Math.pow(Math.abs(Math.cos(4 * t)), .9),
  Gem: t => { const n = 6, a = ((t % (2 * Math.PI / n)) + 2 * Math.PI / n) % (2 * Math.PI / n) - Math.PI / n; return .8 * Math.cos(Math.PI / n) / Math.cos(a) + .2; },
};
function shapeRadii(name) {
  const f = SHAPES[name], out = []; let max = 0;
  for (let i = 0; i < N_PTS; i++) { const v = f(i / N_PTS * 2 * Math.PI); out.push(v); max = Math.max(max, v); }
  return out.map(v => v / max);
}
function radiiToPath(rad, R, rot = 0) {
  let d = '';
  rad.forEach((v, i) => { const t = i / N_PTS * 2 * Math.PI + rot; d += (i ? 'L' : 'M') + (Math.cos(t) * v * R).toFixed(2) + ' ' + (Math.sin(t) * v * R).toFixed(2); });
  return d + 'Z';
}
function radiiToPolygon(rad) {
  return 'polygon(' + rad.map((v, i) => { const t = i / N_PTS * 2 * Math.PI - Math.PI / 2; return (50 + Math.cos(t) * v * 50).toFixed(2) + '% ' + (50 + Math.sin(t) * v * 50).toFixed(2) + '%'; }).join(',') + ')';
}
(function installShapeVars() {
  const rs = document.documentElement.style;
  rs.setProperty('--shape-cookie9', radiiToPolygon(shapeRadii('Cookie9Sided')));
  rs.setProperty('--shape-sunny', radiiToPolygon(shapeRadii('Sunny')));
  rs.setProperty('--shape-clover', radiiToPolygon(shapeRadii('Clover4Leaf')));
})();

/* ---------- spring easing: same parameters as MotionScheme.expressive() ---------- */
function springLinear(damping, stiffness) {
  const w0 = Math.sqrt(stiffness), z = damping;
  const settle = Math.min(2.5, -Math.log(0.002) / (z * w0));
  const x = t => {
    if (z < 1) { const wd = w0 * Math.sqrt(1 - z * z); return 1 - Math.exp(-z * w0 * t) * (Math.cos(wd * t) + (z * w0 / wd) * Math.sin(wd * t)); }
    return 1 - Math.exp(-w0 * t) * (1 + w0 * t);
  };
  const pts = []; const n = 48;
  for (let i = 0; i <= n; i++) { const t = i / n; pts.push(`${x(t * settle).toFixed(4)} ${(t * 100).toFixed(1)}%`); }
  pts[n] = '1 100%';
  return { easing: `linear(${pts.join(',')})`, ms: Math.round(settle * 1000) };
}
const MOTION = [
  { name: 'fastSpatial', z: .6, k: 800, use: '焦点放大、按钮按压变形、点赞弹出' },
  { name: 'defaultSpatial', z: .8, k: 380, use: '卡片圆角变形、导航轨展开、底部抽屉' },
  { name: 'slowSpatial', z: .8, k: 200, use: '卡片 → 播放器的共享元素转场、刷刷翻页回弹' },
  { name: 'fastEffects', z: 1, k: 3800, use: '焦点描边出现、按钮颜色切换' },
  { name: 'defaultEffects', z: 1, k: 1600, use: '控制层淡入淡出、标签切换' },
  { name: 'slowEffects', z: 1, k: 800, use: '主题色切换、氛围光渐变' },
];
(function installSprings() {
  if (!window.CSS || !CSS.supports('transition-timing-function', 'linear(0, 1)')) return;
  const rs = document.documentElement.style;
  const fast = springLinear(.6, 800), def = springLinear(.8, 380);
  rs.setProperty('--spring-fast', fast.easing); rs.setProperty('--spring-fast-dur', fast.ms + 'ms');
  rs.setProperty('--spring-default', def.easing); rs.setProperty('--spring-default-dur', def.ms + 'ms');
})();
</script>
