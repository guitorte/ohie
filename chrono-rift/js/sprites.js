'use strict';
// ─────────────────────────────────────────────
//  sprites.js  –  all pixel-art drawing
// ─────────────────────────────────────────────

// ── Tile palette ──────────────────────────────
const TILE_COLORS = {
  0:  ['#3a9a3a','#2d7a2d'],  // grass
  1:  ['#2244cc','#1a33aa'],  // water
  2:  ['#1a5e1a','#0e3d0e'],  // tree
  3:  ['#888888','#666666'],  // mountain
  4:  ['#888877','#666655'],  // stone floor
  5:  ['#445566','#334455'],  // wall
  6:  ['#aa8855','#886633'],  // path/cobble
  7:  ['#ddbb77','#ccaa55'],  // sand
  8:  ['#3a3a3a','#282828'],  // dark floor
  9:  ['#2a2a1a','#1a1a0e'],  // cave wall
  10: ['#cc4400','#aa3300'],  // lava
  11: ['#112288','#0a1855'],  // deep water
  12: ['#aaddff','#88ccee'],  // ice
  13: ['#886655','#664433'],  // house wall
  14: ['#bb4422','#993311'],  // roof
  15: ['#664422','#442200'],  // door
  16: ['#4a7a9a','#385e78'],  // shallow water / shore
  17: ['#cc8833','#aa6622'],  // festival decoration
  18: ['#550055','#330033'],  // shadow tile
  19: ['#7a9a3a','#5a7a2a'],  // light grass (path edge)
};

const TILE_SOLID = new Set([1,2,3,5,9,10,11,13,14]);

function isSolid(id) { return TILE_SOLID.has(id); }

function drawTile(c2d, id, px, py) {
  const [fill, dark] = TILE_COLORS[id] || TILE_COLORS[0];
  c2d.fillStyle = fill;
  c2d.fillRect(px, py, TS, TS);

  // Simple detail per tile type
  c2d.fillStyle = dark;
  if (id === 0) { // grass – wavy lines
    c2d.fillRect(px+2,py+4,2,1); c2d.fillRect(px+8,py+6,2,1); c2d.fillRect(px+12,py+3,2,1);
  } else if (id === 1) { // water – ripples
    c2d.fillRect(px+1,py+3,4,1); c2d.fillRect(px+9,py+7,5,1); c2d.fillRect(px+3,py+11,4,1);
  } else if (id === 2) { // tree – canopy
    c2d.fillStyle = '#0e3d0e';
    c2d.fillRect(px+4,py+1,8,6); c2d.fillRect(px+3,py+5,10,8);
    c2d.fillStyle = '#22661a';
    c2d.fillRect(px+5,py+2,6,4); c2d.fillRect(px+4,py+6,8,5);
    c2d.fillStyle = '#664433';
    c2d.fillRect(px+6,py+11,4,5);
  } else if (id === 3) { // mountain
    c2d.fillStyle = '#888';
    c2d.fillRect(px+6,py+1,4,3); c2d.fillRect(px+4,py+3,8,4); c2d.fillRect(px+2,py+6,12,4);
    c2d.fillStyle = '#fff';
    c2d.fillRect(px+7,py+1,2,2); // snow cap
    c2d.fillStyle = '#555';
    c2d.fillRect(px+3,py+8,4,3); c2d.fillRect(px+10,py+7,3,4);
  } else if (id === 4) { // stone floor – grid
    c2d.fillRect(px,py+8,TS,1); c2d.fillRect(px+8,py,1,TS);
  } else if (id === 5) { // wall – bricks
    c2d.fillRect(px,py+4,TS,1); c2d.fillRect(px,py+9,TS,1);
    c2d.fillRect(px+3,py,1,4); c2d.fillRect(px+11,py+5,1,4); c2d.fillRect(px+7,py+10,1,6);
  } else if (id === 6) { // path – stones
    c2d.fillRect(px+1,py+1,5,5); c2d.fillRect(px+9,py+4,5,5); c2d.fillRect(px+3,py+10,6,4);
    c2d.fillStyle = fill;
    c2d.fillRect(px+2,py+2,3,3); c2d.fillRect(px+10,py+5,3,3);
  } else if (id === 8) { // dark floor
    c2d.fillRect(px,py+8,TS,1); c2d.fillRect(px+8,py,1,TS);
    c2d.fillStyle = '#222'; c2d.fillRect(px+2,py+2,2,2); c2d.fillRect(px+10,py+10,2,2);
  } else if (id === 13) { // house wall – stones
    c2d.fillRect(px,py+5,TS,1); c2d.fillRect(px,py+11,TS,1);
    c2d.fillRect(px+4,py,1,5); c2d.fillRect(px+10,py+6,1,5); c2d.fillRect(px+6,py+12,1,4);
  } else if (id === 14) { // roof – tiles
    for(let r=0;r<4;r++) for(let cc=0;cc<4;cc++) {
      c2d.fillRect(px+cc*4+(r%2)*2,py+r*4,3,1);
    }
  } else if (id === 15) { // door
    c2d.fillStyle = '#886633';
    c2d.fillRect(px+3,py+3,10,13);
    c2d.fillStyle = '#cc9944';
    c2d.fillRect(px+4,py+4,4,5); c2d.fillRect(px+9,py+4,3,5);
    c2d.fillStyle = '#ffcc55'; c2d.fillRect(px+11,py+7,2,2); // knob
  } else if (id === 17) { // festival deco
    c2d.fillStyle = '#ffcc00';
    c2d.fillRect(px+2,py+2,2,2); c2d.fillRect(px+12,py+3,2,2);
    c2d.fillRect(px+6,py+7,4,1);
    c2d.fillStyle = '#ff4444'; c2d.fillRect(px+4,py+1,3,3);
    c2d.fillStyle = '#4444ff'; c2d.fillRect(px+10,py+1,3,3);
  } else if (id === 18) { // shadow tile
    c2d.fillStyle = '#330033';
    c2d.fillRect(px+1,py+1,14,14);
    c2d.fillStyle = '#550055';
    c2d.fillRect(px+4,py+4,8,8);
  }
}

