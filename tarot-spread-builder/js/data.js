// ==============================================================================
// Dados do Tarot — UIDs, Layouts, Baralhos, Settings e Spreads Customizados
// ==============================================================================

// ── Card UIDs ──────────────────────────────────────────────────────────────────

const UIDS_BASE = [
    "Cups01","Cups02","Cups03","Cups04","Cups05","Cups06","Cups07",
    "Cups08","Cups09","Cups10","Cups11","Cups12","Cups13","Cups14",
    "MA-01-Mago","MA-02-Papisa","MA-03-Imperatriz","MA-04-Imperador",
    "MA-05-Papa","MA-06-Amantes","MA-07-Carruagem","MA-08-Justica",
    "MA-09-Eremita","MA-10-Roda","MA-11-Forca","MA-12-Enforcado",
    "MA-13-Morte","MA-14-Temperanca","MA-15-Diabo","MA-16-Torre",
    "MA-17-Estrela","MA-18-Lua","MA-19-Sol","MA-20-Julgamento",
    "MA-21-Mundo","MA-22-Louco",
    "Pents01","Pents02","Pents03","Pents04","Pents05","Pents06",
    "Pents07","Pents08","Pents09","Pents10","Pents11","Pents12",
    "Pents13","Pents14",
    "Swords01","Swords02","Swords03","Swords04","Swords05","Swords06",
    "Swords07","Swords08","Swords09","Swords10","Swords11","Swords12",
    "Swords13","Swords14",
    "Wands01","Wands02","Wands03","Wands04","Wands05","Wands06",
    "Wands07","Wands08","Wands09","Wands10","Wands11","Wands12",
    "Wands13","Wands14"
];

const UIDS_MAJORS = UIDS_BASE.filter(uid => uid.startsWith("MA-"));

const UIDS_LENORMAND = [
    "01-cavaleiro","02-trevo","03-navio","04-casa","05-arvore",
    "06-nuvens","07-cobra","08-caixao","09-buque","10-foice",
    "11-chicote","12-passaros","13-crianca","14-raposa","15-urso",
    "16-estrelas","17-cegonha","18-cachorro","19-torre","20-jardim",
    "21-montanha","22-encruzilhadas","23-ratos","24-coracao","25-anel",
    "26-livro","27-carta","28-homem","29-mulher","30-lirios",
    "31-sol","32-lua","33-chave","34-peixes","35-ancora","36-cruz"
];

// Multiplicadores de baralho
const NUM_DECKS_FULL      = 14;
const NUM_DECKS_MAJORS    = 50;
const NUM_DECKS_LENORMAND = 30;

// ── Baralhos padrão ────────────────────────────────────────────────────────────

const DEFAULT_DECKS = {
    tdm: {
        id: 'tdm', nome: 'Marselha', nomeCompleto: 'Tarot de Marselha',
        dir: 'decks/img-tdm', ext: '.jpg',
        uidsBase: UIDS_BASE, uidsMajors: UIDS_MAJORS,
        hasMajors: true, color: '#c9a84c'
    },
    rws: {
        id: 'rws', nome: 'RWS', nomeCompleto: 'Rider-Waite-Smith',
        dir: 'decks/img-rws', ext: '.jpg',
        uidsBase: UIDS_BASE, uidsMajors: UIDS_MAJORS,
        hasMajors: true, color: '#9b6fd0'
    },
    len: {
        id: 'len', nome: 'Lenormand', nomeCompleto: 'Lenormand',
        dir: 'decks/img-len', ext: '.jpg',
        uidsBase: UIDS_LENORMAND, uidsMajors: null,
        hasMajors: false, color: '#d06080'
    }
};

// ── Layouts padrão ─────────────────────────────────────────────────────────────

const DEFAULT_LAYOUTS = {
    1:  { nome: "Linha de 7",               estrutura: [[5,3,1,0,2,4,6]] },
    2:  { nome: "Templo de Afrodite (7)",   estrutura: [[1,null,4],[2,0,5],[3,null,6]] },
    3:  { nome: "Três Cartas",              estrutura: [[0,1,2]] },
    4:  { nome: "Dúvida (4)",               estrutura: [[3],[1,2],[0]] },
    5:  { nome: "Cruz Simples (5)",         estrutura: [[4],[0,1,2],[3]] },
    6:  { nome: "Escolha A/B (7)",          estrutura: [[0],[1,null,null,null,2],[3,4,null,null,5,6]] },
    7:  { nome: "Realização (10)",          estrutura: [[9],[7,8],[5,6,null,3,4],[2],[0,null,1]] },
    8:  { nome: "Tempo (7)",                estrutura: [[4],[0,null,null,null,1],[2,3],[5,null,null,6]] },
    9:  { nome: "Cruz Parisse (5)",         estrutura: [[2],[0,4,1],[3]] },
    10: { nome: "Sitships (8)",             estrutura: [[7],[5,6],[4,null,3],[2],[0,null,null,1]] },
    11: { nome: "Templo Duplo (11)",        estrutura: [[1,null,4,null,7],[2,0,5,10,8],[3,null,6,null,9]] },
    12: { nome: "Tableau 6×6 (36)",        estrutura: [[0,1,2,3,4,5,6,7,8],[9,10,11,12,13,14,15,16,17],[18,19,20,21,22,23,24,25,26],[27,28,29,30,31,32,33,34,35]] },
    13: { nome: "Oito Cavalos (8)",         estrutura: [[6,null,null,null,7],[4,null,0,null,5],[2,1,3]] },
    14: { nome: "Mundo (15)",              estrutura: [[6,7,8,null,null,3,4,5],[0,1,2],[9,10,11,null,null,12,13,14]] },
    15: { nome: "Linha Dupla (7)",          estrutura: [[0],[1,3,5],[2,4,6]] },
    16: { nome: "Linha Dupla Casal (8)",    estrutura: [[0,1],[2,4,6],[3,5,7]] },
    17: { nome: "Cruz c/ Sobreposição (6)", estrutura: [[4],[0,[1,2,184,128],3],[5]] },
    18: { nome: "Triângulo Central (5)",   estrutura: [[0],[1,2,3,4]] },
    19: { nome: "4×4 (16)",               estrutura: [[0,1,2,3],[4,5,6,7],[8,9,10,11],[12,13,14,15]] },
    20: { nome: "5×5 (25)",               estrutura: [[0,1,2,3,4],[5,6,7,8,9],[10,11,12,13,14],[15,16,17,18,19],[20,21,22,23,24]] }
};

