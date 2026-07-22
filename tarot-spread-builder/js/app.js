// ==============================================================================
// Arcanum — Controlador Principal
// ==============================================================================

// ── Estado global ──────────────────────────────────────────────────────────────

let settings = {};

let state = {
    selectedLayoutId: null,
    baralho:          [],        // Array<{uid, inverted, index}>
    selecionados:     [],        // índices 1-based escolhidos
    numNecessarias:   0,
    seedCount:        0,
    lastCanvas:       null       // canvas mais recente para exportação
};

// ── Navegação ──────────────────────────────────────────────────────────────────

const NAV_SCREENS  = ['screen-home', 'screen-spreads', 'screen-settings'];
const PLAY_SCREENS = ['screen-play', 'screen-result'];

function showScreen(id, skipAnim) {
    const all = document.querySelectorAll('.screen');
    const target = document.getElementById(id);
    const nav = document.getElementById('bottom-nav');

    if (skipAnim) {
        all.forEach(s => { s.classList.remove('active','screen-exit'); s.style.display = 'none'; });
        target.style.display = 'flex';
        target.classList.add('active');
    } else {
        const cur = document.querySelector('.screen.active');
        if (cur && cur !== target) {
            cur.classList.add('screen-exit');
            cur.classList.remove('active');
            setTimeout(() => { cur.classList.remove('screen-exit'); cur.style.display = 'none'; }, 300);
        }
        target.style.display = 'flex';
        void target.offsetHeight;
        target.classList.add('active');
    }

    // Show/hide bottom nav
    if (NAV_SCREENS.includes(id)) {
        nav.style.display = 'flex';
        // Highlight active nav item
        document.querySelectorAll('.nav-item').forEach(n => {
            n.classList.toggle('active', n.dataset.screen === id);
        });
    } else {
        nav.style.display = 'none';
    }

    // Update toast position
    const toast = document.getElementById('toast');
    toast.classList.toggle('no-nav', PLAY_SCREENS.includes(id));
}

function navTo(screenId) {
    showScreen(screenId);
    if (screenId === 'screen-home')     renderHome();
    if (screenId === 'screen-spreads')  renderSpreadsList();
    if (screenId === 'screen-settings') renderSettings();
}

function goBack(targetScreen) {
    showScreen(targetScreen);
    if (targetScreen === 'screen-home') renderHome();
}

// ── Inicialização ──────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    settings = loadSettings();
    renderHome();
    showScreen('screen-home', true);
    initSlider();
    initExifAuthorField();
});

// ══════════════════════════════════════════
// HOME
// ══════════════════════════════════════════

function renderHome() {
    settings = loadSettings();
    renderDeckStrip();
    renderSpreadGrid();
    updateQuickConfig();
}

// ── Deck strip ─────────────────────────────────────────────────────────────────

function renderDeckStrip() {
    const strip = document.getElementById('home-deck-strip');
    const allDecks = getAllDecks();
    strip.innerHTML = '';

    for (const [id, deck] of Object.entries(allDecks)) {
        const pill = document.createElement('div');
        pill.className = 'deck-pill' + (id === settings.activeDeck ? ' active' : '');
        pill.innerHTML = `<div class="deck-dot" style="background:${deck.color||'#c9a84c'}"></div>
                          <span class="deck-pill-name">${deck.nome}</span>`;
        pill.onclick = () => setActiveDeck(id);
        strip.appendChild(pill);
    }

    // "+" pill
    const addPill = document.createElement('div');
    addPill.className = 'deck-pill';
    addPill.innerHTML = `<span class="deck-pill-name" style="color:var(--text-dim)">+ Novo</span>`;
    addPill.style.borderStyle = 'dashed';
    addPill.onclick = () => { navTo('screen-settings'); };
    strip.appendChild(addPill);
}

function setActiveDeck(id) {
    settings.activeDeck = id;
    saveSettings(settings);
    renderDeckStrip();
    updateQuickConfig();
}

// ── Spread grid ────────────────────────────────────────────────────────────────

function renderSpreadGrid() {
    const grid = document.getElementById('home-spread-grid');
    const allLayouts = getAllLayouts();
    grid.innerHTML = '';

    const allEntries = Object.entries(allLayouts);
    const favIds = settings.favoriteSpreads || [];

    // Featured: selected or first favorite or layout 5
    const featId = state.selectedLayoutId
        || (favIds.length > 0 ? favIds[0] : null)
        || 5;

    // Sort: featured first, then others
    const sorted = allEntries.sort(([a],[b]) => {
        const aFeat = Number(a) === Number(featId);
        const bFeat = Number(b) === Number(featId);
        return bFeat - aFeat;
    });

    for (const [id, layout] of sorted) {
        const numId = Number(id);
        const numCartas = contarCartasLayout(numId);
        const isFeat = numId === Number(featId);
        const isFav = favIds.includes(numId);
        const isCustom = numId < 0;
        const hasOverlap = temSobreposicao(layout.estrutura);
        const isSelected = numId === Number(state.selectedLayoutId);

        const card = document.createElement('div');
        card.className = 'spread-card' + (isFeat ? ' featured' : '') + (isSelected ? ' active' : '');

        const stars = isFav ? `<span class="fav-star" title="Favorito">★</span>` : '';
        const ovlpBadge = hasOverlap ? `<span class="spread-badge ovlp">sobrepost.</span>` : '';
        const customBadge = isCustom ? `<span class="spread-badge" style="background:rgba(107,63,160,.15);color:var(--violet-light);border-color:rgba(107,63,160,.3)">custom</span>` : '';
        const selStyle = isSelected ? 'border-color:var(--gold);box-shadow:0 0 14px var(--gold-glow-s)' : '';

        card.innerHTML = `
            <div class="spread-card-top">
                <div class="spread-card-name">${layout.nome}</div>
                ${stars}
            </div>
            <div class="spread-card-meta">
                <span class="spread-badge">${numCartas} cartas</span>
                ${ovlpBadge}${customBadge}
            </div>
            ${buildSpreadPreview(layout.estrutura)}`;
        if (selStyle) card.style.cssText = selStyle;

        card.onclick = () => selectSpread(numId);
        grid.appendChild(card);
    }

    // Add card
    const addCard = document.createElement('div');
    addCard.className = 'spread-card spread-card-add';
    addCard.innerHTML = `<div class="add-icon">+</div><div class="spread-card-name">Novo Spread</div>`;
    addCard.onclick = () => { navTo('screen-spreads'); };
    grid.appendChild(addCard);
}

