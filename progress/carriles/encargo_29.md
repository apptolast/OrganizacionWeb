# Encargo de carril — feature 29

4 condiciones del veredicto de cierre que NO dependen de ninguna decision del propietario.

## 1. B6 se declaro CERRADO y solo se cerro la mitad. GitlabConnectorController$ConnectionResponse.lastActivityAt:291 sigue vivo con NullReturnVals: el accesor puede devolver null y la suite entera sigue verde, de modo que @s9:105 («lastActivityAt igual al instante de la conexion») y @s15:188 no tienen oraculo por valor.

**Estimacion del juez:** 30 min · **Bloqueante:** si

Ampliar el oraculo de frontera en GitlabConnectorApiTest para afirmar $.lastActivityAt POR VALOR contra el instante del reloj fijo en la conexion, y contra finishedAt del recibo en la ruta de @s15, igual que se hizo con ImportResponse. Comprobar despues que el mutante muere volviendo a correr el ambito de backend.

## 2. Los dos VoidMethodCall vivos sobre rejectQuery(parameters) en GitlabConnectorController.delete:92 y startImport:105 borran la guarda de parametros de consulta del DELETE y del POST /imports y la suite entera sigue verde. El unico oraculo, s31_aqueryStringOnAWriteRouteIsRefusedBeforeReachingTheUseCase, prueba solo el PUT, y @s31 no tiene ninguna fila de query string en sus ocho ejemplos: es produccion que ningun test exige.

**Estimacion del juez:** 30 min · **Bloqueante:** no

Ampliar esa prueba a las tres rutas de escritura (o parametrizarla) para que un DELETE y un POST /imports con parametros de consulta se rechacen antes de llegar al caso de uso, y anadir la fila correspondiente a los Examples de @s31 para que la guarda tenga respaldo en el contrato.

## 3. @s15:187 («existen exactamente 2 enlaces ... con url igual a web_url de cada issue») se declara cubierta y ninguna prueba lee esas columnas contra la tabla real: GitlabConnectorPersistenceTest solo hace count("task_external_links") en :188, :214 y :291. Su gemelo de GitHub si lo hace (GithubConnectorPersistenceTest:289, SELECT source, external_id, url, task_id).

**Estimacion del juez:** 25 min · **Bloqueante:** no

Copiar el patron del gemelo de GitHub: en GitlabConnectorPersistenceTest, SELECT source, external_id, url, task_id y afirmar los dos external_id y que cada url es el web_url del issue que le toca. Cubre de paso la columna que PostgresImportedTaskCommit.java:121 escribe y que hoy no afirma nadie en la 29.

## 4. B11 sigue PARCIAL: la rama ratelimit-reset de HttpGitlabIssueSource:180-181 esta en NO_COVERAGE con dos mutantes. No contradice la fila 2 de @s24 —sin cabecera de reset produccion devuelve 60, que es lo que la fila pide—, pero es una rama de produccion que ninguna prueba toca y que decide el plazo de reintento.

**Estimacion del juez:** 30 min · **Bloqueante:** no

O anadir a @s24 una cuarta fila que fije la conducta con RateLimit-Reset presente y su prueba, o que el propietario declare la rama fuera de contrato y se retire de produccion. Cualquiera de las dos mata los dos mutantes.

