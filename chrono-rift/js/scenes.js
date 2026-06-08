'use strict';
// ─────────────────────────────────────────────
//  scenes.js  –  Title, Overworld, Dialog, Menu,
//                Victory, GameOver, Ending
// ─────────────────────────────────────────────

// ── TITLE SCENE ──────────────────────────────
class TitleScene {
  constructor() {
    this.frame   = 0;
    this.sel     = 0;
    this.stars   = Array.from({length:80}, ()=>({
      x: Math.random()*W, y: Math.random()*H,
      s: Math.random()*1.5+0.5, sp: 0.2+Math.random()*0.4,
    }));
    this.hasSave = !!localStorage.getItem('chronorift_save');
    this.blinkT  = 0;
    this.blink   = true;
  }
  enter() { Audio.playMusic('title'); }
  exit()  { Audio.stopMusic(); }

  update(dt) {
    this.frame++;
    this.blinkT += dt;
    if (this.blinkT > 0.5) { this.blinkT=0; this.blink=!this.blink; }
    this.stars.forEach(s => { s.x -= s.sp; if (s.x<0) { s.x=W; s.y=Math.random()*H; } });

    const opts = this.hasSave ? ['NEW GAME','CONTINUE'] : ['NEW GAME'];
    if (Input.pressed('ArrowUp'))   { this.sel=Math.max(0,this.sel-1); Audio.sfx('cursor'); }
    if (Input.pressed('ArrowDown')) { this.sel=Math.min(opts.length-1,this.sel+1); Audio.sfx('cursor'); }
    if (Input.confirm()) {
      Audio.sfx('confirm');
      if (opts[this.sel]==='CONTINUE') {
        this._loadGame();
      } else {
        this._newGame();
      }
    }
  }

  _newGame() {
    State.party     = [makeCharacter('kael', 1)];
    State.inventory = [];
    addItem('potion', 3);
    addItem('ether',  1);
    State.flags     = {};
    State.mapId     = 'village';
    State.playerX   = 12;
    State.playerY   = 18;
    State.playerFacing = 2;
    State.gold      = 50;
    SceneMgr.set(OpeningScene);
  }

  _loadGame() {
    try {
      const data = JSON.parse(localStorage.getItem('chronorift_save'));
      Object.assign(State, data);
      SceneMgr.set(OverworldScene);
    } catch(e) { this._newGame(); }
  }

  draw(c2d) {
    c2d.fillStyle = '#000010'; c2d.fillRect(0,0,W,H);
    // Stars
    c2d.fillStyle = '#ffffff';
    this.stars.forEach(s => c2d.fillRect(Math.round(s.x),Math.round(s.y),Math.round(s.s),Math.round(s.s)));

    // Nebula effect
    const t = this.frame*0.01;
    for(let i=0;i<3;i++) {
      const px = 80+Math.cos(t+i*2.1)*60, py = 100+Math.sin(t+i*1.7)*30;
      c2d.fillStyle=`rgba(${40+i*20},${10},${80+i*20},0.05)`;
      c2d.fillRect(Math.round(px-30),Math.round(py-20),60,40);
    }

    // Game logo
    const pulse = 0.7+0.3*Math.sin(this.frame*0.04);
    c2d.fillStyle=`rgba(80,160,255,${pulse*0.15})`;
    c2d.fillRect(30,28,260,30);
    drawTextShadow(c2d,'CHRONO', 50, 32, '#88ccff', '#002244', 3);
    drawTextShadow(c2d,'RIFT',   104, 48, '#ff88cc', '#440022', 3);

    // Subtitle
    drawText(c2d, 'A TIME-BENDING ADVENTURE', 40, 78, '#556688', 1);

    // Menu options
    const opts = this.hasSave ? ['NEW GAME','CONTINUE'] : ['NEW GAME'];
    opts.forEach((o,i) => {
      const sel = i===this.sel;
      if (sel) {
        c2d.fillStyle='#ffffff18'; c2d.fillRect(90,100+i*18,140,14);
        drawText(c2d,'>', 82, 102+i*18, '#ffff00', 1);
      }
      drawText(c2d, o, 96, 102+i*18, sel?'#ffff00':'#aaaacc', 1);
    });

    // Blink prompt
    if (this.blink) drawText(c2d, 'PRESS Z TO SELECT', 72, 155, '#446688', 1);

    // Controls hint
    drawText(c2d, 'ARROWS:MOVE  Z:CONFIRM  X:CANCEL  ENTER:MENU', 6, 210, '#334455', 1);
    drawText(c2d, 'M:MUTE MUSIC', 6, 220, '#334455', 1);

    // Decorative characters at bottom
    drawCharSprite(c2d,'kael',  30, 175, 2, Math.floor(this.frame/10)%2);
    drawCharSprite(c2d,'lyra',  70, 175, 2, Math.floor(this.frame/10)%2);
    drawCharSprite(c2d,'gorak',110, 175, 2, Math.floor(this.frame/10)%2);
  }
}

