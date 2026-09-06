import assert from "node:assert/strict";
import test from "node:test";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { readFileSync } from "node:fs";
import { createHash } from "node:crypto";
import * as commands from "./project.mjs";

const root = fileURLToPath(new URL("../", import.meta.url));
function capture() {
  const calls = [];
  // An injected runner keeps these tests from launching Gradle or Stryker.
  const project = commands.createProject((...args) => calls.push(args));
  return { calls, project };
}

test("today backend scope runs only its fixed PIT target", () => {
  const { calls, project } = capture();
  project("mutate", "today-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=today"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("today frontend scope runs only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "today-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.today.config.json",
      ],
    ],
  ]);
});

test("today replay runs only its fixed separate Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "today-frontend-replay");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.today.replay.config.json",
      ],
    ],
  ]);
});

test("today replay preserves 63 reviewed identities plus the new focus region without rewriting initial measurement", () => {
  const read = (path) => JSON.parse(readFileSync(resolve(root, path), "utf8"));
  const manifest = read("progress/today_frontend_replay_selection.json");
  const inventory = read("progress/mutation_today_frontend_inventory.json");
  const config = read("frontend/stryker.today.replay.config.json");
  assert.equal(
    manifest.originalReportSha256,
    "5cc335b97919aaa1bcd3cf4cf956af54ee55db3a02fdb23a060c77508f5c47a3",
  );
  assert.equal(manifest.selection.length, 63);
  assert.equal(
    new Set(manifest.selection.map((item) => item.originalId)).size,
    63,
  );
  assert.deepEqual(
    [...manifest.excludedIds].sort(),
    "78,85,89,93,209,215,216,219,222,253,259,291,336,338,350,351,371,411,435,487,493,506,507,508,509,510,511,517,518,519,520,521,522,523,529,530,531,532,533,534"
      .split(",")
      .sort(),
  );
  assert.equal(
    new Set([
      ...manifest.selection.map((item) => item.originalId),
      ...manifest.excludedIds,
    ]).size,
    103,
  );
  assert.deepEqual(
    manifest.selection.map((item) => item.originalId),
    inventory.pending
      .filter((item) => !manifest.excludedIds.includes(item.id))
      .map((item) => item.id),
  );
  assert.deepEqual(config.mutate, [
    ...new Set([
      ...manifest.selection.map((item) => item.range),
      manifest.newFocusRegion.range,
    ]),
  ]);
  assert.equal(manifest.newFocusRegion.originalId, null);
  assert.equal(manifest.newFocusRegion.range, "src/today.tsx:124:0-124:78");
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-today/replay.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-today/replay.html",
  );
  assert.deepEqual(config.thresholds, { high: 90, low: 80, break: 80 });
  assert.equal(config.coverageAnalysis, "perTest");
  assert.equal(config.concurrency, 2);
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-availability-replay"]);
  for (const item of manifest.selection) {
    assert.ok(["Survived", "NoCoverage"].includes(item.originalStatus));
    const original = inventory.pending.find(
      (entry) => entry.id === item.originalId,
    );
    assert.equal(item.sourceExpression, original.sourceExpression);
    assert.equal(item.replacement, original.replacement);
    assert.equal(item.operator, original.operator);
    assert.equal(item.originalStatus, original.status);
    assert.deepEqual(item.originalLocation, original.location);
    const source = readFileSync(
      resolve(root, "frontend", item.file),
      "utf8",
    ).replace(/\r\n/g, "\n");
    const offset = ({ line, column }) =>
      source
        .split("\n")
        .slice(0, line - 1)
        .reduce((sum, text) => sum + text.length + 1, 0) + column;
    assert.equal(
      source.slice(
        offset(item.mappedLocation.start),
        offset(item.mappedLocation.end),
      ),
      item.sourceExpression,
      item.originalId,
    );
  }
  for (const [file, hash] of Object.entries(manifest.sourceSha256))
    assert.equal(
      createHash("sha256")
        .update(readFileSync(resolve(root, "frontend", file)))
        .digest("hex"),
      hash,
      file,
    );
});