// ── UI helpers ────────────────────────────────
function drawBox(c2d, x, y, w, h, fill='#1a1a2e', border='#5588cc') {
  c2d.fillStyle = '#000a';
  c2d.fillRect(x+2, y+2, w, h);
  c2d.fillStyle = fill;
  c2d.fillRect(x, y, w, h);
  c2d.strokeStyle = border;
  c2d.lineWidth = 1;
  c2d.strokeRect(x+.5, y+.5, w-1, h-1);
  c2d.strokeStyle = '#aaccff44';
  c2d.strokeRect(x+1.5, y+1.5, w-3, h-3);
}

function drawHpBar(c2d, x, y, w, hp, maxHp) {
  const ratio = clamp(hp / maxHp, 0, 1);
  const color = ratio > 0.5 ? '#33cc33' : ratio > 0.25 ? '#cccc33' : '#cc3333';
  c2d.fillStyle = '#111';
  c2d.fillRect(x, y, w, 4);
  c2d.fillStyle = color;
  c2d.fillRect(x, y, Math.round(w * ratio), 4);
  c2d.strokeStyle = '#333';
  c2d.lineWidth = 1;
  c2d.strokeRect(x+.5, y+.5, w-1, 3);
}

function drawMpBar(c2d, x, y, w, mp, maxMp) {
  const ratio = clamp(mp / maxMp, 0, 1);
  c2d.fillStyle = '#111';
  c2d.fillRect(x, y, w, 3);
  c2d.fillStyle = '#3388ff';
  c2d.fillRect(x, y, Math.round(w * ratio), 3);
}

function drawAtbBar(c2d, x, y, w, atb) {
  const full = atb >= 100;
  c2d.fillStyle = '#111';
  c2d.fillRect(x, y, w, 3);
  c2d.fillStyle = full ? '#ffffaa' : '#886622';
  c2d.fillRect(x, y, Math.round(w * clamp(atb/100,0,1)), 3);
  if (full) {
    c2d.fillStyle = '#fff8';
    c2d.fillRect(x, y, w, 1);
  }
}

// ── Character sprites (overworld, 14×20) ──────
function drawCharSprite(c2d, id, x, y, facing, frame, alive=true) {
  const sprites = { kael: _kaelSprite, lyra: _lyraSprite, gorak: _gorakSprite };
  const fn = sprites[id] || _kaelSprite;
  c2d.save();
  if (!alive) c2d.globalAlpha = 0.4;
  fn(c2d, x, y, facing, frame);
  c2d.restore();
}

function _kaelSprite(c2d, x, y, facing, frame) {
  const f = frame % 2;
  // Body
  c2d.fillStyle = '#2244aa';
  c2d.fillRect(x+3, y+8, 8, 9);
  // Belt
  c2d.fillStyle = '#884422';
  c2d.fillRect(x+3, y+12, 8, 2);
  // Head
  c2d.fillStyle = '#f0c080';
  c2d.fillRect(x+3, y+2, 8, 7);
  // Hair
  c2d.fillStyle = '#553311';
  c2d.fillRect(x+3, y+1, 8, 3);
  c2d.fillRect(x+2, y+2, 2, 3);
  // Eyes
  c2d.fillStyle = '#111';
  if (facing===2 || facing===0) {
    c2d.fillRect(x+5, y+5, 2, 2); c2d.fillRect(x+9, y+5, 2, 2);
  } else {
    c2d.fillRect(facing===1?x+9:x+4, y+5, 2, 2);
  }
  // Sword
  c2d.fillStyle = '#aaaacc';
  if (facing===1) { c2d.fillRect(x+11,y+7,2,9); c2d.fillStyle='#664422'; c2d.fillRect(x+10,y+11,4,2); }
  else if (facing===3) { c2d.fillRect(x+1,y+7,2,9); c2d.fillStyle='#664422'; c2d.fillRect(x+0,y+11,4,2); }
  // Legs
  c2d.fillStyle = '#112266';
  if (f===0) {
    c2d.fillRect(x+3,y+17,4,5); c2d.fillRect(x+7,y+17,4,5);
  } else {
    c2d.fillRect(x+3,y+16,4,6); c2d.fillRect(x+7,y+18,4,4);
  }
  // Boots
  c2d.fillStyle = '#552211';
  if (f===0) {
    c2d.fillRect(x+3,y+21,4,2); c2d.fillRect(x+7,y+21,4,2);
  } else {
    c2d.fillRect(x+3,y+21,4,2); c2d.fillRect(x+7,y+21,4,2);
  }
}

