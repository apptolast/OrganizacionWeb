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
];

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
