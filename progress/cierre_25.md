# Veredicto de cierre — feature 25-webhooks (features/webhooks.feature)

**NO APROBADA** — 13 condiciones, 9 bloqueantes. 10 de septiembre de 2026.

Dos verificadores independientes (cierres declarados / lo que sigue bloqueando) más
síntesis. Sin ejecutar nada: la máquina estaba midiendo.

## Resumen

NO aprobada. Comprobé a mano cada cita; los dos verificadores coinciden en lo esencial y casi todo se sostiene, pero corregí y descarté varias cosas.

DESCARTADO / CORREGIDO (4 puntos):
1) El mecanismo de la rotura de SECRET_UNREADABLE que describe el verificador 1 es FALSO tal como está escrito: webhooks.tsx:118-123 (setLoadFailed) cuelga de listWebhooks, la lista de ENDPOINTS, no de las entregas. El daño real es más estrecho pero igual de permanente: listWebhookDeliveries se invoca en webhooks.tsx:280 dentro de act(...,"No se han podido cargar las entregas."), así que el propietario sigue viendo sus endpoints y lo que pierde para siempre es el panel de entregas del endpoint envenenado. El defecto SIGUE EN PIE (errorClasses en webhooks-client.ts:22-30 es un catálogo cerrado de siete y decodeDelivery lanza en :138-139), pero el efecto no es "no vuelve a ver NINGUNA entrega" de toda la cuenta.
2) Descartado el trabajo de "su etiqueta en español en la tabla": no hay mapa de etiquetas de errorClass; webhooks.tsx:554 imprime {row.errorClass ?? "—"} en crudo. No hay nada que traducir.
3) Descartado como condición separada el "C1 no puede seguir marcado [x]" del verificador 2: es la misma condición que la enmienda de @s9, no una segunda.
4) Rebajado el hallazgo del SHA: la cifra 92,81 % SÍ describe el árbol de cierre (git diff --stat 95cf64bf HEAD -- backend/ sale vacío y af581554..HEAD sólo toca progress/), así que es un error de atribución en una línea, no una medida caducada.

CONFIRMADO CON LUPA, lo que bloquea:
- Puerta de mutación de frontend sin medir. Recontado del propio mutation.json: 429 Killed + 3 Timeout + 151 Survived + 22 NoCoverage = 605; 432/605 = 71,40 %, contra break 80 de stryker.webhooks.config.json y 0,80 de harness.config.json. Y ese informe es de las 11:13, anterior a 11e744d8 (14:07) que cambió webhooks.tsx. El ~90 % está honestamente rotulado como previsión (mutacion_webhooks_frontend.md:906-926), y como previsión no cierra nada.
- Producción por delante del contrato: SECRET_UNREADABLE existe en WebhookAttempt.java:16, V31 y DispatchWebhooks.java:46, el API lo sirve sin traducir (WebhookDeliveryView.java:29) y @s25 (features/webhooks.feature:330-338) más project-spec.md:2038 cierran el conjunto en siete. Encima el contrato ejecutable del cliente PROHÍBE expresamente el octavo (webhooks-client.test.ts:500). Ninguna prueba cruza la frontera: la CI seguirá verde con el producto roto.
- @s9 (líneas 129-142) promete "la aplicación queda disponible" para "base64 de 31 bytes" y "texto no base64"; AesGcmWebhookSecrets.versioned lanza IllegalStateException en los dos casos (:64-72). Dos filas de un Examples describen algo que el código no hace.
- @s32: "transcurren 1500 ms desde el arranque" con @Scheduled(fixedDelay=1000, initialDelay=1000) (WebhookSchedule.java:27) da UN tic; la fila 3 exige dos ciclos. La prueba nueva de reflexión confirma la contradicción, no la resuelve.
- @s8:125-126 dice "el id del endpoint como dato adicional autenticado"; associatedData es ownerId + "|" + endpointId (AesGcmWebhookSecrets.java:135-136). La política está ratificada en project-spec.md (nota B5 y B6), el .feature nunca recibió su nota fechada.
- Segunda mitad de B7 sin hacer, verificada: judge_webhooks_cierre.md sigue sin tocar desde f969d608 y afirma en :86-87 y en :233-241 que la de @s25 es "la ÚNICA cláusula del contrato que el código incumple" (hoy son al menos cuatro) y cita la línea 320 con un texto que ya no dice y JdkWebhookSender:123/209-211 cuando elapsedMillis se usa en :161-168.
- Los dos ficheros que el dictamen exige POR NOMBRE no existen: ls progress/ da mutation_webhooks.md, mutacion_webhooks_backend_medida.md y mutacion_webhooks_frontend.md.

