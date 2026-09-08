# Revisión independiente de cuatro refuerzos UI21

APPROVED con alcance acotado. Lectura completa de los cuatro archivos por A; ningún P1/P2 concreto detectado. No se ejecutaron pruebas/campañas ni se editaron producto/tests. La aprobación valora sus oráculos y fixtures, no anticipa kills ni sustituye los gates de sus autores.

|Archivo frontend/src|Casos leídos|SHA256|
|---|---:|---|
|customization.mutation.test.tsx|3|BDE5591040C6D1D07108E48DC84B588FE1922E5908DCA4282115F9211607AD9D|
|customization-state.mutation.test.tsx|3|04DC4586032BFE1254DB09E4EDD22FEA85ACC469D3122F1EB93608E11547DCA1|
|custom-fields.mutation.test.tsx|11|8C6A7116FB5B434F8EA0373EBA5CAFEAB3246400354134494DC0A552B77D0651|
|customization-metadata.mutation.test.tsx|2|D1DDBEB657C727BD44B137974689ABD8741D3A5E4D12C53674B06C18E248F399|

Evidencia de lectura7e2e93/512fb1. Total19 casos, sin equipararlos a escenarios completos.

Los formularios usan componentes y hook reales; sólo sustituyen fetch. La creación confirma etiquetaUnicode de60 puntos, normalización exterior y renombrado propio sin falsa colisión. Reordenar/desmarcar verifica orden visible, controles de extremo y cuerpo transmitido. Cancelar definición NUMBER inactiva comprueba que crear vuelve a TEXT y que la anterior conserva su activación. No hay aserciones de getters ni llamadas a setters internos.

Estado acredita recuperación seguida de un nuevo PUT confirmado, no sólo aparición del botón: PROJECT recupera mientras TASK conserva incertidumbre, y dos tareas mantienen borradores independientes sin solicitudes a la otra. El esquema UUID ajeno tiene versión superior, por lo que el rechazo no puede explicarse simplemente por una versión anterior; se observa ausencia de datos/edición y recuperación disponible. Los contadores y cuerpos limitan falsos positivos por respuestas genéricas. El primer mock discrimina ámbito por URL parcial: adecuado para aislamiento de estos dos consumidores; no acredita sintaxis del endpoint y no se le atribuye esa cobertura. El cliente nuevo A comprueba la URL TASK exacta separadamente.

Valores cubren límites válidos deUnicode y NUMBER, false real, distinción503 frente a retirada por acceso, callback actualizado durante solicitud, foco voluntario y mapeo índice10 al undécimo control. La prueba de error indexado comprueba borradores de los12 controles, aria-invalid y descripción accesible: no se conforma con cualquier alert. Las respuestas de escritura usan mismo ID y avance de versión, y una fecha igual es válida para tiempo no decreciente. El caso de foco que hace blur del otro botón verifica específicamente no restaurar foco al volver body tras una decisión voluntaria; no acredita por sí solo geometría ni comportamiento de todos los navegadores.

Metadatos comprueba etiquetas y orden con dos instantes diferentes, tanto atributos datetime como textoUTC. Por eso no pasa si ambas ramas imprimen el mismo dato o si se invierte la presentación. Son pruebas de render con entornoIntl instalado, no auditoría visual o de todos los locales.

Límites: no autorizaciónHTTP/PG, E2E ni accesibilidad física global acreditados por estos archivos. Los fallos de mutación del runner siguen siendo errores; el inventario original no se reclasifica. No se requiere ampliar una matriz por estas observaciones.