// Verifica a mano que los oráculos nuevos discriminan: aplica cada mutante superviviente
// al fichero de producción, ejecuta la suite de automatizaciones y anota qué pruebas caen.
// No modifica nada de forma permanente: restaura el fichero tras cada pasada.
//
// Uso: node scripts/verificar-mutantes-automations.mjs [filtro]
import { execFileSync } from "node:child_process";
import { readFileSync, writeFileSync } from "node:fs";

const VIEW = "frontend/src/automations.tsx";
const CLIENT = "frontend/src/automations-api.ts";
const originals = {
  [VIEW]: readFileSync(VIEW, "utf8"),
  [CLIENT]: readFileSync(CLIENT, "utf8"),
};

/** [fichero, nombre, texto exacto a buscar, texto de reemplazo] */
const MUTANTS = [
  [
    CLIENT,
    "124 uuid typeof -> true",
    `    typeof value === "string" &&
    /^[0-9a-f]`,
    `    true &&
    /^[0-9a-f]`,
  ],
  [
    CLIENT,
    "125 uuid regex sin ancla inicial",
    `/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(`,
    `/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(`,
  ],
  [
    CLIENT,
    "125 uuid regex sin ancla final",
    `/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(`,
    `/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i.test(`,
  ],
  [
    CLIENT,
    "132 nullableUuid -> true",
    `  return value === null || uuid(value);`,
    `  return true;`,
  ],
  [
    CLIENT,
    "137 action exact -> true",
    `    exact(
      value,
      "type projectId titleTemplate criterionTemplate estimatedMinutes",
    ) &&`,
    `    true &&`,
  ],
  [
    CLIENT,
    "141 action type CREATE_TASK -> true",
    `    value.type === "CREATE_TASK"
  )
    return (
      uuid(value.projectId) &&
      typeof value.titleTemplate === "string" &&`,
    `    true
  )
    return (
      uuid(value.projectId) &&
      typeof value.titleTemplate === "string" &&`,
  ],
  [
    CLIENT,
    "144 action uuid(projectId) -> true",
    `      uuid(value.projectId) &&
      typeof value.titleTemplate === "string" &&`,
    `      true &&
      typeof value.titleTemplate === "string" &&`,
  ],
  [
    CLIENT,
    "145 action titleTemplate string -> true",
    `      typeof value.titleTemplate === "string" &&`,
    `      true &&`,
  ],
  [
    CLIENT,
    "146 action criterionTemplate grupo -> true",
    `      (value.criterionTemplate === null ||
        typeof value.criterionTemplate === "string") &&`,
    `      true &&`,
  ],
  [
    CLIENT,
    "147 action criterionTemplate string -> true",
    `        typeof value.criterionTemplate === "string") &&`,
    `        true) &&`,
  ],
  [
    CLIENT,
    "148 action estimatedMinutes grupo -> true",
    `      (value.estimatedMinutes === null ||
        Number.isInteger(value.estimatedMinutes))
    );`,
    `      true
    );`,
  ],
  [
    CLIENT,
    "148 action estimatedMinutes === -> !==",
    `      (value.estimatedMinutes === null ||
        Number.isInteger(value.estimatedMinutes))
    );`,
    `      (value.estimatedMinutes !== null ||
        Number.isInteger(value.estimatedMinutes))
    );`,
  ],
  [
    CLIENT,
    "152 webhook exact -> true",
    `    exact(value, "type endpointId") &&`,
    `    true &&`,
  ],
  [
    CLIENT,
    "153 webhook type -> true",
    `    value.type === "NOTIFY_WEBHOOK" &&
    uuid(value.endpointId)
  );`,
    `    true &&
    uuid(value.endpointId)
  );`,
  ],
  [
    CLIENT,
    "154 webhook uuid(endpointId) -> true",
    `    value.type === "NOTIFY_WEBHOOK" &&
    uuid(value.endpointId)
  );`,
    `    value.type === "NOTIFY_WEBHOOK" &&
    true
  );`,
  ],
  [
    CLIENT,
    "165 automation name string -> true",
    `    typeof value.name === "string" &&`,
    `    true &&`,
  ],
  [
    CLIENT,
    "166 automation enabled boolean -> true",
    `    typeof value.enabled === "boolean" &&`,
    `    true &&`,
  ],
  [
    CLIENT,
    "169 automation condition grupo -> true",
    `    (value.condition === null ||
      (exact(value.condition, "projectId") &&
        uuid(value.condition.projectId))) &&`,
    `    true &&`,
  ],
  [
    CLIENT,
    "170 automation condition exact -> true",
    `      (exact(value.condition, "projectId") &&`,
    `      (true &&`,
  ],
  [
    CLIENT,
    "171 automation condition uuid -> true",
    `        uuid(value.condition.projectId))) &&`,
    `        true)) &&`,
  ],
  [
    CLIENT,
    "181 preview exact -> true",
    `    exact(
      value,
      "type projectId title completionCriterion estimatedMinutes wouldFail",
    ) &&`,
    `    true &&`,
  ],
  [
    CLIENT,
    "185 preview type CREATE_TASK -> true",
    `    value.type === "CREATE_TASK"
  )
    return (
      uuid(value.projectId) &&
      typeof value.title === "string" &&`,
    `    true
  )
    return (
      uuid(value.projectId) &&
      typeof value.title === "string" &&`,
  ],
  [
    CLIENT,
    "188 preview uuid(projectId) -> true",
    `      uuid(value.projectId) &&
      typeof value.title === "string" &&`,
    `      true &&
      typeof value.title === "string" &&`,
  ],
  [
    CLIENT,
    "189 preview title string -> true",
    `      typeof value.title === "string" &&`,
    `      true &&`,
  ],
  [
    CLIENT,
    "190 preview completionCriterion string -> true",
    `      typeof value.completionCriterion === "string" &&`,
    `      true &&`,
  ],
  [
    CLIENT,
    "191 preview estimatedMinutes grupo -> true",
    `      (value.estimatedMinutes === null ||
        Number.isInteger(value.estimatedMinutes)) &&`,
    `      true &&`,
  ],
  [
    CLIENT,
    "193 preview wouldFail grupo -> true",
    `      (value.wouldFail === null || typeof value.wouldFail === "string")`,
    `      true`,
  ],
  [
    CLIENT,
    "196 preview webhook exact -> true",
    `    exact(value, "type endpointId eventId") &&`,
    `    true &&`,
  ],
  [
    CLIENT,
    "197 preview webhook type -> true",
    `    value.type === "NOTIFY_WEBHOOK" &&
    uuid(value.endpointId) &&
    uuid(value.eventId)`,
    `    true &&
    uuid(value.endpointId) &&
    uuid(value.eventId)`,
  ],
  [
    CLIENT,
    "198 preview webhook endpointId -> true",
    `    uuid(value.endpointId) &&
    uuid(value.eventId)`,
    `    true &&
    uuid(value.eventId)`,
  ],
  [
    CLIENT,
    "199 preview webhook eventId -> true",
    `    uuid(value.endpointId) &&
    uuid(value.eventId)`,
    `    uuid(value.endpointId) &&
    true`,
  ],
  [
    CLIENT,
    "205 match exact -> true",
    `    exact(value, "eventId eventType occurredAt preview loopGuarded") &&`,
    `    true &&`,
  ],
  [
    CLIENT,
    "210 match loopGuarded -> true",
    `    typeof value.loopGuarded === "boolean"`,
    `    true`,
  ],
  [
    CLIENT,
    "216 run exact -> true",
    `    exact(
      value,
      "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt",
    ) &&`,
    `    true &&`,
  ],
  [
    CLIENT,
    "228 run errorCode grupo -> true",
    `    (value.errorCode === null || typeof value.errorCode === "string") &&`,
    `    true &&`,
  ],
  [
    CLIENT,
    "278 readAutomations exact -> false",
    `    !exact(body, "items") ||`,
    `    false ||`,
  ],
  [
    CLIENT,
    "357 simulate exact -> false",
    `    !exact(body, "evaluatedEvents matches") ||`,
    `    false ||`,
  ],
  [
    CLIENT,
    "360 simulate every -> some",
    `    !body.matches.every(match)`,
    `    !body.matches.some(match)`,
  ],
  [
    CLIENT,
    "381 runs exact -> false",
    `    !exact(body, "items nextCursor") ||`,
    `    false ||`,
  ],
  [
    CLIENT,
    "384 runs nextCursor -> false",
    `    !(body.nextCursor === null || typeof body.nextCursor === "string")`,
    `    false`,
  ],
  [
    CLIENT,
    "94 mensaje de AutomationFieldErrors -> ''",
    `    super("Revisa los campos indicados.");`,
    `    super("");`,
  ],
  [
    CLIENT,
    "95 nombre de AutomationFieldErrors -> ''",
    `    this.name = "AutomationFieldErrors";`,
    `    this.name = "";`,
  ],
  [
    CLIENT,
    "102 mensaje de AutomationConflict -> ''",
    `    super("Otra pestaña cambió esta regla.");`,
    `    super("");`,
  ],
  [
    CLIENT,
    "103 nombre de AutomationConflict -> ''",
    `    this.name = "AutomationConflict";`,
    `    this.name = "";`,
  ],
  [
    CLIENT,
    "111 mensaje UNKNOWN_EVENT_TYPE -> ''",
    `  UNKNOWN_EVENT_TYPE: "Elige uno de los tipos de evento publicados.",`,
    `  UNKNOWN_EVENT_TYPE: "",`,
  ],
  [
    CLIENT,
    "114 mensaje TOO_LONG -> ''",
    `  TOO_LONG: "El valor es demasiado largo.",`,
    `  TOO_LONG: "",`,
  ],
  [
    CLIENT,
    "116 mensaje OUT_OF_RANGE -> ''",
    `  OUT_OF_RANGE: "El valor está fuera del rango permitido.",`,
    `  OUT_OF_RANGE: "",`,
  ],
  [
    CLIENT,
    "235 puerta de estado 400/422 -> true",
    `  if (response.status === 400 || response.status === 422) {`,
    `  if (true) {`,
  ],
  [
    CLIENT,
    "238 body -> true",
    `      body &&
      typeof body === "object" &&`,
    `      true &&
      typeof body === "object" &&`,
  ],
  [
    CLIENT,
    "239 typeof body object -> true",
    `      typeof body === "object" &&
      "errors" in body &&`,
    `      true &&
      "errors" in body &&`,
  ],
  [
    CLIENT,
    "246 error -> true",
    `          error &&
          typeof error === "object" &&`,
    `          true &&
          typeof error === "object" &&`,
  ],
  [
    CLIENT,
    "247 typeof error object -> true",
    `          typeof error === "object" &&
          "field" in error &&`,
    `          true &&
          "field" in error &&`,
  ],
  [
    CLIENT,
    "249 typeof error.field string -> true",
    `          typeof error.field === "string" &&`,
    `          true &&`,
  ],
  [
    CLIENT,
    "251 typeof error.code string -> true",
    `          typeof error.code === "string"`,
    `          true`,
  ],
  [
    CLIENT,
    "255 length > 0 -> >= 0",
    `      if (Object.keys(fields).length > 0)`,
    `      if (Object.keys(fields).length >= 0)`,
  ],
  [
    CLIENT,
    "263 json throwIfAborted -> ;",
    `async function json(response: Response, signal: AbortSignal) {
  signal.throwIfAborted();
`,
    `async function json(response: Response, signal: AbortSignal) {
`,
  ],
  [
    CLIENT,
    "271 opciones del read -> {}",
    `    await apiRequest("/api/v1/me/automations", {
      signal,
      headers: { Accept: "application/json" },
    }),`,
    `    await apiRequest("/api/v1/me/automations", {}),`,
  ],
  [
    CLIENT,
    "273 Accept del read -> ''",
    `    await apiRequest("/api/v1/me/automations", {
      signal,
      headers: { Accept: "application/json" },
    }),`,
    `    await apiRequest("/api/v1/me/automations", {
      signal,
      headers: { Accept: "" },
    }),`,
  ],
  [
    CLIENT,
    "293 write throwIfAborted -> ;",
    `  version?: number,
) {
  signal.throwIfAborted();
`,
    `  version?: number,
) {
`,
  ],
  [
    CLIENT,
    "294 cabeceras de escritura -> {}",
    `  const headers: Record<string, string> = {
    Accept: "application/json",
    "Content-Type": "application/json",
  };`,
    `  const headers: Record<string, string> = {};`,
  ],
  [
    CLIENT,
    "295 Accept de escritura -> ''",
    `    Accept: "application/json",
    "Content-Type": "application/json",`,
    `    Accept: "",
    "Content-Type": "application/json",`,
  ],
  [
    CLIENT,
    "296 Content-Type de escritura -> ''",
    `    "Content-Type": "application/json",`,
    `    "Content-Type": "",`,
  ],
  [
    CLIENT,
    "298 If-Match siempre",
    `  if (version !== undefined) headers["If-Match"] = \`"\${version}"\`;`,
    `  if (true) headers["If-Match"] = \`"\${version}"\`;`,
  ],
  [
    CLIENT,
    "312 create throwIfAborted -> ;",
    `  const response = await write("/api/v1/me/automations", "POST", draft, signal);
  signal.throwIfAborted();
`,
    `  const response = await write("/api/v1/me/automations", "POST", draft, signal);
`,
  ],
  [
    CLIENT,
    "313 puerta del 201 -> true",
    `  if (response.status !== 201) await failure(response);`,
    `  if (true) await failure(response);`,
  ],
  [
    CLIENT,
    "315 create shape -> false",
    `  const body: unknown = await response.json();
  if (!automation(body)) throw incompatible();`,
    `  const body: unknown = await response.json();
  if (false) throw incompatible();`,
  ],
  [
    CLIENT,
    "329 replace shape -> false",
    `  if (!automation(body)) throw incompatible();
  return body;
}

export async function deleteAutomation(`,
    `  if (false) throw incompatible();
  return body;
}

export async function deleteAutomation(`,
  ],
  [
    CLIENT,
    "338 delete throwIfAborted -> ;",
    `) {
  signal.throwIfAborted();
  const response = await apiRequest(`,
    `) {
  const response = await apiRequest(`,
  ],
  [
    CLIENT,
    "339 URL del delete -> ''",
    `  const response = await apiRequest(\`/api/v1/me/automations/\${id}\`, {`,
    `  const response = await apiRequest(\`\`, {`,
  ],
  [
    CLIENT,
    "344 delete throwIfAborted tras la respuesta -> ;",
    `  });
  signal.throwIfAborted();
  if (response.status !== 204)`,
    `  });
  if (response.status !== 204)`,
  ],
  [
    CLIENT,
    "345 puerta del 204 -> false",
    `  if (response.status !== 204) await failure(response);`,
    `  if (false) await failure(response);`,
  ],
  [
    CLIENT,
    "371 runs throwIfAborted -> ;",
    `) {
  signal.throwIfAborted();
  const query = `,
    `) {
  const query = `,
  ],
  [
    CLIENT,
    "374 opciones del historial -> {}",
    `    await apiRequest(\`/api/v1/me/automations/\${id}/runs\${query}\`, {
      signal,
      headers: { Accept: "application/json" },
    }),`,
    `    await apiRequest(\`/api/v1/me/automations/\${id}/runs\${query}\`, {}),`,
  ],
  [
    CLIENT,
    "376 Accept del historial -> ''",
    `    await apiRequest(\`/api/v1/me/automations/\${id}/runs\${query}\`, {
      signal,
      headers: { Accept: "application/json" },
    }),`,
    `    await apiRequest(\`/api/v1/me/automations/\${id}/runs\${query}\`, {
      signal,
      headers: { Accept: "" },
    }),`,
  ],
];

