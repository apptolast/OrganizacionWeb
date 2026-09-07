# Stryker original 20

## Resultado original preservado

Comando autorizado: `node scripts/project.mjs mutate appearance-frontend`.
Sesión 79876, EXIT 0 comprobado en f0a339. Inicio local 2026-09-07 20:22:43;
final 20:46:25, duración reportada 23 minutos y 43 segundos.
Dry run: 883 tests GREEN en 3m27s. No representa la regresión completa:
el gate previo separado fue 2046 tests/44 archivos.

**747 mutantes: 637 Killed, 102 Survived, 8 NoCoverage, cero Timeout y
cero errores. Score original: 637/747 = 85,27443105756359 % (85,27 %).**
Supera el umbral global 80. El Provider tiene 75,18 % individual: el
umbral configurado es global, y ese resultado inferior no se oculta.

| Fuente | Total | Killed | Survived | NoCoverage | Score |
| --- | ---: | ---: | ---: | ---: | ---: |
| appearance-api.ts | 262 | 226 | 36 | 0 | 86,26 % |
| appearance-state.tsx | 137 | 103 | 28 | 6 | 75,18 % |
| appearance.tsx | 213 | 177 | 34 | 2 | 83,10 % |
| App.tsx | 37 | 37 | 0 | 0 | 100 % |
| session-gate.tsx | 17 | 15 | 2 | 0 | 88,24 % |
| use-session.ts | 76 | 74 | 2 | 0 | 97,37 % |
| workspace.tsx | 5 | 5 | 0 | 0 | 100 % |

## Alcance e integridad

Siete fuentes, nueve selectores aprobados; ocho workers, perTest, todos
los candidatos de vite.config.ts, sin exclusiones añadidas. Rango final
Workspace 74:10–79:22 verificado tras mover el mismo enlace de navegación.
Configuración SHA256:
`5CA42254882AAE51702093BE0A28450C1E765E3711265DB3FFB5CF7115E60625`.

Comparación 7a6d41: **119/119 inputs idénticos**, ninguna nueva fuente
frontend/src. Los manifiestos before/after tienen ambos SHA256
`70BA634628612859D779FADE72E857D10DD526A5F6DD357C1552949192C3FB3C`.
No hubo edición de producción/tests/config durante esta campaña. El
checkpoint Git b430741 de root no alteró sus bytes.

El log no emitió progreso intermedio durante la mutación. Una comprobación
acotada c18cd0/dd2ad5 verificó ocho runners con CPU creciente y 21,65 GB
libres; no se canceló ni reinició la campaña.

## Evidencia durable

- appearance_stryker_original/mutation.json, original completo, SHA256
  `56D3A57C3393400EFDD98CA463CFB2B0C0E5274D4E878793FBA15EBB49D60122`.
- appearance_stryker_original/mutation.html, SHA256
  `71C8385A47DC72842D65DB3CDD326B83259452572D30EEA9A639BCBDD73FB0BF`.
- appearance_stryker_original/run.log, SHA256
  `9A0D14E3CD86993DD2ABE131212E8B4E5852C4000C1381E6B4ECFE1B6072335F`.
- appearance_stryker_original_before.json y _after.json: inputs.
- appearance_stryker_original_inventory.json: 747 estados y firmas por
  archivo/rango/mutador/reemplazo, ID local y fragmento original.
- appearance_stryker_original_residues.json: 110 residuos sin reclasificar,
  incluidos ocho NoCoverage.
- appearance_stryker_original_artifacts.json: tamaños y hashes de raw,
  manifiestos, inventarios y configuración.

## Revisión de residuos

Revisión distribuida por root, pendiente de consolidación: B revisa
Provider/Form (62 S + 8 NC), C API (36 S), root SessionGate/use-session
(4 S). No se infiere equivalencia por pasar el umbral, ni se convierten
los 110 residuos en una matriz automática de nuevas pruebas. Cualquier
refuerzo o replay requiere primero un hallazgo público concreto y review.
No se ha ejecutado replay ni cambiado producción tras esta medición.

Esta campaña no mide SCSS, geometría, contraste renderizado, persistencia
PG, acceso remoto ni E2E: conservan sus gates y dictámenes separados.

### Convención de firmas

Las posiciones del JSON original son líneas/columnas 1-based, extremo
final exclusivo. Los rangos AST de configuración usan columnas 0-based.
El inventario conserva locations originales y resta uno sólo al extraer
el fragmento: corrección documental 25ae54, sin cambio de raw ni estados.

### Clasificación recibida: API y routing

- C: `review_appearance_stryker_api_residues.md`, los 36 supervivientes
  enumerados. Prioriza defaults/revisión estrictos (235/238/243/284),
  gramática HEX (73/77/78), vector público de contraste (61) y problemas
  HTTP400 incompatibles (154 y grupo). Son huecos de oráculo, no defectos
  demostrados en las guardas actuales. 59 tiene argumento exacto de
  dominio HEX8bit; 58/86 permanecen límites no demostrados equivalentes.
- Root: `review_appearance_stryker_routing_residues.md`, cuatro S.
  705 elimina role=alert de renovación CSRF, diferencia accesible pública.
  707 conserva un límite de copy. 740/742 mantienen destino raíz por
  fallback, pero pueden añadir replaceState/popstate: no equivalencia
  general de todos los efectos.

Ambos dictámenes mantienen los estados originales. La selección final
acotada de refuerzos sigue pendiente de root y de la lectura B de UI/estado.

### Clasificación recibida: Provider y formulario

B cerró `review_appearance_stryker_ui_residues.md`: 70/70 residuos
inventariados, 62 S + 8 NC, sin faltantes (09e8eb). Prioridades concretas:
retirada de escuchas SYSTEM después de desmontar (441/443), invalidación
GET antes de completar PUT (352), selector oscuro y descripción accesible
(623/620/633/527, con mensaje local642), query matchMedia exacta (412).
Los ocho NC quedan expresos: 311/313/314/316 son defensas del contexto
por defecto sin Provider; 351/377 mensajes de guards internos; 623/642
son interacción/error oscuro local no ejecutados. No se restan del total.

Hay otros límites públicos documentados (foco/tabIndex, mensajes de otros
campos, aviso al abandonar, previews y referencias ARIA), además de
redundancias locales razonadas. Ninguna de ellas se usa para recalcular el
score original ni para afirmar equivalencia general. Los tres dictámenes
cubren los 110 residuos; no hay defecto del producto original demostrado
en estas lecturas. Root selecciona el paquete acotado de refuerzos y un
posible replay posterior, siempre separado de la medición original.
