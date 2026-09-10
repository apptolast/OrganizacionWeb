# Ramas que no están integradas, y por qué está bien

A 10 de septiembre de 2026, 21:00. Escrito porque un `git branch` muestra dos
ramas por delante de `main` y eso parece trabajo perdido. **No lo es.**

## `claude/gh-resto` — pruebas de una feature retirada

Commit `7ddc0f68`: oráculos para el `@s41` del conector de GitHub, el borrador
sin guarda y el 503 al conectar. Trabajo correcto y bien hecho.

**No se integra porque la feature 27 se retiró del producto** esa misma tarde
(ratificación R8). Sus pruebas no tienen nada que probar. Se conserva la rama en
vez de borrarla por si alguna vez se quiere reconstruir esa feature: el commit
existe y es autocontenido.

## `claude/reschedule-frontend-api-checkpoint` — un corte de referencia

Commit `d0e83bb1`, de hace cuatro días: «wip: preserve the stopped frontend
client as a reference cut». Es un **archivo deliberado** de un cliente de frontend
que se dejó de usar, guardado como referencia. La feature 13 (reschedule) está en
`done` desde entonces.

## Cómo distinguir esto de trabajo parado de verdad

Una rama por delante de `main` es sospechosa **sólo si** su feature sigue viva y
su carril ya no está trabajando. Las dos de aquí fallan la primera condición: una
pertenece a una feature retirada y la otra a una cerrada hace días.

Si aparece una tercera, la comprobación es: mirar a qué feature pertenece su
diff, y si esa feature está en `in_progress` en `feature_list.json`, entonces
sí hay que integrarla o declararla abierta con su razón.