// ── OPENING SCENE ─────────────────────────────
class OpeningScene {
  constructor() {
    this.lines = [
      {text:"Year 1000 AD.", color:'#aaaacc', delay:1.5},
      {text:"A thousand years of peace in the Kingdom of Guardia.", color:'#aaaacc', delay:2},
      {text:"Tonight, the Millennium Festival lights the sky.", color:'#aaaacc', delay:2},
      {text:"But somewhere, deep in the earth, something ancient stirs...", color:'#8888aa', delay:2.5},
    ];
    this.lineIdx  = 0;
    this.charIdx  = 0;
    this.lineText = '';
    this.timer    = 0;
    this.done     = false;
    this.donePause= 0;
    this.frame    = 0;
    this.stars    = Array.from({length:60}, ()=>({x:Math.random()*W,y:Math.random()*H,s:Math.random()+0.5}));
  }

  update(dt) {
    this.frame++;
    if (this.done) {
      this.donePause += dt;
      if (this.donePause > 1.5 || Input.confirm() || Input.cancel()) {
        SceneMgr.set(OverworldScene);
      }
      return;
    }
    if (this.lineIdx >= this.lines.length) { this.done=true; return; }
    const line = this.lines[this.lineIdx];
    if (this.charIdx < line.text.length) {
      this.timer += dt;
      if (this.timer > 0.03) {
        this.timer = 0; this.charIdx++; this.lineText = line.text.slice(0,this.charIdx);
      }
    } else {
      this.timer += dt;
      if (this.timer > line.delay || Input.confirm()) {
        this.timer=0; this.lineIdx++; this.charIdx=0; this.lineText='';
      }
    }
    if (Input.cancel()) { this.done=true; }
  }

  draw(c2d) {
    c2d.fillStyle='#00000e'; c2d.fillRect(0,0,W,H);
    c2d.fillStyle='#fff';
    this.stars.forEach(s=>c2d.fillRect(s.x,s.y,s.s,s.s));
    // Moon
    const mx=240, my=50;
    c2d.fillStyle='#eeeebb'; c2d.fillRect(mx-14,my-14,28,28);
    c2d.fillStyle='#cccc99'; c2d.fillRect(mx-4,my-10,6,6); c2d.fillRect(mx+4,my+2,4,4);
    // Ground silhouette
    c2d.fillStyle='#111122';
    for(let i=0;i<W;i+=4) c2d.fillRect(i,H-30-Math.sin(i*0.08)*8,4,40);

    // Text
    this.lines.slice(0,this.lineIdx).forEach((l,i)=>{
      const y = 80+i*22;
      const a = Math.min(1,(this.lineIdx-i)*0.8);
      c2d.save(); c2d.globalAlpha=a;
      drawTextShadow(c2d, l.text, (W-textWidth(l.text))>>1, y, l.color,'#00000e',1);
      c2d.restore();
    });
    if (this.lineIdx < this.lines.length) {
      drawTextShadow(c2d, this.lineText, (W-textWidth(this.lineText))>>1, 80+this.lineIdx*22, this.lines[this.lineIdx].color,'#00000e',1);
    }
    if (this.done) drawText(c2d,'Press Z to continue',(W-textWidth('Press Z to continue'))>>1,185,'#556688',1);
  }
}

// ── DIALOG / CUTSCENE SCENE ───────────────────
class DialogSequenceScene {
  constructor({dialogs, onDone, overworldRef}) {
    this.dialogs     = dialogs || [];
    this.onDone      = onDone  || (()=>{});
    this.overworldRef= overworldRef || null;
    this.idx         = 0;
    this.charIdx     = 0;
    this.timer       = 0;
    this.done        = false;
    this.waitBattle  = false;
    this.shopOpen    = false;
    this.shopSel     = 0;
    this.shopItems   = Object.entries(DIALOGS.shop_items);
  }

  enter() {}

  _currentLine() { return this.dialogs[this.idx]; }

  _advance() {
    const line = this._currentLine();
    if (!line) { this.done=true; return; }

    if (line.type === 'join') {
      const charId = line.char;
      if (!State.party.find(c=>c.id===charId)) {
        const lvl = State.party[0] ? State.party[0].level : 1;
        State.party.push(makeCharacter(charId, lvl));
      }
      Audio.sfx('join');
      this.idx++; this._startLine(); return;
    }
    if (line.type === 'battle') {
      State.flags['in_battle']=true;
      const enemies = line.enemies;
      const bg      = line.bg || 'plains';
      const music   = line.music || 'battle';
      const remaining = this.dialogs.slice(this.idx+1);
      const onDone = this.onDone;
      SceneMgr.set(BattleScene, {
        enemies, bg, music,
        onVictory: ()=>{
          SceneMgr.set(DialogSequenceScene, {dialogs: remaining, onDone});
        },
        onDefeat: ()=>{ SceneMgr.set(GameOverScene); },
      });
      return;
    }
    if (line.type === 'shop') {
      this.shopOpen = true; this.shopSel=0;
      this.idx++; return;
    }
    if (line.type === 'ending') {
      SceneMgr.set(EndingScene);
      return;
    }

    this.idx++;
    this._startLine();
  }

