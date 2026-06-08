'use strict';
// ─────────────────────────────────────────────
//  CHRONO RIFT  –  core.js
//  Engine: constants, input, audio, game loop
// ─────────────────────────────────────────────

const W = 320, H = 240, TS = 16; // canvas size, tile size

// ── Canvas & scale ──────────────────────────
const canvas = document.getElementById('c');
const ctx    = canvas.getContext('2d');
ctx.imageSmoothingEnabled = false;

let SCALE = 1;
function resizeCanvas() {
  SCALE = Math.max(1, Math.min(
    Math.floor(window.innerWidth  / W),
    Math.floor(window.innerHeight / H)
  ));
  canvas.width  = W;
  canvas.height = H;
  canvas.style.width  = (W * SCALE) + 'px';
  canvas.style.height = (H * SCALE) + 'px';
}
resizeCanvas();
window.addEventListener('resize', resizeCanvas);

// ── Input ────────────────────────────────────
const Input = (() => {
  const cur  = {};
  const prev = {};
  const touchMap = {};  // button-id -> key

  window.addEventListener('keydown', e => {
    cur[e.key] = true;
    if (['ArrowUp','ArrowDown','ArrowLeft','ArrowRight',' '].includes(e.key)) e.preventDefault();
  });
  window.addEventListener('keyup',   e => { cur[e.key] = false; });

  // Touch controls
  const btnMap = {
    bu:'ArrowUp', bd:'ArrowDown', bl:'ArrowLeft', br:'ArrowRight',
    bz:'z', bx:'x', bst:'Enter'
  };
  Object.entries(btnMap).forEach(([id, key]) => {
    const el = document.getElementById(id);
    if (!el) return;
    el.addEventListener('touchstart', e => { e.preventDefault(); cur[key] = true;  }, {passive:false});
    el.addEventListener('touchend',   e => { e.preventDefault(); cur[key] = false; }, {passive:false});
    el.addEventListener('touchcancel',e => { e.preventDefault(); cur[key] = false; }, {passive:false});
  });

  return {
    down(k)    { return !!cur[k]; },
    pressed(k) { return !!cur[k] && !prev[k]; },
    dir()      {
      let x=0,y=0;
      if(cur['ArrowLeft']  || cur['a']) x=-1;
      if(cur['ArrowRight'] || cur['d']) x= 1;
      if(cur['ArrowUp']    || cur['w']) y=-1;
      if(cur['ArrowDown']  || cur['s']) y= 1;
      return {x,y};
    },
    confirm()  { return this.pressed('z') || this.pressed('Enter') || this.pressed(' '); },
    cancel()   { return this.pressed('x') || this.pressed('Escape'); },
    menu()     { return this.pressed('Enter') || this.pressed('Escape'); },
    tick()     { Object.keys(cur).forEach(k => prev[k] = cur[k]); }
  };
})();