test("today configuration measures the frozen new code and changed shared regions", () => {
  const read = (path) => JSON.parse(readFileSync(resolve(root, path), "utf8"));
  const config = read("frontend/stryker.today.config.json");
  const full = read("frontend/stryker.config.json");
  const manifest = read("progress/today_frontend_mutation_scope.json");
  assert.deepEqual(config.thresholds, { high: 90, low: 80, break: 80 });
  assert.equal(config.coverageAnalysis, "perTest");
  assert.equal(config.concurrency, 2);
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-availability-replay"]);
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-today/mutation.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-today/mutation.html",
  );
  assert.deepEqual(config.mutate, manifest.mutate);
  assert.deepEqual(config.mutate, [
    "src/today-api.ts",
    "src/today.tsx",
    "src/App.tsx:10:0-49:1",
    "src/workspace.tsx:27:0-54:22",
    "src/workspace.tsx:85:0-85:64",
    "src/project-reader.tsx:61:0-61:70",
    "src/project-reader.tsx:157:0-157:76",
    "src/use-session.ts:29:0-29:43",
    "src/use-session.ts:189:0-189:36",
  ]);
  for (const [path, hash] of Object.entries(manifest.sourceSha256)) {
    assert.equal(
      createHash("sha256")
        .update(readFileSync(resolve(root, "frontend", path)))
        .digest("hex"),
      hash,
      path,
    );
  }
  for (const range of config.mutate) {
    const file = range.split(":")[0];
    assert.ok(full.mutate.includes(range) || full.mutate.includes(file), range);
  }
  assert.ok(!full.mutate.includes("src/App.tsx"));
  assert.ok(!full.mutate.includes("src/App.tsx:10:0-18:200"));
  assert.ok(!full.mutate.includes("src/workspace.tsx:72:0-72:200"));
});

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
    ["mutate", "today-backend;echo unsafe"],
    ["mutate", "today-frontend --config other.json"],
    ["test", "today-backend"],
    ["install", "today-frontend"],
    ["mutate", "today-frontend-replay --config other.json"],
    ["mutate", "today-frontend-final --config other.json"],
    ["test", "today-frontend-final"],
    ["test", "today-frontend-replay"],
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
    "reports/mutation-schedule-block/final.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-schedule-block/final.html",
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
test("final replay selects the reviewed outstanding identities without changing history", () => {
  const manifest = JSON.parse(
    readFileSync(
      resolve(root, "progress/schedule_block_frontend_final_selection.json"),
      "utf8",
    ),
  );
  const config = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.schedule-block.replay.config.json"),
      "utf8",
    ),
  );
  const ids = manifest.selection.map((item) => item.originalId);
  assert.equal(new Set(ids).size, 29);
  assert.deepEqual(
    [...ids].sort(),
    "746,931,1070,1303,1304,1401,1403,750,757,814,816,829,887,894,897,898,996,1124,1143,1154,1157,1186,1241,1248,1250,1267,1268,1269,1273"
      .split(",")
      .sort(),
  );
  assert.ok(
    manifest.selection.every(
      (item) =>
        item.replayStatus === "Survived" && item.replayId && item.sourceSha256,
    ),
  );
  assert.deepEqual(
    [...new Set(manifest.selection.map((item) => item.range))].sort(),
    [...config.mutate].sort(),
  );
  assert.notEqual(
    config.jsonReporter.fileName,
    manifest.previousReplayReport.replace(/^frontend\//, ""),
  );
  assert.equal(
    manifest.previousSelectionManifest,
    "progress/schedule_block_frontend_replay_selection.json",
  );
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
    ["mutate", "today-backend"],
    ["mutate", "today-frontend"],
    ["mutate", "today-frontend-replay"],
    ["mutate", "today-frontend-final"],
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

test("today final runs only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "today-frontend-final");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.today.final.config.json",
      ],
    ],
  ]);
});

test("today final selects only the two unresolved exact identities", () => {
  const read = (p) => JSON.parse(readFileSync(resolve(root, p), "utf8"));
  const config = read("frontend/stryker.today.final.config.json"),
    manifest = read("progress/today_frontend_final_selection.json"),
    prior = read("progress/today_frontend_replay_selection.json");
  assert.deepEqual(
    manifest.selection,
    prior.selection.filter((x) => ["337", "412"].includes(x.originalId)),
  );
  assert.deepEqual(manifest.priorReplayIds, ["74", "88"]);
  assert.deepEqual(config.mutate, [
    "src/today.tsx:61:8-61:13",
    "src/today.tsx:111:30-111:34",
  ]);
  assert.deepEqual(
    config.mutate,
    manifest.selection.map((x) => x.range),
  );
  assert.deepEqual(config.thresholds, { high: 90, low: 80, break: 80 });
  assert.equal(config.coverageAnalysis, "perTest");
  assert.equal(config.concurrency, 2);
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-availability-replay"]);
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-today/final.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-today/final.html",
  );
  assert.equal(
    manifest.sourceSha256,
    createHash("sha256")
      .update(readFileSync(resolve(root, "frontend/src/today.tsx")))
      .digest("hex"),
  );
  for (const x of manifest.selection) {
    const lines = readFileSync(resolve(root, "frontend", x.file), "utf8").split(
      /\r?\n/,
    );
    assert.equal(
      lines[x.mappedLocation.start.line - 1].slice(
        x.mappedLocation.start.column,
        x.mappedLocation.end.column,
      ),
      x.sourceExpression,
    );
  }
});
