// El inverso de patrones-muertos.mjs. Aquel avisa de patrones que no resuelven a
// ninguna clase; este avisa de CLASES DE PRODUCCION que ningun ambito NOMBRADO
// alcanza, o sea produccion que ninguna campana por feature puntua nunca.
//
// Los dos fallos son el mismo de fondo -la puerta mide algo que se parece a lo
// que queria medir- pero se ven distinto: un patron muerto se descarta en
// silencio, y una clase huerfana ni siquiera aparece en el informe, asi que
// nadie la echa de menos. Hoy han salido seis ambitos mal apuntados por esto.
//
// El ambito por defecto lleva el comodin application.* y alcanza casi todo, asi
// que aqui se ignora a proposito: lo que importa es si la campana DE SU FEATURE
// la mide.
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";

const ROOT = "backend/src/main/java";
const build = readFileSync("backend/build.gradle.kts", "utf8");

const walk = (dir, out = []) => {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) walk(full, out);
    else if (entry.endsWith(".java")) out.push(full.split("\\").join("/"));
  }
  return out;
};

const clases = walk(ROOT).map((f) =>
  f.slice(ROOT.length + 1, -".java".length).split("/").join("."),
);

// Los conjuntos nombrados, uno por feature. Se excluye el comodin del ambito por
// defecto, que taparia justo lo que se busca.
const conjuntos = [...build.matchAll(/val (\w+Classes) = setOf\(([\s\S]*?)\n    \)/g)];
const patrones = [];
for (const [, nombre, cuerpo] of conjuntos)
  for (const [, p] of cuerpo.matchAll(/"(com\.apptolast\.organization\.[^"]+)"/g))
    if (p !== "com.apptolast.organization.*") patrones.push({ nombre, p });

const casa = (p, fqn) =>
  new RegExp("^" + p.replace(/[.]/g, "\.").replace(/\*/g, ".*") + "$").test(fqn);

const huerfanas = clases.filter((c) => !patrones.some(({ p }) => casa(p, c)));

console.log(
  `${clases.length} clases de produccion, ${patrones.length} patrones en ${conjuntos.length} ambitos nombrados`,
);
console.log(`${huerfanas.length} sin ningun ambito nombrado que las alcance:`);
for (const h of huerfanas) console.log("  HUERFANA:", h);
