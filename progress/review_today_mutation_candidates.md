# Revisión independiente de candidatos de mutación Today

**Dictamen acotado:** 40 equivalencias lógicas/contextuales aprobadas por root, 63 identidades sin excluir (incluye NoCoverage225). No es un nuevo score ni un cierre de feature. Root conserva aprobación final de exclusiones; autores refuerzan observables. Rol judge, Ponytail full/Caveman lite. Sólo lectura de código/report y este documento; sin ejecutar tests, mutación ni editar producción.

Base: inventario103 de campaña521, checkpointf568f6e, JSON original SHA2565cc335b97919aaa1bcd3cf4cf956af54ee55db3a02fdb23a060c77508f5c47a3. Se contrastó la fuente embebida preservada, no se reasignan resultados a fuente nueva. Todos los status brutos se mantienen. La corrección posterior de foco no tenía mutante de atributo en el catálogo original; deberá medirse como generación nueva, sin atribuirle un ID histórico.

## Criterios que limitan las equivalencias

- API: includes por identidad y checks posteriores completos hacen redundantes cuatro validaciones iniciales; Set hace redundante el empate de UUID. Normalización upper/lower sólo sobre UUID hex validado conserva igualdad. Son equivalencias de resultado para JSON ordinario, sin getters/proxies ajenos al transporte.
-253/259 son exclusivamente contractuales: Background de today y contrato11 prohíben reservas solapadas; duración positiva y orden por inicio implican fines estrictamente crecientes. isBlock valida cada intervalo pero NO impone no-solape entre items. Payloads solapados pueden distinguir253 y259; además instant admite Z y .0Z, por lo que igualdad de instante no implica igualdad textual. No se afirma equivalencia universal ni se descuenta por simple ausencia de tests. Los empates209/215/219 se excluyen sólo por endurecer rechazo de empates imposibles válidos;211/217/218 permanecen observables por poder aceptar orden descendente manipulado.
- Dots: @s36 exige una sección aria-current y breadcrumb correcto. Cada atributo activo y la expresión breadcrumb son independientes de los18 candidatos. styles.scss94–122 aplica fondo y borde a aria-current, y<=700 oculta nav-dot. Span de dot está vacío y aria-hidden; booleanos renderizados no producen texto React. Retirar/añadir/invertir dots cambia decoración, NO el único enlace aria-current ni el borde/fondo activo. Esto acepta diferencia visual redundante, no afirma píxeles idénticos ni excluye por ser CSS. Si se exige en futuro que dot sea otro indicador normativo, hay que reabrir estos18. No se añaden IDs de aria-hidden ajenos al inventario; incluso un span vacío expuesto no aporta etiqueta ni acción.
- Espacios: se excluyen sólo donde permanece otra frontera visual/semántica (raya, botón separado, dot vacío). Se retienen286/452/App40 donde desaparece la separación entre valores/destinos/texto inline. No se exige el glifo exacto por gusto tipográfico.

## Contraejemplo temporal399/412

No se acepta asumir que el callback se ejecuta siempre después del deadline fraccionario. HTML declara timeout como long; WebIDL convierte a entero antes del algoritmo. Una espera calculada de10,8ms puede entregarse como10ms; con performance.now aún anterior al deadline, quitar rollover explícito deja snapshot viejo mientras la nueva lectura está pendiente.411 sí es redundante porque conserva segundo argumento true y force se recompone. Es un recorrido derivado de API nativa, no acceso artificial a refs. La reproducción se delega al autor, sin ejecutar pruebas aquí.

