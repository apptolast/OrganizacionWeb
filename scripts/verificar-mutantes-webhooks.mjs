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
