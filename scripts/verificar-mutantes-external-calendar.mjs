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
  [
    "D",
    "failed(): se borra el bloque de ConnectorsDisabledError",
    VIEW,
    `    if (error instanceof ConnectorsDisabledError) {\n      setFailure(\`\${error.message} \${UNCERTAIN}\`);\n      return;\n    }\n`,
    ``,
  ],
  [
    "D",
    "failed(): el prefijo de conectores se pierde al componer",
    VIEW,
    `      setFailure(\`\${error.message} \${UNCERTAIN}\`);`,
    `      setFailure(UNCERTAIN);`,
  ],
  [
    "D",
    "save: setAnnouncement('') del catch -> se borra",
    VIEW,
    `      await loadEvents(signal);\n    } catch (error) {\n      if (signal.aborted) return;\n      setAnnouncement("");`,
    `      await loadEvents(signal);\n    } catch (error) {\n      if (signal.aborted) return;`,
  ],
  [
    "D",
    "vista: separador ' … ' -> ''",
    VIEW,
    `            <span aria-hidden="true"> … </span>`,
    `            <span aria-hidden="true"></span>`,
  ],
  [
    "D",
    "vista: aria-hidden='true' -> ''",
    VIEW,
    `            <span aria-hidden="true"> … </span>`,
    `            <span aria-hidden=""> … </span>`,
  ],
  [
    "D",
    "montaje: setLabel(subscription.label) -> ''",
    VIEW,
    `          setLabel(snapshot.subscription.label);`,
    `          setLabel("");`,
  ],
  [
    "D",
    "synchronise: literal 'Sincronización fallida.' -> ''",
    VIEW,
    `          : "Sincronización fallida.",`,
    `          : "",`,
  ],
  [
    "D",
    "synchronise: el ternario del anuncio -> siempre 'Sincronizado.'",
    VIEW,
    `      setAnnouncement(\n        outcome.subscription.lastStatus === "OK"\n          ? "Sincronizado."\n          : "Sincronización fallida.",\n      );`,
    `      setAnnouncement("Sincronizado.");`,
  ],
  [
    "D",
    "synchronise: la guarda de recarga -> recarga siempre",
    VIEW,
    `      if (outcome.subscription.lastStatus === "OK") await loadEvents(signal);`,
    `      await loadEvents(signal);`,
  ],
  [
    "D",
    "synchronise: la guarda de recarga -> no recarga nunca",
    VIEW,
    `      if (outcome.subscription.lastStatus === "OK") await loadEvents(signal);`,
    `      if (false) await loadEvents(signal);`,
  ],
  [
    "D",
    "hoy: pendingSync ? ... : null -> true",
    TODAY,
    `          {pendingSync ? <p>Sincronización pendiente.</p> : null}`,
    `          {true ? <p>Sincronización pendiente.</p> : null}`,
  ],
  [
    "D",
    "hoy: el ternario de fallo -> true",
    TODAY,
    `      {reading.kind === "unreadable" || reading.kind === "unreachable" ? (`,
    `      {true ? (`,
  ],
  [
    "E",
    "regex de uuid: se quita el anclaje ^",
    API,
    `    !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(`,
    `    !/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(`,
  ],
  [
    "E",
    "regex de uuid: se quita el anclaje $",
    API,
    `    !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(`,
    `    !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i.test(`,
  ],
  [
    "E",
    "subscriptionOf: typeof label -> guarda apagada",
    API,
    `    typeof value.label !== "string" ||\n    value.label.length === 0 ||`,
    ``,
  ],
  [
    "E",
    "subscriptionOf: label.length === 0 -> !==",
    API,
    `    value.label.length === 0 ||`,
    `    value.label.length !== 0 ||`,
  ],
  [
    "E",
    "subscriptionOf: typeof urlHost -> guarda apagada",
    API,
    `    typeof value.urlHost !== "string" ||`,
    ``,
  ],
  [
    "E",
    "subscriptionOf: typeof urlTail !== -> ===",
    API,
    `    typeof value.urlTail !== "string" ||`,
    `    typeof value.urlTail === "string" ||`,
  ],
  [
    "E",
    "subscriptionOf: typeof urlTail contra ''",
    API,
    `    typeof value.urlTail !== "string" ||`,
    `    typeof value.urlTail !== "" ||`,
  ],
  [
    "E",
    "subscriptionOf: typeof id !== -> ===",
    API,
    `    typeof value.id !== "string" ||`,
    `    typeof value.id === "string" ||`,
  ],
  [
    "E",
    "subscriptionOf: typeof id contra ''",
    API,
    `    typeof value.id !== "string" ||`,
    `    typeof value.id !== "" ||`,
  ],
  [
    "E",
    "subscriptionOf: optionalInstant(lastAttemptAt) -> guarda apagada",
    API,
    `    !optionalInstant(value.lastAttemptAt) ||`,
    ``,
  ],
  [
    "E",
    "subscriptionOf: snapshotZoneId -> guarda apagada",
    API,
    `    !(\n      value.snapshotZoneId === null || typeof value.snapshotZoneId === "string"\n    ) ||`,
    ``,
  ],
  [
    "E",
    "subscriptionOf: counter(skippedRecurring) -> guarda apagada",
    API,
    `    !counter(value.skippedRecurring) ||`,
    ``,
  ],
  [
    "E",
    "subscriptionOf: counter(skippedCancelled) -> guarda apagada",
    API,
    `    !counter(value.skippedCancelled) ||`,
    ``,
  ],
  [
    "E",
    "subscriptionOf: counter(skippedInvalid) -> guarda apagada",
    API,
    `    !counter(value.skippedInvalid) ||`,
    ``,
  ],
  [
    "E",
    "subscriptionOf: typeof truncated -> guarda apagada",
    API,
    `    typeof value.truncated !== "boolean" ||`,
    ``,
  ],
  [
    "E",
    "subscriptionOf: instant(updatedAt) -> guarda apagada",
    API,
    `    !instant(value.updatedAt)\n  )`,
    `    false\n  )`,
  ],
  [
    "E",
    "counter: Number.isInteger -> siempre cierto",
    API,
    `  return Number.isInteger(value) && (value as number) >= 0;`,
    `  return (value as number) >= 0;`,
  ],
  [
    "E",
    "counter: >= 0 -> > 0",
    API,
    `  return Number.isInteger(value) && (value as number) >= 0;`,
    `  return Number.isInteger(value) && (value as number) > 0;`,
  ],
  [
    "E",
    "eventOf: instant(endAt) -> guarda apagada",
    API,
    `    !instant(value.endAt) ||`,
    ``,
  ],
  [
    "E",
    "eventOf: typeof uid -> guarda apagada",
    API,
    `    typeof value.uid !== "string" ||`,
    ``,
  ],
  [
    "E",
    "eventOf: typeof allDay -> guarda apagada",
    API,
    `    typeof value.allDay !== "boolean"\n  )`,
    `    false\n  )`,
  ],
  [
    "E",
    "snapshotOf: exact -> guarda apagada",
    API,
    `    !exact(value, "configured subscription") ||\n    typeof value.configured !== "boolean"`,
    `    typeof value.configured !== "boolean"`,
  ],
  [
    "E",
    "snapshotOf: typeof configured !== -> ===",
    API,
    `    typeof value.configured !== "boolean"\n  )`,
    `    typeof value.configured === "boolean"\n  )`,
  ],
  [
    "E",
    "snapshotOf: typeof configured contra ''",
    API,
    `    typeof value.configured !== "boolean"\n  )`,
    `    typeof value.configured !== ""\n  )`,
  ],
  [
    "E",
    "json: invalid() del cuerpo no-JSON -> null",
    API,
    `  return (await response.json().catch(() => invalid())) as unknown;`,
    `  return (await response.json().catch(() => null)) as unknown;`,
  ],
  [
    "E",
    "json: cabecera Accept -> ''",
    API,
    `    headers: { Accept: "application/json", ...options.headers },`,
    `    headers: { Accept: "", ...options.headers },`,
  ],
  [
    "E",
    "json: se pierde el spread de options.headers",
    API,
    `    headers: { Accept: "application/json", ...options.headers },`,
    `    headers: { Accept: "application/json" },`,
  ],
  [
    "E",
    "writing: Content-Type -> ''",
    API,
    `  headers: { "Content-Type": "application/json" },`,
    `  headers: { "Content-Type": "" },`,
  ],
  [
    "E",
    "save: if (!snapshot.configured) invalid() -> guarda apagada",
    API,
    `  if (!snapshot.configured) invalid();`,
    ``,
  ],
  [
    "E",
    "delete: throwIfAborted previo -> se borra",
    API,
    `  signal?.throwIfAborted();\n  const response = await apiRequest(ROUTE, { method: "DELETE", signal });`,
    `  const response = await apiRequest(ROUTE, { method: "DELETE", signal });`,
  ],
  [
    "E",
    "delete: throwIfAborted posterior -> se borra",
    API,
    `  const response = await apiRequest(ROUTE, { method: "DELETE", signal });\n  signal?.throwIfAborted();`,
    `  const response = await apiRequest(ROUTE, { method: "DELETE", signal });`,
  ],
  [
    "E",
    "sync: exact(performed subscription) -> guarda apagada",
    API,
    `    !exact(body, "performed subscription") ||\n    typeof body.performed !== "boolean"`,
    `    typeof body.performed !== "boolean"`,
  ],
  [
    "E",
    "readExternalEvents: typeof configured -> guarda apagada",
    API,
    `    typeof body.configured !== "boolean" ||\n    !optionalInstant(body.lastSyncAt) ||`,
    `    !optionalInstant(body.lastSyncAt) ||`,
  ],
  [
    "E",
    "readExternalEvents: optionalInstant(lastSyncAt) -> guarda apagada",
    API,
    `    !optionalInstant(body.lastSyncAt) ||`,
    ``,
  ],
  [
    "E",
    "readExternalEvents: lastStatus -> guarda apagada",
    API,
    `    !(\n      body.lastStatus === null || STATUSES.includes(body.lastStatus as string)\n    ) ||`,
    ``,
  ],
  [
    "E",
    "readExternalEvents: Array.isArray(items) -> guarda apagada",
    API,
    `    !Array.isArray(body.items)\n  )`,
    `    false\n  )`,
  ],
  [
    "E",
    "refuse: 503 && -> ||",
    API,
    `  if (response.status === 503 && body?.code === "CONNECTORS_DISABLED")`,
    `  if (response.status === 503 || body?.code === "CONNECTORS_DISABLED")`,
  ],
  [
    "E",
    "refuse: 404 && -> ||",
    API,
    `    response.status === 404 &&\n    body?.code === "EXTERNAL_CALENDAR_NOT_CONFIGURED"`,
    `    response.status === 404 ||\n    body?.code === "EXTERNAL_CALENDAR_NOT_CONFIGURED"`,
  ],
  [
    "E",
    "refuse: 400 && -> ||",
    API,
    `  if (response.status === 400 && body?.code === "VALIDATION_ERROR") {`,
    `  if (response.status === 400 || body?.code === "VALIDATION_ERROR") {`,
  ],
  [
    "E",
    "problem: se quita el .clone()",
    API,
    `  return (await response\n    .clone()\n    .json()`,
    `  return (await response\n    .json()`,
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