CONFIRMADO Y BIEN CERRADO, no lo toco: B2 (92,81 % recomputable), B3, B8, el arreglo de B10 en el adaptador (el descifrado salió del RowMapper y el fallo pasa por WebhookAudit, no por el LOG.warn mudo), la migración V31, el ámbito PIT y la CI en verde sobre 9d1e7d2e sin deriva de código.

NADA de esto lo he ejecutado: la máquina está ocupada. Las comprobaciones que faltan van declaradas como condiciones de campaña con su comando exacto.

---

## Condiciones

| # | Condición | Bloq. | De quién | Min. |
|---|---|---|---|---|
| 1 | La puerta de mutación de frontend sigue sin medir y el único dato medido está en rojo (432/605 = 71,40 % contra break 80), sobre un árbol anterior a 1 | **SÍ** | campana | 40 |
| 2 | Producción escribe una octava clase de error, SECRET_UNREADABLE, que el contrato no tiene: @s25 (features/webhooks.feature:330-338) y project-spec.md: | **SÍ** | propietario | 20 |
| 3 | El decodificador del frontend rechaza SECRET_UNREADABLE y deja ciego el panel de entregas del endpoint afectado, para siempre y sin decir por qué; ade | **SÍ** | carril | 45 |
| 4 | B6: dos filas de @s9 («base64 de 31 bytes» y «texto no base64») prometen aplicación disponible con 503 CONNECTORS_DISABLED, y AesGcmWebhookSecrets.ver | **SÍ** | propietario | 30 |
| 5 | B9: el plazo de @s32 («transcurren 1500 ms») no cabe en @Scheduled(initialDelay=1000, fixedDelay=1000), que a los 1500 ms sólo ha corrido un ciclo, y  | **SÍ** | propietario | 25 |
| 6 | @s8:125-126 sigue diciendo que el dato adicional autenticado es «el id del endpoint» cuando producción usa ownerId + "|" + endpointId; leída al pie de | **SÍ** | propietario | 15 |
| 7 | La segunda mitad de B7 no se hizo: progress/judge_webhooks_cierre.md sigue intacto desde f969d608 y sostiene el cierre sobre tres afirmaciones falsas  | **SÍ** | orquestador | 15 |
| 8 | Los dos ficheros que el punto 1 de «Cambios requeridos» del dictamen exige por nombre no existen: no hay progress/mutation_webhooks_backend.md ni prog | **SÍ** | orquestador | 25 |
| 9 | El árbol acreditado en verde (bin/harness init, 95/95 guardas sobre 9d1e7d2e) deja de cubrir el cierre en cuanto se toque el .feature, el cliente de f | **SÍ** | campana | 60 |
| 10 | Decisión del juez sobre las ocho guardas if (!aborted) de webhooks.tsx, medidas dos veces con 0 muertos de 8 y declaradas equivalentes por el carril | no | orquestador | 10 |
| 11 | El SHA publicado de la campaña de backend no es auditable: mutations.xml se escribió a las 15:18:02 y el commit 95cf64bf que declara es de las 15:19:1 | no | orquestador | 10 |
| 12 | Slf4jWebhookAuditTest.s35_evenAPoisonedCodeCannotPutAUrlOrASecretInTheTrail lleva un javadoc que afirma entregar al sujeto los valores prohibidos, y e | no | carril | 15 |
| 13 | Las mitades «no se resuelve DNS» (@s2:26) y «no se abre conexión saliente» (@s5:80) no tienen oráculo: el resolutor de CreateWebhookTest es un método  | no | carril | 25 |

## El trabajo, una por una

### 1. La puerta de mutación de frontend sigue sin medir y el único dato medido está en rojo (432/605 = 71,40 % contra break 80), sobre un árbol anterior a 11e744d8

- **Bloqueante:** sí · **De:** campana · **Estimación:** 40 min

Lanzar la campaña sobre HEAD limpio y publicar la cifra RECOMPUTADA del mutation.json (contando status Killed+Timeout sobre el total), nunca leída del HTML: node scripts/project.mjs mutate webhooks-frontend. Hacerlo DESPUÉS de las condiciones de código, para no medir un árbol que va a cambiar.

