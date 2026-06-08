'use strict';
// ─────────────────────────────────────────────
//  data.js  –  all game data
// ─────────────────────────────────────────────

// ── Skills ────────────────────────────────────
const SKILLS = {
  attack:      {name:'Attack',    mp:0,  power:1.0, type:'physical', target:'single_enemy', anim:'slash'},
  slash:       {name:'Slash',     mp:0,  power:1.4, type:'physical', target:'single_enemy', anim:'slash'},
  cyclone:     {name:'Cyclone',   mp:4,  power:1.1, type:'physical', target:'all_enemies',  anim:'slash'},
  time_strike: {name:'T-Strike',  mp:6,  power:2.0, type:'physical', target:'single_enemy', anim:'slash'},
  wind_blast:  {name:'Wind',      mp:4,  power:1.3, type:'magic',    target:'all_enemies',  anim:'magic'},
  heal:        {name:'Heal',      mp:5,  power:50,  type:'healing',  target:'single_ally',  anim:'heal'},
  aura:        {name:'Aura',      mp:8,  power:80,  type:'healing',  target:'all_allies',   anim:'heal'},
  laser:       {name:'Laser',     mp:4,  power:1.5, type:'magic',    target:'single_enemy', anim:'magic'},
  robo_beam:   {name:'R-Beam',    mp:8,  power:1.2, type:'magic',    target:'all_enemies',  anim:'magic'},
  overdrive:   {name:'Overdrive', mp:12, power:3.0, type:'physical', target:'single_enemy', anim:'slash'},
  // Enemy skills
  brutal_strike:{name:'Brutal Strike',mp:0, power:2.0, type:'physical', target:'single_ally', anim:'slash'},
  shadow_bolt:  {name:'Shadow Bolt',  mp:0, power:1.6, type:'magic',    target:'single_ally', anim:'magic'},
  shadow_wave:  {name:'Dark Wave',    mp:0, power:1.2, type:'magic',    target:'all_allies',  anim:'magic'},
  devour:       {name:'Devour',       mp:0, power:2.5, type:'physical', target:'single_ally', anim:'slash'},
  poison_spore: {name:'Spore',        mp:0, power:0.5, type:'status',   target:'single_ally', anim:'magic', status:'poison'},
  stun:         {name:'Stun',         mp:0, power:0.3, type:'status',   target:'single_ally', anim:'magic', status:'stun'},
};

// ── Character templates ───────────────────────
const CHAR_TEMPLATES = {
  kael: {
    id:'kael', name:'Kael', sprite:'kael', color:'#4488ff',
    baseStats: {maxHp:65, maxMp:5,  atk:8, def:5, mag:3, spd:7},
    hpGrow:12, mpGrow:2, atkGrow:2, defGrow:1, magGrow:0, spdGrow:0,
    skills: ['slash','cyclone','time_strike'],
    skillLevels: [1,4,7],
    description: 'Young inventor. Strong attacker with time-bending techniques.',
  },
  lyra: {
    id:'lyra', name:'Lyra', sprite:'lyra', color:'#ff88cc',
    baseStats: {maxHp:48, maxMp:25, atk:4, def:4, mag:8, spd:8},
    hpGrow:8,  mpGrow:4, atkGrow:0, defGrow:1, magGrow:2, spdGrow:1,
    skills: ['wind_blast','heal','aura'],
    skillLevels: [1,1,5],
    description: 'Mage with wind and healing magic. Essential for long fights.',
  },
  gorak: {
    id:'gorak', name:'Gorak', sprite:'gorak', color:'#ff6633',
    baseStats: {maxHp:85, maxMp:10, atk:10, def:8, mag:5, spd:5},
    hpGrow:15, mpGrow:2, atkGrow:2, defGrow:2, magGrow:1, spdGrow:0,
    skills: ['laser','robo_beam','overdrive'],
    skillLevels: [1,5,8],
    description: 'Robot from the future. Tanks damage and hits hard.',
  },
};