  _startLine() {
    const line = this._currentLine();
    if (!line) { this.done=true; return; }
    if (line.type) { this._advance(); return; }
    this.charIdx = 0;
    this.timer   = 0;
  }

  update(dt) {
    if (this.done) {
      if (this.onDone) { this.onDone(); this.onDone=null; }
      return;
    }

    if (this.shopOpen) {
      const items = this.shopItems;
      if (Input.pressed('ArrowUp'))   { this.shopSel=Math.max(0,this.shopSel-1); Audio.sfx('cursor'); }
      if (Input.pressed('ArrowDown')) { this.shopSel=Math.min(items.length,this.shopSel+1); Audio.sfx('cursor'); }
      if (Input.confirm()) {
        if (this.shopSel===items.length) { this.shopOpen=false; } // Exit
        else {
          const [id, itemData] = items[this.shopSel];
          if (State.gold >= itemData.price) {
            State.gold -= itemData.price;
            addItem(id,1);
            Audio.sfx('chest');
          } else { Audio.sfx('cancel'); }
        }
      }
      if (Input.cancel()) { this.shopOpen=false; }
      return;
    }

    const line = this._currentLine();
    if (!line) { this.done=true; return; }
    const text = line.text || '';
    if (this.charIdx < text.length) {
      this.timer += dt;
      if (this.timer > 0.025) {
        this.timer=0; this.charIdx++;
        if (this.charIdx%3===0) Audio.sfx('step');
      }
    } else {
      if (Input.confirm() || Input.cancel()) {
        Audio.sfx('confirm');
        this._advance();
      }
    }
  }

  draw(c2d) {
    // Draw overworld in background if available
    if (this.overworldRef) {
      this.overworldRef.draw(c2d);
    } else {
      c2d.fillStyle='#1a1a2e'; c2d.fillRect(0,0,W,H);
    }

    if (this.shopOpen) { this._drawShop(c2d); return; }

    const line = this._currentLine();
    if (!line) return;

    // Dialog box
    drawBox(c2d, 0, 158, W, 82, '#0d0d1e', '#3355aa');

    // Speaker portrait area
    const speaker = line.speaker || '';
    if (speaker) {
      drawBox(c2d, 2, 148, 80, 12, '#0a0a22', '#4466bb');
      drawText(c2d, speaker.toUpperCase(), 5, 151, '#ffdd88', 1);
    }

    // Text
    const displayText = (line.text || '').slice(0, this.charIdx);
    this._wrapText(c2d, displayText, 6, 168, W-12, '#e0e0f0', 1);

    // Advance indicator
    const text = line.text || '';
    if (this.charIdx >= text.length) {
      const blink = Math.floor(Date.now()/400)%2;
      if (blink) drawText(c2d, '▼', W-14, 230, '#aaaaff', 1);
    }
  }

  _drawShop(c2d) {
    drawBox(c2d, 30, 30, 260, 180, '#0d0d1e', '#3355aa');
    drawText(c2d, "MERCHANT'S SHOP", 70, 36, '#ffdd88', 1);
    drawText(c2d, `GOLD: ${State.gold}G`, 180, 36, '#ffcc44', 1);
    this.shopItems.forEach(([id, data], i) => {
      const sel = i===this.shopSel;
      if (sel) { c2d.fillStyle='#ffffff18'; c2d.fillRect(32,55+i*20,256,18); }
      drawText(c2d, data.name,  40, 58+i*20, sel?'#ffff00':'#cccccc', 1);
      drawText(c2d, `${data.price}G`, 220, 58+i*20, '#ffcc44', 1);
      drawText(c2d, ITEMS[id].desc, 40, 68+i*20, '#778899', 1);
    });
    const exitY = 55+this.shopItems.length*20;
    const selExit = this.shopSel===this.shopItems.length;
    if (selExit) { c2d.fillStyle='#ffffff18'; c2d.fillRect(32,exitY,256,14); }
    drawText(c2d, 'EXIT', 40, exitY+2, selExit?'#ff8888':'#888', 1);
    drawText(c2d, 'UP/DOWN:SELECT  Z:BUY  X:EXIT', 40, 195, '#445566', 1);
  }

  _wrapText(c2d, text, x, y, maxW, color, scale) {
    const cw = 6*scale;
    const words = text.split(' ');
    let line='', ly=y;
    for (const w of words) {
      const test = line ? line+' '+w : w;
      if (textWidth(test,scale) > maxW && line) {
        drawText(c2d, line, x, ly, color, scale);
        ly+=10*scale; line=w;
      } else { line=test; }
    }
    if (line) drawText(c2d, line, x, ly, color, scale);
  }
}

