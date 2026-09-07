# Revisión del frontend de historial

APROBADO para integración y preparación del scope de mutación, sujeto a UX real y gates finales. Diff de producción revisado en 92c1e9 y 083237, pruebas en 0a2804. El cliente y los tres exports compartidos conservan el checkpoint aprobado; no cambia su lógica. Los detalles existentes reciben únicamente enlaces dentro del contexto autorizado. History incorpora contexto vacío sin lectura adicional, enlaces de filtro, advertencia de empate sin causalidad y guardas de foco ligadas al control iniciador.

Se conserva el resultado únicamente para route/refresh vigentes y se aborta la lectura retirada; las pruebas cubren JSON y HTTP antiguos, retirada de notas privadas y recuperación GET. El foco pasa al encabezado cuando desaparece el iniciador, pero se cancela esa intención si la persona escoge otro control. El recorrido físico completo de teclado queda para C.

El rango de fechas invertido señalado en revisión se corrige localmente: mensaje asociado a Hasta, sin GET inválido ni cambio de cursor; una corrección permite aplicar. RED58da38 y GREENd47b5d, 30 pruebas History. Formato, lint y tipos finales e2193a. La regresión previa154/154 se conserva con su alcance anterior: no se presenta155 como una nueva ejecución conjunta.

Manifiesto de14 archivos BC993D1C1EB16C1726AE1095ABDFCC7915868125750A0F13484E5DAF94306BE6. Se contrastaron todos sus hashes antes de versionar. SCSS sigue limitado a conservar notas; no acredita44px, responsive, zoom ni treinta principios por sí solo. C realizará esa validación y cualquier corrección tendrá evidencia propia. No se marca18 terminada.
