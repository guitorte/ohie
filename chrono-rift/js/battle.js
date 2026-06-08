'use strict';
// ─────────────────────────────────────────────
//  battle.js  –  ATB battle system
// ─────────────────────────────────────────────

const ATB_RATE = 22;  // gauge units per second per speed point
const ATB_MAX  = 100;

class BattleScene {
  constructor({enemies, bg, music, onVictory, onDefeat, postDialog}) {
    this.enemies     = enemies.map(id => makeEnemy(id));
    this.bg          = bg || 'plains';
    this.music       = music || 'battle';
    this.onVictory   = onVictory || (() => {});
    this.onDefeat    = onDefeat  || (() => {});
    this.postDialog  = postDialog || null;

    this.party       = State.party;
    this.particles   = [];
    this.floaters    = [];   // damage numbers
    this.animQueue   = [];   // {fn, delay} steps
    this.animTimer   = 0;
    this.frame       = 0;

    // ATB state
    this.state       = 'running';  // running | char_turn | result
    this.activeChar  = null;
    this.menuState   = 'main';     // main | skill | item | target_enemy | target_ally
    this.selMenu     = 0;
    this.selSkill    = 0;
    this.selItem     = 0;
    this.selTarget   = 0;
    this.pendingSkill= null;
    this.pendingItem = null;

    this.resultState = null;  // victory | defeat
    this.resultTimer = 0;
    this.expGained   = 0;
    this.goldGained  = 0;
    this.levelUps    = [];

    // Positions
    this._calcPositions();

    // Reset ATB & defending
    this.party.forEach(c => { c.atb = c.dead ? 0 : randInt(0,30); c.defending=false; });
    this.enemies.forEach(e => { e.atb = randInt(0,30); });
  }

  enter() {
    Audio.playMusic(this.music);
  }

  _calcPositions() {
    // Party: right side, stacked vertically
    const alive = this.party.filter(c=>!c.dead);
    const partyCount = this.party.length;
    this.partyPos = this.party.map((c,i) => ({
      x: 195 + (i%2)*15,
      y: 50 + i * 52,
    }));
    // Enemies: left side
    const eCount = this.enemies.length;
    this.enemyPos = this.enemies.map((e,i) => ({
      x: eCount === 1 ? 60 : 30 + i * 55,
      y: eCount === 1 ? 50 : 55 + (i%2)*20,
    }));
  }

  update(dt) {
    this.frame++;
    // Update particles & floaters
    this.particles = this.particles.filter(p => { p.update(dt); return !p.dead; });
    this.floaters  = this.floaters.filter(f  => { f.life -= dt; f.y -= 18*dt; return f.life > 0; });

    // Shake timer on enemies
    this.enemies.forEach(e => {
      if (e.shakTimer > 0) { e.shakTimer -= dt; if (e.shakTimer<=0) e.shaking=false; }
    });
    this.party.forEach(c => {
      if (c.shakTimer > 0) { c.shakTimer -= dt; if (c.shakTimer<=0) c.shaking=false; }
    });

    // Status timers
    [...this.party, ...this.enemies].forEach(u => {
      if (u.status === 'poison' && !u.dead) {
        u.statusTimer -= dt;
        if (u.statusTimer <= 0) {
          const dmg = Math.max(1, Math.round(u.maxHp * 0.05));
          this._applyDmg(u, dmg, '#44cc44');
          u.statusTimer = 3;
        }
      }
      if (u.status === 'stun') {
        u.statusTimer -= dt;
        if (u.statusTimer <= 0) u.status = null;
      }
    });

    if (this.state === 'running') {
      this._tickATB(dt);
    } else if (this.state === 'char_turn') {
      this._handleInput();
    } else if (this.state === 'anim') {
      this.animTimer -= dt;
      if (this.animTimer <= 0) {
        if (this.animQueue.length > 0) {
          const step = this.animQueue.shift();
          step.fn();
          this.animTimer = step.delay || 0.4;
        } else {
          this.state = 'running';
          this._checkEnd();
        }
      }
    } else if (this.state === 'result') {
      this.resultTimer -= dt;
      if (this.resultTimer <= 0) {
        if (this.resultState === 'victory') {
          Audio.stopMusic();
          if (this.postDialog) {
            SceneMgr.set(DialogSequenceScene, {
              dialogs: DIALOGS[this.postDialog] || [],
              onDone: this.onVictory,
            });
          } else {
            this.onVictory(this.expGained, this.goldGained, this.levelUps);
          }
        } else {
          Audio.stopMusic();
          this.onDefeat();
        }
      }
    }
  }