function failedTests(output) {
  return [...output.matchAll(/^\s+×\s+(.+?)\s+\d+ms$/gm)].map(
    (match) => match[1],
  );
}

const filter = process.argv[2];
const results = [];
for (const [file, name, search, replacement] of MUTANTS) {
  if (filter && !name.includes(filter)) continue;
  const original = originals[file];
  const occurrences = original.split(search).length - 1;
  if (occurrences !== 1) {
    results.push({
      file,
      name,
      verdict: `ANCLA AMBIGUA (${occurrences} apariciones)`,
      tests: [],
    });
    console.log(`AMBIGUA   ${name} (${occurrences})`);
    continue;
  }
  writeFileSync(file, original.replace(search, replacement));
  let output = "";
  try {
    output = execFileSync(
      "pnpm",
      ["--dir", "frontend", "exec", "vitest", "run", "src/automations"],
      { encoding: "utf8", shell: true },
    );
  } catch (error) {
    output = `${error.stdout ?? ""}${error.stderr ?? ""}`;
  }
  writeFileSync(file, original);
  const tests = failedTests(output);
  const compiles = !/Error: Failed to (parse|load)/.test(output);
  results.push({
    file,
    name,
    verdict: tests.length > 0 ? "MUERE" : compiles ? "SOBREVIVE" : "NO COMPILA",
    tests,
  });
  console.log(
    `${tests.length > 0 ? "MUERE   " : "SOBREVIVE"}  ${name}  ->  ${tests.join(" | ") || "(nadie falla)"}`,
  );
}

for (const [file, original] of Object.entries(originals))
  writeFileSync(file, original);
const dead = results.filter((result) => result.verdict === "MUERE").length;
console.log(`\nTOTAL: ${dead} mueren de ${results.length}`);
writeFileSync(
  "progress/verificacion_mutantes_automations.json",
  `${JSON.stringify(results, null, 2)}\n`,
);
