# Revalidación de la feature 27 `github_connector` — carril D

**Motivo**: el dictamen `progress/judge_github_connector_final.md` dictó REJECTED por **un solo
bloqueante ajeno al artesano**: `e2e/github-connector.spec.mjs:168` afirmaba
`String(1 + 12 + TOKEN.length + 16)` (49 octetos) contra un formato en reposo que el merge de la
unificación de `SecretCipher` (features 27 y 28) dejó en 48 al retirar el byte de versión.

El juez cerró su veredicto con una promesa explícita (`:340`): *«Cuando salga verde, esta feature
está aprobada por mi parte: no tengo ninguna otra objeción pendiente.»* Lo que falta, por tanto, no
es cobertura ni diseño: es **una campaña verde medida sobre el árbol de hoy**, porque la anterior se
midió sobre un formato en reposo que ya no existe.

- Worktree: `C:/Users/vhurt/ow-worktrees/github-connector`, rama `claude/github-connector`.
- Base: `c5f9f93` (main del 9-sep, con las features 25, 26, 28 y 30 integradas).
- `E2E_WEB_PORT` asignado: **18096**.

---

## Paso 1 — Barrido: ¿queda algo que asuma 49 octetos o el byte de versión?

`d418a5d` («fix(e2e): el texto cifrado del conector mide 48 octetos, no 49») **es ancestro de
`HEAD`** en este worktree, comprobado con `git merge-base --is-ancestor d418a5d HEAD`. La corrección
está, pero el encargo era barrer el árbol entero, no fiarse de un commit.

### Lo que se barrió

Tres barridos sobre `e2e/`, `backend/src/` y `frontend/src/` (excluyendo `backend/build/`, que es
salida de compilación):

1. `octet_length` en `.mjs`, `.java`, `.sql`, `.ts`, `.tsx`
2. `token_ciphertext` / `tokenCiphertext` (la columna y su acceso, todos los puntos)
3. `byte de versión` / `version byte` / `VERSION_BYTES` / `1 + 12` / `1+12`

### Resultado, punto por punto

| Punto | Qué afirma hoy | Formato | Veredicto |
| --- | --- | --- | --- |
| `e2e/github-connector.spec.mjs:170` | `String(12 + TOKEN.length + 16)` = **48** | sin byte | **correcto** |
| `backend/.../AesGcmSecretCipher.java:51` | `new byte[NONCE_BYTES + sealed.length]` = 12 + n + 16 | sin byte | correcto (es el productor) |
| `backend/.../AesGcmSecretCipherTest.java:38` | `assertEquals(12 + TOKEN.length() + 16, sealed.length)` | sin byte | correcto |
| `backend/.../GithubConnectorWiringTest.java:46` | `hasSize(12 + "ghp_secreto123".length() + 16)` | sin byte | correcto, y es **literalmente** lo que pide @s1 |
| `backend/.../GithubConnectorPersistenceTest.java:150,154` | `hasSize(43)` y `octet_length = 43` | **indiferente al formato** | correcto, ver nota |
| `backend/.../ConnectGithubTest.java:60-62,130,161-175` | identidad, no longitud (`decrypt` de ida y vuelta, AAD del propietario, nonce nuevo por escritura) | indiferente | correcto |

**Nota sobre la persistencia**: `GithubConnectorPersistenceTest` no usa el cifrador real. Su
`ciphertext(marker)` (`:99-103`) fabrica un `byte[43]` con un marcador en la primera posición. El 43
no es «12 + n + 16» de nada: es un relleno arbitrario que cae dentro del `CHECK` de la tabla, y su
oficio es distinguir la fila A de la fila B en las pruebas de reemplazo (`:174`, `:187`). El cambio
de formato **no le afecta**, y es correcto que no le afecte: esa clase mide persistencia, no cifrado.

### Un hallazgo del barrido, ajeno al bloqueante

`backend/src/main/resources/db/migration/V25__github_connector.sql:4-6` — el **comentario** de la
migración sigue describiendo el formato viejo:

    -- es 1 byte de versión de clave + 12 de nonce + texto cifrado + 16 de etiqueta.

Es el mismo territorio del hallazgo menor 5 del juez (la cota inferior del `CHECK`), y la aritmética
confirma su sospecha:

| | formato viejo (1+12+n+16) | formato de hoy (12+n+16) |
| --- | --- | --- |
| token de 1 carácter | 30 | **29** |
| token de 255 (máximo de `PersonalAccessToken`) | 284 | **283** |
| `CHECK` escrito | `BETWEEN 30 AND 284` | sin tocar |

La cota alta (284) es hoy **inalcanzable**, y la baja (30) **excluye un token de un solo carácter**,
que el dominio sí admite: `PersonalAccessToken` sólo acota el máximo (255) y exige ASCII sin
espacios. Un token de un carácter produciría 29 octetos y la fila sería rechazada por el `CHECK` en
vez de por el dominio.

**No lo he corregido, y aquí está el porqué.** En este repositorio las migraciones de Flyway **no se
reescriben nunca** una vez creadas: `git log` sobre `V25__github_connector.sql` y sobre
`V23__webhooks.sql` devuelve **un solo commit cada una**, el de su creación. Reescribir el fichero
—aunque fuera sólo el comentario— cambia la suma de comprobación de Flyway y rompe cualquier base ya
migrada. La corrección correcta es una migración nueva (`ALTER TABLE ... DROP CONSTRAINT` /
`ADD CONSTRAINT` con `BETWEEN 29 AND 283`), y el siguiente número de versión lo están consumiendo
otros carriles ahora mismo: crearlo aquí garantiza un conflicto de integración por un defecto que el
propio juez calificó de **menor y no bloqueante**. Queda escrito para el orquestador, con el número
recalculado, para que lo tome el carril que abra la siguiente migración.

**Conclusión del paso 1**: no queda en el árbol **ningún** punto que asuma 49 octetos ni el byte de
versión, salvo el comentario de la migración descrito arriba —que no es una aserción y no puede
hacer fallar ninguna prueba— y `AesGcmWebhookSecrets`, que es el cifrador **de la feature 23**
(webhooks) y tiene su propio formato con byte de versión por diseño: no comparte columna, ni tabla,
ni contrato con el conector.
