import { test, expect } from "./support/authenticated-test.mjs";
import AxeBuilder from "@axe-core/playwright";
import { create, sql } from "./support/projects.mjs";
import { withConnectorDisabled } from "./support/connector.mjs";

/**
 * @s42 el recorrido del conector de GitHub es accesible en sus siete estados y en los tres anchos.
 *
 * Los estados se alcanzan **de verdad**, por la interfaz y contra el servicio falso de GitHub que
 * vive en la pila de compose bajo el perfil e2e. El falso comparte el espacio de red del backend,
 * así que éste lo alcanza por loopback y la lista blanca de `GithubApiBase` no se relaja: ninguna
 * petición sale hacia api.github.com. Su conducta depende del nombre del repositorio, de modo que
 * cada recorrido es determinista y no hay estado compartido entre pruebas.
 */

const PAGE = "/integraciones/github";
const OWNER = "e2e-user";
const TOKEN = "ghp_token_de_pruebas";

/** Repositorios que entiende el servicio falso, cada uno con su conducta. */
const REPOSITORY = {
  ok: "octocat/hello-world",
  many: "octocat/muchas",
  withFailure: "octocat/con-fallo",
  rateLimited: "octocat/limitado",
  rejected: "octocat/rechazado",
};

/** Limpia lo del propietario de pruebas respetando el orden de las claves ajenas. */
function forget() {
  sql(`DELETE FROM task_external_links WHERE owner_id='${OWNER}'`);
  sql(`DELETE FROM issue_import_receipts WHERE owner_id='${OWNER}'`);
  sql(
    `DELETE FROM tasks WHERE project_id IN (SELECT id FROM projects WHERE owner_id='${OWNER}')`,
  );
  sql(`DELETE FROM outbox_events WHERE owner_id='${OWNER}'`);
  sql(`DELETE FROM projects WHERE owner_id='${OWNER}'`);
  sql(`DELETE FROM connector_connections WHERE owner_id='${OWNER}'`);
}

test.beforeEach(() => forget());
test.afterEach(() => forget());

const axeTags = ["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"];

async function auditAxe(page, state) {
  const results = await new AxeBuilder({ page }).withTags(axeTags).analyze();
  expect(results.violations, `axe en el estado ${state}`).toEqual([]);
}

/** El resumen de la importación, distinto de la región aria-live que repite los contadores. */
function summary(page) {
  return page.getByRole("region", { name: "Resultado de la importación" });
}

/** Conecta por la interfaz, como haría una persona: nada de INSERT directo. */
async function connectThrough(page, repository) {
  await page.goto(PAGE);
  await page.getByLabel(/repositorio/i).fill(repository);
  await page.getByLabel(/token/i).fill(TOKEN);
  await page.getByRole("button", { name: "Conectar" }).click();
}

/**
 * Geometría de **todos** los controles visibles del contenido: botones, enlaces, campos y el
 * selector. No se excluye ningún enlace: los de esta pantalla son el único contenido de su bloque
 * y no caen bajo la excepción *Inline* de WCAG 2.2 §2.5.8, que cubre destinos dentro de una frase.
 */
function geometry(page) {
  return page.evaluate(() => ({
    width: document.documentElement.clientWidth,
    scroll: document.documentElement.scrollWidth,
    controls: [
      ...document.querySelectorAll(
        "main button, main a, main select, main input, main textarea",
      ),
    ]
      .filter((element) => element.getClientRects().length)
      .map((element) => {
        const box = element.getBoundingClientRect();
        return {
          name:
            element.getAttribute("aria-label") ||
            element.labels?.[0]?.textContent ||
            element.textContent ||
            element.id,
          x: box.x,
          width: box.width,
          height: box.height,
        };
      }),
  }));
}

/** Sin desplazamiento horizontal, sin contenido recortado y con destinos de 44 × 44. */
async function assertLayout(page, state) {
  const measured = await geometry(page);
  expect(
    measured.scroll,
    `desplazamiento horizontal en ${state}`,
  ).toBeLessThanOrEqual(measured.width + 1);
  for (const control of measured.controls) {
    const where = `${state} → «${String(control.name).trim().slice(0, 40)}»`;
    expect(
      control.x,
      `recortado por la izquierda en ${where}`,
    ).toBeGreaterThanOrEqual(-1);
    expect(
      control.x + control.width,
      `recortado por la derecha en ${where}`,
    ).toBeLessThanOrEqual(measured.width + 1);
    expect(control.width, `ancho mínimo en ${where}`).toBeGreaterThanOrEqual(
      44,
    );
    expect(control.height, `alto mínimo en ${where}`).toBeGreaterThanOrEqual(
      44,
    );
  }
  return measured;
}