### 2. Producción escribe una octava clase de error, SECRET_UNREADABLE, que el contrato no tiene: @s25 (features/webhooks.feature:330-338) y project-spec.md:2038 cierran el conjunto en siete

- **Bloqueante:** sí · **De:** propietario · **Estimación:** 20 min

Decisión del propietario sobre progress/decisiones_pendientes.md punto 9: o contrafirma la ampliación del catálogo a ocho —y entonces se escribe la nota fechada en @s25 al modo de la de @s33 y de la de latencyMs, más la línea de project-spec.md:2038 y una entrada en progress/ratificaciones.md—, o manda quitar la clase y liquidar el secreto ilegible con una de las siete.

### 3. El decodificador del frontend rechaza SECRET_UNREADABLE y deja ciego el panel de entregas del endpoint afectado, para siempre y sin decir por qué; además webhooks-client.test.ts:500 fija el catálogo viejo, así que la CI defiende la rotura

- **Bloqueante:** sí · **De:** carril · **Estimación:** 45 min

Rojo primero en frontend/src/webhooks-client.test.ts: una entrega con errorClass SECRET_UNREADABLE que hoy lanza «Confirmación incompatible» y debe decodificarse. Añadir la octava clase a errorClasses (webhooks-client.ts:22-30) y reescribir la prueba de :500 para que siga prohibiendo un valor inventado sin prohibir el que el backend ya escribe. Sólo tras la contrafirma del propietario. Verificación pendiente de ejecución: pnpm --dir frontend exec vitest run src/webhooks-client.

### 4. B6: dos filas de @s9 («base64 de 31 bytes» y «texto no base64») prometen aplicación disponible con 503 CONNECTORS_DISABLED, y AesGcmWebhookSecrets.versioned lanza IllegalStateException, así que el contexto no arranca

- **Bloqueante:** sí · **De:** propietario · **Estimación:** 30 min

Resolver progress/decisiones_pendientes.md punto 7. Si vale la política ya ratificada (fallo rápido con clave malformada, degradado sólo si está ausente), reescribir las dos filas del Examples con su nota fechada y su entrada en ratificaciones.md; si el propietario quiere el degradado que el contrato promete, es cambio de producción con su rojo primero (unos 60 min de carril adicionales).

### 5. B9: el plazo de @s32 («transcurren 1500 ms») no cabe en @Scheduled(initialDelay=1000, fixedDelay=1000), que a los 1500 ms sólo ha corrido un ciclo, y la fila 3 exige dos

- **Bloqueante:** sí · **De:** propietario · **Estimación:** 25 min

Resolver progress/decisiones_pendientes.md punto 8: o el When pasa a un plazo que cubra dos tics (p. ej. 2500 ms) con nota fechada, o se cambia el cableado. La prueba de reflexión WebhookScheduleTest.s32_b9_* ya sujeta initialDelay, fixedDelay y unidad, así que cualquiera de los dos caminos tiene rojo inmediato.

### 6. @s8:125-126 sigue diciendo que el dato adicional autenticado es «el id del endpoint» cuando producción usa ownerId + "|" + endpointId; leída al pie de la letra, la línea del contrato es falsa

- **Bloqueante:** sí · **De:** propietario · **Estimación:** 15 min

Escribir en features/webhooks.feature la nota de enmienda fechada que reconcilia @s8 con la política ya ratificada en project-spec.md (nota «B5 y B6»: los datos asociados atan propietario además de recurso), contrafirmada, con entrada en progress/ratificaciones.md. El oráculo ya existe y es bueno: AesGcmWebhookSecretsTest.s8_b6_theAssociatedDataBindsBothTheOwnerAndTheEndpoint.

### 7. La segunda mitad de B7 no se hizo: progress/judge_webhooks_cierre.md sigue intacto desde f969d608 y sostiene el cierre sobre tres afirmaciones falsas del árbol de hoy

- **Bloqueante:** sí · **De:** orquestador · **Estimación:** 15 min

Corregir en progress/judge_webhooks_cierre.md §6.3 (:233-241) y en :86-87: quitar «es la ÚNICA cláusula del contrato que el código incumple», actualizar la cita de features/webhooks.feature:320 («cronómetro monótono inyectado»), y sustituir JdkWebhookSender.java:123 y :209-211 por las líneas reales del uso de elapsedMillis (:161-168 y su definición). Anotar allí mismo la enmienda R4 que ya cerró la cláusula.

