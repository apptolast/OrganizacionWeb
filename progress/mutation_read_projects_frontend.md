# read_projects — mutación frontend

La ejecución completa45125 terminó exit0: **276/297 (92,93 %)**, 21 supervivientes, cero timeout, noCoverage o errores. Baseline73 tests. El informe original se conserva en frontend/reports/mutation/mutation.json y mutation.html. No se declara una nueva puntuación global después de los refuerzos focalizados.

## Alcance y resultados

Scope exacto de frontend/stryker.config.json: src/projects-api.ts, src/use-create-project.ts, src/read-projects-api.ts, src/use-read-projects.ts y src/navigation.tsx. Los dos primeros conservan la lógica del corte anterior; los tres nuevos cubren contrato de lectura, estado, retry, cancelación, obsoletos y navegación.

Vitest4.1.10, Stryker10, coverageAnalysis perTest, dos workers. Umbral80 sin reducción. JSX de presentación y SCSS se verifican mediante comportamiento y navegador; su cobertura no se atribuye a este score.

| Archivo | Eliminados / total | Supervivientes iniciales |
| --- | --- | --- |
| navigation.tsx | 22/26 | 4 |
| projects-api.ts | 92/94 | 2 |
| read-projects-api.ts | 82/92 | 10 |
| use-create-project.ts | 51/54 | 3 |
| use-read-projects.ts | 29/31 | 2 |

## Reproducción focalizada

Se añadieron cinco casos observables: estado desconocido, ownerId no textual, cursor array, cancelación del salto nativo y liberación de la suscripción al desmontar. No se cambió producción. Los40 casos focalizados de lectura pasan; junto con los38 anteriores hay78 casos declarados. Lint posterior verde. La ejecución completa anterior verificó73 tests y build; no se afirma una nueva ejecución completa de78.

Replay12207 exit0: **17/17** mutantes eliminados, cero supervivientes, timeout, noCoverage o errores. Este resultado corresponde exclusivamente a la reproducción selectiva, no al frontend completo. Cubre los seis huecos reales identificados abajo y mutantes vecinos. Informes separados: frontend/reports/mutation-targeted/mutation.json y mutation.html. Configuración temporal: frontend/.stryker-tmp/read-projects-survivors.json. El alcance canónico no se modificó.

Rangos del replay: navigation.tsx líneas5 y26; read-projects-api.ts líneas19,39 y49. Comando: pnpm exec stryker run .stryker-tmp/read-projects-survivors.json.

## Los21 supervivientes de la ejecución completa

Los IDs corresponden al informe original; el replay renumera los mutantes. La equivalencia se evalúa respecto al comportamiento de la aplicación y datos JSON del contrato, no a objetos JavaScript arbitrarios con métodos inyectados.

| Archivo / ID / línea | Evaluación y evidencia |
| --- | --- |
| navigation /5 /5 | Hueco real: elimina cleanup. La prueba de desmontaje observa cero listeners; eliminado en replay. |
| navigation /6 /5 | Hueco real: removeEventListener con nombre vacío deja suscripción viva. Misma prueba; eliminado en replay. |
| navigation /24 /26 | Hueco real: suprime preventDefault y permite segunda navegación nativa. fireEvent.click debe devolver false; eliminado en replay. |
| navigation /26 /27 | Equivalente: cambia el segundo argumento de pushState, título ignorado por History API; ruta y UI no cambian. |
| projects-api /35 /19 | Equivalente: debilita typeof object; las comprobaciones posteriores rechazan primitivas o desembocan en el mismo fallo seguro. Conservado del informe create_project. |
| projects-api /88 /55 | Equivalente: catch JSON devuelve undefined en vez de null; ambos activan el mismo fallback sin detalles internos. |
| read-projects-api /124 /11 | Equivalente: isRecord siempre true. Campos posteriores rechazan el valor o lanzan; el hook produce el mismo error genérico, sin falso éxito. |
| read-projects-api /126 /11 | Equivalente: OR en isRecord admite primitivas/null, rechazados por campos posteriores o excepción capturada con el mismo resultado. |
| read-projects-api /127 /11 | Equivalente: elimina typeof; validación restante o excepción impide publicar datos incompatibles. |
| read-projects-api /130 /11 | Equivalente: elimina guard null; acceso posterior falla y se convierte en el mismo error genérico. |
| read-projects-api /153 /19 | Hueco real: admite estado desconocido. Caso status unknown rechaza respuesta; eliminado en replay. |
| read-projects-api /184 /39 | Hueco real: admite ownerId numérico. Caso42 rechaza detalle; eliminado en replay. |
| read-projects-api /199 /45 | Equivalente: suprime prefijo isRecord/Array.isArray. El posterior items.every valida arrays; otros valores JSON no aportan un método every ejecutable y fallan de forma segura. |
| read-projects-api /200 /45 | Equivalente: OR en ese prefijo mantiene las validaciones posteriores; objetos sin array causan el mismo fallo seguro en every. |
| read-projects-api /208 /49 | Hueco real: cursor array con length pasa como string. Caso array rechaza respuesta; eliminado en replay. |
| read-projects-api /215 /52 | Equivalente: vacía el mensaje privado de Error; nunca se presenta, el hook lo transforma en el mismo error de lectura. |
| use-create-project /220 /8 | Equivalente: error inicial ficticio no coincide con un campo y no produce mensaje visible. Conservado de create_project. |
| use-create-project /252 /29 | Equivalente: mismo error ficticio al limpiar; no coincide con campos ni altera el flujo. |
| use-create-project /268 /38 | Equivalente: cambia fallback de identificador de campo inexistente; ninguno encuentra control que enfocar. |
| use-read-projects /303 /27 | Equivalente en App: retry solo se ofrece tras fallo y cada ruta remonta el lector; data ya es undefined. El clear conservado es defensivo. |
| use-read-projects /306 /29 | Equivalente: revision es un contador opaco para dependencias; decrementarlo también dispara cada retry sin diferencias visibles. |

Quedan15 equivalentes justificados de la primera ejecución; los seis huecos observables fueron eliminados en replay. No se suman ejecuciones para fabricar un denominador nuevo ni se excluye código para elevar el resultado.
