# Fixture14: fecha prevista duplicada por panel17

Global7009 terminó EXIT1 (root12850a),119PASS/2FAIL de121. Uno de los fallos es start-work-session.spec.mjs:236: tras recarga, plannedEndAt aparece tanto en «Fin previsto:» como en «Fin previsto original:». Error-context real preservado por copia en progress/start_e2e_historical_end_error.md, SHA1010AF02C9671FED21C91A0C8FFD42BDD7C70FA6324C26B72D2967CCCC5FD532 (311ccd).

Cambio mínimo autorizado: ambos selectores time[datetime=plannedEndAt] de este archivo se acotan al párrafo /^Fin previsto:/, conservando instante exacto, toBeVisible y demás oráculos/API/SQL. El selector de línea50 (activa de otra tarea) sí pasó global; se corrige preventivamente porque puede evaluarse antes o después de llegar E. No se afirma un RED propio de esa línea.

rg901432 sólo encontró esos dos selectores y el de pause-resume ya corregido. No se cambian otros tests ni producto. GREEN del ajuste pendiente del global integrado que ejecutará root; sin foco redundante, runner ni campaña propios.

## Verificación integrada posterior

Corrección7bf7a1a acreditada en global96039: EXIT0 real5df5b0 comunicado por root,121/121 en11,3min. Log final línea328: caso116 GREEN (2,2s); caso114 de otra tarea también GREEN (1,4s). SHA448D71B2BBD177BD383DD11A3E31AF07208130CBF4A39D64922158A0208642FA verificado2582af. Test2D092547CB21A14F3E4C0F509131DCBBD13C60708C8E5D0A457A8E35F59C607A coincide con entrega7733c1. El pendiente anterior queda resuelto sin producción nueva ni runner adicional propio; se conserva el RED real y la distinción del selector preventivo.