// ------------------------------------------------------- los siete estados, uno a uno, con axe

test("@s42 estado deshabilitado: el servidor sin clave de conectores", async ({
  page,
  request,
}) => {
  await withConnectorDisabled(request, async () => {
    await page.goto(PAGE);
    await expect(page.getByRole("alert")).toContainText(
      /configuración del servidor/i,
    );
    await expect(page.getByLabel(/token/i)).toHaveCount(0);
    await expect(
      page.getByRole("button", { name: "Importar issues abiertas" }),
    ).toHaveCount(0);
    await auditAxe(page, "deshabilitado");
    await assertLayout(page, "deshabilitado");
  });
});

test("@s42 estado sin conexión", async ({ page }) => {
  await page.goto(PAGE);

  await expect(page.getByLabel(/repositorio/i)).toBeVisible();
  await expect(page.getByLabel(/token/i)).toBeVisible();
  await auditAxe(page, "sin conexión");
  await assertLayout(page, "sin conexión");
});

test("@s42 estado conectada, alcanzado conectando de verdad", async ({
  page,
  request,
}) => {
  await create(request, "Proyecto destino");

  await connectThrough(page, REPOSITORY.ok);

  await expect(page.getByText("Conectada")).toBeVisible();
  await expect(page.getByText("octocat/hello-world")).toBeVisible();
  // El login lo devuelve el servicio falso, no el formulario: prueba que hubo ida y vuelta.
  await expect(page.getByText("octocat", { exact: true })).toBeVisible();
  // Y la fila guardada lleva texto cifrado de verdad, no ceros.
  // Formato en reposo: nonce de 12 octetos, cifrado y etiqueta GCM de 16. Sin byte de
  // versión: al unificar SecretCipher entre las features 27 y 28 se retiró, porque @s1 de
  // este mismo contrato exige «octet_length 12 + 14 + 16» y el byte lo contradecía.
  expect(
    sql(
      `SELECT octet_length(token_ciphertext) FROM connector_connections WHERE owner_id='${OWNER}'`,
    ),
  ).toBe(String(12 + TOKEN.length + 16));
  await auditAxe(page, "conectada");
  await assertLayout(page, "conectada");
});

test("@s42 estado importando y estado resultado, importando de verdad", async ({
  page,
  request,
}) => {
  await create(request, "Proyecto destino");
  await connectThrough(page, REPOSITORY.withFailure);
  await expect(page.getByText("Conectada")).toBeVisible();

  await page.getByRole("button", { name: "Importar issues abiertas" }).click();

  // Estado importando: el aviso honesto, sin porcentaje, y el botón inhabilitado.
  const live = page.getByRole("status");
  await expect(live).toContainText("Importando issues…");
  await expect(live).not.toContainText("%");

  // Estado resultado: contadores medibles y enlace al proyecto destino.
  await expect(summary(page).getByText("Creadas 1")).toBeVisible();
  await expect(summary(page).getByText("Omitidas 0")).toBeVisible();
  await expect(summary(page).getByText("Fallidas 1")).toBeVisible();
  await expect(
    summary(page).getByRole("link", { name: /Proyecto destino/ }),
  ).toBeVisible();
  await expect(page.getByRole("heading", { level: 1 })).toBeFocused();
  await auditAxe(page, "resultado");
  await assertLayout(page, "resultado");
});

test("@s42 reimportar omite lo ya importado", async ({ page, request }) => {
  await create(request, "Proyecto destino");
  await connectThrough(page, REPOSITORY.ok);
  await expect(page.getByText("Conectada")).toBeVisible();
  await page.getByRole("button", { name: "Importar issues abiertas" }).click();
  await expect(summary(page).getByText("Creadas 3")).toBeVisible();

  await page.getByRole("button", { name: "Importar issues abiertas" }).click();

  await expect(summary(page).getByText("Creadas 0")).toBeVisible();
  await expect(summary(page).getByText("Omitidas 3")).toBeVisible();
  expect(
    sql(`SELECT count(*) FROM task_external_links WHERE owner_id='${OWNER}'`),
  ).toBe("3");
});

