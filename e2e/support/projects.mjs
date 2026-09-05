import { expect } from "@playwright/test";
import { execFileSync } from "node:child_process";
export function sql(statement) {
  const project = process.env.E2E_COMPOSE_PROJECT;
  if (!project?.startsWith("organizationweb-e2e-") || !process.env.E2E_ENV_FILE)
    throw new Error("Run using the isolated E2E fixture");
  return execFileSync(
    "docker",
    [
      "compose",
      "--env-file",
      process.env.E2E_ENV_FILE,
      "-p",
      project,
      "-f",
      "docker-compose.yml",
      "exec",
      "-T",
      "postgres",
      "psql",
      "-U",
      "e2e_user",
      "-d",
      "organization",
      "-tA",
      "-v",
      "ON_ERROR_STOP=1",
      "-c",
      statement,
    ],
    { encoding: "utf8", timeout: 15_000 },
  ).trim();
}
export async function create(
  request,
  name,
  description = "Descripción conservada",
) {
  const response = await request.post("/api/v1/projects", {
    data: { name, description },
  });
  expect(response.status()).toBe(201);
  return response.json();
}
