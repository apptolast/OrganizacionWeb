# Revisión final del backend de historial

APROBADO para integración y gates globales. Fuente y nuevos oráculos revisados en3b67ce,0c30e2,22d4a1,d86911 y d2b56e; el último cambio es la guardia de JSON literal null. Se conserva el núcleo y HTTP ya revisados. No hay migraciones ni nuevos eventos de historial.

Las consultas aíslan las cinco fuentes por propietario/contexto, usan metadatos durables para comprobar identidad/tipo/revisión de recibos y rechazan detalles corruptos sin entregar una página parcial. Se añaden comprobaciones de aritmética y forma de decisiones mediante los tipos y guardas existentes. La atribución de cierre se conserva sin resolver TZDB de nuevo.

La revisión detectó identidades nulas, recibos internamente coherentes pero ajenos a la fila y formas discriminadas incompletas. Sus correcciones y RED/GREEN están en la bitácora. El último caso demuestra que receipt='null'::jsonb es posible en sesiones: RED6206b1, GREENe97112; la guardia evita que el valor escape de instanceof y termine en una respuesta malformada.

La fecha0001 reveló dos defectos reales corregidos: JSON de timestamptz dependía de la zona de conexión y Timestamp usaba un calendario híbrido. UTC explícito en JSON y OffsetDateTime en lectura/cursor conservan los extremos contractuales. Los oráculos reales de PostgreSQL comprueban extremos, orden, inserciones tardías, aislamiento por propietario de las cinco fuentes y snapshot por petición con escritor coordinado. No se promete snapshot entre páginas.

Freeze FF91C30D5D90A74A9191689BC6A3753FE1DF08C2DD7CA3008DB28866A2507014:14 archivos y seis XML contrastados. Formato y regresión12c2e4 EXIT0:163 pruebas (52 consultas PostgreSQL,4 transacción,3 aplicación,22 wiring,7 configuración y75 HTTP), sin fallos, errores ni omitidos. Los límites de mocks, reinicio y broker se conservan en el mapa; E2E aporta prueba complementaria real. PIT sigue pendiente y no se marca18 terminada.
