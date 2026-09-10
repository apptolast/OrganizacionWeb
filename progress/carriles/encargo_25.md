# Encargo de carril — feature 25

2 condiciones del veredicto de cierre que NO dependen de ninguna decision del propietario.

## 1. Slf4jWebhookAuditTest.s35_evenAPoisonedCodeCannotPutAUrlOrASecretInTheTrail lleva un javadoc que afirma entregar al sujeto los valores prohibidos, y el cuerpo pasa HTTP_ERROR, UNSUPPORTED_EVENT y CONFIGURATION_ERROR: los seis assertFalse no pueden fallar

**Estimacion del juez:** 15 min · **Bloqueante:** no

Envenenar de verdad los dos parámetros de texto libre del puerto (pasar como código de error y como estado cadenas que contengan https://…?token=abc, whsec_…, v1=… y example.com) para que los assertFalse muerdan; o, si se prefiere no meter basura, borrar el javadoc que afirma lo que no hace. No bloquea porque @s35 sí queda sujeta por las igualdades exactas de formato y por WebhookScheduleTest.s35_neither…, que engancha un appender a ROOT y hace pasar la URL, el secreto y el cuerpo por el sujeto.

## 2. Las mitades «no se resuelve DNS» (@s2:26) y «no se abre conexión saliente» (@s5:80) no tienen oráculo: el resolutor de CreateWebhookTest es un método estático sin contador (:22-30) y la de @s14:198 es estructural porque ManageWebhook no recibe WebhookSender

**Estimacion del juez:** 25 min · **Bloqueante:** no

Dar contador al resolutor de CreateWebhookTest para que @s2 y @s5 afirmen cero resoluciones cuando la intención se rechaza antes de resolver. Para @s14 y la mitad de «conexión saliente» de @s5, dejar escrita la garantía estructural en progress/literales_sin_oraculo_webhooks.md nombrando el colaborador que no existe en el caso de uso, en vez de dejarla implícita.