function selectSpread(id) {
    state.selectedLayoutId = id;
    renderSpreadGrid();
    const btn = document.getElementById('btn-start');
    btn.disabled = false;
}

// ── Quick config ───────────────────────────────────────────────────────────────

function updateQuickConfig() {
    const toggleInv = document.getElementById('toggle-invertidas');
    toggleInv.classList.toggle('off', !settings.invertidas);

    const arcDeck = getAllDecks()[settings.activeDeck];
    const majorsOk = arcDeck && arcDeck.hasMajors;
    const arcVal = (!majorsOk || settings.tipoArcanos === 'full') ? '78' : '22';
    document.getElementById('qc-arcanos-val').textContent = arcVal;

    // Disable arcanos tile if deck has no majors
    const qcArcanos = document.getElementById('qc-arcanos');
    qcArcanos.style.opacity = majorsOk ? '1' : '.4';
    qcArcanos.style.pointerEvents = majorsOk ? '' : 'none';
}

function toggleInvertidas() {
    settings.invertidas = !settings.invertidas;
    saveSettings(settings);
    updateQuickConfig();
    // sync settings screen toggle
    syncSettingsToggles();
}

function cycleArcanos() {
    settings.tipoArcanos = settings.tipoArcanos === 'full' ? 'majors' : 'full';
    saveSettings(settings);
    updateQuickConfig();
    syncSettingsToggles();
}

// ── Start reading ──────────────────────────────────────────────────────────────

function startReading() {
    if (!state.selectedLayoutId) { showToast('Escolha um spread primeiro'); return; }

    state.numNecessarias = contarCartasLayout(state.selectedLayoutId);
    state.selecionados   = [];
    state.seedCount      = 1;

    generateSeed();
    showScreen('screen-play');

    const layout = getAllLayouts()[state.selectedLayoutId];
    document.getElementById('play-title').textContent   = layout.nome;
    document.getElementById('play-subtitle').textContent =
        `${getAllDecks()[settings.activeDeck].nome} · ${settings.tipoArcanos === 'majors' ? '22 Arcanos' : '78 Arcanos'} · ${settings.invertidas ? settings.invertidasPct + '% inv.' : 'sem inversão'}`;
    document.getElementById('cards-needed').textContent = state.numNecessarias;
    updateSelectionUI();
}

// ══════════════════════════════════════════
// PLAY SCREEN
// ══════════════════════════════════════════

function generateSeed() {
    const pct = settings.invertidas ? settings.invertidasPct : 0;
    state.baralho     = prepararBaralho(settings.activeDeck, settings.tipoArcanos, pct, settings.deckMultiplier);
    state.selecionados = [];

    document.getElementById('play-seed-badge').textContent = `SEED #${state.seedCount}`;
    document.getElementById('seed-total-badge').textContent = `${state.baralho.length} cartas`;

    renderSeedList();
    updateSelectionUI();
}

function renderSeedList() {
    const list = document.getElementById('seed-list');
    list.innerHTML = '';

    for (const card of state.baralho) {
        const row = document.createElement('div');
        row.className = 'seed-row' + (state.selecionados.includes(card.index) ? ' selected' : '');
        row.dataset.index = card.index;

        const invLabel = card.inverted ? `<span class="inv-tag">↕ inv</span>` : '';
        const miniCls  = card.inverted ? 'seed-mini inv' : 'seed-mini';
        const selCls   = state.selecionados.includes(card.index) ? ' sel' : '';

        row.innerHTML = `
            <span class="seed-num">${card.index}</span>
            <div class="${miniCls}${selCls}"></div>
            <span class="seed-card-name">${card.uid}${invLabel}</span>
            <div class="seed-check">✓</div>`;
        row.onclick = () => toggleCard(card.index);
        list.appendChild(row);
    }
}

function toggleCard(index) {
    const pos = state.selecionados.indexOf(index);
    if (pos >= 0) {
        state.selecionados.splice(pos, 1);
    } else if (state.selecionados.length < state.numNecessarias) {
        state.selecionados.push(index);
        if (state.selecionados.length === state.numNecessarias) {
            showToast('Todas as cartas selecionadas');
        }
    }
    updateSeedRowVisuals();
    updateSelectionUI();
}

