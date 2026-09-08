# Revisión independiente de la propuesta 24

**APPROVED para destilación del contrato**, limitado a la propuesta SHA256 `B948AB34566EA6CE1EED299B5CD7DF1DF42B3E22C56E13EA38F680A25CAE349B`, sobre checkout aislado de `99cd366`. No aprueba implementación, gates ni despliegue; 23 permanece independiente.

No se identifican contradicciones nuevas que bloqueen el contrato en la frontera revisada:

- La allowlist por método/ruta corresponde a los mappings publicados de proyectos, tareas, bloques, estado, agenda e historial. Las escrituras son únicamente los POST/PUT indicados; no incluyen transiciones, export/import, personalización o sesiones. HEAD/OPTIONS y OpenAPI tienen decisiones explícitas, sin permisos por prefijo.
- Cualquier Authorization selecciona el canal stateless sin fallback a cookie. El owner procede de la credencial y debe seguir habilitado en el bootstrap. La precedencia autenticación, Origin, scope, cuota y negocio está cerrada y conserva la cadena humana cuando no hay Authorization. Los secretos no aparecen en listados, export/import o almacenamiento del navegador.
- La id estable y la intención canónica permiten resolver COMMIT incierto sin generar otro secreto ni prolongar caducidad. Un 404 no libera automáticamente el intento. La propuesta distingue memoria del formulario, intención mínima en sessionStorage y secreto de una única visualización; cubre retiro de identidad y respuestas tardías. POST de negocio sigue expresamente sin garantía idempotente.
- Cupo y creación se serializan por owner; cuota comparte persistencia entre réplicas, comprueba ambos límites en una transacción y fija owner antes de credencial. Revocación tiene una frontera explícita: autorizar antes de su commit puede terminar después, y autenticar después lo rechaza. No promete cancelar operaciones ya admitidas ni restaurar una credencial revocada.

Dos fronteras ya exigidas por la propuesta merecen conservarse literalmente al destilar, sin añadir alcance: una cookie inválida o un fallo JDBC de sesiones no afecta al canal Bearer; y un replay idéntico de creación no consume otro cupo ni vuelve a mostrar secreto. La cadena actual `SecurityConfiguration` es de sesión y `SessionFailureFilter` envuelve excepciones de persistencia: esos comportamientos futuros requieren pruebas reales de filtro, no se acreditan por la configuración actual ni por mocks del controller.

Revisión sólo lectura de propuesta, mappings y seguridad vigente. Sin pruebas, init repetido, normativa, Gherkin ni cambios de producto. PIT 23 continúa en su checkout y sesión originales.
