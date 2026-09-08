# Refuerzo individual de clientes21

Autor A; alcance autorizado: sólo nuevo frontend/src/customization-clients.mutation.test.ts. No producto, tests originales, configuración ni helpers globales modificados. Inventario y revisión previa: review_customization_state_clients_mutation.md. Se conservan original y estados, sin atribuir kills antes del incremental autorizado de B.

Cada fila se añadió tras terminar GREEN anterior, con `pnpm --dir frontend exec vitest run src/customization-clients.mutation.test.ts`, salida en customization_clients_refinement_NN.log. Todos los EXIT fueron0 y todos los casos fueron inicialmenteGREEN: no existe RED productivo y no se inventa uno.

|Ciclo|Contrato/oráculo|Candidatos originales|Referencia|
|---|---|---|---|
|01|s28 creación sobre configuración previa con inactiva preservada|905/906/908/909/910|3e2bb5|
|02|s28 update no cambia identidad ajena|1012|25425b|
|03|s28 update no confirma con revisión anterior|996|d0e229|
|04|s11 PUT TASK real, dos valores, URL/If-Match/cuerpo/confirmación|302 NC, nominal multivalor|545343|
|05|s28 sólo primer valor confirmado no es éxito|356/363|39233c|
|06|s28 tipo cambiado aun con valores compatibles no es éxito|375/386|0c8fa6|
|07|s27 BOOLEAN/DATE nulos y longMAX compuesto válidos|185/204/90/91/93|7815da|
|08|s27 doce valores íntegros válidos|265|7c892d|
|09|s34 dos errores indexados íntegros preservados|mapeo400, códigos válidos|0c1a0c|
|10|s34 miembro extra en un error conserva Response completa|1157, integridad every|799ba4|
|11|s34 índice igual a longitud no se considera corregible|317 y guardas de índice|755192|
|12|s37 aborto durante JSON400 descarta validación tardía|1129;1132/1133 NC sólo si mensaje fuese observable|4d0abc|
|13|s27 versión de20 dígitos rechazada|83/84/87/88|56d361|

Los candidatos enlazan firmas originales detalladas en customization_state_clients_residues.json; no se promete que todos cambien estado. Los strings de AbortError pueden permanecer como residuos aunque el caso alcance la rama; no se exige su copy interno.

Formato real4cb904, ESLint5e5e1c y tipos completos a93031 EXIT0. Regresión focal posterior al formato: nuevo archivo13 + originales88 =101/101, tres archivos, EXIT0 8b9014; log customization_clients_refinement_final.log. Git diff de los dos clientes y sus dos pruebas originales vacío al cierre5decf9. No Stryker, Java ni globalfrontend ejecutados por A.

Freeze nuevo test SHA256 5EA020A626F9D447556BF823C0AF43CCE244F3573DF5737AB86DC3ABF9B753DC. Logs/manifest acompañan. Estado UI/aislamiento pertenece a B y sus oráculos no se atribuyen a estos clientes. Quedan residuos de confirmación/errores no ejercitados por este corte; no se amplía por cantidad ni se baja80.