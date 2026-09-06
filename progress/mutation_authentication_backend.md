# Mutación de autenticación backend

Ponytail full y Caveman lite activos. Comando ejecutado: `gradlew.bat -PmutationScope=authentication pitest`. Informe XML: `backend/build/reports/pitest-authentication/mutations.xml`.

Resultado final: 41 de 44 mutantes eliminados (93,18 %), cero NO_COVERAGE y 66 de 67 líneas cubiertas. El umbral permanece en 80 %. El alcance contiene exclusivamente SessionController, SessionAccessDeniedHandler, SessionFailureFilter y SessionCookiePolicy. Las pruebas unitarias de esos adaptadores ejecutan la mutación; las pruebas HTTP reales con PostgreSQL verifican por separado la integración del framework. No se presenta el dominio intacto como cobertura de autenticación.

El primer resultado fue 39 de 44. Se cerraron dos huecos observables: rechazar una dirección IP literal remota en HTTP y comprobar el título correcto del error según identidad/CSRF. El segundo resultado elimina ambos mutantes. No hubo cambios de producción durante este refuerzo.

Los tres supervivientes eliminan llamadas explícitas del serializador estándar: setCookieName("SESSION"), setUseHttpOnlyCookie(true) y setSameSite("Lax"). Son equivalentes con Spring Session 3.5.5: esos tres valores ya son sus defaults. Las pruebas comprueban los atributos de la cookie producida, pero retirar una asignación idéntica no cambia esa salida. Se conservan las asignaciones explícitas como política legible. La única línea no cubierta es el constructor privado de la utilidad, sin mutantes no cubiertos.

La configuración PIT predeterminada une los cuatro adaptadores y sus pruebas al alcance anterior de dominio/aplicación. Así la CI existente los verifica en una sola ejecución. El comando focal mantiene un informe independiente y no reemplaza las pruebas HTTP o E2E.

Identificadores reproducibles del XML (clase SessionCookiePolicy, método create, mutador VoidMethodCallMutator):

| Línea | Índice | Bloque | Llamada eliminada |
| --- | --- | --- | --- |
| 25 | 101 | 32 | setCookieName |
| 27 | 111 | 34 | setUseHttpOnlyCookie |
| 28 | 116 | 35 | setSameSite |

Cada superviviente ejecutó seis casos de atributos de cookie. La equivalencia se limita a la versión 3.5.5 del serializador; una actualización de dependencia debe volver a contrastar esos defaults.
