// Red de regresion del troceado de la campana de mutacion y de su veredicto.
// Los veredictos se prueban contra repositorios de juguete en directorios
// temporales reales (build.gradle.kts, fuentes .java, stryker.config.json y
// harness.config.json), con artefactos construidos igual que los deja
// `run`. Ninguno de estos tests lanza Gradle ni Stryker.
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import {
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import {
  backendUniverse,
  elsePatterns,
  frontendPartition,
  frontendUnits,
  parsePitXml,
  parseShard,
  parseShardCount,
  partition,
  pitGlobToRegExp,
  pitName,
  pitScore,
  plan,
  shardStrykerConfig,
  strykerName,
  verdict,
} from "./mutation-shards.mjs";

const root = fileURLToPath(new URL("../", import.meta.url));
const sha256 = (data) => createHash("sha256").update(data).digest("hex");
const SHA = "0123456789abcdef0123456789abcdef01234567";

// Doble de prueba de calculateMutationTestMetrics (la formula de
// mutation-testing-metrics 3.8.4). El veredicto real carga el paquete de
// Stryker y NUNCA cae a esta funcion.
const metricsDouble = (report) => {
  const count = { Killed: 0, Timeout: 0, Survived: 0, NoCoverage: 0 };
  for (const file of Object.values(report.files))
    for (const m of file.mutants) if (m.status in count) count[m.status]++;
  const detected = count.Killed + count.Timeout;
  const valid = detected + count.Survived + count.NoCoverage;
  return {
    systemUnderTestMetrics: {
      metrics: { mutationScore: valid > 0 ? (detected / valid) * 100 : NaN },
    },
  };
};

// Pseudoaleatorio con semilla: los tests son reproducibles.
function shuffled(items, seed) {
  const out = [...items];
  let s = seed;
  for (let i = out.length - 1; i > 0; i--) {
    s = (s * 1103515245 + 12345) % 2147483648;
    const j = s % (i + 1);
    [out[i], out[j]] = [out[j], out[i]];
  }
  return out;
}

// ── Troceado ────────────────────────────────────────────────────────────────

test("partition is disjoint, complete and order-independent for every N up to |U|", () => {
  const universe = Array.from({ length: 23 }, (_, i) => `c${String(i).padStart(2, "0")}`);
  for (let n = 1; n <= universe.length; n++) {
    const shards = partition(universe, n);
    assert.equal(shards.length, n);
    const flat = shards.flat();
    assert.equal(flat.length, universe.length);
    assert.deepEqual([...flat].sort(), [...universe].sort());
    assert.equal(new Set(flat).size, universe.length);
    const sizes = shards.map((s) => s.length);
    assert.ok(Math.max(...sizes) - Math.min(...sizes) <= 1);
    assert.ok(sizes.every((size) => size > 0));
    for (const seed of [1, 7, 42])
      assert.deepEqual(partition(shuffled(universe, seed), n), shards);
  }
  assert.deepEqual(partition(["b", "a", "c", "d"], 2), [
    ["a", "c"],
    ["b", "d"],
  ]);
});

test("partition refuses N beyond the universe, N below one and repeated items", () => {
  assert.throws(() => partition(["a", "b"], 3), /mayor que el universo/);
  assert.throws(() => partition(["a"], 0), /N invalido/);
  assert.throws(() => partition(["a"], 1.5), /N invalido/);
  assert.throws(() => partition(["a", "a"], 1), /repetido/);
  assert.throws(() => partition([], 1), /mayor que el universo/);
});

test("malformed shard strings are rejected instead of degrading to everything", () => {
  for (const bad of ["0/3", "4/3", "1/0", "01/3", "1/03", "a/b", "1/3 ", " 1/3", "", "1", "1/3/4", "-1/3", "1.0/3", undefined, 3])
    assert.throws(() => parseShard(bad), /Trozo invalido/, String(bad));
  assert.deepEqual(parseShard("3/3"), { k: 3, n: 3 });
  assert.deepEqual(parseShard("1/12"), { k: 1, n: 12 });
  for (const bad of ["0", "21", "06", "6 ", "", "x", undefined])
    assert.throws(() => parseShardCount(bad), /invalido/, String(bad));
  assert.equal(parseShardCount("20"), 20);
});

test("PIT globs convert exactly like org.pitest.util.Glob and refuse what it treats specially", () => {
  assert.ok(pitGlobToRegExp("a.b.Task").test("a.b.Task"));
  assert.ok(!pitGlobToRegExp("a.b.Task").test("a.b.Task$Page"));
  assert.ok(!pitGlobToRegExp("a.b.Task").test("a.b.TaskController"));
  assert.ok(!pitGlobToRegExp("a.b.Task").test("aXb.Task"));
  assert.ok(pitGlobToRegExp("a.b.Task*").test("a.b.TaskController"));
  assert.ok(pitGlobToRegExp("a.b.*").test("a.b.c.D"));
  assert.ok(pitGlobToRegExp("a.b.Tas?").test("a.b.Task"));
  assert.ok(pitGlobToRegExp("a.B$*").test("a.B$C"));
  for (const bad of ["~a.*", "a.**.B", "a+b", "a[b]", "a(b)", "a\\b", "a b", ""])
    assert.throws(() => pitGlobToRegExp(bad), /alfabeto/, bad);
});

test("frontend units keep every range of a file together and preserve all entries", () => {
  const config = {
    mutate: ["src/z.ts", "src/gate.tsx:22:0-31:100", "src/a.ts", "src/gate.tsx:44:10-44:37"],
  };
  assert.deepEqual(frontendUnits(config), [
    { file: "src/a.ts", entries: ["src/a.ts"] },
    { file: "src/gate.tsx", entries: ["src/gate.tsx:22:0-31:100", "src/gate.tsx:44:10-44:37"] },
    { file: "src/z.ts", entries: ["src/z.ts"] },
  ]);
  for (let n = 1; n <= 3; n++) {
    const parts = frontendPartition(config, n);
    const withGate = parts.filter((p) => p.entries.some((e) => e.startsWith("src/gate.tsx")));
    assert.equal(withGate.length, 1);
    assert.equal(withGate[0].entries.filter((e) => e.startsWith("src/gate.tsx")).length, 2);
    assert.deepEqual(parts.flatMap((p) => p.entries).sort(), [...config.mutate].sort());
  }
  for (const bad of [["!src/a.ts"], ["src/*.ts"], ["src/{a,b}.ts"], [""], []])
    assert.throws(() => frontendUnits({ mutate: bad }), /stryker/);
});

test("the real Stryker default splits into 53 files and keeps its 54 entries for every N", () => {
  const config = JSON.parse(readFileSync(resolve(root, "frontend/stryker.config.json"), "utf8"));
  const units = frontendUnits(config);
  assert.equal(new Set(units.map((u) => u.file)).size, units.length);
  assert.equal(units.flatMap((u) => u.entries).length, config.mutate.length);
  const gate = units.find((u) => u.file === "src/session-gate.tsx");
  assert.deepEqual(gate.entries, config.mutate.filter((e) => e.startsWith("src/session-gate.tsx")));
  for (const n of [1, 6, 12, 20]) {
    const parts = frontendPartition(config, n);
    assert.deepEqual(parts.flatMap((p) => p.entries).sort(), [...config.mutate].sort());
    assert.deepEqual(parts.flatMap((p) => p.files).sort(), units.map((u) => u.file));
  }
});

test("a shard Stryker config differs from the default only in mutate and thresholds.break", () => {
  const base = JSON.parse(readFileSync(resolve(root, "frontend/stryker.config.json"), "utf8"));
  const entries = frontendPartition(base, 12)[4].entries;
  const shard = shardStrykerConfig(base, entries);
  assert.deepEqual(shard.mutate, entries);
  assert.equal(shard.thresholds.break, null);
  assert.equal(shard.thresholds.high, base.thresholds.high);
  assert.equal(shard.thresholds.low, base.thresholds.low);
  const strip = (c) => {
    const copy = structuredClone(c);
    delete copy.mutate;
    delete copy.thresholds.break;
    return copy;
  };
  assert.deepEqual(strip(shard), strip(base));
  assert.equal(shard.concurrency, base.concurrency);
  assert.equal(shard.coverageAnalysis, base.coverageAnalysis);
  assert.equal(typeof base.thresholds.break, "number", "the base config is not mutated");
  assert.throws(() => shardStrykerConfig({ thresholds: { break: null } }, []), /break/);
});

test("pitScore reproduces PIT 1.22.0 PercentageCalculator including float rounding", () => {
  assert.equal(pitScore(0, 0), 100);
  assert.equal(pitScore(10, 0), 0);
  assert.equal(pitScore(10, 10), 100);
  assert.equal(pitScore(1000, 999), 99);
  assert.equal(pitScore(1000, 795), 80);
  assert.equal(pitScore(1000, 794), 79);
  assert.equal(pitScore(200, 159), 80);
  assert.equal(pitScore(3, 1), 33);
  assert.equal(pitScore(3, 2), 67);
  assert.equal(pitScore(7, 5), 71);
  assert.equal(pitScore(35, 28), 80);
});

// ── Universo real ───────────────────────────────────────────────────────────

test("the real backend universe is the else branch: core wildcards whole, nested types never owners", () => {
  const u = backendUniverse(root);
  assert.ok(u.patterns.includes("com.apptolast.organization.domain.*"));
  assert.ok(u.patterns.includes("com.apptolast.organization.application.*"));
  assert.equal(u.threshold, 80);
  assert.equal(u.pitVersion, "1.22.0");
  assert.equal(u.patternsSha256, sha256(u.patterns.join("\n")));
  assert.ok(u.owners.every((o) => !o.includes("$")));
  // Todo domain/application entra por el comodin de `core`, que es lo que ningun
  // ambito con nombre reproduce (progress/medicion_ambito_mutacion.md).
  assert.ok(u.owners.includes("com.apptolast.organization.domain.Project"));
  assert.ok(u.owners.includes("com.apptolast.organization.application.CreateTaskUseCase"));
  assert.ok(u.owners.includes("com.apptolast.organization.domain.ValidationException"));
  assert.ok(u.owners.includes("com.apptolast.organization.adapter.http.TaskController"));
  assert.ok(!u.owners.includes("com.apptolast.organization.OrganizationApplication"));
  const p = plan(root, 6, 12);
  assert.equal(p.backend.length, 6);
  assert.equal(p.frontend.length, 12);
  assert.deepEqual(p.backend[0], { shard: "1/6", name: "pit-shard-1-of-6" });
  assert.deepEqual(p.frontend[11], { shard: "12/12", name: "stryker-shard-12-of-12" });
});

test("the else parser rejects shapes it cannot evaluate instead of returning less", () => {
  const block = (elseLine, extra = "") =>
    `pitest {\n    val core = setOf("a.*")\n${extra}    targetClasses.set(when {\n        ${elseLine}\n    })\n    targetTests.set(when {\n        else -> core\n    })\n    mutationThreshold.set(80)\n}\n`;
  assert.deepEqual(elsePatterns(block("else -> core")), ["a.*"]);
  assert.throws(() => elsePatterns(block("else -> core + missing")), /desconocido/);
  assert.throws(() => elsePatterns(block("else -> core.map { it }")), /no soportado/);
  assert.throws(
    () => elsePatterns(block("else -> core + other", '    val other = setOf(prefix + "b")\n')),
    /no literales|no soportado/,
  );
  assert.throws(() => elsePatterns("plugins {}\n"), /pitest/);
});

// ── Workflow y Gradle, juntos o ninguno ─────────────────────────────────────

test("the threshold-free shards and the mandatory verdict exist together", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const workflow = readFileSync(resolve(root, ".github/workflows/harness-mutation.yml"), "utf8");
  assert.ok(build.includes("        mutationThreshold.set(0)"));
  const verdictJob = workflow.slice(workflow.indexOf("\n  verdict:\n"));
  assert.ok(workflow.includes("\n  verdict:\n"), "verdict job");
  assert.match(verdictJob, /\n {4}needs: \[plan, init, backend-shard, frontend-shard\]\n/);
  assert.match(verdictJob, /\n {4}if: always\(\)\n/);
  assert.match(verdictJob, /node scripts\/mutation-shards\.mjs verdict --artifacts artifacts/);
  assert.match(verdictJob, /needs\.backend-shard\.result/);
  assert.match(verdictJob, /needs\.frontend-shard\.result/);
  assert.match(workflow, /node scripts\/mutation-shards\.mjs run backend "\$SHARD"/);
  assert.match(workflow, /node scripts\/mutation-shards\.mjs run frontend "\$SHARD"/);
  assert.match(workflow, /fail-fast: false/);
  assert.equal((workflow.match(/if-no-files-found: error/g) || []).length, 2);
  // Sin cron hasta que la calibracion demuestre que cada trozo cabe.
  assert.doesNotMatch(workflow, /^\s*schedule:/m);
  // Kotlin y Node admiten el mismo alfabeto de patrones.
  assert.ok(build.includes('Regex("^[A-Za-z0-9_.\\$*?]+\\$")'));
});

