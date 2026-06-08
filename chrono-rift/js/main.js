'use strict';
// ─────────────────────────────────────────────
//  main.js  –  bootstrap
// ─────────────────────────────────────────────

// Fix 25-char map rows (some rows may be 24 chars)
Object.values(MAPS).forEach(map => {
  map.data = map.data.map(row => {
    while (row.length < map.width) row.push(0);
    return row.slice(0, map.width);
  });
});

// Start the game loop then boot the title scene
requestAnimationFrame(ts => {
  lastTs = ts;
  SceneMgr.boot(TitleScene);
  requestAnimationFrame(loop);
});