function makeCharacter(id, level=1) {
  const tmpl = CHAR_TEMPLATES[id];
  const lvl = Math.max(1, level);
  const gain = lvl - 1;
  const stats = {
    maxHp:  tmpl.baseStats.maxHp  + tmpl.hpGrow  * gain,
    maxMp:  tmpl.baseStats.maxMp  + tmpl.mpGrow  * gain,
    atk:    tmpl.baseStats.atk    + tmpl.atkGrow  * gain,
    def:    tmpl.baseStats.def    + tmpl.defGrow  * gain,
    mag:    tmpl.baseStats.mag    + tmpl.magGrow  * gain,
    spd:    tmpl.baseStats.spd    + tmpl.spdGrow  * gain,
  };
  const unlockedSkills = tmpl.skills.filter((_,i) => tmpl.skillLevels[i] <= lvl);
  return {
    id, name: tmpl.name, sprite: tmpl.sprite, color: tmpl.color,
    level: lvl,
    exp: 0,
    expNext: expForLevel(lvl + 1),
    hp: stats.maxHp, mp: stats.maxMp,
    ...stats,
    skills: unlockedSkills,
    allSkills: tmpl.skills,
    skillLevels: tmpl.skillLevels,
    atb: 0,
    status: null, statusTimer: 0,
    defending: false, dead: false,
    description: tmpl.description,
  };
}

function expForLevel(lvl) {
  if (lvl <= 1) return 0;
  return Math.floor(100 * Math.pow(1.6, lvl - 2));
}

function levelUp(char) {
  const tmpl = CHAR_TEMPLATES[char.id];
  char.level++;
  char.maxHp  += tmpl.hpGrow;
  char.maxMp  += tmpl.mpGrow;
  char.atk    += tmpl.atkGrow;
  char.def    += tmpl.defGrow;
  char.mag    += tmpl.magGrow;
  char.spd    += tmpl.spdGrow;
  char.hp = char.maxHp;
  char.mp = char.maxMp;
  char.expNext = expForLevel(char.level + 1);
  // Unlock new skills
  tmpl.skills.forEach((sk, i) => {
    if (tmpl.skillLevels[i] === char.level && !char.skills.includes(sk)) {
      char.skills.push(sk);
    }
  });
}

// ── Items ─────────────────────────────────────
const ITEMS = {
  potion:   {name:'Potion',    desc:'Restore 80 HP',  type:'healing', hp:80,   mp:0,   target:'single_ally'},
  hipotion: {name:'Hi-Potion', desc:'Restore 200 HP', type:'healing', hp:200,  mp:0,   target:'single_ally'},
  ether:    {name:'Ether',     desc:'Restore 15 MP',  type:'healing', hp:0,    mp:15,  target:'single_ally'},
  elixir:   {name:'Elixir',   desc:'Full HP + MP',   type:'healing', hp:9999, mp:9999,target:'single_ally'},
  revive:   {name:'Revive',   desc:'Revive (50% HP)', type:'revive',  hp:0.5,  mp:0,   target:'single_ally'},
  bomb:     {name:'Bomb',     desc:'50 fire dmg all', type:'damage',  power:50,        target:'all_enemies'},
};

function addItem(id, count=1) {
  const slot = State.inventory.find(s => s.id === id);
  if (slot) slot.count += count;
  else State.inventory.push({id, count});
}

function removeItem(id, count=1) {
  const slot = State.inventory.find(s => s.id === id);
  if (!slot) return false;
  slot.count -= count;
  if (slot.count <= 0) State.inventory.splice(State.inventory.indexOf(slot), 1);
  return true;
}

function itemCount(id) {
  const slot = State.inventory.find(s => s.id === id);
  return slot ? slot.count : 0;
}

