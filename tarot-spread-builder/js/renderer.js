// ==============================================================================
// Motor de Renderização — Canvas para composição de layouts
// ==============================================================================

const CARD_HEIGHT      = 400;
const CARD_WIDTH_RATIO = 233 / 405;

function loadImage(src) {
    return new Promise(resolve => {
        const img = new Image();
        img.onload  = () => resolve(img);
        img.onerror = () => resolve(null);
        img.src = src;
    });
}

/**
 * Renderiza o layout no canvas.
 * @param {string[]}          cartasSelecionadas  UIDs com prefixo "inv" se invertida
 * @param {string}            deckDir             diretório do baralho
 * @param {string}            ext                 extensão do arquivo
 * @param {Array}             estrutura           estrutura do layout
 * @param {HTMLCanvasElement} canvas
 * @param {boolean}           [transparent=false] fundo transparente (PNG) ou preto (JPEG)
 */
async function renderLayout(cartasSelecionadas, deckDir, ext, estrutura, canvas, transparent) {
    const alturaPadrao      = CARD_HEIGHT;
    const larguraEspacoNone = Math.floor(alturaPadrao * CARD_WIDTH_RATIO);

    // Carregar imagens
    const imgs = [];
    for (const uid of cartasSelecionadas) {
        const invertida = uid.startsWith("inv");
        const uidLimpo  = invertida ? uid.substring(3) : uid;
        const img       = await loadImage(deckDir + "/" + uidLimpo + ext);
        if (img) {
            const proporcao   = alturaPadrao / img.height;
            const novaLargura = Math.floor(img.width * proporcao);
            imgs.push({ img, width: novaLargura, height: alturaPadrao, invertida });
        } else {
            imgs.push(null);
        }
    }

    // Calcular dimensões de cada linha
    const rowWidths = [], rowExtents = [];
    for (const row of estrutura) {
        let w = 0, minDy = 0, maxDy = 0;
        for (const item of row) {
            if (item === null) {
                w += larguraEspacoNone;
            } else if (Array.isArray(item) && item[0] === 'gap') {
                w += Math.floor(item[1] * larguraEspacoNone);
            } else if (typeof item === 'number') {
                w += (item < imgs.length && imgs[item]) ? imgs[item].width : larguraEspacoNone;
            } else if (Array.isArray(item)) {
                if (item.length === 2) {
                    const [idx, dy] = item;
                    w += (idx < imgs.length && imgs[idx]) ? imgs[idx].width : larguraEspacoNone;
                    minDy = Math.min(minDy, dy); maxDy = Math.max(maxDy, dy);
                } else if (item.length === 4) {
                    const [idxF, idxB, dx, dyB] = item;
                    if (idxF < imgs.length && imgs[idxF] && idxB < imgs.length && imgs[idxB]) {
                        w += Math.max(imgs[idxF].width, dx + imgs[idxB].width);
                        minDy = Math.min(minDy, 0, dyB); maxDy = Math.max(maxDy, 0, dyB);
                    } else { w += larguraEspacoNone; }
                }
            }
        }
        rowWidths.push(w);
        rowExtents.push([minDy, maxDy]);
    }

    const maxWidth   = Math.max(...rowWidths);
    const totalHeight = rowExtents.reduce((s, [mn, mx]) => s + alturaPadrao + mx - mn, 0);
    if (!maxWidth || !totalHeight) return;

    canvas.width  = maxWidth;
    canvas.height = totalHeight;
    const ctx = canvas.getContext('2d');

    if (transparent) {
        ctx.clearRect(0, 0, maxWidth, totalHeight);
    } else {
        ctx.fillStyle = '#000000';
        ctx.fillRect(0, 0, maxWidth, totalHeight);
    }

    let currentY = 0;
    for (let i = 0; i < estrutura.length; i++) {
        const row = estrutura[i];
        const [minDy, maxDy] = rowExtents[i];
        const yOff = currentY - minDy;
        let   xOff = Math.floor((maxWidth - rowWidths[i]) / 2);

        for (const item of row) {
            if (item === null) {
                xOff += larguraEspacoNone;
            } else if (Array.isArray(item) && item[0] === 'gap') {
                xOff += Math.floor(item[1] * larguraEspacoNone);
            } else if (typeof item === 'number') {
                if (item < imgs.length && imgs[item]) {
                    drawCard(ctx, imgs[item], xOff, yOff);
                    xOff += imgs[item].width;
                } else { xOff += larguraEspacoNone; }
            } else if (Array.isArray(item)) {
                if (item.length === 2) {
                    const [idx, dy] = item;
                    if (idx < imgs.length && imgs[idx]) {
                        drawCard(ctx, imgs[idx], xOff, yOff + dy);
                        xOff += imgs[idx].width;
                    } else { xOff += larguraEspacoNone; }
                } else if (item.length === 4) {
                    const [idxF, idxB, dx, dyB] = item;
                    if (idxF < imgs.length && imgs[idxF] && idxB < imgs.length && imgs[idxB]) {
                        drawCard(ctx, imgs[idxB], xOff + dx, yOff + dyB);
                        drawCard(ctx, imgs[idxF], xOff, yOff);
                        xOff += Math.max(imgs[idxF].width, dx + imgs[idxB].width);
                    } else { xOff += larguraEspacoNone; }
                }
            }
        }
        currentY += alturaPadrao + maxDy - minDy;
    }
}