// ── MENU OVERLAY ──────────────────────────────
class MenuScene {
  constructor({onClose}) {
    this.onClose = onClose || (()=>{});
    this.tab     = 0;  // 0=chars, 1=items, 2=equip, 3=save
    this.selChar = 0;
    this.selItem = 0;
    this.useMode = false;
    this.selTarget=0;
  }

  update(dt) {
    if (Input.cancel() || Input.menu()) { Audio.sfx('cancel'); this.onClose(); return; }

    const tabs = ['CHARS','ITEMS','SAVE'];
    if (Input.pressed('ArrowLeft'))  { this.tab=Math.max(0,this.tab-1); Audio.sfx('cursor'); }
    if (Input.pressed('ArrowRight')) { this.tab=Math.min(tabs.length-1,this.tab+1); Audio.sfx('cursor'); }

    if (this.tab===0) { // chars
      if (Input.pressed('ArrowUp'))   { this.selChar=Math.max(0,this.selChar-1); Audio.sfx('cursor'); }
      if (Input.pressed('ArrowDown')) { this.selChar=Math.min(State.party.length-1,this.selChar+1); Audio.sfx('cursor'); }
    } else if (this.tab===1) { // items
      const inv=State.inventory;
      if (!this.useMode) {
        if (Input.pressed('ArrowUp'))   { this.selItem=Math.max(0,this.selItem-1); Audio.sfx('cursor'); }
        if (Input.pressed('ArrowDown')) { this.selItem=Math.min(inv.length-1,this.selItem+1); Audio.sfx('cursor'); }
        if (Input.confirm() && inv.length) {
          const item=ITEMS[inv[this.selItem].id];
          if (item.target==='single_ally'||item.target==='all_allies') {
            this.useMode=true; this.selTarget=0; Audio.sfx('confirm');
          }
        }
      } else {
        if (Input.pressed('ArrowUp'))   { this.selTarget=Math.max(0,this.selTarget-1); Audio.sfx('cursor'); }
        if (Input.pressed('ArrowDown')) { this.selTarget=Math.min(State.party.length-1,this.selTarget+1); Audio.sfx('cursor'); }
        if (Input.confirm()) {
          const slot=inv[this.selItem];
          if (!slot){this.useMode=false;return;}
          const item=ITEMS[slot.id];
          const target=State.party[this.selTarget];
          if(!target||target.dead){Audio.sfx('cancel');return;}
          removeItem(slot.id);
          if(item.type==='healing'){
            target.hp=Math.min(target.maxHp,target.hp+(item.hp||0));
            target.mp=Math.min(target.maxMp,target.mp+(item.mp||0));
            Audio.sfx('heal');
          }
          this.useMode=false;
          Audio.sfx('confirm');
        }
        if (Input.cancel()) { this.useMode=false; Audio.sfx('cancel'); }
      }
    } else if (this.tab===2) { // save
      if (Input.confirm()) { this._save(); }
    }
  }

  _save() {
    const saveData = {
      party: State.party,
      inventory: State.inventory,
      flags: State.flags,
      mapId: State.mapId,
      playerX: State.playerX,
      playerY: State.playerY,
      playerFacing: State.playerFacing,
      gold: State.gold,
    };
    localStorage.setItem('chronorift_save', JSON.stringify(saveData));
    Audio.sfx('chest');
  }

  draw(c2d) {
    // semi-transparent overlay
    c2d.fillStyle='rgba(0,0,10,0.85)'; c2d.fillRect(0,0,W,H);
    drawBox(c2d,0,0,W,H,'#0d0d1e88','#3355aa');

    // Tabs
    const tabs=['CHARS','ITEMS','SAVE'];
    tabs.forEach((t,i)=>{
      const sel=i===this.tab;
      if(sel){c2d.fillStyle='#ffffff22';c2d.fillRect(i*106,0,104,14);}
      drawText(c2d,t,4+i*106,3,sel?'#ffff00':'#888',1);
    });
    drawText(c2d,`GOLD: ${State.gold}G`,220,3,'#ffcc44',1);

    if (this.tab===0) this._drawChars(c2d);
    else if (this.tab===1) this._drawItems(c2d);
    else if (this.tab===2) this._drawSave(c2d);
  }