// ── Enemy templates ───────────────────────────
const ENEMY_TEMPLATES = {
  slime:    {name:'Slime',     hp:30,  mp:0,  atk:5,  def:2,  mag:0, spd:4,  exp:12,  gold:5,  sprite:'slime',
             actions:[{skill:'attack',w:100}], bg:'plains'},
  bat:      {name:'Bat',       hp:22,  mp:0,  atk:6,  def:1,  mag:0, spd:8,  exp:10,  gold:4,  sprite:'bat',
             actions:[{skill:'attack',w:100}], bg:'plains'},
  goblin:   {name:'Goblin',    hp:48,  mp:0,  atk:8,  def:4,  mag:0, spd:5,  exp:18,  gold:8,  sprite:'goblin',
             actions:[{skill:'attack',w:80},{skill:'stun',w:20}], bg:'forest'},
  skeleton: {name:'Skeleton',  hp:62,  mp:0,  atk:10, def:8,  mag:0, spd:4,  exp:25,  gold:12, sprite:'skeleton',
             actions:[{skill:'attack',w:70},{skill:'brutal_strike',w:30}], bg:'cave'},
  mushroom: {name:'Mushroom',  hp:55,  mp:0,  atk:7,  def:5,  mag:4, spd:4,  exp:20,  gold:10, sprite:'mushroom',
             actions:[{skill:'attack',w:60},{skill:'poison_spore',w:40}], bg:'forest'},
  wyvern:   {name:'Wyvern',    hp:90,  mp:0,  atk:14, def:7,  mag:6, spd:6,  exp:40,  gold:20, sprite:'wyvern',
             actions:[{skill:'attack',w:60},{skill:'shadow_bolt',w:40}], bg:'cave'},
  guardian: {name:'Guardian Knight', hp:280, mp:20, atk:16, def:12, mag:5, spd:5, exp:200, gold:80,
             sprite:'guardian', boss:true,
             actions:[{skill:'attack',w:50},{skill:'brutal_strike',w:30},{skill:'stun',w:20}],
             phase2Hp:140, phase2Actions:[{skill:'brutal_strike',w:60},{skill:'stun',w:40}],
             bg:'forest'},
  shadowcore:{name:'Shadowtide Core', hp:520, mp:40, atk:18, def:10, mag:15, spd:6, exp:500, gold:200,
              sprite:'shadowcore', boss:true,
              actions:[{skill:'shadow_bolt',w:40},{skill:'shadow_wave',w:40},{skill:'devour',w:20}],
              phase2Hp:260, phase2Actions:[{skill:'shadow_wave',w:50},{skill:'devour',w:50}],
              bg:'shadow'},
};

function makeEnemy(id, hpMult=1) {
  const t = ENEMY_TEMPLATES[id];
  const hp = Math.round(t.hp * hpMult);
  return {
    id, name: t.name, sprite: t.sprite, boss: !!t.boss, bg: t.bg || 'plains',
    hp, maxHp: hp,
    mp: t.mp || 0, maxMp: t.mp || 0,
    atk: t.atk, def: t.def, mag: t.mag || 0, spd: t.spd,
    exp: t.exp, gold: t.gold || 0,
    actions: t.actions,
    phase2Hp: t.phase2Hp ? Math.round(t.phase2Hp * hpMult) : 0,
    phase2Actions: t.phase2Actions || null,
    inPhase2: false,
    atb: 0,
    status: null, statusTimer: 0,
    dead: false,
    shaking: false, shakTimer: 0,
  };
}

// ── Map tile shorthand decoder ─────────────────
const TILE_CHARS = {
  'G':0,'g':0,' ':0,  // grass
  'W':1,              // water
  'T':2,              // tree
  'M':3,              // mountain
  'F':4,              // stone floor
  '#':5,              // wall
  'P':6,              // path
  'S':7,              // sand
  'D':8,              // dark floor
  'C':9,              // cave wall
  'X':10,             // lava
  'N':11,             // deep water
  'I':12,             // ice
  'H':13,             // house wall
  'R':14,             // roof
  'O':15,             // door
  'L':16,             // shore
  'V':17,             // festival deco
  'Z':18,             // shadow tile
  'E':19,             // light grass
};

function decodeMap(rows) {
  return rows.map(row => row.split('').map(ch => TILE_CHARS[ch] !== undefined ? TILE_CHARS[ch] : 0));
}

// ── Map definitions ───────────────────────────
const MAPS = {};

