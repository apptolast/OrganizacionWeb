# El enlace del formulario de webhooks a la guía de firma

Carril `claude/webhooks-enlace`, 10 de septiembre de 2026. Cierra por el lado del
**producto** el hueco que `project-spec.md:2044` declaraba: la ayuda del
formulario de creación decía enlazar `docs/webhooks.md` y no enlazaba nada.

Esta mañana se cerró por el otro lado —se corrigió el documento y se declaró
falsa la línea del spec—, y el juez de cierre ratificó ese razonamiento: ningún
`@s` pedía el enlace y clavarlo con un oráculo sería «fijar una ruta de
documentación en la interfaz: frágil, y no mata mutantes». El propietario mandó
hacerlo bien por el lado del producto, que es lo que el propio juez dijo que
haría falta: **fila de contrato y oráculo**.

---

## 1. La fila de contrato propuesta, y escrita

Escrita en `features/webhooks.feature`, al final, como `@s43`, **marcada como
pendiente de contrafirma** con su nota fechada, al modo de las enmiendas del
`@s25` y del `@s33`:

```gherkin
  @s43
  Scenario: La ayuda del formulario ofrece la guía pública de verificación de firma
    Given la vista de Webhooks con el formulario de creación visible
    When recorre el formulario con el teclado
    Then la ayuda del formulario contiene un enlace a la guía pública de verificación de firma
    And ese enlace se alcanza con Tab y cumple el foco visible y los 44 px que el @s42 exige a todo control
    And su nombre accesible dice que explica cómo verificar la firma, no «aquí» ni «más información»
    And el enlace apunta a un destino no vacío; el contrato no fija cuál
```

Por qué un `@s` nuevo y no una cláusula colgada del `@s36` o del `@s37`: los dos
describen otra cosa —el estado inicial de la vista y el ciclo de creación con su
secreto—, y la traza `@s → prueba` queda más limpia con un escenario propio que
con un `And` huérfano dentro de un `Scenario Outline` ajeno.

---

## 2. El rojo, acreditado

**Rojo 1 — no hay enlace.** Prueba escrita antes de tocar `webhooks.tsx`
(`frontend/src/webhooks.test.tsx`, `@s43 the creation form links the public guide
on verifying the signature`). `npx vitest run src/webhooks.test.tsx -t "@s43"`:

```
 FAIL  src/webhooks.test.tsx > @s43 the creation form links the public guide on verifying the signature
TestingLibraryElementError: Unable to find an accessible element with the role "link" and name `/firma/i`
```

(El volcado del error imprime el `<form>` entero: URL, Descripción, los doce
tipos y «Crear webhook». Ni un `<a>`.)

**Verde 1.** El mínimo en `frontend/src/webhooks.tsx`: un `<a>` en el párrafo de
ayuda del formulario, con el rótulo «Cómo verificar la firma» y el destino en una
constante (`signatureGuide`). 88/88 en la suite de la vista.

**Rojo 2 — el enlace incumplía los 44 px del `@s42`.** El enlace ya existía, pero
el oráculo de geometría del E2E mide **todo** `main a`, así que apareció solo:

```
Error: empty:320:Cómo verificar la firma target height
expect(received).toBeGreaterThanOrEqual(expected)
Expected: >= 44
Received:    21
```

**Verde 2.** Regla `.webhook-guide` en `frontend/src/webhooks.scss`: caja propia
con `min-height: 44px`. Sin colores propios —hereda el acento del tema, cuyo
oráculo es el paso de axe—.

---

## 3. La aserción elegida, y por qué no clava la ruta

```ts
const form = screen
  .getByRole("button", { name: "Crear webhook" })
  .closest("form") as HTMLFormElement;
const guide = within(form).getByRole("link", { name: /firma/i });

expect(await tabReaches(user, guide)).toBe(true);
const destination = guide.getAttribute("href")?.trim();
expect(destination).toMatch(/^\S+$/);
expect(destination).not.toBe("#");
```

Afirma cuatro cosas, y las cuatro son lo que le importa a quien usa el
formulario:

1. **Hay un enlace**, y está **dentro del formulario** —no perdido en otra parte
   de la vista—: `within(form).getByRole("link", ...)`.
2. **Dice de qué es.** El nombre accesible tiene que hablar de la **firma**. Un
   «aquí» o un «más información» no casa con `/firma/i` y la prueba cae.
3. **Se alcanza con el teclado.** `tabReaches` camina el orden de tabulación
   pulsando Tab y comprueba que el enlace llega a tomar el foco. Un
   `tabindex="-1"`, un enlace oculto o uno sacado del documento no lo toman.
