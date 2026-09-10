// Barre backend/build.gradle.kts y avisa de todo patron PIT que no resuelva a
// ninguna clase real, mirando los DOS arboles de fuentes: targetClasses apunta a
// src/main y targetTests a src/test, asi que comprobar solo uno da falsos
// positivos por decenas.
//
// Un patron muerto no aborta PIT: lo descarta en silencio y la campana da una
// puntuacion estupenda sin haber mutado la clase que importaba. Esta noche han
// aparecido tres: adapter.crypto (el cifrador), ConnectorStatusSource* (que solo
// resolvia a la interfaz) e ImportGithubIssues* (borrada al unificar el caso de
// uso, dejando ImportIssues sin mutantes en todo el repositorio).
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";

const ROOTS = ["backend/src/main/java", "backend/src/test/java"];

const walk = (dir, out) => {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) walk(full, out);
    else if (entry.endsWith(".java")) out.push(full.split("\\").join("/"));
  }
  return out;
};

const fqns = [];
for (const root of ROOTS)
  for (const file of walk(root, []))
    fqns.push(
      file
        .slice(root.length + 1, -".java".length)
        .split("/")
        .join("."),
    );

const patterns = [];
for (const line of readFileSync("backend/build.gradle.kts", "utf8").split(
  /\r?\n/,
)) {
  const match = line.match(/"(com\.apptolast\.organization\.[^"]+)"/);
  if (match) patterns.push(match[1]);
}

const dead = patterns.filter((pattern) => {
  const rx = new RegExp(
    "^" + pattern.replace(/[.]/g, "\\.").replace(/\*/g, ".*") + "$",
  );
  return !fqns.some((f) => rx.test(f));
});

console.log(`${patterns.length} patrones, ${dead.length} muertos`);
for (const pattern of [...new Set(dead)]) console.log("  MUERTO:", pattern);