// ── Audio ────────────────────────────────────
const Audio = (() => {
  let actx = null, masterGain, musicGain, sfxGain;
  let seq = null;      // current music sequencer
  let muted = false;

  function init() {
    if (actx) return;
    try {
      actx = new (window.AudioContext || window.webkitAudioContext)();
      masterGain = actx.createGain(); masterGain.connect(actx.destination);
      musicGain  = actx.createGain(); musicGain.connect(masterGain);
      sfxGain    = actx.createGain(); sfxGain.connect(masterGain);
      masterGain.gain.value = 0.7;
      musicGain.gain.value  = 0.35;
      sfxGain.gain.value    = 0.55;
    } catch(e) { actx = null; }
  }

  function resume() { if (actx && actx.state === 'suspended') actx.resume(); }

  function osc(freq, type, dur, gain, dest) {
    if (!actx) return;
    dest = dest || sfxGain;
    const o = actx.createOscillator();
    const g = actx.createGain();
    o.connect(g); g.connect(dest);
    o.type = type; o.frequency.value = freq;
    const t = actx.currentTime;
    g.gain.setValueAtTime(gain, t);
    g.gain.exponentialRampToValueAtTime(0.001, t + dur);
    o.start(t); o.stop(t + dur + 0.01);
  }

  const SFX = {
    cursor()  { osc(440,'square',.05,.12); },
    confirm() { osc(523,'square',.07,.12); setTimeout(()=>osc(659,'square',.07,.12),60); },
    cancel()  { osc(330,'square',.09,.12); setTimeout(()=>osc(262,'square',.09,.12),75); },
    attack()  { osc(220,'sawtooth',.06,.2); setTimeout(()=>osc(165,'square',.05,.18),45); },
    magic()   { [523,659,784,1047].forEach((f,i)=>setTimeout(()=>osc(f,'sine',.12,.16),i*70)); },
    heal()    { [523,659,784].forEach((f,i)=>setTimeout(()=>osc(f,'sine',.1,.15),i*55)); },
    hurt()    { osc(280,'square',.14,.22); },
    boss()    { [200,250,180,300].forEach((f,i)=>setTimeout(()=>osc(f,'sawtooth',.12,.25),i*60)); },
    levelup() { [262,330,392,523,659,784].forEach((f,i)=>setTimeout(()=>osc(f,'square',.18,.2),i*75)); },
    victory() { [523,659,784,1047,784,659,523].forEach((f,i)=>setTimeout(()=>osc(f,'square',.14,.22),i*90)); },
    chest()   { [392,523,659,784].forEach((f,i)=>setTimeout(()=>osc(f,'square',.12,.18),i*90)); },
    warp()    { for(let i=0;i<18;i++) setTimeout(()=>osc(200+i*55,'sine',.04,.12),i*28); },
    gameover(){ [440,392,349,294,262,220].forEach((f,i)=>setTimeout(()=>osc(f,'sawtooth',.2,.22),i*120)); },
    step()    { osc(90+Math.random()*30,'square',.015,.04); },
    join()    { [330,392,440,523,440,523,659].forEach((f,i)=>setTimeout(()=>osc(f,'square',.1,.15),i*60)); },
  };

  // Music patterns (arrays of steps; each step: array of {f,t,d,g} or null)
  const MUSIC = {
    title: {bpm:80, notes:[
      [{f:262,t:'sine',d:.4,g:.12}],null,null,null,
      [{f:330,t:'sine',d:.4,g:.12}],null,null,null,
      [{f:392,t:'sine',d:.4,g:.12}],null,null,null,
      [{f:523,t:'sine',d:.6,g:.12}],null,null,null,
      null,null,null,null,
      [{f:494,t:'sine',d:.4,g:.12}],null,null,null,
      [{f:440,t:'sine',d:.4,g:.12}],null,null,null,
      [{f:392,t:'sine',d:.8,g:.12}],null,null,null,null,null,null,null,
    ]},
    village: {bpm:105, notes:[
      [{f:392,t:'square',d:.18,g:.09}],[{f:440,t:'square',d:.18,g:.09}],
      [{f:392,t:'square',d:.18,g:.09}],null,
      [{f:349,t:'square',d:.18,g:.09}],[{f:392,t:'square',d:.18,g:.09}],
      [{f:440,t:'square',d:.28,g:.09}],null,
      [{f:523,t:'square',d:.18,g:.09}],[{f:440,t:'square',d:.18,g:.09}],
      [{f:392,t:'square',d:.18,g:.09}],null,
      [{f:330,t:'square',d:.18,g:.09}],[{f:349,t:'square',d:.18,g:.09}],
      [{f:392,t:'square',d:.38,g:.09}],null,
    ]},
    overworld: {bpm:120, notes:[
      [{f:330,t:'square',d:.12,g:.1}],[{f:392,t:'square',d:.12,g:.1}],
      [{f:440,t:'square',d:.12,g:.1}],[{f:523,t:'square',d:.12,g:.1}],
      [{f:440,t:'square',d:.12,g:.1}],[{f:392,t:'square',d:.12,g:.1}],
      [{f:349,t:'square',d:.12,g:.1}],[{f:330,t:'square',d:.12,g:.1}],
      [{f:294,t:'square',d:.12,g:.1}],[{f:330,t:'square',d:.12,g:.1}],
      [{f:392,t:'square',d:.12,g:.1}],[{f:440,t:'square',d:.12,g:.1}],
      [{f:494,t:'square',d:.12,g:.1}],[{f:523,t:'square',d:.12,g:.1}],
      [{f:587,t:'square',d:.12,g:.1}],[{f:523,t:'square',d:.12,g:.1}],
    ]},
    dungeon: {bpm:75, notes:[
      [{f:196,t:'sawtooth',d:.3,g:.09}],null,
      [{f:185,t:'sawtooth',d:.3,g:.09}],null,
      [{f:175,t:'sawtooth',d:.3,g:.09}],null,
      [{f:165,t:'sawtooth',d:.5,g:.09}],null,
      [{f:175,t:'sawtooth',d:.3,g:.09}],null,
      [{f:196,t:'sawtooth',d:.3,g:.09}],null,
      [{f:220,t:'sawtooth',d:.3,g:.09}],null,
      [{f:196,t:'sawtooth',d:.5,g:.09}],null,
    ]},
    battle: {bpm:155, notes:[
      [{f:330,t:'square',d:.08,g:.11},{f:165,t:'square',d:.08,g:.07}],
      [{f:330,t:'square',d:.08,g:.11}],
      [{f:392,t:'square',d:.08,g:.11},{f:196,t:'square',d:.08,g:.07}],
      [{f:440,t:'square',d:.12,g:.11}],
      [{f:330,t:'square',d:.08,g:.11}],[{f:330,t:'square',d:.08,g:.11}],
      [{f:349,t:'square',d:.08,g:.11},{f:174,t:'square',d:.08,g:.07}],
      [{f:330,t:'square',d:.12,g:.11}],
      [{f:294,t:'square',d:.08,g:.11},{f:147,t:'square',d:.08,g:.07}],
      [{f:294,t:'square',d:.08,g:.11}],
      [{f:330,t:'square',d:.08,g:.11},{f:165,t:'square',d:.08,g:.07}],
      [{f:392,t:'square',d:.12,g:.11}],
      [{f:440,t:'square',d:.08,g:.11}],[{f:523,t:'square',d:.08,g:.11}],
      [{f:440,t:'square',d:.08,g:.11},{f:220,t:'square',d:.08,g:.07}],
      [{f:392,t:'square',d:.12,g:.11}],
    ]},
    boss: {bpm:175, notes:[
      [{f:110,t:'sawtooth',d:.1,g:.13},{f:440,t:'square',d:.1,g:.09}],
      [{f:110,t:'sawtooth',d:.1,g:.13}],
      [{f:110,t:'sawtooth',d:.1,g:.13},{f:523,t:'square',d:.1,g:.09}],
      [{f:110,t:'sawtooth',d:.1,g:.13}],
      [{f:116,t:'sawtooth',d:.1,g:.13},{f:494,t:'square',d:.1,g:.09}],
      [{f:116,t:'sawtooth',d:.1,g:.13}],
      [{f:116,t:'sawtooth',d:.1,g:.13},{f:440,t:'square',d:.1,g:.09}],
      [{f:116,t:'sawtooth',d:.1,g:.13}],
      [{f:98,t:'sawtooth',d:.1,g:.13},{f:392,t:'square',d:.1,g:.09}],
      [{f:98,t:'sawtooth',d:.1,g:.13}],
      [{f:110,t:'sawtooth',d:.1,g:.13},{f:440,t:'square',d:.1,g:.09}],
      [{f:110,t:'sawtooth',d:.1,g:.13}],
      [{f:123,t:'sawtooth',d:.1,g:.13},{f:523,t:'square',d:.1,g:.09}],
      [{f:123,t:'sawtooth',d:.1,g:.13}],
      [{f:130,t:'sawtooth',d:.1,g:.13},{f:587,t:'square',d:.1,g:.09}],
      [{f:130,t:'sawtooth',d:.1,g:.13}],
    ]},
    ending: {bpm:70, notes:[
      [{f:523,t:'sine',d:.5,g:.13}],null,null,null,
      [{f:659,t:'sine',d:.5,g:.13}],null,null,null,
      [{f:784,t:'sine',d:.5,g:.13}],null,null,null,
      [{f:1047,t:'sine',d:.8,g:.13}],null,null,null,null,null,null,null,
      [{f:880,t:'sine',d:.5,g:.13}],null,null,null,
      [{f:784,t:'sine',d:.5,g:.13}],null,null,null,
      [{f:659,t:'sine',d:.8,g:.13}],null,null,null,null,null,null,null,
    ]},
  };

  function playTrack(name) {
    stopMusic();
    if (!actx || !MUSIC[name]) return;
    const track = MUSIC[name];
    const stepMs = (60000 / track.bpm) / 4;
    let step = 0;
    function tick() {
      if (!actx) return;
      const notes = track.notes[step % track.notes.length];
      if (notes) notes.forEach(n => n && osc(n.f, n.t, n.d, n.g, musicGain));
      step++;
    }
    tick();
    seq = setInterval(tick, stepMs);
  }

  function stopMusic() {
    if (seq) { clearInterval(seq); seq = null; }
  }

  return {
    init, resume, stopMusic,
    sfx(name) {
      init(); resume();
      if (muted || !actx) return;
      SFX[name] && SFX[name]();
    },
    playMusic(name) {
      init(); resume();
      if (muted) return;
      playTrack(name);
    },
    toggleMute() { muted = !muted; if (muted) stopMusic(); }
  };
})();

