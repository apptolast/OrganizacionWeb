import { expect } from "@playwright/test";
import { execFileSync } from "node:child_process";
import { loginSession } from "../../scripts/session-client.mjs";

/** Lo que este andamiaje se concede para que la API vuelva a contestar tras el reinicio. */
export const RESTART_READY_TIMEOUT_MS = 45_000;

export async function restartBackend(request) {
  const fixture = process.env.E2E_COMPOSE_PROJECT;
  if (
    !/^organizationweb-e2e-\d+$/.test(fixture ?? "") ||
    !process.env.E2E_ENV_FILE
  )
    throw new Error("Restart requires the isolated E2E fixture");
  const compose = (...args) =>
    execFileSync(
      "docker",
      [
        "compose",
        "--env-file",
        process.env.E2E_ENV_FILE,
        "-p",
        fixture,
        "-f",
        "docker-compose.yml",
        // El perfil e2e hace direccionable al servicio falso de GitHub, que comparte el espacio de
        // red del backend y por tanto no sobrevive por su cuenta a un reinicio de éste.
        "--profile",
        "e2e",
        ...args,
      ],
      { encoding: "utf8", timeout: 30_000 },
    ).trim();
  const inspect = (id) =>
    JSON.parse(
      execFileSync("docker", ["inspect", id], {
        encoding: "utf8",
        timeout: 10_000,
      }),
    )[0];
  const backendId = compose("ps", "-q", "backend");
  const databaseId = compose("ps", "-q", "postgres");
  const beforeBackend = inspect(backendId);
  const beforeDatabase = inspect(databaseId);
  compose("restart", "backend");
  // El servicio falso de GitHub vive **dentro** del espacio de red del backend
  // (`network_mode: service:backend`). Al reiniciarse el backend, Docker le crea un espacio de red
  // nuevo y el falso se queda hablando con el viejo: desde entonces el backend no lo alcanza en
  // 127.0.0.1:9000 y el conector responde 503. Se reinicia con él, igual que `withConnectorDisabled`
  // los recrea en los dos sentidos. Si la pila se levantó sin el perfil e2e, no hay nada que
  // reiniciar y el reinicio del backend basta.
  const fakeId = compose("ps", "-q", "github-fake");
  if (fakeId) compose("restart", "github-fake");
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
      { timeout: RESTART_READY_TIMEOUT_MS, intervals: [500, 1000] },
    )
    .toBe(200);
  const afterBackend = inspect(backendId);
  const afterDatabase = inspect(databaseId);
  expect(afterBackend.State.Running).toBe(true);
  expect(afterBackend.State.StartedAt).not.toBe(beforeBackend.State.StartedAt);
  expect(afterDatabase.State.StartedAt).toBe(beforeDatabase.State.StartedAt);
  expect(afterDatabase.Mounts).toEqual(beforeDatabase.Mounts);
  const session = await (await request.get("/api/session")).json();
  if (!session.authenticated)
    await loginSession(request, {
      username: "e2e-user",
      password: "e2e-only-password",
    });
  return {
    fixture,
    backendId,
    databaseId,
    beforeBackend,
    afterBackend,
    beforeDatabase,
    afterDatabase,
    session,
  };
}
