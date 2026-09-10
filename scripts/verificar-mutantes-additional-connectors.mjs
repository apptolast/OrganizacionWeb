// Verifica a mano que los oráculos nuevos discriminan: aplica cada mutante superviviente al
// fichero de producción real, ejecuta las suites de la feature 29 y anota qué pruebas caen.
// No modifica nada de forma permanente: restaura el fichero tras cada pasada.
//
// Uso: node scripts/verificar-mutantes-additional-connectors.mjs [racimo]
import { execFileSync } from "node:child_process";
import { readFileSync, writeFileSync } from "node:fs";

const SOURCES = {
  client: "frontend/src/gitlab-connector-client.ts",
  screen: "frontend/src/gitlab-connector.tsx",
  catalog: "frontend/src/connectors-catalog.tsx",
  catalogClient: "frontend/src/connectors-catalog-client.ts",
  feedError:
    "backend/src/main/java/com/apptolast/organization/domain/FeedError.java",
};

const originals = Object.fromEntries(
  Object.entries(SOURCES).map(([key, path]) => [key, readFileSync(path, "utf8")]),
);

/** [racimo, nombre, fichero, texto exacto a buscar, texto de reemplazo] */
const MUTANTS = [
  // -------------------------------------------------- racimo 1: los 17 sin cobertura
  [
    1,
    "client 112 CE decodeFailure -> true",
    "client",
    `    !exact(value, ERROR_FIELDS) ||\n    !nonEmpty(value.code) ||\n    !instant(value.at)\n  )`,
    `    true\n  )`,
  ],
  [
    1,
    "client 112 CE decodeFailure -> false",
    "client",
    `    !exact(value, ERROR_FIELDS) ||\n    !nonEmpty(value.code) ||\n    !instant(value.at)\n  )`,
    `    false\n  )`,
  ],
  [
    1,
    "client 112 LogicalOperator (A||B) && C",
    "client",
    `    !exact(value, ERROR_FIELDS) ||\n    !nonEmpty(value.code) ||\n    !instant(value.at)\n  )`,
    `    (!exact(value, ERROR_FIELDS) || !nonEmpty(value.code)) &&\n    !instant(value.at)\n  )`,
  ],
  [
    1,
    "client 112 CE (A||B) -> false",
    "client",
    `    !exact(value, ERROR_FIELDS) ||\n    !nonEmpty(value.code) ||\n    !instant(value.at)\n  )`,
    `    false ||\n    !instant(value.at)\n  )`,
  ],
  [
    1,
    "client 112 LogicalOperator A && B",
    "client",
    `    !exact(value, ERROR_FIELDS) ||\n    !nonEmpty(value.code) ||\n    !instant(value.at)\n  )`,
    `    (!exact(value, ERROR_FIELDS) && !nonEmpty(value.code)) ||\n    !instant(value.at)\n  )`,
  ],
  [
    1,
    "client 112 BooleanLiteral !exact -> exact",
    "client",
    `    !exact(value, ERROR_FIELDS) ||\n    !nonEmpty(value.code) ||`,
    `    exact(value, ERROR_FIELDS) ||\n    !nonEmpty(value.code) ||`,
  ],
  [
    1,
    "client 113 BooleanLiteral !nonEmpty -> nonEmpty",
    "client",
    `    !nonEmpty(value.code) ||\n    !instant(value.at)\n  )`,
    `    nonEmpty(value.code) ||\n    !instant(value.at)\n  )`,
  ],
  [
    1,
    "client 114 BooleanLiteral !instant -> instant",
    "client",
    `    !nonEmpty(value.code) ||\n    !instant(value.at)\n  )`,
    `    !nonEmpty(value.code) ||\n    instant(value.at)\n  )`,
  ],
  [
    1,
    "client 116 CallExpression throw -> ;",
    "client",
    `    !instant(value.at)\n  )\n    throw new Error(INCOMPATIBLE);`,
    `    !instant(value.at)\n  )\n    ;`,
  ],
  [
    1,
    "client 167 CE errorCode !== null -> true",
    "client",
    `    (value.status === "running" && value.errorCode !== null) ||`,
    `    (value.status === "running" && true) ||`,
  ],
  [
    1,
    "client 167 EqualityOperator errorCode === null",
    "client",
    `    (value.status === "running" && value.errorCode !== null) ||`,
    `    (value.status === "running" && value.errorCode === null) ||`,
  ],
  [
    1,
    "screen 368 BooleanLiteral Cancelar -> setConfirming(true)",
    "screen",
    `<button type="button" onClick={() => setConfirming(false)}>`,
    `<button type="button" onClick={() => setConfirming(true)}>`,
  ],
  [
    1,
    "screen 368 ArrowFunction Cancelar -> () => undefined",
    "screen",
    `<button type="button" onClick={() => setConfirming(false)}>`,
    `<button type="button" onClick={() => undefined}>`,
  ],
  [
    1,
    "screen 409 BooleanLiteral aria-invalid token -> false",
    "screen",
    `aria-invalid={connectError?.fields.token ? true : undefined}`,
    `aria-invalid={connectError?.fields.token ? false : undefined}`,
  ],
  [
    1,
    "catalogo 67 StringLiteral texto de reserva -> ''",
    "catalog",
    `return ERROR_TEXT[code] ?? "Hay un problema con esta integración";`,
    `return ERROR_TEXT[code] ?? "";`,
  ],
  [
    1,
    "catalogo 54 StringLiteral FEED_UNREACHABLE -> ''",
    "catalog",
    `  FEED_UNREACHABLE: "El calendario no responde",`,
    `  FEED_UNREACHABLE: "",`,
  ],
  [
    1,
    "FeedError: una constante nueva sin traduccion",
    "feedError",
    `  SECRET_UNREADABLE\n}`,
    `  SECRET_UNREADABLE,\n  FEED_NUEVO\n}`,
  ],
  // ------------------------------------ racimo 2: la tabla de rechazo de decodeConnection
  [
    2,
    "client 106 CE value.length > 0 -> true",
    "client",
    `return typeof value === "string" && value.length > 0;`,
    `return typeof value === "string" && true;`,
  ],
  [
    2,
    "client 106 EqualityOperator length >= 0",
    "client",
    `return typeof value === "string" && value.length > 0;`,
    `return typeof value === "string" && value.length >= 0;`,
  ],
  [
    2,
    "client 88 CE typeof && isInteger -> true",
    "client",
    `  return typeof value === "number" && Number.isInteger(value) && value >= 0`,
    `  return true && value >= 0`,
  ],
  [
    2,
    "client 88 LogicalOperator typeof || isInteger",
    "client",
    `  return typeof value === "number" && Number.isInteger(value) && value >= 0`,
    `  return (typeof value === "number" || Number.isInteger(value)) && value >= 0`,
  ],
  [
    2,
    "client 94 CE isCount -> true",
    "client",
    `  return counter(value) !== null;`,
    `  return true;`,
  ],
  [
    2,
    "client 99 CE uuid(value) -> true",
    "client",
    `    uuid(value) &&`,
    `    true &&`,
  ],
  [
    2,
    "client 100 CE minusculas -> true",
    "client",
    `    value === (value as string).toLowerCase() &&`,
    `    true &&`,
  ],
  [
    2,
    "client 110 CE value === null -> true",
    "client",
    `  if (value === null) return null;`,
    `  if (true) return null;`,
  ],
  [
    2,
    "client 124 StringLiteral 'error' -> ''",
    "client",
    `    !["connected", "error", "not_connected"].includes(value.status as string)`,
    `    !["connected", "", "not_connected"].includes(value.status as string)`,
  ],
  [
    2,
    "client 142 CE (null || instant) -> true",
    "client",
    `        !(value.lastActivityAt === null || instant(value.lastActivityAt))`,
    `        !(true)`,
  ],
  [
    2,
    "client 142 CE lastActivityAt === null -> false",
    "client",
    `        !(value.lastActivityAt === null || instant(value.lastActivityAt))`,
    `        !(false || instant(value.lastActivityAt))`,
  ],
  [
    2,
    "client 142 EqualityOperator lastActivityAt !== null",
    "client",
    `        !(value.lastActivityAt === null || instant(value.lastActivityAt))`,
    `        !(value.lastActivityAt !== null || instant(value.lastActivityAt))`,
  ],
  ...absentChainMutants(),

  // ------------------------------------------- racimo 3: la tabla de rechazo del recibo
  [
    3,
    "client 153 LogicalOperator !exact && !identifier",
    "client",
    `    !exact(value, RECEIPT_KEYS) ||\n    !identifier(value.id) ||`,
    `    (!exact(value, RECEIPT_KEYS) && !identifier(value.id)) ||`,
  ],
  [
    3,
    "client 153 CE !exact -> false",
    "client",
    `    !exact(value, RECEIPT_KEYS) ||\n    !identifier(value.id) ||`,
    `    false ||\n    !identifier(value.id) ||`,
  ],
  [
    3,
    "client 158 StringLiteral 'running' -> ''",
    "client",
    `    !["running", "completed", "failed"].includes(value.status as string) ||`,
    `    !["", "completed", "failed"].includes(value.status as string) ||`,
  ],
  [
    3,
    "client 158 StringLiteral 'failed' -> ''",
    "client",
    `    !["running", "completed", "failed"].includes(value.status as string) ||`,
    `    !["running", "completed", ""].includes(value.status as string) ||`,
  ],
  [
    3,
    "client 162 CE truncated no booleano -> false",
    "client",
    `    typeof value.truncated !== "boolean" ||`,
    `    false ||`,
  ],
  [
    3,
    "client 164 CE (errorCode null || nonEmpty) -> true",
    "client",
    `    !(value.errorCode === null || nonEmpty(value.errorCode)) ||`,
    `    !(true) ||`,
  ],
  [
    3,
    "client 166 CE status === 'running' -> false",
    "client",
    `    (value.status === "running") !== (value.finishedAt === null) ||`,
    `    (false) !== (value.finishedAt === null) ||`,
  ],
  [
    3,
    "client 166 StringLiteral 'running' -> ''",
    "client",
    `    (value.status === "running") !== (value.finishedAt === null) ||`,
    `    (value.status === "") !== (value.finishedAt === null) ||`,
  ],
  [
    3,
    "client 167 CE clausula running+errorCode -> false",
    "client",
    `    (value.status === "running" && value.errorCode !== null) ||`,
    `    (false) ||`,
  ],
  [
    3,
    "client 167 CE status === 'running' -> true",
    "client",
    `    (value.status === "running" && value.errorCode !== null) ||`,
    `    (true && value.errorCode !== null) ||`,
  ],
  [
    3,
    "client 167 LogicalOperator running || errorCode",
    "client",
    `    (value.status === "running" && value.errorCode !== null) ||`,
    `    (value.status === "running" || value.errorCode !== null) ||`,
  ],
  [
    3,
    "client 167 EqualityOperator status !== 'running'",
    "client",
    `    (value.status === "running" && value.errorCode !== null) ||`,
    `    (value.status !== "running" && value.errorCode !== null) ||`,
  ],
  [
    3,
    "client 167 StringLiteral 'running' -> ''",
    "client",
    `    (value.status === "running" && value.errorCode !== null) ||`,
    `    (value.status === "" && value.errorCode !== null) ||`,
  ],
  [
    3,
    "client 168 CE finishedAt !== null -> true",
    "client",
    `    (value.finishedAt !== null &&`,
    `    (true &&`,
  ],
  [
    3,
    "client 168 CE finishedAt !== null -> false",
    "client",
    `    (value.finishedAt !== null &&`,
    `    (false &&`,
  ],
  [
    3,
    "client 168 EqualityOperator finishedAt === null",
    "client",
    `    (value.finishedAt !== null &&`,
    `    (value.finishedAt === null &&`,
  ],
  [
    3,
    "client 169 LogicalOperator !instant && orden",
    "client",
    `      (!instant(value.finishedAt) ||\n        microseconds(value.finishedAt)! < microseconds(value.startedAt)!))`,
    `      (!instant(value.finishedAt) &&\n        microseconds(value.finishedAt)! < microseconds(value.startedAt)!))`,
  ],
  [
    3,
    "client 170 CE orden -> false",
    "client",
    `        microseconds(value.finishedAt)! < microseconds(value.startedAt)!))`,
    `        false))`,
  ],
  [
    3,
    "client 170 EqualityOperator < -> <=",
    "client",
    `        microseconds(value.finishedAt)! < microseconds(value.startedAt)!))`,
    `        microseconds(value.finishedAt)! <= microseconds(value.startedAt)!))`,
  ],
  [
    3,
    "client 252 CE status !== 200 -> false",
    "client",
    `  if (response.status !== 200) return failure(response);\n  const body: unknown = await response.json();\n  signal.throwIfAborted();\n  return decodeReceipt(body);`,
    `  if (false) return failure(response);\n  const body: unknown = await response.json();\n  signal.throwIfAborted();\n  return decodeReceipt(body);`,
  ],

  // -------------- racimo 4: abortos, cabeceras, el error tipado y el mapa de campos
  ...abortMutants(),
  [
    4,
    "client 188 ObjectLiteral { signal } -> {} (leer conexion)",
    "client",
    `const response = await apiRequest(CONNECTION_URL, { signal });`,
    `const response = await apiRequest(CONNECTION_URL, {});`,
  ],
  [
    4,
    "client 250 ObjectLiteral { signal } -> {} (leer recibo)",
    "client",
    `const response = await apiRequest(\`\${IMPORTS_URL}/\${id}\`, { signal });`,
    `const response = await apiRequest(\`\${IMPORTS_URL}/\${id}\`, {});`,
  ],
  [
    4,
    "client 204 ObjectLiteral headers -> {} (conectar)",
    "client",
    `    method: "PUT",\n    signal,\n    headers: { "Content-Type": "application/json" },`,
    `    method: "PUT",\n    signal,\n    headers: {},`,
  ],
  [
    4,
    "client 204 StringLiteral Content-Type -> '' (conectar)",
    "client",
    `    method: "PUT",\n    signal,\n    headers: { "Content-Type": "application/json" },`,
    `    method: "PUT",\n    signal,\n    headers: { "": "application/json" },`,
  ],
  [
    4,
    "client 235 ObjectLiteral headers -> {} (importar)",
    "client",
    `    method: "POST",\n    signal,\n    headers: { "Content-Type": "application/json" },`,
    `    method: "POST",\n    signal,\n    headers: {},`,
  ],
  [
    4,
    "client 235 StringLiteral Content-Type -> '' (importar)",
    "client",
    `    method: "POST",\n    signal,\n    headers: { "Content-Type": "application/json" },`,
    `    method: "POST",\n    signal,\n    headers: { "": "application/json" },`,
  ],
  [
    4,
    "client 58 CE super(code) -> true",
    "client",
    `    super(typeof body.code === "string" ? body.code : "CONNECTOR_ERROR");`,
    `    super(true ? body.code : "CONNECTOR_ERROR");`,
  ],
  [
    4,
    "client 58 CE super(code) -> false",
    "client",
    `    super(typeof body.code === "string" ? body.code : "CONNECTOR_ERROR");`,
    `    super(false ? body.code : "CONNECTOR_ERROR");`,
  ],
  [
    4,
    "client 58 EqualityOperator typeof !== string",
    "client",
    `    super(typeof body.code === "string" ? body.code : "CONNECTOR_ERROR");`,
    `    super(typeof body.code !== "string" ? body.code : "CONNECTOR_ERROR");`,
  ],
  [
    4,
    "client 58 StringLiteral 'string' -> ''",
    "client",
    `    super(typeof body.code === "string" ? body.code : "CONNECTOR_ERROR");`,
    `    super(typeof body.code === "" ? body.code : "CONNECTOR_ERROR");`,
  ],
  [
    4,
    "client 58 StringLiteral CONNECTOR_ERROR -> ''",
    "client",
    `    super(typeof body.code === "string" ? body.code : "CONNECTOR_ERROR");`,
    `    super(typeof body.code === "string" ? body.code : "");`,
  ],
  [
    4,
    "client 59 StringLiteral this.name -> ''",
    "client",
    `    this.name = "GitlabConnectorError";`,
    `    this.name = "";`,
  ],
  [
    4,
    "client 75 CE filtro entero -> true",
    "client",
    `      entry &&\n      typeof entry === "object" &&\n      nonEmpty((entry as Record<string, unknown>).field) &&\n      nonEmpty((entry as Record<string, unknown>).code)`,
    `      true`,
  ],
  [
    4,
    "client 75 LogicalOperator ultimo && -> ||",
    "client",
    `      entry &&\n      typeof entry === "object" &&\n      nonEmpty((entry as Record<string, unknown>).field) &&\n      nonEmpty((entry as Record<string, unknown>).code)`,
    `      (entry &&\n        typeof entry === "object" &&\n        nonEmpty((entry as Record<string, unknown>).field)) ||\n      nonEmpty((entry as Record<string, unknown>).code)`,
  ],
  [
    4,
    "client 75 CE (entry && typeof && field) -> true",
    "client",
    `      entry &&\n      typeof entry === "object" &&\n      nonEmpty((entry as Record<string, unknown>).field) &&\n      nonEmpty((entry as Record<string, unknown>).code)`,
    `      true && nonEmpty((entry as Record<string, unknown>).code)`,
  ],
  [
    4,
    "client 75 CE (entry && typeof) -> true",
    "client",
    `      entry &&\n      typeof entry === "object" &&\n      nonEmpty((entry as Record<string, unknown>).field) &&\n      nonEmpty((entry as Record<string, unknown>).code)`,
    `      true &&\n      nonEmpty((entry as Record<string, unknown>).field) &&\n      nonEmpty((entry as Record<string, unknown>).code)`,
  ],
  [
    4,
    "client 75 LogicalOperator segundo && -> ||",
    "client",
    `      entry &&\n      typeof entry === "object" &&\n      nonEmpty((entry as Record<string, unknown>).field) &&\n      nonEmpty((entry as Record<string, unknown>).code)`,
    `      ((entry && typeof entry === "object") ||\n        nonEmpty((entry as Record<string, unknown>).field)) &&\n      nonEmpty((entry as Record<string, unknown>).code)`,
  ],
  [
    4,
    "client 75 LogicalOperator primer && -> ||",
    "client",
    `      entry &&\n      typeof entry === "object" &&\n      nonEmpty((entry as Record<string, unknown>).field) &&\n      nonEmpty((entry as Record<string, unknown>).code)`,
    `      (entry || typeof entry === "object") &&\n      nonEmpty((entry as Record<string, unknown>).field) &&\n      nonEmpty((entry as Record<string, unknown>).code)`,
  ],
];

