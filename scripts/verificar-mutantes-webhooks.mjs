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
  // ── Racimo 4: identidad, forma de la petición y contrato de respuesta ──
  [
    "199 ConditionalExpression !identifier(id) -> false",
    CLIENT,
    `  if (!identifier(id)) throw new Error("Identidad incompatible");`,
    `  if (false) throw new Error("Identidad incompatible");`,
    "client",
  ],
  [
    "201 StringLiteral Identidad incompatible (webhookUrl) -> ''",
    CLIENT,
    `  if (!identifier(id)) throw new Error("Identidad incompatible");`,
    `  if (!identifier(id)) throw new Error("");`,
    "client",
  ],
  [
    "282 ConditionalExpression !identifier(deliveryId) -> false",
    CLIENT,
    `  if (!identifier(deliveryId)) throw new Error("Identidad incompatible");`,
    `  if (false) throw new Error("Identidad incompatible");`,
    "client",
  ],
  [
    "284 StringLiteral Identidad incompatible (redeliver) -> ''",
    CLIENT,
    `  if (!identifier(deliveryId)) throw new Error("Identidad incompatible");`,
    `  if (!identifier(deliveryId)) throw new Error("");`,
    "client",
  ],
  [
    "219 ObjectLiteral headers de createWebhook -> {}",
    CLIENT,
    `    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),`,
    `    headers: {},
    body: JSON.stringify(input),`,
    "client",
  ],
  [
    "220 StringLiteral application/json (createWebhook) -> ''",
    CLIENT,
    `    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),`,
    `    headers: { "Content-Type": "" },
    body: JSON.stringify(input),`,
    "client",
  ],
  [
    "243 ObjectLiteral headers de setWebhookStatus -> {}",
    CLIENT,
    `    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),`,
    `    headers: {},
    body: JSON.stringify({ status }),`,
    "client",
  ],
  [
    "244 StringLiteral application/json (setWebhookStatus) -> ''",
    CLIENT,
    `    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),`,
    `    headers: { "Content-Type": "" },
    body: JSON.stringify({ status }),`,
    "client",
  ],
  [
    "212 ObjectLiteral { signal } de listWebhooks -> {}",
    CLIENT,
    `  const response = await apiRequest("/api/v1/me/webhooks", { signal });`,
    `  const response = await apiRequest("/api/v1/me/webhooks", {});`,
    "client",
  ],
  [
    "276 ObjectLiteral { signal } de deliveries -> {}",
    CLIENT,
    `  const response = await apiRequest(\`\${webhookUrl(id)}/deliveries\`, { signal });`,
    `  const response = await apiRequest(\`\${webhookUrl(id)}/deliveries\`, {});`,
    "client",
  ],
  [
    "224 ConditionalExpression !exact(endpoint secret) -> false",
    CLIENT,
    `  if (!exact(body, "endpoint secret")) throw incompatible();`,
    `  if (false) throw incompatible();`,
    "client",
  ],
  [
    "229 ConditionalExpression typeof secret !== string -> false",
    CLIENT,
    `    typeof body.secret !== "string" ||`,
    `    false ||`,
    "client",
  ],
  [
    "233 Regex ancla ^ del secreto",
    CLIENT,
    `    !/^whsec_[A-Za-z0-9_-]{43}$/.test(body.secret)`,
    `    !/whsec_[A-Za-z0-9_-]{43}$/.test(body.secret)`,
    "client",
  ],
  [
    "248 ConditionalExpression crosscheck de status -> false",
    CLIENT,
    `  if (endpoint.id !== id || endpoint.status !== status) throw incompatible();`,
    `  if (false) throw incompatible();`,
    "client",
  ],
  [
    "249 LogicalOperator crosscheck || -> &&",
    CLIENT,
    `  if (endpoint.id !== id || endpoint.status !== status) throw incompatible();`,
    `  if (endpoint.id !== id && endpoint.status !== status) throw incompatible();`,
    "client",
  ],
  [
    "250 ConditionalExpression endpoint.id !== id -> false",
    CLIENT,
    `  if (endpoint.id !== id || endpoint.status !== status) throw incompatible();`,
    `  if (false || endpoint.status !== status) throw incompatible();`,
    "client",
  ],
  [
    "252 ConditionalExpression endpoint.status !== status -> false",
    CLIENT,
    `  if (endpoint.id !== id || endpoint.status !== status) throw incompatible();`,
    `  if (endpoint.id !== id || false) throw incompatible();`,
    "client",
  ],
  [
    "260 ConditionalExpression status !== 204 -> false",
    CLIENT,
    `  if (response.status !== 204) throw response;`,
    `  if (false) throw response;`,
    "client",
  ],
  [
    "266 ConditionalExpression !exact(delivery) -> false",
    CLIENT,
    `  if (!exact(body, "delivery")) throw incompatible();`,
    `  if (false) throw incompatible();`,
    "client",
  ],
  // ── Racimo 5: las doce etiquetas y su emparejamiento con los tipos ─────
  [
    "290 StringLiteral Editar proyecto -> ''",
    VIEW,
    `  "Editar proyecto",`,
    `  "",`,
    "view",
  ],
  [
    "291 StringLiteral Cambiar estado de proyecto -> ''",
    VIEW,
    `  "Cambiar estado de proyecto",`,
    `  "",`,
    "view",
  ],
  [
    "294 StringLiteral Cambiar estado de tarea -> ''",
    VIEW,
    `  "Cambiar estado de tarea",`,
    `  "",`,
    "view",
  ],
  [
    "295 StringLiteral Planificar bloque -> ''",
    VIEW,
    `  "Planificar bloque",`,
    `  "",`,
    "view",
  ],
  [
    "299 StringLiteral Extender sesion -> ''",
    VIEW,
    `  "Extender sesión",`,
    `  "",`,
    "view",
  ],
  [
    "300 StringLiteral Cerrar sesion de trabajo -> ''",
    VIEW,
    `  "Cerrar sesión de trabajo",`,
    `  "",`,
    "view",
  ],
  [
    "DEFECTO desalineamiento de las dos listas paralelas",
    VIEW,
    `  "Crear proyecto",
  "Editar proyecto",`,
    `  "Crear proyecto",
  "Archivar proyecto",
  "Editar proyecto",`,
    "view",
  ],
  // ── Racimo 7: marcar y DESMARCAR tipos de evento ───────────────────────
  [
    "557 ConditionalExpression maestra checked -> false",
    VIEW,
    `              checked={types.length === webhookEventTypes.length}`,
    `              checked={false}`,
    "view",
  ],
  [
    "561 ArrayDeclaration rama de desmarcar la maestra -> ['Stryker']",
    VIEW,
    `                setTypes(event.target.checked ? [...webhookEventTypes] : [])`,
    `                setTypes(
                  event.target.checked
                    ? [...webhookEventTypes]
                    : ["Stryker was here"],
                )`,
    "view",
  ],
  [
    "572 MethodExpression desmarcar uno -> current",
    VIEW,
    `                      : current.filter((candidate) => candidate !== type),`,
    `                      : current,`,
    "view",
  ],
  [
    "573 ArrowFunction predicado de desmarcar -> undefined",
    VIEW,
    `                      : current.filter((candidate) => candidate !== type),`,
    `                      : current.filter((() => undefined) as never),`,
    "view",
  ],
  [
    "574 ConditionalExpression candidate !== type -> true",
    VIEW,
    `                      : current.filter((candidate) => candidate !== type),`,
    `                      : current.filter(() => true),`,
    "view",
  ],
  [
    "575 ConditionalExpression candidate !== type -> false",
    VIEW,
    `                      : current.filter((candidate) => candidate !== type),`,
    `                      : current.filter(() => false),`,
    "view",
  ],
  [
    "576 EqualityOperator candidate !== type -> ===",
    VIEW,
    `                      : current.filter((candidate) => candidate !== type),`,
    `                      : current.filter((candidate) => candidate === type),`,
    "view",
  ],
  [
    "347 StringLiteral description inicial -> 'Stryker'",
    VIEW,
    `  const [description, setDescription] = useState("");`,
    `  const [description, setDescription] = useState("Stryker was here!");`,
    "view",
  ],
  [
    "348 ArrayDeclaration types inicial -> ['Stryker']",
    VIEW,
    `  const [types, setTypes] = useState<string[]>([]);`,
    `  const [types, setTypes] = useState<string[]>(["Stryker was here"]);`,
    "view",
  ],
  [
    "350 BooleanLiteral uncertain inicial -> true",
    VIEW,
    `  const [uncertain, setUncertain] = useState(false);`,
    `  const [uncertain, setUncertain] = useState(true);`,
    "view",
  ],
  [
    "352 StringLiteral announcement inicial -> 'Stryker'",
    VIEW,
    `  const [announcement, setAnnouncement] = useState("");`,
    `  const [announcement, setAnnouncement] = useState("Stryker was here!");`,
    "view",
  ],
  [
    "555 ArrowFunction onChange de descripcion -> undefined",
    VIEW,
    `            onChange={(event) => setDescription(event.target.value)}`,
    `            onChange={() => undefined}`,
    "view",
  ],
  // -- Racimo 8: report() reutilizado por act()
  [
    "445 ConditionalExpression code === WEBHOOK_INVALID -> false",
    VIEW,
    '    if (code === "WEBHOOK_INVALID") {',
    "    if (false) {",
    "view",
  ],
  [
    "447 StringLiteral WEBHOOK_INVALID -> ''",
    VIEW,
    '    if (code === "WEBHOOK_INVALID") {',
    '    if (code === "") {',
    "view",
  ],
  [
    "448 BlockStatement rama WEBHOOK_INVALID -> {}",
    VIEW,
    '    if (code === "WEBHOOK_INVALID") {\n      setFormError("Revisa los datos del formulario.");\n      return;\n    }',
    '    if (code === "WEBHOOK_INVALID") {\n    }',
    "view",
  ],
  [
    "450 StringLiteral Revisa los datos -> ''",
    VIEW,
    '      setFormError("Revisa los datos del formulario.");',
    '      setFormError("");',
    "view",
  ],
  [
    "452 ConditionalExpression error instanceof Response -> false",
    VIEW,
    "    if (error instanceof Response) {",
    "    if (false) {",
    "view",
  ],
  [
    "453 BlockStatement rama Response generica -> {}",
    VIEW,
    '    if (error instanceof Response) {\n      setFormError("No se ha podido crear el webhook.");\n      return;\n    }',
    "    if (error instanceof Response) {\n    }",
    "view",
  ],
  [
    "455 StringLiteral No se ha podido crear -> ''",
    VIEW,
    '      setFormError("No se ha podido crear el webhook.");',
    '      setFormError("");',
    "view",
  ],
  [
    "460 BlockStatement catch de act -> {}",
    VIEW,
    "    } catch (error) {\n      if (!controller.signal.aborted) await reportAction(error, failure);\n    }",
    "    } catch {\n    }",
    "view",
  ],
  [
    "461 BooleanLiteral !aborted -> aborted (act)",
    VIEW,
    "      if (!controller.signal.aborted) await reportAction(error, failure);",
    "      if (controller.signal.aborted) await reportAction(error, failure);",
    "view",
  ],
  [
    "462 ConditionalExpression guarda de act -> true",
    VIEW,
    "      if (!controller.signal.aborted) await reportAction(error, failure);",
    "      if (true) await reportAction(error, failure);",
    "view",
  ],
  [
    "463 ConditionalExpression guarda de act -> false",
    VIEW,
    "      if (!controller.signal.aborted) await reportAction(error, failure);",
    "      if (false) await reportAction(error, failure);",
    "view",
  ],
  [
    "NUEVO mensaje de desactivar -> ''",
    VIEW,
    '        ? "No se ha podido desactivar el webhook."',
    '        ? ""',
    "view",
  ],
  [
    "NUEVO mensaje de activar -> ''",
    VIEW,
    '        : "No se ha podido activar el webhook.",',
    '        : "",',
    "view",
  ],
  [
    "NUEVO mensaje de ping -> ''",
    VIEW,
    '    }, "No se ha podido enviar el ping.");',
    '    }, "");',
    "view",
  ],
  [
    "NUEVO mensaje de eliminar -> ''",
    VIEW,
    '    }, "No se ha podido eliminar el webhook.");',
    '    }, "");',
    "view",
  ],
  [
    "NUEVO mensaje de entregas -> ''",
    VIEW,
    '    }, "No se han podido cargar las entregas.");',
    '    }, "");',
    "view",
  ],
  [
    "NUEVO mensaje de reenviar -> ''",
    VIEW,
    '    }, "No se ha podido reenviar la entrega.");',
    '    }, "");',
    "view",
  ],
  [
    "DEFECTO act vuelve a usar el reportero de la creacion",
    VIEW,
    "      if (!controller.signal.aborted) await reportAction(error, failure);",
    "      if (!controller.signal.aborted) await report(error);",
    "view",
  ],
  // -- Racimo 9: la fuga de entregas entre webhooks
  [
    "489 MethodExpression filtro del ping -> current",
    VIEW,
    "        ...current.filter((d) => d.id !== sent.id),",
    "        ...current,",
    "view",
  ],
  [
    "490 ArrowFunction predicado del filtro -> undefined",
    VIEW,
    "        ...current.filter((d) => d.id !== sent.id),",
    "        ...current.filter((() => undefined) as never),",
    "view",
  ],
  [
    "491 ConditionalExpression d.id !== sent.id -> true",
    VIEW,
    "        ...current.filter((d) => d.id !== sent.id),",
    "        ...current.filter(() => true),",
    "view",
  ],
  [
    "492 ConditionalExpression d.id !== sent.id -> false",
    VIEW,
    "        ...current.filter((d) => d.id !== sent.id),",
    "        ...current.filter(() => false),",
    "view",
  ],
  [
    "493 EqualityOperator d.id !== sent.id -> ===",
    VIEW,
    "        ...current.filter((d) => d.id !== sent.id),",
    "        ...current.filter((d) => d.id === sent.id),",
    "view",
  ],
  [
    "609 ConditionalExpression item.id === deliveriesOf -> true",
    VIEW,
    "              const endpoint = items.find((item) => item.id === deliveriesOf);",
    "              const endpoint = items.find(() => true);",
    "view",
  ],
  [
    "612 ConditionalExpression if (endpoint) -> true",
    VIEW,
    "              if (endpoint) openDeliveries(endpoint);",
    "              openDeliveries(endpoint as WebhookEndpoint);",
    "view",
  ],
  [
    "DEFECTO la tabla conserva las filas del webhook anterior",
    VIEW,
    "    if (deliveriesOf !== endpointId) setDeliveries([]);\n    setDeliveriesOf(endpointId);",
    "    setDeliveriesOf(endpointId);",
    "view",
  ],
  // -- Racimo 10: guardas de aborto
  [
    "204 CallExpression throwIfAborted de json (1) -> ;",
    CLIENT,
    "  signal.throwIfAborted();\n  if (!expected.includes(response.status)) throw response;",
    "  if (!expected.includes(response.status)) throw response;",
    "client",
  ],
  [
    "208 CallExpression throwIfAborted de json (2) -> ;",
    CLIENT,
    "  const body: unknown = await response.json();\n  signal.throwIfAborted();\n  return body;",
    "  const body: unknown = await response.json();\n  return body;",
    "client",
  ],
  [
    "215 CallExpression arranque de createWebhook -> ;",
    CLIENT,
    "export async function createWebhook(input: WebhookInput, signal: AbortSignal) {\n  signal.throwIfAborted();",
    "export async function createWebhook(input: WebhookInput, signal: AbortSignal) {",
    "client",
  ],
  [
    "239 CallExpression arranque de setWebhookStatus -> ;",
    CLIENT,
    ") {\n  signal.throwIfAborted();\n  const response = await apiRequest(`${webhookUrl(id)}/status`, {",
    ") {\n  const response = await apiRequest(`${webhookUrl(id)}/status`, {",
    "client",
  ],
  [
    "255 CallExpression arranque de deleteWebhook -> ;",
    CLIENT,
    "export async function deleteWebhook(id: string, signal: AbortSignal) {\n  signal.throwIfAborted();",
    "export async function deleteWebhook(id: string, signal: AbortSignal) {",
    "client",
  ],
  [
    "258 CallExpression throwIfAborted tras el DELETE -> ;",
    CLIENT,
    "  signal.throwIfAborted();\n  if (response.status !== 204) throw response;",
    "  if (response.status !== 204) throw response;",
    "client",
  ],
  [
    "269 CallExpression arranque de pingWebhook -> ;",
    CLIENT,
    "export async function pingWebhook(id: string, signal: AbortSignal) {\n  signal.throwIfAborted();",
    "export async function pingWebhook(id: string, signal: AbortSignal) {",
    "client",
  ],
  [
    "274 CallExpression arranque de listWebhookDeliveries -> ;",
    CLIENT,
    "export async function listWebhookDeliveries(id: string, signal: AbortSignal) {\n  signal.throwIfAborted();",
    "export async function listWebhookDeliveries(id: string, signal: AbortSignal) {",
    "client",
  ],
  [
    "279 CallExpression arranque de redeliverWebhook -> ;",
    CLIENT,
    ") {\n  signal.throwIfAborted();\n  if (!identifier(deliveryId))",
    ") {\n  if (!identifier(deliveryId))",
    "client",
  ],
  [
    "365 ConditionalExpression !aborted (setItems) -> true",
    VIEW,
    "        if (!controller.signal.aborted) setItems(next);",
    "        if (true) setItems(next);",
    "view",
  ],
  [
    "370 ConditionalExpression !aborted (setLoadFailed) -> true",
    VIEW,
    "        if (!controller.signal.aborted) setLoadFailed(true);",
    "        if (true) setLoadFailed(true);",
    "view",
  ],
  [
    "376 ConditionalExpression !aborted (setLoading) -> true",
    VIEW,
    "        if (!controller.signal.aborted) setLoading(false);",
    "        if (true) setLoading(false);",
    "view",
  ],
  [
    "402 ConditionalExpression aborted -> false (submit exito)",
    VIEW,
    "      if (controller.signal.aborted) return;\n      setSecret(created.secret);",
    "      if (false) return;\n      setSecret(created.secret);",
    "view",
  ],
  [
    "415 ConditionalExpression aborted -> false (submit fallo)",
    VIEW,
    "      if (controller.signal.aborted) return;\n      await report(error);",
    "      if (false) return;\n      await report(error);",
    "view",
  ],
  [
    "418 ConditionalExpression !aborted (setCreating) -> true",
    VIEW,
    "      if (!controller.signal.aborted) setCreating(false);",
    "      if (true) setCreating(false);",
    "view",
  ],
  [
    "462 ConditionalExpression guarda de act -> true",
    VIEW,
    "      if (!controller.signal.aborted) await reportAction(error, failure);",
    "      if (true) await reportAction(error, failure);",
    "view",
  ],
  [
    "467 ConditionalExpression aborted -> false (changeStatus)",
    VIEW,
    "        const updated = await setWebhookStatus(endpoint.id, status, signal);\n        if (signal.aborted) return;",
    "        const updated = await setWebhookStatus(endpoint.id, status, signal);\n        if (false) return;",
    "view",
  ],
  // -- Racimo 11: las celdas de la tabla de entregas
  [
    "617 LogicalOperator httpStatus ?? -> &&",
    VIEW,
    '{row.httpStatus ?? "—"}',
    '{row.httpStatus && "—"}',
    "view",
  ],
  [
    "618 StringLiteral guion de httpStatus -> ''",
    VIEW,
    '{row.httpStatus ?? "—"}',
    '{row.httpStatus ?? ""}',
    "view",
  ],
  [
    "619 ConditionalExpression latencyMs === null -> true",
    VIEW,
    '{row.latencyMs === null ? "—" : `${row.latencyMs} ms`}',
    '{true ? "—" : `${row.latencyMs} ms`}',
    "view",
  ],
  [
    "620 ConditionalExpression latencyMs === null -> false",
    VIEW,
    '{row.latencyMs === null ? "—" : `${row.latencyMs} ms`}',
    '{false ? "—" : `${row.latencyMs} ms`}',
    "view",
  ],
  [
    "621 EqualityOperator latencyMs === null -> !==",
    VIEW,
    '{row.latencyMs === null ? "—" : `${row.latencyMs} ms`}',
    '{row.latencyMs !== null ? "—" : `${row.latencyMs} ms`}',
    "view",
  ],
  [
    "622 StringLiteral guion de latencia -> ''",
    VIEW,
    '{row.latencyMs === null ? "—" : `${row.latencyMs} ms`}',
    '{row.latencyMs === null ? "" : `${row.latencyMs} ms`}',
    "view",
  ],
  [
    "623 StringLiteral plantilla de ms -> ''",
    VIEW,
    '{row.latencyMs === null ? "—" : `${row.latencyMs} ms`}',
    '{row.latencyMs === null ? "—" : ``}',
    "view",
  ],
  [
    "624 LogicalOperator errorClass ?? -> &&",
    VIEW,
    '{row.errorClass ?? "—"}',
    '{row.errorClass && "—"}',
    "view",
  ],
  [
    "625 StringLiteral guion de errorClass -> ''",
    VIEW,
    '{row.errorClass ?? "—"}',
    '{row.errorClass ?? ""}',
    "view",
  ],
  [
    "626 MethodExpression updatedAt.slice -> updatedAt",
    VIEW,
    "{row.updatedAt.slice(0, 10)}",
    "{row.updatedAt}",
    "view",
  ],
  [
    "586 ArrowFunction traduccion de tipos -> undefined",
    VIEW,
    "                .map(\n                  (type) =>\n                    eventLabels[webhookEventTypes.indexOf(type as never)],\n                )",
    "                .map((() => undefined) as never)",
    "view",
  ],
  [
    "587 StringLiteral join(', ') -> ''",
    VIEW,
    '.join(", ")}',
    '.join("")}',
    "view",
  ],
  [
    "DEFECTO data-label de Latencia desalineado",
    VIEW,
    '<td role="cell" data-label="Latencia">',
    '<td role="cell" data-label="Intento">',
    "view",
  ],
  // -- Racimo 12: identidad por elemento, reinicio del formulario y a11y
  [
    "471 ConditionalExpression item.id === updated.id -> true",
    VIEW,
    "          current.map((item) => (item.id === updated.id ? updated : item)),",
    "          current.map(() => updated),",
    "view",
  ],
  [
    "503 ArrowFunction filtro de eliminar -> undefined",
    VIEW,
    "      setItems((current) => current.filter((item) => item.id !== endpoint.id));",
    "      setItems((current) => current.filter((() => undefined) as never));",
    "view",
  ],
  [
    "505 ConditionalExpression item.id !== endpoint.id -> false",
    VIEW,
    "      setItems((current) => current.filter((item) => item.id !== endpoint.id));",
    "      setItems((current) => current.filter(() => false));",
    "view",
  ],
  [
    "507 CallExpression setConfirming(null) -> ;",
    VIEW,
    '      setConfirming(null);\n      setAnnouncement("Webhook eliminado.");',
    '      setAnnouncement("Webhook eliminado.");',
    "view",
  ],
  [
    "525 ConditionalExpression item.id === reopened.id -> true",
    VIEW,
    "        current.map((item) => (item.id === reopened.id ? reopened : item)),",
    "        current.map(() => reopened),",
    "view",
  ],
  [
    "408 StringLiteral setUrl('') -> 'Stryker'",
    VIEW,
    '      setUrl("");',
    '      setUrl("Stryker was here!");',
    "view",
  ],
  [
    "410 StringLiteral setDescription('') -> 'Stryker'",
    VIEW,
    '      setDescription("");',
    '      setDescription("Stryker was here!");',
    "view",
  ],
  [
    "412 ArrayDeclaration setTypes([]) -> ['Stryker']",
    VIEW,
    "      setTypes([]);",
    '      setTypes(["Stryker was here"]);',
    "view",
  ],
  [
    "395 CallExpression setFormError(null) -> ;",
    VIEW,
    "    setFormError(null);\n    setUrlError(null);",
    "    setUrlError(null);",
    "view",
  ],
  [
    "396 CallExpression setUrlError(null) -> ;",
    VIEW,
    "    setFormError(null);\n    setUrlError(null);",
    "    setFormError(null);",
    "view",
  ],
  [
    "390 CallExpression preventDefault -> ;",
    VIEW,
    "    event.preventDefault();\n    if (creating) return;",
    "    if (creating) return;",
    "view",
  ],
  [
    "385 BooleanLiteral setLoading(true) -> false",
    VIEW,
    "    setLoading(true);\n    setLoadFailed(false);",
    "    setLoading(false);\n    setLoadFailed(false);",
    "view",
  ],
  [
    "387 BooleanLiteral setLoadFailed(false) -> true",
    VIEW,
    "    setLoading(true);\n    setLoadFailed(false);",
    "    setLoading(true);\n    setLoadFailed(true);",
    "view",
  ],
  [
    "543 StringLiteral alerta de carga fallida -> ''",
    VIEW,
    '          No se ha podido cargar la lista de webhooks.{" "}',
    '          {""}{" "}',
    "view",
  ],
  [
    "536 UnaryOperator tabIndex del main -> +1",
    VIEW,
    '<main id="proyectos" tabIndex={-1} className="webhooks">',
    '<main id="proyectos" tabIndex={+1} className="webhooks">',
    "view",
  ],
  [
    "584 UnaryOperator tabIndex del h2 -> +1",
    VIEW,
    "      <h2 tabIndex={-1} ref={listHeading}>",
    "      <h2 tabIndex={+1} ref={listHeading}>",
    "view",
  ],
  [
    "547 ArrowFunction onFocus del secreto -> undefined",
    VIEW,
    "            onFocus={(e) => e.currentTarget.select()}",
    "            onFocus={() => undefined}",
    "view",
  ],
];