  _tickATB(dt) {
    // Fill party gauges
    for (const c of this.party) {
      if (c.dead || c.status==='stun') continue;
      if (c.atb < ATB_MAX) {
        c.atb = Math.min(ATB_MAX, c.atb + c.spd * ATB_RATE * dt);
        if (c.atb >= ATB_MAX && this.state === 'running') {
          this.state = 'char_turn';
          this.activeChar = c;
          this.menuState = 'main';
          this.selMenu = 0;
          Audio.sfx('cursor');
          return;
        }
      }
    }
    // Fill enemy gauges
    for (const e of this.enemies) {
      if (e.dead || e.status==='stun') continue;
      if (e.atb < ATB_MAX) {
        e.atb = Math.min(ATB_MAX, e.atb + e.spd * ATB_RATE * dt);
        if (e.atb >= ATB_MAX && this.state === 'running') {
          e.atb = 0;
          this._enemyTurn(e);
          return;
        }
      }
    }
  }

  _handleInput() {
    const ip = Input;
    const c  = this.activeChar;
    if (!c) return;

    if (this.menuState === 'main') {
      const opts = ['Attack','Skill','Item','Defend'];
      if (ip.pressed('ArrowLeft'))  { this.selMenu = Math.max(0, this.selMenu-1); Audio.sfx('cursor'); }
      if (ip.pressed('ArrowRight')) { this.selMenu = Math.min(opts.length-1, this.selMenu+1); Audio.sfx('cursor'); }
      if (ip.confirm()) {
        Audio.sfx('confirm');
        switch(opts[this.selMenu]) {
          case 'Attack':  this.menuState='target_enemy'; this.selTarget=0; break;
          case 'Skill':   this.menuState='skill';        this.selSkill=0;  break;
          case 'Item':    this.menuState='item';         this.selItem=0;   break;
          case 'Defend':  this._executeDefend(c); break;
        }
      }
    } else if (this.menuState === 'skill') {
      const skills = c.skills;
      if (!skills.length) { this.menuState='main'; return; }
      if (ip.pressed('ArrowUp'))   { this.selSkill = Math.max(0, this.selSkill-1); Audio.sfx('cursor'); }
      if (ip.pressed('ArrowDown')) { this.selSkill = Math.min(skills.length-1, this.selSkill+1); Audio.sfx('cursor'); }
      if (ip.confirm()) {
        const sk = SKILLS[skills[this.selSkill]];
        if (!sk) return;
        if (c.mp < sk.mp) { Audio.sfx('cancel'); return; }
        this.pendingSkill = skills[this.selSkill];
        Audio.sfx('confirm');
        if (sk.target === 'all_enemies' || sk.target === 'all_allies') {
          this._executeSkill(c, this.pendingSkill, null);
        } else if (sk.target === 'single_ally') {
          this.menuState = 'target_ally'; this.selTarget = 0;
        } else {
          this.menuState = 'target_enemy'; this.selTarget = 0;
        }
      }
      if (ip.cancel()) { Audio.sfx('cancel'); this.menuState='main'; }
    } else if (this.menuState === 'item') {
      const inv = State.inventory.filter(s=>s.count>0);
      if (!inv.length) { this.menuState='main'; return; }
      if (ip.pressed('ArrowUp'))   { this.selItem = Math.max(0, this.selItem-1); Audio.sfx('cursor'); }
      if (ip.pressed('ArrowDown')) { this.selItem = Math.min(inv.length-1, this.selItem+1); Audio.sfx('cursor'); }
      if (ip.confirm()) {
        const slot = inv[this.selItem];
        if (!slot) return;
        const item = ITEMS[slot.id];
        this.pendingItem = slot.id;
        Audio.sfx('confirm');
        if (item.target === 'all_enemies') {
          this._executeItem(c, this.pendingItem, null);
        } else if (item.target === 'single_ally') {
          this.menuState = 'target_ally'; this.selTarget = 0;
        } else {
          this.menuState = 'target_enemy'; this.selTarget = 0;
        }
      }
      if (ip.cancel()) { Audio.sfx('cancel'); this.menuState='main'; }
    } else if (this.menuState === 'target_enemy') {
      const alive = this.enemies.filter(e=>!e.dead);
      if (!alive.length) { this.menuState='main'; return; }
      if (ip.pressed('ArrowLeft'))  { this.selTarget = Math.max(0, this.selTarget-1); Audio.sfx('cursor'); }
      if (ip.pressed('ArrowRight')) { this.selTarget = Math.min(alive.length-1, this.selTarget+1); Audio.sfx('cursor'); }
      if (ip.confirm()) {
        Audio.sfx('confirm');
        const target = alive[this.selTarget];
        if (this.pendingSkill) {
          this._executeSkill(c, this.pendingSkill, target);
        } else {
          this._executeAttack(c, target);
        }
      }
      if (ip.cancel()) {
        Audio.sfx('cancel');
        this.menuState = this.pendingSkill ? 'skill' : 'main';
        this.pendingSkill = null;
      }
    } else if (this.menuState === 'target_ally') {
      const alive = this.party.filter(p=>!p.dead);
      const all   = this.party;
      const tgts  = this.pendingItem ? all : alive;
      if (ip.pressed('ArrowLeft'))  { this.selTarget = Math.max(0, this.selTarget-1); Audio.sfx('cursor'); }
      if (ip.pressed('ArrowRight')) { this.selTarget = Math.min(tgts.length-1, this.selTarget+1); Audio.sfx('cursor'); }
      if (ip.confirm()) {
        Audio.sfx('confirm');
        const target = tgts[this.selTarget];
        if (this.pendingItem) {
          this._executeItem(c, this.pendingItem, target);
        } else {
          this._executeSkill(c, this.pendingSkill, target);
        }
      }
      if (ip.cancel()) {
        Audio.sfx('cancel');
        this.menuState = this.pendingItem ? 'item' : 'skill';
        this.pendingItem = null; this.pendingSkill = null;
      }
    }
  }