// VILLAGE (25 × 22)
MAPS.village = {
  id: 'village', name: 'Truce Village',
  music: 'village', encounters: null,
  enter: {x:12, y:18},
  width: 25, height: 22,
  data: decodeMap([
    'TTTTTTTTTTTTTTTTTTTTTTTTT',
    'TGGGGGGGGGGGGGGGGGGGGGGT',  // NOTE: we'll handle width off-by-one gracefully
    'TGGGGGGGGGGPGGGGGGGGGGGT',
    'TGHHHGGGGGGGPGGGGGHHHGGT',
    'TGHOHHGGGGGGPGGGGGHOHHGT',
    'TGHHHGGGGGGGPGGGGGHHHGGT',
    'TGGGGGGGGGGGPGGGGGGGGGT',
    'TGGGPPPPPPPPPPPPPGGGGGGT',
    'TGGGPGGGGGGGGGGGPGGGGGt',
    'TGGGPGGGGGGGGGGGPGGGGGt',
    'TGGGPPPPPPPPPPPPPGGGGGt',
    'TGGGGGGGGGGGPGGGGGGGGGT',
    'TGHHHGGGGGGGPGGGGGHHHGGT',
    'TGHOHHGGGGGGPGGGGGHOHHGT',
    'TGHHHGGGGGGGPGGGGGHHHGGT',
    'TGGGGGGGGGGGPGGGGGGGGGT',
    'TPPPPPPPPPPPPPPPPPPPPGGT',
    'TGGGGGGGGGGGPGGGGGGGGGT',
    'TGGGGGGGGGGGPGGGGGGGGGT',
    'TGGGGGGGGGGGPGGGGGGGGGT',
    'TGGGGGGGGGGGPGGGGGGGGGT',
    'TTTTTTTTTTTTTTTTTTTTTTTTT',
  ]),
  objects: [
    // NPCs
    {type:'npc', id:'elder',    x:12, y:5,  sprite:'elder',    facing:2, dialog:'elder_1',    condition:null},
    {type:'npc', id:'lyra_npc', x:9,  y:9,  sprite:'lyra_npc', facing:2, dialog:'lyra_join',  condition:'!flag_lyra_joined'},
    {type:'npc', id:'lyra_npc', x:9,  y:9,  sprite:'lyra_npc', facing:2, dialog:'lyra_after', condition:'flag_lyra_joined&&!flag_gorak_joined'},
    {type:'npc', id:'merchant', x:19, y:13, sprite:'merchant', facing:2, dialog:'shop',        condition:null},
    {type:'npc', id:'guard',    x:12, y:2,  sprite:'guard',    facing:2, dialog:'guard_1',     condition:null},
    {type:'npc', id:'villager', x:4,  y:16, sprite:'villager', facing:1, dialog:'vill_1',      condition:null},
    {type:'npc', id:'woman',    x:20, y:8,  sprite:'woman',    facing:3, dialog:'woman_1',     condition:null},
    // Warp exits
    {type:'warp', x:12, y:0,  target:'overworld', tx:12, ty:28, condition:null},
    {type:'warp', x:12, y:1,  target:'overworld', tx:12, ty:28, condition:null},
  ]
};

