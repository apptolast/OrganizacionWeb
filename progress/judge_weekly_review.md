# Revisión final de la revisión semanal

Aprobación funcional y de calidad del corte d065af3. Integración en main y despliegue pendientes de sus controles respectivos; este dictamen no acredita producción.

El contrato mantiene 34 escenarios y 100 ejemplos. La API obtiene un snapshot PostgreSQL consistente antes de leer el reloj y compara plan vigente, intervalos reales y presupuesto actual de siete días. Conserva precisión de microsegundos, límites civiles y DST. No escribe hechos ni eventos. El cliente valida el contrato cerrado y rechaza respuestas obsoletas; la URL conserva la selección aplicada y los reintentos son deliberados.

La revisión exigió corregir duplicados HTTP, estados temporales futuros ocultos, intervalos futuros, áreas interactivas pequeñas y pérdida de foco. El último ajuste SCSS separa 16 px el título: una medida RED mostró 7 px de invasión del contorno; los recorridos finales dejan 9 px libres. Las pruebas conservan tanto el foco elegido como su traslado voluntario al body.

## Evidencia final contrastada

- Java: 2.289 pruebas en 97 suites, XML preservados y manifiesto. Arnés: 51 pruebas. Init reparado EXIT 0 cb996d; build integrado EXIT 0 777610.
- Frontend final: 1.968 pruebas en 42 archivos, EXIT 0 0d4523, incluidas las correcciones de sincronización de fixtures y los seis refuerzos dirigidos. Lint y build finales verdes.
- E2E completo: 136 casos, EXIT 0 b2e8fe. Publisher: 13 recorridos con RabbitMQ, EXIT 0 41112e. El cambio posterior sólo de margen se revalidó en los recorridos UX afectados, sin atribuirle el E2E anterior.
- UX: tres motores, anchuras de 320 a 2560 px, recuperación, texto al 200 % y zoom nativo Chromium al 200 %. Se conservan matriz de los 30 principios y límites de dispositivos físicos, lectores de pantalla y evaluación humana.
- PIT original: 178 KILLED de 190 (93,6842 %), seis supervivientes y seis sin cobertura. Replay de fronteras: nueve KILLED, seis objetivos y tres extras, con 389 hashes intactos. No se recalcula una campaña global con ese subconjunto.
- Stryker original: 615 Killed, 144 Survived, dos NoCoverage, tres Timeout y dos RuntimeError. Killed estricto: 615/764 = 80,4974 %, sin contar Timeout como Killed; el 80,89 % de herramienta tiene otra fórmula.
- Replay frontend: 46 mutantes, 38 Killed y ocho Survived (82,6087 %), sin errores ni timeouts. Se contrastaron raw y 142 hashes antes/después: cero diferencias. De las 19 firmas objetivo, 17 Killed y dos Survived; 27 extras separados. El informe justifica el alcance de cada residuo, sin declarar equivalencia universal ni ocultar la higiene del listener pendiente de cobertura directa.

Los dos fixtures históricos corregidos esperan a que su efecto asigne el callback antes de invocarlo; conservan los oráculos y no amplían timeouts. Los fallos originales locales y de CI permanecen documentados.

La revisión acepta los límites residuales documentados: superan el umbral global exigido y no muestran un incumplimiento funcional pendiente de los recorridos contratados. No se exige perseguir un 100 % de mutación ni se lo declara alcanzado. No cambian migraciones existentes ni el contrato de persistencia.

La entrega de infraestructura publica inicialmente el MVP18 a5d1586. Incorporar esta feature al servidor requiere imágenes nuevas y su revisión de despliegue; la aprobación de código no sustituye esa comprobación.
