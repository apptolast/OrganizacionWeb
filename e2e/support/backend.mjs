import { expect } from "@playwright/test";
import { execFileSync } from "node:child_process";
import { loginSession } from "../../scripts/session-client.mjs";

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
      { timeout: 45_000, intervals: [500, 1000] },
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
