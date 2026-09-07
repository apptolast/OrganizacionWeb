# Interfaz de inicio de trabajo14: revisión de los30 criterios

Fuentes: `docs/ux-requirements.md` y `features/start_work_session.feature` @s28–42. Esta tabla describe aplicaciones observables; no certifica efectos psicológicos ni comprensión por personas. No habilita uso habitual sin16 ni incorpora pausa/cierre15–18.

Evidencia de autor:

- **F**: tres E2E reales, POST/PG/GETid/key, ACK perdido, recarga, activa de otra tarea y logout confirmado. Chromium5d91ac/e53689/2450d0; Firefox5/5a884d3 y WebKit5/5815ca6 incluyen estos tres y las dos pruebas de presentación.
- **U**: ausencia→envío pendiente→incertidumbre→confirmación,31anchos320–2560 con fronteras y altura400 a768,124medidas de controles44px, bounds y no intersección;4axe sin violaciones. Chromium540246; Firefoxa884d3; WebKit815ca6. Datos y PNG: `.e2e-work/start-work-real/{chromium,firefox,webkit}/ux`.
- **T**: mismo flujo con texto de main al200%, factor2 verificado,320/768/1440,12medidas/4axe. Chromium13e501, Firefoxa884d3 y WebKit815ca6; directorios `text200`. No equivale al zoom global.
- **N**: zoom nativo Chromium mediante perfil/extensión aislados, zoom2 y DPR1,5→3, viewport320;4estados/4axe GREEN57e4f9. Capturas ordinarias recortadas identificadas; capturador CDP heredado13 corregido y repetición GREEN3fb766. Evidencia completa inspeccionada en `.e2e-work/start-work-native/organizationweb-e2e-4660/evidence`; primer intento preservado por separado.
- **C**: API/UI/integración focales existentes, incluidos contexto global, precisiónµs, privacidad tras await, CSRF manual y fallback UTC. Refuerzos85/85caf80f. JSDOM no acredita geometría.