4. **Lleva a algún sitio de verdad.** El destino existe, no está en blanco y no
   es el `#` de relleno.

**Lo que deliberadamente NO afirma:** que el `href` sea `docs/webhooks.md`. Esa
es exactamente la aserción que el juez desaconsejó, y con razón: el día que la
guía se publique en otra dirección, `toHaveAttribute("href", "docs/webhooks.md")`
se pondría roja sin que ningún usuario hubiera perdido nada —una prueba que falla
cuando el producto sigue estando bien es una prueba que se acaba borrando—.

Y sigue siendo un oráculo con dientes, que es la otra mitad del asunto. Cae si:
el enlace desaparece; se saca del formulario; se queda sin nombre o con un nombre
que no dice de qué es; se le pone `tabindex="-1"` o se oculta; se queda con
`href` vacío o con `#`. El `@s42` del E2E remata con los 44 px, el anillo de foco
y el paso de axe, medidos sobre el enlace nuevo en los cuatro anchos.

---

## 4. Traza `@s → prueba`

| Cláusula del `@s43`                    | Oráculo                                                                                  |
| -------------------------------------- | ---------------------------------------------------------------------------------------- |
| enlace a la guía, dentro del formulario | `webhooks.test.tsx` `@s43` — `within(form).getByRole("link", { name: /firma/i })`         |
| nombre accesible que dice de qué es    | misma prueba, el `name: /firma/i`                                                          |
| alcanzable con Tab                     | misma prueba, `tabReaches`                                                                 |
| destino real, sin fijar cuál           | misma prueba, `toMatch(/^\S+$/)` y `not.toBe("#")`                                         |
| foco visible y 44 px                   | `e2e/webhooks-ux.spec.mjs` `@s42` — geometría (4 anchos × 2 temas) y el recorrido de Tab   |

---

## 5. Verificación ejecutada

| Qué                                                          | Resultado                                    |
| ------------------------------------------------------------ | -------------------------------------------- |
| `npx vitest run src/webhooks.test.tsx`                        | 88/88                                        |
| `npx vitest run src/webhooks-route.test.tsx src/webhooks-client.test.ts src/App.test.tsx` | 96/96            |
| `npx tsc --noEmit`                                            | limpio                                       |
| `npx eslint .` y `npx prettier --check .` (frontend)          | limpios                                      |
| `node --test scripts/project.test.mjs`                        | **89/89**, ninguna guarda tocada             |
| `npx playwright test e2e/webhooks-ux.spec.mjs`                | 8/8 (axe, geometría, tab-order, aria-live)   |
| `npx playwright test e2e/webhooks-native-zoom.spec.mjs`       | 1/1                                          |

Los dos E2E se corrieron contra `vite preview` en el **puerto 18112**, que es el
`E2E_WEB_PORT` de este carril; la spec simula la API entera con `page.route`, así
que no necesita la pila de Docker. El recorrido de teclado registra el enlace
nuevo como **primera parada dentro de `main`**
(`.e2e-work/webhooks-ux/keyboard/tab-order.json`, entrada `"Cómo verificar la
firma"`, presente tanto en `visible` como en `reachable`).

---

## 6. Documentos alineados

- **`project-spec.md:2044`** — la nota decía que la línea era falsa y que se
  corregía el documento. Ya no es verdad: ahora cuenta la historia completa y
  dice que el formulario enlaza, que el contrato pide el **enlace** y no la
  **ruta**, y que el `@s43` está pendiente de contrafirma.
- **`docs/webhooks.md`** — el aviso «Hueco conocido» se sustituye por «De dónde
  se llega a esta página»: el formulario enlaza aquí, lo pide el `@s43`, y la
  prueba no fija la ruta, de modo que mudar la guía no rompe nada.
- **`progress/decisiones_pendientes.md`**, punto 1 — reescrito: lo que falta ya
  no es contrafirmar una corrección documental, sino **contrafirmar el `@s43`** y
  la nueva redacción del spec.

---

## 7. Lo que queda pendiente

1. **Contrafirma del propietario sobre el `@s43`** y sobre la nueva redacción de
   `project-spec.md:2044`. Hasta que exista, el escenario lleva su marca en el
   `.feature` y no acredita cierre. Cuando llegue, entrada en
   `progress/ratificaciones.md` con la pregunta y la opción elegida, como R1-R13.
2. **Remedir la mutación del frontend de la feature 25.** Se ha tocado
   `frontend/src/webhooks.tsx`, que está en el ámbito de
   `frontend/stryker.webhooks.config.json`, así que el **94,57 %** medido queda
   pendiente de rehacer. Este carril **no** lanza campañas: lo relanza el lead.
   El backend de la 25 no se ha tocado y su cifra no se mueve.