function _lyraSprite(c2d, x, y, facing, frame) {
  const f = frame % 2;
  // Dress
  c2d.fillStyle = '#aaaaff';
  c2d.fillRect(x+3,y+8,8,12);
  c2d.fillStyle = '#8888dd';
  c2d.fillRect(x+3,y+14,8,6);
  // Collar
  c2d.fillStyle = '#ffffff';
  c2d.fillRect(x+4,y+8,6,2);
  // Head
  c2d.fillStyle = '#f0c090';
  c2d.fillRect(x+3,y+2,8,7);
  // Hair (pink)
  c2d.fillStyle = '#ff88bb';
  c2d.fillRect(x+2,y+1,10,4);
  c2d.fillRect(x+1,y+4,3,5); c2d.fillRect(x+10,y+4,3,5);
  // Eyes
  c2d.fillStyle = '#111';
  if (facing===2 || facing===0) {
    c2d.fillRect(x+5,y+5,2,2); c2d.fillRect(x+9,y+5,2,2);
  } else {
    c2d.fillRect(facing===1?x+9:x+4,y+5,2,2);
  }
  // Staff
  c2d.fillStyle = '#886633';
  if (facing===1) { c2d.fillRect(x+12,y+4,2,16); c2d.fillStyle='#ffcc00'; c2d.fillRect(x+11,y+3,4,4); }
  else if (facing===3) { c2d.fillRect(x+0,y+4,2,16); c2d.fillStyle='#ffcc00'; c2d.fillRect(x+0,y+3,4,4); }
  else { c2d.fillRect(x+12,y+4,2,16); c2d.fillStyle='#ffcc00'; c2d.fillRect(x+11,y+3,4,4); }
  // Feet (peeking from dress)
  c2d.fillStyle = '#f0c090';
  if (f===0) {
    c2d.fillRect(x+5,y+19,3,2); c2d.fillRect(x+8,y+19,3,2);
  } else {
    c2d.fillRect(x+4,y+18,3,3); c2d.fillRect(x+9,y+20,3,1);
  }
}

function _gorakSprite(c2d, x, y, facing, frame) {
  const f = frame % 2;
  // Body (metal)
  c2d.fillStyle = '#778899';
  c2d.fillRect(x+2,y+7,12,12);
  c2d.fillStyle = '#556677';
  c2d.fillRect(x+3,y+8,10,10);
  // Core (chest light)
  c2d.fillStyle = '#ff4400';
  c2d.fillRect(x+6,y+10,4,4);
  // Head
  c2d.fillStyle = '#889aaa';
  c2d.fillRect(x+3,y+1,10,8);
  c2d.fillStyle = '#667788';
  c2d.fillRect(x+3,y+1,10,2);
  // Eyes (glowing)
  c2d.fillStyle = '#ff6600';
  if (facing===2 || facing===0) {
    c2d.fillRect(x+5,y+4,3,3); c2d.fillRect(x+9,y+4,3,3);
  } else {
    c2d.fillRect(facing===1?x+10:x+4,y+4,3,3);
  }
  // Arms
  c2d.fillStyle = '#667788';
  if (facing!==1) { c2d.fillRect(x+0,y+8,3,8); }
  if (facing!==3) { c2d.fillRect(x+13,y+8,3,8); }
  // Legs
  c2d.fillStyle = '#556677';
  if (f===0) {
    c2d.fillRect(x+3,y+19,5,4); c2d.fillRect(x+8,y+19,5,4);
  } else {
    c2d.fillRect(x+3,y+17,5,6); c2d.fillRect(x+8,y+20,5,3);
  }
  c2d.fillStyle = '#334455';
  c2d.fillRect(x+3,y+22,5,1); c2d.fillRect(x+8,y+22,5,1);
}

// ── NPC sprites ───────────────────────────────
function drawNPC(c2d, type, x, y, frame) {
  const npcFn = {
    elder: _elderNPC, merchant: _merchantNPC, guard: _guardNPC,
    villager: _villagerNPC, woman: _womanNPC, chest: _chestObj,
    warpgate: _warpGate, shadowgate: _shadowGate,
    lyra_npc: (c,x,y,f)=>_lyraSprite(c,x,y,2,f),
    gorak_npc: (c,x,y,f)=>_gorakSprite(c,x,y,2,f),
  };
  const fn = npcFn[type];
  if (fn) fn(c2d, x, y, frame);
}

