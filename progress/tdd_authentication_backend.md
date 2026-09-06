# authentication — TDD backend



Contrato aprobado y baseline confirmado por el coordinador. Ponytail full y Caveman lite activos. Spring Security 6.5.8 / Session JDBC 3.5.5 contrastados previamente con BOM y fuentes exactas. Autenticación queda en adaptadores; dominio de proyectos intacto.



1. HTTP real GET anónimo: RED 401. GREEN con Spring Session JDBC, migración V6 y endpoint de cuatro campos que materializa CSRF. Se comprueban cookie /api HttpOnly/Lax, no-store y fila PostgreSQL.

2. Login real: RED por endpoint de autenticación inexistente. GREEN con el filtro estándar de formulario, respuesta 204, rotación de cookie, identidad posterior y sesión autenticada guardada.

3. Credenciales inválidas: tres casos RED por redirección predeterminada. GREEN con el mismo problem 401 genérico y ninguna sesión autenticada.

4. Basic: RED porque las credenciales Basic aún daban acceso. GREEN al retirar ese mecanismo y configurar entry point JSON sin desafío. OriginGuard se reubica antes de CSRF para cubrir también el filtro de login.

5. Logout real: RED porque no existía la ruta configurada. GREEN con el filtro estándar, DELETE de sesión y expiración de SESSION con Path /api; la cookie antigua ya no accede a proyectos.

6. CSRF: cinco operaciones RED por estado o cuerpo incorrecto. GREEN con AccessDeniedHandler propio que devuelve 403 CSRF_INVALID, sin ejecutar escrituras.

7. Expiración: sesión JDBC vencida daba 403 en escritura sin token. RED y GREEN al distinguir sesión no autenticada antes de devolver error CSRF: 401 UNAUTHENTICATED, sin escritura y sin acceso posterior.



8. Persistencia de sesión: dos pruebas HTTP reales RED con 500 al fallar UPDATE de login y DELETE de logout mediante triggers PostgreSQL. GREEN con filtro exterior mínimo que reinicia la respuesta aún no comprometida y devuelve 503 SESSION_UNAVAILABLE. Ninguna cookie provisional se confirma; logout conserva la cookie anterior y el reintento funciona.

9. Lectura de sesión con tabla temporalmente inaccesible, con y sin cookie: GREEN inicial de regresión; devuelve 503 y nunca authenticated false ficticio.

10. GET de logout y origen extranjero con CSRF válido: GREEN inicial de regresión; GET no invalida la sesión y las tres escrituras de origen extranjero reciben 403 UNTRUSTED_ORIGIN.

11. Política de cookie: RED de compilación para la política extraída; GREEN con rechazo de orígenes ambiguos o HTTP remoto. Se comprueban seis orígenes admitidos, doce rechazados y atributos reales del serializador, ignorando cabeceras reenviadas.

12. Migración de pruebas históricas de propietarios: RED observado al retirar Basic. Se sustituyen identidades de prueba por user() y csrf().asHeader(); el primer caso de creación vuelve a GREEN. La regresión completa del corte queda pendiente.

13. Pruebas unitarias de los cuatro adaptadores propios: GREEN inicial de regresión para identidad, clasificación de CSRF, fallos de transacción, respuesta comprometida y cookie. Se añaden para mutación pertinente, sin atribuir estas verificaciones al dominio intacto.

14. Login y escritura real: GREEN inicial; el token anterior recibe 403, el token nuevo permite crear un proyecto propio y consultarlo.



Los ciclos HTTP utilizan servidor real y PostgreSQL Testcontainers. La mutación focal de autenticación conserva informe separado; la configuración predeterminada incorpora esos cuatro adaptadores junto al dominio y aplicación para la verificación continua existente.


15. Regresión focal final: 217 pruebas verdes, incluyendo las cuatro API históricas migradas, HTTP real de autenticación y unidades de sesión. Formato aplicado. No se repite la baseline global; el coordinador ejecuta la regresión conjunta tras congelar.
16. Mutación focal: 39/44 inicialmente; se añaden comprobaciones de IP remota y título de error. Pruebas relevantes verdes y PIT final 41/44, con tres equivalencias documentadas en mutation_authentication_backend.md. No se modifica producción tras esta revisión.

## Correspondencia del contrato

- s1: respuesta anónima exacta y error JDBC que no simula anonimato, mediante HTTP real.
- s2 y s18: login, rotación de sesión y CSRF, identidad preservada y creación/lectura propia, mediante HTTP real.
- s3: credenciales inválidas indistinguibles, mediante tres casos HTTP reales.
- s4: Basic no autentica ni provoca desafío del navegador.
- s5: esquema JDBC y sesión persistida verificados aquí; reinicio de contenedor con cookie conservada corresponde a integración.
- s6: expiración real mediante fila JDBC vencida, lectura y escritura 401.
- s7 y s8: eliminación y cookie antigua denegada; GET no invalida la sesión.
- s9 y s10: cinco escrituras sin CSRF y expiración sin escritura, mediante HTTP real.
- s11: tres operaciones con origen extranjero y token válido, mediante HTTP real.
- s12: triggers PostgreSQL reales durante login y logout, respuesta 503 sin Set-Cookie provisional y reintento válido.
- s13: política de origen/cookie en unidades del serializador; atributos reales en el servidor HTTP local.
- s14 a s17 y s19: interacción, accesibilidad, estados de interfaz y navegadores pertenecen a frontend/integración y a sus informes independientes.

Fuentes congeladas y Gradle liberado para revisión conjunta. No se declara cierre de la feature ni CI propia completada desde esta bitácora.
