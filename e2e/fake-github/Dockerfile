# Servicio falso de GitHub, sólo para la pila de E2E. Comparte el espacio de red del backend, así
# que éste lo alcanza por loopback y la lista blanca de GithubApiBase no necesita relajarse.
FROM node:24-alpine
WORKDIR /app
COPY server.mjs ./
USER node
EXPOSE 9000
CMD ["node", "server.mjs"]