function updateSeedRowVisuals() {
    document.querySelectorAll('.seed-row').forEach(row => {
        const idx = Number(row.dataset.index);
        const sel = state.selecionados.includes(idx);
        row.classList.toggle('selected', sel);
        const mini = row.querySelector('.seed-mini');
        if (mini) mini.classList.toggle('sel', sel);
    });
}

function applyNumberInput() {
    const input = document.getElementById('card-number-input');
    const text = input.value.trim();
    if (!text) return;

    const nums = text.split(/[\s,;]+/).map(s => parseInt(s)).filter(n => !isNaN(n));
    if (!nums.length) { showToast('Digite números válidos'); return; }

    const max = state.baralho.length;
    const invalidos = nums.filter(n => n < 1 || n > max);
    if (invalidos.length) { showToast(`Fora do intervalo (1–${max}): ${invalidos.join(', ')}`); return; }

    const remaining = state.numNecessarias - state.selecionados.length;
    const toAdd = nums.filter(n => !state.selecionados.includes(n)).slice(0, remaining);

    toAdd.forEach(n => state.selecionados.push(n));
    updateSeedRowVisuals();
    updateSelectionUI();
    input.value = '';

    if (state.selecionados.length === state.numNecessarias) showToast('Todas as cartas selecionadas');
}

function clearSelection() {
    state.selecionados = [];
    document.getElementById('card-number-input').value = '';
    updateSeedRowVisuals();
    updateSelectionUI();
}

function updateSelectionUI() {
    const sel = state.selecionados.length;
    const needed = state.numNecessarias;

    document.getElementById('cards-selected-count').textContent = sel;
    document.getElementById('btn-confirm-cards').disabled = (sel !== needed);
    document.getElementById('number-hint').textContent =
        `${sel} selecionada${sel !== 1 ? 's' : ''} de ${state.baralho.length} disponíveis`;

    // Chips
    const chips = document.getElementById('selected-chips');
    chips.innerHTML = '';
    state.selecionados.forEach(idx => {
        const card = state.baralho.find(c => c.index === idx);
        if (!card) return;
        const chip = document.createElement('span');
        chip.className = 'sel-chip' + (card.inverted ? ' inv-chip' : '');
        chip.textContent = `#${idx} ${card.uid.split('-').slice(-1)[0] || card.uid}`;
        chips.appendChild(chip);
    });
}

function openMultiSeed() {
    const pct = settings.invertidas ? settings.invertidasPct : 0;
    const seeds = [
        prepararBaralho(settings.activeDeck, settings.tipoArcanos, pct, settings.deckMultiplier),
        prepararBaralho(settings.activeDeck, settings.tipoArcanos, pct, settings.deckMultiplier),
        prepararBaralho(settings.activeDeck, settings.tipoArcanos, pct, settings.deckMultiplier)
    ];

    const grid = document.getElementById('multi-seed-grid');
    grid.innerHTML = '';

    seeds.forEach((seed, si) => {
        const col = document.createElement('div');
        col.className = 'seed-column';
        const preview = seed.slice(0, 30);
        col.innerHTML = `
            <div class="seed-col-header">
                <span class="seed-col-title">Seed ${si + 1}</span>
                <span class="seed-col-use" onclick="useSeed(${si})">Usar</span>
            </div>
            <div class="seed-col-list">
                ${preview.map(c => `
                    <div class="seed-col-row">
                        <span class="seed-col-num">${c.index}</span>
                        <div class="seed-col-mini${c.inverted ? ' inv' : ''}"></div>
                        <span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${c.uid}</span>
                    </div>`).join('')}
            </div>`;
        col.dataset.seedIndex = si;
        grid.appendChild(col);
    });

    grid._seeds = seeds;
    openModal('modal-multiseed');
}

function useSeed(si) {
    const grid = document.getElementById('multi-seed-grid');
    state.baralho     = grid._seeds[si];
    state.selecionados = [];
    state.seedCount++;
    document.getElementById('play-seed-badge').textContent = `SEED #${state.seedCount}`;
    document.getElementById('seed-total-badge').textContent = `${state.baralho.length} cartas`;
    renderSeedList();
    updateSelectionUI();
    closeModal('modal-multiseed');
    showToast(`Seed ${si + 1} selecionado`);
}

// ── Confirm & render ───────────────────────────────────────────────────────────

function confirmCards() {
    if (state.selecionados.length !== state.numNecessarias) return;

    const allLayouts = getAllLayouts();
    const allDecks   = getAllDecks();
    const layout     = allLayouts[state.selectedLayoutId];
    const deck       = allDecks[settings.activeDeck];

    const cartasSelecionadas = state.selecionados.map(idx => {
        const c = state.baralho.find(c => c.index === idx);
        return (c.inverted ? 'inv' : '') + c.uid;
    });

    // Populate result screen
    document.getElementById('result-subtitle').textContent =
        `${layout.nome} · ${deck.nome}`;
    document.getElementById('result-info').innerHTML =
        `<strong>${layout.nome}</strong> <span>— ${deck.nome}</span>`;

    const listEl = document.getElementById('result-cards-list');
    listEl.innerHTML = '';
    cartasSelecionadas.forEach((uid, i) => {
        const inv = uid.startsWith('inv');
        const clean = inv ? uid.substring(3) : uid;
        const li = document.createElement('div');
        li.className = 'result-card-item';
        li.innerHTML = `<span class="result-pos">Posição ${i}</span>
                        <span class="result-uid">${clean}</span>
                        ${inv ? '<span class="result-inv">↕ invertida</span>' : ''}`;
        listEl.appendChild(li);
    });

    showScreen('screen-result');
    renderResultCanvas(cartasSelecionadas, deck, layout.estrutura);
}

