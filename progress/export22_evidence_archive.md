# Evidencia adicional de exportación22

`export22_additional_evidence.zip` conserva 482 archivos originales de
pruebas, regresión, mutación y correcciones de CI. Incluye los logs ignorados
y artefactos que aún no estaban versionados, con sus rutas `progress/...`.
El inventario incorporado `evidence-inventory.json` describe cada tamaño,
SHA256 y clasificación. No incluye código productivo ni sustituye los
resultados fallidos por los posteriores correctos.

SHA256 del ZIP:
`DA5E7FA5CFC38A3CCEF2405D95CF9E3C9E80199DC1C2D988775C3A263E3ACE1A`.
Tamaño: 1530661 bytes. Root verificó las 482 entradas contra tamaño y hash
originales antes de mover ningún archivo (8e1b94).

Los originales sueltos se trasladaron con rutas absolutas verificadas a un
directorio propio de deployment-preparation; cada hash volvió a comprobarse
después (ffc77f). No hubo borrado recursivo ni git clean. El manifiesto de
traslado externo es `export22-loose-evidence-relocation.json`.

Para leer una evidencia referida por otra bitácora y que ya no esté suelta,
abrir la entrada homónima de este ZIP o extraerlo en un directorio temporal
nuevo. No extraer sobre el repositorio sobrescribiendo archivos existentes.
Los originales PIT completos también conservan su paquete independiente
`export_persistence_original_evidence.zip` y su manifiesto interno.