test("@s42 estado de error recuperable: cuota agotada con su plazo", async ({
  page,
  request,
}) => {
  await create(request, "Proyecto destino");
  await connectThrough(page, REPOSITORY.rateLimited);
  await expect(page.getByText("Conectada")).toBeVisible();

  await page.getByRole("button", { name: "Importar issues abiertas" }).click();

  await expect(page.getByText(/Reintenta en 90 segundos/)).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Importar issues abiertas" }),
  ).toBeEnabled();
  await auditAxe(page, "error recuperable");
  await assertLayout(page, "error recuperable");
});

test("@s42 estado inválida, tras un token que GitHub deja de aceptar", async ({
  page,
  request,
}) => {
  await create(request, "Proyecto destino");
  await connectThrough(page, REPOSITORY.ok);
  await expect(page.getByText("Conectada")).toBeVisible();
  sql(
    `UPDATE connector_connections SET status='invalid' WHERE owner_id='${OWNER}'`,
  );

  await page.reload();

  await expect(page.getByText("Conexión inválida")).toBeVisible();
  await expect(page.getByRole("button", { name: "Reconectar" })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Importar issues abiertas" }),
  ).toHaveCount(0);
  await auditAxe(page, "inválida");
  await assertLayout(page, "inválida");
});

// ------------------------------------------------------------------- teclado: Tab, Enter, Escape

test("@s42 el recorrido con Tab alcanza los controles en el orden en que se leen", async ({
  page,
  request,
}) => {
  await create(request, "Proyecto destino");
  await connectThrough(page, REPOSITORY.ok);
  await expect(page.getByText("Conectada")).toBeVisible();

  const expectedOrder = await page.evaluate(() =>
    [
      ...document.querySelectorAll(
        "main button, main a, main select, main input",
      ),
    ]
      .filter((element) => element.getClientRects().length)
      .map((element) => element.textContent?.trim() || element.id),
  );

  await page.evaluate(() => document.querySelector("main h1").focus());
  const reached = [];
  for (let step = 0; step < expectedOrder.length + 4; step++) {
    await page.keyboard.press("Tab");
    const current = await page.evaluate(() => {
      const active = document.activeElement;
      if (!active || !active.closest("main")) return null;
      return active.textContent?.trim() || active.id;
    });
    if (current && !reached.includes(current)) reached.push(current);
  }

  expect(reached).toEqual(expectedOrder);
});

test("@s42 el foco es visible en cada control del contenido", async ({
  page,
  request,
}) => {
  await create(request, "Proyecto destino");
  await connectThrough(page, REPOSITORY.ok);
  await expect(page.getByText("Conectada")).toBeVisible();

  // El foco tiene que llegar **con el teclado**: la hoja usa `:focus-visible`, que Chromium no
  // aplica a un `element.focus()` programático sobre botones y enlaces. Medirlo así daría un
  // falso negativo, que es justo lo que le pasó a la primera versión de esta prueba.
  const controls = await page.evaluate(
    () =>
      [
        ...document.querySelectorAll(
          "main button, main a, main select, main input",
        ),
      ].filter((element) => element.getClientRects().length).length,
  );
  await page.evaluate(() => document.querySelector("main h1").focus());

  const invisible = [];
  for (let step = 0; step < controls; step++) {
    await page.keyboard.press("Tab");
    const ring = await page.evaluate(() => {
      const active = document.activeElement;
      if (!active || !active.closest("main")) return null;
      const style = getComputedStyle(active);
      return {
        name: active.textContent?.trim() || active.id,
        matchesFocusVisible: active.matches(":focus-visible"),
        outlineStyle: style.outlineStyle,
        outlineWidth: parseFloat(style.outlineWidth),
        outlineColor: style.outlineColor,
      };
    });
    if (!ring) continue;
    const visible =
      ring.matchesFocusVisible &&
      ring.outlineStyle !== "none" &&
      ring.outlineWidth >= 1 &&
      !ring.outlineColor.includes("transparent");
    if (!visible) invisible.push(ring);
  }

  expect(invisible).toEqual([]);
});