  _drawChars(c2d) {
    State.party.forEach((c,i)=>{
      const y=20+i*65;
      const sel=i===this.selChar;
      if(sel){c2d.fillStyle='#ffffff10';c2d.fillRect(0,y,W,62);}
      drawCharSprite(c2d,c.sprite,4,y+10,2,0,!c.dead);
      drawText(c2d,`${c.name} Lv.${c.level}`,26,y+4,c.color,1);
      drawText(c2d,`HP:`,26,y+15,'#aaa',1);
      drawHpBar(c2d,44,y+16,100,c.hp,c.maxHp);
      drawText(c2d,`${c.hp}/${c.maxHp}`,150,y+14,'#fff',1);
      drawText(c2d,`MP:`,26,y+24,'#6af',1);
      drawMpBar(c2d,44,y+25,100,c.mp,c.maxMp);
      drawText(c2d,`${c.mp}/${c.maxMp}`,150,y+24,'#8cf',1);
      drawText(c2d,`EXP: ${c.exp}/${c.expNext}`,26,y+34,'#aaa',1);
      drawText(c2d,`ATK:${c.atk} DEF:${c.def} MAG:${c.mag} SPD:${c.spd}`,26,y+44,'#778899',1);
      if(sel && c.skills.length){
        drawText(c2d,`SKILLS: ${c.skills.map(s=>SKILLS[s].name).join(' ')}`,4,y+54,'#aaaacc',1);
      }
    });
  }

  _drawItems(c2d) {
    const inv=State.inventory.filter(s=>s.count>0);
    if(!inv.length){drawText(c2d,'No items.',4,24,'#666',1);return;}
    inv.forEach((slot,i)=>{
      const y=20+i*20;
      const sel=i===this.selItem&&!this.useMode;
      if(sel){c2d.fillStyle='#ffffff18';c2d.fillRect(0,y,W,18);}
      const item=ITEMS[slot.id];
      drawText(c2d,`${item.name}`,6,y+4,sel?'#ffff00':'#ccc',1);
      drawText(c2d,`x${slot.count}`,100,y+4,'#aaa',1);
      drawText(c2d,item.desc,120,y+4,'#778899',1);
    });
    if(this.useMode){
      drawBox(c2d,60,150,200,60,'#0d0d1e','#5588cc');
      drawText(c2d,'USE ON:',70,156,'#ffdd88',1);
      State.party.forEach((c,i)=>{
        const y=168+i*14;
        const sel=i===this.selTarget;
        if(sel){c2d.fillStyle='#ffffff22';c2d.fillRect(62,y-2,196,13);}
        drawText(c2d,`${c.name} ${c.hp}/${c.maxHp}HP`,68,y,sel?'#ffff00':'#ccc',1);
      });
    }
  }

  _drawSave(c2d) {
    drawBox(c2d,40,40,240,100,'#0d0d1e','#3355aa');
    drawText(c2d,'SAVE GAME',90,50,'#ffdd88',1);
    drawText(c2d,'Your progress will be saved',50,68,'#aaa',1);
    drawText(c2d,'to this browser.',50,80,'#aaa',1);
    drawText(c2d,'Press Z to save.',90,100,'#ffcc44',1);
    drawText(c2d,'Press X to cancel.',82,116,'#888',1);
  }
}

// ── OVERWORLD / EXPLORATION SCENE ────────────
class OverworldScene {
  constructor() {
    this.map       = null;
    this.camX      = 0;
    this.camY      = 0;
    this.moveTimer = 0;
    this.moveDelay = 0.13;
    this.frame     = 0;
    this.walkFrame = 0;
    this.walkTimer = 0;
    this.dialogActive = false;
    this.dialogScene  = null;
    this.menuOpen     = false;
    this.menuScene    = null;
    this.msgText      = '';
    this.msgTimer     = 0;
    this.encTimer     = 0;
    this.encSteps     = 0;
    this.transitioning= false;
  }

  enter() {
    this._loadMap(State.mapId);
    Audio.playMusic(this.map.music);
  }

  exit() { Audio.stopMusic(); }

  _loadMap(id) {
    this.map = MAPS[id];
    State.mapId = id;
    this._clampCamera();
    this.encTimer = 0;
    this.encSteps = 0;
    // Process any auto-events on enter (but not warps/events that need walking)
  }

  _clampCamera() {
    const mapPxW = this.map.width  * TS;
    const mapPxH = this.map.height * TS;
    this.camX = clamp(State.playerX*TS - W/2, 0, Math.max(0,mapPxW-W));
    this.camY = clamp(State.playerY*TS - H/2, 0, Math.max(0,mapPxH-H));
  }

  update(dt) {
    this.frame++;
    this.walkTimer += dt;

    if (this.dialogActive) {
      this.dialogScene.update(dt);
      if (this.dialogScene.done) {
        this.dialogActive = false;
        this.dialogScene  = null;
        // Re-enter overworld music
        Audio.playMusic(this.map.music);
      }
      return;
    }
    if (this.menuOpen) {
      this.menuScene.update(dt);
      return;
    }

    if (this.msgTimer > 0) { this.msgTimer -= dt; }
    if (this.transitioning) return;

    if (Input.menu()) {
      Audio.sfx('confirm');
      this.menuOpen  = true;
      this.menuScene = new MenuScene({onClose:()=>{ this.menuOpen=false; this.menuScene=null; }});
      return;
    }
    if (Input.pressed('m') || Input.pressed('M')) { Audio.toggleMute(); }

    // Z key: interact with adjacent object
    if (Input.confirm()) { this._interact(); }

    this._handleMovement(dt);
  }

