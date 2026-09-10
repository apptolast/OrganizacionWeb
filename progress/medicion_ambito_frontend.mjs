import { readFileSync, writeFileSync } from "node:fs";
import { execSync } from "node:child_process";

const ROOT = "C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb";
const FE = `${ROOT}/frontend`;
const load = (f) => JSON.parse(readFileSync(`${FE}/${f}`, "utf8"));
const fileOf = (entry) => entry.split(":")[0];

const base = load("stryker.config.json");
const baseFiles = [...new Set(base.mutate.map(fileOf))].sort();

const pmjs = readFileSync(`${ROOT}/scripts/project.mjs`, "utf8");
const cfgs = [...new Set([...pmjs.matchAll(/"(stryker[\w.-]*\.config\.json)"/g)].map((x) => x[1]))].sort();
const inline = [...pmjs.matchAll(/"--mutate",\s*\n\s*"([^"]+)"/g)].map((x) => x[1]);

const perCfg = {};
const union = new Set();
for (const c of cfgs) {
  const j = load(c);
  const fs2 = [...new Set(j.mutate.map(fileOf))];
  perCfg[c] = { ficheros: fs2.length, entradas: j.mutate.length, concurrency: j.concurrency };
  for (const f of fs2) union.add(f);
}
const inlineFiles = inline.flatMap((s) => s.split(","));
for (const f of inlineFiles) union.add(f);

const all = execSync(
  `find src -type f \\( -name '*.ts' -o -name '*.tsx' \\) ! -name '*.test.*' ! -name '*.spec.*' ! -name '*.d.ts'`,
  { cwd: FE, shell: "bash" },
).toString().trim().split("\n").map((s) => s.trim()).filter(Boolean).sort();

const baseSet = new Set(baseFiles);
const enBaseNoEnUnion = baseFiles.filter((f) => !union.has(f));
const enUnionNoEnBase = [...union].filter((f) => !baseSet.has(f)).sort();
const sinMutarPorNadie = all.filter((f) => !baseSet.has(f) && !union.has(f));
const conRangoParcial = base.mutate.filter((e) => e.includes(":"));

const out = {
  frontend_src_total: all.length,
  base_stryker_config_entradas: base.mutate.length,
  base_stryker_config_ficheros: baseFiles.length,
  base_concurrency: base.concurrency,
  base_thresholds: base.thresholds,
  base_entradas_con_rango_de_lineas: conRangoParcial,
  configs_alcanzables_desde_project_mjs: cfgs,
  configs_por_fichero: perCfg,
  mutate_en_linea_desde_project_mjs: inlineFiles,
  union_frontend_ficheros: union.size,
  en_base_y_NO_en_union: enBaseNoEnUnion,
  en_union_y_NO_en_base: enUnionNoEnBase,
  src_no_mutado_por_nadie_total: sinMutarPorNadie.length,
  src_no_mutado_por_nadie: sinMutarPorNadie,
  base_ficheros: baseFiles,
  union_ficheros: [...union].sort(),
};
writeFileSync(process.argv[2], JSON.stringify(out, null, 2));
const s = { ...out };
delete s.base_ficheros;
delete s.union_ficheros;
delete s.src_no_mutado_por_nadie;
console.log(JSON.stringify(s, null, 2));
