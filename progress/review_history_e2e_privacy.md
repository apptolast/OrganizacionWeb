# Revisión del recorrido de privacidad y errores

APROBADO para integración. Diff completo revisado a22d4e: sesión y cierre reales, nota visible,503 inyectado explícitamente en transporte, retirada de lista y nota, reintento del mismo GET,404 real de contexto inexistente y401 real tras retirar cookies. Se comprueban login heredado y recuentos durables intactos.

El waitForResponse de revocación ahora se espera y comprueba: la revisión previa388f6b detectó la promesa sin consumir antes de ejecutar. No se cambió producción. Primer resultado GREEN094109,1/1 y RUN_EXIT=0; cuatro hashes de freeze contrastados. Snapshot241a2f0 incluye UI final y PG nominal: no se atribuye al backend posterior.

No acredita contexto de segundo propietario ni respuesta obsoleta retenida. Esa privacidad dispone de pruebas específicas de aplicación/cliente; el recorrido retenido de navegador sigue dentro del trabajo UX pendiente. El global final ejecutará los cuatro E2E sobre el corte integrado; no se repite un foco sólo por copiar backend.
