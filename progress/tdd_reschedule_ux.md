# UX13 — geometría y teclado del recorrido principal

Corte aislado de Java179 COPY2bd776, registrado por root35d5c92; no fusionar snapshot. Recuperación/helper congelados en4818f7f. Ponytail full/Caveman lite. Único test nuevo e2e/reschedule-ux.spec.mjs para @s40, sin producción ni datos matriciales adicionales.

Patrón Today reutilizado localmente:31anchos320–2560 y lados de breakpoints, altura400 a768. Estados mover/revisión/cancelar/historial. Objetivo Unicode con texto largo, endpoints reales de creación/movimiento/cancelación. Se miden overflow horizontal, límites de controles,44×44, intersección entre controles y bordes efectivos de campos. Capturas320/1440 y axe WCAG2/2.1/2.2AA por estado; acciones activadas con teclado y foco de revisión/cierre comprobados.

Primera ejecución1555/6269f7 pendiente. Evidencia parcial se escribe por medición en `.e2e-work/reschedule-real/chromium/ux/geometry.json`, capturas antes de comprobar para conservar diagnóstico RED. No se atribuye PASS de Hoy, ni nativezoom, texto200, otros motores, dispositivos físicos o los30principios completos.

## Primer resultado y alcance

Sesión1555 EXIT0e1c698, **1/1 inicialmente GREEN**, log3fe314:15,1s recorrido/16,6s total. Chromium real contra API/PostgreSQL del snapshot.124 medidas (4estados×31anchos), cuatro alturas400 y4axe sin infracciones. Ocho capturas320/1440 en `.e2e-work/reschedule-real/chromium/ux/`; geometríaJSON conserva cada control/rectángulo/borde. Activación Enter desde acciones enfocadas, foco conservado en Revisar movimiento y trasladado al encabezado al confirmar cancelación. No se añadió ni corrigió producción.

Prettier focal write/check y nodecheck3fe314 GREEN. Test SHA256 BA3F51CD41D979A1DD3D25B9785A15FD670553860648EB5B3DDBB6BA74D00595; log1B9970BDB418BDDB023A3C5F511EF088923884BAE39996DA28EEE8EB686EC646; geometryDB989831C8C8B81DDE55A0B4F0CBA8F303417524811FB165DAEEBF633D5E6635. Stack68476 retirado por runner. Nominal/recuperación congelados no se editaron.

Evidencia por criterio de esta pista: U3/U10/U16/U21/U22 capturas para juicio humano; U13 geometría44×44 y no solapes; U15 activación Enter/foco observado; U20 nombres y bordes de campos/axe; U19 textoUnicode en un fixture; U24 antes/después/historial con recibos reales. Son evidencias parciales, no certificación de heurísticas. Matriz30 en tdd_reschedule_e2e.md sigue necesitando revisión porfila y recorridos de espera/error, feedback, nativezoom/text200 y motores posteriores. No demuestra navegación Tab exhaustiva, lectores de pantalla ni dispositivos físicos.

## Texto200 y diagnóstico skiplink

Único caso separado reschedule-text.spec.mjs. Amplía en dos pasadas cada font-size calculado del main al doble (incluidos valores CSS fijos); comprueba razón2, sin CSS zoom ni viewport simulado como zoom. Cuatro estados a320/768altura400/1440, controles/bordes/solapes/axe y activación teclado del recorrido. No modifica el caso31anchos.

Incidencia ce3e48: generación JavaScript falló por comillas antes de crear archivo; runner llegó a No tests found/EXIT1a59950. Corregida sólo generación, nodecheckfca27f. Primer resultado real inicialmente GREEN627f29:1/1,9,4s total; log reschedule_text_snapshot_actual.log. No RED de producto. Capturas/12medidas/4axe en .e2e-work/reschedule-real/chromium/text200. Rect skiplink sin foco y captura viewport por estado separan el artefacto de fullPage.

## Zoom nativo200 separado

Nuevo reschedule-native-zoom.spec.mjs reutiliza patrón Today: extensión efímera tabs, Chromium headful con viewport:null, chrome.tabs.setZoom/getZoom=2 y DPR doble; ventana real ajustada para innerWidth320 CSS. Un recorrido real mover/revisión/cancelación/historial, controles44×44/overflow/axe y foco al cancelar. Sin CSS zoom ni emulación de viewport presentada como zoom.

