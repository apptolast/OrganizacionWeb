# Sesión actual

- **Feature en curso:** #2 publish_outbox.
- **Fase:** TDD tras aprobación explícita del usuario el 5 de septiembre de 2026 («Sí la apruebo… continúa»).
- **Estado:** in_progress. 23 escenarios / 36 casos aprobados; no ampliar a consumidores o conectores.

## Bitácora

- Init raíz completado correctamente: lint, backend y 38 tests frontend verdes.
- Backend: caso de uso, puertos, migración, adaptadores PostgreSQL/RabbitMQ y disparador; TDD incremental y mutación.
- Integración: Compose y comprobación de publicación/caída/recuperación en stack aislado.
- Juez independiente: revisión de contrato, código y evidencia; sin autoría de producción de esta feature.
- El frontend permanece en su primera entrega. Monorepo y criterios UI/UX confirmados se conservan.
- No se despliega en el servidor. Commits y push autorizados; cierre tras pruebas, revisión y mutación.