async function renderResultCanvas(cartas, deck, estrutura) {
    const canvas  = document.getElementById('result-canvas');
    const loading = document.getElementById('result-loading');
    const actions = document.getElementById('result-actions');

    loading.style.display = 'block';
    actions.style.display  = 'none';
    canvas.style.display   = 'none';

    const transp = settings.transpBackground;
    await renderLayout(cartas, deck.dir, deck.ext, estrutura, canvas, transp);

    loading.style.display  = 'none';
    canvas.style.display   = 'block';
    actions.style.display  = 'flex';
    state.lastCanvas = canvas;
}

// ── Result actions ─────────────────────────────────────────────────────────────

function saveImage() {
    const canvas = state.lastCanvas || document.getElementById('result-canvas');
    if (!canvas || !canvas.width) { showToast('Nenhuma imagem gerada'); return; }

    let dataUrl;
    try {
        const transp = settings.transpBackground;
        dataUrl = canvasToDataURL(canvas, 0.95, transp);
    } catch (e) {
        showToast('Erro ao gerar imagem');
        return;
    }

    const transp   = settings.transpBackground;
    const ext      = transp ? 'png' : 'jpg';
    const layout   = getAllLayouts()[state.selectedLayoutId];
    const name     = layout ? layout.nome.replace(/[^a-zA-Z0-9]/g, '-').toLowerCase() : 'tarot';
    const filename = `arcanum-${name}-${Date.now()}.${ext}`;

    if (window.Android && window.Android.saveImage) {
        window.Android.saveImage(dataUrl, filename);
        showToast('Salvo na galeria');
        return;
    }
    const link = document.createElement('a');
    link.download = filename;
    link.href = dataUrl;
    link.click();
    showToast('Imagem baixada');
}

function shareImage() {
    const canvas = state.lastCanvas || document.getElementById('result-canvas');
    if (!canvas || !canvas.width) { showToast('Nenhuma imagem gerada'); return; }

    const transp = settings.transpBackground;
    canvasToBlob(canvas, 0.95, transp).then(async blob => {
        const ext  = transp ? 'png' : 'jpg';
        const mime = transp ? 'image/png' : 'image/jpeg';
        const file = new File([blob], `arcanum-resultado.${ext}`, { type: mime });
        if (navigator.share && navigator.canShare && navigator.canShare({ files: [file] })) {
            try { await navigator.share({ files: [file], title: 'Arcanum — Leitura de Tarot' }); }
            catch(e) { /* cancelled */ }
        } else { saveImage(); }
    });
}

function newReading() {
    state.selecionados = [];
    state.baralho = [];
    showScreen('screen-home');
    renderHome();
}

// ══════════════════════════════════════════
// SPREADS SCREEN
// ══════════════════════════════════════════

function renderSpreadsList() {
    const list = document.getElementById('spreads-list');
    const allLayouts = getAllLayouts();
    const favIds = settings.favoriteSpreads || [];
    list.innerHTML = '';

    for (const [id, layout] of Object.entries(allLayouts)) {
        const numId = Number(id);
        const numCartas  = contarCartasLayout(numId);
        const hasOverlap = temSobreposicao(layout.estrutura);
        const isCustom   = numId < 0;
        const isFav      = favIds.includes(numId);

        const item = document.createElement('div');
        item.className = 'spread-item' + (isFav ? ' is-fav' : '');

        item.innerHTML = `
            <div class="siv">${buildSiv(layout.estrutura)}</div>
            <div class="spread-item-info">
                <div class="spread-item-name">${layout.nome}</div>
                <div class="spread-item-tags">
                    <span class="tag t-cards">${numCartas} cartas</span>
                    ${hasOverlap ? '<span class="tag t-ovlp">sobreposição</span>' : ''}
                    ${isCustom   ? '<span class="tag t-custom">custom</span>' : ''}
                    ${isFav      ? '<span class="tag" style="background:rgba(201,168,76,.08);color:var(--gold-dim);border:1px solid rgba(201,168,76,.2)">★ fav</span>' : ''}
                </div>
            </div>
            <div class="spread-actions">
                <div class="act-btn edit" title="Editar" onclick="openEditor(${numId})">✎</div>
                <div class="act-btn dup"  title="Duplicar" onclick="dupSpread(${numId})">⊕</div>
                ${isCustom ? `<div class="act-btn danger" title="Remover" onclick="removeSpread(${numId})">✕</div>` : ''}
            </div>`;
        list.appendChild(item);
    }
}

function dupSpread(id) {
    const newId = duplicateSpread(id);
    if (newId === null) return;
    showToast('Spread duplicado');
    renderSpreadsList();
    renderSpreadGrid();
}

function removeSpread(id) {
    if (!deleteSpread(id)) { showToast('Não é possível remover um spread padrão'); return; }
    if (state.selectedLayoutId === id) state.selectedLayoutId = null;
    showToast('Spread removido');
    renderSpreadsList();
    renderSpreadGrid();
}

