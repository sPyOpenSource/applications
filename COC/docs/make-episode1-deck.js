const pptxgen = require('pptxgenjs');

const deck = new pptxgen();
deck.layout = 'LAYOUT_16x9';

// Palette — dark tech theme, no default blue
const DARK   = '0E1526';   // near-black navy (dominant)
const PANEL  = '141E36';   // card background
const ACCENT = '7CFC00';   // bright green accent (game/terminal feel)
const MUTED  = '8FA3C2';   // muted blue-gray text
const WHITE  = 'F5F5F5';
const CODE   = '0A0A0A';

function baseSlide() {
    const slide = deck.addSlide();
    slide.background = { color: DARK };
    return slide;
}

function titleBar(slide, text, kicker) {
    slide.addText(kicker, {
        x: 0.7, y: 0.35, w: 11.9, h: 0.3,
        fontSize: 13, color: ACCENT, fontFace: 'Arial', bold: true,
    });
    slide.addText(text, {
        x: 0.7, y: 0.62, w: 11.9, h: 0.75,
        fontSize: 34, color: WHITE, fontFace: 'Arial', bold: true,
    });
    slide.addShape(deck.ShapeType.rect, {
        x: 0.7, y: 1.42, w: 11.9, h: 0.03,
        fill: { color: PANEL },
    });
}

function footer(slide, page) {
    slide.addText('YouTube \u2022 JavaFX by Walking Through Clash of Clans \u2022 Ep 1 \u2022 ' + page + '/8', {
        x: 0.7, y: 7.15, w: 11.9, h: 0.3,
        fontSize: 10, color: MUTED, fontFace: 'Arial',
    });
}

function codeBox(slide, x, y, w, h, lines) {
    slide.addShape(deck.ShapeType.rect, {
        x, y, w, h,
        fill: { color: PANEL },
        line: { color: '1E2A47', width: 1 },
        shadow: { type: 'outer', color: '000000', blur: 8, offset: 4, angle: 90, opacity: 0.6 },
    });
    const items = lines.map((ln, i) => ({
        text: ln,
        bullet: { type: 'none' },
        breakLine: i < lines.length - 1,
        fontSize: 12, fontFace: 'Consolas', color: MUTED, margin: 0,
    }));
    slide.addText(items, {
        x: x + 0.25, y: y + 0.2, w: w - 0.5, h: h - 0.4,
        valign: 'top',
    });
}

function statCard(slide, x, y, w, h, number, label, color) {
    slide.addShape(deck.ShapeType.rect, {
        x, y, w, h,
        fill: { color: PANEL },
        line: { color: '1E2A47', width: 1 },
    });
    slide.addText(number, {
        x, y: y + 0.18, w, h: 0.9,
        fontSize: 30, color: color || ACCENT, fontFace: 'Arial', bold: true, align: 'center',
    });
    slide.addText(label, {
        x, y: y + 1.05, w, h: 0.5,
        fontSize: 13, color: MUTED, fontFace: 'Arial', align: 'center',
    });
}

module.exports = { pptxgen, deck, baseSlide, titleBar, footer, codeBox, statCard, DARK, PANEL, ACCENT, MUTED, WHITE, CODE };
