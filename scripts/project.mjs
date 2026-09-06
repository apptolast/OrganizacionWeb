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
export function createProject(runner = run) {
  return function project(task, target) {
    if (
      target !== undefined &&
      target !== "" &&
      (task !== "mutate" ||
        !["schedule_block-backend", "schedule_block-frontend"].includes(target))
    ) {
      throw new Error(`Invalid target: ${target}`);
    }
    const backend = (taskName, args = []) =>
      runner(
        process.platform === "win32" ? "gradlew.bat" : "./gradlew",
        [taskName, "--no-daemon", ...args],
        { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
      );
    if (task === "install") {
      runner("pnpm", ["install", "--frozen-lockfile"]);
      runner("pnpm", ["--dir", "frontend", "install", "--frozen-lockfile"]);
      return;
    }
    const commands = {
      test: "test",
      build: "bootJar",
      lint: "spotlessCheck",
      mutate: "pitest",
    };
    if (!commands[task]) throw new Error(`Unknown task: ${task}`);
    if (task === "mutate" && target === "schedule_block-backend") {
      backend("pitest", ["-PmutationScope=schedule_block"]);
      return;
    }
    if (task === "mutate" && target === "schedule_block-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "mutate",
        "--mutate",
        "src/schedule-block-api.ts,src/task-blocks.tsx,src/task-reader.tsx,src/task-state.tsx",
      ]);
      return;
    }
    if (task === "lint") {
      for (const file of [
        "scripts/project.mjs",
        "scripts/project.test.mjs",
        "scripts/e2e.mjs",
        "playwright.config.mjs",
        "e2e/create-project.spec.mjs",
      ]) {
        runner(process.execPath, ["--check", file]);
      }
    }
    if (task === "test")
      runner(process.execPath, ["--test", "scripts/project.test.mjs"]);
    backend(commands[task]);
    runner("pnpm", ["--dir", "frontend", task]);
  };
}
export const project = createProject();
export function main(args, projectRunner = project) {
  if (args.length > 2) throw new Error("Expected task and optional target");
  projectRunner(args[0], args[1]);
}
if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  try {
    main(process.argv.slice(2));
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
