# Revisión del smoke de ampliación

APPROVED para el recorrido de recuperación y publicación de los escenarios @s23/@s24. No acredita la interfaz ni el cierre global de la funcionalidad.

Revisión del diff completo: 932596. Verificación independiente fb1437: 249 hashes actuales coinciden con ambos manifiestos finales; 13 PASS. Ejecución del agente ac63df EXIT0. El recorrido conserva las assertions anteriores y añade respuesta perdida después de 201, segunda ampliación, cierre real, reinicio de API, recuperación original por C/K y replay sin nuevas escrituras. Rabbit conserva evento original de once campos, persistencia, quorum y routing tras reinicio. La fórmula se contrasta con PostgreSQL.

Script final SHA256: 4A0A2E1F7AFA1DB37B644D30D11A8E6D5E01C7449BA4E9A50ADDBF7C9BFC0D36. El texto estático de las etiquetas produce el mismo hash de log que la primera ejecución parcial; la bitácora distingue ambos resultados y conserva su evidencia. El primer recorrido abierto no se reinterpreta como cerrado.