// ── Text rendering ────────────────────────────
// Tiny 5×7 pixel font (uppercase + digits + symbols)
const FONT = {};
(function buildFont() {
  const chars = {
    'A':'01110 10001 10001 11111 10001 10001 10001',
    'B':'11110 10001 10001 11110 10001 10001 11110',
    'C':'01110 10001 10000 10000 10000 10001 01110',
    'D':'11100 10010 10001 10001 10001 10010 11100',
    'E':'11111 10000 10000 11110 10000 10000 11111',
    'F':'11111 10000 10000 11110 10000 10000 10000',
    'G':'01110 10001 10000 10111 10001 10001 01111',
    'H':'10001 10001 10001 11111 10001 10001 10001',
    'I':'01110 00100 00100 00100 00100 00100 01110',
    'J':'00111 00010 00010 00010 10010 10010 01100',
    'K':'10001 10010 10100 11000 10100 10010 10001',
    'L':'10000 10000 10000 10000 10000 10000 11111',
    'M':'10001 11011 10101 10001 10001 10001 10001',
    'N':'10001 11001 10101 10011 10001 10001 10001',
    'O':'01110 10001 10001 10001 10001 10001 01110',
    'P':'11110 10001 10001 11110 10000 10000 10000',
    'Q':'01110 10001 10001 10001 10101 10010 01101',
    'R':'11110 10001 10001 11110 10100 10010 10001',
    'S':'01111 10000 10000 01110 00001 00001 11110',
    'T':'11111 00100 00100 00100 00100 00100 00100',
    'U':'10001 10001 10001 10001 10001 10001 01110',
    'V':'10001 10001 10001 10001 10001 01010 00100',
    'W':'10001 10001 10001 10101 10101 11011 10001',
    'X':'10001 10001 01010 00100 01010 10001 10001',
    'Y':'10001 10001 01010 00100 00100 00100 00100',
    'Z':'11111 00001 00010 00100 01000 10000 11111',
    '0':'01110 10001 10011 10101 11001 10001 01110',
    '1':'00100 01100 00100 00100 00100 00100 01110',
    '2':'01110 10001 00001 00010 00100 01000 11111',
    '3':'01110 10001 00001 00110 00001 10001 01110',
    '4':'00010 00110 01010 10010 11111 00010 00010',
    '5':'11111 10000 10000 11110 00001 00001 11110',
    '6':'00110 01000 10000 11110 10001 10001 01110',
    '7':'11111 00001 00010 00100 01000 01000 01000',
    '8':'01110 10001 10001 01110 10001 10001 01110',
    '9':'01110 10001 10001 01111 00001 00010 01100',
    '.':'00000 00000 00000 00000 00000 00000 00100',
    ',':'00000 00000 00000 00000 00000 00100 01000',
    '!':'00100 00100 00100 00100 00100 00000 00100',
    '?':'01110 10001 00001 00010 00100 00000 00100',
    ':':'00000 00100 00100 00000 00100 00100 00000',
    "'":"00100 00100 01000 00000 00000 00000 00000",
    '-':'00000 00000 00000 11111 00000 00000 00000',
    '/':'00001 00010 00010 00100 01000 01000 10000',
    ' ':'00000 00000 00000 00000 00000 00000 00000',
    '%':'11001 11010 00100 00100 01011 10011 00000',
    '+':'00000 00100 00100 11111 00100 00100 00000',
    '*':'00000 10101 01110 11111 01110 10101 00000',
    '(':'00010 00100 01000 01000 01000 00100 00010',
    ')':'01000 00100 00010 00010 00010 00100 01000',
  };
  Object.entries(chars).forEach(([c,rows]) => {
    const bits = rows.split(' ');
    FONT[c] = bits.map(r => r.split('').map(Number));
  });
})();