  _handleMovement(dt) {
    this.moveTimer -= dt;
    if (this.moveTimer > 0) return;

    const dir = Input.dir();
    if (!dir.x && !dir.y) return;

    const nx = State.playerX + dir.x;
    const ny = State.playerY + dir.y;

    // Face direction
    if      (dir.y < 0) State.playerFacing = 0;
    else if (dir.x > 0) State.playerFacing = 1;
    else if (dir.y > 0) State.playerFacing = 2;
    else if (dir.x < 0) State.playerFacing = 3;

    // Bounds check
    if (nx<0||nx>=this.map.width||ny<0||ny>=this.map.height) return;

    // Tile collision
    const tile = this.map.data[ny] && this.map.data[ny][nx];
    if (isSolid(tile)) {
      this.moveTimer = this.moveDelay * 0.5;
      return;
    }

    // Object collision (NPCs, chests)
    const obj = this._objectAt(nx, ny);
    if (obj && (obj.type==='npc'||obj.type==='chest')) {
      if (Input.confirm() || true) { // always check on move into it
        // Only block if object is NPC or solid chest; warps/events are passable
        if (obj.type==='npc') {
          this.moveTimer = this.moveDelay;
          // Don't walk through NPCs, but face them
          return;
        }
      }
    }

    // Move player
    State.playerX = nx;
    State.playerY = ny;
    this.moveTimer = this.moveDelay;
    if (this.walkTimer > 0.1) { this.walkFrame++; this.walkTimer=0; }
    if (this.frame%8===0) Audio.sfx('step');

    // Smooth camera
    const targetCX = State.playerX*TS - W/2;
    const targetCY = State.playerY*TS - H/2;
    const mapPxW = this.map.width*TS, mapPxH=this.map.height*TS;
    this.camX = clamp(targetCX,0,Math.max(0,mapPxW-W));
    this.camY = clamp(targetCY,0,Math.max(0,mapPxH-H));

    // Check for objects on new tile
    this._checkTileObjects(nx, ny);

    // Random encounters
    if (this.map.encounters) {
      this.encSteps++;
      const r = Math.random();
      if (r < this.map.encounters.rate) {
        this._triggerEncounter();
        return;
      }
    }
  }

  _objectAt(x, y, checkCond=true) {
    if (!this.map.objects) return null;
    return this.map.objects.find(o => {
      if (o.x!==x || o.y!==y) return false;
      if (checkCond && o.condition) return this._evalCondition(o.condition);
      return true;
    }) || null;
  }

  _evalCondition(cond) {
    if (!cond) return true;
    // Simple parser: flag_xxx, !flag_xxx, combined with &&
    return cond.split('&&').every(part => {
      part = part.trim();
      if (part.startsWith('!')) return !State.flags[part.slice(1)];
      return !!State.flags[part];
    });
  }

  _checkTileObjects(x, y) {
    const obj = this._objectAt(x, y);
    if (!obj) return;

    if (obj.type === 'warp') {
      this.transitioning = true;
      Audio.sfx('warp');
      setTimeout(()=>{
        State.playerX = obj.tx; State.playerY = obj.ty;
        this._loadMap(obj.target);
        this.transitioning = false;
        Audio.playMusic(this.map.music);
      }, 400);
      return;
    }
    if (obj.type === 'event') {
      if (!State.flags[`event_${obj.event}`]) {
        this._triggerEvent(obj.event, obj);
      }
      return;
    }
  }

  _triggerEvent(name, obj) {
    State.flags[`event_${name}`] = true;
    switch(name) {
      case 'festival_cutscene':
        State.flags.flag_festival_done = true;
        this._startDialog(DIALOGS.festival_cutscene, ()=>{
          State.flags.flag_gorak_joined = true;
        });
        break;
      case 'gorak_join':
        State.flags.flag_gorak_joined = true;
        this._startDialog(DIALOGS.gorak_join, ()=>{
          this._showMsg('Head deeper into the cave!');
        });
        break;
      case 'boss_shadowcore':
        State.flags.flag_boss_done = true;
        this._startDialog(DIALOGS.boss_shadowcore, null);
        break;
    }
  }

  _triggerEncounter() {
    const table = this.map.encounters.table;
    const total = table.reduce((s,e)=>s+e.w,0);
    let r = Math.random()*total;
    let chosen = table[0];
    for (const e of table) { r-=e.w; if(r<=0){chosen=e;break;} }

    // Random 1-3 enemies
    const count = randInt(1, 2);
    const enemies = Array.from({length:count},()=>chosen.id);

    Audio.sfx('encounter');
    const bg = this.map.id==='cave'?'cave':this.map.id==='overworld'?'plains':'forest';
    this.transitioning=true;
    setTimeout(()=>{
      SceneMgr.set(BattleScene, {
        enemies,
        bg,
        music:'battle',
        onVictory:()=>{
          this.transitioning=false;
          SceneMgr.set(OverworldScene);
        },
        onDefeat:()=>{ SceneMgr.set(GameOverScene); },
      });
    }, 300);
  }

