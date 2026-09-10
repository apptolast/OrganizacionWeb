# Encargo de carril — feature 27

3 condiciones del veredicto de cierre que NO dependen de ninguna decision del propietario.

## 1. El @s41 fila 2 (otra persona inicia sesión y no hereda el repositorio de la primera) está declarado cubierto sin ningún oráculo que pueda fallar: no existe github-connector-draft.test.ts, y la prueba que lleva el nombre de la fila nunca escribe borrador, así que el guardián de propietario nunca se evalúa. Borrar `draft.owner !== owner ||` deja la suite verde.

**Estimacion del juez:** 35 min · **Bloqueante:** si

Escribir frontend/src/github-connector-draft.test.ts cubriendo las tres ramas defensivas sin oráculo: borrador de otro propietario (github-connector-draft.ts:39), repository que no es cadena (:40) y JSON ilegible que entra por el catch (:33-35), afirmando en cada caso que devuelve "" y que la clave queda borrada. Y reforzar la prueba de la fila en github-connector.test.tsx:568: servir 404 para que se pinte el formulario, teclear "octocat/Hello-World" con el primer propietario, remontar con owner="otra-persona" y afirmar getByLabelText(/repositorio/i) toHaveValue("") más sessionStorage sin el repositorio ajeno. OJO: la producción está bien —github-connector.tsx:29 lleva key={owner}, así que el cambio de propietario sí remonta—; lo que falta es la prueba, no un arreglo.

## 2. El 503 GITHUB_UNAVAILABLE que produce el arreglo de M12 no tiene oráculo en la frontera HTTP: el manejador GithubConnectorController.githubUnavailable():215-218 sale NO_COVERAGE en la campaña vigente, y el único 503 que prueban las de API viene de IssueImportFailedException (GithubConnectorApiTest:496-519), no de esta excepción. La prueba de M12 es de caso de uso y sólo afirma la excepción.

**Estimacion del juez:** 20 min · **Bloqueante:** no

Añadir a GithubConnectorApiTest una fila que haga lanzar GithubUnavailableException al PUT de conexión y afirme 503 con código GITHUB_UNAVAILABLE y Cache-Control no-store. Mata de paso el NullReturnVals NO_COVERAGE de :217. Pendiente de ejecución: gradlew.bat :backend:test --tests "*GithubConnectorApiTest".

## 3. saveRepositoryDraft escribe en sessionStorage sin guarda y se llama en CADA pulsación desde el onChange del campo repositorio (github-connector.tsx:396); el módulo del que la bitácora dice estar calcado, import-data-intent.ts:17-19, sí comprueba la escritura. Un setItem que lance —cuota, modo privado— rompe el tecleo de la pantalla, y ninguna prueba lo ejerce.

**Estimacion del juez:** 10 min · **Bloqueante:** no

Envolver el setItem de github-connector-draft.ts:19-22 de modo que un almacén que rechaza escribir degrade en silencio (el borrador es una comodidad, no contrato) en vez de propagar la excepción al manejador de React, y añadir una prueba con sessionStorage.setItem simulado lanzando que compruebe que la pantalla sigue tecleando.

