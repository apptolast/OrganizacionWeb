# CI de pausa y reanudación

PR14 publicada como borrador para ejecutar la regresión remota mientras terminan las campañas locales. No habilita el merge ni marca15 done. URL:https://github.com/apptolast/OrganizacionWeb/pull/14.

El primer corte ae6e478 figuraba CONFLICTING por el squash previo de14. Root76e849 confirmó que origin/main seguía en353c9d4; no había trabajo externo nuevo. Antes de resolver, comparó el baseline deca25989ebfcadb9c90cfd92a3b93bbe52adc4b con ese main: diff vacío. El merge de historia6b8f360fb5ef932729931b4d91993d3f436655f8 conserva por completo el árbol de la rama, que ya incorpora el contenido de14. Rootca2e4b verificó ambos árboles idénticos0bb179e77ec726a2838c6bb2fe0b1bf972da5800 y ningún cambio de fuente durante las campañas.

El primer comando de diagnóstico de árbol e8eee2 omitió comillas en HEAD^{tree}; PowerShell interpretó las llaves y falló esa consulta. La resolución de historia sí terminó. La consulta corregida ca2e4b verificó explícitamente padres, árbol y diff antes del push. No se atribuye éxito al diagnóstico fallido.

GitHub confirmó MERGEABLE en5a1142, con Application CI34075064014 sobre6b8f360 en curso. El guardián de rutas sensibles pasó; la validación completa aún no tiene resultado. Watch root95413 conserva su salida en ci_pause_resume_pr.log. No se cancelan ejecuciones para publicar cambios meramente documentales.

Resultado posterior: watch95413 terminó EXIT0 (c0bb63). Root585ede verificó SUCCESS sobre6b8f360 completo y conservó el log remoto en ci_pause_resume_pr_complete.log. Init íntegro verde, frontend1666/33, build y108E2E aprobados; el smoke remoto pasó también la pérdida de ACK de PAUSE, RESUME, eventos persistentes y recuperación C/K después de reiniciar. No se extrapola este resultado a commits posteriores sólo porque pertenezcan a la misma PR.

El corte9fc414e20f04fb8d42c4ecb108a967edac4475d5 también terminó SUCCESS: CI34076019493, verificada por root6d29cf; watch34749 EXIT0 eea5e3. Init completo con1668 pruebas frontend, build,108E2E y smoke de pausa/reanudación verdes. Log remoto preservado en ci_pause_resume_oracles_complete.log. La siguiente publicación sólo incorpora informes, estado done y configuración del replay diagnóstico ya ejecutado/formateado; no modifica el producto ni sus tests.

El corte documental e091e7c falló en CI34076959702 (rootcec5f4; watch55907 EXIT1):1667/1668 frontend. WorkSession@s37 usaba tres respuestas secuenciales, pero el nuevo GETstate15 consumía el503 reservado al refresh de active. La siguiente petición sin respuesta simulada añadía un segundo alert y la consulta genérica podía observar el equivocado. Root9fa2f1 confirmó ausencia de diff productivo/tests entre9fc y e091 y la preparación insuficiente del caso. Se corrige la fixture por ruta y el oráculo, sin rerun ciego ni declarar ese fallo verde. PR14 permanece draft hasta resolver la regresión; propuesta16 queda congelada, sin código nuevo.

La corrección2e492a09a74eb3c64c0fcc1c8176101550141091 pasó CI34077907038 SUCCESS, verificada por root5443f0: init/build,108E2E en6 minutos y smoke completo. Watch99763 terminó EXIT0 (0eaa4c); log preservado en ci_pause_resume_fixture_complete.log. El fallo de fixture queda resuelto y el umbral de mutación conserva su cota revisada85,71%.

Root fusionó PR14 mediante squash tras verificar ese head exacto. GitHub confirmó MERGED a2026-09-07T03:11:50Z en main b2ea1f211068e7d93c74d0a8d7e8717ec04323c3 (bcb02f). La comparación completa entre main y2e492a0 fue vacía antes de reconciliar la historia de la rama16. Merge0cf35fda83207f46e7f946fe545529c5ea18e2ca conserva árbol58e3f515f58285f37b7dcf92ef08d32f964b31b2, idéntico a su primer padre (63ea56); no altera los cambios16 en curso. CI de main34078825723 queda en curso, separada de la CI de PR ya aprobada.