Fuentes primarias consultadas: [HTML: firma setTimeout y conversión previa](https://html.spec.whatwg.org/multipage/webappapis.html#windoworworkerglobalscope), [WebIDL: conversión a enteros](https://webidl.spec.whatwg.org/#abstract-opdef-converttoint). Estas reglas sustentan la posibilidad temporal; no se afirma una medición de navegador nueva.

## Inventario individual y efecto exigido

E significa equivalencia aceptable sólo dentro de las condiciones anteriores; NE significa conservar en seguimiento/replay, sin afirmar que ya fue Killed. El status original permanece independiente.

| Archivo | ID | Status original | Dictamen y efecto/no efecto |
| --- | --- | --- | --- |
| src/today-api.ts | 225 | NoCoverage | NE: Rechazar UUID repetidos no adyacentes con intervalos distintos y sumas coherentes; alcanzar Set tras guardas anteriores. |
| src/today-api.ts | 78 | Survived | E lógica: includes exige identidad con tres strings; retirar typeof no admite otro tipo. |
| src/today-api.ts | 85 | Survived | E lógica: rama AVAILABILITY vuelve a exigir entero0–1440; fallback exige null. |
| src/today-api.ts | 89 | Survived | E lógica: igualdad estricta con max exige número entero no negativo; fallback exige null. |
| src/today-api.ts | 93 | Survived | E lógica: igualdad estricta con max exige número entero no negativo; fallback exige null. |
| src/today-api.ts | 127 | Survived | NE: Rechazar date array/string coercible; tipo de fecha debe seguir siendo string. |
| src/today-api.ts | 171 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 172 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 173 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 174 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 175 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 176 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 177 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 178 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 181 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 183 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 185 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 192 | Survived | NE: Rechazar fallback con único campo incoherente de zona/preferencia/capacidad; conservar null frente a cero. |
| src/today-api.ts | 209 | Survived | E contractual: > pasa a >= y sólo añade rechazo de inicios iguales; imposible con reservas positivas sin solape. No admite ningún payload antes rechazado. |
| src/today-api.ts | 211 | Survived | NE: No excluir aún: revisar rechazo de payload empatado/orden UUID; dominio válido no tiene empates, sin fabricar positivos con solape. |
| src/today-api.ts | 212 | Survived | NE: Aceptar cronología válida aunque UUID aparezcan en orden inverso; no ordenar por UUID cuando instantes difieren. |
| src/today-api.ts | 213 | Survived | NE: Aceptar cronología válida aunque UUID aparezcan en orden inverso; no ordenar por UUID cuando instantes difieren. |
| src/today-api.ts | 214 | Survived | NE: Aceptar cronología válida aunque UUID aparezcan en orden inverso; no ordenar por UUID cuando instantes difieren. |
| src/today-api.ts | 215 | Survived | E contractual: comparación UUID pasa a true sólo dentro de empate de inicio; rechaza más empates, ninguno válido. No debilita rechazo de JSON manipulado. |
| src/today-api.ts | 216 | Survived | E lógica: igualdad de UUID se rechaza después por Set normalizado aunque >= cambie a >. |
| src/today-api.ts | 217 | Survived | NE: No excluir aún: revisar rechazo de payload empatado/orden UUID; dominio válido no tiene empates, sin fabricar positivos con solape. |
| src/today-api.ts | 218 | Survived | NE: No excluir aún: revisar rechazo de payload empatado/orden UUID; dominio válido no tiene empates, sin fabricar positivos con solape. |
| src/today-api.ts | 219 | Survived | E contractual: UUIDhex upper(rhs)<=lower(rhs); p>=lower implica p>=upper. Sólo endurece rechazo en empates imposibles válidos;218 no comparte este argumento. |
| src/today-api.ts | 222 | Survived | E lógica: upper y lower conservan exactamente las clases de igualdad de UUID hex ya validado. |
| src/today-api.ts | 224 | Survived | NE: Rechazar UUID repetidos no adyacentes con intervalos distintos y sumas coherentes; alcanzar Set tras guardas anteriores. |
| src/today-api.ts | 226 | Survived | NE: Rechazar UUID repetidos no adyacentes con intervalos distintos y sumas coherentes; alcanzar Set tras guardas anteriores. |
| src/today-api.ts | 232 | Survived | NE: Rechazar item que sólo toca frontera y aporta cero segundos, aunque resumen declare cero. |
| src/today-api.ts | 233 | Survived | NE: Rechazar item que sólo toca frontera y aporta cero segundos, aunque resumen declare cero. |
| src/today-api.ts | 253 | Survived | E contractual: intervalos positivos ordenados sin solape tienen fin máximo en el último item. No universal con solapes. |
| src/today-api.ts | 259 | Survived | E contractual: fin empatado imposible entre reservas positivas sin solape. No universal con Z/.0Z y solape. |
| src/App.tsx | 18 | Survived | NE: Ruta desconocida no activa sección Proyectos por startsWith vacío. |
| src/App.tsx | 28 | Survived | NE: Rutas con prefijos/sufijos extra no abren pantallas ni disparan lecturas accidentales. |
| src/App.tsx | 29 | Survived | NE: Rutas con prefijos/sufijos extra no abren pantallas ni disparan lecturas accidentales. |
| src/App.tsx | 32 | Survived | NE: Rutas con prefijos/sufijos extra no abren pantallas ni disparan lecturas accidentales. |
| src/App.tsx | 39 | Survived | NE: Main404 no se adelanta al orden natural de Tab mediante tabindex positivo. |
| src/App.tsx | 40 | Survived | NE: Sin separador, enlaces inline quedan visualmente HoyProyectos; conservar frontera legible entre destinos. |
| src/today.tsx | 286 | Survived | NE: Quitar separador concatena hora formateada y zona (p.ej.12:00UTC); conservar límite explícito legible. |
| src/today.tsx | 291 | Survived | E contextual: antes del primer snapshot no hay timer; éxito reinicia awaitingVisibleSnapshot=false antes de publicar snapshot. |
| src/today.tsx | 292 | Survived | NE: Anunciar carga inicial/refresco y retirar alerta previa antes de confirmación; no falsos estados. |
| src/today.tsx | 309 | Survived | NE: Resultado/finally antiguo no modifica error/loading de generación vigente. |
| src/today.tsx | 315 | Survived | NE: Resultado/finally antiguo no modifica error/loading de generación vigente. |
| src/today.tsx | 329 | Survived | NE: Respetar deadline y reemplazo forzado incluso con lectura pendiente; no retirar demasiado pronto ni conservar agenda vencida. |
| src/today.tsx | 331 | Survived | NE: Respetar deadline y reemplazo forzado incluso con lectura pendiente; no retirar demasiado pronto ni conservar agenda vencida. |
| src/today.tsx | 336 | Survived | E contextual: pending&&!force retorna antes; con pending=false la petición actual ya finalizó, abort extra no cambia datos ni red pendiente. |
| src/today.tsx | 337 | Survived | NE: Respetar deadline y reemplazo forzado incluso con lectura pendiente; no retirar demasiado pronto ni conservar agenda vencida. |
| src/today.tsx | 338 | Survived | E contextual: efecto inicial asigna active antes de listeners/timers; refresh forzado no es alcanzable con active=null. |
| src/today.tsx | 347 | Survived | NE: Anunciar carga inicial/refresco y retirar alerta previa antes de confirmación; no falsos estados. |
| src/today.tsx | 350 | Survived | E lógica: revision es sólo dependencia; +1 y -1 producen un valor nuevo por la misma cantidad de revisiones. |
| src/today.tsx | 351 | Survived | E lógica: array vacío y array de string constante mantienen la misma identidad de callback entre renders. |
| src/today.tsx | 366 | Survived | NE: Desmontaje retira listeners propios; no acumular callbacks ni actividad tras salir. |
| src/today.tsx | 368 | Survived | NE: Desmontaje retira listeners propios; no acumular callbacks ni actividad tras salir. |
| src/today.tsx | 370 | Survived | NE: Desmontaje retira listeners propios; no acumular callbacks ni actividad tras salir. |
| src/today.tsx | 371 | Survived | E contextual: refresh tiene identidad estable durante todo el montaje; quitarlo de dependencias no cambia suscripción. |
| src/today.tsx | 378 | Survived | NE: Respuesta mientras oculto no programa frontera; volver consulta y conserva deadline. |
| src/today.tsx | 380 | Survived | NE: Respuesta mientras oculto no programa frontera; volver consulta y conserva deadline. |
| src/today.tsx | 397 | Survived | NE: Respetar deadline y reemplazo forzado incluso con lectura pendiente; no retirar demasiado pronto ni conservar agenda vencida. |
| src/today.tsx | 399 | Survived | NE: NE temporal: delay fraccionario se convierte a long; callback puede preceder deadline submilisegundo. Sin rollover explícito se conserva agenda hasta resolver lectura nueva. |
| src/today.tsx | 411 | Survived | E lógica: segundo argumento rollover=true hace force ||= rollover verdadero aunque primer argumento sea false. |
| src/today.tsx | 412 | Survived | NE: NE temporal: delay fraccionario se convierte a long; callback puede preceder deadline submilisegundo. Sin rollover explícito se conserva agenda hasta resolver lectura nueva. |
| src/today.tsx | 426 | Survived | NE: Anunciar carga inicial/refresco y retirar alerta previa antes de confirmación; no falsos estados. |
| src/today.tsx | 435 | Survived | E de presentación: botón conserva rol, etiqueta, fondo y padding propios; quitar espacio tras punto no fusiona palabras dentro del texto ni oculta acción. |
| src/today.tsx | 443 | Survived | NE: Snapshot configurado no presenta aviso de fallback/capacidad desconocida. |
| src/today.tsx | 445 | Survived | NE: Snapshot configurado no presenta aviso de fallback/capacidad desconocida. |
| src/today.tsx | 452 | Survived | NE: Eliminar espacio concatena texto del aviso con enlace inline sin margen; conservar separación legible del destino. |
| src/today.tsx | 465 | Survived | NE: Presentar exceso positivo en minutos y desconocido en fallback; no intercambiar null/cero/texto. |
| src/today.tsx | 466 | Survived | NE: Presentar exceso positivo en minutos y desconocido en fallback; no intercambiar null/cero/texto. |
| src/today.tsx | 467 | Survived | NE: Presentar exceso positivo en minutos y desconocido en fallback; no intercambiar null/cero/texto. |
| src/today.tsx | 468 | Survived | NE: Presentar exceso positivo en minutos y desconocido en fallback; no intercambiar null/cero/texto. |
| src/today.tsx | 469 | Survived | NE: Presentar exceso positivo en minutos y desconocido en fallback; no intercambiar null/cero/texto. |
| src/today.tsx | 470 | Survived | NE: Presentar exceso positivo en minutos y desconocido en fallback; no intercambiar null/cero/texto. |
| src/today.tsx | 471 | Survived | NE: Cierre vacío muestra Sin bloques, no sólo ausencia de items. |
| src/today.tsx | 479 | Survived | NE: Etiquetas actual/próximo pertenecen a su item y no a otra reserva. |
| src/today.tsx | 481 | Survived | NE: Etiquetas actual/próximo pertenecen a su item y no a otra reserva. |
| src/today.tsx | 484 | Survived | NE: Etiquetas actual/próximo pertenecen a su item y no a otra reserva. |
| src/today.tsx | 486 | Survived | NE: Etiquetas actual/próximo pertenecen a su item y no a otra reserva. |
| src/today.tsx | 487 | Survived | E de presentación: permanece la raya entre dos fechas; quitar espacio después de ella no une valores ni elimina delimitador. |
| src/today.tsx | 491 | Survived | NE: No duplicar zona original si coincide con la efectiva. |
| src/today.tsx | 493 | Survived | E de presentación: permanece etiqueta Zona original y raya entre instantes; cambia sólo espaciado junto al delimitador. |
| src/workspace.tsx | 507 | Survived | E contractual visual: retira dot de Hoy; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 506 | Survived | E contractual visual: retira dot de Hoy; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 509 | Survived | E contractual visual: siempre añade dot de Hoy; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 508 | Survived | E contractual visual: invierte dot de Hoy; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 511 | Survived | E contractual visual: retira dot de Hoy; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 510 | Survived | E contractual visual: invierte dot de Hoy; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 517 | Survived | E de presentación: espacio posterior a Proyectos precede un span vacío, no otra palabra; flex ignora whitespace aislado y dot usa margin-left:auto. |
| src/workspace.tsx | 518 | Survived | E contractual visual: retira dot de Proyectos; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 519 | Survived | E contractual visual: retira dot de Proyectos; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 520 | Survived | E contractual visual: invierte dot de Proyectos; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 521 | Survived | E contractual visual: siempre añade dot de Proyectos; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 522 | Survived | E contractual visual: invierte dot de Proyectos; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 523 | Survived | E contractual visual: retira dot de Proyectos; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 529 | Survived | E contractual visual: retira dot de Disponibilidad; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 530 | Survived | E contractual visual: retira dot de Disponibilidad; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 531 | Survived | E contractual visual: invierte dot de Disponibilidad; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 532 | Survived | E contractual visual: siempre añade dot de Disponibilidad; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 533 | Survived | E contractual visual: invierte dot de Disponibilidad; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 534 | Survived | E contractual visual: retira dot de Disponibilidad; aria-current, borde/fondo y breadcrumb quedan intactos. Dot vacío/aria-hidden, oculto en móvil. |
| src/workspace.tsx | 536 | Survived | NE: Breadcrumb desconocido conserva Página no encontrada. |

## Aprobación independiente del refuerzo API

Revisión readonly d48a40 de today-api.test.ts y progress/tdd_today_api_mutation.md: **APPROVED** para17 casos nuevos/seis comportamientos. Autor reporta88/88 GREENb85355 y lint/types288a83; este juez no repite suites ni convierte ese resultado en kills. No cambia producción. Se comprobaron fixture de fecha array coercible, diez fallback con defecto aislado, duplicados no adyacentes con sumas/candidatos coherentes, cronología válida/UUID inversos, negativo empatado descendente explícitamente inválido y dos contactos de frontera con cero segundos. No se inventa positivo con solapes. La diferencia219 frente218 es monotónica y quedó ratificada por root antes de seleccionar replay.

Inventario final ratificado por root:40E (11API y29UI/shared),63NE (24API y39UI/shared), statusoriginal intacto. Original418/52180,23% no se ajusta. La región de foco corregida se añade como generación nueva, nunca como una identidad de los103 originales. No queda candidato PENDING en esta revisión; las63NE esperan medición para demostrar qué tests las matan.
