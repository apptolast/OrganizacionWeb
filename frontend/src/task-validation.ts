export const taskMessages = {
  title: "Escribe un título de entre 1 y 160 caracteres.",
  completionCriterion: "El criterio admite hasta 2000 caracteres.",
  estimatedMinutes: "La estimación debe ser un número entero entre 1 y 1440.",
};
export type TaskField = keyof typeof taskMessages;
export function validateTask(
  title: string,
  criterion: string,
  estimate: string,
): TaskField[] {
  const errors: TaskField[] = [];
  const length = [...title.replace(/^\p{White_Space}+|\p{White_Space}+$/gu, "")]
    .length;
  if (length < 1 || length > 160) errors.push("title");
  if ([...criterion].length > 2000) errors.push("completionCriterion");
  if (
    estimate !== "" &&
    (!Number.isInteger(Number(estimate)) ||
      Number(estimate) < 1 ||
      Number(estimate) > 1440)
  )
    errors.push("estimatedMinutes");
  return errors;
}