function _elderNPC(c2d, x, y, frame) {
  // Robe (purple)
  c2d.fillStyle='#663388'; c2d.fillRect(x+2,y+8,12,13);
  c2d.fillStyle='#441166'; c2d.fillRect(x+4,y+10,8,11);
  // Head
  c2d.fillStyle='#e0b080'; c2d.fillRect(x+3,y+2,10,7);
  // Beard
  c2d.fillStyle='#eeeecc'; c2d.fillRect(x+3,y+6,10,6);
  // Hair / hat
  c2d.fillStyle='#888877'; c2d.fillRect(x+3,y+1,10,3);
  c2d.fillStyle='#333322'; c2d.fillRect(x+2,y+0,12,2);
  // Eyes
  c2d.fillStyle='#111'; c2d.fillRect(x+5,y+4,2,2); c2d.fillRect(x+9,y+4,2,2);
  // Staff
  c2d.fillStyle='#886633'; c2d.fillRect(x+13,y+3,2,18);
  c2d.fillStyle='#ffdd00'; c2d.fillRect(x+12,y+2,4,3);
}

function _merchantNPC(c2d, x, y, frame) {
  c2d.fillStyle='#cc8822'; c2d.fillRect(x+3,y+8,10,12);
  c2d.fillStyle='#aa6611'; c2d.fillRect(x+7,y+12,2,8);
  c2d.fillStyle='#f0b070'; c2d.fillRect(x+4,y+2,8,7);
  c2d.fillStyle='#884422'; c2d.fillRect(x+3,y+1,10,3); c2d.fillRect(x+2,y+3,2,3);
  c2d.fillStyle='#111'; c2d.fillRect(x+5,y+5,2,2); c2d.fillRect(x+9,y+5,2,2);
  c2d.fillStyle='#cc9933'; c2d.fillRect(x+1,y+6,2,1); c2d.fillRect(x+13,y+6,2,1);
}

function _guardNPC(c2d, x, y, frame) {
  c2d.fillStyle='#445566'; c2d.fillRect(x+2,y+6,12,15);
  c2d.fillStyle='#667788'; c2d.fillRect(x+4,y+8,8,11);
  c2d.fillStyle='#889aaa'; c2d.fillRect(x+3,y+1,10,7);
  c2d.fillStyle='#556677'; c2d.fillRect(x+3,y+0,10,3);
  c2d.fillStyle='#ff2222'; c2d.fillRect(x+4,y+0,8,2);
  c2d.fillStyle='#111'; c2d.fillRect(x+5,y+4,2,2); c2d.fillRect(x+9,y+4,2,2);
  c2d.fillStyle='#aaaacc'; c2d.fillRect(x+1,y+7,2,12); c2d.fillRect(x+0,y+7,3,2);
}

function _villagerNPC(c2d, x, y, frame) {
  c2d.fillStyle='#55aa55'; c2d.fillRect(x+3,y+8,10,12);
  c2d.fillStyle='#f0b070'; c2d.fillRect(x+4,y+2,8,7);
  c2d.fillStyle='#885522'; c2d.fillRect(x+3,y+1,10,3);
  c2d.fillStyle='#111'; c2d.fillRect(x+5,y+5,2,2); c2d.fillRect(x+9,y+5,2,2);
  c2d.fillStyle='#3a7a3a'; c2d.fillRect(x+3,y+16,4,6); c2d.fillRect(x+9,y+16,4,6);
}

function _womanNPC(c2d, x, y, frame) {
  c2d.fillStyle='#dd4466'; c2d.fillRect(x+3,y+8,10,13);
  c2d.fillStyle='#f0b080'; c2d.fillRect(x+4,y+2,8,7);
  c2d.fillStyle='#cc8844'; c2d.fillRect(x+2,y+1,12,4); c2d.fillRect(x+1,y+4,3,5);
  c2d.fillStyle='#111'; c2d.fillRect(x+5,y+5,2,2); c2d.fillRect(x+9,y+5,2,2);
}

function _chestObj(c2d, x, y, opened) {
  c2d.fillStyle = opened ? '#664411' : '#aa6622';
  c2d.fillRect(x+1,y+6,14,10);
  c2d.fillStyle = opened ? '#442200' : '#884400';
  c2d.fillRect(x+1,y+6,14,3);
  c2d.fillStyle = '#ccaa44';
  c2d.fillRect(x+1,y+8,14,1);
  c2d.fillStyle = '#ffcc00';
  c2d.fillRect(x+6,y+7,4,3);
  if (!opened) {
    c2d.fillStyle='#ffdd66'; c2d.fillRect(x+7,y+9,2,1);
  }
}

function _warpGate(c2d, x, y, frame) {
  const t = frame * 0.08;
  const pulse = 0.5 + 0.5 * Math.sin(t * 3);
  // Gate ring
  c2d.fillStyle = `rgba(100,50,200,${0.6+pulse*0.4})`;
  c2d.fillRect(x+2,y+0,12,2); c2d.fillRect(x+2,y+14,12,2);
  c2d.fillRect(x+0,y+2,2,12); c2d.fillRect(x+14,y+2,2,12);
  // Inner glow
  c2d.fillStyle = `rgba(180,100,255,${0.3+pulse*0.4})`;
  c2d.fillRect(x+2,y+2,12,12);
  // Shimmer
  for(let i=0;i<4;i++) {
    const px = x+3+Math.floor((i*3+frame*2)%10);
    const py = y+3+Math.floor((i*5+frame)%10);
    c2d.fillStyle = '#fff8';
    c2d.fillRect(px,py,2,2);
  }
}

