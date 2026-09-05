export const statusLabels = {
  idea: "Idea",
  active: "Activo",
  paused: "Pausado",
  completed: "Terminado",
} as const;
export type ProjectStatus = keyof typeof statusLabels;
export function isProjectStatus(value: unknown): value is ProjectStatus {
  return typeof value === "string" && Object.hasOwn(statusLabels, value);
}
export const statusActions: Record<
  ProjectStatus,
  { label: string; status: ProjectStatus }[]
> = {
  idea: [
    { label: "Activar", status: "active" },
    { label: "Marcar terminado", status: "completed" },
  ],
  active: [
    { label: "Pausar", status: "paused" },
    { label: "Marcar terminado", status: "completed" },
  ],
  paused: [
    { label: "Retomar", status: "active" },
    { label: "Marcar terminado", status: "completed" },
  ],
  completed: [{ label: "Reabrir en pausa", status: "paused" }],
};
