# Revisión del soporte Stryker de historial

APROBADO para integración; la campaña espera init/build global. Se revisaron cambios de dispatcher, configuración dedicada/default y pruebas en57ac7e, parser y nodos finales enee0ce6. Cinco hashes del manifiesto AE2503E2FA628369A11867E4F9B3C5407C33892750F7CD5D1942C374C582F589 coinciden.

History API y página se incluyen completos. La revisión2100a6 pidió ampliar las selecciones parciales de App a los dos ConditionalExpression completos:25:12–31:22 y38:10–62:7. El parser confirma esos nodos; sus alternativas heredadas forman parte del alcance necesario para instrumentar las condiciones nuevas. Los otros rangos incluyen reconocimiento de ruta y enlaces completos. Se conservan todos los tests, perTest, ocho workers, umbral80, mutadores e ignorePatterns protegidos, con reportes separados.

El default incorpora18. La regresión encontró que un oráculo de la campaña histórica14 leía líneas del TaskReader actual, desplazadas por el enlace nuevo. Se aprobó conservar el archivo exacto del commit46913a71c1a582357fa8d50053766a50a5793299 y comprobar sus bytes/hash, manteniendo aserciones. No se modifican fuente, configuración ni resultados14; ese ámbito histórico no se presenta como validación18. TaskReader está completo en el default actual.

Resultado final47/47 Node c2e1b5 y formato correcto. No hay todavía instrumentación, conteos ni score de18. El delta sólo añade soporte; no cambia los14 hashes de producto ya revisados.