function _shadowGate(c2d, x, y, frame) {
  const t = frame * 0.08;
  const pulse = 0.5 + 0.5*Math.sin(t*2);
  c2d.fillStyle = `rgba(50,0,80,${0.7+pulse*0.3})`;
  c2d.fillRect(x,y,16,16);
  c2d.fillStyle = `rgba(150,0,200,${0.5+pulse*0.4})`;
  c2d.fillRect(x+3,y+3,10,10);
  c2d.fillStyle = '#ff00ff88';
  c2d.fillRect(x+6,y+6,4,4);
  // Tendrils
  for(let i=0;i<3;i++) {
    const a = (frame/20 + i*2.1) % (Math.PI*2);
    const tx = x+8+Math.cos(a)*6;
    const ty = y+8+Math.sin(a)*6;
    c2d.fillStyle='#aa00cc88';
    c2d.fillRect(Math.round(tx),Math.round(ty),2,2);
  }
}

// ── Battle sprites (larger, ~32×32 logical) ───
function drawBattleSprite(c2d, id, x, y, frame, shaking=false) {
  const dx = shaking ? (Math.random()>0.5?2:-2) : 0;
  c2d.save();
  c2d.translate(dx, 0);
  const fn = BATTLE_SPRITES[id];
  if (fn) fn(c2d, x, y, frame);
  c2d.restore();
}