// ── Veredicto contra un repositorio de juguete ──────────────────────────────

const FIXTURE_BUILD = `plugins { java }
pitest {
    pitestVersion.set("1.22.0")
    val scope = providers.gradleProperty("mutationScope").orNull
    val webOnly = scope == "web"
    val core = setOf("com.acme.domain.*")
    val webClasses = setOf(
        // comentario que el parser ignora
        "com.acme.adapter.Web",
        "com.acme.adapter.Api*"
    )
    val webTests = setOf("com.acme.*")
    targetClasses.set(when {
        webOnly -> webClasses
        else -> core + webClasses
    })
    targetTests.set(when {
        webOnly -> webTests
        else -> core + webTests.filter { !it.contains("broker") }
    })
    mutationThreshold.set(80)
    threads.set(4)
}
`;
const FIXTURE_CLASSES = [
  "com/acme/domain/A",
  "com/acme/domain/B",
  "com/acme/domain/C",
  "com/acme/domain/D",
  "com/acme/adapter/ApiClient",
  "com/acme/adapter/ApiServer",
  "com/acme/adapter/Web",
  "com/acme/adapter/WebHelper",
  "com/acme/Outside",
];
const FIXTURE_STRYKER = {
  testRunner: "vitest",
  mutate: ["src/a.ts", "src/gate.tsx:1:0-2:10", "src/b.ts", "src/gate.tsx:5:0-5:9", "src/c.tsx"],
  thresholds: { high: 90, low: 80, break: 80 },
  concurrency: 2,
  coverageAnalysis: "perTest",
};