| Principio              | Aplicación y evidencia                                                                     | Resultado y límite                                                                      |
| ---------------------- | ------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------- |
| Atención selectiva     | Encabezado Sesión de trabajo, una duración y acción explícita; alertas diferenciadas. U/T. | Jerarquía observable en capturas; atención humana no medida.                            |
| Carga cognitiva        | Sólo duración; ni key ni catálogo de zonas exigidos. F/C.                                  | Recorrido directo verificado; comprensión humana pendiente.                             |
| Estética-usabilidad    | Reutiliza campo, formulario, región y tipografía. U/T/N.                                   | Consistencia visual observable; no inferencia de facilidad por estética.                |
| Posición en serie      | Duración→inicio→recuperación; Enter conserva el flujo y foco. U/F.                         | Secuencia comprobada; memoria humana no medida.                                         |
| Tendencia a la meta    | No presenta progreso ni tiempo neto por iniciar. C/F.                                      | Ausencia deliberada; progreso real fuera de14.                                          |
| Von Restorff           | Errores y estados tienen texto/roles, no sólo color. U/axe.                                | Distinción y comprobaciones automáticas de contraste; no sustituye lector de pantalla.  |
| Zeigarnik              | Retiene intención y explica que cerrar no revoca envío. U/F/C.                             | Recuperación real por key sin otro POST; persistencia tras recarga verificada.          |
| Fluir                  | Minutos y fin fijo visibles; sin cierre automático. F.                                     | Datos comprobados; evaluación de concentración no realizada.                            |
| Fragmentación          | Región propia dentro de TaskReader separada de estado/bloques. U/T.                        | Grupos visibles y semánticos; no número mágico de grupos.                               |
| Memoria de trabajo     | Conserva25min y key ante ACK perdido/503. F/U.                                             | Usuario no reconstruye intención; otras variantes C.                                    |
| Navaja de Occam        | Empezar, actualizar, comprobar y reenviar según estado. C/U.                               | Sin menú o dependencia nueva; cada acción tiene propósito contractual.                  |
| Conectividad uniforme  | Enlace conduce a tarea del recibo, incluso otra propia. F.                                 | Relación real verificada; no conexiones decorativas.                                    |
| Fitts                  | Controles nativos medidos44×44 y sin intersecciones. U/T/N.                                | Geometría en escritorio/emulación; dedo y dispositivo físico no probados.               |
| Hick                   | Una decisión inicial explícita; recuperación aparece cuando necesaria. U/F.                | Opciones visibles por estado; decisión humana no cronometrada.                          |
| Jakob                  | Formulario/enlace nativos, Enter y foco al desaparecer iniciador. U/F/C.                   | Foco real medido; Tab/ShiftTab del componente no sustituyen lector de pantalla.         |
| Semejanza              | Reutiliza estilos globales y SessionGate. U/T.                                             | Capturas mantienen patrones; CSRF manual cubierto en C.                                 |
| Miller                 | Agrupa contexto, duración y horas por significado. U.                                      | Estructura observada; comprensión pendiente.                                            |
| Parkinson              | Fin viene del recibo, estable después de recarga. F/C.                                     | No extensión/cierre por latencia ni tiempo neto ficticio.                               |
| Postel                 | Identidades coherentes, duración1–1440 y DTO7 cerrado conµs exactos. C/F.                  | No relaja validación ni utiliza Number para precisión submilisegundo.                   |
| Proximidad             | Label/error asociados; control dentro de campo propio. C/U.                                | Proximidad visual y asociaciones; error de validación concreto cubierto por C.          |
| Prägnanz               | Textos distinguen ausencia, consulta, incertidumbre y confirmación. U.                     | Defecto de ausencia obsoleta reproducido17bf9f, mínimo corregido y GREENcfe3a8/540246.  |
| Región común           | Sección nombrada y formulario agrupan el inicio. U/T.                                      | Bordes y fondo visibles en capturas; no dependencia exclusiva del color.                |
| Tesler                 | Key interna y fallback UTC conserva zona histórica. C/F.                                   | Recuperación real sin reconstruir key; fallback de catálogo sólo evidencia C.           |
| Modelo mental          | Distingue duración prevista, fin y tarea completada. F/U/C.                                | Texto explícito; no se afirma comprensión universal ni se añade estados15–18.           |
| Usuario activo         | Ausencia orienta; duración empieza vacía y no hay POST al montar. F/U.                     | Comprobación funcional; evaluación de primer uso pendiente.                             |
| Pareto                 | Inicio directo; recuperación contextual. F/U.                                              | Decisión de diseño, sin porcentajes de uso inventados.                                  |
| Fin de pico            | Confirma recibo compatible y conserva hecho ante fallo posterior. F/C.                     | Recuperación exitosa observable; satisfacción humana no medida.                         |
| Sesgo cognitivo        | Lenguaje neutral, sin culpa ni métricas falsas. U/C.                                       | Revisión textual; efectos psicológicos no certificados.                                 |
| Sobrecarga de opciones | No personalización ajena al inicio; duración bloqueada en incertidumbre. U/F.              | Alcance y lectura de intención comprobados.                                             |
| Doherty                | Anuncio tras Enter antes de entregar respuesta pendiente. U.                               | Chromium6,5ms en esta muestra (<400); no latencia universal ni rendimiento de servidor. |

Límites: Chromium, Firefox y WebKit ejecutados en escritorio Windows; ampliación nativa sólo Chromium. Dispositivos físicos, teclado virtual, áreas seguras reales, lector de pantalla y estudio de uso no ejecutados. No hay variantes propias de tema/densidad ni drag en14. Axe sin violaciones no equivale a certificación WCAG completa. El logout E2E se comprueba tras respuesta confirmada; retirada inmediata y respuestas tardías se acreditan por C.

El skiplink no enfocado está fuera del viewport (top=-100,bottom=-55); su aparición en fullPage fue artefacto de captura, confirmado por viewport y root. No se alteró CSS global. Evidencia previa a corrección de ausencia conservada en `chromium/ux-initial`; Stryker84,80% pertenece a ese corte anterior. Medición final es tarea separada.

Cierre de autor: cuatro archivos E2E congelados, syntax+Prettier391b8a y hashes en `start_work_e2e_freeze.json`. Total de geometría final:372 medidas UX +36 texto ampliado +4 zoom nativo =412;28 análisis axe sin violaciones (12+12+4). Son mediciones dentro de recorridos, no412 pruebas independientes. Cada motor tiene los mismos cuatro estados, con3funcionales+UX+texto; Chromium añade zoom nativo. La evidencia funcional Chromium anterior al mínimo render se conserva, sin repetirla por un cambio que sólo retira un párrafo; el flujo UX corregido sí repitió ambos estados afectados. Pendiente dictamen del juez; no cierra feature14.