function drawText(c2d, text, x, y, color='#fff', scale=1) {
  const t = text.toUpperCase();
  let cx = x;
  c2d.fillStyle = color;
  for (const ch of t) {
    const glyph = FONT[ch] || FONT[' '];
    for (let row=0;row<glyph.length;row++) {
      for (let col=0;col<glyph[row].length;col++) {
        if (glyph[row][col]) {
          c2d.fillRect(cx + col*scale, y + row*scale, scale, scale);
        }
      }
    }
    cx += (6)*scale;
  }
}

function textWidth(text, scale=1) { return text.length * 6 * scale; }

function drawTextShadow(c2d, text, x, y, color='#fff', shadow='#000', scale=1) {
  drawText(c2d, text, x+scale, y+scale, shadow, scale);
  drawText(c2d, text, x, y, color, scale);
}

// ── Easing & Math utils ───────────────────────
function lerp(a, b, t) { return a + (b-a)*t; }
function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }
function randInt(lo, hi) { return Math.floor(Math.random()*(hi-lo+1))+lo; }
function randChoice(arr) { return arr[Math.floor(Math.random()*arr.length)]; }

// ── Game state ────────────────────────────────
const State = {
  party:        [],
  inventory:    [],
  flags:        {},
  mapId:        'village',
  playerX:      12,
  playerY:      16,
  playerFacing: 2, // 0=up,1=right,2=down,3=left
  gold:         50,
  steps:        0,
  chestedMaps:  {}, // mapId -> set of chest indices opened
};

