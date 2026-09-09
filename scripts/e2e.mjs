import { mkdirSync, mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { join, resolve, relative, isAbsolute } from "node:path";
import { fileURLToPath } from "node:url";
import { run } from "./project.mjs";
const root = fileURLToPath(new URL("../", import.meta.url));
const scratchRoot = resolve(root, ".e2e-work");
mkdirSync(scratchRoot, { recursive: true });
const scratch = mkdtempSync(join(scratchRoot, "run-"));
const environmentFile = join(scratch, "test.env");
// E2E_WEB_PORT permite levantar varias pilas E2E en paralelo (worktrees).
const webPort = process.env.E2E_WEB_PORT ?? "18080";
if (!/^\d{2,5}$/.test(webPort)) throw new Error("Invalid E2E_WEB_PORT");
const baseUrl = `http://127.0.0.1:${webPort}`;
// Clave de conectores sólo para pruebas: 32 bytes en base64. Habilita las rutas del conector para
// que su E2E recorra los estados reales en lugar del 503 de "sin configurar".
const connectorKey = "ZTJlLW9ubHktY29ubmVjdG9yLWtleS0zMi1ieXRlcyE=";
// Puerto de descarte dentro del propio contenedor: cualquier salida hacia GitHub muere en el acto.
// Ninguna prueba de este repositorio habla con api.github.com.
const githubApiBase = "http://127.0.0.1:9";
writeFileSync(
  environmentFile,
  `DB_USERNAME=e2e_user\nDB_PASSWORD=e2e-only-database\nAPP_AUTH_USERNAME=e2e-user\nAPP_AUTH_PASSWORD=e2e-only-password\nWEB_PORT=${webPort}\nAPP_CONNECTOR_KEY=${connectorKey}\nAPP_GITHUB_API_BASE=${githubApiBase}\n`,
);
const project = `organizationweb-e2e-${process.pid}`;
const composeArgs = [
  "compose",
  "--env-file",
  environmentFile,
  "-p",
  project,
  "-f",
  resolve(root, "docker-compose.yml"),
];
const env = {
  ...process.env,
  DB_USERNAME: "e2e_user",
  DB_PASSWORD: "e2e-only-database",
  APP_AUTH_USERNAME: "e2e-user",
  APP_AUTH_PASSWORD: "e2e-only-password",
  WEB_PORT: webPort,
  APP_PUBLIC_ORIGIN: baseUrl,
  E2E_BASE_URL: process.env.E2E_BASE_URL ?? baseUrl,
  APP_MAX_ACTIVE_PROJECTS: "3",
  APP_CONNECTOR_KEY: connectorKey,
  APP_GITHUB_API_BASE: githubApiBase,
  E2E_COMPOSE_PROJECT: project,
  E2E_ENV_FILE: environmentFile,
};
try {
  run(
    "docker",
    [...composeArgs, "up", "--build", "-d", "--wait", "--wait-timeout", "180"],
    { env },
  );
  let ready = false;
  for (let attempt = 0; attempt < 90; attempt++) {
    try {
      const response = await fetch(`${baseUrl}/api/session`, {
        signal: AbortSignal.timeout(2000),
      });
      if (response.status === 200) {
        ready = true;
        break;
      }
    } catch {
      /* Service is still starting. */
    }
    await new Promise((resolveWait) => setTimeout(resolveWait, 1000));
  }
  if (!ready) throw new Error("API did not become ready");
  run(
    "pnpm",
    [
      "exec",
      "playwright",
      "test",
      ...process.argv.slice(2).filter((argument) => argument !== "--"),
    ],
    { env },
  );
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
} finally {
  try {
    run("docker", [...composeArgs, "down", "--volumes", "--remove-orphans"], {
      env,
    });
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
  const cleanupTarget = resolve(scratch);
  const withinScratch = relative(scratchRoot, cleanupTarget);
  if (
    !withinScratch ||
    withinScratch.startsWith("..") ||
    isAbsolute(withinScratch)
  ) {
    throw new Error("Refusing cleanup outside the E2E scratch directory");
  }
  rmSync(cleanupTarget, { recursive: true, force: true });
}