/**
 * Las tres paradas de `throwIfAborted()` de cada una de las cinco llamadas del cliente. Se
 * generan a partir del texto de cada función para no escribir catorce anclas a mano.
 */
function abortMutants() {
  const functions = [
    ["readGitlabConnection", 187, `CONNECTION_URL, { signal }`],
    ["connectGitlab", 200, `CONNECTION_URL, {\n    method: "PUT"`],
    ["disconnectGitlab", 218, `CONNECTION_URL, {\n    method: "DELETE"`],
    ["startGitlabImport", 231, `IMPORTS_URL, {\n    method: "POST"`],
    ["readGitlabImport", 249, `\\\`\${IMPORTS_URL}/\${id}\\\`, { signal }`],
  ];
  const source = originals.client;
  const mutants = [];
  for (const [name] of functions) {
    const start = source.indexOf(`export async function ${name}(`);
    const body = source.slice(start, source.indexOf("\n}\n", start) + 3);
    const stops = body.split("signal.throwIfAborted();").length - 1;
    for (let stop = 1; stop <= stops; stop++) {
      const pieces = body.split("signal.throwIfAborted();");
      const mutated = pieces.reduce(
        (text, piece, index) =>
          index === 0
            ? piece
            : `${text}${index === stop ? ";" : "signal.throwIfAborted();"}${piece}`,
        "",
      );
      mutants.push([
        4,
        `client ${name} throwIfAborted parada ${stop} de ${stops} -> ;`,
        "client",
        body,
        mutated,
      ]);
    }
  }
  return mutants;
}