// ── Scene manager ─────────────────────────────
const SceneMgr = (() => {
  let current = null;
  let alpha = 0, fadingIn = false, fadingOut = false;
  let pendingScene = null, pendingData = null;
  const FADE_SPD = 2.5;

  return {
    set(scene, data) {
      if (fadingOut) return;
      fadingOut = true;
      alpha = 0;
      pendingScene = scene;
      pendingData  = data;
    },
    boot(scene, data) {
      current = new scene(data);
      current.enter && current.enter();
      alpha = 1;
      fadingIn = true;
      fadingOut = false;
    },
    update(dt) {
      if (fadingOut) {
        alpha = Math.min(1, alpha + FADE_SPD * dt);
        if (alpha >= 1) {
          fadingOut = false;
          if (current && current.exit) current.exit();
          current = new pendingScene(pendingData);
          current.enter && current.enter();
          fadingIn = true;
        }
      } else if (fadingIn) {
        alpha = Math.max(0, alpha - FADE_SPD * dt);
        if (alpha <= 0) fadingIn = false;
      }
      current && current.update && current.update(dt);
    },
    draw(c2d) {
      current && current.draw && current.draw(c2d);
      if (alpha > 0) {
        c2d.fillStyle = `rgba(0,0,0,${alpha})`;
        c2d.fillRect(0,0,W,H);
      }
    }
  };
})();

// ── Main loop ─────────────────────────────────
let lastTs = 0;
function loop(ts) {
  const dt = Math.min((ts - lastTs) / 1000, 0.05);
  lastTs = ts;
  ctx.clearRect(0,0,W,H);
  SceneMgr.update(dt);
  SceneMgr.draw(ctx);
  Input.tick();
  requestAnimationFrame(loop);
}