// ── Spread Editor ──────────────────────────────────────────────────────────────

let editorState = {
    id:       null,
    name:     '',
    grid:     [],     // 2D: null | 'card' | 'overlap'
    tool:     'card',
    history:  []
};

function openEditor(id) {
    editorState.id = id;
    editorState.history = [];

    const allLayouts = getAllLayouts();
    if (id !== null && allLayouts[id]) {
        editorState.name = allLayouts[id].nome;
        editorState.grid = layoutToEditorGrid(allLayouts[id].estrutura);
        document.getElementById('editor-title').textContent = 'Editar Spread';
    } else {
        editorState.name = '';
        // Fine grid: 6 half-cells wide = 3 card-widths, 3 rows
        editorState.grid = Array.from({length: 3}, () => Array(6).fill(null));
        document.getElementById('editor-title').textContent = 'Novo Spread';
    }

    document.getElementById('editor-name').value = editorState.name;
    selectTool('card');
    renderEditorGrid();
    document.getElementById('editor-overlay').classList.add('open');
}

function closeEditor() {
    document.getElementById('editor-overlay').classList.remove('open');
}

// Vertical half-step: cards can be nudged up/down by half a card height,
// mirroring the half-cell (half-column) granularity already available horizontally.
const HALF_STEP_DY = CARD_HEIGHT / 2;
const DY_OF        = { card: 0, 'card+': 1, 'card-': -1 };
const MARKER_OF_DY = { '0': 'card', '1': 'card+', '-1': 'card-' };

function isAnchorCell(cell) {
    return cell === 'card' || cell === 'card+' || cell === 'card-' || cell === 'overlap';
}

// Convert layout format → fine grid (each cell = 0.5 card widths; cards span 2 cells)
function layoutToEditorGrid(estrutura) {
    const fineRows = [];
    for (const row of estrutura) {
        const fineRow = [];
        for (const item of row) {
            if (item === null) {
                fineRow.push(null, null);  // full space = 2 half-cells
            } else if (Array.isArray(item) && item[0] === 'gap') {
                const halfCells = Math.max(1, Math.round(item[1] * 2));
                for (let i = 0; i < halfCells; i++) fineRow.push(null);
            } else if (typeof item === 'number') {
                fineRow.push('card', '_');
            } else if (Array.isArray(item) && item.length === 4) {
                fineRow.push('overlap', '_');
            } else if (Array.isArray(item) && item.length === 2 && typeof item[0] === 'number') {
                const dy = item[1];
                const marker = dy > 0 ? 'card+' : dy < 0 ? 'card-' : 'card';
                fineRow.push(marker, '_');
            } else {
                fineRow.push(null, null);
            }
        }
        fineRows.push(fineRow);
    }
    // Centre each row within maxLen using symmetric leading + trailing padding
    const maxLen = Math.max(...fineRows.map(r => r.length));
    for (const row of fineRows) {
        const totalPad = maxLen - row.length;
        const leadPad  = Math.floor(totalPad / 2);
        const trailPad = totalPad - leadPad;
        for (let i = 0; i < leadPad;  i++) row.unshift(null);
        for (let i = 0; i < trailPad; i++) row.push(null);
    }
    return fineRows;
}

// Convert fine grid → layout format
function editorGridToLayout(grid) {
    let cardIndex = 0;
    const estrutura = [];
    for (const row of grid) {
        const layoutRow = [];
        let i = 0;
        while (i < row.length) {
            const cell = row[i];
            if (cell === null) {
                let count = 0;
                while (i < row.length && row[i] === null) { count++; i++; }
                const fullGaps = Math.floor(count / 2);
                const halfRem  = count % 2;
                for (let g = 0; g < fullGaps; g++) layoutRow.push(null);
                if (halfRem) layoutRow.push(['gap', 0.5]);
            } else if (cell === 'card' || cell === 'card+' || cell === 'card-') {
                const dy = DY_OF[cell] * HALF_STEP_DY;
                layoutRow.push(dy === 0 ? cardIndex++ : [cardIndex++, dy]);
                i++;
                if (i < row.length && row[i] === '_') i++;
            } else if (cell === 'overlap') {
                layoutRow.push([cardIndex++, cardIndex++, 120, 80]);
                i++;
                if (i < row.length && row[i] === '_') i++;
            } else {
                i++; // skip stray '_'
            }
        }
        if (layoutRow.some(x => x !== null)) estrutura.push(layoutRow);
    }
    return estrutura;
}

function renderEditorGrid() {
    const container = document.getElementById('editor-grid');
    container.innerHTML = '';

    editorState.grid.forEach((row, ri) => {
        const rowEl = document.createElement('div');
        rowEl.className = 'editor-row';
        row.forEach((cell, ci) => {
            const cellEl = document.createElement('div');
            cellEl.className = 'editor-cell';
            if (cell === 'card' || cell === 'card+' || cell === 'card-') {
                cellEl.classList.add('c-card', 'c-card-l');
                if (cell === 'card+') cellEl.classList.add('c-nudge-down');
                if (cell === 'card-') cellEl.classList.add('c-nudge-up');
                cellEl.textContent = getCardIndex(ri, ci);
            } else if (cell === '_') {
                cellEl.classList.add('c-card', 'c-card-r');
                const anchor = ci > 0 ? row[ci - 1] : null;
                if (anchor === 'card+') cellEl.classList.add('c-nudge-down');
                if (anchor === 'card-') cellEl.classList.add('c-nudge-up');
            } else if (cell === 'overlap') {
                cellEl.classList.add('c-card', 'c-ovlp', 'c-card-l');
                cellEl.textContent = getCardIndex(ri, ci);
            }
            cellEl.onclick = () => applyCellTool(ri, ci);
            rowEl.appendChild(cellEl);
        });
        container.appendChild(rowEl);
    });
}