function write(path, content) {
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, content);
}

function fixtureRepo(t) {
  const dir = mkdtempSync(join(tmpdir(), "mutation-shards-"));
  t.after(() => rmSync(dir, { recursive: true, force: true }));
  write(join(dir, "backend/build.gradle.kts"), FIXTURE_BUILD);
  for (const c of FIXTURE_CLASSES)
    write(join(dir, "backend/src/main/java", `${c}.java`), `class ${c.split("/").pop()} {}\n`);
  write(join(dir, "backend/src/main/java/com/acme/domain/package-info.java"), "package com.acme.domain;\n");
  write(join(dir, "frontend/stryker.config.json"), `${JSON.stringify(FIXTURE_STRYKER, null, 2)}\n`);
  write(join(dir, "harness.config.json"), JSON.stringify({ mutation: { threshold: 0.8, targets: [] } }));
  return dir;
}

const xmlMutation = ({ cls, method = "run", line = 1, index = 1, status = "KILLED", detected }) =>
  `<mutation detected='${detected ?? ["KILLED", "TIMED_OUT", "NON_VIABLE", "MEMORY_ERROR", "RUN_ERROR"].includes(status)}' status='${status}' numberOfTestsRun='1'><sourceFile>X.java</sourceFile><mutatedClass>${cls}</mutatedClass><mutatedMethod>${method}</mutatedMethod><methodDescription>(Ljava/lang/String;)V</methodDescription><lineNumber>${line}</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator</mutator><indexes><index>${index}</index></indexes><blocks><block>0</block></blocks><killingTest/><description>removed call</description></mutation>`;

