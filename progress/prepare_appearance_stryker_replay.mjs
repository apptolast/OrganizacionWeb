import assert from "node:assert/strict";
import { readFileSync, writeFileSync } from "node:fs";
import { createHash } from "node:crypto";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";
const root = resolve(import.meta.dirname, "..");
const read = (p) =>
  JSON.parse(readFileSync(resolve(root, p), "utf8").replace(/^\uFEFF/, ""));
const digest = (p) =>
  createHash("sha256")
    .update(readFileSync(resolve(root, p)))
    .digest("hex")
    .toUpperCase();
const original = "progress/appearance_stryker_original/mutation.json";
assert.equal(
  digest(original),
  "56D3A57C3393400EFDD98CA463CFB2B0C0E5274D4E878793FBA15EBB49D60122",
);
const raw = read(original),
  base = read("frontend/stryker.appearance.config.json");
const api = read("progress/appearance_api_refinement_targets.json").targets;
const ids = new Set([
  ...api.map((t) => t.id),
  "352",
  "412",
  "441",
  "443",
  "527",
  "620",
  "623",
  "633",
  "642",
  "705",
  "707",
]);
const all = Object.entries(raw.files).flatMap(([file, f]) =>
  f.mutants.map((m) => ({ file, ...m })),
);
const targets = all
  .filter((m) => ids.has(m.id))
  .map(({ file, id, location, mutatorName, replacement, status }) => ({
    file,
    id,
    location,
    mutatorName,
    replacement,
    status,
  }));
assert.equal(targets.length, 31);
for (const a of api) {
  const t = targets.find((t) => t.id === a.id);
  for (const key of [
    "file",
    "location",
    "mutatorName",
    "replacement",
    "status",
  ])
    assert.deepEqual(t[key], a[key]);
}
const frozen = read("progress/appearance_refined_final_freeze.json");
assert.equal(frozen.length, 119);
for (const f of frozen)
  assert.equal(digest(f.path), f.sha256.toUpperCase(), f.path);
const compare = (a, b) => a.line - b.line || a.column - b.column;
const contains = (a, b) =>
  a.file === b.file &&
  compare(a.location.start, b.location.start) <= 0 &&
  compare(a.location.end, b.location.end) >= 0;
const sorted = [...targets].sort(
  (a, b) =>
    a.file.localeCompare(b.file) ||
    compare(a.location.start, b.location.start) ||
    compare(b.location.end, a.location.end),
);
const ranges = [];
for (const t of sorted) if (!ranges.some((r) => contains(r, t))) ranges.push(t);
const selector = (t) =>
  `${t.file}:${t.location.start.line}:${t.location.start.column - 1}-${t.location.end.line}:${t.location.end.column - 1}`;
await import(
  pathToFileURL(
    resolve(
      root,
      "frontend/node_modules/@stryker-mutator/core/dist/src/index.js",
    ),
  )
);
const { ProjectReader } = await import(
  pathToFileURL(
    resolve(
      root,
      "frontend/node_modules/@stryker-mutator/core/dist/src/fs/project-reader.js",
    ),
  )
);
for (const r of ranges) {
  assert.ok(
    base.mutate.some((p) => p === r.file || p.startsWith(r.file + ":")),
  );
  const actual = ProjectReader.prototype
    .filterMutatePattern([r.file], selector(r))
    .get(r.file).mutate[0];
  assert.deepEqual(actual, {
    start: {
      line: r.location.start.line - 1,
      column: r.location.start.column - 1,
    },
    end: { line: r.location.end.line - 1, column: r.location.end.column - 1 },
  });
}
for (const file of new Set(targets.map((t) => t.file)))
  assert.equal(
    readFileSync(resolve(root, "frontend", file), "utf8").replace(
      /\r\n/g,
      "\n",
    ),
    raw.files[file].source.replace(/\r\n/g, "\n"),
    file,
  );
const config = {
  ...base,
  mutate: ranges.map(selector),
  jsonReporter: {
    fileName: "reports/mutation-appearance-replay/mutation.json",
  },
  htmlReporter: {
    fileName: "reports/mutation-appearance-replay/mutation.html",
  },
  tempDirName: ".stryker-tmp-appearance-replay",
};
const included = all.filter((m) => ranges.some((r) => contains(r, m)));
const manifest = {
  originalSha256: digest(original),
  freeze119Sha256: digest("progress/appearance_refined_final_freeze.json"),
  targets: targets.map((t) => ({
    ...t,
    selector: selector(ranges.find((r) => contains(r, t))),
  })),
  selectors: config.mutate,
  originalMutantsContained: included.map(
    ({ file, id, status, location, mutatorName, replacement }) => ({
      file,
      id,
      status,
      location,
      mutatorName,
      replacement,
    }),
  ),
  note: "Prevalidation only; exact replay signatures and all extra outcomes must be verified after one authorized run.",
};
writeFileSync(
  resolve(root, "frontend/stryker.appearance-replay.config.json"),
  JSON.stringify(config, null, 2) + "\n",
);
writeFileSync(
  resolve(root, "progress/appearance_stryker_replay_targets.json"),
  JSON.stringify(manifest, null, 2) + "\n",
);
console.log(
  JSON.stringify({
    targets: targets.length,
    ranges: ranges.length,
    originalContained: included.length,
    freeze: frozen.length,
    parser: "installed ProjectReader.filterMutatePattern",
    campaignExecuted: false,
  }),
);
