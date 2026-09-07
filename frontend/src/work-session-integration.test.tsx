import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { TaskReader } from "./task-reader";
import { SessionGate } from "./session-gate";
import { observeAccess, setCsrfToken } from "./api-client";

const projectId = "22345678-1234-1234-1234-123456789abc";
const taskId = "32345678-1234-1234-1234-123456789abc";
const time = "2026-09-06T10:00:00Z";
afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
  setCsrfToken();
  window.history.replaceState(null, "", "/");
});
function fixture(
  override: (url: string) => Response | Promise<Response> | undefined = () =>
    undefined,
) {
  const unexpected: string[] = [];
  stubBusinessFetch(
    vi.fn(async (url: string, options?: RequestInit) => {
      const custom = override(url);
      if (custom) return custom;
      if (url === `/api/v1/projects/${projectId}/tasks/${taskId}`)
        return Response.json({
          id: taskId,
          projectId,
          title: "Leer el capítulo",
          completionCriterion: "Notas listas",
          estimatedMinutes: null,
          status: "pending",
          createdAt: time,
          updatedAt: time,
        });
      if (url === `/api/v1/projects/${projectId}`)
        return Response.json(
          {
            id: projectId,
            ownerId: "persona",
            name: "Lectura",
            description: "",
            status: "active",
            createdAt: time,
            updatedAt: time,
          },
          { headers: { ETag: '"project"' } },
        );
      if (url.endsWith("/status"))
        return Response.json(
          { status: "pending", completedAt: null, updatedAt: time },
          { headers: { ETag: `"task:${taskId}:0"` } },
        );
      if (url.endsWith("/parent")) return Response.json({ parent: null });
      if (
        url.endsWith("/subtasks") ||
        url.endsWith("/blocks") ||
        url.endsWith("/history")
      )
        return Response.json({ items: [], nextCursor: null });
      if (url === "/api/v1/work-sessions/active")
        return Response.json({ session: null });
      if (url.endsWith("/work-sessions") && options?.method === "POST")
        return Response.json(
          {
            id: projectId,
            projectId,
            taskId,
            startedAt: time,
            plannedMinutes: 25,
            plannedEndAt: "2026-09-06T10:25:00Z",
            zoneId: "UTC",
          },
          {
            status: 201,
            headers: { Location: `/api/v1/work-sessions/${projectId}` },
          },
        );
      if (
        url === `/api/v1/work-sessions/${projectId}/state` ||
        url === `/api/v1/work-sessions/${projectId}/end-time`
      )
        return Response.json(
          {
            state: {
              session: {
                id: projectId,
                projectId,
                taskId,
                startedAt: time,
                plannedMinutes: 25,
                plannedEndAt: "2026-09-06T10:25:00Z",
                zoneId: "UTC",
              },
              status: "running",
              revision: "1",
              changedAt: time,
              workedMicroseconds: "0",
              runningSince: time,
            },
            serverNow: time,
            ...(url.endsWith("/end-time")
              ? { effectiveEndAt: "2026-09-06T10:25:00Z" }
              : { netMicroseconds: "0" }),
          },
          {
            headers: { "Work-Session-Revision": `work-session-${projectId}-1` },
          },
        );
      unexpected.push(url);
      throw new Error("Unexpected request " + url);
    }),
  );
  return unexpected;
}
it("@s39 SessionGate hides work data before logout responds and ignores the pending lookup", async () => {
  let deliver!: (response: Response) => void;
  let reads = 0;
  const session = {
    id: projectId,
    projectId,
    taskId,
    startedAt: time,
    plannedMinutes: 25,
    plannedEndAt: "2026-09-06T10:25:00Z",
    zoneId: "UTC",
  };
  const unexpected = fixture((url) => {
    if (url === "/api/session")
      return Response.json({
        authenticated: true,
        username: "persona",
        csrfToken: "token",
        csrfHeaderName: "X-CSRF-TOKEN",
      });
    if (url === "/api/session/logout") return new Promise(() => {});
    if (url === "/api/v1/work-sessions/active") {
      reads++;
      return reads === 1
        ? Response.json({ session })
        : new Promise((resolve) => {
            deliver = resolve;
          });
    }
  });
  window.history.replaceState(
    null,
    "",
    `/proyectos/${projectId}/tareas/${taskId}`,
  );
  render(<SessionGate />);
  expect(
    await screen.findByText("Duración prevista: 25 minutos"),
  ).toBeVisible();
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar sesión activa" }),
  );
  await waitFor(() => expect(deliver).toBeTypeOf("function"));
  fireEvent.click(screen.getByRole("button", { name: "Cerrar sesión" }));
  expect(await screen.findByText("Cerrando sesión")).toBeVisible();
  expect(
    screen.queryByText("Duración prevista: 25 minutos"),
  ).not.toBeInTheDocument();
  await act(async () => deliver(Response.json({ session })));
  expect(
    screen.queryByText("Duración prevista: 25 minutos"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("heading", { name: "Sesión de trabajo" }),
  ).not.toBeInTheDocument();
  expect(unexpected).toEqual([]);
});
it("@s28 integrates explicit work start in the existing task reader", async () => {
  const unexpected = fixture();
  render(<TaskReader projectId={projectId} id={taskId} />);
  const duration = await screen.findByLabelText("Duración prevista (minutos)");
  const start = screen.getByRole("button", { name: "Empezar a trabajar" });
  await waitFor(() => expect(start).toHaveAttribute("aria-disabled", "false"));
  fireEvent.change(duration, { target: { value: "25" } });
  fireEvent.click(start);
  await screen.findByText("Sesión iniciada");
  expect(
    screen.getByRole("heading", { name: "Leer el capítulo" }),
  ).toBeVisible();
  expect(screen.getByText("Sin estimación")).toBeVisible();
  expect(unexpected).toEqual([]);
});

it("history18 @s28 discovers history from the existing task detail", async () => {
  const unexpected = fixture();
  render(<TaskReader projectId={projectId} id={taskId} />);
  expect(
    await screen.findByRole("heading", { name: "Leer el capítulo", level: 1 }),
  ).toBeVisible();
  expect(
    screen.getByRole("link", { name: "Ver historial de esta tarea" }),
  ).toHaveAttribute(
    "href",
    `/historial?projectId=${projectId}&taskId=${taskId}`,
  );
  await screen.findByLabelText("Duración prevista (minutos)");
  expect(unexpected).toEqual([]);
});

// Global appearance GET has its own fixture; business requests and counts
// are delegated unchanged to the original mock.
function stubBusinessFetch(mock: unknown) {
  const traffic = mock as typeof fetch;
  vi.stubGlobal("fetch", (input: RequestInfo | URL, init?: RequestInit) => {
    if (
      input === "/api/v1/me/appearance" &&
      (init?.method === undefined || init.method === "GET")
    ) {
      return Promise.resolve(
        Response.json(
          {
            configured: false,
            theme: "SYSTEM",
            accentLight: "#244C3C",
            accentDark: "#B7E4C7",
            updatedAt: null,
          },
          { headers: { ETag: '"appearance:unconfigured"' } },
        ),
      );
    }
    return traffic(input, init);
  });
}