Incidencia43ac6c: generación de archivo falló por comillas antes de escribir/ejecutar suite; se escribió directamente y nodecheck01e1ba correcto. Primera ejecución real11320/56acb9 inicialmente GREEN,1/1 en9,3s total/5,4s recorrido (76c45e). Prettier focal48f9ec correcto. Se añaden únicamente capturas CDP full-document siguiendo el patrón Today para poder revisar paneles fuera de primera pantalla, manteniendo capturas viewport. Repetición focal47503/925f7a pendiente por este cambio de evidencia, sin nuevos oráculos ni producción.

Skiplink: text200 review-skiplink.json b175b5 confirma focused=false,y=-100,height45,bottom=-55 con viewport700. No está dentro de pantalla; artefacto de captura fullPage, no bug de markup diagnosticado. Capturas viewport separadas disponibles en text200 por estado.

Entrega de paquete texto/zoom: repetición con capturas native fd7b5e EXIT0,1/1 PASS38da21 (7,1s total), sin cambio de oráculos. Prueba real: zoom2, DPR1,5→3; innerWidth1426→713 y ventana real654 para320 CSS; document scroll/client312 sin overflow. Cuatro estados, controles16/17/13/13, cuatroaxe0. Ocho PNG viewport/full y JSON conservados bajo `.e2e-work/reschedule-native-zoom/organizationweb-e2e-38660/evidence/reschedule-native-zoom-res-c838b--history-at-200-percent-s40/`, copia para que futuros tests no sobrescriban test-results. Stack38660 retirado; contexto headful cerrado en finally.

Texto: cuatro estados×3anchos=12medidas; factor2 calculado verificado, cuatroaxe0, skiplink fuera del viewport en cuatro estados. Zoom: API/PG y confirmaciones reales, no retención/errores nuevos. Datos no certifican dispositivos físicos, lector de pantalla, toda navegaciónTab ni controles DST/consentimiento aún ausentes del nominal. Próximo recorrido de esos controles requiere asignación separada; no se amplían motores en esta entrega.

Hashes38da21: reschedule-text.spec.mjs89DDE04D24B640BE8BAF698BDD97917976A4FD9AA7177D8A13F257120861D84B; reschedule-native-zoom.spec.mjs065CA4F1F641D2F4EA28C80485DF5723F2D3FF2820E5417DF0CEB30BED8F98D1. Prettiercheck38da21 GREEN; no producción/SCSS ni archivos congelados modificados. Original UX31anchos conserva BA3F51CD41D979A1DD3D25B9785A15FD670553860648EB5B3DDBB6BA74D00595.

## Controles condicionales — caso único posterior

Nuevo reschedule-conditional-ux.spec.mjs; único movimiento con destino Madrid2030-10-27 02:15→02:45. Error real400 de inicio ambiguo, elección +02 por teclado, error real400 de fin, elección +01; preview20090min y exceso3600segundos sobre presupuesto30min. Sin datos matriciales nuevos ni repetición31anchos: sólo320/768altura400/1440 en tres estados.

Tab/ShiftTab reales desde Revisar recorre selectores y checkbox; Home/ArrowDown/End eligen offsets, Space consiente y Enter confirma. El área medida para checkbox es label clicable contenedora; caja interna se registra aparte y no se confunde con objetivo44px. Geometría y axe guardados por estado antes de confirmar. Primera ejecución74679/dcd0d3 pendiente, sin tocar producción ni archivos congelados.

Resultado condicional: **inicialmente GREEN8b0b99/d680b1**,1/1,4,6s recorrido/6,6s total. Nueve medidas y tresaxe0 f1eaf9. Selectores y confirmación cumplen44×44; label checkbox medido248×126,375 a320,442,094×75,188 a768,992,25×52 a1440. Caja interna22×22 queda registrada y no se considera defecto porque el label es el control clicable efectivo. Selecciones reales Home/ArrowDown/End, navegación Tab/ShiftTab con foco exacto y Space; POST201 envía ambos offsets y allowOverBudget=true. Sin recibo antes de consentimiento y exactamente uno después.