  _executeAttack(attacker, target) {
    this.state = 'anim';
    this.animTimer = 0.05;
    const dmg = this._calcDmg(attacker, target, 1.0, 'physical');
    this.animQueue = [
      {fn: ()=>{ Audio.sfx('attack'); this._applyDmg(target, dmg, '#ffffff'); target.shaking=true; target.shakTimer=0.3; }, delay:0.5},
      {fn: ()=>{ attacker.atb=0; attacker.defending=false; this.pendingSkill=null; }, delay:0.1},
    ];
  }

  _executeSkill(attacker, skillId, target) {
    const sk   = SKILLS[skillId];
    const cost = sk.mp;
    if (attacker.mp < cost) { this.state='char_turn'; return; }
    attacker.mp = Math.max(0, attacker.mp - cost);
    this.state = 'anim';
    this.animTimer = 0.05;

    const steps = [];
    if (sk.type === 'healing') {
      Audio.sfx('heal');
      const targets = sk.target === 'all_allies' ? this.party.filter(c=>!c.dead) : [target];
      steps.push({fn:()=>{
        targets.forEach(t=>{
          const heal = Math.min(t.maxHp-t.hp, sk.power);
          t.hp = Math.min(t.maxHp, t.hp+heal);
          this._addFloater(this._partyPosOf(t), `+${heal}`, '#44ff88');
          spawnHealParticles(this.particles, this._partyPosOf(t).x+8, this._partyPosOf(t).y+8);
        });
      }, delay:0.6});
    } else if (sk.type === 'status') {
      const targets2 = sk.target==='all_allies' ? this.party.filter(c=>!c.dead)
                     : sk.target==='all_enemies' ? this.enemies.filter(e=>!e.dead)
                     : [target];
      steps.push({fn:()=>{
        Audio.sfx('magic');
        targets2.forEach(t=>{
          if (!t.status) { t.status=sk.status; t.statusTimer=sk.status==='stun'?2:999; }
          this._addFloater(this._posOf(t), sk.status.toUpperCase(), '#ffaa00');
        });
      }, delay:0.7});
    } else {
      const targets3 = sk.target==='all_enemies' ? this.enemies.filter(e=>!e.dead)
                     : sk.target==='all_allies'  ? this.party.filter(c=>!c.dead)
                     : [target];
      const sfxName = sk.type==='magic' ? 'magic' : 'attack';
      steps.push({fn:()=>{
        Audio.sfx(sfxName);
        targets3.forEach(t=>{
          const dmg = this._calcDmg(attacker, t, sk.power, sk.type);
          this._applyDmg(t, dmg, sk.type==='magic'?'#88aaff':'#ffcc44');
          t.shaking=true; t.shakTimer=0.3;
        });
      }, delay:0.7});
    }
    steps.push({fn:()=>{ attacker.atb=0; attacker.defending=false; this.pendingSkill=null; }, delay:0.1});
    this.animQueue = steps;
  }