function drawCard(ctx, card, x, y) {
    if (card.invertida) {
        ctx.save();
        ctx.translate(x + card.width / 2, y + card.height / 2);
        ctx.rotate(Math.PI);
        ctx.drawImage(card.img, -card.width / 2, -card.height / 2, card.width, card.height);
        ctx.restore();
    } else {
        ctx.drawImage(card.img, x, y, card.width, card.height);
    }
}

// Draw an image into an explicit w×h box (used by the grid renderer, where every
// slot is a uniform card cell rather than the image's natural width).
function drawCardBox(ctx, card, x, y, w, h) {
    if (card.invertida) {
        ctx.save();
        ctx.translate(x + w / 2, y + h / 2);
        ctx.rotate(Math.PI);
        ctx.drawImage(card.img, -w / 2, -h / 2, w, h);
        ctx.restore();
    } else {
        ctx.drawImage(card.img, x, y, w, h);
    }
}

// ==============================================================================
// Grid layout model — cards live on a fine grid whose cell is half a card in
// BOTH axes, so a card occupies a 2×2 block. Half-step offsets (horizontal or
// vertical) are just the card's position on that grid; nothing special. This is
// the format the editor produces for custom spreads. Built-in spreads keep the
// legacy row `estrutura` and go through renderLayout above.
//
//   grid = { cols, rows, cards: [ { r, c, t } ] }
//     r, c : top-left fine cell of the card (each cell = 0.5 card)
//     t    : 'card' (single) | 'overlap' (front + crossed back, uses 2 cards)
// ==============================================================================

// Back card offset for an 'overlap' slot, as a fraction of one card's size.
const GRID_OVERLAP_DX = 0.5;
const GRID_OVERLAP_DY = 0.32;

// Cards in a stable reading order (top→bottom, left→right), each annotated with
// the index of the first spread-card it consumes. An overlap consumes two.
// Every consumer (renderer, editor, previews, card count) uses this same order
// so image N always lands in the same slot.
function gridCardOrder(cards) {
    const sorted = cards.map(c => ({ r: c.r, c: c.c, t: c.t }))
                        .sort((a, b) => (a.r - b.r) || (a.c - b.c));
    let idx = 0;
    for (const card of sorted) {
        card.idx = idx;
        idx += (card.t === 'overlap') ? 2 : 1;
    }
    return sorted;
}

// Footprint bounds in fine-cell units (each card covers r..r+2, c..c+2).
function gridBounds(grid) {
    let minR = Infinity, minC = Infinity, maxR = -Infinity, maxC = -Infinity;
    for (const card of grid.cards) {
        minR = Math.min(minR, card.r);
        minC = Math.min(minC, card.c);
        maxR = Math.max(maxR, card.r + 2);
        maxC = Math.max(maxC, card.c + 2);
    }
    if (!grid.cards.length) return { minR: 0, minC: 0, maxR: grid.rows || 2, maxC: grid.cols || 2 };
    return { minR, minC, maxR, maxC };
}

async function renderLayoutGrid(cartasSelecionadas, deckDir, ext, grid, canvas, transparent) {
    const cardW = Math.floor(CARD_HEIGHT * CARD_WIDTH_RATIO);
    const halfW = cardW / 2;
    const halfH = CARD_HEIGHT / 2;

    // Load images (same convention as renderLayout).
    const imgs = [];
    for (const uid of cartasSelecionadas) {
        const invertida = uid.startsWith("inv");
        const uidLimpo  = invertida ? uid.substring(3) : uid;
        const img       = await loadImage(deckDir + "/" + uidLimpo + ext);
        imgs.push(img ? { img, invertida } : null);
    }

    // Build the list of draw operations in reading order (back cards first so
    // the front of an overlap lands on top).
    const ops = [];
    for (const card of gridCardOrder(grid.cards)) {
        const x = card.c * halfW;
        const y = card.r * halfH;
        if (card.t === 'overlap') {
            const front = imgs[card.idx];
            const back  = imgs[card.idx + 1];
            const dx = cardW * GRID_OVERLAP_DX;
            const dy = CARD_HEIGHT * GRID_OVERLAP_DY;
            if (back)  ops.push({ img: back,  x: x + dx, y: y + dy });
            if (front) ops.push({ img: front, x,          y });
        } else {
            const front = imgs[card.idx];
            if (front) ops.push({ img: front, x, y });
        }
    }
    if (!ops.length) return;

    // Tight bounding box over the actually-drawn rectangles.
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    for (const op of ops) {
        minX = Math.min(minX, op.x);
        minY = Math.min(minY, op.y);
        maxX = Math.max(maxX, op.x + cardW);
        maxY = Math.max(maxY, op.y + CARD_HEIGHT);
    }

    const W = Math.ceil(maxX - minX);
    const H = Math.ceil(maxY - minY);
    if (!W || !H) return;

    canvas.width  = W;
    canvas.height = H;
    const ctx = canvas.getContext('2d');
    if (transparent) {
        ctx.clearRect(0, 0, W, H);
    } else {
        ctx.fillStyle = '#000000';
        ctx.fillRect(0, 0, W, H);
    }

    for (const op of ops) {
        drawCardBox(ctx, op.img, op.x - minX, op.y - minY, cardW, CARD_HEIGHT);
    }
}

function canvasToBlob(canvas, quality, transparent) {
    const mime = transparent ? 'image/png' : 'image/jpeg';
    const q    = transparent ? undefined    : (quality || 0.95);
    return new Promise(resolve => canvas.toBlob(resolve, mime, q));
}

function canvasToDataURL(canvas, quality, transparent) {
    if (transparent) return canvas.toDataURL('image/png');
    return canvas.toDataURL('image/jpeg', quality || 0.95);
}
