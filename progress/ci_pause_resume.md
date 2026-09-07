# CI de pausa y reanudación

PR14 publicada como borrador para ejecutar la regresión remota mientras terminan las campañas locales. No habilita el merge ni marca15 done. URL:https://github.com/apptolast/OrganizacionWeb/pull/14.

El primer corte ae6e478 figuraba CONFLICTING por el squash previo de14. Root76e849 confirmó que origin/main seguía en353c9d4; no había trabajo externo nuevo. Antes de resolver, comparó el baseline deca25989ebfcadb9c90cfd92a3b93bbe52adc4b con ese main: diff vacío. El merge de historia6b8f360fb5ef932729931b4d91993d3f436655f8 conserva por completo el árbol de la rama, que ya incorpora el contenido de14. Rootca2e4b verificó ambos árboles idénticos0bb179e77ec726a2838c6bb2fe0b1bf972da5800 y ningún cambio de fuente durante las campañas.

El primer comando de diagnóstico de árbol e8eee2 omitió comillas en HEAD^{tree}; PowerShell interpretó las llaves y falló esa consulta. La resolución de historia sí terminó. La consulta corregida ca2e4b verificó explícitamente padres, árbol y diff antes del push. No se atribuye éxito al diagnóstico fallido.

GitHub confirmó MERGEABLE en5a1142, con Application CI34075064014 sobre6b8f360 en curso. El guardián de rutas sensibles pasó; la validación completa aún no tiene resultado. Watch root95413 conserva su salida en ci_pause_resume_pr.log. No se cancelan ejecuciones para publicar cambios meramente documentales.

Resultado posterior: watch95413 terminó EXIT0 (c0bb63). Root585ede verificó SUCCESS sobre6b8f360 completo y conservó el log remoto en ci_pause_resume_pr_complete.log. Init íntegro verde, frontend1666/33, build y108E2E aprobados; el smoke remoto pasó también la pérdida de ACK de PAUSE, RESUME, eventos persistentes y recuperación C/K después de reiniciar. No se extrapola este resultado a commits posteriores sólo porque pertenezcan a la misma PR.