  _executeItem(user, itemId, target) {
    const item = ITEMS[itemId];
    if (!removeItem(itemId)) { this.state='char_turn'; return; }
    this.state = 'anim';
    this.animTimer = 0.05;
    this.animQueue = [
      {fn:()=>{
        Audio.sfx('heal');
        if (item.type === 'healing') {
          const targets = item.target==='all_allies' ? this.party : [target];
          targets.forEach(t=>{
            if (t.dead) return;
            const hp = Math.min(t.maxHp-t.hp, item.hp||0);
            const mp = Math.min(t.maxMp-t.mp, item.mp||0);
            t.hp = Math.min(t.maxHp, t.hp+hp);
            t.mp = Math.min(t.maxMp, t.mp+mp);
            if (hp>0) { this._addFloater(this._partyPosOf(t), `+${hp}`, '#44ff88'); spawnHealParticles(this.particles,this._partyPosOf(t).x+8,this._partyPosOf(t).y+8); }
          });
        } else if (item.type === 'revive') {
          if (target.dead) {
            target.dead=false; target.hp=Math.floor(target.maxHp*(item.hp||0.5));
            target.atb=0;
            this._addFloater(this._partyPosOf(target), 'REVIVE!', '#ffffaa');
          }
        } else if (item.type === 'damage') {
          Audio.sfx('magic');
          this.enemies.filter(e=>!e.dead).forEach(e=>{
            const dmg = item.power||50;
            this._applyDmg(e, dmg, '#ff8800'); e.shaking=true; e.shakTimer=0.3;
          });
        }
        this.pendingItem=null;
      }, delay:0.6},
      {fn:()=>{ user.atb=0; user.defending=false; }, delay:0.1},
    ];
  }

  _executeDefend(c) {
    c.defending = true;
    c.atb = 0;
    this.state = 'running';
    Audio.sfx('cursor');
  }

