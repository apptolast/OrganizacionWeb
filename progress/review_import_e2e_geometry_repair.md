# Reparación de instrumentación geométrica E2E tras importación 23

El global oficial sobre bb0064c terminó EXIT1 con 159 passed, 2 skipped y 2 failed en 21,6 minutos. Los fallos son reschedule-ux y start-work-session-ux: timeout global de 120000 ms, seguido del cierre de página observado por setViewportSize. No se demostró un fallo previo del viewport ni un bloqueo sostenido de PostgreSQL. Los casos de importación y exportación reales pasaron. El global original sigue siendo fallido; no se convierte en verde por sumar el foco posterior.

La geometría original contenía 102 medidas de reprogramación y 124 de inicio de trabajo, que generaban como mínimo 50649 y 53506 llamadas síncronas a expect, respectivamente. La segunda agotó el tiempo justo antes del axe final. El coste de pasos del runner era evitable para valores numéricos ya medidos.

Root aprobó reemplazar sólo siete comprobaciones numéricas por archivo por node:assert/strict. Se conservan operands, tolerancias de 1 px, tamaños de 44 px, mensajes, rechazo de NaN mediante comparación positiva y false estricto para solapamientos. Siguen iguales los 31 anchos, cuatro estados por caso, capturas, axe, locators y timeout de 120 s. Verificación AST completa contra la transformación prevista: ninguna otra diferencia semántica. Formato y node --check correctos.

Foco oficial de sólo esos dos archivos, sobre el mismo producto bb0064c: EXIT0, 2/2 en 17,2 s; casos de 7,9 s y 7,2 s. Se completaron 124 medidas por caso (31 por estado). Las 743 entradas before/after coinciden. PIT de C seguía activo: no se atribuye el resultado a ausencia total de carga. La disminución observada apoya la causa de instrumentación. No se repitió el global completo ni se elevó ningún timeout.

Los stacks propios organizationweb-e2e-12024 y organizationweb-e2e-63548 quedaron retirados, sin contenedores, volúmenes ni redes restantes según sus etiquetas. Fuentes productivas sin cambios.

Paquete portable externo: deployment-preparation/import23-e2e-geometry-repair-evidence.zip. Incluye global original, contextos, geometrías/capturas originales, equivalencia, foco final, before/after y fuentes exactas. SHA256: 3D6AA10FB5CF0931DA6CEAF2F99D855D4F6FA671FE03716E20323F0F6512A347.

SHA256 fuentes finales:
- reschedule-ux.spec.mjs: 8c12b2c9d5487430ba6f91fda8485da11e4e57aa0f43ed26c2bce409598041e4.
- start-work-session-ux.spec.mjs: c47de5646c2a0b4eedc5cd54ccc629db5da79b0d426e662d86f80f6c8a64d5a0.