const BATTLE_SPRITES = {
  kael(c2d, x, y, frame) {
    const bob = Math.sin(frame*0.1)*1;
    const by = y + bob;
    c2d.fillStyle='#2244aa'; c2d.fillRect(x+6,y+16,20,20);
    c2d.fillStyle='#884422'; c2d.fillRect(x+6,y+24,20,4);
    c2d.fillStyle='#f0c080'; c2d.fillRect(x+7,y+4,18,14);
    c2d.fillStyle='#553311'; c2d.fillRect(x+7,y+2,18,6); c2d.fillRect(x+5,y+4,4,6);
    c2d.fillStyle='#111'; c2d.fillRect(x+10,y+10,4,3); c2d.fillRect(x+18,y+10,4,3);
    c2d.fillStyle='#f06050'; c2d.fillRect(x+15,y+16,2,2);
    c2d.fillStyle='#aaaacc'; c2d.fillRect(x+28,y+10,3,22);
    c2d.fillStyle='#664422'; c2d.fillRect(x+26,y+19,7,3);
    c2d.fillStyle='#112266'; c2d.fillRect(x+6,y+36,9,8); c2d.fillRect(x+17,y+36,9,8);
    c2d.fillStyle='#552211'; c2d.fillRect(x+6,y+42,9,4); c2d.fillRect(x+17,y+42,9,4);
  },
  lyra(c2d, x, y, frame) {
    const bob = Math.sin(frame*0.1)*1;
    c2d.fillStyle='#aaaaff'; c2d.fillRect(x+6,y+16,20,28);
    c2d.fillStyle='#8888dd'; c2d.fillRect(x+4,y+28,24,16);
    c2d.fillStyle='#ffffff'; c2d.fillRect(x+8,y+16,12,4);
    c2d.fillStyle='#f0c090'; c2d.fillRect(x+7,y+4,18,14);
    c2d.fillStyle='#ff88bb'; c2d.fillRect(x+5,y+2,22,8);
    c2d.fillRect(x+2,y+8,6,10); c2d.fillRect(x+24,y+8,6,10);
    c2d.fillStyle='#111'; c2d.fillRect(x+10,y+10,4,3); c2d.fillRect(x+18,y+10,4,3);
    c2d.fillStyle='#886633'; c2d.fillRect(x+28,y+0,3,40);
    c2d.fillStyle='#ffcc00'; c2d.fillRect(x+25,y+0,9,6);
  },
  gorak(c2d, x, y, frame) {
    const bob = Math.sin(frame*0.1)*0.5;
    c2d.fillStyle='#778899'; c2d.fillRect(x+4,y+12,24,24);
    c2d.fillStyle='#556677'; c2d.fillRect(x+6,y+14,20,20);
    c2d.fillStyle='#ff4400'; c2d.fillRect(x+11,y+18,10,8);
    c2d.fillStyle='#ff8800'; c2d.fillRect(x+13,y+20,6,4);
    c2d.fillStyle='#889aaa'; c2d.fillRect(x+5,y+2,22,14);
    c2d.fillStyle='#556677'; c2d.fillRect(x+5,y+2,22,4);
    c2d.fillStyle='#ff6600'; c2d.fillRect(x+9,y+6,6,6); c2d.fillRect(x+17,y+6,6,6);
    c2d.fillStyle='#ffaa44'; c2d.fillRect(x+10,y+7,4,4); c2d.fillRect(x+18,y+7,4,4);
    c2d.fillStyle='#667788';
    c2d.fillRect(x+0,y+14,5,16); c2d.fillRect(x+27,y+14,5,16);
    c2d.fillStyle='#334455'; c2d.fillRect(x+0,y+29,5,3); c2d.fillRect(x+27,y+29,5,3);
    c2d.fillStyle='#556677'; c2d.fillRect(x+4,y+36,10,8); c2d.fillRect(x+18,y+36,10,8);
  },

  // Enemies
  slime(c2d, x, y, frame) {
    const bob = Math.abs(Math.sin(frame*0.07))*3;
    c2d.fillStyle='#44cc44'; c2d.fillRect(x+4,y+8+bob,24,16-bob);
    c2d.fillStyle='#66ee66'; c2d.fillRect(x+6,y+6+bob,20,12-bob);
    c2d.fillStyle='#22aa22'; c2d.fillRect(x+4,y+22,24,4);
    c2d.fillStyle='#111'; c2d.fillRect(x+9,y+11+bob,4,4); c2d.fillRect(x+19,y+11+bob,4,4);
    c2d.fillStyle='#88ff88'; c2d.fillRect(x+11,y+13+bob,2,2); c2d.fillRect(x+21,y+13+bob,2,2);
  },
  bat(c2d, x, y, frame) {
    const flap = Math.sin(frame*0.15)*4;
    c2d.fillStyle='#442266';
    c2d.fillRect(x+0,y+8-flap,12,8);  // left wing
    c2d.fillRect(x+20,y+8-flap,12,8); // right wing
    c2d.fillStyle='#663388'; c2d.fillRect(x+4,y+6-flap,8,4); c2d.fillRect(x+20,y+6-flap,8,4);
    c2d.fillStyle='#553377'; c2d.fillRect(x+10,y+6,12,14);
    c2d.fillStyle='#442266'; c2d.fillRect(x+11,y+4,10,4);
    c2d.fillStyle='#ff2222'; c2d.fillRect(x+13,y+8,4,4); c2d.fillRect(x+15,y+8,4,4);
    c2d.fillStyle='#ffaa00'; c2d.fillRect(x+14,y+9,2,2); c2d.fillRect(x+16,y+9,2,2);
  },
  goblin(c2d, x, y, frame) {
    const bob = Math.sin(frame*0.08)*1;
    c2d.fillStyle='#556622'; c2d.fillRect(x+6,y+12+bob,20,20);
    c2d.fillStyle='#888811'; c2d.fillRect(x+6,y+22+bob,20,4);
    c2d.fillStyle='#667733'; c2d.fillRect(x+8,y+2+bob,16,12);
    c2d.fillStyle='#334411'; c2d.fillRect(x+8,y+1+bob,16,4);
    c2d.fillRect(x+5,y+3+bob,4,5); c2d.fillRect(x+23,y+3+bob,4,5);
    c2d.fillStyle='#ff6600'; c2d.fillRect(x+10,y+7+bob,4,4); c2d.fillRect(x+18,y+7+bob,4,4);
    c2d.fillStyle='#aaaacc'; c2d.fillRect(x+2,y+14+bob,4,10); c2d.fillRect(x+26,y+14+bob,4,10);
    c2d.fillStyle='#334411'; c2d.fillRect(x+6,y+32+bob,8,8); c2d.fillRect(x+18,y+32+bob,8,8);
  },
  skeleton(c2d, x, y, frame) {
    const bob = Math.sin(frame*0.06)*1;
    c2d.fillStyle='#ddddcc';
    // Skull
    c2d.fillRect(x+7,y+1+bob,18,16);
    c2d.fillStyle='#bbbbaa'; c2d.fillRect(x+9,y+3+bob,14,12);
    c2d.fillStyle='#111'; c2d.fillRect(x+9,y+6+bob,5,6); c2d.fillRect(x+18,y+6+bob,5,6);
    c2d.fillStyle='#bbbbaa'; c2d.fillRect(x+13,y+13+bob,6,4);
    c2d.fillStyle='#ddddcc'; c2d.fillRect(x+7,y+17+bob,18,3); // neck/spine
    // Ribcage
    c2d.fillRect(x+6,y+20+bob,20,14);
    c2d.fillStyle='#111'; // gaps between ribs
    for(let i=0;i<3;i++) c2d.fillRect(x+9,y+22+i*4+bob,14,2);
    c2d.fillStyle='#ddddcc';
    c2d.fillRect(x+4,y+21+bob,3,12); c2d.fillRect(x+25,y+21+bob,3,12);
    c2d.fillRect(x+6,y+34+bob,8,10); c2d.fillRect(x+18,y+34+bob,8,10);
  },
  mushroom(c2d, x, y, frame) {
    const bob = Math.sin(frame*0.05)*1;
    c2d.fillStyle='#cc2222';
    c2d.fillRect(x+4,y+2+bob,24,14);
    c2d.fillStyle='#ee4444'; c2d.fillRect(x+6,y+2+bob,20,10);
    c2d.fillStyle='#ffffff'; // spots
    c2d.fillRect(x+6,y+4+bob,4,4); c2d.fillRect(x+16,y+3+bob,5,5); c2d.fillRect(x+10,y+8+bob,3,3);
    c2d.fillStyle='#ffeecc'; // stem
    c2d.fillRect(x+8,y+14+bob,16,20);
    c2d.fillStyle='#ddeebb'; c2d.fillRect(x+10,y+16+bob,12,16);
    c2d.fillStyle='#111'; c2d.fillRect(x+10,y+18+bob,4,4); c2d.fillRect(x+18,y+18+bob,4,4);
    c2d.fillStyle='#ff2222'; c2d.fillRect(x+12,y+24+bob,8,3);
  },
  wyvern(c2d, x, y, frame) {
    const flap = Math.sin(frame*0.1)*3;
    c2d.fillStyle='#226644';
    c2d.fillRect(x+8,y+10,16,22); // body
    c2d.fillStyle='#338855'; c2d.fillRect(x+10,y+12,12,18);
    // Wings
    c2d.fillStyle='#115533';
    c2d.fillRect(x+0,y+8-flap,10,14);
    c2d.fillRect(x+22,y+8-flap,10,14);
    c2d.fillStyle='#226644';
    c2d.fillRect(x+2,y+10-flap,6,8); c2d.fillRect(x+24,y+10-flap,6,8);
    // Head / neck
    c2d.fillRect(x+10,y+2,12,12);
    c2d.fillStyle='#338855'; c2d.fillRect(x+12,y+3,8,10);
    c2d.fillStyle='#ffcc00'; c2d.fillRect(x+13,y+5,4,4); c2d.fillRect(x+19,y+5,4,4);
    c2d.fillStyle='#111'; c2d.fillRect(x+14,y+6,2,2); c2d.fillRect(x+20,y+6,2,2);
    // Tail
    c2d.fillStyle='#226644';
    c2d.fillRect(x+20,y+28,8,4); c2d.fillRect(x+26,y+30,6,3); c2d.fillRect(x+30,y+31,4,2);
    // Legs
    c2d.fillRect(x+8,y+32,6,8); c2d.fillRect(x+18,y+32,6,8);
  },

  // Bosses
  guardian(c2d, x, y, frame) {
    const bob = Math.sin(frame*0.05)*1;
    c2d.fillStyle='#8899aa'; c2d.fillRect(x+4,y+8+bob,40,40);
    c2d.fillStyle='#aabbcc'; c2d.fillRect(x+6,y+10+bob,36,36);
    // Helmet
    c2d.fillStyle='#778899'; c2d.fillRect(x+10,y+0+bob,28,14);
    c2d.fillStyle='#889aab'; c2d.fillRect(x+12,y+2+bob,24,10);
    c2d.fillStyle='#cc2222'; c2d.fillRect(x+14,y+0+bob,20,3); // plume
    c2d.fillStyle='#111'; c2d.fillRect(x+14,y+4+bob,10,6); c2d.fillRect(x+26,y+4+bob,10,6);
    // Shield
    c2d.fillStyle='#556677'; c2d.fillRect(x+0,y+10+bob,8,24);
    c2d.fillStyle='#778899'; c2d.fillRect(x+1,y+12+bob,6,20);
    c2d.fillStyle='#ffcc00'; c2d.fillRect(x+2,y+16+bob,4,12); c2d.fillRect(x+1,y+20+bob,6,4);
    // Sword
    c2d.fillStyle='#ccccee'; c2d.fillRect(x+44,y+4+bob,4,34);
    c2d.fillStyle='#886633'; c2d.fillRect(x+40,y+18+bob,12,4);
    c2d.fillStyle='#ffcc00'; c2d.fillRect(x+42,y+16+bob,8,2); c2d.fillRect(x+42,y+24+bob,8,2);
    // Legs
    c2d.fillStyle='#667788'; c2d.fillRect(x+4,y+48+bob,16,12); c2d.fillRect(x+28,y+48+bob,16,12);
    c2d.fillStyle='#445566'; c2d.fillRect(x+4,y+58+bob,16,4); c2d.fillRect(x+28,y+58+bob,16,4);
  },
  shadowcore(c2d, x, y, frame) {
    const pulse = 0.5+0.5*Math.sin(frame*0.08);
    const t = frame * 0.05;
    // Shadow body
    c2d.fillStyle=`rgba(30,0,50,${0.7+pulse*0.3})`;
    c2d.fillRect(x+4,y+8,40,44);
    c2d.fillStyle=`rgba(80,0,120,${0.5+pulse*0.4})`;
    c2d.fillRect(x+8,y+12,32,36);
    // Core crystal
    c2d.fillStyle=`rgba(200,0,255,${0.8+pulse*0.2})`;
    c2d.fillRect(x+18,y+20,12,16);
    c2d.fillStyle='#ff00ff';
    c2d.fillRect(x+20,y+22,8,12);
    c2d.fillStyle='#fff';
    c2d.fillRect(x+22,y+24,4,8);
    // Eyes (3)
    c2d.fillStyle='#ff0000';
    c2d.fillRect(x+10,y+14,6,6); c2d.fillRect(x+32,y+14,6,6); c2d.fillRect(x+21,y+8,6,6);
    c2d.fillStyle='#ffaa00';
    c2d.fillRect(x+11,y+15,4,4); c2d.fillRect(x+33,y+15,4,4); c2d.fillRect(x+22,y+9,4,4);
    // Shadow tendrils (animated)
    for(let i=0;i<5;i++) {
      const a = t + i * 1.256;
      const tx = x+24+Math.cos(a)*18;
      const ty = y+30+Math.sin(a)*14;
      c2d.fillStyle=`rgba(150,0,200,${0.4+pulse*0.3})`;
      c2d.fillRect(Math.round(tx),Math.round(ty),4,4);
    }
    // Crown spikes
    c2d.fillStyle='#660088';
    for(let i=0;i<5;i++) {
      c2d.fillRect(x+8+i*8,y+2,4,8-i%2*3);
    }
  }
};

