# Destilación de export_data

Contrato redactado sólo en el checkout limpio OrganizacionWeb-export-data,
desde la sección22 revisada por root. La base publicada de datos es 5a5464c;
no se ha consultado V14 protegido de COMMON ni modificado producto o estado.
Ponytail full y Caveman lite aplicados conservando la prosa normal del contrato.

La corrección de recibos conserva BlockChangeReceipt7 con version textual,
PlannedBlock6/request7/time5 y offsets de intención separados de los resueltos.
Durante la destilación se detectó MOVED en la prosa: MoveBlock.java:76 y
PostgresBlockStore.java:100 publicados usan RESCHEDULED. Root corrigió ambos
literales; el escenario s9 conserva RESCHEDULED/CANCELLED sin traducir historia.

Los 33 escenarios separan datos y fidelidad (s1–14), límites y fallos (s15–17),
HTTP (s18–21 y s33), recorrido y privacidad del cliente (s22–30) y foco/UX
(s31–32). Cada escenario contiene un único When. Las tablas de ejemplos agrupan
variantes de la misma acción; no representan resultados de pruebas ejecutadas.
El esquema exacto se referencia a la sección22 para evitar una segunda tabla
de catorce colecciones divergente. Se conservan oráculos específicos para
recibos, inactivos, null, enteros largos y relaciones entre propietarios.

Comprobación documental: tags consecutivos y únicos, un When por escenario,
sin pasos posteriores a Examples y diff sin errores de espacios. No se instaló
un parser Gherkin ni se ejecutaron campañas. La revisión root precede al TDD;
este documento no acredita implementación, mutación, rendimiento ni UX.

Revisión acotada de s16/s22: s16 usa registros válidos cuyo contenido exportable
supera el límite de bytes y permite acreditarlo sin acumular la cuenta completa.
El tamaño bruto de JSONB desconocido o corrupto no prueba ese exceso; corrupción
sin exceso demostrado conserva 503, cubierto por s5. s22 no exige persistencia
global de borradores entre rutas: sólo impide añadir mutaciones o resets a los
efectos del ciclo de navegación existente. La sección22 incorpora ambas
precisiones sin cambiar límites, errores ni comportamiento de otras features.
Precisión adicional del reloj: s14 añade nanos 123456789 truncados a 123456,
reutilizados en envelope y filename sin reinterpretar datos persistidos.
Se mantienen los 33 escenarios, ahora con 79 ejemplos expandidos por conteo léxico.
Revisión root aprobada: 33 escenarios y 79 ejemplos declarados. Las precisiones s14/s16/s22 conservan el alcance y resuelven reloj, exceso demostrado y navegación. La autorización global del usuario permite comenzar TDD sin otra confirmación. No equivale a implementación ni a pruebas ejecutadas.
Revisión de frontera de sesión: el código 401 se alinea con UNAUTHENTICATED ya definido en SecurityConfiguration y feature6. No se introduce un código distinto para exportación ni se modifica el contrato global de autenticación. Los fallos de persistencia de sesión anteriores a la lectura conservan SESSION_UNAVAILABLE.
