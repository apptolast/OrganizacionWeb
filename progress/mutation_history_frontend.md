# Mutación frontend18 — campaña original y prioridades

Campaña única history-frontend EXIT0 c132ca:741mutantes,621Killed,110Survived,7NoCoverage,3RuntimeError,0Timeout. Duración Stryker11m23. Score oficial621/738=84,15% (excluye3errores); cociente estricto621/741=83,81% separado. Por archivo: App21K/2S; API246K/19S; History347K/89S/7NC/3Error (78,33% oficial); los tres enlaces restantes7K/0residuos. No se aplica el score global a cada archivo.

Raw preservado `history_stryker_original.json` SHA21F58FA90232A5C85FB5A78AE889F75FC718FB2739D150684867655E6BC65FF5. Before/after136entradas idénticas, comprobación a3bb43; afterB2AC0E5D3A16D512A2F69A1B9B43034A04B8D05CC6B8BE597ED046C795CE31E4. Inventario `history_stryker_residuals.json`; extracción exacta `history_stryker_classified_residuals.json`; tabla individual `history_stryker_residual_table.md`:120IDs únicos, sin omisiones ni reclasificación del estado raw. Coordenadas del reporte son línea/columna1-based, distintas de las columnas0-based de configuración.

## Refuerzos prioritarios autorizados por root

Máximo12oráculos, uno por ciclo; producto actual correcto y GREEN inicial se registrará honestamente. No implementación ni replay en este dictamen.

1. D1: entrada con campoextra y detalles válidos,76/77. El test de envelopeextra no cubre exact8 de la entrada.
2. D2: cuatro detalles estructuralmente inválidos con id/tiempo/contexto coherentes, uno por TASK_STATUS_CHANGED(124), BLOCK_PLANNED(136/137), BLOCK_CHANGED(150) y SESSION_CHANGED(166/167). Ejemplos focales, no copiar matrices de validadores heredados.
3. D3: cuatro DTO válidos bajo otra familia conocida con contexto/tiempo coherentes,125/138/151/168. No equivalentes: sin discriminación puede aceptarse una unión incoherente y llegar a presentación incorrecta.
4. F1: error local de rango, navegar a otra URL y volver:302NC/298S. Sin limpiar el estado, el error reaparece al volver aunque el formulario fue remontado con valores aplicados válidos.
5. F4: segundo503 y segundo reintento.547 sustituye el contador porundefined: la primera actualización aún cambia0→undefined, la segunda no cambia y deja de consultar.
6. F5: CLOSE con ambas notas vacías,692/696NC; deben aparecer los mensajes de ausencia de anotación, no huecos sin significado.

Los tres RuntimeError438/442/443 provienen del adaptador Vitest/Stryker al serializar excepciones (`Cannot convert object to primitive value`, dos reinicios sin éxito). Mutan acceso a link nulo. Son errores de herramienta sobre esos mutantes, nunca Killed ni prueba positiva de protección. No requieren cambiar producto ni perseguir100%.

## Equivalencias contextualizadas y límites diferidos

- E1 API192/282/283/285/287: el consumidor sólo contrasta el signo mediante >=0 y rechaza por separado duplicados(type,id). Cambiar1→0 para UUID descendente o el valor de comparación de IDs iguales no cambia aceptación observable.253 elimina slice del límite inferior: con instanteUTC canónico y fromfechaYYYY-MM-DD, comparar el instante completo contra la fecha conserva el orden del día, incluida igualdad. No escribir tests espejo para estos seis.
- E2 regex617 pierdeanclafinal pero el ETag ya está validado/cerrado antes de presentar. E3 fallbacks FormData388/391/412 no son alcanzables con los controles nativos presentes. E4 valores alternativos inválidos de inputdate/select461/464/476 se normalizan a vacío/primeraopción en este formulario; no se afirma equivalencia fuera de esta estructura.
- E5 dependencias constantes313, signo del contador548, argumento title de pushState418, refheading montado335, href existente451 y discriminadores650/667/681 redundantes con action validada. Conservan comportamiento en el dominio aceptado; no se descartan del raw.
- Fechas abiertas/igualdía395/396/398, valor Hasta restaurado474/475, limpiar criterio individual416 y URLvacía380/421 son diferenciables, diferidos expresamente por root.403 podría conservar el error al corregir hasta exactamente la misma URL; también queda como límite concreto.
- Categoría planning positiva291, anclajes App0/1, filtroúnico358–361, campo válido aria-invalid/describedby465/469, tabindex424/453, no-retry404540/no-paginación401700, revisión de dosdígitos618, transicionescompletion yRESUME(597/602/605/607/608/658/659/660/747) quedan como huecos observables diferidos.593 y649/653 pueden mostrar etiquetas adicionales inadecuadas a otra familia. No se llaman equivalentes.
- Semántica nativa submit382 y clavesReact566 tienen límites de observación jsdom/reconciliación; no se propone afirmar ausencia de efecto sólo porque sobreviven. La cancelación nativa y navegación se verifican además en E2E sin atribuirle Killed a la campaña unitaria.
- L2 captura de foco/modificadores/cleanup: los defensivos se solapan con RouteLink y el listener que cancela intención cuando se mueve foco.324/326 adelantan el foco a carga pero mantienen el destino al finalizar;311/312 omiten limpieza y pueden retener un listener aunque no alteren estado visible. Se documentan como baja prioridad, no equivalencia universal ni defecto del código actual.
- L1 separadores tipográficos y L3 advertencia/lista vacía son de presentación secundaria. UX original sigue teniendo sus propios oráculos; no se solicita una matriz cosmética para elevar porcentaje.

Hasta esta revisión no se modificaron fuentes ni tests después de la campaña. La autorización de los12refuerzos es posterior y tendrá evidencia/replay separado; el score84,15% y los tres errores permanecen como resultado original.