// OVERWORLD (45 × 35)
MAPS.overworld = {
  id: 'overworld', name: '1000 AD — Overworld',
  music: 'overworld',
  encounters: {rate:0.04, table:[
    {id:'slime',  w:35},
    {id:'bat',    w:35},
    {id:'goblin', w:30},
  ]},
  enter: {x:12, y:29},
  width: 45, height: 35,
  data: decodeMap([
    'MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM',
    'MMMMMMMTTTTTTTTTTTTMMMMMMMMMMMMMMMMMMMMMMMMM',
    'MMTTTTTTGGGGGGGGGGTTTTMMMMMMMMMMMMMMMMMMMMMM',
    'MTTGGGGGGGGGGGGGGGGGGGTTMMMMMMMMMMMMMMMMMMTM',
    'MTGGGGGGGGGGGGGGGGGGGGGTMMMMMMMMMMMMMTTTTTM',
    'MTGGGGGGGGWWWWWGGGGGGGGTMMMMMMMMMMMTTGGGTTM',
    'MTGGGGGGGWWWWWWWGGGGGGGTMMMMMMMMMMTTGGGGGTM',
    'MTGGGGGGWWWWWWWWWGGGGGGTMMMMMMMMMTTGGGGGGTM',
    'MTGGGGGGGGWWWWWGGGGGGGGTMMMMMMMMTGGGGVVVGTM',
    'MTGGGGGGGGGGGGGGGGGGGGGGTMMMMMMTGGGVVVVVGTM',
    'MTGGGGGGGGGGGGGGGGGGGGGGGTMMMTTGGGVVVVVGGTM',
    'MTTGGGGGGGGGGGGGGGGGGGGGGGTTTTGGGGGGGGGGGMM',
    'MMTTGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGMMM',
    'MMMTTTGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGTTMMM',
    'MMMMTTTGGGGGGGGGGGGGGGGGGGGGGGGGGGGGTTMMMMM',
    'MMMMMTTTGGGGGGGGGGGGGGGGGGGGGGGGGGTTMMMMMMM',
    'MMMMMMTTGGGGGGGGGGGGGGGGGGGGGGGGGTTMMMMMMMM',
    'MMMMMMMTTGGGGGGGGGGGGGGGGGGGGGGGTTMMMMMMMMM',
    'MMMMMMMMTTTGGGGGGGGGGGGGGGGGGGTTMMMMMMMMMM',
    'MMMMMMMMMTTTGGGGGGGGGGGGGGGGGTTMMMMMMMMMMM',
    'MMMMMMMMMMTTTTGGGGGGGGGGGGGTTTTMMMMMMMMMMMM',
    'MMMMMMMMMMMMMTTTTTGGGGGGTTTTTMMMMMMMMMMMMM',
    'MMMMMMMMMMMMMMMMTTTTTTTTTTTMMMMMMMMMMMMMMMM',
    'MMMMMMMMMMMMMMMMMMMTTTTTTMMMMMMMMMMMMMMMMMM',
    'MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM',
    'MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM',
    'MGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGM',
    'MGPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPGGM',
    'MGPPGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGPGGM',
    'MGPPGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGPGGGM',
    'MGPPGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGPGGGGM',
    'MGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGM',
    'MGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGM',
    'MGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGM',
    'MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM',
  ]),
  objects: [
    // Village entrance
    {type:'warp', x:12, y:29, target:'village', tx:12, ty:19, condition:null},
    {type:'warp', x:12, y:30, target:'village', tx:12, ty:19, condition:null},
    // Cave entrance (NW area – encoded as event trigger)
    {type:'warp', x:5,  y:4,  target:'cave',    tx:10, ty:27, condition:null},
    {type:'warp', x:5,  y:3,  target:'cave',    tx:10, ty:27, condition:null},
    // Festival site (NE area, triggers cutscene)
    {type:'event', x:20, y:9, event:'festival_cutscene', condition:'flag_lyra_joined&&!flag_festival_done'},
    {type:'sign',  x:20, y:10, text:'MILLENNIUM FESTIVAL GROUNDS'},
    // Forest entrance (leads directly into overworld combat zone – just an encounter boost marker)
    {type:'sign',  x:5,  y:4,  text:'ANCIENT CAVE  \nEnter at your own risk!'},
  ]
};