function getCardIndex(row, col) {
    let idx = 1;
    for (let r = 0; r < editorState.grid.length; r++) {
        for (let c = 0; c < editorState.grid[r].length; c++) {
            const cell = editorState.grid[r][c];
            if (isAnchorCell(cell)) {
                if (r === row && c === col) return idx;
                idx += (cell === 'overlap') ? 2 : 1;
            }
        }
    }
    return '?';
}

function applyCellTool(r, c) {
    pushHistory();
    const tool  = editorState.tool;
    const grid  = editorState.grid;
    const cell  = grid[r][c];

    if (tool === 'card' || tool === 'overlap') {
        const marker = (tool === 'overlap') ? 'overlap' : 'card';
        if (cell === '_') {
            // Clicked right-half: erase the whole card
            if (c > 0 && isAnchorCell(grid[r][c-1])) grid[r][c-1] = null;
            grid[r][c] = null;
        } else if (isAnchorCell(cell)) {
            // Toggle off
            grid[r][c] = null;
            if (c + 1 < grid[r].length && grid[r][c+1] === '_') grid[r][c+1] = null;
        } else {
            // Place card (spans this cell + next)
            grid[r][c] = marker;
            if (c + 1 < grid[r].length) {
                // Clear whatever was in next cell first
                if (isAnchorCell(grid[r][c+1])) {
                    if (c + 2 < grid[r].length && grid[r][c+2] === '_') grid[r][c+2] = null;
                }
                grid[r][c+1] = '_';
            }
        }
    } else if (tool === 'nudge-up' || tool === 'nudge-down') {
        // Half-row vertical nudge — the vertical counterpart of half-column placement
        let ac = c;
        if (cell === '_' && c > 0 && isAnchorCell(grid[r][c-1])) ac = c - 1;
        const anchor = grid[r][ac];
        if (!(anchor in DY_OF)) {
            showToast('Meio-passo vertical só se aplica a cartas simples');
        } else {
            let dy = DY_OF[anchor] + (tool === 'nudge-down' ? 1 : -1);
            dy = Math.max(-1, Math.min(1, dy));
            grid[r][ac] = MARKER_OF_DY[dy];
        }
    } else {
        // erase / empty
        if (cell === '_') {
            if (c > 0 && isAnchorCell(grid[r][c-1])) grid[r][c-1] = null;
        } else if (isAnchorCell(cell)) {
            if (c + 1 < grid[r].length && grid[r][c+1] === '_') grid[r][c+1] = null;
        }
        grid[r][c] = null;
    }
    renderEditorGrid();
}

const TOOL_HINTS = {
    card:         'Carta — ocupa 1 posição',
    empty:        'Vazio — espaço sem carta',
    overlap:      'Sobreposição — 2 cartas: frente + cruzada (usa 2 cartas do spread)',
    erase:        'Apagar — remove carta ou vazio',
    'nudge-up':   'Meio-passo ↑ — clique numa carta para deslocá-la meia linha para cima',
    'nudge-down': 'Meio-passo ↓ — clique numa carta para deslocá-la meia linha para baixo'
};

function selectTool(tool) {
    editorState.tool = tool;
    document.querySelectorAll('.tool-btn').forEach(b => b.classList.toggle('active', b.dataset.tool === tool));
    const hint = document.getElementById('tool-hint');
    if (hint) hint.textContent = TOOL_HINTS[tool] || '';
}

function editorAddRow() {
    pushHistory();
    const cols = editorState.grid[0] ? editorState.grid[0].length : 6;
    editorState.grid.push(Array(cols).fill(null));
    renderEditorGrid();
}

function editorDelRow() {
    if (editorState.grid.length <= 1) return;
    pushHistory();
    editorState.grid.pop();
    renderEditorGrid();
}

function editorAddCol() {
    pushHistory();
    // Add 2 half-cells = 1 card-width
    editorState.grid.forEach(row => row.push(null, null));
    renderEditorGrid();
}

function editorDelCol() {
    if (!editorState.grid[0] || editorState.grid[0].length <= 2) return;
    pushHistory();
    editorState.grid.forEach(row => row.splice(row.length - 2, 2));
    renderEditorGrid();
}

function editorUndo() {
    if (!editorState.history.length) return;
    editorState.grid = editorState.history.pop();
    renderEditorGrid();
}

function pushHistory() {
    editorState.history.push(JSON.parse(JSON.stringify(editorState.grid)));
    if (editorState.history.length > 30) editorState.history.shift();
}

function saveEditor() {
    const name = document.getElementById('editor-name').value.trim();
    if (!name) { showToast('Digite um nome para o spread'); return; }

    const estrutura = editorGridToLayout(editorState.grid);
    if (!estrutura.length || estrutura.every(r => r.every(x => x === null))) {
        showToast('O spread não pode ser vazio');
        return;
    }

    const id = (editorState.id !== null && editorState.id < 0)
        ? editorState.id    // edit existing custom
        : getNextCustomId(); // new custom

    saveSpread(id, { nome: name, estrutura });
    closeEditor();
    showToast('Spread salvo');
    renderSpreadsList();
    renderSpreadGrid();
}

