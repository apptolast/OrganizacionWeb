# Frontera positiva del publicador16

Root autorizó un único oráculo después de la campaña completa: OutboxMessage.validationCode:173 sobrevivió al cambiar año<1 por<=1. AGENTS y Ponytail full/Caveman lite aplicables releídos; no se repite init, conforme a la autorización de foco sobre el corte integrado ya validado.

Se añadió sólo `PublishOutboxTest.closeWork_s29_publishesFirstValidCalendarYear`: evento CLOSE con occurredAt/workDate0001-01-01 UTC y trabajo0; el helper existente exige entrega del mismo evento y persistencia published. Primera ejecución **GREEN6eaa57 EXIT0**, honesta: no hubo modificación productiva ni RED fabricado. Log `close_publisher_year_one_initial.log`.

Regresión `PublishOutboxTest` y SpotlessJavaCheck reales **GREEN273597 EXIT0**:192/192, cero fallos/errores/omitidos, XML preservado en `close_publisher_year_one_xml/PublishOutboxTest.xml`, recuento718d3e. Log `close_publisher_year_one_focal.log`. Test SHA40CC594F0AE2554FA83D7AF125FCE54D3201CAC7980E822A8479EBC584B65283. OutboxMessage conserva E2CC330F2FB778AE6EDFF27CBE508E68673B3CFF3EBBC819ED70B6B4E1CA8F7A. No se atribuye aún eliminación del mutante.

## Replay preparado, sin ejecutar

Root precisó que permite reducir por clase/mutador con infraestructura existente. El init script `close_work_boundary_replay.init.gradle` deriva del build actual: sólo fija OutboxMessage, CONDITIONALS_BOUNDARY y carpeta separada. No cambia dispatcher/build/config global ni exclusiones, selección JUnit, workers4 o umbral80. La API instalada del plugin1.19.0-rc.3 se verificó con javap723842: targetClasses y mutators son SetProperty y reportDir DirectoryProperty. El primer javap sin ruta no estaba en PATH; se utilizó el binario JAVA_HOME, sin instalación ni workaround del runner.

Comando propuesto desde backend, pendiente de revisión root:

```powershell
.\gradlew.bat pitest --daemon '-Dorg.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m' -PmutationScope=close_work_session --init-script ../progress/close_work_boundary_replay.init.gradle
```

Es un replay complementario por clase/mutador, no un filtro exacto de una sola instrucción: puede producir otros boundaries de OutboxMessage. Se inventariarán todos y se verificará expresamente la firma validationCode:173/ConditionalsBoundaryMutator/índice725/bloque115. No sustituye el XML520 original ni cambia sus2S/2NC. Se preservarán hashes antes/después y reporte separado; cualquier fallo del runner se diagnostica antes de reintentar.