const suites = {
  client: "src/webhooks-client.test.ts",
  view: "src/webhooks.test.tsx",
  both: "src/webhooks",
};

/**
 * NO EJECUTES ESTE SCRIPT EN SEGUNDO PLANO.
 *
 * El `finally` de cada pasada sólo restaura si el proceso sigue vivo. Si se
 * mata a mitad —y en Windows una tarea en segundo plano que agota su tiempo se
 * mata sin avisar, además de que matar el envoltorio de msys **no** alcanza al
 * proceso real— el mutante se queda escrito en producción y el siguiente
 * `git add -A` lo commitea. Pasó: el mutante 461 llegó a un commit.
 *
 * Estos manejadores cubren la salida ordenada, pero **no** un SIGKILL. La única
 * garantía es la de abajo: al terminar, el script compara con `git` y avisa.
 */
const intactos = new Map(
  [CLIENT, VIEW].map((fichero) => [fichero, readFileSync(fichero, "utf8")]),
);
function restaurarTodo() {
  for (const [fichero, texto] of intactos)
    if (readFileSync(fichero, "utf8") !== texto) writeFileSync(fichero, texto);
}
process.on("exit", restaurarTodo);
for (const senal of ["SIGINT", "SIGTERM", "SIGHUP", "SIGBREAK"])
  process.on(senal, () => {
    restaurarTodo();
    process.exit(130);
  });
process.on("uncaughtException", (error) => {
  restaurarTodo();
  throw error;
});

const filtro = process.argv[2] ?? "";
let rojos = 0;
let verdes = 0;

for (const [nombre, fichero, buscar, reemplazo, suite] of MUTANTS) {
  if (filtro && !new RegExp(filtro, "i").test(nombre)) continue;
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

// La comprobación que de verdad protege: no basta con creerse el `finally`, hay
// que preguntarle a git si producción quedó como estaba. Si no, salida distinta
// de cero para que nadie commitee un mutante sin enterarse.
const sucio = execFileSync("git", ["diff", "--name-only", "--", CLIENT, VIEW], {
  encoding: "utf8",
}).trim();
if (sucio) {
  console.error(
    `\nPRODUCCIÓN SUCIA, un mutante se ha quedado escrito:\n${sucio}`,
  );
  console.error("Restáuralo antes de commitear nada.");
  process.exitCode = 1;
} else {
  console.log("Producción intacta: git diff vacío.");
}
