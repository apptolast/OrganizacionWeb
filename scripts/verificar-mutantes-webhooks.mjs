// Verifica a mano que los oráculos nuevos discriminan: aplica cada mutante superviviente
// al fichero de producción real, ejecuta la suite del componente y anota si cae en rojo.
// No modifica nada de forma permanente: restaura el fichero tras cada pasada.
//
// Uso: node scripts/verificar-mutantes-webhooks.mjs [filtro]
import { execFileSync } from "node:child_process";
import { readFileSync, writeFileSync } from "node:fs";

const CLIENT = "frontend/src/webhooks-client.ts";
const VIEW = "frontend/src/webhooks.tsx";

/** [nombre, fichero, texto exacto a buscar, texto de reemplazo, suite] */
const MUTANTS = [
  // ── Racimo 1: el catálogo de eventTypes en decodeEndpoint ──────────────
  [
    "79 ConditionalExpression orden -> true",
    CLIENT,
    `          webhookEventTypes.indexOf(types[index - 1] as never) >=
            webhookEventTypes.indexOf(type as never)),`,
    `          true),`,
    "client",
  ],
  [
    "80 EqualityOperator >= -> >",
    CLIENT,
    `          webhookEventTypes.indexOf(types[index - 1] as never) >=
            webhookEventTypes.indexOf(type as never)),`,
    `          webhookEventTypes.indexOf(types[index - 1] as never) >
            webhookEventTypes.indexOf(type as never)),`,
    "client",
  ],
  [
    "81 EqualityOperator >= -> <",
    CLIENT,
    `          webhookEventTypes.indexOf(types[index - 1] as never) >=
            webhookEventTypes.indexOf(type as never)),`,
    `          webhookEventTypes.indexOf(types[index - 1] as never) <
            webhookEventTypes.indexOf(type as never)),`,
    "client",
  ],
  [
    "74 ConditionalExpression index>0 && ... -> false",
    CLIENT,
    `        (index > 0 &&`,
    `        (false &&`,
    "client",
  ],
  [
    "75 LogicalOperator index>0 && -> ||",
    CLIENT,
    `        (index > 0 &&
          webhookEventTypes.indexOf(types[index - 1] as never) >=`,
    `        (index > 0 ||
          webhookEventTypes.indexOf(types[index - 1] as never) >=`,
    "client",
  ],
  [
    "78 EqualityOperator index > 0 -> index <= 0",
    CLIENT,
    `        (index > 0 &&`,
    `        (index <= 0 &&`,
    "client",
  ],
  [
    "68 MethodExpression some -> every",
    CLIENT,
    `    value.eventTypes.some(`,
    `    value.eventTypes.every(`,
    "client",
  ],
  [
    "69 ArrowFunction predicado -> undefined",
    CLIENT,
    `      (type, index, types) =>
        !(webhookEventTypes as readonly string[]).includes(type) ||
        (index > 0 &&
          webhookEventTypes.indexOf(types[index - 1] as never) >=
            webhookEventTypes.indexOf(type as never)),`,
    `      () => undefined,`,
    "client",
  ],
  [
    "71 ConditionalExpression !includes(type) -> false",
    CLIENT,
    `        !(webhookEventTypes as readonly string[]).includes(type) ||`,
    `        false ||`,
    "client",
  ],
  [
    "72 LogicalOperator !includes || -> &&",
    CLIENT,
    `        !(webhookEventTypes as readonly string[]).includes(type) ||
        (index > 0 &&`,
    `        !(webhookEventTypes as readonly string[]).includes(type) &&
        (index > 0 &&`,
    "client",
  ],
  [
    "66 ConditionalExpression eventTypes.length === 0 -> false",
    CLIENT,
    `    value.eventTypes.length === 0 ||`,
    `    false ||`,
    "client",
  ],
  [
    "65 ConditionalExpression !Array.isArray -> false",
    CLIENT,
    `    !Array.isArray(value.eventTypes) ||`,
    `    false ||`,
    "client",
  ],
  // ── Racimo 2: el resto de guardas de decodeEndpoint ────────────────────
  [
    "3 ConditionalExpression uuid(value) -> true",
    CLIENT,
    `  return uuid(value) && (value as string).length === 36;`,
    `  return true && (value as string).length === 36;`,
    "client",
  ],
  [
    "5 LogicalOperator identifier && -> ||",
    CLIENT,
    `  return uuid(value) && (value as string).length === 36;`,
    `  return uuid(value) || (value as string).length === 36;`,
    "client",
  ],
  [
    "52 ConditionalExpression typeof url !== string -> false",
    CLIENT,
    `    typeof value.url !== "string" ||`,
    `    false ||`,
    "client",
  ],
  [
    "57 StringLiteral https:// -> ''",
    CLIENT,
    `    !value.url.startsWith("https://") ||`,
    `    !value.url.startsWith("") ||`,
    "client",
  ],
  [
    "58 ConditionalExpression typeof description !== string -> false",
    CLIENT,
    `    typeof value.description !== "string" ||`,
    `    false ||`,
    "client",
  ],
  [
    "61 ConditionalExpression description > 80 -> false",
    CLIENT,
    `    [...value.description].length > 80 ||`,
    `    false ||`,
    "client",
  ],
  [
    "62 EqualityOperator > 80 -> >= 80",
    CLIENT,
    `    [...value.description].length > 80 ||`,
    `    [...value.description].length >= 80 ||`,
    "client",
  ],
  [
    "64 ArrayDeclaration [...description] -> []",
    CLIENT,
    `    [...value.description].length > 80 ||`,
    `    [].length > 80 ||`,
    "client",
  ],
  [
    "98 LogicalOperator invariante disabled || -> &&",
    CLIENT,
    `        )) ||
    disabled !== (value.disabledAt !== null && instant(value.disabledAt))`,
    `        )) &&
    disabled !== (value.disabledAt !== null && instant(value.disabledAt))`,
    "client",
  ],
  [
    "99 ConditionalExpression mitad de la razon -> false",
    CLIENT,
    `    disabled !==
      (value.disabledReason !== null &&
        (disabledReasons as readonly string[]).includes(
          value.disabledReason as string,
        )) ||`,
    `    false ||`,
    "client",
  ],
  [
    "103 LogicalOperator razon && -> ||",
    CLIENT,
    `      (value.disabledReason !== null &&
        (disabledReasons as readonly string[]).includes(`,
    `      (value.disabledReason !== null ||
        (disabledReasons as readonly string[]).includes(`,
    "client",
  ],
  [
    "106 ConditionalExpression mitad del instante -> false",
    CLIENT,
    `    disabled !== (value.disabledAt !== null && instant(value.disabledAt))`,
    `    false`,
    "client",
  ],
  [
    "110 LogicalOperator instante && -> ||",
    CLIENT,
    `    disabled !== (value.disabledAt !== null && instant(value.disabledAt))`,
    `    disabled !== (value.disabledAt !== null || instant(value.disabledAt))`,
    "client",
  ],
  [
    "0 ArrowFunction incompatible -> undefined",
    CLIENT,
    `const incompatible = () => new Error("Confirmación incompatible");`,
    `const incompatible = (): Error => undefined as unknown as Error;`,
    "client",
  ],
  // ── Racimo 3: decodeDelivery, whole() y la deduplicación ───────────────
  [
    "9 ConditionalExpression whole() entero -> true",
    CLIENT,
    `    typeof value === "number" &&
    Number.isInteger(value) &&
    value >= 0 &&
    value <= max`,
    `    true`,
    "client",
  ],
  [
    "11 LogicalOperator whole() ... && <= max -> ||",
    CLIENT,
    `    value >= 0 &&
    value <= max`,
    `    value >= 0 ||
    value <= max`,
    "client",
  ],
  [
    "13 LogicalOperator whole() ... && >= 0 -> ||",
    CLIENT,
    `    Number.isInteger(value) &&
    value >= 0 &&`,
    `    Number.isInteger(value) ||
    value >= 0 &&`,
    "client",
  ],
  [
    "15 LogicalOperator whole() typeof && isInteger -> ||",
    CLIENT,
    `    typeof value === "number" &&
    Number.isInteger(value) &&`,
    `    (typeof value === "number" ||
    Number.isInteger(value)) &&`,
    "client",
  ],
  [
    "19 ConditionalExpression whole() value >= 0 -> true",
    CLIENT,
    `    value >= 0 &&
    value <= max`,
    `    true &&
    value <= max`,
    "client",
  ],
  [
    "22 ConditionalExpression whole() value <= max -> true",
    CLIENT,
    `    value >= 0 &&
    value <= max`,
    `    value >= 0 &&
    true`,
    "client",
  ],
  [
    "143 ConditionalExpression typeof eventType !== string -> false",
    CLIENT,
    `    typeof value.eventType !== "string" ||`,
    `    false ||`,
    "client",
  ],
  [
    "150 ConditionalExpression httpStatus null|whole -> true",
    CLIENT,
    `    !(value.httpStatus === null || whole(value.httpStatus, 599)) ||`,
    `    !true ||`,
    "client",
  ],
  [
    "156 ConditionalExpression latencyMs null|whole -> true",
    CLIENT,
    `      value.latencyMs === null ||
      whole(value.latencyMs, Number.MAX_SAFE_INTEGER)`,
    `      true`,
    "client",
  ],
  [
    "162 ConditionalExpression errorClass null|catalogo -> true",
    CLIENT,
    `      value.errorClass === null ||
      (errorClasses as readonly string[]).includes(value.errorClass as string)`,
    `      true`,
    "client",
  ],
  [
    "168 ConditionalExpression nextAttemptAt null|instant -> true",
    CLIENT,
    `    !(value.nextAttemptAt === null || instant(value.nextAttemptAt)) ||`,
    `    !true ||`,
    "client",
  ],
  [
    "176 ConditionalExpression invariante de pendiente -> false",
    CLIENT,
    `  if ((value.status === "pending") !== (value.nextAttemptAt !== null))`,
    `  if (false)`,
    "client",
  ],
  [
    "193 ConditionalExpression deduplicacion -> false",
    CLIENT,
    `    new Set(items.map((item) => (item as { id: string }).id)).size !==
    items.length
  )`,
    `    false
  )`,
    "client",
  ],
];

