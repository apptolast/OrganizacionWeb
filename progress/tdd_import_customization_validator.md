# TDD — validador de personalización de importación23

Frontera: únicamente ImportCustomizationValidator y su prueba. appearance valida theme/accentLight/accentDark; configuration valida scope, visibleFields y definiciones cerradas; values valida pares cerrados contra definiciones del archivo, incluidas inactivas. A conserva metadatos exteriores, propietario, relaciones y persistencia. Reutiliza AppearanceValues, CustomizationView, CustomFieldLabel y CustomFieldInput, sin modificar JSON de entrada ni aplicar canonizaciones a datos históricos.

| Ciclo | Comportamiento | Evidencia |
| --- | --- | --- |
| 1 | Apariencia válida conserva color minúsculo | RED55c589, GREENa92cc8 |
| 2 | Tipos ausentes y contraste inválido: error seguro de importación | RED874a50; intento cf7424 aún falla porque ValidationException no deriva de IllegalArgumentException; GREEN36b320 |
| 3 | Configuración TASK con definición inactiva y orden | REDf50f63, GREENab6cbe |
| 4 | Scope/visibleFields y forma del array | RED5efbe8, GREENcd9553 |
| 5 | Definiciones cerradas, UUID canónico, etiqueta y tipo | REDb4e61d, GREENb2267e |
| 6 | Máximo12, IDs y etiquetas únicos | REDa473b6, GREEN20a14c |
| 7 | Valores inactivos: Unicode, cero, false, fecha, null y ausentes | RED6c5826, GREEN3d6d17 |
| 8 | Pares cerrados, duplicados y referencias incluso con null | RED448613, GREEN7de24e |
| 9 | Tipos y fracción ultrafina sin redondear | Inicialmente GREENf56f8a |
| 10 | Límites inclusivos12/60/1000/±1e9/fechas y valores vacíos | Inicialmente GREEN3024f7 |
| 11 | NUL, surrogate y excesos Unicode | Inicialmente GREEN08d063 |

Logs originales import_customization_NN_*.log/.exit. Regresión final37/37, cero fallos/errores/skips, SpotlessJavaCheck EXIT0 754bf0; XML y log import_customization_formatted_final*. Primer intento de formato con dos paths separados por coma no aplicó cambios y falló check; preservado. Se corrigió el uso del hook con un path absoluto por invocación, sin ampliar formato a archivos ajenos.

Las campañas PIT anteriores siguen acreditando sus fuentes idénticas; esta clase nueva todavía necesita inclusión y medición en el foco de persistencia final. No hay integración PostgreSQL acreditada por esta prueba pura. El método values no exige conjunto activo completo: importa únicamente pares persistidos, conserva omisiones, null y texto vacío sin canonizar la salida.

## Corrección de representabilidad durable tras revisión

El positivo original de colores minúsculos no acreditaba la restricción SQL V19: únicamente colores hexadecimales en mayúsculas son persistibles. Se conserva el corte37/37 como histórico, con ese límite explícito. La fixture positiva ahora usa mayúsculas y sigue comprobando que la entrada no cambia. Nuevo caso minúscula: RED93dc91, GREENc5b88d. El helper compara el resultado validado de AppearanceValues con ambos colores originales y rechaza discrepancias; no normaliza archivo ni modifica V19.

Regresión refinada38/38 y SpotlessJavaCheck verdes; originals import_customization_refined_final.log/.exit/_xml.xml. La fuente del nuevo helper cambia; todavía no se había ejecutado PIT sobre ella. Reader/decoder/HTTP y sus campañas permanecen intactos.
