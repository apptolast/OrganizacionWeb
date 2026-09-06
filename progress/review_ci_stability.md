# Revisión independiente de estabilidad CI

2026-09-06. Dictamen: APPROVED para publicar la corrección y verificar Linux; no acredita CI verde todavía.

Root revisó el diff completo en c24b00 y la región real en project-status-control.tsx en 1b0ea7. Los siete locators de anuncios de estado se restringen a su región accesible. Se conservan los textos exactos, los dos anuncios del editor y todos los oráculos de persistencia, concurrencia, foco y geometría. No se añaden esperas, skips ni selecciones arbitrarias.

El único cambio del workflow inicia Xvfb alrededor del comando E2E existente. Responde al fallo DISPLAY del run34044566475 sin sustituir el zoom nativo ni relajar la prueba. La evidencia local del autor es 4/4 E2E verdes, 38e7b8; Windows no verifica este cambio Linux.

La corrección CSS previa sigue revisada en review_responsive_navigation.md. Integrar PR4 solamente tras Application CI verde en el commit publicado. El usuario autorizó resolver y fusionar las PR y detuvo Claude; ya no existe espera de coordinación externa.

## Separación de la rama de origen

La inspección final de PR4 (2ebe90/d1b312) detectó que partía de un checkpoint frontend y arrastraba código13 incompleto. Se integró main345543c y se restauraron desde main únicamente esos archivos heredados, resolviendo el conflicto documental con la versión actual de main. El código13 permanece conservado en PR1/9ab4784; no se descarta su desarrollo. El diff neto de PR4 ahora contiene sólo styles.scss, workflow, siete locators y cuatro notas de evidencia (a96e0d). No se escribió implementación nueva en esta operación Git.

Al integrar posteriormente main en PR1 hay que conservar expresamente la implementación frontend13 de su rama: esta separación no es una decisión de eliminarla del producto. La CI de bd1fbde no acredita el nuevo corte; se ejecutará sobre el commit separado antes de merge.
