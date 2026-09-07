# Refuerzo dirigido de StateAPI17

Única fuente de tests modificada: `frontend/src/work-session-state-api.test.ts`, nombre confirmado antes de editar. No producción ni tests de B. Tres casos añadidos individualmente, cada uno ejecutado antes del siguiente; no RED fabricado:

| Caso @s26 | Resultado inicial | Oráculo aislado |
| --- | --- | --- |
| feature17 rejects an EXTEND that closes an otherwise unchanged paused state | GREEN b6d1df, 1 ejecutado/48 omitidos por filtro | before paused, after closed, mismo changedAt/trabajo y runningSince null; estados internamente válidos, sólo cambio de estado incompatible con EXTEND. |
| feature17 rejects an EXTEND that only advances a paused changedAt | GREEN ebe55d, 1/49 omitidos | Ambos paused, trabajo0 y runningSince null; changedAt aumenta hasta occurredAt. La aritmética de trabajo paused sigue válida. |
| feature17 rejects an extra field only inside extension | GREEN e7ff05, 1/50 omitidos | Recibo válido salvo extra:true dentro de extension; shape exterior y fórmula conservados. |

Se reutiliza el fixture receipt/session/state del archivo, con JSON explícito. Los tres esperan el error público controlado «Cambio de sesión inválido» mediante readWorkSessionChange; no llaman validadores privados. Los omitidos de cada foco son selección deliberada, no skips de la regresión final.

Prettier focal GREEN6960f8. Suite completa del archivo **51/51 GREEN2b2949**, EXIT0, sin omitidos. ESLint focal GREEN27d935. Diff58 líneas añadidas, ninguna aserción previa retirada. No campaña ejecutada: el contraste rojo previsto será el mutante dirigido, sujeto a replay único coordinado por root.

## Freeze y firmas objetivo

Test SHA256 `B1F53D3293EEFCED038C5FA3E5E0F55FD687B0D618B5A67E95BF0037B7EE9FA2`; producción StateAPI conserva `929352B95B730B05D62F6C578541AC61D4F98C3A84B75CEC8F2C2F0B5B6CFAB8` (2a8a55). Informe original e inventario compartido permanecen intactos.

Todas las firmas pertenecen a `src/work-session-state-api.ts`; posiciones del JSON original17, columnas base1. Los IDs sólo localizan ese raw y no deben reutilizarse como identidad entre campañas.

| Test | Rango original | Mutador y sustitución | ID original |
| --- | --- | --- | --- |
| Estado | 187:9–188:51 | LogicalOperator: `value.before.status !== "closed" || value.after.status === value.before.status` | 1138 |
| Estado | 188:9–188:51 | ConditionalExpression: `true` | 1142 |
| changedAt | 196:9–196:57 | ConditionalExpression: `true` | 1147 |
| Shape extension | 230:5–231:48 | ConditionalExpression: `false` | 1227 |
| Shape extension | 230:5–231:48 | LogicalOperator: `!exact(value, "additionalMinutes previousEndAt effectiveEndAt") && typeof value.additionalMinutes !== "number"` | 1228 |

Son cinco objetivos principales que los oráculos deberían distinguir por lectura de las guardas. No se afirma todavía Killed. Mutantes adicionales generados por esos rangos deberán inventariarse por separado; por ejemplo quitar before!=closed (1139) no es el objetivo del caso before paused. No ampliar el selector para forzar otros huecos ni sumar resultados al original1980. La corrección de columnas/rangos del replay corresponde al coordinador y debe contrastarse con el parser local ya conocido.