  _startDialog(lines, onDone) {
    this.dialogActive = true;
    this.dialogScene  = new DialogSequenceScene({
      dialogs: lines,
      onDone: ()=>{
        this.dialogActive=false;
        this.dialogScene=null;
        if(onDone) onDone();
        Audio.playMusic(this.map.music);
      },
      overworldRef: this,
    });
  }

  _showMsg(text) {
    this.msgText  = text;
    this.msgTimer = 3;
  }

  // Z key: interact with adjacent NPC
  _interact() {
    const dx=[0,1,0,-1], dy=[-1,0,1,0];
    const f=State.playerFacing;
    const tx=State.playerX+dx[f], ty=State.playerY+dy[f];
    const obj=this._objectAt(tx,ty,true)||this._objectAt(tx,ty,false);
    if(!obj) return;

    if(obj.type==='npc'){
      if(obj.dialog==='shop'){
        this._startDialog([{speaker:'Merchant',text:"Welcome! Take a look at my wares."},{type:'shop'}],null);
        return;
      }
      if(obj.dialog==='lyra_join'){
        State.flags.flag_lyra_joined=true;
        this._startDialog(DIALOGS.lyra_join, ()=>this._showMsg('Lyra has joined!'));
        return;
      }
      const dialog=DIALOGS[obj.dialog];
      if(dialog){ this._startDialog(dialog, null); return; }
      this._showMsg(obj.dialog||'...');
      return;
    }
    if(obj.type==='chest'){
      if(!State.chestedMaps) State.chestedMaps={};
      const mapKey=`${State.mapId}:${obj.x},${obj.y}`;
      if(State.chestedMaps[mapKey]){this._showMsg('(Empty)');return;}
      State.chestedMaps[mapKey]=true;
      addItem(obj.item, obj.count||1);
      Audio.sfx('chest');
      this._showMsg(`Found ${obj.count||1}x ${ITEMS[obj.item].name}!`);
      return;
    }
    if(obj.type==='sign'){
      this._showMsg(obj.text||'(sign)');
      return;
    }
  }

  draw(c2d) {
    // Render tilemap
    const startX = Math.floor(this.camX/TS);
    const startY = Math.floor(this.camY/TS);
    const endX   = Math.min(this.map.width,  startX + Math.ceil(W/TS)+1);
    const endY   = Math.min(this.map.height, startY + Math.ceil(H/TS)+1);

    for(let ty=startY;ty<endY;ty++){
      for(let tx=startX;tx<endX;tx++){
        const row=this.map.data[ty];
        if(!row) continue;
        const tid=row[tx]||0;
        const px=Math.round(tx*TS-this.camX);
        const py=Math.round(ty*TS-this.camY);
        drawTile(c2d,tid,px,py);
      }
    }

    // Draw objects
    if(this.map.objects){
      this.map.objects.forEach(obj=>{
        if(!this._evalCondition(obj.condition)) return;
        const px=Math.round(obj.x*TS-this.camX);
        const py=Math.round(obj.y*TS-this.camY);
        if(px<-TS||px>W+TS||py<-TS||py>H+TS) return;
        if(obj.type==='npc'){
          drawNPC(c2d,obj.sprite,px,py-4,this.frame);
        } else if(obj.type==='chest'){
          const mapKey=`${State.mapId}:${obj.x},${obj.y}`;
          const opened=!!(State.chestedMaps&&State.chestedMaps[mapKey]);
          _chestObj(c2d,px,py-4,opened);
        } else if(obj.type==='event'&&obj.event==='boss_shadowcore'){
          _shadowGate(c2d,px,py-4,this.frame);
        } else if(obj.type==='event'&&obj.event==='festival_cutscene'){
          _warpGate(c2d,px,py-4,this.frame);
        }
      });
    }

    // Draw player
    const ppx=Math.round(State.playerX*TS-this.camX);
    const ppy=Math.round(State.playerY*TS-this.camY);
    drawCharSprite(c2d,'kael',ppx,ppy-4,State.playerFacing,this.walkFrame);

    // Mini party indicators
    State.party.slice(1).forEach((c,i)=>{
      drawCharSprite(c2d,c.sprite,ppx-(i+1)*6,ppy,State.playerFacing,this.walkFrame,!c.dead);
    });

    // Map name
    drawText(c2d, this.map.name, 2, 2, '#ffffffaa', 1);

    // Party HP bar strip (top right)
    State.party.forEach((c,i)=>{
      const bx=W-62, by=2+i*10;
      drawText(c2d,c.name.slice(0,4),bx-24,by,'#ffffff88',1);
      drawHpBar(c2d,bx,by+1,60,c.hp,c.maxHp);
    });

    // Gold
    drawText(c2d,`${State.gold}G`,W-26,H-10,'#ffcc44',1);

    // Message box
    if(this.msgTimer>0&&this.msgText){
      const tw=textWidth(this.msgText);
      const bx=(W-tw-8)>>1, by=H-50;
      drawBox(c2d,bx-2,by-3,tw+12,16,'#1a1a2e','#5588cc');
      drawText(c2d,this.msgText,bx+2,by,'#ffffcc',1);
    }

    // Z-to-interact hint
    if(!this.dialogActive&&!this.menuOpen){
      const dx=[0,1,0,-1],dy=[-1,0,1,0];
      const f=State.playerFacing;
      const tx2=State.playerX+dx[f],ty2=State.playerY+dy[f];
      const adj=this._objectAt(tx2,ty2,false);
      if(adj&&(adj.type==='npc'||adj.type==='chest'||adj.type==='sign')){
        drawText(c2d,'Z: TALK/USE',2,H-10,'#5566aa',1);
      }
    }

    // Dialog overlay
    if(this.dialogActive&&this.dialogScene){
      this.dialogScene.draw(c2d);
    }
    // Menu overlay
    if(this.menuOpen&&this.menuScene){
      this.menuScene.draw(c2d);
    }
  }
}

