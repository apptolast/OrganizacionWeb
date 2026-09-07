# Revisión del smoke de inicio de trabajo

Aprobado para integración. Root revisó el relay y las aserciones en0fc877/2087a6: el servidor confirma201 antes de cortar el transporte; el cliente recupera por su key sin usar la respuesta descartada. El broker real cae y se recupera; se verifica el mismo evento en la cola durable quorum con su binding, once campos y persistencia. Retirar el outbox sólo ocurre después de observar publicación. Tras reinicio se consultan key/active y los conteos acreditan una sesión, cero eventos y ninguna nueva escritura.

El autor registra una ejecución completa inicialmente GREEN, EXIT0 en3e2f05, junto regresión de los seis tipos históricos cubiertos por el script. Root comprobó el hash congelado8afe5c:4CC55B6F31984ACB67F72233F7EC76AF7FB2D83035F3C29A8E2106A073A18C74. No se atribuye una segunda ejecución independiente. TDD, formato, salida y límites constan en tdd_start_work_publisher_smoke.md.

La prueba utiliza contenedores y un corte de transporte controlado; no acredita una caída física ni despliegue productivo. No sustituye PIT, validación global o UX14. No se modificó producción en este refuerzo.
