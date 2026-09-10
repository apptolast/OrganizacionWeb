// Acredita a mano que los oráculos nuevos de la feature 28 discriminan: aplica cada
// mutante al fichero de producción REAL, ejecuta las pruebas del ámbito, anota qué
// pruebas caen y restaura el fichero. No deja nada tocado (termina con git diff limpio).
//
// Uso: node scripts/verificar-mutantes-external-calendar.mjs [subcadena para filtrar]
import { execFileSync } from "node:child_process";
import { readFileSync, writeFileSync } from "node:fs";

const VIEW = "frontend/src/external-calendar.tsx";
const API = "frontend/src/external-calendar-api.ts";
const TODAY = "frontend/src/today-external-calendar.tsx";

const SOURCES = [VIEW, API, TODAY];
const originals = new Map(
  SOURCES.map((source) => [source, readFileSync(source, "utf8")]),
);

const SPECS = {
  [VIEW]: "src/external-calendar.test.tsx",
  [API]: "src/external-calendar-api.test.ts",
  [TODAY]: "src/today-external-calendar.test.tsx",
};

/** [racimo, nombre, fichero, texto exacto a buscar, texto de reemplazo] */
const MUTANTS = [
  [
    "A",
    "loadEvents setInvalidList(true) -> false",
    VIEW,
    `      setInvalidList(true);\n      setEvents([]);`,
    `      setInvalidList(false);\n      setEvents([]);`,
  ],
  [
    "A",
    "loadEvents setEvents([]) -> lista no vacia",
    VIEW,
    `      setInvalidList(true);\n      setEvents([]);`,
    `      setInvalidList(true);\n      setEvents([{ uid: "s", summary: "Stryker was here", startAt: "2030-01-07T08:00:00Z", endAt: "2030-01-07T09:00:00Z", allDay: false }]);`,
  ],
  [
    "A",
    "loadEvents setInvalidList(false) del camino feliz -> true",
    VIEW,
    `      setEvents(view.items);\n      setInvalidList(false);`,
    `      setEvents(view.items);\n      setInvalidList(true);`,
  ],
  [
    "A",
    "vista literal 'No hay eventos en la ventana guardada.' -> ''",
    VIEW,
    `        <p>No hay eventos en la ventana guardada.</p>`,
    `        <p></p>`,
  ],
  [
    "A",
    "vista events.length === 0 -> !==",
    VIEW,
    `      ) : events.length === 0 ? (`,
    `      ) : events.length !== 0 ? (`,
  ],
  [
    "B",
    "montaje ternario de conectores -> siempre generico",
    VIEW,
    `          error instanceof ConnectorsDisabledError\n            ? error.message\n            : "No se ha podido cargar tu calendario externo.",`,
    `          "No se ha podido cargar tu calendario externo.",`,
  ],
  [
    "B",
    "montaje ternario de conectores -> siempre el de conectores",
    VIEW,
    `          error instanceof ConnectorsDisabledError\n            ? error.message\n            : "No se ha podido cargar tu calendario externo.",`,
    `          new ConnectorsDisabledError().message,`,
  ],
  [
    "B",
    "montaje literal generico -> ''",
    VIEW,
    `            : "No se ha podido cargar tu calendario externo.",`,
    `            : "",`,
  ],
  [
    "B",
    "montaje setLoaded(true) del catch -> false",
    VIEW,
    `        if (controller.signal.aborted) return;\n        setLoaded(true);`,
    `        if (controller.signal.aborted) return;\n        setLoaded(false);`,
  ],
  [
    "B",
    "vista loaded ? ... : null -> true",
    VIEW,
    `      ) : loaded ? (`,
    `      ) : true ? (`,
  ],
  [
    "B",
    "vista literal 'Todavia no tienes ningun calendario externo.' -> ''",
    VIEW,
    `          Todavía no tienes ningún calendario externo.\n        </p>`,
    `        </p>`,
  ],
  [
    "C",
    "limpieza del efecto -> no aborta nada",
    VIEW,
    `    return () => inFlight.current?.abort();`,
    `    return () => undefined;`,
  ],
  [
    "C",
    "limpieza del efecto -> aborta solo el controlador del montaje",
    VIEW,
    `    return () => inFlight.current?.abort();`,
    `    return () => controller.abort();`,
  ],
  [
    "C",
    "save: if (signal.aborted) return del catch -> false",
    VIEW,
    `      const saved = await saveExternalCalendar(label, address, signal);\n      setSubscription(saved);\n      setAddress("");\n      setAnnouncement("Guardado.");\n      await loadEvents(signal);\n    } catch (error) {\n      if (signal.aborted) return;`,
    `      const saved = await saveExternalCalendar(label, address, signal);\n      setSubscription(saved);\n      setAddress("");\n      setAnnouncement("Guardado.");\n      await loadEvents(signal);\n    } catch (error) {\n      if (false) return;`,
  ],
  [
    "C",
    "save: if (!signal.aborted) setBusy('') del finally -> true",
    VIEW,
    `      failed(error);\n    } finally {\n      if (!signal.aborted) setBusy("");\n    }\n  }\n\n  async function synchronise() {`,
    `      failed(error);\n    } finally {\n      if (true) setBusy("");\n    }\n  }\n\n  async function synchronise() {`,
  ],
  [
    "C",
    "synchronise: if (signal.aborted) return del catch -> false",
    VIEW,
    `      if (signal.aborted) return;\n      setAnnouncement("");\n      if (error instanceof ExternalCalendarNotConfiguredError) forget();`,
    `      if (false) return;\n      setAnnouncement("");\n      if (error instanceof ExternalCalendarNotConfiguredError) forget();`,
  ],
  [
    "C",
    "confirmRemoval: if (signal.aborted) return del catch -> false",
    VIEW,
    `      setAnnouncement("Suscripción eliminada.");\n    } catch (error) {\n      if (signal.aborted) return;`,
    `      setAnnouncement("Suscripción eliminada.");\n    } catch (error) {\n      if (false) return;`,
  ],
  [
    "C",
    "confirmRemoval: if (busy) return -> false",
    VIEW,
    `  async function confirmRemoval() {\n    if (busy) return;`,
    `  async function confirmRemoval() {\n    if (false) return;`,
  ],
  [
    "C",
    "confirmRemoval: setBusy('deleting') -> ''",
    VIEW,
    `    setBusy("deleting");\n    setAnnouncement("Eliminando…");`,
    `    setBusy("");\n    setAnnouncement("Eliminando…");`,
  ],
  [
    "C",
    "confirmRemoval: literal 'Eliminando…' -> ''",
    VIEW,
    `    setAnnouncement("Eliminando…");`,
    `    setAnnouncement("");`,
  ],
  [
    "C",
    "confirmRemoval: literal 'Suscripción eliminada.' -> ''",
    VIEW,
    `      setAnnouncement("Suscripción eliminada.");`,
    `      setAnnouncement("");`,
  ],
  [
    "C",
    "confirmRemoval: if (!signal.aborted) setBusy('') del finally -> true",
    VIEW,
    `      failed(error);\n    } finally {\n      if (!signal.aborted) setBusy("");\n    }\n  }\n\n  const zone =`,
    `      failed(error);\n    } finally {\n      if (true) setBusy("");\n    }\n  }\n\n  const zone =`,
  ],
  [
    "C",
    "start(): inFlight.current?.abort() -> sin abortar",
    VIEW,
    `  const start = useCallback(() => {
    inFlight.current?.abort();`,
    `  const start = useCallback(() => {`,
  ],
  [
    "C",
    "montaje: if (controller.signal.aborted) return -> false",
    VIEW,
    `        if (controller.signal.aborted) return;`,
    `        if (false) return;`,
  ],
];

