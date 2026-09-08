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

## Ciclos UI y composición

33–43. Un RED y mínimo GREEN por ciclo, logs export_frontend_33 a43:
entrada sin GET (bedb42/314de8); Blob y enlace nativo con bytes originales
(ca4640/ac5014); duplicados y feedback (cbf8da/51c01c); cancelar y HTTP tardío
(58c443/a79ee8);503 manual (a31f3a/a5dee4); desmontaje (551043/0a05ca);
revoke (45cf09/2b50b1); preparar de nuevo (a3f0ff/2e5328); identidad
(a1bb6a/42cdd0);413 explicado (c645a2/83b7ac); navegación (341cf0/599120).
No se usa RouteLink para descargar. El fichero sólo se crea tras validar bytes.

44. Composición SessionGate con identidad real. Los primeros logs red/green
fallaron por una ruta de fixture equivocada (/api/v1/auth/session). Corregida a
/api/session según session-api, se retiró la nueva prop para verificar RED real
f66108 (Página no encontrada), y se repuso: GREEN215c6b. Se preservan ambos
intentos originales sin atribuirles validación. No se altera la API de sesión.
45. Retorno tras login conserva /exportacion:3951b7/a9d351. Sólo nueva ruta
privada; no cambios de persistencia ni resets de formularios ajenos.
46–49. Foco al desaparecer Cancelar, desaparición por413, movimiento voluntario
seguido de blur, entrada por navegación. RED/GREEN respectivos c34b51/7ae8d7,
b6c8af/20f9de,76e61c/b3b788,a070df/379b7b.
50. Ana → logout → Bruno →401 tardío, composición real SessionGate/apiRequest:
GREEN inicial1b9294. No petición adicional ni revocación de Bruno.
51. Intento de simular blur automático falló en la precondición: JSDOM conserva
foco sobre el botón disabled al invocar blur. Logs51_red y51_green preservan
ese límite, no acreditan defecto del producto. Se retiró la modificación de foco
propuesta y el caso se ajustó a lo observable en DOM: conserva el control que
sigue enfocado, GREEN inicial ef66cf. El comportamiento físico queda para UX.
También se hace explícita la guarda local tras await del cliente antes de Blob;
el mismo controller/identidad protege finalmente el resultado y los errores.
52–55. GREEN inicial, uno por ciclo:401 vigente retira acceso (8c0afa), respuesta
incompatible no crea URL (12ea1b), reutilizar enlace conserva archivo sin GET
(62e8f6), JSON de error resuelto después de cancelar no restaura fallo (34adb6).
56. Revisión root corrigió el h1 contractual y ayuda:8a27ee/46bc9e. Nombre final
«Exportar mis datos»; enlace de navegación «Exportación». Se explica JSON
versionado, archivo personal e importación todavía no disponible.
57. Revisión root: cleanup pasivo dejaba ventana hasta layout del nuevo owner.
El caso de identidad previo ahora observa revoke desde un probe de layout:
RED36747d, GREENe05ed1. Cleanup de layout aborta y revoca antes del nuevo owner.

## Corte funcional para revisión

Cliente aprobado en832331f, sin cambios posteriores. UI22 +cliente32 y
regresión App/auth:143/143 (aa6cee y focal_verified). Tipos52570e y lint5ee54c
EXIT0; formato5d37ab. Se añadió después una guarda simétrica antes de consumir
el error tardío; focal_verified conserva143. Build frontend registrado en
export_frontend_ui_build.log. SCSS local usa tokens existentes, áreas44px,
wrap y separación; sólo revisión de código hasta UX real, sin afirmar geometría.
Manifiesto export_frontend_ui_freeze.json. Ninguna suite global/Java/mutación.

## Mapa y pendientes

@s1/@s24: bytes nominales y frontera máxima. @s25: variantes de transporte y
envelope cubiertas; @s28: Response preservada; @s29: abortos/401 tardío cliente.
@s2–21/@s33: servidor/contrato HTTP son responsabilidad de A/C; el cliente no
reinterpreta registros de negocio. @s22–29: composición, descarga y lifecycle
acreditados en DOM; @s30 memoria local por diseño (refs, Blob/objectURL y cleanup,
sin llamadas a almacenamiento web), pendiente observación E2E. @s31 foco DOM
acreditado, comportamiento físico de disabled pendiente. @s32 UX real pendiente.
Todavía no hay E2E/UX ni mutación frontend22 ni gate global de este corte.
