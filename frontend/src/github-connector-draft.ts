import { exact } from "./schedule-block-api";

/**
 * El repositorio a medio escribir sobrevive a salir de la pantalla y volver, porque el contrato
 * (@s41, primera fila) lo pide "con la misma sesión": sessionStorage muere con la pestaña, que es
 * exactamente el alcance de esa frase.
 *
 * Sólo viaja el repositorio, que es un nombre público. El token JAMÁS se guarda aquí: @s34 exige
 * que no aparezca en ningún almacén del navegador, y por eso este módulo no tiene forma de
 * recibirlo.
 */
const storageKey = "organizationweb.github.repository.v1";

export function clearRepositoryDraft() {
  sessionStorage.removeItem(storageKey);
}

/** Un borrador vacío no es un borrador: no deja rastro en la sesión. */
export function saveRepositoryDraft(owner: string, repository: string) {
  if (repository === "") return clearRepositoryDraft();
  sessionStorage.setItem(storageKey, JSON.stringify({ owner, repository }));
}

/**
 * Devuelve el borrador sólo si es de quien pregunta. El de otra persona se borra al leerlo, así
 * que iniciar sesión en la misma pestaña no hereda nada (@s41, segunda fila).
 */
export function readRepositoryDraft(owner: string): string {
  const raw = sessionStorage.getItem(storageKey);
  let draft: unknown;
  try {
    draft = raw ? JSON.parse(raw) : null;
  } catch {
    clearRepositoryDraft();
    return "";
  }
  if (
    !exact(draft, "owner repository") ||
    draft.owner !== owner ||
    typeof draft.repository !== "string"
  ) {
    clearRepositoryDraft();
    return "";
  }
  return draft.repository;
}
