# Revisión parcial: HTTP de pausa y reanudación

APPROVED para integrar los dos Java y la bitácora, sin declarar terminada la funcionalidad 15. Root leyó controller y las 48 pruebas en 521886/89d35a/b47e61. Las cinco rutas conservan propiedad en los puertos, orden de validación, recibos históricos y representación decimal exacta. Work-Session-Revision transporta UUID y revisión completos; los problemas locales conservan los títulos de inicio14. Se reutilizan seguridad, errores comunes y validadores de identidad sin crear otra capa.

Root b47e61 verificó independientemente XML: 48 HTTP15 y 49 HTTP14, cero fallos, errores u omitidas. Ambos hashes Java coinciden con el freeze de la bitácora: controller666B108F70F9BFFB1A8ED3EBCBDC9EA014621ADFCE5C4A2DD3E339B64D51DC63 y test7931BDFFE4E4FBE4A0E1941A38D4C00AD1C70431B8E41BBD4C034D4FF8778FC7. El autor preservó XML y log antes del siguiente foco, y acredita dos pasadas de Spotless focal real.

Los oráculos de recibos y snapshots comparan formas completas y tipos de cadenas, token coherente y ausencia de ETag/Location donde corresponde. Los tests HTTP simulan casos de uso: no prueban por sí solos concurrencia, reloj transaccional, rollback o recuperación tras reinicio. Esos comportamientos requieren PG y smoke integrado. No se repite la suite por esta revisión; falta wiring, recorrido real, UI, regresión integrada y mutación.

Excluir de Git los cinco snapshots de dependencias y todos los artefactos build/log. No fusionar la rama entera: sólo seleccionar este paquete y el publicador previamente aprobado.
