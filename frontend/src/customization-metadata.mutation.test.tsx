import { render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { App } from "./App";
import { ProjectTasks } from "./project-tasks";
import { useCustomizationSession } from "./customization-state";
afterEach(() => {
  vi.unstubAllGlobals();
  window.history.replaceState({}, "", "/");
});
const projectId = "12345678-1234-1234-1234-123456789abc";
const date = "2026-09-07T10:00:00Z";
it("@s29 shows distinct project timestamps with exact labels and UTC content", async () => {
  window.history.replaceState({}, "", "/proyectos");
  const updated = "2026-09-08T11:00:00Z";
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url === "/api/v1/projects")
      return Promise.resolve(
        Response.json({
          items: [
            {
              id: projectId,
              name: "Mi proyecto",
              status: "idea",
              createdAt: date,
              updatedAt: updated,
            },
          ],
          nextCursor: null,
        }),
      );
    if (url === "/api/v1/me/customization/PROJECT")
      return Promise.resolve(
        Response.json(
          {
            configured: true,
            visibleFields: ["updatedAt", "createdAt"],
            customFields: [],
            updatedAt: date,
          },
          {
            headers: {
              ETag: '"customization:PROJECT:22345678-1234-1234-1234-123456789abc:0"',
            },
          },
        ),
      );
    return Promise.reject(new Error("Ruta no prevista"));
  });
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByRole("button", { name: "Personalizar vista" });
  const list = screen.getByRole("list", { name: "Proyectos guardados" });
  expect(
    [...list.querySelectorAll("time")].map((time) => time.dateTime),
  ).toEqual([updated, date]);
  const times = [...list.querySelectorAll("time")];
  expect(times.map((time) => time.textContent)).toEqual([
    "8 sept 2026, 11:00 UTC",
    "7 sept 2026, 10:00 UTC",
  ]);
  expect(times[0].parentElement).toHaveTextContent(/^Actualizado /);
  expect(times[1].parentElement).toHaveTextContent(/^Creado /);
});

it("@s29 shows distinct task timestamps with exact labels and UTC content", async () => {
  const taskId = "32345678-1234-1234-1234-123456789abc";
  const updated = "2026-09-08T12:00:00Z";
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((url: string) => {
      if (url === `/api/v1/projects/${projectId}/tasks`)
        return Promise.resolve(
          Response.json({
            items: [
              {
                id: taskId,
                projectId,
                title: "Paso uno",
                completionCriterion: "Resultado visible",
                estimatedMinutes: 30,
                status: "pending",
                createdAt: date,
                updatedAt: updated,
              },
            ],
            nextCursor: null,
          }),
        );
      if (url === "/api/v1/me/customization/TASK")
        return Promise.resolve(
          Response.json(
            {
              configured: true,
              visibleFields: ["updatedAt", "createdAt"],
              customFields: [],
              updatedAt: date,
            },
            {
              headers: {
                ETag: '"customization:TASK:22345678-1234-1234-1234-123456789abc:0"',
              },
            },
          ),
        );
      return Promise.reject(new Error("Ruta no prevista"));
    }),
  );
  function Tasks() {
    const session = useCustomizationSession();
    return (
      <ProjectTasks
        projectId={projectId}
        projectStatus="idea"
        onProjectConfirmed={() => {}}
        customization={session}
      />
    );
  }
  render(<Tasks />);
  await screen.findByRole("button", { name: "Personalizar vista" });
  const list = screen.getByRole("list", { name: "Tareas guardadas" });
  expect(
    [...list.querySelectorAll("time")].map((time) => time.dateTime),
  ).toEqual([updated, date]);
  const times = [...list.querySelectorAll("time")];
  expect(times.map((time) => time.textContent)).toEqual([
    "8 sept 2026, 12:00 UTC",
    "7 sept 2026, 10:00 UTC",
  ]);
  expect(times[0].parentElement).toHaveTextContent(/^Actualizado /);
  expect(times[1].parentElement).toHaveTextContent(/^Creado /);
});