  _enemyTurn(enemy) {
    this.state = 'anim';
    this.animTimer = 0.3;

    // Phase 2 check
    if (!enemy.inPhase2 && enemy.phase2Hp && enemy.hp <= enemy.phase2Hp && enemy.phase2Actions) {
      enemy.inPhase2 = true;
      enemy.actions = enemy.phase2Actions;
    }

    // Choose action
    const total = enemy.actions.reduce((s,a)=>s+a.w,0);
    let r = Math.random()*total;
    let chosen = enemy.actions[0];
    for (const a of enemy.actions) { r-=a.w; if(r<=0){chosen=a; break;} }

    const skillId = chosen.skill;
    const sk = SKILLS[skillId];
    const alive = this.party.filter(c=>!c.dead);
    if (!alive.length) { this._checkEnd(); return; }

    // Pick target
    let targets;
    if (sk.target === 'all_allies') targets = alive;    // for enemy, allies = party
    else targets = [alive[randInt(0,alive.length-1)]];

    this.animQueue = [
      {fn:()=>{
        if (sk.type === 'healing') {
          // enemy self-heal
          const heal = Math.min(enemy.maxHp-enemy.hp, sk.power);
          enemy.hp = Math.min(enemy.maxHp, enemy.hp+heal);
          this._addFloater(this._enemyPosOf(enemy), `+${heal}`, '#44ff88');
        } else if (sk.type === 'status') {
          targets.forEach(t=>{
            if(!t.status) { t.status=sk.status; t.statusTimer=sk.status==='stun'?2:999; }
            Audio.sfx('magic');
            this._addFloater(this._partyPosOf(t), sk.status.toUpperCase(), '#ffaa00');
          });
        } else {
          const sfx = sk.type==='magic'?'magic':'hurt';
          targets.forEach(t=>{
            const def = t.defending ? t.def*2 : t.def;
            const stat = sk.type==='magic' ? {atk:enemy.mag, def:t.def*0.5} : {atk:enemy.atk, def};
            const base = Math.max(1, stat.atk - Math.floor(def/2));
            const dmg  = Math.max(1, Math.round(base * sk.power * (0.9+Math.random()*0.2)));
            Audio.sfx(sfx);
            this._applyDmg(t, dmg, sk.type==='magic'?'#aa66ff':'#ff4444');
            t.shaking=true; t.shakTimer=0.3;
          });
        }
      }, delay:0.7},
      {fn:()=>{ enemy.atb=0; }, delay:0.05},
    ];
  }

  _calcDmg(attacker, target, power, type) {
    let atk, def;
    if (type === 'magic') {
      atk = attacker.mag; def = Math.floor(target.def * 0.5);
    } else {
      atk = attacker.atk; def = target.def;
    }
    const base = Math.max(1, atk - Math.floor(def/2));
    return Math.max(1, Math.round(base * power * (0.9 + Math.random()*0.2)));
  }

  _applyDmg(target, dmg, color) {
    target.hp = Math.max(0, target.hp - dmg);
    if (target.hp === 0) {
      target.dead = true;
      target.atb  = 0;
      if (target.status==='stun') target.status=null;
    }
    const pos = this.party.includes(target) ? this._partyPosOf(target) : this._enemyPosOf(target);
    this._addFloater(pos, String(dmg), color);
    spawnHitParticles(this.particles, pos.x+10, pos.y+10, color);
  }

  _addFloater(pos, text, color) {
    this.floaters.push({x:pos.x, y:pos.y, text, color, life:1.2});
  }

  _partyPosOf(c) { return this.partyPos[this.party.indexOf(c)] || {x:200,y:80}; }
  _enemyPosOf(e) { return this.enemyPos[this.enemies.indexOf(e)] || {x:60,y:80}; }
  _posOf(u)      { return this.party.includes(u) ? this._partyPosOf(u) : this._enemyPosOf(u); }

  _checkEnd() {
    const allDead = this.party.every(c=>c.dead);
    const enemyDead = this.enemies.every(e=>e.dead);
    if (allDead) {
      this.resultState = 'defeat';
      this.state = 'result';
      this.resultTimer = 2;
      Audio.sfx('gameover');
    } else if (enemyDead) {
      this._calcRewards();
      this.resultState = 'victory';
      this.state = 'result';
      this.resultTimer = 2.5;
      Audio.sfx('victory');
    }
  }

