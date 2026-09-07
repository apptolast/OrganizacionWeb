# Recuperación DNS de la web al arrancar sin API

Estado: implementación y regresión operacional completas; pendiente de revisión independiente e integración por root. No se ha desplegado este cambio.

## Alcance y baseline

Checkout aislado `OrganizacionWeb-web-dns`, base `4d34b9c`. Root confirmó el init exacto de CI `34144139336`, paso `Run node .harness/harness.mjs init`, SUCCESS a las 16:43:36Z del 7 de septiembre de 2026 (evidencia `6b4229`). Se heredó ese gate con autorización; no se ejecutó ni se atribuye un init local. Backend20 permanece congelado y ajeno a este paquete.

Ponytail full y Caveman lite: usar resolución dinámica nativa de Nginx, sin entrypoint, bucle de espera ni dependencia nueva. La propuesta aprobada está fuera de Git en `work/deployment-preparation/organizationweb-cold-start-proposal.md`.

## Ciclo real

1. Se añadió `scripts/web-dns-smoke.mjs` antes de modificar Nginx. Comando reproducible: `node scripts/web-dns-smoke.mjs`. Construye el Dockerfile web real y crea exclusivamente imagen, red y contenedores con UUID propio, puerto loopback dinámico y credenciales sintéticas.
2. RED sobre la configuración estática original: EXIT1 `cdcaff`, log `progress/web_dns_red.log`, evidencia `.build/web-dns/owdns-9ed1b45a-a9bb-40eb-8ca9-08b95e4dbec5/`. Nginx salió con código1 antes de existir backend; `web.log` contiene `host not found in upstream "backend"`. El fallo observado fue arranque, no un timeout de la API.
3. Cambio mínimo: upstream con `zone`, resolver Docker `127.0.0.11`, `valid=5s`, `resolver_timeout 2s` y `server backend:8080 resolve`. `proxy_pass` conserva ausencia de URI; las cabeceras, `proxy_next_upstream off`, healthcheck y timeouts existentes no se modifican.
4. GREEN con el mismo oráculo: EXIT0 `a9f98b`, log `progress/web_dns_green.log`, evidencia `.build/web-dns/owdns-742a5d76-340f-4214-b356-9f467bae1ceb/`. Inicio 17:00:48.608Z, fin 17:01:04.996Z. Imagen construida `sha256:1435e0565e3517825418ea8ca675e4bf17622dee978023645397b18e1adc4da3`.
5. Formato posterior del script con Prettier, sin cambio funcional. `node --check`, `git diff --check` y Prettier check verdes (`01f7fa`, `a1fe21`). No se repitió el ensayo por formato. Listas Docker de recursos `owdns-*` vacías después del cleanup (`01f7fa`).

## Oráculos acreditados

- Sin backend ni su nombre DNS, la web permanece disponible: `/healthz`204, HTML200 y `/api/session`502.
- Backend que aparece después: respuesta200, URI y query exactas, cuerpo POST exacto, cookie y CSRF sintéticos, Host y cabeceras forwarded preservados. Un único POST recibido.
- Reemplazo real de IP: se reserva la dirección anterior `172.18.0.3` y el nuevo backend recibe `172.18.0.4`; el proxy recupera200 del nuevo destino. ID, instante de arranque, PID del contenedor y contador de reinicios de la web permanecen iguales.
- Los cinco recursos finales propios se retiraron; el resultado conserva sus nombres. Los logs y resultados originales RED/GREEN permanecen locales. El manifiesto de este paquete registra sus SHA256.

## Límites

Se prueba Docker bridge con su DNS embebido y backend HTTP de eco, usando la imagen web de producción y sus restricciones UID101/read-only/cap-drop. No es un ensayo Swarm, TLS, login real ni de caída de conexión durante un POST. El contador acredita el POST nominal único; la preservación textual de `proxy_next_upstream off` mantiene la política previa. No se ejecutaron SSH, publish ni operaciones remotas. No se atribuye mutación Java/TS a Nginx; el contraste RED es la configuración anterior ejecutada realmente. La suite global heredada acredita el baseline exacto, no este diff.

## Referencias primarias

- [Nginx upstream resolve](https://nginx.org/en/docs/http/ngx_http_upstream_module.html#resolve): resolución dinámica disponible en open source desde1.27.3; requiere zona compartida y resolver. El Dockerfile usa Nginx1.30.4.
- [Docker DNS services](https://docs.docker.com/engine/network/#dns-services): DNS embebido127.0.0.11 en redes personalizadas.
- [Nginx proxy_pass](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass): preservación de URI cuando no se añade una URI al destino.

## Ratificación tras revisión root

Root aprobó el cambio Nginx y pidió ajustar el oráculo al montaje tmpfs actual de Swarm. Se sustituyeron las dos opciones `--tmpfs ...mode=1777` por `--mount type=tmpfs,destination=...,tmpfs-size=16777216`, sin sobreescribir el modo. Se añadió únicamente `/.build/web-dns/` a `.gitignore`. Un fallo de cleanup ahora también establece `report.status = "FAIL"`, además de EXIT1.

Reejecución única pertinente: `node scripts/web-dns-smoke.mjs`, EXIT0 `536da8`; log `progress/web_dns_swarm_mount_green.log`, raw `.build/web-dns/owdns-b35e1316-ed5c-48c6-a871-7fa2d31a89b1/`. Los tres oráculos completos vuelven a pasar con el montaje tmpfs solicitado. Los resultados anteriores se conservan y no se reinterpretan como esta ejecución. Nginx permanece con el mismo hash aprobado. No se simula un fallo de cleanup; su guardia se revisa en el diff.