function failedTests(output) {
  return [...output.matchAll(/^\s+×\s+(.+?)\s+\d+ms$/gm)].map(
    (match) => match[1],
  );
}

const filter = process.argv[2] ?? "";
const results = [];
for (const [cluster, name, source, search, replacement] of MUTANTS) {
  const label = `[${cluster}] ${name}`;
  if (filter && !label.includes(filter)) continue;
  const original = originals.get(source);
  const occurrences = original.split(search).length - 1;
  if (occurrences !== 1) {
    console.log(`ANCLA AMBIGUA (${occurrences})  ${label}`);
    results.push({ cluster, name, verdict: "ANCLA AMBIGUA", tests: [] });
    continue;
  }
  writeFileSync(source, original.replace(search, replacement));
  let output = "";
  try {
    output = execFileSync(
      "pnpm",
      ["--dir", "frontend", "exec", "vitest", "run", SPECS[source]],
      { encoding: "utf8", shell: true },
    );
  } catch (error) {
    output = `${error.stdout ?? ""}${error.stderr ?? ""}`;
  }
  writeFileSync(source, original);
  const tests = failedTests(output);
  results.push({
    cluster,
    name,
    verdict: tests.length > 0 ? "MUERE" : "SOBREVIVE",
    tests,
  });
  console.log(
    `${tests.length > 0 ? "MUERE    " : "SOBREVIVE"}  ${label}  ->  ${tests.join(" | ") || "(nadie falla)"}`,
  );
}

for (const source of SOURCES) writeFileSync(source, originals.get(source));
const dead = results.filter((result) => result.verdict === "MUERE").length;
console.log(`\nTOTAL: ${dead} mueren de ${results.length}`);
writeFileSync(
  "progress/verificacion_mutantes_external_calendar.json",
  `${JSON.stringify(results, null, 2)}\n`,
);