// CAVE (20 × 30)
MAPS.cave = {
  id: 'cave', name: 'Ancient Cave',
  music: 'dungeon',
  encounters: {rate:0.06, table:[
    {id:'goblin',   w:25},
    {id:'skeleton', w:40},
    {id:'mushroom', w:20},
    {id:'wyvern',   w:15},
  ]},
  enter: {x:10, y:27},
  width: 20, height: 30,
  data: decodeMap([
    'CCCCCCCCCCCCCCCCCCCC',
    'CCCCCCCCCCCCCCCCCCCC',
    'CCDDDDDDDDDDDDDDCCCC',
    'CCDCCCCCCCCCCCDDCCCC',
    'CCDCCCCCCCCCCCDDCCCC',
    'CCDDDDDCCCDDDDDDCCCC',
    'CCCCCDDDDDDDCCCCCCC',
    'CCCCCDDDDDDDDCCCCCC',
    'CCCDDDDDDDDDDDDCCCC',
    'CCDDDCCCCCCCDDDDCCC',
    'CCDDDCCCCCCCDDDDCCC',
    'CCDDDDDDDDDDDDDDCCC',
    'CCCCCDDDDDDDDCCCCC',
    'CCCCCCDDDDDDDDCCCC',
    'CCCDDDDDDDDDDDDCCC',
    'CCDDDCCCCCDDDDDDCC',
    'CCDDDCCCCCDDDDDDCC',
    'CCDDDDDDDDDDDDDDCC',
    'CCCCCCCCCCCCCCCCC',
    'CCCCCCCDDDDDCCCCC',
    'CCCCCDDDDDDDDCCCC',
    'CCCDDDDDDDDDDDCCC',
    'CCCDDDDDDDDDDDDCC',
    'CCDDDDDDDDDDDDCCC',
    'CCDDDDDDDDDDDDCCC',
    'CCDDDDDDDDDDDDDCC',
    'CCDDDDDDDDDDDDDCC',
    'CCDDDDDDDDDDDDDCC',
    'CCDDDDDDDDDDDDDCC',
    'CCCCCCCCCCCCCCCCCC',
  ]),
  objects: [
    // Entrance warp back to overworld
    {type:'warp', x:10, y:28, target:'overworld', tx:5, ty:5, condition:null},
    {type:'warp', x:10, y:29, target:'overworld', tx:5, ty:5, condition:null},
    // Chests
    {type:'chest', x:5,  y:10, item:'hipotion', count:2},
    {type:'chest', x:14, y:10, item:'ether',    count:2},
    {type:'chest', x:10, y:15, item:'elixir',   count:1},
    // Gorak encounter (joins party)
    {type:'event', x:10, y:5, event:'gorak_join', condition:'!flag_gorak_joined'},
    // Boss trigger
    {type:'event', x:10, y:2, event:'boss_shadowcore', condition:'flag_gorak_joined&&!flag_boss_done'},
    {type:'sign',  x:10, y:3, text:'A dark force emanates from deep within...'},
  ]
};

