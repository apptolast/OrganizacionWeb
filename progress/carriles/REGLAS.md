# Reglas de operación de los carriles paralelos — 9 de septiembre de 2026

Cinco carriles trabajan a la vez en worktrees aislados. Estas reglas existen
porque el 8 de septiembre siete carriles simultáneos dieron **rendimiento cero**:
la máquina se saturó con 130-170 contenedores de Testcontainers vivos.

## 1. Nunca ejecutes la suite completa

`bin/harness test`, `node scripts/project.mjs test` y `gradlew test` sin filtro
levantan **49 contenedores PostgreSQL** (hay 49 clases de test con su propio
`PostgreSQLContainer` estático). Está **prohibido** ejecutarlos.

Backend, siempre por clase concreta:

```
cd <tu worktree>/backend
"C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/backend/gradlew.bat" test \
  --tests "com.apptolast.organization.adapter.webhook.JdkWebhookSenderTest" \
  --no-daemon
```

- Ruta **absoluta** a `gradlew.bat`: `NoDefaultCurrentDirectoryInExePath=1` impide
  a cmd.exe encontrarlo en el directorio actual.
- Nunca más de **dos clases** por invocación.
- Prefiere clases de dominio/aplicación puras (sin contenedor) cuando puedas.

Frontend, por fichero:

```
pnpm --dir frontend exec vitest run src/webhooks.test.tsx
```

## 2. E2E: tu puerto y solo tu spec

Cada carril tiene su `E2E_WEB_PORT` asignado en su brief. Levanta la pila con él
y ejecuta **solo tu fichero de spec**:

```
E2E_WEB_PORT=<el tuyo> pnpm test:e2e -- e2e/tu-spec.spec.mjs
```

Baja la pila al terminar. No dejes stacks vivos entre ciclos si no los necesitas.

## 3. TDD estricto y acreditado

Cada hallazgo se cierra **demostrando el rojo primero**: rompe la producción a
mano, comprueba que la prueba nueva falla por el motivo correcto, restaura, y
comprueba el verde. Anota en tu bitácora la evidencia de cada rojo (qué rompiste
y qué mensaje dio). Un oráculo que no puede fallar no vale.

## 4. Commit por ciclo

Commitea **cada** hallazgo cerrado, con mensaje que cite el identificador del
hallazgo y el escenario. Nada de dejar el trabajo sin commitear: si la sesión se
corta, lo que no está commiteado se pierde.

Al final de cada mensaje de commit:

```
Claude-Session: https://claude.ai/code/session_015qbcPcK65swQCKhiiz479f
```

## 5. Escribe en disco, no en el chat

Tu bitácora vive en el fichero que te indica el brief. Devuélveme **una sola
línea** con la ruta y el recuento (cerrados / abiertos). El contenido va al
fichero.

## 6. Ficheros compartidos

Estos ficheros los tocan varios carriles y provocan conflictos en la
integración. Anótalo en tu bitácora si los tocas, y hazlo con el cambio mínimo:

- `frontend/src/App.tsx`, `frontend/src/navigation*`
- `scripts/project.mjs`, `harness.config.json`
- `project-spec.md`, `feature_list.json`
- `docker-compose.yml`, `scripts/e2e.mjs`
- `.github/workflows/*`

Tu propio `features/<nombre>.feature`, tus `frontend/stryker.<feature>.config.json`,
tus specs de E2E y tus ficheros de `progress/` son tuyos: úsalos sin preguntar.

## 7. Herramientas de edición, no heredocs

Escribe los ficheros con Write/Edit. Los heredocs corrompen los escapes de
iCalendar, de las plantillas y de las expresiones regulares.

## 8. Contrato

Si un hallazgo exige cambiar el `.feature`, cámbialo y **razona el porqué** en la
bitácora: el contrato manda sobre el código, pero una fila que el código
contradice hay que alinearla explícitamente, no silenciarla.

## 9. Ámbito

Cierra los hallazgos de tu brief y nada más. No refactorices de paso. Si
encuentras un defecto fuera de tu ámbito, anótalo en la bitácora y sigue.
