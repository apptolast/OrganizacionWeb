# Revisión independiente de refuerzos nuevos frontend17

**APPROVED para los cinco pasos y su selección dirigida**, sin anticipar resultado del replay ni cerrar la feature. Lectura completa del diff de tres tests y del informe/targets fdb0a7; firmas originales verificadas 34d0f8. No se ejecutaron suites ni se modificaron tests durante esta revisión.

## Oráculos y aislamiento

- POST503 seguido de K503 desconocido: conserva cantidad e incertidumbre, muestra Comprobar y no Reenviar; Enter real de userEvent y submit explícito no crean segundo POST. El test espera que termine el anuncio de comprobación y cuenta tres solicitudes, por lo que no se limita a observar la primera espera. El submit explícito ejercita la guarda aunque el input sea readOnly. Usa la interfaz pública; no invoca confirm internamente.
- Dos confirmaciones sucesivas: Surface usa el hook real y StatePanel real; un botón entrega la notificación pública settle. Tercera S queda diferida; no permite POST antes de resolver y el posterior lleva revisión3. Distingue el incremento convertido en undefined, que puede pasar una sola confirmación. Es un oráculo de composición de la notificación, no dos commits reales de servidor; esa integración tiene evidencia separada.
- Timer de25 días: refuerza el caso existente con límite exacto2147483647 pasado a setTimeout, conservando ausencia de GET/aviso anticipados y un timer restante tras avanzar el primer fragmento. No atribuye a fake timers el overflow nativo del navegador: observa el valor entregado a la plataforma, precisamente lo que el runner simulado no truncaba. El tiempo monotónico avanza coherentemente con el fragmento. No espera25 días reales ni afirma esa ejecución.
- E con época1600: dos casos separados mantienen sesión/State y cabecera válidos; sólo serverNow o effectiveEndAt es inválido. La época negativa evita que la comparación numérica posterior de null oculte la guarda explícita. Ambos exigen el error público del decoder. No cambian a la vez otros campos para conseguir rechazo incidental.

El afterEach del panel restaura spies, globals y timers, y retira observador. Los otros archivos conservan su aislamiento existente. No aserción anterior eliminada ni dependencia nueva. El uso del wrapper de composición no equivale a probar campos internos de React ni a una nueva arquitectura productiva.

## Evidencia y freeze

B documenta cinco resultados inicialmente GREEN: a64fb7,1381f6,ea582a,9fe1ae,b15bc8. No RED de producto inventado. Foco conjunto **59/59 GREEN90d674** (End33, API23, hook3), formato077836 y ESLint e3e6d4; resultados del autor, no ejecuciones de este juez.

Hashes actuales contrastados con el freeze del autor:

| Test | SHA256 |
| --- | --- |
| use-work-session-decision.test.tsx | 49BC5F5EAFC41CE3C6D17ADE7F0C6B2B4192F428F485A683CB7206FF0E6E6E26 |
| work-session-end-api.test.ts | 7F7115BEA5881DE9DCF17D35BDAB43ADD60B27E8D7C27D50ACA4A483E3548B9C |
| work-session-end.test.tsx | 39690C261D481F439C63A48EFDF2873BCF288AB2362BFDF5EA132BA0BE52D762 |

Producción coincide con el source del raw original Stryker normalizando sólo CRLF (dfd1d5); hashes actuales: hook A17E547AE2CF14CE074B8BD4C51EF32592573837F5C56D213FCDC2CC2FF8060B; API A7BD65AA29827DE31BE60286C528E55710ACA53F2A968C4B11F6454D267CFCE6; panel 667FA2EEBA44225BD2CB19A9E7DA02BFE664AB158F7212EF106CE1872CEDFBFA. No cambio de producción atribuible a estos refuerzos.

## Selección y límites

Las nueve entradas de end_time_new_reinforcement_targets.json coinciden con raw por archivo/rango/mutador/sustitución: originales14/52/62/155/217/225/226/228/357. Columnas del selector restan1 respecto a JSON, sin desplazar líneas. Los rangos solapados pueden producir extras; el replay debe informar todo el inventario y presencia/estado exacto de cada firma, no sólo score. La eliminación de optional chaining228 podría producir un error de runner en vez de Killed; no reclasificar si ocurre.

Aprobación suficiente para que root configure el único replay conjunto autorizado. No más refuerzos solicitados, no campañas automáticas, no equivalencias generales ni cambios retrospectivos del resultado1980 original. Privacidad tardía y otras firmas permanecen límites documentados del informe, fuera de esta ampliación acotada.

Root revisó el diff funcional b977a5 y configuración c819c7/84fd10; sólo rangos/reportes/temp cambian respecto a la base. Tras revisión de A, el formato del test EndAPI se corrigió sin cambio de AST: hash final10DDBF5871F9B263B50F75C1AC7BC5C7EE7442A95119035E433E7C603D969664. Init final49155 EXIT0 81dc46:2076Java,1802frontend/38,40Node,lint/formato. Se conserva primer intento53535 EXIT1 por formato. Aprobado para replay dirigido con130entradas congeladas, sin cambios productivos.