test("@s42 Escape cancela la confirmación de desconexión y devuelve el foco", async ({
  page,
  request,
}) => {
  await create(request, "Proyecto destino");
  await connectThrough(page, REPOSITORY.ok);
  const disconnect = page.getByRole("button", { name: "Desconectar" });
  await disconnect.focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByRole("button", { name: "Confirmar desconexión" }),
  ).toBeVisible();

  await page.keyboard.press("Escape");

  await expect(
    page.getByRole("button", { name: "Confirmar desconexión" }),
  ).toHaveCount(0);
  await expect(disconnect).toBeFocused();
  expect(
    sql(`SELECT count(*) FROM connector_connections WHERE owner_id='${OWNER}'`),
  ).toBe("1");
});

test("@s42 desconectar desde el teclado borra la conexión y conserva lo importado", async ({
  page,
  request,
}) => {
  await create(request, "Proyecto destino");
  await connectThrough(page, REPOSITORY.ok);
  await expect(page.getByText("Conectada")).toBeVisible();
  await page.getByRole("button", { name: "Importar issues abiertas" }).click();
  await expect(summary(page).getByText("Creadas 3")).toBeVisible();

  await page.getByRole("button", { name: "Desconectar" }).focus();
  await page.keyboard.press("Enter");
  await page.getByRole("button", { name: "Confirmar desconexión" }).focus();
  await page.keyboard.press("Enter");

  await expect(page.getByLabel(/token/i)).toBeVisible();
  expect(
    sql(`SELECT count(*) FROM connector_connections WHERE owner_id='${OWNER}'`),
  ).toBe("0");
  // Las tareas y los enlaces sobreviven: son datos propios, no credenciales.
  expect(
    sql(`SELECT count(*) FROM task_external_links WHERE owner_id='${OWNER}'`),
  ).toBe("3");
});

// ------------------------------------------------------------------------- índice y token

test("@s42 «/integraciones» enlaza el conector y el menú no gana ninguna entrada", async ({
  page,
}) => {
  await page.goto("/integraciones");

  await expect(
    page.getByRole("main").getByRole("link", { name: "Conector de GitHub" }),
  ).toHaveAttribute("href", "/integraciones/github");
  await expect(
    page
      .getByRole("navigation", { name: "Principal" })
      .getByRole("link", { name: "Conector de GitHub" }),
  ).toHaveCount(0);
});

test("@s34 @s42 el token no queda en ningún almacén del navegador", async ({
  page,
}) => {
  await connectThrough(page, REPOSITORY.ok);
  await expect(page.getByText("Conectada")).toBeVisible();

  const leaked = await page.evaluate((token) => {
    const dump = [
      JSON.stringify(localStorage),
      JSON.stringify(sessionStorage),
      document.cookie,
      document.documentElement.outerHTML,
    ].join(" ");
    return dump.includes(token);
  }, TOKEN);

  expect(leaked).toBe(false);
  await expect(page.getByLabel(/token/i)).toHaveCount(0);
});

test("@s34 la respuesta de auditoría de sesión no lleva el token ni rastro del conector", async ({
  page,
  request,
}) => {
  await connectThrough(page, REPOSITORY.ok);
  await expect(page.getByText("Conectada")).toBeVisible();

  const audit = await request.get("/api/session");
  expect(audit.status()).toBe(200);
  const body = await audit.text();

  expect(body).not.toContain(TOKEN);
  expect(body).not.toContain(Buffer.from(TOKEN).toString("base64"));
  // Tampoco el repositorio ni nada del conector: la auditoría de sesión habla de la sesión.
  expect(body.toLowerCase()).not.toContain("github");
  expect(body.toLowerCase()).not.toContain("connector");
});

test("@s37 un token rechazado se explica junto al campo y lo vacía", async ({
  page,
}) => {
  await connectThrough(page, REPOSITORY.rejected);

  const token = page.getByLabel(/token/i);
  await expect(token).toHaveValue("");
  await expect(page.getByLabel(/repositorio/i)).toHaveValue(
    REPOSITORY.rejected,
  );
  await expect(page.getByText(/GitHub rechazó el token/)).toBeVisible();
  await expect(token).toBeFocused();
  await auditAxe(page, "error de conexión");
});
