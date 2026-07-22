// ==============================================================================
// Motor do Jogo — Embaralhamento, Inversão e Seleção
// ==============================================================================

function shuffleArray(arr) {
    for (let i = arr.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
}

/**
 * Prepara o baralho completo: multiplica, embaralha e marca inversões.
 * @param {string} deckId          - 'rws', 'tdm', 'len' ou ID customizado
 * @param {string} tipo            - 'full' ou 'majors'
 * @param {number} invertidasPct   - 0–100 (percentual de cartas invertidas)
 * @param {number} [multiplier]    - cópias do baralho (0 = automático)
 * @returns {Array<{uid, inverted, index}>}
 */
function prepararBaralho(deckId, tipo, invertidasPct, multiplier) {
    const allDecks = getAllDecks();
    const deck = allDecks[deckId];
    if (!deck) return [];

    const pct = (invertidasPct === undefined || invertidasPct === null) ? 30 : invertidasPct;

    let baseUids, numDecks;
    if (!deck.hasMajors || deckId === 'len') {
        baseUids = deck.uidsBase;
        numDecks = NUM_DECKS_LENORMAND;
    } else if (tipo === 'majors') {
        baseUids = deck.uidsMajors;
        numDecks = NUM_DECKS_MAJORS;
    } else {
        baseUids = deck.uidsBase;
        numDecks = NUM_DECKS_FULL;
    }
    if (multiplier > 0) numDecks = multiplier;

    // Multiplicar baralho
    let cards = [];
    for (let d = 0; d < numDecks; d++) {
        for (const uid of baseUids) cards.push(uid);
    }

    // Embaralhar (Fisher-Yates)
    shuffleArray(cards);

    // Marcar inversões de acordo com o percentual definido
    const numInvertidas = Math.round((pct / 100) * cards.length);
    const indicesSet = new Set();
    while (indicesSet.size < numInvertidas) {
        indicesSet.add(Math.floor(Math.random() * cards.length));
    }

    return cards.map((uid, i) => ({
        uid:      uid,
        inverted: indicesSet.has(i),
        index:    i + 1   // 1-based
    }));
}
