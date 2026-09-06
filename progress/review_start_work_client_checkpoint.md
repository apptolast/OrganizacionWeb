# Revisión del cliente14 — POST y activa

**APPROVED checkpoint parcial.** No se encontró un defecto funcional en las operaciones implementadas. No aprueba recuperación por ID/key, UI, privacidad de componentes ni la feature completa.

Leídos cliente, test y bitácora (`c0446c`), más los validadores heredados y `apiRequest` realmente utilizados (`db662c`). El autor registra 17/17 GREEN `2cefd8`, formato, ESLint y TypeScript. Esta revisión no ejecutó suites, código de reproducción, Git ni cambios de producto. Ponytail full y Caveman lite.

## Lógica y precisión

- POST conserva contexto, intención escalar, key, señal, no-store y CSRF del cliente compartido. Sólo acepta 201/200 y exige correspondencia de proyecto, tarea, duración y Location con el recibo validado. El error HTTP se propaga sin convertirlo en éxito; la clasificación y recuperación UI aún no existen en este paquete.
- El DTO exige exactamente siete campos. `integer` exige number seguro entre 1 y1440 antes de convertir a BigInt; UUID y texto comprueban tipos. `instant` verifica formato UTC, calendario, años 0001–9999 y hasta seis decimales. No hay coerción de arrays/objetos a tipos aceptados ni consulta al catálogo histórico.
- La conversión temporal es correcta también antes de 1970: elimina primero la fracción, convierte el segundo UTC completo y **suma** la fracción positiva de ese segundo. Por lectura, `1969-12-31T23:59:59.123456Z` produce `-1000000 + 123456 = -876544` microsegundos; no resta la fracción ni trunca el instante negativo hacia cero. Cruzar el epoch mantiene una diferencia exacta. Este cálculo es análisis del código, no un test ejecutado.
- Los milisegundos de segundos enteros dentro de años 0001–9999 caben en un entero seguro de JavaScript; después la multiplicación y diferencia se realizan en BigInt. No se convierte a number el total de microsegundos. La fracción ausente o corta se rellena a seis dígitos, y una desviación de un microsegundo se rechaza. No se compara con el reloj actual.
- GET active exige 200 y envoltorio cerrado. Sólo null explícito acredita ausencia; una sesión válida de otra tarea se acepta sin compararla con la ruta abierta. Los datos incompatibles o un HTTP503 no se convierten en ausencia.

## Evidencia y límites

Los 17 casos incluyen nominal/CSRF, replay, errores HTTP, campos extra, tipos, identidad/contexto/Location, fin desviado un microsegundo y estados de activa. La bitácora distingue el caso inicialmente GREEN de los RED reales y no atribuye a estos tests límites temporales todavía no ejecutados. Los ejemplos anteriores a 1970 no están cubiertos por esta suite; el algoritmo leído es correcto y no se exige ampliar la matriz en este checkpoint.

El API transporta AbortSignal y reutiliza la protección del observer compartido. Eso no acredita las futuras guardas de generación/ruta tras leer JSON en la UI. ID/key, errores reconocidos de negocio y presentación histórica permanecen pendientes, como declara el autor.

Hashes finales iguales al freeze `2ef490`:

- `work-session-api.ts`: `BD7AE7AA7D266A43DAD2D16B955AACF89DB6A0036135862B0E0162A4F6ED8F05`.
- `work-session-api.test.ts`: `29773D38B9AB3B43DCE876A23C02A9204C1C7F0DFD7054477DFDA25507D04CE6`.
