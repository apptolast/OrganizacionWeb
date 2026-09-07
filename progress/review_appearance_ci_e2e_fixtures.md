# Corrección de dos fixtures E2E tras CI23

CI original34152171279:141 PASS/2 FAIL. Log preservado
`appearance_ci_initial_failed.log`, SHA
`F3B3CBBDCAD917A81C4DE0944C3E5B7C5180D04725519EC85F74EEF1FAEF1C05`.
Sin cambio productivo ni de CI, skip o aumento de timeout.

## Captura de zoom nativo

Linux CI falla en page.screenshot fullPage antes de alcanzar captura CDP:
`Protocol error (Page.captureScreenshot): Unable to capture screenshot`.
Ese PNG ya era redundante y se había documentado recortado en Windows.
Se retira únicamente esa llamada de cuatro líneas. Se conservan extensión,
zoom API2, DPR duplicado,320px CSS, geometría44px/límites, axe íntegro,
captura CDP del viewport y cleanup. Los PNG históricos no se borran.
La causa RED procede de Linux CI; una repetición Windows sólo acredita
el foco local y no sustituye la repetición Linux de CI.

## Historial de cinco fuentes

RED local reproducido EXIT1 b5325e en
`appearance_ci_history_red.log`: el Provider realiza legítimamente también
GET /api/v1/me/appearance. Stack40748 retirado. El fixture anterior exigía
únicamente GET /api/v1/history pese a la integración20.

Se espera el200 de apariencia y se compara la lista exacta de peticiones
ordenada por ruta: un GET history y un GET me/appearance. No se filtran
peticiones nuevas, no se admiten escrituras/duplicados ni orden temporal fijo.
Los diez hechos, detalles públicos, nota segura, enlace a sesión y SQL que
comprueba ausencia de escrituras se conservan.

Hashes anteriores: history898A9E16…69BED1;
audit04CC8D83…13C81. Diff limitado a ambos archivos E2E.
Validación focal posterior:
- History1/1 GREEN, EXIT0 83188b, log `appearance_ci_history_green.log`;
  stack58688 retirado. Oráculo exacto de las dos consultas y10 hechos pasa.
- Zoom1/1 GREEN Windows, EXIT0 4c36d6, log `appearance_ci_zoom_green.log`;
  stack18284 retirado. Zoom2, DPR1.5→3, ancho320, controles y axe pasan;
  se produce `zoom200-native-viewport.png` mediante CDP. Linux pendiente de CI.
- Prettier check de ambos archivos verde. No suite global repetida ni skips.

Hashes finales:
- history `A285CB7DFA4FF208AEC6BB91982A420B207CB58495E5D6194E724F80C8BD4E00`.
- audit `A21FDB76752F787D2E44F6F654A09025175FB3C85FB87C8CE49BF0D7D959F62C`.

18080 libre; no procesos propios pendientes. Freeze para revisión de root,
sin commit propio y sin afirmar todavía que la nueva CI Linux sea verde.