const pitXml = (mutations) =>
  `<?xml version="1.0" encoding="UTF-8"?>\n<mutations partial="false">\n${mutations.map(xmlMutation).join("\n")}\n</mutations>\n`;

// 4 detectados + 1 superviviente por clase: 80 %, justo en el umbral.
const pitMutationsFor = (owners) =>
  owners.flatMap((cls) => [
    { cls, line: 1, status: "KILLED" },
    { cls: `${cls}$Inner`, line: 2, status: "TIMED_OUT" },
    { cls, method: "&lt;init&gt;", line: 3, status: "KILLED" },
    { cls, line: 4, status: "RUN_ERROR" },
    { cls, line: 5, status: "SURVIVED" },
  ]);

const strykerMutants = (file) =>
  ["Killed", "Killed", "Timeout", "Killed", "Survived"].map((status, i) => ({
    id: `${file}-${i}`,
    mutatorName: "StringLiteral",
    replacement: `"m${i}"`,
    location: { start: { line: i + 1, column: 0 }, end: { line: i + 1, column: 5 } },
    status,
  }));

// Construye los artefactos tal y como los deja `run`, con ganchos para
// estropear cada pieza.
function buildArtifacts(repo, { nb = 3, nf = 2, tamper = {} } = {}) {
  const artifacts = join(repo, "artifacts");
  const u = backendUniverse(repo);
  const parts = partition(u.owners, nb);
  parts.forEach((owners, i) => {
    const k = i + 1;
    const name = pitName(k, nb);
    let manifest = {
      tool: "pit",
      k,
      N: nb,
      owners,
      ownersTotal: u.owners.length,
      patternsSha256: u.patternsSha256,
      excludedCount: 2 * (u.owners.length - owners.length),
      pitestVersion: "1.22.0",
    };
    let mutations = pitMutationsFor(owners);
    let done = { tool: "pit", k, N: nb, exitCode: 0, sha: SHA, startedAt: "2026-09-11T00:00:00Z", endedAt: "2026-09-11T01:30:00Z" };
    ({ manifest, mutations, done } = tamper.pit?.({ k, manifest, mutations, done }) ?? { manifest, mutations, done });
    const xml = tamper.pitXml?.({ k, xml: pitXml(mutations) }) ?? pitXml(mutations);
    const manifestText = `${JSON.stringify(manifest)}\n`;
    write(join(artifacts, name, "manifest.json"), manifestText);
    write(join(artifacts, name, "report/mutations.xml"), xml);
    write(
      join(artifacts, name, "done.json"),
      JSON.stringify({ ...done, reportSha256: sha256(xml), manifestSha256: sha256(manifestText), ...tamper.pitDone?.(k) }),
    );
  });
  const baseBytes = readFileSync(join(repo, "frontend/stryker.config.json"));
  const base = JSON.parse(baseBytes);
  frontendPartition(base, nf).forEach((part, i) => {
    const k = i + 1;
    const name = strykerName(k, nf);
    let manifest = { tool: "stryker", k, N: nf, files: part.files, entries: part.entries, configSha256: sha256(baseBytes) };
    let files = Object.fromEntries(
      part.files.map((f) => [f, { language: "typescript", source: "", mutants: strykerMutants(f) }]),
    );
    let done = { tool: "stryker", k, N: nf, exitCode: 0, sha: SHA, startedAt: "2026-09-11T00:00:00Z", endedAt: "2026-09-11T02:00:00Z" };
    ({ manifest, files, done } = tamper.stryker?.({ k, manifest, files, done }) ?? { manifest, files, done });
    const json = JSON.stringify({ schemaVersion: "2", thresholds: { high: 90, low: 80 }, files });
    const manifestText = `${JSON.stringify(manifest)}\n`;
    write(join(artifacts, name, "manifest.json"), manifestText);
    write(join(artifacts, name, "report/mutation.json"), json);
    write(join(artifacts, name, "done.json"), JSON.stringify({ ...done, reportSha256: sha256(json), manifestSha256: sha256(manifestText) }));
  });
  return artifacts;
}

