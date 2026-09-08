# TDD frontend 22: export_data

Checkout exclusivo OrganizacionWeb-export-data; contrato cc78396 aprobado.
Ponytail full/Caveman lite. Init verde proporcionado por root; no se repite.
Sin cambios a COMMON, V14, backend ni Git. Un caso por ciclo; no campaña global.

## Ciclos

1. @s1/@s24, transporte nominal de cuenta vacía: catorce colecciones y counts,
   owner Unicode, archivo con indentación/salto final y bytes exactos. RED
   0e5c2e por módulo ausente. Primer intento a15498 seguía fallando por igualdad
   de Uint8Array entre realms JSDOM/Node, sin diferencia de bytes. El oráculo
   compara ahora todos los bytes como arrays y conserva metadatos exactos;
   GREEN en export_frontend_01_verified.log. El log llamado 01_green conserva
   aquel intento fallido y no acredita un GREEN.
   Cliente deliberadamente parcial: todavía sin validaciones, límite de lectura,
   abortos ni UI. No se presenta este corte como frontera de confianza completa.

2–13. Rechazo de tamaño declarado excesivo, exceso de bytes durante lectura,
   longitud ausente, owner ajeno, envelope adicional, formato/versión ajenos,
   colección ausente, count incoherente, tipo MIME, gzip y ruta en filename.
   Cada caso tuvo RED real y mínimo GREEN antes del siguiente. Logs agrupados
   export_frontend_02_red/green.log hasta export_frontend_13_red/green.log;
   último resultado 13/13, confirmado en 33368b. Reader acotado y cancelación
   del exceso sin retener el lock. Se reutiliza exact; no cambia su fuente.
14–21. Correspondencia de filename con exportedAt, fecha inexistente, UTF-8
   inválido, BOM, Response HTTP rechazada, aborto previo, HTTP tardío y aborto
   durante stream. RED/GREEN en logs 14–21. Último GREEN 9cdc99, 21/21.
   instant se reutiliza sólo para el timestamp del envelope; sus microsegundos
   siguen textuales. La prueba21 primero cerraba el stream tras abortar;
   se ajustó para entregar el chunk y dejar abierto el transporte, permitiendo
   comprobar cancelación sin provocar un enqueue sobre stream ya cancelado.
22–31. GREEN inicial, declarado: 401 tardío no revoca sesión actual; truncamiento;
   aborto esperando datos; límite inclusivo de 33554432 bytes con long textual;
   longitud cero, filename* adicional, colección no array, count fraccionario,
   interrupción y JSON incompleto. Logs 22–31 initial_green, uno por ciclo.
   En23 se hizo explícito el contador final: antes JSON ya rechazaba el padding
   de un archivo truncado; no se presenta como RED. Regresión 31/31 34480c.
32. Revisión root: cabeceras rechazadas dejaban transporte abierto. RED 2b83cd
   con stream sin datos, cancel no llamado; GREEN 44ccde, 32/32. Cancelación
   sin pull/getReader. Cinco oráculos previos ahora observan ausencia de lectura
   y lock liberado: bodyUsed también cambia al cancelar y no era el hecho
   contractual. Response no200 conserva cuerpo intacto para el flujo existente.

## Checkpoint cliente

Dos fuentes únicamente: export-data-api.ts y export-data-api.test.ts. Comandos:
pnpm --dir frontend test src/export-data-api.test.ts; exec tsc --noEmit;
exec eslint de ambos archivos y exec prettier --write de ambos. Evidencia final
export_frontend_client_final_verified.log, client_types_final.log,
client_lint_final.log y client_format_final.log. El primer lint fcb759 detectó
dos escapes innecesarios en el fixture filename*, corregidos sin cambio lógico.
Manifiesto export_frontend_client_freeze.json. Sin suite global ni mutación.

## Mapa y pendientes

@s1/@s24: bytes nominales y frontera máxima. @s25: variantes de transporte y
envelope cubiertas; @s28: Response preservada; @s29: abortos/401 tardío cliente.
@s2–21/@s33: servidor/contrato HTTP son responsabilidad de A/C; el cliente no
reinterpreta registros de negocio. @s22–32: falta montaje UI, feedback, descarga
nativa, lifecycle del objectURL y foco. No hay evidencia E2E/UX ni mutación.