// ══════════════════════════════════════════
// SETTINGS SCREEN
// ══════════════════════════════════════════

function renderSettings() {
    settings = loadSettings();
    renderSettingsDeckList();
    syncSettingsToggles();
}

function renderSettingsDeckList() {
    const list = document.getElementById('settings-deck-list');
    const allDecks = getAllDecks();
    list.innerHTML = '';

    for (const [id, deck] of Object.entries(allDecks)) {
        const isActive = id === settings.activeDeck;
        const isDefault = !!DEFAULT_DECKS[id];
        const item = document.createElement('div');
        item.className = 'deck-item' + (isActive ? ' active-deck' : '');
        item.innerHTML = `
            <div class="deck-color-bar" style="background:${deck.color||'#c9a84c'}"></div>
            <div class="deck-item-info">
                <div class="deck-item-name">${deck.nomeCompleto || deck.nome}</div>
                <div class="deck-item-meta">${deck.dir} · ${deck.uidsBase ? deck.uidsBase.length : '?'} cartas${isActive ? ' · ativo' : ''}</div>
            </div>
            <div class="deck-item-actions">
                <div class="act-btn edit" onclick="setActiveDeck('${id}');renderSettings();renderHome()">✓</div>
                ${!isDefault ? `<div class="act-btn danger" onclick="removeDeck('${id}')">✕</div>` : ''}
            </div>`;
        list.appendChild(item);
    }
}

function syncSettingsToggles() {
    // Settings screen toggles
    const invToggle = document.getElementById('toggle-inv-settings');
    if (invToggle) invToggle.classList.toggle('off', !settings.invertidas);

    const arcMajors = document.getElementById('arcana-majors');
    const arcFull   = document.getElementById('arcana-full');
    if (arcMajors && arcFull) {
        arcMajors.classList.toggle('active', settings.tipoArcanos === 'majors');
        arcFull.classList.toggle('active',   settings.tipoArcanos === 'full');
    }

    const sliderBlock = document.getElementById('slider-block');
    if (sliderBlock) sliderBlock.style.opacity = settings.invertidas ? '1' : '.4';

    const exifToggle = document.getElementById('toggle-exif');
    if (exifToggle) exifToggle.classList.toggle('off', !settings.exifEnabled);

    const bgBlack  = document.getElementById('bg-black');
    const bgTransp = document.getElementById('bg-transp');
    if (bgBlack && bgTransp) {
        bgBlack.classList.toggle('active',  !settings.transpBackground);
        bgTransp.classList.toggle('active', !!settings.transpBackground);
    }

    // Slider position
    updateSliderUI(settings.invertidasPct);

    // Deck multiplier stepper
    const multVal  = document.getElementById('multiplier-val');
    const multHint = document.getElementById('multiplier-hint');
    if (multVal) {
        const m = settings.deckMultiplier || 0;
        multVal.textContent = m === 0 ? 'Auto' : `${m}x`;
        if (multHint) {
            multHint.textContent = m === 0
                ? 'Padrão automático por tipo'
                : m === 1
                    ? '1 cópia — sem cartas repetidas'
                    : `${m} cópias de cada carta`;
        }
    }
}

function changeMultiplier(delta) {
    const cur = settings.deckMultiplier || 0;
    const next = Math.max(0, Math.min(99, cur + delta));
    settings.deckMultiplier = next;
    saveSettings(settings);
    syncSettingsToggles();
}

function toggleInvSettings() {
    settings.invertidas = !settings.invertidas;
    saveSettings(settings);
    syncSettingsToggles();
    updateQuickConfig();
}

function setArcana(tipo) {
    settings.tipoArcanos = tipo;
    saveSettings(settings);
    syncSettingsToggles();
    updateQuickConfig();
}

function toggleExif() {
    settings.exifEnabled = !settings.exifEnabled;
    saveSettings(settings);
    syncSettingsToggles();
}

function setBg(val) {
    settings.transpBackground = (val === 'transp');
    saveSettings(settings);
    syncSettingsToggles();
}

function initExifAuthorField() {
    const field = document.getElementById('exif-author-input');
    if (!field) return;
    field.value = settings.exifAuthor || '';
    field.addEventListener('change', () => {
        settings.exifAuthor = field.value.trim();
        saveSettings(settings);
    });
}

function removeDeck(id) {
    const custom = loadCustomDecks();
    delete custom[id];
    saveCustomDecks(custom);
    _customDecks = null;
    if (settings.activeDeck === id) {
        settings.activeDeck = 'tdm';
        saveSettings(settings);
    }
    renderSettings();
    renderHome();
    showToast('Baralho removido');
}

// ── Slider ─────────────────────────────────────────────────────────────────────

