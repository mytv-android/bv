<script>
'use strict';
(function () {
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ---------- static content ---------- */
  $('#principleCards').innerHTML = CONTENT.principleCards.join('');
  $('#axisCards').innerHTML = CONTENT.axisCards.join('');
  $('#colorRules').innerHTML = CONTENT.colorRules.join('');
  $('#shortsCards').innerHTML = CONTENT.shortsCards.join('');
  $('#tvCards').innerHTML = CONTENT.tvCards.join('');
  $('#motionRows').innerHTML = CONTENT.motionRows();
  $('#compRows').innerHTML = CONTENT.compRows;
  $('#routeRows').innerHTML = CONTENT.routeRows;
  $('#phases').innerHTML = CONTENT.phases;
  $('#risks').innerHTML = CONTENT.risks;
  $('#modules').innerHTML = CONTENT.modules;

  /* ---------- color engine ---------- */
  const ROLES = ['primary', 'onPrimary', 'primaryContainer', 'onPrimaryContainer', 'secondary', 'onSecondary', 'secondaryContainer', 'onSecondaryContainer', 'tertiary', 'onTertiary', 'tertiaryContainer', 'onTertiaryContainer', 'surface', 'onSurface', 'onSurfaceVariant', 'surfaceContainerLowest', 'surfaceContainerLow', 'surfaceContainer', 'surfaceContainerHigh', 'surfaceContainerHighest', 'outline', 'outlineVariant', 'inverseSurface', 'inverseOnSurface', 'inversePrimary'];
  const kebab = s => s.replace(/[A-Z]/g, m => '-' + m.toLowerCase());
  const state = { seed: '#FB7299', style: 'Vibrant', contrast: 0 };
  let M = null;

  const hex2rgb = h => [1, 3, 5].map(i => parseInt(h.slice(i, i + 2), 16));
  const rgb2hex = (r, g, b) => '#' + [r, g, b].map(v => Math.round(Math.max(0, Math.min(255, v))).toString(16).padStart(2, '0')).join('');
  function rgb2hsl([r, g, b]) { r /= 255; g /= 255; b /= 255; const mx = Math.max(r, g, b), mn = Math.min(r, g, b); let h = 0, s = 0; const l = (mx + mn) / 2; if (mx !== mn) { const d = mx - mn; s = l > .5 ? d / (2 - mx - mn) : d / (mx + mn); h = mx === r ? (g - b) / d + (g < b ? 6 : 0) : mx === g ? (b - r) / d + 2 : (r - g) / d + 4; h *= 60; } return [h, s * 100, l * 100]; }
  function hsl(h, s, l) { h = ((h % 360) + 360) % 360; s /= 100; l /= 100; const k = n => (n + h / 30) % 12, a = s * Math.min(l, 1 - l); const f = n => l - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1))); return rgb2hex(f(0) * 255, f(8) * 255, f(4) * 255); }
  function fallbackScheme(seed, dark) {
    const [h, s0] = rgb2hsl(hex2rgb(seed)), s = Math.min(s0, 75);
    const t = h + 60, n = Math.min(s, 14);
    return dark ? {
      primary: hsl(h, s, 80), onPrimary: hsl(h, s, 20), primaryContainer: hsl(h, s * .7, 30), onPrimaryContainer: hsl(h, s, 90),
      secondary: hsl(h, s * .3, 80), onSecondary: hsl(h, s * .3, 20), secondaryContainer: hsl(h, s * .25, 30), onSecondaryContainer: hsl(h, s * .4, 90),
      tertiary: hsl(t, s * .5, 78), onTertiary: hsl(t, s * .5, 20), tertiaryContainer: hsl(t, s * .4, 30), onTertiaryContainer: hsl(t, s * .6, 90),
      surface: hsl(h, n, 7), onSurface: hsl(h, n, 90), onSurfaceVariant: hsl(h, n, 80),
      surfaceContainerLowest: hsl(h, n, 5), surfaceContainerLow: hsl(h, n, 10), surfaceContainer: hsl(h, n, 12), surfaceContainerHigh: hsl(h, n, 17), surfaceContainerHighest: hsl(h, n, 22),
      outline: hsl(h, n, 58), outlineVariant: hsl(h, n, 30), inverseSurface: hsl(h, n, 90), inverseOnSurface: hsl(h, n, 20), inversePrimary: hsl(h, s, 40),
    } : {
      primary: hsl(h, s, 40), onPrimary: '#ffffff', primaryContainer: hsl(h, s, 90), onPrimaryContainer: hsl(h, s, 25),
      secondary: hsl(h, s * .3, 40), onSecondary: '#ffffff', secondaryContainer: hsl(h, s * .4, 90), onSecondaryContainer: hsl(h, s * .3, 25),
      tertiary: hsl(t, s * .5, 38), onTertiary: '#ffffff', tertiaryContainer: hsl(t, s * .7, 90), onTertiaryContainer: hsl(t, s * .5, 25),
      surface: hsl(h, n * 2, 98), onSurface: hsl(h, n, 10), onSurfaceVariant: hsl(h, n, 30),
      surfaceContainerLowest: '#ffffff', surfaceContainerLow: hsl(h, n * 2, 96), surfaceContainer: hsl(h, n * 2, 94), surfaceContainerHigh: hsl(h, n * 2, 92), surfaceContainerHighest: hsl(h, n * 2, 90),
      outline: hsl(h, n, 48), outlineVariant: hsl(h, n, 80), inverseSurface: hsl(h, n, 20), inverseOnSurface: hsl(h, n, 95), inversePrimary: hsl(h, s, 80),
    };
  }
  const schemeCache = new Map();
  function schemeFor(seed, dark) {
    const key = [seed, dark, state.style, state.contrast, !!M].join('|');
    if (schemeCache.has(key)) return schemeCache.get(key);
    let out;
    if (M) {
      const S = M['Scheme' + state.style] || M.SchemeTonalSpot;
      const sc = new S(M.Hct.fromInt(M.argbFromHex(seed)), dark, state.contrast);
      out = {}; ROLES.forEach(r => { out[r] = M.hexFromArgb(M.MaterialDynamicColors[r].getArgb(sc)); });
    } else out = fallbackScheme(seed, dark);
    schemeCache.set(key, out); return out;
  }
  function applyScheme(el, seed) {
    const L = schemeFor(seed, false), D = schemeFor(seed, true);
    ROLES.forEach(r => { const k = kebab(r); el.style.setProperty('--l-' + k, L[r]); el.style.setProperty('--d-' + k, D[r]); });
  }

  /* cover -> seed (QuantizerCelebi + Score, same pipeline as material-kolor's themeColors) */
  const seedCache = new Map();
  function coverSeeds(idx) {
    const key = idx + '|' + !!M;
    if (seedCache.has(key)) return seedCache.get(key);
    const cv = document.createElement('canvas'); cv.width = 96; cv.height = 60;
    const ctx = cv.getContext('2d', { willReadFrequently: true }); drawCover(ctx, 96, 60, idx);
    const d = ctx.getImageData(0, 0, 96, 60).data, px = [];
    for (let i = 0; i < d.length; i += 4) if (d[i + 3] === 255) px.push(((255 << 24) | (d[i] << 16) | (d[i + 1] << 8) | d[i + 2]) >>> 0);
    let seeds;
    if (M) {
      const ranked = M.Score.score(M.QuantizerCelebi.quantize(px, 128), { desired: 4, fallbackColorARGB: 0xff4285f4, filter: true });
      seeds = ranked.map(a => M.hexFromArgb(a));
    } else {
      let best = null, bs = -1; for (const a of px) { const r = (a >> 16) & 255, g = (a >> 8) & 255, b = a & 255; const [, s, l] = rgb2hsl([r, g, b]); const sc = s * (1 - Math.abs(l - 50) / 50); if (sc > bs) { bs = sc; best = rgb2hex(r, g, b); } }
      seeds = [best || '#888888'];
    }
    seedCache.set(key, seeds); return seeds;
  }
  function applyScopes(root) {
    $$('[data-seed-cover]', root).forEach(el => applyScheme(el, coverSeeds(+el.dataset.seedCover)[0]));
    $$('[data-seed-hex]', root).forEach(el => applyScheme(el, el.dataset.seedHex));
  }
  const isDark = () => { const t = document.documentElement.dataset.theme; return t ? t === 'dark' : matchMedia('(prefers-color-scheme: dark)').matches; };

  function renderPalette() {
    const dark = isDark(), S = schemeFor(state.seed, dark);
    const cell = (role, on, label, big) => `<div style="background:var(--${kebab(role)});color:var(--${kebab(on)});padding:${big ? '14px 12px 22px' : '10px 12px'};border-radius:${big ? 18 : 12}px;display:grid;gap:2px;min-width:0"><b style="font:600 12px var(--font-display)">${label}</b><span class="mono" style="font-size:11px;opacity:.85">${S[role]}</span></div>`;
    const col = (r, n) => `<div style="display:grid;gap:4px;min-width:0">${cell(r, 'on' + r[0].toUpperCase() + r.slice(1), n, 1)}${cell(r + 'Container', 'on' + r[0].toUpperCase() + r.slice(1) + 'Container', n + ' Container')}</div>`;
    const surf = ['surfaceContainerLowest', 'surfaceContainerLow', 'surfaceContainer', 'surfaceContainerHigh', 'surfaceContainerHighest'];
    $('#palette').innerHTML = `<div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px">${col('primary', 'Primary')}${col('secondary', 'Secondary')}${col('tertiary', 'Tertiary')}</div>
      <div style="display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:4px;margin-top:8px">${surf.map((r, i) => `<div style="background:var(--${kebab(r)});color:var(--on-surface);padding:10px 8px;border-radius:10px;box-shadow:inset 0 0 0 1px var(--outline-variant);min-width:0"><b style="font:600 11px var(--font-display)">${['Lowest', 'Low', 'Container', 'High', 'Highest'][i]}</b><div class="mono" style="font-size:10px;opacity:.8">${S[r]}</div></div>`).join('')}</div>
      <p class="muted" style="font-size:13px;margin-top:8px">当前显示${dark ? '深色' : '浅色'}方案 · 种子 <span class="mono">${state.seed.toUpperCase()}</span> · PaletteStyle.${state.style}</p>`;
  }
  function refresh() {
    applyScheme(document.documentElement, state.seed);
    applyScopes(document);
    renderPalette();
    $('#engineStatus').textContent = M ? `配色：material-color-utilities 0.3.0 · ${state.style} · 对比度 ${state.contrast}` : '配色库未加载，正在使用近似算法（颜色与真实结果有差异）';
  }

  /* ---------- controls ---------- */
  const PRESETS = [['B 站粉', '#FB7299'], ['天蓝', '#23ADE5'], ['琥珀', '#F2A53A'], ['青绿', '#1FA88C'], ['紫罗兰', '#8C6FF7'], ['朱红', '#E5484D']];
  function renderSeeds() {
    $('#seeds').innerHTML = PRESETS.map(([n, c]) => `<button type="button" class="swatch" style="--c:${c}" aria-label="${n} ${c}" aria-pressed="${state.seed.toUpperCase() === c}" data-seed="${c}"></button>`).join('')
      + `<button type="button" class="swatch cover" style="background-image:url(${cover(0)})" aria-label="跟随封面：从极光封面取色" aria-pressed="${state.fromCover === 0}" data-cover="0"></button>`;
  }
  $('#seeds').addEventListener('click', e => {
    const b = e.target.closest('button'); if (!b) return;
    if (b.dataset.cover != null) { state.seed = coverSeeds(+b.dataset.cover)[0]; state.fromCover = +b.dataset.cover; }
    else { state.seed = b.dataset.seed; state.fromCover = null; }
    renderSeeds(); refresh();
  });
  function bindSeg(sel, attr, apply) {
    $(sel).addEventListener('click', e => {
      const b = e.target.closest('button'); if (!b) return;
      $$('button', $(sel)).forEach(x => x.setAttribute('aria-pressed', String(x === b)));
      apply(b.dataset[attr]);
    });
  }
  bindSeg('#modeSeg', 'mode', m => { if (m === 'auto') delete document.documentElement.dataset.theme; else document.documentElement.dataset.theme = m; renderPalette(); });
  bindSeg('#styleSeg', 'style', s => { state.style = s; refresh(); renderCoverResult(); });
  bindSeg('#contrastSeg', 'contrast', c => { state.contrast = +c; refresh(); renderCoverResult(); });
  matchMedia('(prefers-color-scheme: dark)').addEventListener('change', renderPalette);

  /* cover extraction demo */
  const PICKS = [0, 1, 2, 3, 6, 8];
  let picked = 6;
  $('#coverPick').innerHTML = PICKS.map(c => `<button type="button" class="swatch cover" style="width:100%;height:auto;aspect-ratio:3/4;border-radius:14px;background-image:url(${cover(c, 'S')})" aria-label="封面 ${c + 1}" aria-pressed="${c === picked}" data-c="${c}"></button>`).join('');
  $('#coverPick').addEventListener('click', e => { const b = e.target.closest('button'); if (!b) return; picked = +b.dataset.c; $$('button', $('#coverPick')).forEach(x => x.setAttribute('aria-pressed', String(x === b))); renderCoverResult(); });
  function renderCoverResult() {
    const seeds = coverSeeds(picked), v = VIDS.find(x => x.c === picked) || { t: PGCS.find(p => p.c === picked)?.t || '示例视频', up: '示例 UP 主' };
    $('#coverResult').innerHTML = `<div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap"><span class="muted" style="font-size:13px">Score 排序后的候选种子</span>${seeds.map((s, i) => `<span style="display:inline-flex;align-items:center;gap:6px;padding:4px 10px 4px 4px;border-radius:999px;background:var(--surface-container-high);font:500 12px var(--font-mono)"><i style="width:22px;height:22px;border-radius:${i ? '50%' : '30%'};background:${s};display:inline-block"></i>${s.toUpperCase()}</span>`).join('')}</div>
      <div class="scr scope" data-seed-hex="${seeds[0]}" style="border-radius:28px;padding:16px;display:grid;grid-template-columns:120px minmax(0,1fr);gap:16px;align-items:center;font-size:14px">
        <div class="m-cv" style="aspect-ratio:3/4">${img(picked, 'S')}</div>
        <div class="m-col" style="gap:10px;min-width:0"><span class="m-eyebrow">局部主题预览</span><b class="m-t2" style="font-size:16px;line-height:1.4">${v.t}</b>
          <div style="display:flex;gap:8px;flex-wrap:wrap"><span class="m-btn fill" style="height:36px">${ic('play')}播放</span><span class="m-btn t" style="height:36px">${ic('add')}追番</span></div>
          <div class="m-bg" style="max-width:300px">${ACTIONS.map(([i, l, on]) => `<div class="${on ? 'on' : ''}">${ic(i)}${l}</div>`).join('')}</div></div></div>
      <button type="button" class="m-btn" id="useCoverSeed" style="justify-self:start;border:0;cursor:pointer;background:var(--secondary-container);color:var(--on-secondary-container);font-family:var(--font-body)">把 ${seeds[0].toUpperCase()} 设为全局种子色</button>`;
    applyScopes($('#coverResult'));
    $('#useCoverSeed').onclick = () => { state.seed = seeds[0]; state.fromCover = picked; renderSeeds(); refresh(); };
  }

  /* ---------- device frames ---------- */
  const PAD = { phone: 10, phoneL: 10, tablet: 16, tv: 8 };
  const ro = new ResizeObserver(es => es.forEach(e => { const f = e.target; if (f._dims && f.clientWidth !== f._w) fitFrame(f); }));
  function fitFrame(fit) {
    const [fw, fh] = fit._dims, w = fit.clientWidth, maxh = +fit.dataset.maxh || Infinity;
    fit._w = w; const s = Math.min(w / fw, maxh / fh);
    const fr = fit.firstElementChild; fr.style.transform = `scale(${s})`; fr.style.left = ((w - fw * s) / 2) + 'px';
    fit.style.height = (fh * s) + 'px';
  }
  function mount(fit, id, dev) {
    const sc = SCREENS.find(s => s.id === id);
    const key = id === 'player' && dev === 'phone' ? 'phoneL' : dev;
    const [w, h] = DEV[key], pad = PAD[key];
    const cinema = dev === 'tv' || id === 'player' || id === 'shorts';
    const frameCls = dev === 'phone' ? 'phone' : dev;
    fit.innerHTML = `<div class="frame ${frameCls}"><div class="viewport"><div class="scr ${dev}${cinema ? ' cinema' : ''}" data-screen="${id}" style="width:${w}px;height:${h}px"${dev === 'tv' ? ' tabindex="0" role="application" aria-label="电视界面原型：点击后用方向键移动焦点"' : ''}>${sc.fn[dev]()}</div></div></div>`;
    fit._dims = [w + pad * 2, h + pad * 2]; fit._w = -1;
    fitFrame(fit); ro.observe(fit);
    applyScopes(fit);
    const scr = $('.scr', fit);
    if (dev === 'tv') initTv(scr);
    if (id === 'shorts' && dev !== 'tv') initFeed(scr);
  }

  /* ---------- TV focus engine: 2D focus search like Compose's FocusOwner ---------- */
  function initTv(scr) {
    const list = () => $$('[data-f]', scr);
    let cur = $('[data-start]', scr) || list()[0];
    if (cur) cur.classList.add('fx');
    const rail = $('.m-tvrail', scr);
    function move(n) {
      if (!n || n === cur) return;
      cur.classList.remove('fx'); cur = n; n.classList.add('fx');
      if (rail) rail.classList.toggle('open', rail.contains(n));
      const row = n.closest('.tvrow');
      if (row) row.scrollTo({ left: Math.max(0, n.offsetLeft - row.clientWidth * .15), behavior: reduced ? 'auto' : 'smooth' });
    }
    scr.addEventListener('keydown', e => {
      const dir = { ArrowLeft: 'l', ArrowRight: 'r', ArrowUp: 'u', ArrowDown: 'd' }[e.key];
      if (!dir && e.key !== 'Enter' && e.key !== ' ') return;
      e.preventDefault();
      if (!dir) { cur.animate([{ transform: getComputedStyle(cur).transform }, { transform: 'scale(.94)' }, { transform: getComputedStyle(cur).transform }], { duration: 260 }); return; }
      if (scr.dataset.screen === 'shorts' && cur.hasAttribute('data-start') && (dir === 'u' || dir === 'd')) { swapShort(scr, dir === 'd' ? 1 : -1); return; }
      move(nearest(cur, dir, list()));
    });
    scr.addEventListener('click', e => { const t = e.target.closest('[data-f]'); scr.focus({ preventScroll: true }); if (t) move(t); });
  }
  function nearest(cur, dir, list) {
    const a = cur.getBoundingClientRect(), horiz = dir === 'l' || dir === 'r', sign = dir === 'r' || dir === 'd' ? 1 : -1;
    let best = null, bestScore = Infinity;
    for (const el of list) {
      if (el === cur || el.contains(cur) || cur.contains(el)) continue;
      const b = el.getBoundingClientRect();
      const main = horiz ? (sign > 0 ? b.left - a.right : a.left - b.right) : (sign > 0 ? b.top - a.bottom : a.top - b.bottom);
      const center = horiz ? (sign * ((b.left + b.right) - (a.left + a.right)) / 2) : (sign * ((b.top + b.bottom) - (a.top + a.bottom)) / 2);
      if (center <= 2) continue;
      const overlap = horiz ? Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top) : Math.min(a.right, b.right) - Math.max(a.left, b.left);
      const cross = horiz ? Math.abs((b.top + b.bottom) - (a.top + a.bottom)) / 2 : Math.abs((b.left + b.right) - (a.left + a.right)) / 2;
      const score = (overlap > 0 ? 0 : 1e5) + Math.max(main, 0) * 13 + cross * cross / 40;
      if (score < bestScore) { bestScore = score; best = el; }
    }
    return best;
  }
  let shortIdx = 0;
  function swapShort(scr, delta) {
    shortIdx = (shortIdx + delta + SHORTS.length) % SHORTS.length;
    const s = SHORTS[shortIdx];
    $$('[data-seed-cover]', scr).forEach(el => { el.dataset.seedCover = s.c; });
    $$('.m-ambient img, [data-start] > img', scr).forEach(im => { im.src = cover(s.c, 'P'); });
    const info = $$('[data-seed-cover]', scr)[1];
    if (info) { const t = info.children[1]; if (t) t.textContent = s.t; const u = $('b', info); if (u) u.textContent = s.up; }
    applyScopes(scr);
    const v = $('[data-start]', scr); v.animate([{ transform: `translateY(${delta * 40}px)`, opacity: .3 }, { transform: 'none', opacity: 1 }], { duration: reduced ? 1 : 420, easing: 'cubic-bezier(.2,0,0,1)' });
  }

  /* ---------- phone feed: double-tap like ---------- */
  function initFeed(scr) {
    scr.addEventListener('dblclick', e => {
      const slide = e.target.closest('.m-slide'); if (!slide) return;
      const r = slide.getBoundingClientRect(), k = r.width / slide.offsetWidth;
      const h = document.createElement('span'); h.className = 'm-heart';
      h.style.left = ((e.clientX - r.left) / k - 48) + 'px'; h.style.top = ((e.clientY - r.top) / k - 48) + 'px';
      h.innerHTML = `<svg viewBox="0 0 24 24"><path d="${ICONS.heart}"/></svg>`;
      slide.appendChild(h); setTimeout(() => h.remove(), 900);
      const b = slide.querySelector('.m-acts > div:nth-child(2) .b'); if (b) b.classList.add('on');
    });
  }

  /* ---------- screens section ---------- */
  const DEV_NAMES = { phone: ['phone', '手机 · Compact'], tablet: ['tablet', '平板 · Expanded'], tv: ['tv', '电视 · Far + 方向键'] };
  let curScreen = 'home', curDev = 'tv';
  $('#screenSeg').innerHTML = SCREENS.map(s => `<button type="button" data-id="${s.id}" aria-pressed="${s.id === curScreen}">${s.name}</button>`).join('');
  function renderScreen() {
    const s = SCREENS.find(x => x.id === curScreen), stage = $('#screenStage');
    stage.dataset.maxh = curDev === 'phone' ? 680 : '';
    mount(stage, curScreen, curDev);
    $('#screenCap').innerHTML = `${s.name} · <b>${DEV_NAMES[curDev][1]}</b>${curDev === 'tv' ? ' · 点击画面后用方向键' : ''}`;
    $('#screenRules').innerHTML = `<div class="card" style="gap:12px"><span class="eyebrow">用到的组件</span><div style="display:flex;flex-wrap:wrap;gap:6px">${s.comps.map(c => `<span class="pill o">${c}</span>`).join('')}</div></div>`
      + ['phone', 'tablet', 'tv'].map(d => `<div class="card${d === curDev ? ' cur' : ''}"><h3 style="font-size:16px"><span class="ico">${ic(DEV_NAMES[d][0])}</span>${DEV_NAMES[d][1]}</h3>${UL(s.rules[d])}</div>`).join('');
  }
  $('#screenSeg').addEventListener('click', e => { const b = e.target.closest('button'); if (!b) return; curScreen = b.dataset.id; $$('button', $('#screenSeg')).forEach(x => x.setAttribute('aria-pressed', String(x === b))); renderScreen(); });
  bindSeg('#deviceSeg', 'dev', d => { curDev = d; renderScreen(); });

  /* ---------- shapes + loader + focus demo ---------- */
  const SHAPE_USE = [['Cookie9Sided', '头像'], ['Sunny', '等级徽章'], ['Clover4Leaf', '设置图标容器'], ['SoftBurst', '点赞反馈'], ['Pentagon', '加载'], ['Pill', '选中按钮'], ['Flower', '空状态插图'], ['Gem', '大会员标识']];
  const shapeFill = ['primary-container', 'tertiary-container', 'secondary-container', 'primary'];
  $('#shapes').innerHTML = SHAPE_USE.map(([n, u], i) => `<div style="display:grid;gap:6px;justify-items:center;padding:14px 8px;border-radius:20px;background:var(--surface-container);min-width:0"><svg viewBox="-52 -52 104 104" width="64" height="64" aria-hidden="true"><path d="${radiiToPath(shapeRadii(n), 50, -Math.PI / 2)}" fill="var(--${shapeFill[i % 4]})"/></svg><b style="font:600 12px var(--font-display);text-align:center;overflow-wrap:anywhere">${n}</b><span class="muted" style="font-size:12px">${u}</span></div>`).join('');
  const LOADER = ['SoftBurst', 'Cookie9Sided', 'Pentagon', 'Pill', 'Sunny', 'Cookie4Sided', 'Circle'].map(shapeRadii);
  const path = $('#loader path');
  if (reduced) path.setAttribute('d', radiiToPath(LOADER[1], 46));
  else {
    const spring = t => 1 - Math.exp(-6 * t) * Math.cos(9 * t);
    const t0 = performance.now(), step = 650;
    const tick = now => {
      const el = Math.max(0, now - t0), i = Math.floor(el / step) % LOADER.length, p = Math.min(1, (el % step) / step * 1.6);
      const A = LOADER[i], B = LOADER[(i + 1) % LOADER.length], e = spring(p);
      path.setAttribute('d', radiiToPath(A.map((v, k) => v + (B[k] - v) * e), 46, el * .004 + i * .9));
      requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  }
  $('#focusDemo').innerHTML = [1, 2, 3, 6].map(i => vcard(VIDS[i], { f: 1, noMeta: true })).join('');
  $$('#focusDemo .m-vc').forEach(c => {
    c.tabIndex = 0;
    const on = () => c.classList.add('fx'), off = () => c.classList.remove('fx');
    c.addEventListener('mouseenter', on); c.addEventListener('mouseleave', () => { if (document.activeElement !== c) off(); });
    c.addEventListener('focus', on); c.addEventListener('blur', off);
  });

  /* ---------- toc highlight ---------- */
  const links = $$('.toc nav a');
  const io = new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting) links.forEach(a => a.classList.toggle('on', a.getAttribute('href') === '#' + e.target.id)); }), { rootMargin: '-40% 0px -55% 0px' });
  $$('section.sec').forEach(s => io.observe(s));

  /* ---------- boot ---------- */
  renderSeeds();
  refresh();
  $$('.fit[data-device]').forEach(f => mount(f, f.dataset.screen, f.dataset.device));
  mount($('#shortsPhone'), 'shorts', 'phone');
  renderScreen();
  renderCoverResult();
  import('https://cdn.jsdelivr.net/npm/@material/material-color-utilities@0.3.0/+esm')
    .then(mod => { M = mod; schemeCache.clear(); seedCache.clear(); if (state.fromCover != null) state.seed = coverSeeds(state.fromCover)[0]; refresh(); renderCoverResult(); })
    .catch(() => { $('#engineStatus').textContent = '配色库加载失败，正在使用近似算法（颜色与真实结果有差异）'; });
})();
</script>