const judge = (repo, artifacts, extra = {}) =>
  verdict({
    root: repo,
    artifacts,
    backendShards: 3,
    frontendShards: 2,
    sha: SHA,
    calculateMutationTestMetrics: metricsDouble,
    ...extra,
  });

function assertRed(result, pattern) {
  assert.equal(result.ok, false, "the verdict must be red");
  assert.ok(
    result.errors.some((e) => pattern.test(e)),
    `expected ${pattern} in:\n${result.errors.join("\n")}`,
  );
}

test("fixture universe: exact patterns do not swallow siblings and package-info is not a class", (t) => {
  const repo = fixtureRepo(t);
  const u = backendUniverse(repo);
  assert.deepEqual(u.owners, [
    "com.acme.adapter.ApiClient",
    "com.acme.adapter.ApiServer",
    "com.acme.adapter.Web",
    "com.acme.domain.A",
    "com.acme.domain.B",
    "com.acme.domain.C",
    "com.acme.domain.D",
  ]);
  assert.deepEqual(u.patterns, ["com.acme.adapter.Api*", "com.acme.adapter.Web", "com.acme.domain.*"]);
});

test("verdict is green when every shard is present, complete, disjoint and above threshold", (t) => {
  const repo = fixtureRepo(t);
  const result = judge(repo, buildArtifacts(repo));
  assert.deepEqual(result.errors, []);
  assert.equal(result.ok, true);
  assert.deepEqual(result.backend, { total: 35, detected: 28, score: 80, threshold: 80 });
  assert.equal(result.frontend.score, 80);
  assert.equal(result.rows.length, 5);
});