// ── Dialog data ───────────────────────────────
const DIALOGS = {
  elder_1: [
    {speaker:'Elder Janus', text:"Kael! You've finally left your workshop. Today is the Millennium Festival!"},
    {speaker:'Elder Janus', text:"A thousand years of peace in Guardia. We should celebrate... yet something feels wrong."},
    {speaker:'Elder Janus', text:"The pendant Lyra carries — it's been glowing strangely. Find her in the square, quickly."},
  ],
  elder_2: [
    {speaker:'Elder Janus', text:"Whatever that gate was... it means the timeline is in danger."},
    {speaker:'Elder Janus', text:"If this robot speaks truth, there's no time to waste. Investigate the Ancient Cave to the northwest."},
  ],
  lyra_join: [
    {speaker:'Lyra', text:"Kael! There you are. My pendant started glowing this morning — I can't explain it."},
    {speaker:'Lyra', text:"I feel like something is calling me northward. Like a pull..."},
    {speaker:'Kael',  text:"Let's check out the festival grounds. Maybe Elder Janus knows something."},
    {speaker:'Lyra', text:"Right. Lead the way!"},
    {type:'join', char:'lyra'},
    {speaker:'',   text:"Lyra has joined the party!"},
  ],
  lyra_after: [
    {speaker:'Lyra', text:"The festival is north on the world map. My pendant keeps reacting..."},
  ],
  merchant: [
    {speaker:'Merchant', text:"Finest goods in Guardia! Can I interest you in something?"},
    {type:'shop'},
  ],
  guard_1: [
    {speaker:'Guard', text:"The path north leads to the festival grounds. Stay on the road — strange things have been seen in the fields."},
  ],
  vill_1: [
    {speaker:'Villager', text:"Can you believe it? A thousand years of Guardia! My grandfather's grandfather was alive for the last festival."},
  ],
  woman_1: [
    {speaker:'Woman', text:"Have you heard? Someone spotted lights over the hills last night. Shooting stars, maybe. Or something else..."},
  ],
  festival_cutscene: [
    {speaker:'', text:"The festival grounds — decorated banners hang in the breeze."},
    {speaker:'Lyra', text:"Kael! The pendant — it's reacting to something HERE."},
    {speaker:'', text:"A swirling vortex tears open in the air before you. Purple light floods the clearing."},
    {speaker:'???', text:"Systems... online. Location: confirmed. Year: 1000 AD."},
    {speaker:'Gorak', text:"I am GORAK. Combat unit, year 2300 AD. I have been searching for this convergence point."},
    {speaker:'Kael',  text:"A robot? From the future?!"},
    {speaker:'Gorak', text:"The Shadowtide is a temporal parasite. It consumes eras, erasing them from history. It originates from beneath this land."},
    {speaker:'Lyra', text:"The pendant is responding to it! It's always been keyed to temporal energy..."},
    {speaker:'Gorak', text:"In my era — nothing remains. No Guardia. No humanity. Only shadow."},
    {speaker:'', text:"The gate pulses violently. A dark shape tears through—"},
    {speaker:'Guardian Knight', text:"HALT. None shall track the Shadowtide and live."},
    {type:'battle', enemies:['guardian'], bg:'forest', music:'boss'},
    // Post-battle — these run after victory:
    {speaker:'Kael',  text:"What WAS that thing?"},
    {speaker:'Gorak', text:"A Shadowtide Vanguard. Where there is one, the Core is near."},
    {speaker:'Gorak', text:"I have traced its signal. The Shadowtide Core lies beneath — in the cave to the northwest."},
    {speaker:'Lyra', text:"Then we go there. All three of us."},
    {type:'join', char:'gorak'},
    {speaker:'', text:"Gorak has joined the party! Head northwest to the Ancient Cave."},
  ],
  gorak_join: [
    {speaker:'Gorak', text:"...You have come to the cave. Then you are already hunting the Shadowtide."},
    {speaker:'Gorak', text:"I have been waiting. Analyzing. The Core is deeper within."},
    {speaker:'Kael',  text:"Then you're with us."},
    {speaker:'Gorak', text:"Affirmative. I was built to destroy it."},
    {type:'join', char:'gorak'},
    {speaker:'', text:"Gorak has joined the party!"},
  ],
  boss_shadowcore: [
    {speaker:'', text:"Deep in the cave, the walls pulse with violet light."},
    {speaker:'Lyra', text:"I can feel it — the source of everything. It's right here."},
    {speaker:'', text:"The darkness coalesces into a massive crystalline form, eyes burning red."},
    {speaker:'Shadowtide Core', text:"YOU ARE TOO LATE. THE CONSUMPTION HAS BEGUN."},
    {speaker:'Gorak', text:"Negative. We are exactly in time."},
    {type:'battle', enemies:['shadowcore'], bg:'shadow', music:'boss'},
    // Post-battle ending sequence:
    {speaker:'', text:"The Shadowtide Core shatters. Light floods the cave."},
    {speaker:'Lyra', text:"The pendant... it's going dark. Whatever it was responding to — it's gone."},
    {speaker:'Gorak', text:"Core: destroyed. Timeline: stabilized. My mission is complete."},
    {speaker:'Kael',  text:"Does that mean you can go home?"},
    {speaker:'Gorak', text:"...Unknown. The gate may not open again. But perhaps that is acceptable."},
    {speaker:'Lyra', text:"Then you stay here. With us."},
    {speaker:'Gorak', text:"...Affirmative."},
    {speaker:'', text:"The three of you emerge from the cave as dawn breaks over Guardia."},
    {speaker:'', text:"The Shadowtide is defeated. The timeline is safe. For now."},
    {type:'ending'},
  ],
  shop_items: {
    potion:   {name:'Potion',    price:50},
    hipotion: {name:'Hi-Potion', price:150},
    ether:    {name:'Ether',     price:100},
    revive:   {name:'Revive',    price:200},
    bomb:     {name:'Bomb',      price:80},
  },
};
