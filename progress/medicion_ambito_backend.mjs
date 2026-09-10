import { readFileSync, writeFileSync } from "node:fs";
import { execSync } from "node:child_process";

const ROOT = "C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb";
const gradle = readFileSync(`${ROOT}/backend/build.gradle.kts`, "utf8").split(/\r?\n/);

// ---- 1. Universo de produccion: un FQN por fichero .java bajo backend/src/main/java
const files = execSync(`find backend/src/main/java -name '*.java'`, {
  cwd: ROOT,
  shell: "bash",
})
  .toString()
  .trim()
  .split("\n")
  .map((s) => s.trim())
  .filter(Boolean);
const FQNS = files
  .map((f) =>
    f
      .replace(/^backend\/src\/main\/java\//, "")
      .replace(/\.java$/, "")
      .replace(/\//g, "."),
  )
  .sort();

// ---- 2. Parseo de `val NAME = <expr>` dentro del bloque pitest
const start = gradle.findIndex((l) => l.trim() === "pitest {") + 1;
const end = gradle.findIndex((l) => l.includes("targetClasses.set(when {"));
const bodyLines = gradle.slice(start, end).map((l) => l.replace(/\s*\/\/.*$/, ""));

// Acumulacion por lineas: una declaracion empieza en `    val NAME = ...` y
// termina justo antes de la siguiente linea con 4 espacios de indentacion que
// abre algo nuevo (`    val ` o `    <letra>`).
const decls = [];
let curName = null;
let curBuf = [];
for (const line of bodyLines) {
  const d = line.match(/^ {4}val (\w+) = (.*)$/);
  if (d) {
    if (curName) decls.push([curName, curBuf.join("\n")]);
    curName = d[1];
    curBuf = [d[2]];
    continue;
  }
  if (curName && /^ {4}[A-Za-z]/.test(line)) {
    decls.push([curName, curBuf.join("\n")]);
    curName = null;
    curBuf = [];
    continue;
  }
  if (curName) curBuf.push(line);
}
if (curName) decls.push([curName, curBuf.join("\n")]);

const env = {};
function evalExpr(expr) {
  const parts = [];
  let depth = 0;
  let cur = "";
  let inStr = false;
  for (let i = 0; i < expr.length; i++) {
    const c = expr[i];
    if (inStr) {
      cur += c;
      if (c === '"') inStr = false;
      continue;
    }
    if (c === '"') {
      inStr = true;
      cur += c;
      continue;
    }
    if (c === "(" || c === "{") depth++;
    if (c === ")" || c === "}") depth--;
    if (c === "+" && depth === 0) {
      parts.push(cur);
      cur = "";
      continue;
    }
    cur += c;
  }
  parts.push(cur);
  const out = new Set();
  for (const raw of parts) {
    const t = raw.trim();
    if (!t) continue;
    if (t.startsWith("setOf(")) {
      for (const s of t.match(/"[^"]*"/g) || []) out.add(s.slice(1, -1));
    } else {
      const idm = t.match(/^(\w+)(\.filter\s*\{\s*!it\.contains\("([^"]+)"\)\s*\})?$/);
      if (!idm) throw new Error("termino no parseado: " + JSON.stringify(t));
      const base = env[idm[1]];
      if (!base) throw new Error("identificador desconocido: " + idm[1]);
      for (const v of base) {
        if (idm[3] && v.includes(idm[3])) continue;
        out.add(v);
      }
    }
  }
  return out;
}
const parseErrors = [];
for (const [name, expr] of decls) {
  if (/^(scope|\w+Only)$/.test(name)) continue;
  try {
    env[name] = evalExpr(expr);
  } catch (e) {
    parseErrors.push(name + ": " + e.message);
  }
}

// ---- 3. glob de PIT -> regex ('*' casa cualquier cosa, incluido el punto)
const SPECIAL = new Set([".", "+", "?", "^", "$", "{", "}", "(", ")", "|", "[", "]", String.fromCharCode(92)]);
const esc = (s) =>
  [...s].map((c) => (SPECIAL.has(c) ? String.fromCharCode(92) + c : c)).join("");
const toRe = (g) => new RegExp("^" + g.split("*").map(esc).join(".*") + "$");
const resolve = (patterns) => {
  const s = new Set();
  for (const p of patterns) {
    const r = toRe(p);
    for (const f of FQNS) if (r.test(f)) s.add(f);
  }
  return s;
};

// ---- 4. la rama `else` de targetClasses, leida literalmente del fichero
const elseIdx = gradle.findIndex((l) => l.includes("else -> core + authenticationClasses"));
const elseExpr = gradle[elseIdx].replace(/^\s*else ->\s*/, "");
const elsePatterns = evalExpr(elseExpr);
const UNIVERSE = resolve(elsePatterns);

// ---- 5. cada rama con nombre
const whenStart = end + 1;
const whenEnd = gradle.findIndex((l) => l.includes("targetTests.set(when {"));
const branches = {};
for (let i = whenStart; i < whenEnd; i++) {
  const line = gradle[i].replace(/\s*\/\/.*$/, "").trim();
  const bm = line.match(/^(\w+)(?:\s*\|\|\s*(\w+))?\s*->\s*(.+?)$/);
  if (!bm || bm[1] === "else") continue;
  const flags = [bm[1], bm[2]].filter(Boolean);
  const set = evalExpr(bm[3]);
  for (const f of flags) branches[f] = set;
}
const flagToScope = {};
for (const l of gradle) {
  const fm = l.match(/^\s*val (\w+Only) = scope == "([^"]+)"/);
  if (fm) flagToScope[fm[1]] = fm[2];
}
const scopeClasses = {};
const scopePatterns = {};
for (const [flag, set] of Object.entries(branches)) {
  const sc = flagToScope[flag];
  if (sc) {
    scopeClasses[sc] = resolve(set);
    scopePatterns[sc] = [...set].sort();
  }
}

// ---- 6. ambitos alcanzables desde scripts/project.mjs
const pmjs = readFileSync(`${ROOT}/scripts/project.mjs`, "utf8");
const reachable = [...new Set([...pmjs.matchAll(/-PmutationScope=(\w+)/g)].map((x) => x[1]))];
const named = reachable.filter((s) => scopeClasses[s]);
const unmapped = reachable.filter((s) => !scopeClasses[s]);
const declaredNoTarget = Object.keys(scopeClasses).filter((s) => !reachable.includes(s));

const UNION = new Set();
for (const s of named) for (const c of scopeClasses[s]) UNION.add(c);

const orphans = [...UNIVERSE].filter((c) => !UNION.has(c)).sort();
const pkgOf = (c) => c.replace(/\.[^.]+$/, "").replace("com.apptolast.organization.", "");
const count = (list) => {
  const o = {};
  for (const x of list) o[pkgOf(x)] = (o[pkgOf(x)] || 0) + 1;
  return o;
};
const notInUniverse = FQNS.filter((f) => !UNIVERSE.has(f)).sort();

// union de TODAS las ramas con nombre (incluidas las 5 sin objetivo)
const UNION_ALL = new Set();
for (const s of Object.keys(scopeClasses)) for (const c of scopeClasses[s]) UNION_ALL.add(c);
const orphansAll = [...UNIVERSE].filter((c) => !UNION_ALL.has(c)).sort();

const domApp = [...UNIVERSE].filter(
  (c) => c.startsWith("com.apptolast.organization.domain.") || c.startsWith("com.apptolast.organization.application."),
);
const domAppOrphans = orphans.filter(
  (c) => c.startsWith("com.apptolast.organization.domain.") || c.startsWith("com.apptolast.organization.application."),
);

const report = {
  errores_parseo: parseErrors,
  produccion_total_ficheros: FQNS.length,
  produccion_por_paquete: count(FQNS),
  universo_else: UNIVERSE.size,
  universo_por_paquete: count([...UNIVERSE]),
  produccion_fuera_del_else_total: notInUniverse.length,
  produccion_fuera_del_else: notInUniverse,
  patrones_else_total: elsePatterns.size,
  patrones_else: [...elsePatterns].sort(),
  domain_application_en_universo: domApp.length,
  domain_application_huerfanas: domAppOrphans.length,
  scopes_alcanzables_desde_project_mjs: reachable.sort(),
  scopes_con_rama_en_gradle: named.sort(),
  scopes_alcanzables_SIN_rama_en_gradle: unmapped.sort(),
  scopes_declarados_en_gradle_SIN_objetivo: declaredNoTarget.sort(),
  union_scopes_alcanzables: UNION.size,
  huerfanas_total: orphans.length,
  huerfanas_por_paquete: count(orphans),
  union_TODAS_las_ramas: UNION_ALL.size,
  huerfanas_si_se_anaden_los_5_sin_objetivo: orphansAll.length,
  huerfanas_por_paquete_todas: count(orphansAll),
  tamano_por_scope: Object.fromEntries(
    Object.entries(scopeClasses)
      .map(([k, v]) => [k, v.size])
      .sort((a, b) => b[1] - a[1]),
  ),
  huerfanas: orphans,
  huerfanas_incluso_con_los_5: orphansAll,
  patrones_por_scope: scopePatterns,
};
writeFileSync(process.argv[2], JSON.stringify(report, null, 2));
const short = { ...report };
delete short.huerfanas;
delete short.huerfanas_incluso_con_los_5;
delete short.patrones_por_scope;
delete short.patrones_else;
console.log(JSON.stringify(short, null, 2));