const suites = {
  client: "src/webhooks-client.test.ts",
  view: "src/webhooks.test.tsx",
  both: "src/webhooks",
};

const filtro = process.argv[2] ?? "";
let rojos = 0;
let verdes = 0;

for (const [nombre, fichero, buscar, reemplazo, suite] of MUTANTS) {
  if (filtro && !nombre.includes(filtro)) continue;
  const original = readFileSync(fichero, "utf8");
  if (!original.includes(buscar)) {
    console.log(`?? ${nombre}: el texto a mutar no aparece en ${fichero}`);
    continue;
  }
  writeFileSync(fichero, original.replace(buscar, reemplazo));
  let salida = "";
  let cayo = false;
  try {
    salida = execFileSync(
      "pnpm",
      ["--dir", "frontend", "exec", "vitest", "run", suites[suite]],
      { encoding: "utf8", stdio: "pipe", shell: true },
    );
  } catch (error) {
    cayo = true;
    salida = `${error.stdout ?? ""}${error.stderr ?? ""}`;
  } finally {
    writeFileSync(fichero, original);
  }
  const resumen = /Tests\s+(\d+) failed \| (\d+) passed/.exec(salida);
  const primera = /(?:FAIL|×|✗)\s+(src\/[^\n]+)/.exec(salida);
  if (cayo) {
    rojos++;
    const cuantos = resumen ? `${resumen[1]} rojas` : "suite rota";
    console.log(`ROJO ${nombre}\n     ${cuantos} · ${primera?.[1] ?? ""}`);
  } else {
    verdes++;
    console.log(`VIVO ${nombre}`);
  }
}

console.log(`\nMuertos: ${rojos} · Vivos: ${verdes}`);