Capturas9PNG y geometría/axe por estado: `.e2e-work/reschedule-real/chromium/conditional/`. Hash test6283DBA3A04984B4D7E68684167E2133B4521DAA9163879381B8ABB09A02746F; geometryC823E889C251A7B1E40D6A9290F55CFE4CCED4DAA5CB1042F4342BC0EFEE71EA; log8D65978635DA6245822901C847953EB2678222AA56537B170CC40D14747DDEDF. Prettierwrite/check/nodecheckf1eaf9 GREEN. Stack64916 retirado. Fuente/SCSS/nominal/UX31anchos/texto/zoom intactos.

Este caso aporta evidencia de U2/U9/U14/U20/U23/U29 (revelación y corrección de condiciones), U13 (targets efectivos), U15 (Tab/teclado reales) y U28 (consentimiento explícito). Revisión humana y matriz30 global siguen separadas; no acredita estos controles con nativezoom/text200 ni Firefox/WebKit o dispositivos físicos.

## Motores nominal y condicional

Runner real leído99b2aa, CLI Playwright1.63 confirma --browser y binarios Firefox/WebKit presentes141c78; configuración única playwright.config.mjs conservada. No existe config cross-browser separada en este corte; el primer intento de leerla falló sin cambios. Listado previo exacto2tests por motor141c78/b467f5. Dos archivos existentes, ninguna adaptación de oráculo.

Firefox sesión65297 EXIT0b0ed71, **2/2PASS** b467f5 (15,3s). WebKit97754 EXIT0ed8d27, **2/2PASS** e37af9 (15,4s). Nominal de creación/movimiento/cancelación/historial y recorrido DST/consentimiento/Tab pasan en ambos motores. Cada condicional conserva9mediciones/3axe/capturas en .e2e-work/reschedule-real/{firefox,webkit}/conditional. No se extrapola UX31anchos/text200/nativezoom/reinicio13 a esos motores; tampoco dispositivos físicos. Stacks4020/65352 identificados en logs y retirados por runner; no procesos activos de esas ejecuciones.

## Feedback U30 — caso único

Lectura b12668 confirma ausencia de medición directa reschedule anterior. Nuevo reschedule-feedback.spec.mjs: preview real route.fetch200 retenido; mide desde keydown Enter hasta MutationObserver anuncia Revisando movimiento, antes de liberar respuesta. Primer resultado se reemplaza por503 controlado, no se simula éxito; retry devuelve200 real. Foco, borrador, ausencia de duplicado/recibo, geometría320 y axe pendiente/error se comprueban. Primera ejecución72031/e37af9 pendiente. No nuevas matrices ni producción.


Feedback inicialmente GREEN3b3449/e526d3:1/1 en5s total,2,5ms desde keydown hasta anuncio (objetivo<400ms) con respuesta todavía retenida. Dos llamadas total: primera503 controlada y retry200 real. Foco sigue en Revisar tanto pendiente como error y después de reintento, borrador conservado, sin recibos. Geometría320 y dosaxe0 en .e2e-work/reschedule-real/chromium/feedback/{pending-320.png,error-320.png,evidence.json}. No simula éxito ni mide latencia de servidor como feedback. Stack24084 retirado.

Entrega final para review: Prettierwrite/check/nodecheck7a7f0d GREEN. Fuente feedback7C2B024A44285FE17D6DDAE3B8BD553076E840BF385EDB4199473D813EE6B160; evidencia0F24E84F9594F758009CD0DC93CBE747DF45E7B7D3B6265AE3E596352CB4F717. Nominal0286BD931CD1F461981F086AEF895E4864F1CA2E786A09E1D1283374FD649E62 y condicional6283DBA3A04984B4D7E68684167E2133B4521DAA9163879381B8ABB09A02746F intactos. Por motor, JSON7a7f0d verifica9medidas/3axe0 tanto Firefox como WebKit. El feedback es sólo Chromium; no extrapolado a motores alternativos. Ningún cambio productivo ni commit/push por agente; espera revisión final y confirmación root para commit aislado sólo E2E/docs, nunca snapshot Java.
