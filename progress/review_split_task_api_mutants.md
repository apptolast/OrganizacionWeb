# Revisión independiente de mutantes del cliente API

El coordinador revisa `tasks-api.ts`, implementado por integration_craftsman. Este último no actúa como juez de su propio módulo. Fuente contrastada con el JSON original de Stryker split_task: 601 mutaciones globales de la campaña, cuatro supervivientes en este archivo. La ejecución original se conserva; no se calcula un score nuevo a partir de un replay posterior.

| ID original | Identidad | Dictamen |
| --- | --- | --- |
| 321 | tasks-api.ts:15, ConditionalExpression, sustituye typeof value === string por true | Hueco de pruebas. isTask llama a sameId con projectId todavía desconocido. Un DTO válido en los otros campos con projectId numérico o null debe producir el error controlado de tarea; el mutante llega a toLowerCase y lanza TypeError. No es equivalente por el hecho de que una vista capture ambos errores. |
| 349 | tasks-api.ts:36, ConditionalExpression, sustituye typeof data !== object por false | Hueco de pruebas. Una respuesta JSON textual de un carácter tiene una clave enumerable; el mutante alcanza el operador in sobre el primitivo y lanza TypeError en lugar de Respuesta de relación inválida. |
| 391 | tasks-api.ts:65, ConditionalExpression, sustituye typeof value !== object por false | Equivalente para valores JSON en los puntos de entrada actuales. Null se rechaza por separado. Otros primitivos tienen un número de claves distinto de ocho, salvo una cadena de ocho caracteres, que no supera el siguiente typeof data.id === string. Se conserva el rechazo controlado. |
| 474 | tasks-api.ts:83, ConditionalExpression, sustituye typeof estimatedMinutes === number por true | Equivalente: Number.isInteger ya rechaza valores no numéricos antes de comprobar los límites. Null conserva su rama opcional independiente. |

Se solicita al autor frontend añadir los casos observables de 321 y 349 con el error exacto esperado y repetir únicamente esos rangos. No se pide modificar producción ni probar directamente helpers privados para forzar un porcentaje.

Verificado por el coordinador: suite final 475/475 y replay separado 56/58. Las identidades originales 321 y 349 corresponden exactamente a 53 y 60 del replay, ambas Killed. Los dos huecos quedan cerrados sin modificar producción. Las equivalencias se limitan a las entradas JSON y consumidores actuales; deben revisarse si cambia el contrato del módulo.