  _calcRewards() {
    this.expGained  = this.enemies.reduce((s,e)=>s+e.exp,0);
    this.goldGained = this.enemies.reduce((s,e)=>s+e.gold,0);
    State.gold += this.goldGained;
    this.levelUps = [];
    this.party.filter(c=>!c.dead).forEach(c=>{
      c.exp += this.expGained;
      while (c.exp >= c.expNext && c.level < 20) {
        c.exp -= c.expNext;
        levelUp(c);
        this.levelUps.push(c.name);
        Audio.sfx('levelup');
      }
    });
  }

  // ── Draw ────────────────────────────────────
  draw(c2d) {
    drawBattleBg(c2d, this.bg);
    this._drawEnemies(c2d);
    this._drawParty(c2d);
    this._drawUI(c2d);
    this.particles.forEach(p=>p.draw(c2d));
    this._drawFloaters(c2d);
    if (this.state==='result') this._drawResult(c2d);
  }

  _drawEnemies(c2d) {
    this.enemies.forEach((e,i) => {
      if (e.dead) return;
      const {x,y} = this.enemyPos[i];
      c2d.save();
      if (e.shaking) c2d.translate(Math.random()>0.5?2:-2,0);
      if (e.status==='stun') c2d.globalAlpha=0.7;
      const sz = e.boss ? 48 : 32;
      drawBattleSprite(c2d, e.sprite, x, y, this.frame);
      c2d.restore();
      // Name + HP bar
      const bw = e.boss ? 60 : 40;
      drawHpBar(c2d, x, y-10, bw, e.hp, e.maxHp);
      drawText(c2d, e.name, x, y-20, '#fff', 1);
      // Phase 2 indicator
      if (e.inPhase2) drawText(c2d, 'PHASE2', x, y-28, '#ff4400', 1);
      // Status
      if (e.status) drawText(c2d, e.status.toUpperCase(), x, y+34, '#ffaa00', 1);
      // Selection arrow
      if ((this.menuState==='target_enemy') && !e.dead) {
        const alive = this.enemies.filter(e2=>!e2.dead);
        if (alive[this.selTarget]===e) {
          c2d.fillStyle='#ffff00';
          c2d.fillRect(x+10, y-30, 6, 6);
        }
      }
    });
  }

  _drawParty(c2d) {
    this.party.forEach((c,i) => {
      const {x,y} = this.partyPos[i];
      c2d.save();
      if (c.shaking) c2d.translate(Math.random()>0.5?2:-2,0);
      drawBattleSprite(c2d, c.sprite, x, y, this.frame);
      if (c.dead) { c2d.globalAlpha=0.3; c2d.fillStyle='#ff0000'; c2d.fillRect(x,y,32,4); }
      if (c.defending) { c2d.fillStyle='#4488ff44'; c2d.fillRect(x-2,y-2,36,50); }
      c2d.restore();
      // Target indicator
      if (this.menuState==='target_ally') {
        const all = this.pendingItem && ITEMS[this.pendingItem]?.type==='revive'
                    ? this.party : this.party.filter(p=>!p.dead);
        if (all[this.selTarget]===c) {
          c2d.fillStyle='#ffff00'; c2d.fillRect(x+10,y-8,6,6);
        }
      }
    });
  }

