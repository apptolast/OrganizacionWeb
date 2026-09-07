# Destilación de pausa y reanudación

Contrato: features/pause_resume_session.feature, 39 escenarios / 105 casos con tags estables @s1–@s39. Fuente: sección 15 de project-spec.md, revisión APPROVED y precisión final eb1a9bb de Work-Session-Revision. Estado spec_ready; pendiente juez independiente antes de TDD. No se han ejecutado pruebas ni modificado producción.

Trazabilidad: @s1–9 cubren migración de 14, intervalos exactos, límites de reloj, elegibilidad y plaza pausada; @s10–18 cabeceras, precedencias, privacidad, conflictos y replay; @s19–26 concurrencia, atomicidad, snapshot, consultas/recuperación y evento; @s27–39 validación cliente, precisión, recuperación de incertidumbre y UX. Convenciones comunes de 14 se referencian; las cinco rutas nuevas tienen conexión explícita de query y los tres GET de privacidad/503.

El escenario de dos sesiones distintas no cerradas del mismo propietario se omite porque viola la unicidad vigente. La regla persistente owner/key y resolución de colisión se conserva; su evidencia entre sesiones sucesivas queda para 16, cuando exista cierre. Root aceptó este límite, sin fabricar estado futuro ni reducir unicidad.

Comprobaciones documentales: 38 tags consecutivos, un When por escenario y JSON de metadata válido (36a191). Las filas de Examples son casos para ciclos TDD individuales, no evidencia ejecutada. No se ha repetido init por esta fase documental.

Corrección de revisión: se retiraron dos filas duplicadas de @s10; @s12 conserva el código heredado MALFORMED_JSON. @s39 cubre una respuesta antigua de S tras POST y nueva lectura confirmados en el mismo contexto. Corte actualizado: 39 escenarios y 105 casos, un When por escenario; SHA256 del contrato 1224D37A592545781F18E0564EB33404DD2AF67270B9FB0C8833BB65AFC48D6E. La comprobación anterior de 38 tags corresponde al primer borrador.

Precisión del juez en @s36: 401 atrasado sólo antes de entregar HTTP; JSON diferido pertenece a 200 y clasificación diferida a error no 401. No añade casos: 39/105. SHA256 final 15B7F783EB709F4BC0A4CD15E877BB74C095052548B409F2AB349E41AB1BBEBB.
