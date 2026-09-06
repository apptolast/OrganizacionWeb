import assert from "node:assert/strict";
import test from "node:test";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { readFileSync } from "node:fs";
import * as commands from "./project.mjs";

const root = fileURLToPath(new URL("../", import.meta.url));
function capture() {
  const calls = [];
  // An injected runner keeps these tests from launching Gradle or Stryker.
  const project = commands.createProject((...args) => calls.push(args));
  return { calls, project };
}

test("backend scope runs only the configured schedule_block PIT target", () => {
  const { calls, project } = capture();
  project("mutate", "schedule_block-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=schedule_block"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("frontend scope includes new code and both shared guards without changing thresholds", () => {
  const { calls, project } = capture();
  project("mutate", "schedule_block-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "mutate",
        "--mutate",
        "src/schedule-block-api.ts,src/task-blocks.tsx,src/task-reader.tsx,src/task-state.tsx",
      ],
    ],
  ]);
});

test("unknown or misplaced targets fail before starting any subprocess", () => {
  for (const [task, target] of [
    ["mutate", "other"],
    ["mutate", "schedule_block-backend;echo unsafe"],
    ["mutate", "schedule_block-frontend-replay --config other.json"],
    ["test", "schedule_block-backend"],
    ["install", "schedule_block-frontend"],
    ["test", "schedule_block-frontend-replay"],
  ]) {
    const { calls, project } = capture();
    assert.throws(() => project(task, target), /Invalid target/);
    assert.deepEqual(calls, []);
  }
});
test("frontend replay invokes only its fixed isolated report configuration", () => {
  const { calls, project } = capture();
  project("mutate", "schedule_block-frontend-replay");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.schedule-block.replay.config.json",
      ],
    ],
  ]);
});
test("frontend replay keeps measurement policy and writes separate reports", () => {
  const config = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.schedule-block.replay.config.json"),
      "utf8",
    ),
  );
  assert.deepEqual(config.thresholds, { high: 90, low: 80, break: 80 });
  assert.equal(config.coverageAnalysis, "perTest");
  assert.equal(config.concurrency, 2);
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-availability-replay"]);
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-schedule-block/replay.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-schedule-block/replay.html",
  );
  assert.deepEqual(config.reporters, ["clear-text", "json", "html"]);
  assert.ok(config.mutate.length > 0);
  for (const range of config.mutate)
    assert.match(
      range,
      /^src\/(schedule-block-api\.ts|task-blocks\.tsx|task-reader\.tsx|task-state\.tsx):\d+:\d+-\d+:\d+$/,
    );
});

test("absent or empty target preserves both full mutation suites", () => {
  for (const target of [undefined, ""]) {
    const { calls, project } = capture();
    project("mutate", target);
    assert.deepEqual(calls, [
      [
        process.platform === "win32" ? "gradlew.bat" : "./gradlew",
        ["pitest", "--no-daemon"],
        { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
      ],
      ["pnpm", ["--dir", "frontend", "mutate"]],
    ]);
  }
});

test("harness placeholder and CLI deliver the selected target unchanged", () => {
  const config = JSON.parse(
    readFileSync(resolve(root, "harness.config.json"), "utf8"),
  );
  assert.equal(
    config.commands.mutate,
    "node scripts/project.mjs mutate {{target}}",
  );
  for (const args of [
    ["mutate"],
    ["mutate", "schedule_block-backend"],
    ["mutate", "schedule_block-frontend"],
    ["mutate", "schedule_block-frontend-replay"],
  ]) {
    const calls = [];
    commands.main(args, (...values) => calls.push(values));
    assert.deepEqual(calls, [[args[0], args[1]]]);
  }
});

test("normal CI test and lint include the script regression", () => {
  const testing = capture();
  testing.project("test");
  assert.deepEqual(testing.calls[0], [
    process.execPath,
    ["--test", "scripts/project.test.mjs"],
  ]);
  assert.deepEqual(
    testing.calls.slice(1).map((call) => call[1]),
    [
      ["test", "--no-daemon"],
      ["--dir", "frontend", "test"],
    ],
  );
  const linting = capture();
  linting.project("lint");
  assert.ok(
    linting.calls.some(
      (call) =>
        call[0] === process.execPath &&
        call[1][0] === "--check" &&
        call[1][1] === "scripts/project.test.mjs",
    ),
  );
});

test("CLI rejects extra arguments instead of silently discarding them", () => {
  const calls = [];
  assert.throws(
    () =>
      commands.main(
        ["mutate", "schedule_block-backend", "unexpected"],
        (...args) => calls.push(args),
      ),
    /Expected task and optional target/,
  );
  assert.deepEqual(calls, []);
});