// ── GAME OVER SCENE ───────────────────────────
class GameOverScene {
  constructor() { this.timer=0; this.frame=0; }
  enter() { Audio.sfx('gameover'); }
  update(dt) {
    this.timer+=dt; this.frame++;
    if((this.timer>2&&Input.confirm())||this.timer>8){
      SceneMgr.set(TitleScene);
    }
  }
  draw(c2d) {
    c2d.fillStyle='#080008'; c2d.fillRect(0,0,W,H);
    const pulse=0.5+0.5*Math.sin(this.frame*0.03);
    c2d.fillStyle=`rgba(40,0,20,${pulse})`;c2d.fillRect(0,0,W,H);
    drawTextShadow(c2d,'GAME OVER',86,90,'#cc2222','#000',2);
    if(this.timer>2){drawText(c2d,'Press Z to return',70,130,'#556',1);}
  }
}

// ── ENDING SCENE ─────────────────────────────
class EndingScene {
  constructor() {
    this.frame  = 0;
    this.timer  = 0;
    this.phase  = 0;
    this.stars  = Array.from({length:100},()=>({x:Math.random()*W,y:Math.random()*H,s:Math.random()+0.5,sp:Math.random()*0.3+0.1}));
    this.credits= [
      'CHRONO RIFT',
      '',
      'THE SHADOWTIDE IS DEFEATED.',
      'THE TIMELINE IS RESTORED.',
      '',
      'KAEL, LYRA AND GORAK',
      'RETURN TO TRUCE VILLAGE',
      'AS THE SUN RISES OVER GUARDIA.',
      '',
      'A NEW ERA OF PEACE BEGINS.',
      '',
      '~ FIN ~',
      '',
      '',
      'THANK YOU FOR PLAYING!',
    ];
    this.scrollY = H;
  }

  enter() { Audio.playMusic('ending'); }
  exit()  { Audio.stopMusic(); }

  update(dt) {
    this.frame++;
    this.timer+=dt;
    this.stars.forEach(s=>{s.x-=s.sp;if(s.x<0){s.x=W;s.y=Math.random()*H;}});
    this.scrollY -= 18*dt;
    if(this.scrollY < -this.credits.length*16 - 20){
      this.scrollY = H;
    }
    if(Input.confirm()&&this.timer>3){SceneMgr.set(TitleScene);}
  }

  draw(c2d) {
    c2d.fillStyle='#000010';c2d.fillRect(0,0,W,H);
    c2d.fillStyle='#fff';
    this.stars.forEach(s=>c2d.fillRect(Math.round(s.x),Math.round(s.y),Math.round(s.s),Math.round(s.s)));

    // Characters at bottom
    const bob=Math.sin(this.frame*0.04)*2;
    drawCharSprite(c2d,'kael', 100,190+bob,2,Math.floor(this.frame/12)%2);
    drawCharSprite(c2d,'lyra', 140,190+bob,2,Math.floor(this.frame/12)%2);
    drawCharSprite(c2d,'gorak',180,190+bob,2,Math.floor(this.frame/12)%2);

    // Scrolling credits
    this.credits.forEach((line,i)=>{
      const y=Math.round(this.scrollY+i*16);
      if(y<-10||y>H+10) return;
      const col=line==='CHRONO RIFT'?'#88ccff':line==='~ FIN ~'?'#ffdd88':'#aaaacc';
      const sc=line==='CHRONO RIFT'?2:1;
      const tx=(W-textWidth(line,sc))>>1;
      drawTextShadow(c2d,line,tx,y,col,'#000',sc);
    });

    if(this.timer>3){
      const blink=Math.floor(Date.now()/600)%2;
      if(blink) drawText(c2d,'Press Z for title',(W-textWidth('Press Z for title'))>>1,225,'#446688',1);
    }
  }
}
