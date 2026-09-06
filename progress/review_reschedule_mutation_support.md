# Revisión del soporte integrado de PIT

APROBADO para ejecutar la campaña. No es un resultado de mutación.

Root revisó el diff95b173: el dispatcher mantiene frontend y scopes anteriores,
acepta sólo el target cerrado reschedule-backend y ejecuta exclusivamente PIT
con mutationScope=reschedule. El test de dispatch fue RED antes de implementar;
los negativos inicialmente verdes se registran por separado. Los22 tests de
scripts pasan.

El build conserva threads4, umbral80, mutadores, timeout y exclusiones previas.
Incluye caminos compartidos afectados de creación, consulta y publicación,
sin filtrar tests difíciles. Inventario independiente e181b3:23 patrones,
54 fuentes, cero hashes distintos. Las63 clases compiladas incluyen anidadas;
ese número no equivale al número de mutantes.

Init completo EXIT0 del autor91757f. Root09ceee comprobó independientemente
los67 XML:1617 tests, cero fallos, errores y omitidos. Frontend1498 y scripts22
verdes en ese mismo init. El paquete UX añadido después sólo contiene E2E y
documentos; se verifica en la suite de navegador, no modifica el corte Java.

La CI por push no ejecuta mutación. Sí existe el workflow periódico/manual
harness-mutation.yml, que llama harness verify e incluye la mutación de forma
indirecta; no se afirma que haya terminado una campaña remota sobre este corte.

Se autoriza campaña local con fuentes y tests congelados. Registrar estados
de mutantes, errores y timeouts por separado y conservar el informe original.