test("PIT XML is parsed with entities decoded and a truncated file is refused", () => {
  const [m] = parsePitXml(pitXml([{ cls: "a.B$C", method: "&lt;init&gt;", status: "SURVIVED" }]));
  assert.equal(m.mutatedClass, "a.B$C");
  assert.equal(m.mutatedMethod, "<init>");
  assert.equal(m.detected, false);
  assert.deepEqual(m.indexes, [1]);
  const full = pitXml([{ cls: "a.B" }]);
  assert.throws(() => parsePitXml(full.replace("</mutations>\n", "")), /incompleto/);
  assert.throws(() => parsePitXml(full.replace("</mutations>", "<junk/></mutations>")), /no reconocido/);
});

const RED_CASES = [
  [
    "a missing artifact",
    (repo, a) => rmSync(join(a, pitName(2, 3)), { recursive: true }),
    /pit-shard-2-of-3: falta el artefacto/,
  ],
  [
    "an extra artifact",
    (repo, a) => mkdirSync(join(a, pitName(1, 2))),
    /Artefacto inesperado: pit-shard-1-of-2/,
  ],
  ["a shard run on another commit", { pit: (x) => ({ ...x, done: { ...x.done, sha: "f".repeat(40) } }) }, /se ejecuto sobre/],
  ["a shard whose mutator failed", { pit: (x) => (x.k === 3 ? { ...x, done: { ...x.done, exitCode: 1 } } : x) }, /salio con 1/],
  [
    "a mutation counted in two shards",
    { pit: (x) => (x.k === 2 ? { ...x, mutations: [...x.mutations, ...pitMutationsFor(["com.acme.adapter.ApiClient"]).slice(0, 1)] } : x) },
    /mutante repetido|otro trozo/,
  ],
  [
    "a class leaking into another shard",
    // Reparto de 7 en 3: el trozo 1 es ApiClient, A y D; B es del trozo 2.
    { pit: (x) => (x.k === 1 ? { ...x, mutations: [...x.mutations, { cls: "com.acme.domain.B$Leak", line: 99 }] } : x) },
    /com\.acme\.domain\.B\$Leak, que es de otro trozo/,
  ],
  [
    "a mutated class outside the universe",
    { pit: (x) => ({ ...x, mutations: [...x.mutations, { cls: "com.acme.Outside", line: 7 + x.k }] }) },
    /com\.acme\.Outside, fuera del universo/,
  ],
  [
    "an unfinished PIT mutant",
    { pit: (x) => (x.k === 1 ? { ...x, mutations: [{ ...x.mutations[0], status: "STARTED" }, ...x.mutations.slice(1)] } : x) },
    /STARTED .*campana incompleta/,
  ],
  [
    "a detected flag that contradicts the status",
    { pit: (x) => (x.k === 1 ? { ...x, mutations: [{ ...x.mutations[0], detected: false }, ...x.mutations.slice(1)] } : x) },
    /no casa con KILLED/,
  ],
  [
    "a manifest that is not the recomputed partition",
    {
      pit: (x) =>
        x.k === 1 ? { ...x, manifest: { ...x.manifest, owners: [...x.manifest.owners].slice(1) } } : x,
    },
    /no son el reparto recalculado/,
  ],
  [
    "a Gradle universe different from the recomputed else branch",
    { pit: (x) => ({ ...x, manifest: { ...x.manifest, patternsSha256: "0".repeat(64) } }) },
    /rama else recalculada/,
  ],
  [
    "universe drift: a class added after the shards ran",
    (repo) => write(join(repo, "backend/src/main/java/com/acme/domain/E.java"), "class E {}\n"),
    /reparto recalculado|universo de 7 clases/,
  ],
  [
    "a tampered report",
    (repo, a) => write(join(a, pitName(1, 3), "report/mutations.xml"), pitXml([])),
    /informe no es el que registro/,
  ],
  [
    "a missing report",
    (repo, a) => rmSync(join(a, strykerName(2, 2), "report"), { recursive: true }),
    /stryker-shard-2-of-2: falta report\/mutation\.json/,
  ],
  [
    "a backend score below the threshold",
    {
      pit: (x) =>
        x.k === 1 ? { ...x, mutations: x.mutations.map((m) => ({ ...m, status: "SURVIVED" })) } : x,
    },
    /backend: puntuacion PIT \d+ por debajo del umbral 80/,
  ],
  [
    "threshold drift between Gradle, Stryker and the harness",
    (repo) => write(join(repo, "harness.config.json"), JSON.stringify({ mutation: { threshold: 0.7, targets: [] } })),
    /Deriva de umbral/,
  ],
  [
    "a Gradle threshold that is not a single literal",
    (repo) =>
      write(
        join(repo, "backend/build.gradle.kts"),
        FIXTURE_BUILD.replace("    mutationThreshold.set(80)\n", "    mutationThreshold.set(80)\n    mutationThreshold.set(70)\n"),
      ),
    /No se puede recalcular el universo: .*mutationThreshold/,
  ],
  [
    "another PIT version",
    (repo) => write(join(repo, "backend/build.gradle.kts"), FIXTURE_BUILD.replace('"1.22.0"', '"1.23.0"')),
    /PIT 1\.23\.0/,
  ],
  [
    "a Stryker file reported by the wrong shard",
    // Reparto de 4 ficheros en 2: el trozo 1 es a.ts y c.tsx; b.ts es del trozo 2.
    { stryker: (x) => (x.k === 1 ? { ...x, files: { ...x.files, "src/b.ts": { mutants: [] } } } : x) },
    /informa de src\/b\.ts, que no es de este trozo/,
  ],
  [
    "a pending Stryker mutant",
    {
      stryker: (x) => {
        const [file] = Object.keys(x.files);
        const mutants = x.files[file].mutants.map((m, i) => (i === 0 ? { ...m, status: "Pending" } : m));
        return { ...x, files: { ...x.files, [file]: { ...x.files[file], mutants } } };
      },
    },
    /Pending/,
  ],
  [
    "a frontend manifest that drops one session-gate range",
    {
      stryker: (x) =>
        x.manifest.files.includes("src/gate.tsx")
          ? { ...x, manifest: { ...x.manifest, entries: x.manifest.entries.filter((e) => e !== "src/gate.tsx:5:0-5:9") } }
          : x,
    },
    /no son el reparto recalculado|entradas de los manifiestos/,
  ],
  [
    "a frontend score below break",
    {
      stryker: (x) => ({
        ...x,
        files: Object.fromEntries(
          Object.entries(x.files).map(([f, d]) => [f, { ...d, mutants: d.mutants.map((m) => ({ ...m, status: "Survived" })) }]),
        ),
      }),
    },
    /frontend: puntuacion Stryker 0\.00 por debajo de break 80/,
  ],
];

