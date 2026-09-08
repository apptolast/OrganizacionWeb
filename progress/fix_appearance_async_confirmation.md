# Corrección de sincronización de apariencia en CI22

CI34189142299 (PR25) falló sólo @s36 de appearance.test.tsx: dataset.theme
undefined frente a light. Java pasó; frontend tuvo 2294 PASS y 1 FAIL.
RED original preservado fuera del repo en
../deployment-preparation/export22-pr25-ci-failure.log, SHA256
C2C4B74FBCAA70E871F97200B3C60B87D89E4D355B8B06E2EFDDB119D6311045.

Causa concreta: Appearance monta el formulario únicamente cuando snapshot
existe, y el radio deriva del borrador de ese snapshot. Por tanto, no era
un default previo a confirmación. Sin embargo, esperar el radio marcado
sólo observa el commit del formulario. AppearanceProvider aplica dataset,
colorScheme y --accent en un useEffect posterior sobre snapshot. La espera
podía resolver antes de ese efecto, como muestra el fallo remoto.

Arreglo test-only: el mismo waitFor espera radio Sistema marcado y dataset
light. Los asserts de colorScheme, acento confirmado, una sola petición,
URL y ausencia de método de escritura permanecen intactos. No cambia timeout,
fixture, producción ni configuración. Si el efecto deja de aplicar light,
el test sigue fallando al agotar la espera existente.

Validación:
- Focal original sin cambios: GREEN EXIT0 376552; el fallo intermitente no
  se reprodujo localmente y no se inventa RED adicional.
- Suite appearance corregida: 50/50 GREEN EXIT0 a30cbb.
- ESLint focal EXIT0 9eec21 y Prettier check EXIT0 bda081.
- Frontend completo: 2295/2295, 56 suites, EXIT0 664056; 26,22 segundos.
- Los 129 paths capturados de src, test-fixtures y package/lock/vite config
  permanecieron idénticos antes/después de la global. Respecto al corte
  inicial sólo cambia appearance.test.tsx. git diff --check EXIT0.

Logs y snapshots propios: progress/export22_appearance_async_*. La fuente
productiva y las imágenes candidatas no fueron alteradas. No se relanzó CI,
init global, Java, Stryker ni E2E desde esta reparación. Root coordina el
siguiente gate remoto sobre el parche revisado.