/**
 * Los 19 mutantes de la cadena `absent` de decodeConnection (líneas 130-136), generados en vez
 * de escritos a mano: siete `||` encadenados dan siete nodos con su ConditionalExpression, seis
 * operandos más y seis LogicalOperator. Escribirlos a mano invita a olvidar uno.
 */
function absentChainMutants() {
  const fields = [
    "apiBase",
    "projectPath",
    "projectId",
    "tokenHint",
    "lastActivityAt",
    "lastError",
    "version",
  ];
  const operands = fields.map((field) => `value.${field} !== null`);
  const search = `      ? ${operands.join(" ||\n        ")}\n      : !nonEmpty(value.apiBase) ||`;
  const wrap = (chain) => `      ? ${chain}\n      : !nonEmpty(value.apiBase) ||`;
  const prefix = (upTo) => operands.slice(0, upTo).join(" || ");
  const rest = (from) =>
    operands.slice(from).map((each) => ` || ${each}`).join("");
  const mutants = [];
  for (let node = 1; node <= 7; node++)
    mutants.push([
      2,
      `client 130 CE nodo 1..${node} (${fields[node - 1]}) -> false`,
      "client",
      search,
      wrap(`false${rest(node)}`),
    ]);
  for (let operand = 2; operand <= 7; operand++) {
    const copy = [...operands];
    copy[operand - 1] = "false";
    mutants.push([
      2,
      `client 130 CE operando ${fields[operand - 1]} -> false`,
      "client",
      search,
      wrap(copy.join(" || ")),
    ]);
  }
  for (let node = 2; node <= 7; node++)
    mutants.push([
      2,
      `client 130 LogicalOperator nodo ${node} (${fields[node - 1]}) -> &&`,
      "client",
      search,
      wrap(`(${prefix(node - 1)}) && ${operands[node - 1]}${rest(node)}`),
    ]);
  return mutants;
}

