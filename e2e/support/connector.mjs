import { expect } from "@playwright/test";
import { execFileSync } from "node:child_process";
import { readFileSync, writeFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { loginSession } from "../../scripts/session-client.mjs";

/**
 * Ayudas de la pila de E2E para el conector de GitHub.
 *
 * El estado «deshabilitado» de @s42 no se puede fingir desde la interfaz: depende de que el
 * servidor arranque **sin** `APP_CONNECTOR_KEY`. Aquí se recrea el backend con la clave vacía, se
 * recorre el estado de verdad y se restaura la pila, en lugar de simular un 503 desde el cliente.
 */

function fixture() {
  const project = process.env.E2E_COMPOSE_PROJECT;
  if (
    !/^organizationweb-e2e-\d+$/.test(project ?? "") ||
    !process.env.E2E_ENV_FILE
  )
    throw new Error("Requiere la pila aislada de E2E");
  return project;
}

/**
 * El entorno del proceso manda sobre `--env-file` en Docker Compose, y el arnés exporta
 * `APP_CONNECTOR_KEY` al lanzar Playwright. Por eso la clave se pasa **explícitamente** aquí: si
 * sólo se cambiara el fichero, la variable heredada ganaría y el backend seguiría habilitado.
 */
function compose(environmentFile, connectorKey, ...args) {
  return execFileSync(
    "docker",
    [
      "compose",
      "--env-file",
      environmentFile,
      "-p",
      fixture(),
      "-f",
      "docker-compose.yml",
      "--profile",
      "e2e",
      ...args,
    ],
    {
      encoding: "utf8",
      timeout: 180_000,
      env: { ...process.env, APP_CONNECTOR_KEY: connectorKey },
    },
  ).trim();
}

async function waitForBackend(request) {
  await expect
    .poll(
      async () => {
        try {
          return (
            await request.get("/api/session", { timeout: 2000 })
          ).status();
        } catch {
          return 0;
        }
      },
      { timeout: 90_000, intervals: [500, 1000] },
    )
    .toBe(200);
  const session = await (await request.get("/api/session")).json();
  if (!session.authenticated)
    await loginSession(request, {
      username: "e2e-user",
      password: "e2e-only-password",
    });
}

/**
 * Recrea el backend sin clave de conectores, ejecuta el recorrido y restaura la pila. El servicio
 * falso comparte el espacio de red del backend, así que se recrea con él en los dos sentidos.
 */
export async function withConnectorDisabled(request, run) {
  const original = process.env.E2E_ENV_FILE;
  const originalKey = /^APP_CONNECTOR_KEY=(.*)$/m.exec(
    readFileSync(original, "utf8"),
  )?.[1];
  if (!originalKey)
    throw new Error("La pila de E2E no declara APP_CONNECTOR_KEY");
  const disabled = join(dirname(original), "test.disabled.env");
  writeFileSync(
    disabled,
    readFileSync(original, "utf8").replace(
      /^APP_CONNECTOR_KEY=.*$/m,
      "APP_CONNECTOR_KEY=",
    ),
  );
  try {
    compose(
      disabled,
      "",
      "up",
      "-d",
      "--force-recreate",
      "backend",
      "github-fake",
    );
    await waitForBackend(request);
    await run();
  } finally {
    compose(
      original,
      originalKey,
      "up",
      "-d",
      "--force-recreate",
      "backend",
      "github-fake",
    );
    await waitForBackend(request);
  }
}
