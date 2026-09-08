# Refuerzo C de oráculos UI21

Ownership exclusivo: dos nuevos archivos custom-fields.mutation.test.tsx y
customization-metadata.mutation.test.tsx. Producción y tests originales no
editados. Pequeños fixtures locales con useCustomizationSession real,
CustomFieldsPanel/App/ProjectTasks y fetch simulado; sin mocks del estado ni
nuevas abstracciones de infraestructura. No acredita HTTP/socket/PG.

Cada caso se añadió y ejecutó individualmente. Inicialmente GREEN sobre el
producto existente; no se inventa RED productivo:

| Ciclo | Conducta | EXIT0 / evidencia |
| --- | --- | --- |
| C01 | TEXT exactamente1000 astrales, body y confirmación exactos | fecc2c |
| C02 | +1e9 escrito sin signo, entero confirmado | cd4dee |
| C03 | -1e9 aceptado | 395e02 |
| C04 | false editado enviado como booleano | eaa1e8 |
| C05 |503 escritura incierta, sin retirada ni falsa confirmación |24bbdc |
| C06 |401 GET retenido usa callback vigente |57eb39 |
| C07 |404 recuperación retenida usa callback vigente |102608 |
| C08 |503 GET inicial ofrece Reintentar sin retirar ni publicar vacío |7d8eb0 |
| C09 |503 recuperación mantiene bloqueo sin retirar |8cc083 |
| C10 | Recuperación no roba foco movido voluntariamente a otro control/body |7e4d29 |
| C11 |400 values[10] en12campos, asociación y todos borradores/body exactos |025940 |
| C12 |PROJECT fechas distintas: datetime, etiqueta y textoUTC exactos |9d2493 |
| C13 |TASK fechas distintas: datetime, etiqueta y textoUTC exactos |41ecc9 |

Logs separados customization_refinement_c01.log a c13.log. C05 inicial
526bc9 falló por mi premisa de textbox visible durante incertidumbre; el
formulario está oculto y el contrato conserva recuperación, no esa presencia
DOM. Se corrigió a heading/recovery/callback, sin afirmar lectura del borrador
oculto. C11 inicial9ecca4 usó problem400 incompleto (sin type/title); decoder
lo rechazó correctamente. Fixture completo pasó025940. Ambos fallos quedan
preservados como errores de test, no RED del producto.

Precisión del análisis previo: los tests originales específicos de fechas
ya usaban instantes distintos. El hueco real era texto y etiquetas: sólo
comprobaban datetime+presenciaUTC. Los nuevos casos copian ese fixture local
y añaden strings públicos exactos, sin comparar contra el mismo formatter.
No se modifica retrospectivamente el resultado de aquellos tests.

Primer conjunto13/13 pasa, pero tipos rechaza exact:true (opción Playwright,
no ByRoleOptions de Testing Library). Se retiró esa opción: nombre string
coincide exactamente por defecto. Formato y misma regresión tras corrección:
13/13 en2 suites,3.74s; tipos y ESLint de ambos archivos EXIT0 5ec926.
Logs *_verified.log, *_types_final.log, *_lint_final.log. Sin supresiones.

Objetivos originales propuestos: custom-fields518,538,544,564; guardias
433–441,454–460,573–579,415/416; foco392–399;595 índice10; metadatos
1877–1883,1921–1927,1933,1937–1941. Firmas exactas permanecen en
customization_ui_mutation_review_signatures.json. No se afirma muerte de
ningún mutante antes del incremental oficial, ni se reclasifican RuntimeError.
Testsoriginales y producción permanecen congelados para esa campaña.