  _drawUI(c2d) {
    // Status panel at bottom
    drawBox(c2d, 0, 175, W, 65, '#0d0d1e', '#3355aa');
    this.party.forEach((c,i) => {
      const px = 4 + i*106;
      // ATB glow if full
      if (c === this.activeChar) {
        c2d.fillStyle='#ffffff22'; c2d.fillRect(px-1,176,103,62);
      }
      drawText(c2d, c.name, px, 179, c.dead?'#666':c.color, 1);
      if (c.status) drawText(c2d, c.status.slice(0,3).toUpperCase(), px+40,179,'#ffaa00',1);
      drawText(c2d, `HP`, px, 188, '#aaa', 1);
      drawHpBar(c2d, px+14, 189, 84, c.hp, c.maxHp);
      drawText(c2d, `${c.hp}`, px+14, 194, '#fff', 1);
      drawText(c2d, `MP`, px, 198, '#6af', 1);
      drawMpBar(c2d, px+14, 199, 84, c.mp, c.maxMp);
      drawText(c2d, `${c.mp}`, px+14, 204, '#8cf', 1);
      drawText(c2d, 'ATB', px, 208, '#886', 1);
      drawAtbBar(c2d, px+18, 209, 80, c.atb);
    });

    // Command menu
    if (this.state === 'char_turn') {
      const c = this.activeChar;
      if (!c) return;
      drawBox(c2d, 0, 140, W, 34, '#1a1a2e', '#5588cc');
      drawText(c2d, `${c.name}'S TURN`, 4, 143, c.color, 1);

      if (this.menuState === 'main') {
        const opts = ['Attack','Skill','Item','Defend'];
        opts.forEach((o,i) => {
          const selected = i===this.selMenu;
          const col = selected ? '#ffff00' : '#cccccc';
          if (selected) { c2d.fillStyle='#ffffff22'; c2d.fillRect(4+i*78,152,74,12); }
          drawText(c2d, o, 8+i*78, 154, col, 1);
        });
      } else if (this.menuState === 'skill') {
        const skills = c.skills.map(id=>SKILLS[id]);
        drawText(c2d, 'SELECT SKILL:', 4, 152, '#aaa', 1);
        skills.forEach((sk,i) => {
          const sel = i===this.selSkill;
          const canUse = c.mp >= sk.mp;
          const col = sel ? '#ffff00' : canUse ? '#cccccc' : '#666';
          if (sel) { c2d.fillStyle='#ffffff22'; c2d.fillRect(4+i*78,152,74,12); }
          drawText(c2d, `${sk.name}(${sk.mp})`, 8+i*78, 154, col, 1);
        });
      } else if (this.menuState === 'item') {
        const inv = State.inventory.filter(s=>s.count>0);
        drawText(c2d, 'SELECT ITEM:', 4, 152, '#aaa', 1);
        if (!inv.length) { drawText(c2d, 'EMPTY', 80, 154, '#666', 1); }
        inv.slice(0,4).forEach((slot,i) => {
          const sel = i===this.selItem;
          const col = sel ? '#ffff00' : '#cccccc';
          if (sel) { c2d.fillStyle='#ffffff22'; c2d.fillRect(4+i*78,152,74,12); }
          drawText(c2d, `${ITEMS[slot.id].name}x${slot.count}`, 8+i*78, 154, col, 1);
        });
      } else if (this.menuState === 'target_enemy') {
        drawText(c2d, 'SELECT TARGET:', 4, 154, '#ffcc00', 1);
      } else if (this.menuState === 'target_ally') {
        drawText(c2d, 'SELECT ALLY:', 4, 154, '#ffcc00', 1);
      }
    }
  }

  _drawFloaters(c2d) {
    this.floaters.forEach(f => {
      c2d.save();
      c2d.globalAlpha = Math.min(1, f.life);
      drawTextShadow(c2d, f.text, Math.round(f.x), Math.round(f.y), f.color, '#000', 1);
      c2d.restore();
    });
  }

  _drawResult(c2d) {
    const a = Math.min(1, (2.5-this.resultTimer)/0.4);
    c2d.fillStyle = `rgba(0,0,0,${a*0.7})`;
    c2d.fillRect(0,0,W,H);
    if (this.resultState === 'victory') {
      drawTextShadow(c2d, 'VICTORY!', 100, 80, '#ffdd00', '#000', 2);
      drawText(c2d, `EXP: +${this.expGained}`, 110, 105, '#aaffaa', 1);
      drawText(c2d, `GOLD: +${this.goldGained}G`, 110, 115, '#ffcc44', 1);
      if (this.levelUps.length) {
        drawText(c2d, `LEVEL UP: ${this.levelUps.join(', ')}`, 80, 128, '#ffffaa', 1);
      }
    } else {
      drawTextShadow(c2d, 'DEFEATED...', 90, 90, '#ff4444', '#000', 2);
      drawText(c2d, 'Your journey ends here.', 74, 118, '#888', 1);
    }
  }
}
