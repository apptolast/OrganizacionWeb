# Mutación — create_project

Revisión del coordinador sobre los informes ejecutados por los responsables de cada frontera. Umbral obligatorio: 80 % por suite, sin combinar scores para ocultar un resultado inferior.

| Suite | Alcance | Muertos / total | Score |
| --- | --- | --- | --- |
| PIT | Dominio y aplicación Java | 36 / 36 | 100 % |
| Stryker | Cliente HTTP y hook de creación React | 143 / 148 | 96,62 % |

PIT desactiva el filtro FRECORD para incluir los constructores compactos escritos a mano. Excluye solamente equals, hashCode y toString generados por records. El informe XML contiene 36 KILLED, ningún superviviente. No se presenta esta cifra como mutación de adaptadores Spring/JDBC: esos límites se comprueban con PostgreSQL real y pruebas API/arquitectura.

Stryker no excluye los cinco supervivientes del denominador. Las equivalencias revisadas contra los consumidores actuales son: guardia de tipo sobre valores JSON; null frente a undefined en fallback; dos arrays internos cuyos elementos alternativos nunca coinciden con los campos del formulario; nombre de campo alternativo que tampoco existe. Detalle de cada identificador y evidencia en [informe frontend](mutation_create_project_frontend.md). Si se amplían los consumidores o campos, revisar esas equivalencias. JSX y SCSS se verifican mediante pruebas de interfaz y navegador, sin atribuirles el score de la lógica.

No hay mutantes sin cobertura, errores ni timeouts en el resultado final frontend. No se han rebajado umbrales ni alterado dependencias instaladas para fabricar el resultado.

Evidencia reproducible: `node .harness/harness.mjs verify`, con PIT XML/HTML en backend/build/reports/pitest y Stryker en frontend/reports/mutation (generados, fuera de Git). Las bitácoras TDD y el informe del juez documentan los defectos que exigieron pruebas adicionales y su corrección.
