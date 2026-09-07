# Revisión de corrección visual y refuerzos frontend14

Aprobado para regresión final y nueva medición de mutación. La inspección real detectó una afirmación de ausencia obsoleta durante un POST pendiente o incierto. RED17bf9f lo reprodujo; la autora limitó sólo la presentación de ese párrafo a !busy && !uncertain, sin modificar elegibilidad, key o recuperación. GREENcfe3a8 y UX corregido540246 confirman el comportamiento. Root revisó el diff3665aa y la captura móvil actual; el aparente skiplink superpuesto era artefacto de captura completa, con elemento realmente fuera del viewport.

Los refuerzos de9e28ac comprueban duración POST distinta pero internamente válida, problemas HTTP contradictorios, ausencia retirada tras conflicto de activa, elegibilidad independiente, lookup401 obsoleto/finally y submit durante incertidumbre. Se usan interfaces públicas y se conserva el body del error. El test de Enter diferencia interacción de usuario y evento DOM cancelable; el fallo inicial de ese montaje no se presenta como bug.

85/85 API/UI, lint/tipos/formato verdes según caf80f/d41bb7/a5804b/da3ac3. Root verificó hashes bc4510: APItest8E6C39B5A0594188F44A19F1B8B8AD052676F0DF7066B0F71C0069D65C850485; UItestF65E0F54EEB638741C9E57E6295AB19D500F36E173D2EC6BF469CBEB590075F7; UI47A35F19C2E47E35C633A3D60C0880A1822DBDD5C130ED326177892646C66005. No se repite el backend por este cambio disjunto.

La campaña inicial84,8030% corresponde al corte anterior y conserva sus81residuales. Se ha encargado una nueva campaña tras regresión global frontend, archivando primero JSON/HTML originales sin alterar filtros ni umbral. No se anticipan kills ni score final.
