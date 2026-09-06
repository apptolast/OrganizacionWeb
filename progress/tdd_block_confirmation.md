# TDD confirmación de bloque

Sólo block-confirmation.tsx/test y esta bitácora. Firma acordada con autor TaskBlocks: BlockConfirmation({block,change?,onAccessFailure}), sin callback de estado porque cada acción consulta de nuevo al abrir. Se conservan «Bloque guardado»11 y «Cambio confirmado (hecho histórico)»13. API y detalles compartidos inmutables. Ponytail full/Caveman lite; focales propios únicamente.

## Ciclo1 — @s36, creación histórica y lectura automática

RED5352d9 import ausente; componente mantiene creación durante GETstate y representa vigencia separada sólo al recibir respuesta válida. GREENf6fe7a:1test, cliente/fetch real, sin POST ni lectura de listado.

## Ciclo2 — recibos históricos
RED f4b0f1 por recibo ausente. Corregido fixture de cancelación terminal (no movimiento posterior); RED ad1864 confirmado con fixture válido. GREEN 932a25:3 casos. Antes/después y hecho histórico permanecen separados del estado cancelado consultado.

## Ciclo3 — consulta fallida y reintento
RED 8b37f9:503 no gestionado, falta alert y rechazo no capturado. Se conserva hecho histórico y se ofrece reintento manual; respuesta posterior representa vigencia.
GREEN ciclo3 e2a67a:4/4.

## Ciclo4 — acceso vigente
RED2f448e callbacks ausentes; implementación usa readChangeError y guardas antes/después. Revisión de fixture detectó type about:blank incompatible con API:77c321 sólo404 seguía rojo por ese fixture. Corregido a URN contractual, GREEN494b14:2casos. No se atribuye el fallo de fixture a producción.

## Ciclo5 — recibo nuevo invalida vigencia anterior
RED051ea6 mostraba Planificado de creación tras recibir cancelación. Remontaje por contenido histórico/contexto retira estado anterior y consulta nueva vigencia.
GREEN ciclo5 30b69a:7/7.

## Ciclo6 — foco del reintento
RED470ca7: BODY al retirar botón durante GET pendiente. Foco al heading sólo tras interacción y si BODY sigue activo; no roba control externo. GREENe5f4cd.

## Ciclo7 — respuestas obsoletas
Dos oráculos de éxito/401 tras recibo nuevo inicialmente GREEN f000d2; abort previo impide observer compartido y estado anterior. Sin producción adicional.

## Ciclo8 — clasificación demorada
ReadableStream real de404: desmontar antes de completar JSON; guardas conservan callback sin llamadas. Inicialmente GREEN f334bc, sin producción adicional.

## Ciclo9 — contexto y foco externo
Proyecto/tarea/bloque nuevos retiran vigencia anterior, abortan y forman ruta nueva; no roban foco externo. Tres casos inicialmente GREEN 8e330a.

## Ciclo10 — anuncio accesible
Petición de revisión raíz: preservar role=status del hecho confirmado11/13. RED49dd3d:2casos sin role; mínimo role=status en ambos textos históricos, sin duplicarlo en detalles. Oráculos de carga ahora seleccionan su texto para distinguir los dos anuncios. GREEN d09716:16/16.

## Entrega
Formato focal03cbbd; diff-check ab1683. Mapa: @s36 hecho histórico/estado separado/fallo/reintento/anuncio; @s38 contextos, cancelación, guardas y acceso; @s40 foco local sin robo. Cliente real sobre fetch controlado, sin escrituras ni listas sintetizadas. Integración TaskBlocks y UX real pertenecen al autor principal y al juez; no se ejecutaron suites globales, tsc global ni mutación.
ESLint focal 7d610b EXIT0. Freeze source SHA256 6A882F6A46FF3B718D30676BA39E0838C820806C0A8C14664E94A876DCC8F73E; test C69C5C8C24DADECCD55EF1372D6C29E0FCF260945362905D029157C45694856F (d1274d).
