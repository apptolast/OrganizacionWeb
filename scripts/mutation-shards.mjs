// Troceado mecanico de la campana completa de mutacion, y su veredicto.
//
// La campana completa (`harness verify` sin objetivo) muta el universo de la
// rama `else` de `targetClasses` en backend/build.gradle.kts y los ficheros de
// frontend/stryker.config.json. No cabe en un job hospedado (ver la cabecera de
// .github/workflows/harness-mutation.yml y progress/troceado_mutacion.md), asi
// que en CI se reparte en N trozos. Este fichero es la unica pieza de Node que
// decide ese reparto y la unica que dice si la suma de los trozos equivale a la
// campana entera.
//
// Tres reglas que no se negocian:
//
// 1. El reparto es FUNCION del universo, no una lista escrita a mano: se ordena
//    el universo y el trozo k de N se lleva las posiciones i con i % N == k-1.
//    La union de los trozos es el universo por construccion.
// 2. Cada trozo corre SIN umbral propio (PIT mutationThreshold 0, Stryker
//    thresholds.break null). El umbral es el de hoy, sobre el agregado, y lo
//    aplica `verdict`. Sin el job de veredicto los trozos no prueban nada.
// 3. El veredicto recalcula el universo desde el arbol, nunca desde los
//    artefactos, y se pone rojo si falta un trozo, sobra uno, una clase aparece
//    en dos trozos, un mutante se cuenta dos veces o el umbral deriva.
//
// Deliberadamente NO es un objetivo de scripts/project.mjs: `harness mutate`
// imprime "Prueba de mutacion superada" con el codigo de salida del mutador, y
// un trozo sin umbral no debe poder decir eso.
//
// Solo usa la stdlib de Node. La unica dependencia externa, cargada en tiempo
// de veredicto, es `mutation-testing-metrics`, resuelta desde la ubicacion de
// @stryker-mutator/core para que sea exactamente la version que uso Stryker.
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import {
  appendFileSync,
  cpSync,
  existsSync,
  mkdirSync,
  readdirSync,
  readFileSync,
  realpathSync,
  rmSync,
  statSync,
  writeFileSync,
} from "node:fs";
import { createRequire } from "node:module";
import { join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

export const ROOT = fileURLToPath(new URL("../", import.meta.url));
export const MAX_SHARDS = 20;
export const PIT_VERSION = "1.22.0";
// Sin punto inicial: upload-artifact@v4 omite por defecto rutas ocultas.
export const STAGE_DIR = "mutation-shards-out";
// Segunda senal del modo troceado de Gradle. La propiedad `mutationShard` sola
// no basta: Gradle tambien la lee de ORG_GRADLE_PROJECT_mutationShard, de
// ~/.gradle/gradle.properties y de -P en GRADLE_OPTS, y un `harness verify`
// local con cualquiera de esas fuentes mutaria 1/N de las clases con umbral 0 y
// diria verde. Solo `runShard` pone esta variable, y solo en el entorno del hijo
// Gradle; scripts/project.mjs se niega a correr si la ve.
export const SHARD_RUNNER_ENV = "MUTATION_SHARD_RUNNER";
export const SHARD_RUNNER = "scripts/mutation-shards.mjs";

// DetectionStatus de PIT 1.22.0: detected=true para estos cinco.
export const PIT_DETECTED = new Set([
  "KILLED",
  "TIMED_OUT",
  "NON_VIABLE",
  "MEMORY_ERROR",
  "RUN_ERROR",
]);
const PIT_UNDETECTED = new Set(["SURVIVED", "NO_COVERAGE"]);
// Estados internos de PIT: si aparecen en el XML, la campana no termino.
const PIT_INCOMPLETE = new Set(["STARTED", "NOT_STARTED"]);
const STRYKER_STATUSES = new Set([
  "Killed",
  "Survived",
  "NoCoverage",
  "CompileError",
  "RuntimeError",
  "Timeout",
  "Ignored",
]);

const sha256 = (data) => createHash("sha256").update(data).digest("hex");

// ── Troceado ────────────────────────────────────────────────────────────────

// "k/N" con 1 <= k <= N, sin ceros a la izquierda ni espacios. Un valor raro
// nunca se degrada a "todo": la leccion de `noche_cinco`.
export function parseShard(text) {
  const m = typeof text === "string" && /^([1-9][0-9]*)\/([1-9][0-9]*)$/.exec(text);
  if (!m) throw new Error(`Trozo invalido: ${JSON.stringify(text)} (se espera k/N)`);
  const k = Number(m[1]);
  const n = Number(m[2]);
  if (!Number.isSafeInteger(k) || !Number.isSafeInteger(n) || k > n)
    throw new Error(`Trozo invalido: ${JSON.stringify(text)} (1 <= k <= N)`);
  return { k, n };
}

export function parseShardCount(text, label = "N") {
  const m = typeof text === "string" && /^([1-9][0-9]*)$/.exec(text);
  const n = m ? Number(m[1]) : NaN;
  if (!Number.isSafeInteger(n) || n < 1 || n > MAX_SHARDS)
    throw new Error(
      `${label} invalido: ${JSON.stringify(text)} (entero entre 1 y ${MAX_SHARDS})`,
    );
  return n;
}

// Reparto round-robin sobre la lista ordenada (orden de unidades UTF-16, el
// mismo que String.compareTo de Kotlin). Devuelve N listas disjuntas cuya
// union es `items`. Mismo resultado sea cual sea el orden de entrada.
export function partition(items, n) {
  if (!Array.isArray(items) || items.some((x) => typeof x !== "string"))
    throw new Error("partition: se espera una lista de cadenas");
  const sorted = [...items].sort();
  for (let i = 1; i < sorted.length; i++)
    if (sorted[i] === sorted[i - 1])
      throw new Error(`partition: elemento repetido ${sorted[i]}`);
  if (!Number.isSafeInteger(n) || n < 1)
    throw new Error(`partition: N invalido ${n}`);
  if (n > sorted.length)
    throw new Error(
      `partition: N=${n} es mayor que el universo (${sorted.length}); habria trozos vacios`,
    );
  const shards = Array.from({ length: n }, () => []);
  sorted.forEach((item, i) => shards[i % n].push(item));
  return shards;
}

// ── Universo de backend ─────────────────────────────────────────────────────

// Conversion identica a org.pitest.util.Glob para el alfabeto que admitimos.
// Todo lo que PIT trataria de forma especial ('~' regex cruda, '**.', '+',
// corchetes...) se rechaza en vez de aproximarse.
export function pitGlobToRegExp(glob) {
  if (!/^[A-Za-z0-9_.$*?]+$/.test(glob) || glob.includes("**"))
    throw new Error(`Patron PIT fuera del alfabeto soportado: ${JSON.stringify(glob)}`);
  let out = "^";
  for (const c of glob) {
    if (c === "*") out += ".*";
    else if (c === "?") out += ".";
    else if (c === "." || c === "$") out += `\\${c}`;
    else out += c;
  }
  return new RegExp(`${out}$`);
}

function splitTopLevelPlus(expr) {
  const parts = [];
  let depth = 0;
  let inString = false;
  let current = "";
  for (const c of expr) {
    if (inString) {
      current += c;
      if (c === '"') inString = false;
      continue;
    }
    if (c === '"') inString = true;
    else if (c === "(" || c === "{") depth++;
    else if (c === ")" || c === "}") depth--;
    if (c === "+" && depth === 0) {
      parts.push(current);
      current = "";
      continue;
    }
    current += c;
  }
  parts.push(current);
  return parts.map((p) => p.trim());
}

// Evalua el subconjunto de Kotlin que usa el bloque pitest: setOf("..."), la
// suma `+` de conjuntos y `.filter { !it.contains("...") }`. Cualquier otra
// forma es un error, no un conjunto vacio.
function evalSetExpr(expr, env) {
  const out = new Set();
  for (const term of splitTopLevelPlus(expr)) {
    if (!term) throw new Error(`Termino vacio en ${JSON.stringify(expr)}`);
    const literal = /^setOf\(([\s\S]*)\)$/.exec(term);
    if (literal) {
      const body = literal[1].replace(/"[^"]*"/g, "").replace(/[\s,]/g, "");
      if (body) throw new Error(`setOf con elementos no literales: ${term}`);
      for (const s of literal[1].match(/"[^"]*"/g) || []) out.add(s.slice(1, -1));
      continue;
    }
    const ref = /^(\w+)(?:\.filter\s*\{\s*!it\.contains\("([^"]+)"\)\s*\})?$/.exec(term);
    if (!ref) throw new Error(`Termino no soportado: ${JSON.stringify(term)}`);
    const base = env.get(ref[1]);
    if (!base) throw new Error(`Identificador desconocido: ${ref[1]}`);
    for (const v of base) if (!ref[2] || !v.includes(ref[2])) out.add(v);
  }
  return out;
}

function pitestBlock(build) {
  const lines = build.split(/\r?\n/);
  const start = lines.findIndex((l) => l === "pitest {");
  if (start < 0 || lines.indexOf("pitest {", start + 1) >= 0)
    throw new Error("build.gradle.kts: se espera exactamente un bloque `pitest {`");
  const end = lines.indexOf("}", start);
  if (end < 0) throw new Error("build.gradle.kts: bloque pitest sin cerrar");
  return lines.slice(start + 1, end);
}

function exactlyOne(lines, re, what) {
  const hits = lines.map((l) => re.exec(l)).filter(Boolean);
  if (hits.length !== 1)
    throw new Error(`build.gradle.kts: se espera exactamente una linea ${what}, hay ${hits.length}`);
  return hits[0];
}

// El umbral que aplica la campana completa: la linea literal
// `    mutationThreshold.set(80)` del nivel superior del bloque pitest. La del
// modo troceado (sangrada dentro de `if (shard != null)`) no cuenta.
export function gradleThreshold(build) {
  return Number(exactlyOne(pitestBlock(build), /^ {4}mutationThreshold\.set\((\d+)\)$/, "mutationThreshold.set(N)")[1]);
}

export function gradlePitVersion(build) {
  return exactlyOne(pitestBlock(build), /^ {4}pitestVersion\.set\("([^"]+)"\)$/, 'pitestVersion.set("...")')[1];
}

// Patrones de la rama `else` de targetClasses, leidos del propio fichero.
export function elsePatterns(build) {
  const block = pitestBlock(build).map((l) => l.replace(/\s*\/\/.*$/, ""));
  const whenAt = block.findIndex((l) => l === "    targetClasses.set(when {");
  const testsAt = block.findIndex((l) => l === "    targetTests.set(when {");
  if (whenAt < 0 || testsAt < whenAt)
    throw new Error("build.gradle.kts: no se encuentra el when de targetClasses");
  const decls = [];
  let name = null;
  let buf = [];
  for (const line of block.slice(0, whenAt)) {
    const d = /^ {4}val (\w+) = (.*)$/.exec(line);
    // Una declaracion termina en la siguiente linea de nivel superior que
    // empieza por letra; el `    )` que cierra un setOf multilinea es suyo.
    if (d || (name && /^ {4}[A-Za-z]/.test(line))) {
      if (name) decls.push([name, buf.join("\n")]);
      name = d ? d[1] : null;
      buf = d ? [d[2]] : [];
      continue;
    }
    if (name) buf.push(line);
  }
  if (name) decls.push([name, buf.join("\n")]);
  const env = new Map();
  for (const [n, expr] of decls) {
    if (n === "scope" || /Only$/.test(n)) continue;
    env.set(n, evalSetExpr(expr, env));
  }
  const elseLines = block
    .slice(whenAt + 1, testsAt)
    .filter((l) => /^\s*else\s*->/.test(l));
  if (elseLines.length !== 1)
    throw new Error(`build.gradle.kts: se espera una rama else en targetClasses, hay ${elseLines.length}`);
  return [...evalSetExpr(elseLines[0].replace(/^\s*else\s*->\s*/, ""), env)].sort();
}

function walkJava(dir, rel = "", out = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const path = rel ? `${rel}/${entry.name}` : entry.name;
    if (entry.isDirectory()) walkJava(join(dir, entry.name), path, out);
    else if (
      entry.isFile() &&
      entry.name.endsWith(".java") &&
      entry.name !== "package-info.java" &&
      entry.name !== "module-info.java"
    )
      out.push(path.slice(0, -".java".length).split("/").join("."));
  }
  return out;
}

// Universo de backend: los FQN de los .java de produccion que casan con algun
// patron de la rama else. Una clase por fichero (clase propietaria); PIT muta
// ademas sus anidadas, que el veredicto atribuye a la propietaria por el '$'.
export function backendUniverse(root = ROOT) {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const patterns = elsePatterns(build);
  const regexes = patterns.map(pitGlobToRegExp);
  const owners = walkJava(resolve(root, "backend/src/main/java"))
    .filter((fqn) => regexes.some((re) => re.test(fqn)))
    .sort();
  for (const owner of owners)
    if (!/^[A-Za-z0-9_.]+$/.test(owner))
      throw new Error(`Nombre de clase fuera del alfabeto soportado: ${owner}`);
  return {
    patterns,
    patternsSha256: sha256(patterns.join("\n")),
    owners,
    threshold: gradleThreshold(build),
    pitVersion: gradlePitVersion(build),
  };
}

// ── Universo de frontend ────────────────────────────────────────────────────

// Unidades = ficheros. Las entradas con rango de lineas del mismo fichero
// (session-gate.tsx lleva dos) viajan juntas, en su orden original.
export function frontendUnits(config) {
  const entries = config?.mutate;
  if (!Array.isArray(entries) || entries.length === 0)
    throw new Error("stryker.config.json: `mutate` debe ser una lista no vacia");
  const byFile = new Map();
  for (const entry of entries) {
    if (typeof entry !== "string" || !entry)
      throw new Error(`stryker.config.json: entrada invalida ${JSON.stringify(entry)}`);
    const file = entry.split(":")[0];
    // Negaciones y comodines harian que "fichero" no fuese una unidad cerrada.
    if (entry.startsWith("!") || /[*?{}[\]]/.test(file))
      throw new Error(`stryker.config.json: entrada no troceable ${JSON.stringify(entry)}`);
    if (!byFile.has(file)) byFile.set(file, []);
    byFile.get(file).push(entry);
  }
  return [...byFile.keys()].sort().map((file) => ({ file, entries: byFile.get(file) }));
}

export function frontendPartition(config, n) {
  const units = frontendUnits(config);
  const entriesOf = new Map(units.map((u) => [u.file, u.entries]));
  return partition(
    units.map((u) => u.file),
    n,
  ).map((files) => ({ files, entries: files.flatMap((f) => entriesOf.get(f)) }));
}

// Copia profunda de la configuracion base con DOS cambios: `mutate` = las
// entradas del trozo y `thresholds.break` = null. Todo lo demas (concurrency,
// coverageAnalysis, plugins, reporters, vitest...) queda identico.
export function shardStrykerConfig(base, entries) {
  if (typeof base?.thresholds?.break !== "number")
    throw new Error("stryker.config.json: thresholds.break debe ser un numero");
  const config = structuredClone(base);
  config.mutate = [...entries];
  config.thresholds = { ...config.thresholds, break: null };
  return config;
}

// ── Puntuaciones ────────────────────────────────────────────────────────────

// PercentageCalculator.getPercentage de PIT 1.22.0, con aritmetica float:
// Math.min(99, Math.round((100f / total) * actual)).
export function pitScore(total, detected) {
  if (total === 0) return 100;
  if (detected === 0) return 0;
  if (total === detected) return 100;
  const f = Math.fround;
  return Math.min(99, Math.round(f(f(f(100) / f(total)) * f(detected))));
}

// ── Informes ────────────────────────────────────────────────────────────────

function decodeXml(text) {
  return text.replace(/&(lt|gt|amp|quot|apos|#\d+|#x[0-9a-fA-F]+);|&/g, (m, e) => {
    if (e === undefined) throw new Error("mutations.xml: '&' sin escapar");
    if (e === "lt") return "<";
    if (e === "gt") return ">";
    if (e === "amp") return "&";
    if (e === "quot") return '"';
    if (e === "apos") return "'";
    return String.fromCodePoint(e[1] === "x" ? parseInt(e.slice(2), 16) : Number(e.slice(1)));
  });
}

function xmlChild(body, tag) {
  const m = new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>|<${tag}/>`).exec(body);
  if (!m) throw new Error(`mutations.xml: falta <${tag}>`);
  return m[1] === undefined ? "" : m[1];
}

// Formato de XMLReportListener de PIT 1.22.0. Exige el documento completo
// (cabecera y cierre); lo que no sea un <mutation> reconocible es un error.
export function parsePitXml(text) {
  const doc = /^<\?xml [^>]*\?>\s*<mutations(?: [^>]*)?>([\s\S]*)<\/mutations>\s*$/.exec(text);
  if (!doc) throw new Error("mutations.xml: documento incompleto o con otra forma");
  const mutations = [];
  const rest = doc[1].replace(/<mutation ([^>]*)>([\s\S]*?)<\/mutation>/g, (_, attrs, body) => {
    const a = /^detected='(true|false)' status='([A-Z_]+)' numberOfTestsRun='(\d+)'$/.exec(attrs);
    if (!a) throw new Error(`mutations.xml: atributos inesperados ${attrs}`);
    const list = (outer, inner) =>
      [...xmlChild(body, outer).matchAll(new RegExp(`<${inner}>(\\d+)</${inner}>`, "g"))].map(
        (m) => Number(m[1]),
      );
    mutations.push({
      detected: a[1] === "true",
      status: a[2],
      mutatedClass: decodeXml(xmlChild(body, "mutatedClass")),
      mutatedMethod: decodeXml(xmlChild(body, "mutatedMethod")),
      methodDescription: decodeXml(xmlChild(body, "methodDescription")),
      lineNumber: Number(xmlChild(body, "lineNumber")),
      mutator: decodeXml(xmlChild(body, "mutator")),
      indexes: list("indexes", "index"),
      blocks: list("blocks", "block"),
    });
    return "";
  });
  if (rest.trim()) throw new Error("mutations.xml: contenido no reconocido fuera de <mutation>");
  return mutations;
}

// Carga calculateMutationTestMetrics desde la ubicacion de @stryker-mutator/core
// (bajo pnpm no esta izado). Si no aparece, o no es la version exacta que
// declara Stryker, el veredicto falla: nunca una formula escrita a mano.
export async function loadStrykerMetrics(root = ROOT) {
  const corePackage = resolve(root, "frontend/node_modules/@stryker-mutator/core/package.json");
  if (!existsSync(corePackage))
    throw new Error("No esta instalado @stryker-mutator/core en frontend/node_modules");
  const real = realpathSync(corePackage);
  const wanted = JSON.parse(readFileSync(real, "utf8")).dependencies?.["mutation-testing-metrics"];
  const require = createRequire(real);
  const entry = require.resolve("mutation-testing-metrics");
  let dir = resolve(entry, "..");
  while (!existsSync(join(dir, "package.json"))) dir = resolve(dir, "..");
  const found = JSON.parse(readFileSync(join(dir, "package.json"), "utf8"));
  if (found.name !== "mutation-testing-metrics" || found.version !== wanted)
    throw new Error(
      `mutation-testing-metrics ${found.version} no es la version que declara Stryker (${wanted})`,
    );
  const mod = await import(pathToFileURL(entry).href);
  if (typeof mod.calculateMutationTestMetrics !== "function")
    throw new Error("mutation-testing-metrics no exporta calculateMutationTestMetrics");
  return mod.calculateMutationTestMetrics;
}

// ── Veredicto ───────────────────────────────────────────────────────────────

export const pitName = (k, n) => `pit-shard-${k}-of-${n}`;
export const strykerName = (k, n) => `stryker-shard-${k}-of-${n}`;
const PIT_REPORT = "report/mutations.xml";
const STRYKER_REPORT = "report/mutation.json";

const sameList = (a, b) =>
  Array.isArray(a) && Array.isArray(b) && a.length === b.length && a.every((x, i) => x === b[i]);

function readJson(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

// Comprueba done.json + manifest.json de un artefacto y devuelve el texto del
// informe si todo casa. Los errores van a `fail`; devuelve null si no se puede
// seguir con este trozo.
function checkArtifact(dir, name, expect, reportRel, sha, fail) {
  if (!existsSync(dir)) {
    fail(`${name}: falta el artefacto`);
    return null;
  }
  // Sin done.json el mutador no termino: agotado, cancelado o roto. started.json
  // puede estar (se escribe antes de lanzarlo), pero es evidencia, no permiso.
  if (!existsSync(join(dir, "done.json"))) {
    fail(`${name}: sin done.json, el mutador no termino`);
    return null;
  }
  let done;
  let manifestText;
  try {
    done = readJson(join(dir, "done.json"));
    manifestText = readFileSync(join(dir, "manifest.json"));
  } catch (error) {
    fail(`${name}: done.json o manifest.json ilegible (${error.message})`);
    return null;
  }
  if (done.tool !== expect.tool || done.k !== expect.k || done.N !== expect.n)
    fail(`${name}: done.json describe otro trozo (${done.tool} ${done.k}/${done.N})`);
  if (done.exitCode !== 0) fail(`${name}: el mutador salio con ${done.exitCode}`);
  if (done.sha !== sha) fail(`${name}: se ejecuto sobre ${done.sha}, no sobre ${sha}`);
  if (done.manifestSha256 !== sha256(manifestText))
    fail(`${name}: el manifiesto no es el que registro done.json`);
  let manifest;
  try {
    manifest = JSON.parse(manifestText);
  } catch (error) {
    fail(`${name}: manifest.json no es JSON (${error.message})`);
    return null;
  }
  const reportPath = join(dir, reportRel);
  if (!existsSync(reportPath)) {
    fail(`${name}: falta ${reportRel}`);
    return { done, manifest, report: null };
  }
  const report = readFileSync(reportPath);
  if (done.reportSha256 !== sha256(report))
    fail(`${name}: el informe no es el que registro done.json`);
  return { done, manifest, report: report.toString("utf8") };
}

function checkCoverage(label, universe, manifests, fail) {
  const seen = new Map();
  for (const [name, items] of manifests)
    for (const item of items) {
      if (seen.has(item)) fail(`${label}: ${item} esta en ${seen.get(item)} y en ${name}`);
      else seen.set(item, name);
    }
  const missing = universe.filter((u) => !seen.has(u));
  const extra = [...seen.keys()].filter((x) => !universe.includes(x));
  if (missing.length)
    fail(`${label}: ${missing.length} sin trozo: ${missing.slice(0, 20).join(", ")}`);
  if (extra.length)
    fail(`${label}: ${extra.length} fuera del universo: ${extra.slice(0, 20).join(", ")}`);
}

const minutes = (done) => {
  const ms = Date.parse(done?.endedAt) - Date.parse(done?.startedAt);
  return Number.isFinite(ms) ? (ms / 60000).toFixed(1) : "?";
};

// Fila de la tabla del veredicto. Sin done.json, started.json deja al menos
// cuando empezo y en que intento: un trozo agotado en la calibracion no queda
// como "?" sin mas. No cambia nada del rojo, que ya puso checkArtifact.
function shardRow(name, dir, got) {
  let started = null;
  try {
    started = readJson(join(dir, "started.json"));
  } catch {
    started = null;
  }
  const done = got?.done;
  return {
    name,
    attempt: done?.runAttempt ?? started?.runAttempt ?? "?",
    minutes: done
      ? minutes(done)
      : typeof started?.startedAt === "string"
        ? `sin terminar (desde ${started.startedAt})`
        : "?",
    mutants: 0,
    detected: 0,
  };
}

export function verdict({
  root = ROOT,
  artifacts,
  backendShards,
  frontendShards,
  sha,
  calculateMutationTestMetrics,
}) {
  const errors = [];
  const fail = (message) => errors.push(message);
  const rows = [];
  const result = { ok: false, errors, rows, backend: null, frontend: null };

  // (a) Universo y umbrales recalculados desde el arbol.
  let backend;
  let strykerBase;
  let strykerBaseBytes;
  let harnessThreshold;
  try {
    if (!/^[0-9a-f]{40}$/.test(sha ?? "")) throw new Error(`--sha invalido: ${sha}`);
    backend = backendUniverse(root);
    strykerBaseBytes = readFileSync(resolve(root, "frontend/stryker.config.json"));
    strykerBase = JSON.parse(strykerBaseBytes);
    harnessThreshold = readJson(resolve(root, "harness.config.json")).mutation?.threshold;
  } catch (error) {
    fail(`No se puede recalcular el universo: ${error.message}`);
    return result;
  }
  if (backend.pitVersion !== PIT_VERSION)
    fail(`PIT ${backend.pitVersion}: la formula de puntuacion esta verificada para ${PIT_VERSION}`);
  const strykerBreak = strykerBase.thresholds?.break;
  if (
    backend.threshold === 0 ||
    backend.threshold !== strykerBreak ||
    typeof harnessThreshold !== "number" ||
    Math.abs(harnessThreshold * 100 - backend.threshold) > 1e-9
  )
    fail(
      `Deriva de umbral: build.gradle.kts ${backend.threshold}, stryker break ${strykerBreak}, harness.config.json ${harnessThreshold}`,
    );

  let backendParts;
  let frontendParts;
  try {
    backendParts = partition(backend.owners, backendShards);
    frontendParts = frontendPartition(strykerBase, frontendShards);
  } catch (error) {
    fail(`Reparto imposible: ${error.message}`);
    return result;
  }

  // (b) Exactamente los artefactos esperados, ni uno mas.
  const expected = new Set([
    ...backendParts.map((_, i) => pitName(i + 1, backendShards)),
    ...frontendParts.map((_, i) => strykerName(i + 1, frontendShards)),
  ]);
  const present = existsSync(artifacts)
    ? readdirSync(artifacts).filter((e) => statSync(join(artifacts, e)).isDirectory())
    : [];
  for (const name of present)
    if (!expected.has(name)) fail(`Artefacto inesperado: ${name}`);

  // Backend.
  const pitKeys = new Map();
  const pitManifests = [];
  let pitTotal = 0;
  let pitDetected = 0;
  backendParts.forEach((owners, i) => {
    const k = i + 1;
    const name = pitName(k, backendShards);
    const got = checkArtifact(join(artifacts, name), name, { tool: "pit", k, n: backendShards }, PIT_REPORT, sha, fail);
    const row = shardRow(name, join(artifacts, name), got);
    rows.push(row);
    if (!got) return;
    const m = got.manifest;
    if (m.tool !== "pit" || m.k !== k || m.N !== backendShards)
      fail(`${name}: el manifiesto describe otro trozo`);
    if (!sameList(m.owners, owners))
      fail(`${name}: las clases del manifiesto no son el reparto recalculado`);
    if (m.ownersTotal !== backend.owners.length)
      fail(`${name}: universo de ${m.ownersTotal} clases en Gradle y ${backend.owners.length} recalculado`);
    if (m.patternsSha256 !== backend.patternsSha256)
      fail(`${name}: los patrones de targetClasses no son los de la rama else recalculada`);
    if (m.excludedCount !== 2 * (backend.owners.length - owners.length))
      fail(`${name}: excludedClasses no excluye exactamente a las demas clases`);
    if (m.pitestVersion !== PIT_VERSION) fail(`${name}: PIT ${m.pitestVersion}`);
    pitManifests.push([name, Array.isArray(m.owners) ? m.owners : []]);
    if (got.report === null) return;
    let mutations;
    try {
      mutations = parsePitXml(got.report);
    } catch (error) {
      fail(`${name}: ${error.message}`);
      return;
    }
    const mine = new Set(Array.isArray(m.owners) ? m.owners : []);
    const universe = new Set(backend.owners);
    for (const mu of mutations) {
      const owner = mu.mutatedClass.split("$")[0];
      if (!universe.has(owner)) fail(`${name}: muta ${mu.mutatedClass}, fuera del universo`);
      else if (!mine.has(owner)) fail(`${name}: muta ${mu.mutatedClass}, que es de otro trozo`);
      if (PIT_INCOMPLETE.has(mu.status))
        fail(`${name}: mutante ${mu.status} en ${mu.mutatedClass}:${mu.lineNumber} (campana incompleta)`);
      else if (!PIT_DETECTED.has(mu.status) && !PIT_UNDETECTED.has(mu.status))
        fail(`${name}: estado desconocido ${mu.status}`);
      if (mu.detected !== PIT_DETECTED.has(mu.status))
        fail(`${name}: detected='${mu.detected}' no casa con ${mu.status}`);
      const key = JSON.stringify([
        mu.mutatedClass,
        mu.mutatedMethod,
        mu.methodDescription,
        mu.lineNumber,
        mu.mutator,
        mu.indexes,
        mu.blocks,
      ]);
      if (pitKeys.has(key)) fail(`${name}: mutante repetido (tambien en ${pitKeys.get(key)}): ${key}`);
      else pitKeys.set(key, name);
      row.mutants++;
      if (PIT_DETECTED.has(mu.status)) row.detected++;
    }
    pitTotal += row.mutants;
    pitDetected += row.detected;
  });
  checkCoverage("backend", backend.owners, pitManifests, fail);

  // Frontend.
  const configSha = sha256(strykerBaseBytes);
  // Un fichero de `mutate` que ya no existe no rompe Stryker: solo avisa ("did
  // not result in any files") y la campana mide menos sin decirlo. El informe
  // tampoco lo delata, porque Stryker 10.0.0 no lista ficheros sin mutantes.
  // Asi que se exige desde el arbol.
  for (const unit of frontendUnits(strykerBase)) {
    const path = resolve(root, "frontend", unit.file);
    if (!existsSync(path) || !statSync(path).isFile())
      fail(`frontend: ${unit.file} esta en stryker.config.json y no existe en el arbol`);
  }
  const strykerKeys = new Map();
  const strykerManifests = [];
  const mergedFiles = {};
  let schemaVersion;
  const allEntries = [];
  frontendParts.forEach((part, i) => {
    const k = i + 1;
    const name = strykerName(k, frontendShards);
    const got = checkArtifact(join(artifacts, name), name, { tool: "stryker", k, n: frontendShards }, STRYKER_REPORT, sha, fail);
    const row = shardRow(name, join(artifacts, name), got);
    rows.push(row);
    if (!got) return;
    const m = got.manifest;
    if (m.tool !== "stryker" || m.k !== k || m.N !== frontendShards)
      fail(`${name}: el manifiesto describe otro trozo`);
    if (!sameList(m.files, part.files) || !sameList(m.entries, part.entries))
      fail(`${name}: los ficheros del manifiesto no son el reparto recalculado`);
    if (m.configSha256 !== configSha)
      fail(`${name}: se troceo otra stryker.config.json`);
    strykerManifests.push([name, Array.isArray(m.files) ? m.files : []]);
    if (Array.isArray(m.entries)) allEntries.push(...m.entries);
    if (got.report === null) return;
    let report;
    try {
      report = JSON.parse(got.report);
    } catch (error) {
      fail(`${name}: mutation.json no es JSON (${error.message})`);
      return;
    }
    if (schemaVersion === undefined) schemaVersion = report.schemaVersion;
    else if (report.schemaVersion !== schemaVersion)
      fail(`${name}: schemaVersion ${report.schemaVersion} distinto de ${schemaVersion}`);
    // El informe trae las opciones con las que corrio Stryker (`config`, que
    // mutation-test-report-helper de 10.0.0 rellena con las opciones). Su
    // `mutate` tiene que ser el del trozo: asi se prueba lo que se midio, no
    // solo el plan. Lo que sigue sin poder probarse es que un fichero con cero
    // mutantes no se haya descartado: Stryker construye `files` solo a partir
    // de los resultados, asi que exigir cada fichero en `files` pondria rojo un
    // fichero legitimo sin mutantes. PIT tiene el mismo hueco con sus clases.
    if (!sameList(report.config?.mutate, m.entries))
      fail(`${name}: Stryker no corrio con el mutate del trozo (config.mutate del informe)`);
    const mine = new Set(Array.isArray(m.files) ? m.files : []);
    for (const [file, data] of Object.entries(report.files ?? {})) {
      if (!mine.has(file)) {
        fail(`${name}: informa de ${file}, que no es de este trozo`);
        continue;
      }
      mergedFiles[file] = data;
      for (const mu of data.mutants ?? []) {
        if (mu.status === "Pending") fail(`${name}: mutante Pending en ${file} (campana incompleta)`);
        else if (!STRYKER_STATUSES.has(mu.status)) fail(`${name}: estado desconocido ${mu.status}`);
        const loc = mu.location ?? {};
        const key = JSON.stringify([
          file,
          mu.mutatorName,
          loc.start?.line,
          loc.start?.column,
          loc.end?.line,
          loc.end?.column,
          mu.replacement,
        ]);
        if (strykerKeys.has(key)) fail(`${name}: mutante repetido (tambien en ${strykerKeys.get(key)}): ${key}`);
        else strykerKeys.set(key, name);
        row.mutants++;
        if (mu.status === "Killed" || mu.status === "Timeout") row.detected++;
      }
    }
  });
  checkCoverage(
    "frontend",
    frontendUnits(strykerBase).map((u) => u.file),
    strykerManifests,
    fail,
  );
  if (!sameList([...allEntries].sort(), [...strykerBase.mutate].sort()))
    fail("frontend: las entradas de los manifiestos no son las de stryker.config.json");

  // (c) Mismo umbral, misma formula, dos puertas separadas (como hoy).
  const pit = pitScore(pitTotal, pitDetected);
  result.backend = { total: pitTotal, detected: pitDetected, score: pit, threshold: backend.threshold };
  if (backend.threshold !== 0 && pit < backend.threshold)
    fail(`backend: puntuacion PIT ${pit} por debajo del umbral ${backend.threshold}`);
  let score = NaN;
  try {
    // Sin la libreria de Stryker no hay puntuacion: rojo, nunca una formula propia.
    if (typeof calculateMutationTestMetrics !== "function")
      throw new Error("falta calculateMutationTestMetrics");
    score = calculateMutationTestMetrics({
      schemaVersion,
      thresholds: { high: strykerBase.thresholds.high, low: strykerBase.thresholds.low },
      files: mergedFiles,
    }).systemUnderTestMetrics.metrics.mutationScore;
  } catch (error) {
    fail(`frontend: mutation-testing-metrics fallo (${error.message})`);
  }
  result.frontend = {
    total: rows.filter((r) => r.name.startsWith("stryker")).reduce((a, r) => a + r.mutants, 0),
    score,
    threshold: strykerBreak,
  };
  if (!Number.isFinite(score)) fail("frontend: puntuacion Stryker no calculable");
  else if (score < strykerBreak)
    fail(`frontend: puntuacion Stryker ${score.toFixed(2)} por debajo de break ${strykerBreak}`);

  result.ok = errors.length === 0;
  return result;
}

export function summaryMarkdown(result) {
  const lines = [
    `## Veredicto de la campana troceada: ${result.ok ? "VERDE" : "ROJO"}`,
    "",
  ];
  if (result.backend)
    lines.push(
      `- Backend (PIT): ${result.backend.detected}/${result.backend.total} detectados, puntuacion ${result.backend.score} (umbral ${result.backend.threshold}).`,
    );
  if (result.frontend)
    lines.push(
      `- Frontend (Stryker): ${result.frontend.total} mutantes, puntuacion ${Number.isFinite(result.frontend.score) ? result.frontend.score.toFixed(2) : "NaN"} (break ${result.frontend.threshold}).`,
    );
  lines.push(
    "",
    "| trozo | intento | minutos | mutantes | detectados |",
    "| --- | --- | --- | --- | --- |",
  );
  for (const r of result.rows)
    lines.push(`| ${r.name} | ${r.attempt} | ${r.minutes} | ${r.mutants} | ${r.detected} |`);
  if (result.errors.length) {
    lines.push("", "### Fallos", "");
    for (const e of result.errors) lines.push(`- ${e}`);
  }
  return `${lines.join("\n")}\n`;
}

// ── CLI ─────────────────────────────────────────────────────────────────────

function options(args, allowed) {
  const out = {};
  for (let i = 0; i < args.length; i += 2) {
    const key = args[i]?.replace(/^--/, "");
    if (!args[i]?.startsWith("--") || !allowed.includes(key) || args[i + 1] === undefined || key in out)
      throw new Error(`Argumento invalido: ${args[i]}`);
    out[key] = args[i + 1];
  }
  for (const key of allowed) if (!(key in out) && key !== "github-output") throw new Error(`Falta --${key}`);
  return out;
}

export function plan(root, backendShards, frontendShards) {
  const owners = backendUniverse(root).owners;
  const config = JSON.parse(readFileSync(resolve(root, "frontend/stryker.config.json"), "utf8"));
  partition(owners, backendShards);
  frontendPartition(config, frontendShards);
  const matrix = (n, name) =>
    Array.from({ length: n }, (_, i) => ({ shard: `${i + 1}/${n}`, name: name(i + 1, n) }));
  return {
    backend_shards: backendShards,
    frontend_shards: frontendShards,
    backend: matrix(backendShards, pitName),
    frontend: matrix(frontendShards, strykerName),
  };
}

function headSha(root) {
  if (process.env.GITHUB_SHA) return process.env.GITHUB_SHA;
  const r = spawnSync("git", ["rev-parse", "HEAD"], { cwd: root, encoding: "utf8" });
  return r.status === 0 ? r.stdout.trim() : null;
}

function stageAndRecord({ root, name, tool, k, n, startedAt, exitCode, reportDir, reportFile, manifestFrom }) {
  const stage = resolve(root, STAGE_DIR, name);
  if (manifestFrom && existsSync(manifestFrom)) cpSync(manifestFrom, join(stage, "manifest.json"));
  if (existsSync(reportDir)) cpSync(reportDir, join(stage, "report"), { recursive: true });
  const hash = (p) => (existsSync(p) ? sha256(readFileSync(p)) : null);
  const reportSha256 = hash(join(stage, "report", reportFile));
  const manifestSha256 = hash(join(stage, "manifest.json"));
  const done = {
    tool,
    k,
    N: n,
    exitCode,
    sha: headSha(root),
    runAttempt: process.env.GITHUB_RUN_ATTEMPT ?? null,
    startedAt,
    endedAt: new Date().toISOString(),
    reportSha256,
    manifestSha256,
  };
  writeFileSync(join(stage, "done.json"), `${JSON.stringify(done, null, 2)}\n`);
  if (exitCode === 0 && (!reportSha256 || !manifestSha256)) return 1;
  return exitCode;
}

// `spawn` se inyecta solo en los tests, para no lanzar Gradle ni Stryker.
export function runShard(tool, shardText, root = ROOT, spawn = spawnSync) {
  const { k, n } = parseShard(shardText);
  if (tool !== "backend" && tool !== "frontend") throw new Error(`Herramienta invalida: ${tool}`);
  const name = tool === "backend" ? pitName(k, n) : strykerName(k, n);
  const stage = resolve(root, STAGE_DIR, name);
  rmSync(stage, { recursive: true, force: true });
  mkdirSync(stage, { recursive: true });
  const startedAt = new Date().toISOString();
  // Antes de lanzar el mutador: si el job se agota o se cancela, done.json no
  // llega a escribirse, pero el artefacto conserva cuando empezo el trozo.
  const recordStart = (recorded) =>
    writeFileSync(
      join(stage, "started.json"),
      `${JSON.stringify(
        {
          tool: recorded,
          k,
          N: n,
          sha: headSha(root),
          runAttempt: process.env.GITHUB_RUN_ATTEMPT ?? null,
          startedAt,
        },
        null,
        2,
      )}\n`,
    );
  if (tool === "backend") {
    if (process.platform === "win32")
      throw new Error("El modo troceado es solo Linux/CI: excludedClasses no cabe en la linea de ordenes de Windows");
    const reportDir = resolve(root, `backend/build/reports/pitest-shard-${k}-of-${n}`);
    const manifest = `${reportDir}.manifest.json`;
    rmSync(reportDir, { recursive: true, force: true });
    rmSync(manifest, { force: true });
    recordStart("pit");
    const r = spawn("./gradlew", ["pitest", "--no-daemon", `-PmutationShard=${k}/${n}`], {
      cwd: resolve(root, "backend"),
      stdio: "inherit",
      env: { ...process.env, [SHARD_RUNNER_ENV]: SHARD_RUNNER },
    });
    return stageAndRecord({
      root, name, tool: "pit", k, n, startedAt,
      exitCode: r.status ?? 1,
      reportDir,
      reportFile: "mutations.xml",
      manifestFrom: manifest,
    });
  }
  const basePath = resolve(root, "frontend/stryker.config.json");
  const base = JSON.parse(readFileSync(basePath, "utf8"));
  const part = frontendPartition(base, n)[k - 1];
  writeFileSync(
    join(stage, "manifest.json"),
    `${JSON.stringify({ tool: "stryker", k, N: n, files: part.files, entries: part.entries, configSha256: sha256(readFileSync(basePath)) }, null, 2)}\n`,
  );
  const shardConfig = resolve(root, "frontend/stryker.shard.config.json");
  writeFileSync(shardConfig, `${JSON.stringify(shardStrykerConfig(base, part.entries), null, 2)}\n`);
  const jsonReport = base.jsonReporter?.fileName ?? "reports/mutation/mutation.json";
  const reportDir = resolve(root, "frontend", jsonReport, "..");
  rmSync(reportDir, { recursive: true, force: true });
  recordStart("stryker");
  let status;
  try {
    const r = spawn("pnpm", ["--dir", "frontend", "exec", "stryker", "run", "stryker.shard.config.json"], {
      cwd: root,
      stdio: "inherit",
      shell: process.platform === "win32",
    });
    status = r.status ?? 1;
  } finally {
    rmSync(shardConfig, { force: true });
  }
  return stageAndRecord({
    root, name, tool: "stryker", k, n, startedAt,
    exitCode: status,
    reportDir,
    reportFile: jsonReport.split("/").pop(),
    manifestFrom: null,
  });
}

export async function main(argv) {
  const [command, ...rest] = argv;
  if (command === "plan") {
    const o = options(rest, ["backend", "frontend", "github-output"]);
    const p = plan(ROOT, parseShardCount(o.backend, "--backend"), parseShardCount(o.frontend, "--frontend"));
    if (o["github-output"])
      appendFileSync(
        o["github-output"],
        Object.entries(p)
          .map(([key, value]) => `${key}=${JSON.stringify(value)}\n`)
          .join(""),
      );
    console.log(JSON.stringify(p, null, 2));
    return 0;
  }
  if (command === "run") {
    if (rest.length !== 2) throw new Error("Uso: run backend|frontend k/N");
    return runShard(rest[0], rest[1]);
  }
  if (command === "verdict") {
    const o = options(rest, ["artifacts", "backend", "frontend", "sha"]);
    const backendShards = parseShardCount(o.backend, "--backend");
    const frontendShards = parseShardCount(o.frontend, "--frontend");
    let calculate;
    let loadError = null;
    try {
      calculate = await loadStrykerMetrics(ROOT);
    } catch (error) {
      loadError = error;
    }
    const result = verdict({
      root: ROOT,
      artifacts: resolve(o.artifacts),
      backendShards,
      frontendShards,
      sha: o.sha,
      calculateMutationTestMetrics: calculate,
    });
    if (loadError) result.errors.unshift(`mutation-testing-metrics: ${loadError.message}`);
    result.ok = result.errors.length === 0;
    const md = summaryMarkdown(result);
    console.log(md);
    if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, md);
    return result.ok ? 0 : 1;
  }
  throw new Error("Uso: mutation-shards.mjs plan|run|verdict ...");
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main(process.argv.slice(2)).then(
    (code) => {
      process.exitCode = code;
    },
    (error) => {
      console.error(error.message);
      process.exitCode = 1;
    },
  );
}
