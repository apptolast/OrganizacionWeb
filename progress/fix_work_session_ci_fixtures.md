# Corrección de fixtures CI por work_sessions

CI PR7 34063271417 detectó limpieza heredada incompatible con la FK nueva. RED local e555e0: AuthenticationHttpTest.s1_anonymousSessionIsPublicExactAndPersisted, SQLSTATE 0A000; work_sessions referencia tasks. No es fallo del endpoint ni se elimina la FK.

Se añade work_sessions explícitamente a las veinte listas TRUNCATE afectadas que migran la versión actual. No se añade CASCADE, no se modifican fuentes productivas ni se escribe un test espejo. Auth nominal GREEN 89ac1f. Formato focal GJF 1.31.0 sólo de estas veinte clases.

Archivos exactos:

- backend/src/test/java/com/apptolast/organization/adapter/BlockChangesApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/AvailabilityApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/AuthenticationHttpTest.java
- backend/src/test/java/com/apptolast/organization/adapter/ProjectStatesApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/ProjectApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/EditProjectsApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/persistence/BlockChangeQueriesPersistenceTest.java
- backend/src/test/java/com/apptolast/organization/adapter/RescheduleErrorsApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/ReadProjectsApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/ScheduleBlockApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/persistence/OutboxWorkTest.java
- backend/src/test/java/com/apptolast/organization/adapter/RescheduleApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/TaskApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/SubtaskApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/persistence/OutboxRecoveryTest.java
- backend/src/test/java/com/apptolast/organization/adapter/TaskHistoryApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/TaskStatusApiTest.java
- backend/src/test/java/com/apptolast/organization/adapter/persistence/ScheduleBlockPersistenceTest.java
- backend/src/test/java/com/apptolast/organization/adapter/persistence/RescheduleCoordinationTest.java
- backend/src/test/java/com/apptolast/organization/adapter/TodayApiTest.java

Regresión tras formato: GREEN 7c5461, 2m15s, veinte casos existentes de veinte clases, uno por fixture afectado. XML confirma cero fallos/errores/omitidos. No es la suite completa de cada clase ni se presenta como init global. Reproduce cada inicialización que fallaba por la misma FK; autenticación se reejecutó dentro del conjunto después de formato. Ningún cambio en SQL productivo ni en el cuerpo de los tests.

Casos ejecutados:
- com.apptolast.organization.adapter.AuthenticationHttpTest.s1_anonymousSessionIsPublicExactAndPersisted()
- com.apptolast.organization.adapter.AvailabilityApiTest.s1_readsUnconfiguredWithoutCreatingPreference()
- com.apptolast.organization.adapter.BlockChangesApiTest.s16_readsConfirmedEmptyBlockChangeHistory()
- com.apptolast.organization.adapter.EditProjectsApiTest.s1_detailIncludesStrongVersionTagFromSameSnapshot()
- com.apptolast.organization.adapter.persistence.BlockChangeQueriesPersistenceTest.s16_s18_readsDurableCancellationWithoutOutboxOrAvailability()
- com.apptolast.organization.adapter.persistence.OutboxRecoveryTest.s12_realAcceptanceSurvivesDatabaseRollbackAndRetryDuplicatesIdentity()
- com.apptolast.organization.adapter.persistence.OutboxWorkTest.s5_failedPublishPersistsRetryAndExcludesSameCycle()
- com.apptolast.organization.adapter.persistence.RescheduleCoordinationTest.s23_projectCompletionFirstRejectsWaitingMove()
- com.apptolast.organization.adapter.persistence.ScheduleBlockPersistenceTest.s12_previewCountsOwnReservationsAcrossCompletedProjectsOnly()
- com.apptolast.organization.adapter.ProjectApiTest.s14_rejectsTrailingJsonDocumentWithoutWrites()
- com.apptolast.organization.adapter.ProjectStatesApiTest.s1_s11_updatesStateAndPersistsOneExactEvent()
- com.apptolast.organization.adapter.ReadProjectsApiTest.s2_emptyListIsPrivateAndHasNoCursor()
- com.apptolast.organization.adapter.RescheduleApiTest.s2_creationReplayAfterTwoMovesReturnsOnlyTheOriginalFact()
- com.apptolast.organization.adapter.RescheduleErrorsApiTest.moveRejectsBudgetExcessWithCurrentDays()
- com.apptolast.organization.adapter.ScheduleBlockApiTest.s1_previewReturnsExactSnapshotWithoutWriting()
- com.apptolast.organization.adapter.SubtaskApiTest.s1_commitsChildRelationshipAndOnlySubtaskEvent()
- com.apptolast.organization.adapter.TaskApiTest.s1_commitsTaskAndMinimalEvent()
- com.apptolast.organization.adapter.TaskHistoryApiTest.s10_readsConfirmedEmptyHistory()
- com.apptolast.organization.adapter.TaskStatusApiTest.s1_readsLegacyPendingSnapshotAndTaskEtag()
- com.apptolast.organization.adapter.TodayApiTest.s1_emptySnapshotHasExactClosedSchemaAndDoesNotWrite()