// Aliases para compatibilidade
const DECKS   = DEFAULT_DECKS;
const LAYOUTS = DEFAULT_LAYOUTS;

// ── Persistência ───────────────────────────────────────────────────────────────

const SETTINGS_KEY       = 'arcanum_settings';
const CUSTOM_SPREADS_KEY = 'arcanum_spreads';
const CUSTOM_DECKS_KEY   = 'arcanum_decks';

const DEFAULT_SETTINGS = {
    activeDeck:       'tdm',
    tipoArcanos:      'full',
    invertidas:       true,
    invertidasPct:    30,
    transpBackground: false,
    exifEnabled:      true,
    exifAuthor:       'xtr',
    favoriteSpreads:  [5],
    deckMultiplier:   0
};

function loadSettings() {
    try {
        const raw = localStorage.getItem(SETTINGS_KEY);
        return raw ? Object.assign({}, DEFAULT_SETTINGS, JSON.parse(raw)) : Object.assign({}, DEFAULT_SETTINGS);
    } catch(e) { return Object.assign({}, DEFAULT_SETTINGS); }
}

function saveSettings(settings) {
    try { localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings)); } catch(e) {}
}

// ── Custom Spreads ─────────────────────────────────────────────────────────────

let _customSpreads = null;

function loadCustomSpreads() {
    if (_customSpreads) return _customSpreads;
    try {
        const raw = localStorage.getItem(CUSTOM_SPREADS_KEY);
        _customSpreads = raw ? JSON.parse(raw) : {};
    } catch(e) { _customSpreads = {}; }
    return _customSpreads;
}

function saveCustomSpreads(spreads) {
    _customSpreads = spreads;
    try { localStorage.setItem(CUSTOM_SPREADS_KEY, JSON.stringify(spreads)); } catch(e) {}
}

function getAllLayouts() {
    const custom = loadCustomSpreads();
    return Object.assign({}, DEFAULT_LAYOUTS, custom);
}

function getNextCustomId() {
    const custom = loadCustomSpreads();
    const ids = Object.keys(custom).map(Number).filter(n => n < 0);
    return ids.length > 0 ? Math.min(...ids) - 1 : -1;
}

function saveSpread(id, spread) {
    const custom = loadCustomSpreads();
    custom[id] = spread;
    saveCustomSpreads(custom);
}

function deleteSpread(id) {
    if (Number(id) > 0) return false;
    const custom = loadCustomSpreads();
    delete custom[id];
    saveCustomSpreads(custom);
    return true;
}

function duplicateSpread(id) {
    const all = getAllLayouts();
    const source = all[id];
    if (!source) return null;
    const newId = getNextCustomId();
    const copy = {
        nome:      source.nome + ' (cópia)',
        estrutura: JSON.parse(JSON.stringify(source.estrutura))
    };
    saveSpread(newId, copy);
    return newId;
}

// ── Custom Decks ───────────────────────────────────────────────────────────────

let _customDecks = null;

function loadCustomDecks() {
    if (_customDecks) return _customDecks;
    try {
        const raw = localStorage.getItem(CUSTOM_DECKS_KEY);
        _customDecks = raw ? JSON.parse(raw) : {};
    } catch(e) { _customDecks = {}; }
    return _customDecks;
}

function saveCustomDecks(decks) {
    _customDecks = decks;
    try { localStorage.setItem(CUSTOM_DECKS_KEY, JSON.stringify(decks)); } catch(e) {}
}

function getAllDecks() {
    const custom = loadCustomDecks();
    return Object.assign({}, DEFAULT_DECKS, custom);
}

// ── Utilitários ────────────────────────────────────────────────────────────────

function contarCartasLayout(layoutId) {
    const all = getAllLayouts();
    if (!all[layoutId]) return 0;
    let count = 0;
    for (const row of all[layoutId].estrutura) {
        for (const item of row) {
            if (item === null) continue;
            if (typeof item === 'number') { count++; continue; }
            if (Array.isArray(item)) {
                if (item[0] === 'gap') continue;  // fractional space
                if (item.length === 2) count++;
                else if (item.length === 4) count += 2;
            }
        }
    }
    return count;
}

function temSobreposicao(estrutura) {
    for (const row of estrutura)
        for (const item of row)
            if (Array.isArray(item) && item.length === 4) return true;
    return false;
}