// ── Backgrounds for battle ────────────────────
function drawBattleBg(c2d, type) {
  const bgs = {
    plains(c2d) {
      c2d.fillStyle='#6699cc'; c2d.fillRect(0,0,W,100);
      c2d.fillStyle='#88aadd'; c2d.fillRect(0,0,W,60);
      // Clouds
      c2d.fillStyle='#fff9';
      c2d.fillRect(30,15,40,12); c2d.fillRect(26,19,48,8);
      c2d.fillRect(160,10,55,14); c2d.fillRect(155,14,65,10);
      // Ground
      c2d.fillStyle='#44aa44'; c2d.fillRect(0,100,W,H-100);
      c2d.fillStyle='#55bb55'; c2d.fillRect(0,100,W,10);
      for(let i=0;i<20;i++) {
        c2d.fillStyle='#336633';
        c2d.fillRect(i*16+4,104,3,6);
      }
    },
    forest(c2d) {
      c2d.fillStyle='#224422'; c2d.fillRect(0,0,W,H);
      c2d.fillStyle='#1a3a1a'; c2d.fillRect(0,60,W,H-60);
      for(let i=0;i<8;i++) {
        c2d.fillStyle=`#${['1a4e1a','0e3a0e','226622','0a280a'][i%4]}`;
        c2d.fillRect(i*40,20,32,80);
        c2d.fillRect(i*40+4,0,24,50);
      }
      c2d.fillStyle='#334433'; c2d.fillRect(0,100,W,H-100);
      c2d.fillStyle='#3a5a3a'; c2d.fillRect(0,100,W,8);
    },
    cave(c2d) {
      c2d.fillStyle='#111111'; c2d.fillRect(0,0,W,H);
      c2d.fillStyle='#1a1a2a'; c2d.fillRect(0,60,W,H-60);
      for(let i=0;i<12;i++) {
        c2d.fillStyle='#222230';
        c2d.fillRect(i*28,0,20,30+(i%3)*15);
      }
      c2d.fillStyle='#0a0a1a'; c2d.fillRect(0,90,W,H-90);
      c2d.fillStyle='#111120'; c2d.fillRect(0,90,W,8);
      // Crystals
      for(let i=0;i<5;i++) {
        c2d.fillStyle='#2233aa44';
        c2d.fillRect(30+i*55,70,6,20);
      }
    },
    shadow(c2d) {
      // Swirling dark background for boss
      c2d.fillStyle='#050008'; c2d.fillRect(0,0,W,H);
      c2d.fillStyle='#0a0015'; c2d.fillRect(0,50,W,H-50);
      for(let i=0;i<6;i++) {
        c2d.fillStyle=`rgba(80,0,120,0.${2+i%3})`;
        c2d.fillRect(i*55,0,40,H);
      }
      for(let i=0;i<4;i++) {
        c2d.fillStyle='#220033';
        c2d.fillRect(0,i*50,W,30);
      }
      c2d.fillStyle='#110022'; c2d.fillRect(0,100,W,H-100);
    }
  };
  (bgs[type] || bgs.plains)(c2d);
}

