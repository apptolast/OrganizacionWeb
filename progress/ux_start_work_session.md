# Interfaz de inicio de trabajo14: alcance de los30 criterios

Fuente de requisitos: `docs/ux-requirements.md`; contrato `start_work_session.feature` @s28–42. Corte de autor, pendiente de juez y de navegador. La prueba focal `4a7565` acredita73 casos (32 API,39 componente,2 integración); no demuestra geometría, rendimiento humano ni conformidad universal. No hay acciones de15–18, reloj que cierre una sesión ni tiempo neto ficticio.

| Principio | Aplicación y evidencia disponible | Resultado y límite |
| --- | --- | --- |
| Atención selectiva | Encabezado Sesión de trabajo, duración y acción explícita; alertas diferenciadas. | Semántica verificada; jerarquía visual en anchos pendiente. |
| Carga cognitiva | Se solicita sólo duración, sin recordar key ni zona técnica. | Recorrido de componente verificado; comprensión humana pendiente. |
| Estética-usabilidad | Reutiliza `.field`, `.task-form`, `.task-blocks` y tipografía existente. | Consistencia de fuente; capturas éxito/error pendientes. |
| Posición en serie | Orden duración, acción, recuperación y cierre. | Enter/Tab/ShiftTab `98bd6b`; orden visual responsive pendiente. |
| Tendencia a la meta | No muestra progreso ni trabajo acreditado por el mero inicio. | Ausencia deliberada; progreso real fuera de14. |
| Von Restorff | Texto de estado y errores con roles; no dependen sólo del color. | Roles verificados; contraste y prioridad visual pendientes. |
| Zeigarnik | Retiene intención ante fallo y explica que cerrar no revoca envío. | @s32–36 verificados; nueva carga descubre activa `6dfdd5`. |
| Fluir | Muestra duración y fin fijo. | Inicio verificado; pausa/cierre pertenecen a futuras features. |
| Fragmentación | Región separada de estado de tarea y bloques. | TaskReader integrado, prueba `4ab0f6`; revisión visual pendiente. |
| Memoria de trabajo | Contexto, minutos y key retenidos en recuperación. | Fallos de red,503,conflicto y check repetido verificados. |
| Navaja de Occam | Empezar, actualizar, comprobar y reenviar sólo según estado. | Flujos verificados; no menús ni nueva dependencia de UI. |
| Conectividad uniforme | Enlace representa exactamente la tarea del recibo. | Relación comprobada; no conexiones decorativas. |
| Fitts | Botones nativos y ancho limitado por contenedor. | Teclado verificado; áreas44px y separación pendientes de medición. |
| Hick | Una decisión inicial: duración explícita. Recuperación aparece al necesitarse. | Estado inicial y revelación verificados; evaluación humana pendiente. |
| Jakob | Formulario nativo, enlace de tarea, foco al retirarse iniciador. | Enter, Tab y foco verificados; navegador y regreso visual pendientes. |
| Semejanza | Reutiliza patrones de formulario y SessionGate. | Código y CSRF manual verificados; capturas comparativas pendientes. |
| Miller | Datos agrupados por sesión, sin imponer un número de opciones. | Estructura semántica; comprensión humana pendiente. |
| Parkinson | Fin procede del recibo, no se recalcula por latencia ni se extiende. | Relación exactaµs en API; no temporizador de extensión/cierre. |
| Postel | UUID contextual compatible con mayúsculas; duración entera1–1440 y DTO cerrado. | Pruebas API; no relajación de identidad ni precisión temporal. |
| Proximidad | Label y error asociados por id/aria-describedby/aria-invalid. | Oráculo de campo verificado; proximidad en móvil pendiente. |
| Prägnanz | Ausencia, consulta, incertidumbre y confirmación tienen texto explícito. | Estados verificados; sin iconos como explicación única. |
| Región común | Sección nombrada y formulario nombrado agrupan el inicio. | Semántica verificada; bordes/fondos reales pendientes. |
| Tesler | La key permanece interna; fallback UTC explícito conserva zona histórica. | Fallback Intl `e7628a`; no exige catálogo al usuario. |
| Modelo mental | Distingue duración prevista, tiempo neto y tarea completada. | Texto y ausencia de transiciones automáticas; recuperación completed `678cdd`. |
| Usuario activo | Estado vacío orienta y pide duración sin valor inventado. | Montaje no hace POST; evaluación de primer uso pendiente. |
| Pareto | Inicio frecuente directo; recuperación accesible cuando procede. | Priorización de diseño, sin porcentajes de uso medidos. |
| Fin de pico | Confirma sólo DTO compatible; conserva recibo si consulta posterior falla. | @s30/37 y carrera lookup anterior `022727`; revisión humana pendiente. |
| Sesgo cognitivo | Lenguaje neutral, sin culpa ni métricas ficticias. | Fuente revisable; no se afirma efecto psicológico universal. |
| Sobrecarga de opciones | No ofrece personalización ajena al inicio ni estados15–18. | Alcance deliberado; campos bloqueados durante incertidumbre. |
| Doherty | Feedback inmediato en estado de envío, comprobación y consulta. | Anuncios verificados; medición real<400ms pendiente. |

Pendiente de validación navegador: doce anchos320–2560 y fronteras heredadas, alturas reducidas, contenido largo/Unicode, reflow, zoom nativo200%, texto ampliado, axe/contraste/foco visible,44px, Chromium/Firefox/WebKit. Dispositivos físicos, teclado virtual, lector de pantalla y revisión humana se declararán por separado. JSDOM no acredita esos resultados. Esta entrega permite empezar la revisión y preparar E2E; no declara terminado el gate UX14 ni habilita uso habitual sin16.
