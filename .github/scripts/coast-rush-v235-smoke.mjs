import { chromium } from 'playwright';

const url = 'https://lbruz59-maker.github.io/Coast-Rush/versions/coast-rush-v235-render-fix/?smoke=1';
const browser = await chromium.launch({ headless: true });
const pageErrors = [];
const consoleErrors = [];

function watch(page, tag) {
  page.on('pageerror', error => pageErrors.push(tag + ': ' + (error.stack || error.message)));
  page.on('console', message => {
    if (message.type() === 'error') consoleErrors.push(tag + ': ' + message.text());
  });
}

async function assertNoRuntimeError(page, tag) {
  const banner = await page.locator('#errb').textContent().catch(() => '');
  if (banner) throw new Error(tag + ' : ' + banner);
  if (pageErrors.length) throw new Error(tag + ' : ' + pageErrors.join('\\n'));
}

try {
  const desktop = await browser.newPage({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 1 });
  watch(desktop, 'desktop');
  const response = await desktop.goto(url, { waitUntil: 'domcontentloaded', timeout: 45000 });
  if (!response || !response.ok()) throw new Error('V235 ne répond pas en HTTP 200.');
  await desktop.waitForSelector('#c', { timeout: 15000 });
  await desktop.waitForTimeout(2500);

  const initial = await desktop.evaluate(() => ({
    title: document.title,
    version: document.querySelector('meta[name="coast-rush-version"]')?.content || '',
    canvas: !!document.querySelector('#c'),
    menuVisible: getComputedStyle(document.querySelector('#menu')).display !== 'none',
    hotspots: !!document.querySelector('#crMenuHotspots'),
    manifest: !!document.querySelector('link[rel="manifest"]'),
    serviceWorkerSupported: 'serviceWorker' in navigator,
    errorBanner: document.querySelector('#errb')?.textContent || ''
  }));
  if (!initial.canvas || !initial.menuVisible || !initial.hotspots || !initial.manifest) {
    throw new Error('Échec du démarrage initial : ' + JSON.stringify(initial));
  }
  await assertNoRuntimeError(desktop, 'chargement desktop');

  await desktop.locator('#crMenuHotspots button.play').click();
  await desktop.waitForTimeout(5000);
  await assertNoRuntimeError(desktop, 'après JOUER');
  const outroRenderers = await desktop.evaluate(() => {
    drawOutrunMap(1);
    drawOutrunVictoryCine(1);
    return {
      mapRenderer: typeof drawOutrunMap === 'function',
      victoryRenderer: typeof drawOutrunVictoryCine === 'function'
    };
  });
  await assertNoRuntimeError(desktop, 'rendu de la carte et de la victoire OutRun');
  const playing = await desktop.evaluate(() => ({
    menuVisible: getComputedStyle(document.querySelector('#menu')).display !== 'none',
    pauseVisible: getComputedStyle(document.querySelector('#pauseBtn')).display !== 'none',
    pauseLayer: getComputedStyle(document.querySelector('#pauseLayer')).display
  }));
  if (playing.menuVisible || !playing.pauseVisible) throw new Error('La course ne passe pas correctement en état de jeu : ' + JSON.stringify(playing));

  await desktop.locator('#pauseBtn').click();
  await desktop.waitForTimeout(350);
  const paused = await desktop.evaluate(() => ({
    visible: getComputedStyle(document.querySelector('#pauseLayer')).display !== 'none',
    buttonHidden: getComputedStyle(document.querySelector('#pauseBtn')).display === 'none'
  }));
  if (!paused.visible || !paused.buttonHidden) throw new Error('La mise en pause ne fonctionne pas : ' + JSON.stringify(paused));
  await desktop.locator('#resumeBtn').click();
  await desktop.waitForTimeout(1200);
  await assertNoRuntimeError(desktop, 'après reprise');
  const resumed = await desktop.evaluate(() => ({
    layerHidden: getComputedStyle(document.querySelector('#pauseLayer')).display === 'none',
    pauseVisible: getComputedStyle(document.querySelector('#pauseBtn')).display !== 'none'
  }));
  if (!resumed.layerHidden || !resumed.pauseVisible) throw new Error('La reprise ne restaure pas les commandes : ' + JSON.stringify(resumed));

  await desktop.locator('#pauseBtn').click();
  await desktop.locator('#pauseMenuBtn').click();
  await desktop.waitForTimeout(900);
  await assertNoRuntimeError(desktop, 'retour menu');
  const returned = await desktop.evaluate(() => ({
    menuVisible: getComputedStyle(document.querySelector('#menu')).display !== 'none',
    pauseHidden: getComputedStyle(document.querySelector('#pauseBtn')).display === 'none',
    pauseLayerHidden: getComputedStyle(document.querySelector('#pauseLayer')).display === 'none'
  }));
  if (!returned.menuVisible || !returned.pauseHidden || !returned.pauseLayerHidden) {
    throw new Error('Le retour au menu ne réinitialise pas correctement l’interface : ' + JSON.stringify(returned));
  }

  const timeTrial = await desktop.evaluate(() => {
    MODE = 3; MODE0 = 3; selectedTimeTrialCircuit = 0;
    localStorage.setItem('cr_tt_records', JSON.stringify({ '0': 1234 }));
    localStorage.setItem('cr_time_trial_done', JSON.stringify([0, 1, 2, 9, 12]));
    ttStartAt = performance.now() - 73420;
    state = 'play'; score = 1234; endCourse();
    const equalRecord = ttFinishNewBest;
    state = 'play'; score = 1235; endCourse();
    const improvedRecord = ttFinishNewBest;
    const unlocked = JSON.parse(localStorage.getItem('cr_time_trial_done') || '[]');
    renderScene();
    const screen = typeof drawTimeTrialFinish === 'function';
    const continued = crContinueTimeTrial();
    return {
      screen,
      equalRecord,
      improvedRecord,
      nextCircuitUnlocked: unlocked.includes(4),
      continued,
      menuVisible: getComputedStyle(document.querySelector('#menu')).display !== 'none',
      pickerVisible: !!document.querySelector('.crMenuLayer')
    };
  });
  await assertNoRuntimeError(desktop, 'écran de fin du contre-la-montre');
  if (!timeTrial.screen || timeTrial.equalRecord || !timeTrial.improvedRecord || !timeTrial.nextCircuitUnlocked || !timeTrial.continued || !timeTrial.menuVisible || !timeTrial.pickerVisible) {
    throw new Error('Échec de l’écran ou du retour au sélecteur contre-la-montre : ' + JSON.stringify(timeTrial));
  }
  await desktop.keyboard.press('Escape');
  await desktop.waitForTimeout(250);
  await desktop.evaluate(() => { MODE = 0; MODE0 = 0; });
  await desktop.locator('#crMenuHotspots button.play').click();
  await desktop.waitForTimeout(8000);
  await assertNoRuntimeError(desktop, 'après deuxième démarrage prolongé');

  const mobile = await browser.newPage({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 2,
    isMobile: true,
    hasTouch: true
  });
  watch(mobile, 'mobile');
  const mobileResponse = await mobile.goto(url + '&mobile=1', { waitUntil: 'domcontentloaded', timeout: 45000 });
  if (!mobileResponse || !mobileResponse.ok()) throw new Error('V235 ne répond pas en HTTP 200 sur le profil mobile.');
  await mobile.waitForSelector('#crMenuHotspots button.play', { state: 'visible', timeout: 15000 });
  const mobileLayout = await mobile.evaluate(() => ({
    viewportWidth: innerWidth,
    viewportHeight: innerHeight,
    documentWidth: document.documentElement.scrollWidth,
    documentHeight: document.documentElement.scrollHeight,
    playVisible: !!document.querySelector('#crMenuHotspots button.play') &&
      getComputedStyle(document.querySelector('#crMenuHotspots button.play')).display !== 'none'
  }));
  if (!mobileLayout.playVisible || mobileLayout.documentWidth > mobileLayout.viewportWidth + 2) {
    throw new Error('Débordement ou bouton JOUER inaccessible sur mobile : ' + JSON.stringify(mobileLayout));
  }
  const mobileTap = await mobile.evaluate(() => {
    const b = document.querySelector('#crMenuHotspots button.play');
    const r = b.getBoundingClientRect();
    const top = document.elementFromPoint(r.x + r.width / 2, r.y + r.height / 2);
    return {
      buttonRect: { x: r.x, y: r.y, width: r.width, height: r.height },
      topElement: top ? { id: top.id, className: String(top.className || ''), tag: top.tagName } : null,
      menuScrollTop: document.querySelector('#menu').scrollTop,
      menuHeight: document.querySelector('#menu').clientHeight,
      menuScrollHeight: document.querySelector('#menu').scrollHeight
    };
  });
  await mobile.locator('#crMenuHotspots button.play').click();
  await mobile.waitForTimeout(1200);
  const afterHotspot = await mobile.evaluate(() => ({
    menuVisible: getComputedStyle(document.querySelector('#menu')).display !== 'none',
    toast: document.querySelector('#tt')?.textContent || '',
    errorBanner: document.querySelector('#errb')?.textContent || ''
  }));
  if (afterHotspot.menuVisible) await mobile.locator('#go').evaluate(el => el.click());
  await mobile.waitForTimeout(3500);
  await assertNoRuntimeError(mobile, 'jeu mobile');
  const mobilePlaying = await mobile.evaluate(() => ({
    menuHidden: getComputedStyle(document.querySelector('#menu')).display === 'none',
    pauseVisible: getComputedStyle(document.querySelector('#pauseBtn')).display !== 'none',
    toast: document.querySelector('#tt')?.textContent || '',
    errorBanner: document.querySelector('#errb')?.textContent || ''
  }));
  if (!mobilePlaying.menuHidden || !mobilePlaying.pauseVisible || afterHotspot.menuVisible) {
    throw new Error('Le démarrage mobile par zone tactile échoue : ' + JSON.stringify({ mobileTap, afterHotspot, mobilePlaying }));
  }

  console.log(JSON.stringify({
    result: 'PASS',
    url,
    initial,
    playing,
    paused,
    resumed,
    returned,
    mobileLayout,
    mobilePlaying,
    pageErrors,
    consoleErrors
  }, null, 2));
} finally {
  await browser.close();
}
