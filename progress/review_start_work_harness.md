# Revisión del arnés de mutación14

Aprobado. Despachos revisados en121514, selector Gradle en955445: destinos fijos para frontend/backend, diez patrones Java que incluyen dominio, aplicación, persistencia, HTTP, configuración y publicación compartida. PIT mantiene todos los candidatos JUnit, umbral80, cuatro workers y filtros existentes; informe separado. El default incorpora el código nuevo. La selección frontend cubre API/UI completas y composición TaskReader con extremos comprobados, ocho workers y exclusión protegida conservada.

TDD y formato del autor en tdd_start_work_harness.md;29/29checks finales5ea30a. La prueba del selector estático protege la configuración, no acredita una campaña PIT ejecutada. Backend requiere terminar wiring y congelar fuentes/tests antes de esa campaña.

Stryker frontend sí terminó sobre el corte anterior de scripts:452Killed/533,81Survived,84,8030%, ningún otro estado. Root verificó JSON y88hashes before/after sin diferencias en9cbf53; SHA256 JSON E88C2A6C45207C40D68267162F320E84B9A769CD581F69B21896C8B1260764FF. El selector backend y su test se añadieron después de esa campaña. El inventario de supervivientes conserva todos los residuales; superar umbral no implica equivalencia ni ausencia de errores.