function initSlider() {
    const track = document.getElementById('slider-track');
    if (!track) return;

    let dragging = false;

    function setFromEvent(e) {
        const rect = track.getBoundingClientRect();
        const clientX = e.touches ? e.touches[0].clientX : e.clientX;
        let pct = (clientX - rect.left) / rect.width;
        pct = Math.max(0, Math.min(1, pct));
        const val = Math.round(pct * 100);
        settings.invertidasPct = val;
        saveSettings(settings);
        updateSliderUI(val);
    }

    track.addEventListener('mousedown', e => { dragging = true; setFromEvent(e); });
    track.addEventListener('touchstart', e => { dragging = true; setFromEvent(e); }, { passive: true });
    document.addEventListener('mousemove', e => { if (dragging) setFromEvent(e); });
    document.addEventListener('touchmove', e => { if (dragging) setFromEvent(e); }, { passive: true });
    document.addEventListener('mouseup',  () => { dragging = false; });
    document.addEventListener('touchend', () => { dragging = false; });
}

function updateSliderUI(val) {
    const pct = val + '%';
    const fill  = document.getElementById('slider-fill');
    const thumb = document.getElementById('slider-thumb');
    const disp  = document.getElementById('slider-val-display');
    if (fill)  fill.style.width = pct;
    if (thumb) thumb.style.left = pct;
    if (disp)  disp.innerHTML = `${val}<sup>%</sup>`;
}

// ── Add Deck Modal ─────────────────────────────────────────────────────────────

let newDeckType = 'tarot';

function setNewDeckType(type) {
    newDeckType = type;
    document.getElementById('new-deck-type-tarot').classList.toggle('active', type === 'tarot');
    document.getElementById('new-deck-type-oracle').classList.toggle('active', type === 'oracle');
}

function openAddDeckModal() {
    document.getElementById('new-deck-nome').value  = '';
    document.getElementById('new-deck-dir').value   = '';
    document.getElementById('new-deck-ext').value   = '.jpg';
    setNewDeckType('tarot');
    openModal('modal-deck');
}

function confirmAddDeck() {
    const nome = document.getElementById('new-deck-nome').value.trim();
    const dir  = document.getElementById('new-deck-dir').value.trim();
    const ext  = document.getElementById('new-deck-ext').value.trim() || '.jpg';

    if (!nome || !dir) { showToast('Preencha nome e diretório'); return; }

    const id  = 'custom_' + Date.now();
    const isT = (newDeckType === 'tarot');
    const deck = {
        id, nome, nomeCompleto: nome, dir, ext,
        uidsBase:  isT ? UIDS_BASE : UIDS_LENORMAND,
        uidsMajors: isT ? UIDS_MAJORS : null,
        hasMajors: isT,
        color: '#9b6fd0'
    };

    const custom = loadCustomDecks();
    custom[id] = deck;
    saveCustomDecks(custom);
    _customDecks = null;

    closeModal('modal-deck');
    renderSettings();
    renderHome();
    showToast('Baralho cadastrado');
}

// ══════════════════════════════════════════
// HELPERS
// ══════════════════════════════════════════

// ── Modals ─────────────────────────────────────────────────────────────────────

function openModal(id) {
    document.getElementById(id).classList.add('open');
}
function closeModal(id) {
    document.getElementById(id).classList.remove('open');
    // Click outside to close
    document.getElementById(id).onclick = e => { if (e.target.id === id) closeModal(id); };
}

// ── Toast ──────────────────────────────────────────────────────────────────────

let _toastTimer = null;
function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.add('show');
    clearTimeout(_toastTimer);
    _toastTimer = setTimeout(() => toast.classList.remove('show'), 2600);
}

// ── Spread visual preview (home grid) ─────────────────────────────────────────

function buildSpreadPreview(estrutura) {
    // Limit to 5 rows, 9 cols for preview
    const rows = estrutura.slice(0, 5);
    const maxCols = Math.min(9, Math.max(...rows.map(r => r.length)));

    let html = '<div class="spread-preview">';
    for (const row of rows) {
        html += '<div class="prev-row">';
        const sliced = row.slice(0, maxCols);
        for (const item of sliced) {
            if (item === undefined || item === null) {
                html += '<div class="prev-empty"></div>';
            } else if (typeof item === 'number') {
                html += '<div class="prev-card"></div>';
            } else if (Array.isArray(item)) {
                if (item[0] === 'gap') {
                    html += '<div class="prev-empty"></div>';
                } else if (item.length === 2) {
                    html += '<div class="prev-card"></div>';
                } else {
                    // overlap
                    html += '<div class="prev-card gold"></div><div class="prev-card over"></div>';
                }
            }
        }
        html += '</div>';
    }
    html += '</div>';
    return html;
}

// ── SIV (spread item visual in list) ──────────────────────────────────────────

function buildSiv(estrutura) {
    const rows = estrutura.slice(0, 4);
    const maxCols = Math.min(5, Math.max(...rows.map(r => r.length)));
    let html = '';
    for (const row of rows) {
        html += '<div class="siv-row">';
        const sliced = row.slice(0, maxCols);
        for (const item of sliced) {
            if (item === undefined || item === null) {
                html += '<div class="siv-card e"></div>';
            } else if (typeof item === 'number') {
                html += '<div class="siv-card"></div>';
            } else if (Array.isArray(item)) {
                if (item[0] === 'gap') {
                    html += '<div class="siv-card e"></div>';
                } else if (item.length === 4) {
                    html += '<div class="siv-card g"></div><div class="siv-card o"></div>';
                } else {
                    html += '<div class="siv-card"></div>';
                }
            }
        }
        html += '</div>';
    }
    return html;
}
