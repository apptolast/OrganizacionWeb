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
  ...[
    ["30", `  "ProjectUpdated.v1": "Proyecto editado",`],
    ["31", `  "ProjectStatusChanged.v1": "Estado de proyecto cambiado",`],
    ["33", `  "SubtaskCreated.v1": "Subtarea creada",`],
    ["34", `  "TaskStatusChanged.v1": "Estado de tarea cambiado",`],
    ["35", `  "BlockPlanned.v1": "Bloque planificado",`],
    ["36", `  "BlockChanged.v1": "Bloque modificado",`],
    ["37", `  "WorkSessionStarted.v1": "Sesión iniciada",`],
    ["38", `  "WorkSessionStateChanged.v1": "Sesión pausada o reanudada",`],
    ["39", `  "WorkSessionExtended.v1": "Sesión ampliada",`],
    ["51", `  TITLE_TOO_LONG: "título demasiado largo",`],
    ["52", `  CRITERION_TOO_LONG: "criterio demasiado largo",`],
    ["53", `  ENDPOINT_NOT_FOUND: "endpoint no encontrado",`],
    ["54", `  TARGET_NOT_FOUND: "destino no encontrado",`],
    ["65", `  "{{event.type}}": "TaskCreated.v1",`],
    ["67", `  "{{project.name}}": "Marketing",`],
    ["68", `  "{{occurredAt}}": "2026-09-08T10:15:30.123456Z",`],
    ["94", `    eventType: "TaskCreated.v1",`],
    ["612", `      name: "automation-name",`],
    ["613", `      "action.titleTemplate": "automation-title",`],
    ["614", `      "action.criterionTemplate": "automation-criterion",`],
    ["615", `      "trigger.eventType": "automation-trigger",`],
    ["616", `      "condition.projectId": "automation-condition",`],
    ["617", `    }[field] ?? "automation-name"`],
  ].map(([line, text]) => [
    VIEW,
    `${line} StringLiteral -> ''`,
    text,
    text.replace(/"[^"]*",?$/, (tail) => (tail.endsWith(",") ? `"",` : `""`)),
  ]),
  [
    VIEW,
    "515 separador de marcadores -> ''",
    `Marcadores disponibles: {PLACEHOLDERS.join(", ")}`,
    `Marcadores disponibles: {PLACEHOLDERS.join("")}`,
  ],
  [
    VIEW,
    "475 opciones del disparador -> undefined",
    `            {EVENT_TYPES.map((type) => (
              <option key={type} value={type}>
                {TRIGGER_LABELS[type]}
              </option>
            ))}`,
    `            {EVENT_TYPES.map(() => undefined)}`,
  ],
  [
    VIEW,
    "R4 93 nombre en blanco -> Stryker",
    `    name: "",`,
    `    name: "Stryker was here!",`,
  ],
  [
    VIEW,
    "R4 95 conditionProjectId en blanco -> Stryker",
    `    conditionProjectId: "",`,
    `    conditionProjectId: "Stryker was here!",`,
  ],
  [
    VIEW,
    "R4 97 titleTemplate por omisión -> ''",
    `    titleTemplate: "Revisar {{task.title}}",`,
    `    titleTemplate: "",`,
  ],
  [
    VIEW,
    "R4 98 criterionTemplate en blanco -> Stryker",
    `    criterionTemplate: "",`,
    `    criterionTemplate: "Stryker was here!",`,
  ],
  [
    VIEW,
    "R4 99 estimatedMinutes en blanco -> Stryker",
    `    estimatedMinutes: "",`,
    `    estimatedMinutes: "Stryker was here!",`,
  ],
  [
    VIEW,
    "R4 108 conditionProjectId LogicalOperator",
    `    conditionProjectId: rule.condition?.projectId ?? "",`,
    `    conditionProjectId: (rule.condition?.projectId && "") as string,`,
  ],
  [
    VIEW,
    "R4 108 conditionProjectId StringLiteral",
    `    conditionProjectId: rule.condition?.projectId ?? "",`,
    `    conditionProjectId: rule.condition?.projectId ?? "Stryker was here!",`,
  ],
  [
    VIEW,
    "R4 109 projectId ternario -> false",
    `    projectId: rule.action.type === "CREATE_TASK" ? rule.action.projectId : "",`,
    `    projectId: false ? (rule.action as never) : "",`,
  ],
  [
    VIEW,
    "R4 111 titleTemplate ternario -> false",
    `      rule.action.type === "CREATE_TASK" ? rule.action.titleTemplate : "",`,
    `      false ? (rule.action as never) : "",`,
  ],
  [
    VIEW,
    "R4 113 criterionTemplate ternario -> false",
    `      rule.action.type === "CREATE_TASK"
        ? (rule.action.criterionTemplate ?? "")
        : "",`,
    `      false
        ? ((rule.action as never) ?? "")
        : "",`,
  ],
  [
    VIEW,
    "R4 114 criterionTemplate LogicalOperator",
    `        ? (rule.action.criterionTemplate ?? "")`,
    `        ? ((rule.action.criterionTemplate && "") as string)`,
  ],
  [
    VIEW,
    "R4 114 criterionTemplate StringLiteral",
    `        ? (rule.action.criterionTemplate ?? "")`,
    `        ? (rule.action.criterionTemplate ?? "Stryker was here!")`,
  ],
  [
    VIEW,
    "R4 117 estimatedMinutes ternario -> false",
    `      rule.action.type === "CREATE_TASK" &&
      rule.action.estimatedMinutes !== null`,
    `      false &&
      (rule.action as never) !== null`,
  ],
  [
    VIEW,
    "R4 118 estimatedMinutes !== -> ===",
    `      rule.action.estimatedMinutes !== null`,
    `      rule.action.estimatedMinutes === null`,
  ],
  [
    VIEW,
    "R4 125 objeto de draftOf -> {}",
    `function draftOf(editing: Editing): AutomationDraft {
  return {`,
    `function draftOf(editing: Editing): AutomationDraft {
  return {} as never;
  return {`,
  ],
  [
    VIEW,
    "R4 127 enabled -> false",
    `    enabled: editing.rule?.enabled ?? true,`,
    `    enabled: editing.rule?.enabled ?? false,`,
  ],
  [
    VIEW,
    "R4 127 enabled LogicalOperator",
    `    enabled: editing.rule?.enabled ?? true,`,
    `    enabled: (editing.rule?.enabled && true) as boolean,`,
  ],
  [
    VIEW,
    "R4 128 trigger -> {}",
    `    trigger: { eventType: editing.eventType },`,
    `    trigger: {} as never,`,
  ],
  [
    VIEW,
    "R4 130 condición === '' -> true",
    `      editing.conditionProjectId === ""
        ? null
        : { projectId: editing.conditionProjectId },`,
    `      true
        ? null
        : { projectId: editing.conditionProjectId },`,
  ],
  [
    VIEW,
    "R4 130 condición === '' -> !==",
    `      editing.conditionProjectId === ""
        ? null`,
    `      editing.conditionProjectId !== ""
        ? null`,
  ],
  [
    VIEW,
    "R4 133 acción -> {}",
    `    action: {
      type: "CREATE_TASK",
      projectId: editing.projectId,
      titleTemplate: editing.titleTemplate,
      criterionTemplate:
        editing.criterionTemplate === "" ? null : editing.criterionTemplate,
      estimatedMinutes:
        editing.estimatedMinutes === ""
          ? null
          : Number(editing.estimatedMinutes),
    },`,
    `    action: {} as never,`,
  ],
  [
    VIEW,
    "R4 134 tipo de acción -> ''",
    `      type: "CREATE_TASK",`,
    `      type: "" as never,`,
  ],
  [
    VIEW,
    "R4 138 criterio === '' -> true",
    `        editing.criterionTemplate === "" ? null : editing.criterionTemplate,`,
    `        true ? null : editing.criterionTemplate,`,
  ],
  [
    VIEW,
    "R4 138 criterio === '' -> !==",
    `        editing.criterionTemplate === "" ? null : editing.criterionTemplate,`,
    `        editing.criterionTemplate !== "" ? null : editing.criterionTemplate,`,
  ],
  [
    VIEW,
    "R4 140 minutos === '' -> true",
    `        editing.estimatedMinutes === ""
          ? null
          : Number(editing.estimatedMinutes),`,
    `        true
          ? null
          : Number(editing.estimatedMinutes),`,
  ],
  [
    VIEW,
    "R4 140 minutos === '' -> !==",
    `        editing.estimatedMinutes === ""
          ? null`,
    `        editing.estimatedMinutes !== ""
          ? null`,
  ],
  [
    VIEW,
    "R4 243 nombre de proyecto OptionalChaining",
    `  return projects.find((project) => project.id === id)?.name ?? id;`,
    `  return projects.find((project) => project.id === id).name ?? id;`,
  ],
  [
    VIEW,
    "R4 243 nombre de proyecto -> true",
    `  return projects.find((project) => project.id === id)?.name ?? id;`,
    `  return projects.find(() => true)?.name ?? id;`,
  ],
  [
    VIEW,
    "R4 403 destino por omisión (vacío) OptionalChaining",
    `          <button
            type="button"
            onClick={() => setEditing(blank(projects[0]?.id ?? ""))}
          >
            Nueva regla
          </button>
        </div>`,
    `          <button
            type="button"
            onClick={() => setEditing(blank(projects[0].id ?? ""))}
          >
            Nueva regla
          </button>
        </div>`,
  ],
  [
    VIEW,
    "R4 449 destino por omisión (lista) OptionalChaining",
    `          <button
            type="button"
            onClick={() => setEditing(blank(projects[0]?.id ?? ""))}
          >
            Nueva regla
          </button>
        </>`,
    `          <button
            type="button"
            onClick={() => setEditing(blank(projects[0].id ?? ""))}
          >
            Nueva regla
          </button>
        </>`,
  ],
  [
    VIEW,
    "R4 449 botón de nueva regla (lista) -> undefined",
    `            onClick={() => setEditing(blank(projects[0]?.id ?? ""))}
          >
            Nueva regla
          </button>
        </>`,
    `            onClick={() => undefined}
          >
            Nueva regla
          </button>
        </>`,
  ],
  [
    VIEW,
    "R4 468 cambio de disparador -> undefined",
    `            onChange={(event) =>
              setEditing({
                ...editing,
                eventType: event.target.value as EventType,
              })
            }`,
    `            onChange={() => undefined}`,
  ],
  [
    VIEW,
    "R4 485 cambio de condición -> undefined",
    `            onChange={(event) =>
              setEditing({ ...editing, conditionProjectId: event.target.value })
            }`,
    `            onChange={() => undefined}`,
  ],
  [
    VIEW,
    "R4 510 cambio de criterio -> undefined",
    `            onChange={(value) =>
              setEditing({ ...editing, criterionTemplate: value })
            }`,
    `            onChange={() => undefined}`,
  ],
  [
    VIEW,
    "R4 272 lista tras crear -> []",
    `            : [...current, saved],`,
    `            : [],`,
  ],
  [
    VIEW,
    "R4 270 some -> every",
    `          : current.some((rule) => rule.id === saved.id)`,
    `          : current.every((rule) => rule.id === saved.id)`,
  ],
  [
    VIEW,
    "R4 271 sustitución en la lista -> undefined",
    `            ? current.map((rule) => (rule.id === saved.id ? saved : rule))`,
    `            ? current.map(() => undefined as never)`,
  ],
  [
    VIEW,
    "R5 actionOf guarda -> true",
    `  if (editing.rule && editing.rule.action.type !== "CREATE_TASK")`,
    `  if (true)`,
  ],
  [
    VIEW,
    "R5 actionOf guarda -> false",
    `  if (editing.rule && editing.rule.action.type !== "CREATE_TASK")`,
    `  if (false)`,
  ],
  [
    VIEW,
    "R5 actionOf LogicalOperator",
    `  if (editing.rule && editing.rule.action.type !== "CREATE_TASK")`,
    `  if (editing.rule || editing.rule.action.type !== "CREATE_TASK")`,
  ],
  [
    VIEW,
    "R5 actionOf EqualityOperator",
    `  if (editing.rule && editing.rule.action.type !== "CREATE_TASK")`,
    `  if (editing.rule && editing.rule.action.type === "CREATE_TASK")`,
  ],
  [
    VIEW,
    "R5 actionOf StringLiteral",
    `  if (editing.rule && editing.rule.action.type !== "CREATE_TASK")`,
    `  if (editing.rule && editing.rule.action.type !== "")`,
  ],
  [
    VIEW,
    "R5 417 destino de la fila -> true",
    `                  {rule.action.type === "CREATE_TASK"
                    ? nameOfProject(rule.action.projectId)
                    : "Webhook"}`,
    `                  {true
                    ? nameOfProject((rule.action as never)?.projectId)
                    : "Webhook"}`,
  ],
  [
    VIEW,
    "R5 419 Webhook -> ''",
    `                    : "Webhook"}`,
    `                    : ""}`,
  ],
  [
    VIEW,
    "R5 545 previsualización -> true",
    `                      {match.preview.type === "CREATE_TASK"
                        ? match.preview.title
                        : "Aviso al webhook"}`,
    `                      {true
                        ? (match.preview as never as { title: string }).title
                        : "Aviso al webhook"}`,
  ],
  [
    VIEW,
    "R5 547 Aviso al webhook -> ''",
    `                        : "Aviso al webhook"}`,
    `                        : ""}`,
  ],
  [
    VIEW,
    "R5 599 toDraft -> {}",
    `function toDraft(rule: Automation): AutomationDraft {
  return {`,
    `function toDraft(rule: Automation): AutomationDraft {
  return {} as never;
  return {`,
  ],
  [
    VIEW,
    "R6 198 catch de fetchRules -> {}",
    `    } catch {
      if (!mounted.current || live.current !== controller) return;
      setFailed(true);
    }`,
    `    } catch {
    }`,
  ],
  [
    VIEW,
    "R6 200 setFailed(true) -> false",
    `      setFailed(true);
    }
  }, []);`,
    `      setFailed(false);
    }
  }, []);`,
  ],
  [
    VIEW,
    "R6 275 guarda del catch de save -> false",
    `    } catch (error) {
      if (!mounted.current || writeRequest.current !== controller) return;
      if (error instanceof AutomationFieldErrors) {
        invalid.current = controlIdOf(Object.keys(error.fields)[0]);
        setFields(error.fields);
      } else if (error instanceof AutomationConflict) setConflict(true);`,
    `    } catch (error) {
      if (false) return;
      if (error instanceof AutomationFieldErrors) {
        invalid.current = controlIdOf(Object.keys(error.fields)[0]);
        setFields(error.fields);
      } else if (error instanceof AutomationConflict) setConflict(true);`,
  ],
  [
    VIEW,
    "R6 279 instanceof AutomationConflict -> true",
    `      } else if (error instanceof AutomationConflict) setConflict(true);`,
    `      } else if (true) setConflict(true);`,
  ],
  [
    VIEW,
    "R6 280 aviso de guardado -> ''",
    `      else setNotice("No se ha podido guardar. Inténtalo de nuevo.");`,
    `      else setNotice("");`,
  ],
  [
    VIEW,
    "R6 302 guarda del catch de simulate -> false",
    `      if (!mounted.current || writeRequest.current !== controller) return;
      if (error instanceof AutomationFieldErrors) {
        invalid.current = controlIdOf(Object.keys(error.fields)[0]);
        setFields(error.fields);
      } else setNotice("No se ha podido simular. Inténtalo de nuevo.");`,
    `      if (false) return;
      if (error instanceof AutomationFieldErrors) {
        invalid.current = controlIdOf(Object.keys(error.fields)[0]);
        setFields(error.fields);
      } else setNotice("No se ha podido simular. Inténtalo de nuevo.");`,
  ],
  [
    VIEW,
    "R6 303 instanceof en simulate -> false",
    `      if (error instanceof AutomationFieldErrors) {
        invalid.current = controlIdOf(Object.keys(error.fields)[0]);
        setFields(error.fields);
      } else setNotice("No se ha podido simular. Inténtalo de nuevo.");`,
    `      if (false) {
        invalid.current = controlIdOf(Object.keys(error.fields)[0]);
        setFields(error.fields);
      } else setNotice("No se ha podido simular. Inténtalo de nuevo.");`,
  ],
  [
    VIEW,
    "R6 305 setFields en simulate -> ;",
    `        setFields(error.fields);
      } else setNotice("No se ha podido simular. Inténtalo de nuevo.");`,
    `        ;
      } else setNotice("No se ha podido simular. Inténtalo de nuevo.");`,
  ],
  [
    VIEW,
    "R6 306 aviso de simulación -> ''",
    `      } else setNotice("No se ha podido simular. Inténtalo de nuevo.");`,
    `      } else setNotice("");`,
  ],
  [
    VIEW,
    "R6 311 guarda de busyToggle -> false",
    `    if (busyToggle) return;`,
    `    if (false) return;`,
  ],
  [
    VIEW,
    "R6 321 enabled invertido -> sin invertir",
    `        { ...toDraft(rule), enabled: !rule.enabled },`,
    `        { ...toDraft(rule), enabled: rule.enabled },`,
  ],
  [
    VIEW,
    "R6 329 guarda del catch de toggle -> false",
    `      if (!mounted.current || writeRequest.current !== controller) return;
      setNotice("No se ha podido cambiar el estado de la regla.");`,
    `      if (false) return;
      setNotice("No se ha podido cambiar el estado de la regla.");`,
  ],
  [
    VIEW,
    "R6 350 catch de reloadEditing -> {}",
    `    } catch {
      if (!mounted.current || live.current !== controller) return;
      setNotice("No se ha podido cargar la versión actual.");
    }`,
    `    } catch {
    }`,
  ],
  [
    VIEW,
    "R6 352 aviso de versión actual -> ''",
    `      setNotice("No se ha podido cargar la versión actual.");`,
    `      setNotice("");`,
  ],
  [
    VIEW,
    "R6 372 catch de openHistory -> {}",
    `    } catch {
      if (!mounted.current || runsRequest.current !== controller) return;
      setNotice("No se ha podido cargar el historial.");
    }`,
    `    } catch {
    }`,
  ],
  [
    VIEW,
    "R6 373 guarda del catch de openHistory -> false",
    `      if (!mounted.current || runsRequest.current !== controller) return;
      setNotice("No se ha podido cargar el historial.");`,
    `      if (false) return;
      setNotice("No se ha podido cargar el historial.");`,
  ],
  [
    VIEW,
    "R6 374 aviso de historial -> ''",
    `      setNotice("No se ha podido cargar el historial.");`,
    `      setNotice("");`,
  ],
  [
    VIEW,
    "R6 380 tabIndex del encabezado -> +1",
    `      <h1 ref={heading} tabIndex={-1}>`,
    `      <h1 ref={heading} tabIndex={+1}>`,
  ],
  [
    VIEW,
    "R6 384 estado de carga -> true",
    `      {rules === null && !failed && (`,
    `      {true && (`,
  ],
  [
    VIEW,
    "R6 384 estado de carga LogicalOperator",
    `      {rules === null && !failed && (`,
    `      {(rules === null || !failed) && (`,
  ],
  [
    VIEW,
    "R6 395 estado vacío -> true",
    `      {rules !== null && rules.length === 0 && (`,
    `      {true && (`,
  ],
  [
    VIEW,
    "R6 424 clase del interruptor activo -> ''",
    `                  className={rule.enabled ? "is-active" : "is-inactive"}`,
    `                  className={rule.enabled ? "" : "is-inactive"}`,
  ],
  [
    VIEW,
    "R6 424 clase del interruptor inactivo -> ''",
    `                  className={rule.enabled ? "is-active" : "is-inactive"}`,
    `                  className={rule.enabled ? "is-active" : ""}`,
  ],
  [
    VIEW,
    "R6 427 interruptor deshabilitado -> false",
    `                  disabled={busyToggle === rule.id}`,
    `                  disabled={false}`,
  ],
  [
    VIEW,
    "R6 549 aviso de fallo -> true",
    `                    {match.preview.type === "CREATE_TASK" &&
                      match.preview.wouldFail && (`,
    `                    {true && (`,
  ],
  [
    VIEW,
    "R6 549 aviso de fallo LogicalOperator",
    `                    {match.preview.type === "CREATE_TASK" &&
                      match.preview.wouldFail && (`,
    `                    {(match.preview.type === "CREATE_TASK" ||
                      match.preview.wouldFail) && (`,
  ],
  [
    VIEW,
    "R7 158 conflicto inicial -> true",
    `  const [conflict, setConflict] = useState(false);`,
    `  const [conflict, setConflict] = useState(true);`,
  ],
  [
    VIEW,
    "R7 252 setFields({}) en save -> ;",
    `    setFields({});
    setConflict(false);`,
    `    setConflict(false);`,
  ],
  [
    VIEW,
    "R7 253 setConflict(false) en save -> true",
    `    setConflict(false);
    setNotice(null);`,
    `    setConflict(true);
    setNotice(null);`,
  ],
  [
    VIEW,
    "R7 254 setNotice(null) en save -> ;",
    `    setConflict(false);
    setNotice(null);
    try {`,
    `    setConflict(false);
    try {`,
  ],
  [
    VIEW,
    "R7 266 setEditing(null) tras guardar -> ;",
    `      setEditing(null);
      setRules((current) =>`,
    `      setRules((current) =>`,
  ],
  [
    VIEW,
    "R7 292 setFields({}) en simulate -> ;",
    `    setFields({});
    setNotice(null);
    try {
      const result = await simulateAutomation(`,
    `    setNotice(null);
    try {
      const result = await simulateAutomation(`,
  ],
  [
    VIEW,
    "R7 293 setNotice(null) en simulate -> ;",
    `    setNotice(null);
    try {
      const result = await simulateAutomation(`,
    `    try {
      const result = await simulateAutomation(`,
  ],
  [
    VIEW,
    "R7 315 setBusyToggle -> ;",
    `    setBusyToggle(rule.id);
    setNotice(null);`,
    `    setNotice(null);`,
  ],
  [
    VIEW,
    "R7 316 setNotice(null) en toggle -> ;",
    `    setBusyToggle(rule.id);
    setNotice(null);`,
    `    setBusyToggle(rule.id);`,
  ],
  [
    VIEW,
    "R7 326 sustitución en el interruptor -> true",
    `        (current ?? []).map((item) => (item.id === saved.id ? saved : item)),`,
    `        (current ?? []).map((item) => (true ? saved : item)),`,
  ],
  [
    VIEW,
    "R7 340 setConflict(false) en reload -> true",
    `    const id = editing?.rule?.id;
    setConflict(false);`,
    `    const id = editing?.rule?.id;
    setConflict(true);`,
  ],
  [
    VIEW,
    "R7 347 setRules en reload -> ;",
    `      setRules(items);
      const fresh = items.find((rule) => rule.id === id);`,
    `      const fresh = items.find((rule) => rule.id === id);`,
  ],
  [
    VIEW,
    "R7 348 búsqueda de la regla fresca -> true",
    `      const fresh = items.find((rule) => rule.id === id);`,
    `      const fresh = items.find(() => true);`,
  ],
  [
    VIEW,
    "R7 360 vaciado del historial -> false",
    `    if (cursor === null) setHistory({ rule, items: [], nextCursor: null });`,
    `    if (false) setHistory({ rule, items: [], nextCursor: null });`,
  ],
  [
    VIEW,
    "R7 360 lista vacía -> Stryker",
    `    if (cursor === null) setHistory({ rule, items: [], nextCursor: null });`,
    `    if (cursor === null)
      setHistory({ rule, items: ["Stryker was here"] as never, nextCursor: null });`,
  ],
  [
    VIEW,
    "R7 570 hueco del código de error",
    `                {run.errorCode && <span>{run.errorCode}</span>}`,
    `                {run.errorCode || <span>{run.errorCode}</span>}`,
  ],
  [
    VIEW,
    "R7 500 error del título -> ''",
    `            error={fields["action.titleTemplate"]}`,
    `            error={fields[""]}`,
  ],
  [
    VIEW,
    "R8 178 limpieza de desmontaje -> {}",
    `  useLayoutEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      live.current?.abort();
      runsRequest.current?.abort();
      writeRequest.current?.abort();
    };
  }, []);`,
    `  useLayoutEffect(() => {
  }, []);`,
  ],
  [
    VIEW,
    "R8 180 cuerpo de la limpieza -> {}",
    `    return () => {
      mounted.current = false;
      live.current?.abort();
      runsRequest.current?.abort();
      writeRequest.current?.abort();
    };`,
    `    return () => {};`,
  ],
  [
    VIEW,
    "R8 183 aborto del historial -> ;",
    `      runsRequest.current?.abort();
      writeRequest.current?.abort();`,
    `      writeRequest.current?.abort();`,
  ],
  [
    VIEW,
    "R8 184 aborto de la escritura -> ;",
    `      runsRequest.current?.abort();
      writeRequest.current?.abort();`,
    `      runsRequest.current?.abort();`,
  ],
  [
    VIEW,
    "R8 223 limpieza de la lectura inicial -> undefined",
    `      .catch(() => {
        if (mounted.current && live.current === controller) setFailed(true);
      });
    return () => controller.abort();`,
    `      .catch(() => {
        if (mounted.current && live.current === controller) setFailed(true);
      });
    return () => undefined;`,
  ],
  [
    VIEW,
    "R8 233 limpieza de la lectura de proyectos -> undefined",
    `      .catch(() => {});
    return () => controller.abort();`,
    `      .catch(() => {});
    return () => undefined;`,
  ],
  [
    VIEW,
    "R8 207 setFailed(false) al reintentar -> true",
    `    setRules(null);
    setFailed(false);`,
    `    setRules(null);
    setFailed(true);`,
  ],
  [
    VIEW,
    "R8 218 setFailed(false) tras la lectura inicial -> true",
    `        setRules(items);
        setFailed(false);
      })`,
    `        setRules(items);
        setFailed(true);
      })`,
  ],
  [
    VIEW,
    "R8 197 setFailed(false) tras el reintento -> true",
    `      setRules(items);
      setFailed(false);
    } catch {`,
    `      setRules(items);
      setFailed(true);
    } catch {`,
  ],
  [
    VIEW,
    "R8 281 finally de save -> {}",
    `    } finally {
      if (mounted.current && writeRequest.current === controller)
        setSaving(false);
    }`,
    `    } finally {
    }`,
  ],
  [
    VIEW,
    "R8 283 setSaving(false) -> true",
    `        setSaving(false);`,
    `        setSaving(true);`,
  ],
  [
    VIEW,
    "490 opciones de proyecto -> undefined",
    `            {projects.map((project) => (
              <option key={project.id} value={project.id}>
                {project.name}
              </option>
            ))}`,
    `            {projects.map(() => undefined)}`,
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
