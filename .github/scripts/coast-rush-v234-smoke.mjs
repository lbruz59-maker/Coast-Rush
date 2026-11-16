const { chromium } = require('playwright');

const url = 'https://lbruz59-maker.github.io/Coast-Rush/versions/coast-rush-v234-global-state/?smoke=1';
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 1 });
const pageErrors = [];
const consoleErrors = [];
page.on('pageerror', error => pageErrors.push(error.stack || error.message));
page.on('console', message => {
  if (message.type() === 'error') consoleErrors.push(message.text());
});

try {
  let loaded = false;
  for (let attempt = 1; attempt <= 6 && !loaded; attempt++) {
    try {
      const response = await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 30000 });
      loaded = !!response && response.ok();
    } catch (error) {
      if (attempt === 6) throw error;
      await page.waitForTimeout(10000);
    }
  }
  if (!loaded) throw new Error('La page V234 ne répond pas avec HTTP 200.');

  await page.waitForSelector('#c', { timeout: 15000 });
  await page.waitForTimeout(6000);

  const initial = await page.evaluate(() => ({
    title: document.title,
    canvas: !!document.querySelector('#c'),
    menu: !!document.querySelector('#menu'),
    menuVisible: !!document.querySelector('#menu') && getComputedStyle(document.querySelector('#menu')).display !== 'none',
    hotspots: !!document.querySelector('#crMenuHotspots'),
    errorBanner: document.querySelector('#errb')?.textContent || '',
    version: document.querySelector('meta[name="coast-rush-version"]')?.content || ''
  }));

  if (!initial.canvas || !initial.menu || !initial.menuVisible || !initial.hotspots) {
    throw new Error('Échec du démarrage initial : ' + JSON.stringify(initial));
  }
  if (initial.errorBanner) throw new Error('Erreur affichée dans le jeu : ' + initial.errorBanner);
  if (pageErrors.length) throw new Error('Erreurs JavaScript au chargement : ' + pageErrors.join('\n'));

  const playButton = page.locator('#crMenuHotspots button.play');
  await playButton.waitFor({ state: 'visible', timeout: 10000 });
  await playButton.click();
  await page.waitForTimeout(2500);

  const afterPlay = await page.evaluate(() => ({
    errorBanner: document.querySelector('#errb')?.textContent || '',
    menuVisible: getComputedStyle(document.querySelector('#menu')).display !== 'none',
    pauseButton: !!document.querySelector('#pauseBtn')
  }));

  if (afterPlay.errorBanner) throw new Error('Erreur après activation de JOUER : ' + afterPlay.errorBanner);
  if (pageErrors.length) throw new Error('Erreurs JavaScript après activation de JOUER : ' + pageErrors.join('\n'));

  console.log(JSON.stringify({ result: 'PASS', url, initial, afterPlay, pageErrors, consoleErrors }, null, 2));
} finally {
  await browser.close();
}
