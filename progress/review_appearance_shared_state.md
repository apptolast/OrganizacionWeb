# Revisión independiente del estado compartido de apariencia

Veredicto: **CHANGES_REQUESTED**, un hallazgo P2 observable. Alcance:
cliente HTTP, Provider y frontera SessionGate del manifiesto
appearance_shared_freeze.json. Los ocho hashes coinciden (538aaf).
No se han modificado fuentes/tests ni ejecutado suites o Docker.

## P2 — La navegación pierde la prohibición de escribir tras un PUT incierto

`appearance-state.tsx`, función save, conserva la revisión confirmada pero
no registra incertidumbre tras un error. `appearance.tsx`, AppearanceForm,
mantiene uncertain exclusivamente como estado local inicializado a false.
App desmonta ese formulario al cambiar de ruta, mientras SessionGate
conserva el mismo AppearanceProvider y su snapshot entre rutas.

Flujo concreto: guardar una intención válida recibe503 (o respuesta perdida),
se muestra recuperación, navegar a Historial y regresar a Apariencia.
El formulario nuevo tiene uncertain=false y usa el snapshot anterior;
Guardar puede emitir otro PUT sin un GET válido intermedio. Contradice
@s28: no permitir otra escritura hasta Recargar versión guardada válido.
Descartar un borrador al abandonar ruta (@s25) no acredita la recuperación
de una operación que pudo haberse confirmado.

Oráculo mínimo propuesto a B: recorrido público PUT503, navegación y vuelta,
intento de Guardar sin nueva escritura, recuperación GET manual y sólo
después nueva decisión. También debe cubrir la incertidumbre que termina
mientras el formulario está desmontado mediante la misma frontera de
estado compartido; no se pide una matriz de errores repetida. La decisión
de implementación corresponde al autor; esta revisión no modifica código.

## Comprobaciones satisfactorias del snapshot

- Cliente valida DTO cerrado, defaults/tag coherentes, colores canónicos y
  contraste de ambos temas, timestamp e identidad/revisión BIGINT acotada
  antes de BigInt. PUT captura tres valores independientes del objeto del
  llamador y compara confirmación con la intención canónica.
- Guards después de await y antes de aplicar JSON; apiRequest comprueba
  aborto antes del observador401. Los tests de GET/PUT401 tardío y JSON
  diferido usan las etapas correspondientes, sin oráculo imposible.
- Provider aborta una lectura anterior al guardar y una lectura concurrente
  al confirmar PUT. El oráculo ya existente acredita que GET viejo no
  sustituye la confirmación. La carga inicial compartida no depende de
  navegación; StrictMode y reintento tienen pruebas públicas.
- SessionGate monta el Provider sólo autenticado y lo retira al cerrar,
  fallar o perder acceso. Key por username impide herencia entre propietarios.
  Cleanup retira solicitudes y overrides de tema. No hay localStorage como
  autoridad. La recuperación CSRF conserva el límite existente de sesión.
- Foco: se han leído recuperación y retirada del iniciador, incluyendo el
  oráculo de movimiento deliberado seguido de blur. No se atribuye a esta
  lectura una certificación visual o de contraste de todas las superficies.

## Límites

No se aprueba aún UI final, SCSS, navegación visual, responsive, E2E ni
mutación. Los tests del formulario seguían siendo trabajo de B fuera del
manifiesto compartido; se le ha comunicado el flujo anterior para coordinar
un delta mínimo. La regresión previa558/558 prueba ese corte y sus casos,
no este flujo ausente. No se reabre backend20 ni sus contratos de transacción.

Ponytail full/Caveman lite: conservar el requisito en la frontera que ya
sobrevive a la ruta evita lógica de recuperación duplicada por pantalla.

## Ratificación del delta P2

APPROVED para el estado compartido corregido. Se preserva arriba el dictamen
histórico sobre el snapshot anterior. Nuevo manifiesto
appearance_shared_recovery_freeze.json: los tres hashes coinciden298085.
Provider773B9F96…6164B3C conserva incertidumbre después de errores no
corregibles por campo, bloquea save desde la frontera compartida y sólo
la retira tras GET válido no abortado. Appearance consume ese estado en vez
de reinicializarlo al montar. Error de validación reconocido no se trata
como confirmación incierta; abortos no contaminan una sesión retirada.

Oráculo nuevo público:503, Historial, Back, Guardar bloqueado sin GET/PUT
extra; GET manual actualiza la revisión y sólo entonces permite otroPUT.
Lectura del REDdafc8b/GREEN0f21d1 (37UI) y tipos/lint/formato b655c3 del autor;
no se repitieron suites. La misma ubicación del catch cubre que un error
termine fuera del formulario, como razonamiento de código, no como otro
caso ejecutado. Sin nuevos bloqueantes en el delta. Se mantienen límites
UIvisual/SCSS/E2E/mutación separados.
