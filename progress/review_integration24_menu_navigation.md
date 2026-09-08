# Ajuste de navegación E2E para integración API

Revisión root autorizó reproducir y corregir sólo tres posiciones heredadas. Se ejecutó un foco de navegación autenticada real sobre DETACHED 9a15cb5, con el fixture existente, API real y puerto propio 18084. No se repitieron los recorridos completos de exportación/importación ni se interceptó tráfico.

- Original: proyecto UUID 7efdc366-860e-4ff3-b45a-10ecc1f082a9, EXIT 1; tres fallos exactos. Exportación no está en -2, e Importación ya no es última en ambos oráculos.
- Intento intermedio: UUID 7c546d05-cade-423c-b164-70212a551a4b, EXIT 1 por error mío de generación del probe temporal (sustitución textual alcanzó el cierre del locator). No es fallo de producto. Se conserva sin sobrescribir.
- Corrección del probe verificada con node --check, UUID a3bb7425-e94e-4423-9650-9129c9792909: EXIT 0, 1/1 en 4,5 s. Hoy primero; Exportación antepenúltima; Importación penúltima; API para integraciones última.

Cada intento conserva 577 entradas before/after idénticas, logs y EXIT. Inventario posterior de sus propios labels: cero contenedores, redes y volúmenes. Directorios y proyectos UUID distintos. V14 protegido excluido de lectura/hash.

Cambios definitivos sólo en e2e/export-data.spec.mjs e e2e/import-data.spec.mjs: tres selectores posicionales y aserción de última API. Nombres accesibles, navegación y oráculos de negocio intactos. Los probes operativos están únicamente en evidencia; no se añade otra prueba al producto. ZIP adjunto conserva originales y manifiesto de todas las entradas, verificadas. La campaña Stryker activa no recibe cambios de fuentes, tests Vitest ni configuración.