### 8. Los dos ficheros que el punto 1 de «Cambios requeridos» del dictamen exige por nombre no existen: no hay progress/mutation_webhooks_backend.md ni progress/mutation_webhooks_frontend.md

- **Bloqueante:** sí · **De:** orquestador · **Estimación:** 25 min

Publicar las dos bitácoras con esos nombres exactos: la de backend con el 92,81 % recomputado del mutations.xml y su tabla de 31 sin matar; la de frontend con la cifra que devuelva la campaña, no con la previsión. Consolidar o referenciar desde ellas mutation_webhooks.md, mutacion_webhooks_backend_medida.md y mutacion_webhooks_frontend.md para que no queden dos verdades.

### 9. El árbol acreditado en verde (bin/harness init, 95/95 guardas sobre 9d1e7d2e) deja de cubrir el cierre en cuanto se toque el .feature, el cliente de frontend o los tests

- **Bloqueante:** sí · **De:** campana · **Estimación:** 60 min

Tras aplicar las condiciones de contrato y código, reejecutar el arnés completo y la suite de webhooks: bin\harness.ps1 verify, y backend/gradlew test --tests "com.apptolast.organization.*Webhook*" --tests "com.apptolast.organization.adapter.logging.Slf4jWebhookAuditTest" para reconfirmar el «BUILD SUCCESSFUL» por clase de la sección 9 de progress/mutation_webhooks.md, que hoy sólo está leído, no ejecutado.

### 10. Decisión del juez sobre las ocho guardas if (!aborted) de webhooks.tsx, medidas dos veces con 0 muertos de 8 y declaradas equivalentes por el carril

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 10 min

Fallo: LAS ACEPTO como equivalentes —abortar y desmontar son el mismo suceso en esta vista y React descarta el setState sobre un componente desmontado; el mecanismo de @s38 sí tiene oráculo— pero SÓLO si la campaña real cierra el 80 % sin ellas. Registrar el fallo en la bitácora de frontend y, si la cifra medida cae por debajo del umbral y esos ocho son el margen, queda revocado y hay que hacer el aborto observable sin desmontar. Filtrar del mutation.json las líneas 119,122,125,151,158,161,208,221,234,246,253 de src/webhooks.tsx para contrastarlo.

### 11. El SHA publicado de la campaña de backend no es auditable: mutations.xml se escribió a las 15:18:02 y el commit 95cf64bf que declara es de las 15:19:10, posterior al informe

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 10 min

Corregir la línea 5 de progress/mutacion_webhooks_backend_medida.md para nombrar el árbol realmente medido (af581554 o anterior) y añadir la comprobación que salva la cifra: git diff --stat 95cf64bf HEAD -- backend/ vacío y af581554..HEAD sólo toca progress/. La cifra no cambia; lo que se arregla es que se pueda recomputar.

### 12. Slf4jWebhookAuditTest.s35_evenAPoisonedCodeCannotPutAUrlOrASecretInTheTrail lleva un javadoc que afirma entregar al sujeto los valores prohibidos, y el cuerpo pasa HTTP_ERROR, UNSUPPORTED_EVENT y CONFIGURATION_ERROR: los seis assertFalse no pueden fallar

- **Bloqueante:** no · **De:** carril · **Estimación:** 15 min

Envenenar de verdad los dos parámetros de texto libre del puerto (pasar como código de error y como estado cadenas que contengan https://…?token=abc, whsec_…, v1=… y example.com) para que los assertFalse muerdan; o, si se prefiere no meter basura, borrar el javadoc que afirma lo que no hace. No bloquea porque @s35 sí queda sujeta por las igualdades exactas de formato y por WebhookScheduleTest.s35_neither…, que engancha un appender a ROOT y hace pasar la URL, el secreto y el cuerpo por el sujeto.

### 13. Las mitades «no se resuelve DNS» (@s2:26) y «no se abre conexión saliente» (@s5:80) no tienen oráculo: el resolutor de CreateWebhookTest es un método estático sin contador (:22-30) y la de @s14:198 es estructural porque ManageWebhook no recibe WebhookSender

- **Bloqueante:** no · **De:** carril · **Estimación:** 25 min

Dar contador al resolutor de CreateWebhookTest para que @s2 y @s5 afirmen cero resoluciones cuando la intención se rechaza antes de resolver. Para @s14 y la mitad de «conexión saliente» de @s5, dejar escrita la garantía estructural en progress/literales_sin_oraculo_webhooks.md nombrando el colaborador que no existe en el caso de uso, en vez de dejarla implícita.