function failedTests(output) {
  return [...output.matchAll(/^\s+×\s+(.+?)\s+\d+ms$/gm)].map(
    (match) => match[1],
  );
}

const only = process.argv[2] ? Number(process.argv[2]) : null;
const results = [];
for (const [racimo, name, source, search, replacement] of MUTANTS) {
  if (only !== null && racimo !== only) continue;
  const original = originals[source];
  const occurrences = original.split(search).length - 1;
  if (occurrences !== 1) {
    results.push({
      racimo,
      name,
      verdict: `ANCLA AMBIGUA (${occurrences} apariciones)`,
      tests: [],
    });
    console.log(`ANCLA     ${name} (${occurrences} apariciones)`);
    continue;
  }
  writeFileSync(SOURCES[source], original.replace(search, replacement));
  let output = "";
  try {
    output = execFileSync(
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "vitest",
        "run",
        "src/gitlab-connector",
        "src/connectors-catalog",
      ],
      { encoding: "utf8", shell: true },
    );
  } catch (error) {
    output = `${error.stdout ?? ""}${error.stderr ?? ""}`;
  }
  writeFileSync(SOURCES[source], original);
  const tests = failedTests(output);
  results.push({
    racimo,
    name,
    verdict: tests.length > 0 ? "MUERE" : "SOBREVIVE",
    tests,
  });
  console.log(
    `${tests.length > 0 ? "MUERE   " : "SOBREVIVE"}  ${name}  ->  ${tests.slice(0, 3).join(" | ") || "(nadie falla)"}`,
  );
}

for (const [key, path] of Object.entries(SOURCES))
  writeFileSync(path, originals[key]);
const dead = results.filter((result) => result.verdict === "MUERE").length;
console.log(`\nTOTAL: ${dead} mueren de ${results.length}`);
const file = `progress/verificacion_mutantes_additional_connectors${only ?? ""}.json`;
writeFileSync(file, `${JSON.stringify(results, null, 2)}\n`);
