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

function canvasToBlob(canvas, quality, transparent) {
    const mime = transparent ? 'image/png' : 'image/jpeg';
    const q    = transparent ? undefined    : (quality || 0.95);
    return new Promise(resolve => canvas.toBlob(resolve, mime, q));
}

function canvasToDataURL(canvas, quality, transparent) {
    if (transparent) return canvas.toDataURL('image/png');
    return canvas.toDataURL('image/jpeg', quality || 0.95);
}
