# Encargo de carril — feature 28

5 condiciones del veredicto de cierre que NO dependen de ninguna decision del propietario.

## 1. @s35 «POST /sync con onlyIfStale true» no tiene oráculo que pueda fallar (superviviente medido today-external-calendar.tsx:59)

**Estimacion del juez:** 20 min · **Bloqueante:** si

En frontend/src/today-external-calendar.test.tsx, que el doble de fetch guarde también el cuerpo (hoy :57 sólo apila método y url) y que la prueba de orden afirme {onlyIfStale: true} en el POST. Acreditar el rojo cambiando la producción a syncExternalCalendar(false, signal): la prueba tiene que caer.

## 2. C5 (frontend) — falta el veredicto escrito de cada superviviente, y el acta lo confiesa

**Estimacion del juez:** 150 min · **Bloqueante:** si

Sobre la lista de la medida NUEVA, escribir por cada superviviente o la prueba que lo mata o el motivo concreto de equivalencia, en progress/mutacion_external_calendar_frontend.md. Los que ya sé que son hueco real y hay que matar, no justificar: external-calendar.tsx:291 (la etiqueta que teclea el usuario puede no llegar a la petición; su gemelo :311 sí muere), :236 y :237 (ninguna prueba lleva synchronise() a un error distinto del 404, así que el 404 de feature:530 no se distingue de nada), :114/:118/:122 (forget() puede dejar de vaciar la lista, feature:542), today-external-calendar.tsx:105 (el aviso de @s36 fila 3 puede quedarse en blanco; su hermano :104 sí muere) y :27 (hour12, que las dos aserciones de subcadena no fijan). Mirar también por qué los dos mutantes de :76 dan RuntimeError.

## 3. C5 (backend) — no existe acta de mutación de backend, ni siquiera el fichero

**Estimacion del juez:** 60 min · **Bloqueante:** si

Escribir progress/mutacion_external_calendar_backend_medida.md con la cifra recomputada del XML (no copiada del HTML), la tabla por capa y la lista NOMINAL de supervivientes con su veredicto uno a uno, al modo de mutacion_webhooks_backend_medida.md. Hoy ls progress/ no devuelve ningún fichero de backend para esta feature.

## 4. B5 — la mitad literal de C1(a): la rama de la cabecera Host restringida es inalcanzable en los dos JVM de prueba

**Estimacion del juez:** 45 min · **Bloqueante:** no

Añadir a backend/build.gradle.kts la tarea forkeada unrestrictedHostTest SIN -Djdk.httpclient.allowRestrictedHeaders (el snippet ya está escrito en progress/bloqueantes_external_calendar.md:454-472; hoy grep de «unrestrictedHostTest» y de «registering(Test::class)» sale vacío) y su clase de prueba, y ver morir el mutante de HttpCalendarFeed.java:148. No bloqueante: la mitad TLS de C1(a) está cumplida (HttpCalendarFeedTest.java:577, :600 y :615) y el otro disparador del mismo catch ya tiene oráculo en :501.

## 5. Reserva por si adapter.feed no llega al 80 % en la campaña que está corriendo

**Estimacion del juez:** 35 min · **Bloqueante:** no

Sólo si el XML lo pide: cerrar los dos mutantes de HttpCalendarFeed.java:159 (negated conditional y removed call to Thread::interrupt) con una prueba que interrumpa el hilo durante un fetch y afirme que la marca de interrupción sobrevive. La propia bitácora lo declara cerrable y lo presupuesta en 30-40 min.

