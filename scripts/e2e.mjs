import { mkdirSync, mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { join, resolve, relative, isAbsolute } from "node:path";
import { fileURLToPath } from "node:url";
import { run } from "./project.mjs";
const root = fileURLToPath(new URL("../", import.meta.url));
const scratchRoot = resolve(root, ".e2e-work");
mkdirSync(scratchRoot, { recursive: true });
const scratch = mkdtempSync(join(scratchRoot, "run-"));
const environmentFile = join(scratch, "test.env");
writeFileSync(
  environmentFile,
  "DB_USERNAME=e2e_user\nDB_PASSWORD=e2e-only-database\nAPP_AUTH_USERNAME=e2e-user\nAPP_AUTH_PASSWORD=e2e-only-password\nWEB_PORT=18080\n",
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
  WEB_PORT: "18080",
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
      const response = await fetch("http://127.0.0.1:18080/api/session", {
        headers: {
          Authorization:
            "Basic " +
            Buffer.from("e2e-user:e2e-only-password").toString("base64"),
        },
      });
      if (response.status === 204) {
        ready = true;
        break;
      }
    } catch {
      /* Service is still starting. */
    }
    await new Promise((resolveWait) => setTimeout(resolveWait, 1000));
  }
  if (!ready) throw new Error("API did not become ready");
  run("pnpm", ["exec", "playwright", "test"], { env });
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
