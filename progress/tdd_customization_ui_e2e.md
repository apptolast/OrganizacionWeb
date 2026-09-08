# Primer recorrido de UI21

Preparación autorizada por root mientras B termina el corte de UI. Archivo propio: `e2e/customization-ui.spec.mjs`. No se ha ejecutado y no existe resultado RED/GREEN de este caso todavía. El aislado conserva backend/E2E de persistencia ya revisados; no se ha copiado producto frontend WIP.

Único recorrido preparado: desde la lista de proyectos, ocultar fecha con Guardar vista y comprobar que enlace/estado permanecen; crear definición TEXT mediante formulario; abrir detalle, guardar texto con espacios y Unicode, recargar y comprobar control, GET/ETag y JSON SQL exactos. El snapshot de proyecto/outbox anterior debe permanecer idéntico. El fixture autenticado existente limpia únicamente la personalización de e2e-user en el stack efímero; no se añade limpieza global ni se quitan FK.

Los waitForResponse observan solicitudes reales de la UI. No interceptan ni fabrican confirmaciones. El test cubre partes nominales de @s29/@s31/@s32, no la gestión completa, todos los tipos, TASK, incertidumbre ni la matriz UX. Esos ciclos se abrirán después del primer GREEN y de la revisión del corte apropiado.

Preparación local: Prettier del repositorio y `node --check` terminaron EXIT0; no se lanzó Playwright, Docker ni Gradle. SHA256 inicial del spec formateado: `6021A5AF5CAFDA6F444460010353EF152004D9C9FCFCC028FEA9EDD9784E6376`. Pendiente confirmación final de nombres accesibles de B, transferencia de snapshot revisada por root y ejecución individual con hashes antes/después/log/EXIT.

## Ejecución nominal y corrección de fixture

Root autorizó copia del manifiesto parcial UI de B `8A29A26666C06D93CFB011B883DD04D3280A822117410C77BD777BF381A67586`:14/14 hashes exactos, herramienta0a1bd7. HEAD aislado f41e79d, backend nominal anterior al final1cd88e7; no se usa build COMMON ni se atribuye el backend final. TS/TSX copiados intencionalmente permanecen fuera del paquete propio del test. B confirmó nombres y POST de definición200. No CSS nuevo en este corte.

Primer intento: `node scripts/e2e.mjs e2e/customization-ui.spec.mjs`, stack organizationweb-e2e-50904, puerto18080. EXIT1 9c219e por selector de fixture: getByLabel Tipo exacto no encontró el select cuyo label incluye opciones; error-context acredita combobox accesible Tipo. Se conservan log, exit, spec inicial y error-context copiados en progress/customization_ui_nominal*. Los576 inputs before/after coinciden. No fue RED productivo.

Corrección mínima: getByRole combobox con nombre Tipo exacto. Sin quitar oráculos, ampliar timeout ni modificar producto. Repetición del mismo caso: stack organizationweb-e2e-11532, EXIT0 b11ed8;1/1 PASS,4.2s caso/6.4s Playwright. Log customization_ui_nominal_fixed.log y exit separados. Los576 inputs before/after vuelven a coincidir. Fuente final SHA256 `DD046CC43C7A567EC0D4EE1A96D6F0521EDBAFDB2E32EAEF194539944C3B300D`.

El recorrido confirma pintura nominal de vista vacía, enlace/estado conservados, creación TEXT real, guardado con espacios/Unicode, recarga del control, GET/ETag/JSON SQL exactos y filas de negocio/outbox iguales. No acredita recuperación de esquema por GET tras412: root detectó ese pendiente después de la copia y B lo corrige. Tampoco acredita validaciones locales, foco, SCSS, TASK ni matriz UX.

Ambos lifecycle retiraron exclusivamente sus contenedores, red y volumen. Listados filtrados posteriores del stack11532 vacíos;18080 libre. Ninguna campaña global ni mutación, ningún commit realizado por C.
