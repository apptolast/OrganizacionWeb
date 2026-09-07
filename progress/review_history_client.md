# Revisión del cliente de historial

Dictamen: **APPROVED para el cliente**, sin cerrar la funcionalidad 18 ni acreditar todavía el montaje de interfaz.

Root revisó las fuentes nuevas, las 53 pruebas y las tres exportaciones compartidas (f06285). Comprobó cinco hashes del manifiesto y el log focal: 528 pruebas verdes en seis suites (55289b). El autor acredita además ESLint, Prettier y TypeScript con EXIT0 fbda7f. Las otras 475 pruebas son regresiones existentes, no nuevos casos del historial.

El cliente reutiliza los validadores instalados de los cinco tipos de detalles y añade las invariantes propias de la colección: forma cerrada, identificadores canónicos, fechas reales en el rango acordado, correspondencia exterior/detalle, máximo de veinte filas, filtros, orden exacto y unicidad compuesta. El orden usa microsegundos BigInt y UUID canónico; no confunde revisión local con orden global. Captura una copia de la consulta antes de esperar, evitando validar una respuesta contra un borrador posteriormente modificado. Las tres funciones compartidas sólo añaden export; sus cuerpos permanecen idénticos.

Se contrastaron casos positivos de veinte filas, empates entre familias, mismo UUID entre tablas, filtros inclusivos y CLOSE con duración superior al entero seguro de Number. Las notas y la atribución histórica se preservan sin recalcular TZDB. El cursor sigue siendo opaco en cliente; su sintaxis y vinculación pertenecen al servidor.

La cancelación se propaga mediante signal y apiRequest ya protege el observador global de acceso. Sigue pendiente demostrar en la página cada espera, reemplazo de consulta y retirada de datos privados, además de foco, errores, URL, E2E y UX. No se inicia mutación sobre fuentes de integración todavía móviles. La campaña final incluirá el módulo nuevo completo y los cambios reales del montaje, según el plan aprobado.