// ── Particle effects ──────────────────────────
class Particle {
  constructor(x,y,vx,vy,color,life,size=2) {
    this.x=x; this.y=y; this.vx=vx; this.vy=vy;
    this.color=color; this.life=life; this.maxLife=life; this.size=size;
  }
  update(dt) { this.x+=this.vx*dt; this.y+=this.vy*dt; this.life-=dt; }
  draw(c2d) {
    const alpha = this.life/this.maxLife;
    c2d.fillStyle = this.color;
    c2d.globalAlpha = alpha;
    c2d.fillRect(Math.round(this.x),Math.round(this.y),this.size,this.size);
    c2d.globalAlpha = 1;
  }
  get dead() { return this.life <= 0; }
}

function spawnHitParticles(particles, x, y, color) {
  for(let i=0;i<12;i++) {
    const a = Math.random()*Math.PI*2;
    const spd = 30+Math.random()*60;
    particles.push(new Particle(x,y,Math.cos(a)*spd,Math.sin(a)*spd,color,0.4+Math.random()*0.4,2));
  }
}

function spawnHealParticles(particles, x, y) {
  for(let i=0;i<10;i++) {
    particles.push(new Particle(x+Math.random()*20-10,y+Math.random()*20,
      (Math.random()-0.5)*20,-30-Math.random()*40,'#44ff88',0.5+Math.random()*0.5,3));
  }
}
