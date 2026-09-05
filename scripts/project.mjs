import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";
const root = fileURLToPath(new URL("../", import.meta.url));
export function run(command, args, options = {}) {
  const windowsBatch =
    process.platform === "win32" && /(?:pnpm|gradlew)$/.test(command);
  const result = spawnSync(windowsBatch ? `${command}.cmd` : command, args, {
    cwd: root,
    stdio: "inherit",
    shell: windowsBatch,
    ...options,
  });
  if (result.error) throw result.error;
  if (result.status !== 0)
    throw new Error(`${command} exited with ${result.status}`);
}
export function project(task) {
  const backend = (taskName) =>
    run(
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      [taskName, "--no-daemon"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    );
  if (task === "install") {
    run("pnpm", ["install", "--frozen-lockfile"]);
    run("pnpm", ["--dir", "frontend", "install", "--frozen-lockfile"]);
    return;
  }
  const commands = {
    test: "test",
    build: "bootJar",
    lint: "spotlessCheck",
    mutate: "pitest",
  };
  if (!commands[task]) throw new Error(`Unknown task: ${task}`);
  if (task === "lint") {
    for (const file of [
      "scripts/project.mjs",
      "scripts/e2e.mjs",
      "playwright.config.mjs",
      "e2e/create-project.spec.mjs",
    ]) {
      run(process.execPath, ["--check", file]);
    }
  }
  backend(commands[task]);
  run("pnpm", ["--dir", "frontend", task]);
}
if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  try {
    project(process.argv[2]);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