for (const [label, spoil, expected] of RED_CASES)
  test(`verdict goes red on ${label}`, (t) => {
    const repo = fixtureRepo(t);
    const artifacts = buildArtifacts(repo, { tamper: typeof spoil === "function" ? {} : spoil });
    if (typeof spoil === "function") spoil(repo, artifacts);
    assertRed(judge(repo, artifacts), expected);
  });

test("verdict goes red when no artifact was downloaded at all or N disagrees", (t) => {
  const repo = fixtureRepo(t);
  const artifacts = buildArtifacts(repo);
  assertRed(judge(repo, join(repo, "nothing-here")), /falta el artefacto/);
  assertRed(judge(repo, artifacts, { backendShards: 4 }), /Artefacto inesperado: pit-shard-1-of-3/);
  assertRed(judge(repo, artifacts, { backendShards: 8 }), /Reparto imposible/);
  assertRed(judge(repo, artifacts, { sha: "HEAD" }), /--sha invalido/);
  const noMetrics = judge(repo, artifacts, { calculateMutationTestMetrics: undefined });
  assertRed(noMetrics, /frontend: mutation-testing-metrics fallo \(falta calculateMutationTestMetrics\)/);
  // El resto de comprobaciones sigue corriendo: la puerta de backend se calcula.
  assert.deepEqual(noMetrics.backend, { total: 35, detected: 28, score: 80, threshold: 80 });
});
