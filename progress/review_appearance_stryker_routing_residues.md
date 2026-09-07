# Residuos Stryker de integración de sesión y rutas

Original 56D3A57C3393400EFDD98CA463CFB2B0C0E5274D4E878793FBA15EBB49D60122. Root inspeccionó los cuatro supervivientes restantes de estos dos archivos; App y Workspace no tienen residuos.

- 705, session-gate.tsx:36: elimina role=alert del aviso de renovación CSRF. Cambia una propiedad accesible observable; los tests comprueban la acción y la conservación del borrador, pero no este anuncio concreto. Propuesto un refuerzo mínimo en el recorrido existente, sin cambiar producción.
- 707, session-gate.tsx:37: sustituye la cadena vacía del prefijo normal por texto arbitrario. La conducta de recuperación permanece; falta un oráculo de texto exacto de esa rama. Se conserva como límite de copy, no como equivalencia universal.
- 740 y 742, use-session.ts:187: eliminan/reemplazan la aceptación expresa de la ruta raíz. La rama de rechazo redirige precisamente a la misma ruta raíz, por lo que no cambia el destino visible ni permite otra ruta privada. Puede provocar replaceState/popstate adicional; no se afirma equivalencia de todos los efectos internos ni del estado arbitrario del historial. El contrato de retorno de apariencia sí está cubierto y sus mutantes fueron detectados.

No se modifica la configuración ni se excluyen estos mutantes del resultado original. C revisa el cliente y B el estado/formulario antes de decidir un único paquete acotado de refuerzos; no se inicia replay desde esta revisión.
