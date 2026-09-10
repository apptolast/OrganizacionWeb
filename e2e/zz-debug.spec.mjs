import { test } from "@playwright/test";
const CONN = { status:"connected", apiBase:"https://gitlab.com/api/v4", projectPath:"plataforma-de-integraciones/servicios-compartidos/pasarela-de-eventos-internos", projectId:90210, tokenHint:"9f4c", lastActivityAt:"2026-09-08T10:00:00.000000Z", lastError:null, version:3 };
test("debug", async ({ page }) => {
  await page.route("**/api/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/session") return route.fulfill({ json: { authenticated: true, username: "Ana", csrfToken: "x", csrfHeaderName: "X-CSRF-TOKEN" } });
    if (path === "/api/v1/me/connectors/gitlab") return route.fulfill({ json: CONN });
    return route.fulfill({ status: 204, body: "" });
  });
  await page.goto("/conectores/gitlab");
  await page.waitForSelector("main dl");
  await page.evaluate(() => {
    const nodes = [...document.querySelectorAll("main,main *")];
    const before = nodes.map((n) => parseFloat(getComputedStyle(n).fontSize));
    nodes.forEach((n, i) => { n.style.fontSize = `${before[i] * 2}px`; });
  });
  await page.setViewportSize({ width: 320, height: 900 });
  const out = await page.evaluate(() => {
    const dl = document.querySelector("main dl");
    const cs = getComputedStyle(dl);
    return {
      cols: cs.gridTemplateColumns, gap: cs.gap, ow: cs.overflowWrap, wb: cs.wordBreak,
      dlw: dl.clientWidth, dlsw: dl.scrollWidth,
      kids: [...dl.children].map((k) => ({ t: k.tagName, txt: (k.textContent||"").slice(0,20), w: Math.round(k.getBoundingClientRect().width), sw: k.scrollWidth, cw: k.clientWidth, right: Math.round(k.getBoundingClientRect().right), ow: getComputedStyle(k).overflowWrap })),
    };
  });
  console.log(JSON.stringify(out, null, 1));
});
