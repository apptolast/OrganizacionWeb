# Refuerzo de los dos NC del publicador17

Autoría acotada después de cerrar la campaña original. Sólo dos casos en PublishOutboxTest, uno cada vez; ninguna producción modificada.

1. `endTime_s25_blocksNonStringPreviousEnd`: previousEndAt numérico con los demás campos del evento válido. Inicialmente GREENace668,1/1. Alcanza el rechazo de tipo de OutboxMessage.validationCode196.
2. `endTime_s25_blocksImpossibleCalendarEndWithValidUtcShape`: effectiveEndAt2026-02-30T10:20:00.123456Z, forma UTC admitida pero calendario imposible. Inicialmente GREENd30adc,1/1. Alcanza el catch207.

Ambos usan stateChangedWith para serializar un payload coherente y assertStartedBlocked para exigir blocked/INVALID_EVENT sin entrega. No se fabrica RED de producción: el contraste rojo pendiente es la medición dirigida de los retornos mutados.

Freeze: Spotless real y PublishOutboxTest completo211/211 GREENe837e3; XML preservado `progress/end_time_publisher_refinement.xml`, SHA A158C46D425CF7DF9BE3F1B037EFDF4A73A74B245BD8FBDC840CE859791FC62E. Test SHA D68C37123F2F3F62829D92E14B0C35A507498FCF3696003B2C74955C920782C8. Diff backend limitado a16 líneas nuevas de ese único test; diffcheck GREENdd8efd. Ningún Gradle activo tras ese foco.

## Replay propuesto, todavía sin ejecutar

`progress/end_time_publisher_replay.init.gradle` reutiliza el mecanismo ya existente de close_work_boundary_replay.init.gradle. Sobrescribe sólo targetClasses literal OutboxMessage, mutator EMPTY_RETURNS y reportDir propio. El scope end_time_notification conserva todos los candidatos JUnit, threads4, umbral80, motor y restantes filtros de build.gradle.kts. No se edita el build global ni se excluyen otras firmas del mutador mediante filtros opacos.

Comando previsto desde backend:

```powershell
./gradlew.bat pitest --daemon -PmutationScope=end_time_notification --init-script ../progress/end_time_publisher_replay.init.gradle --max-workers=4
```

Se preservarán before/after y XML del replay separados. El inventario incluirá todos los EMPTY_RETURNS generados en OutboxMessage, no sólo los dos objetivos. Se exige localizar las firmas originales validationCode196/207 EmptyObjectReturnValsMutator mediante clase/método/línea/mutador; ni el porcentaje general ni kills de otras líneas prueban esas dos firmas. La campaña original permanece616K/4NC/620. Los retornos heredados Store253/259 quedan con la justificación de serialización y recuperación controlada ya documentada; no se amplían pruebas de concurrencia ni se declaran equivalentes.

Root revisará configuración y test antes de autorizar ejecución.
