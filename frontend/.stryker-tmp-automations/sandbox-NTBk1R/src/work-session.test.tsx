// @ts-nocheck
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { WorkSession } from "./work-session";
import { useSession } from "./use-session";
import { observeAccess, setCsrfToken } from "./api-client";
import { useState } from "react";
import userEvent from "@testing-library/user-event";

const receipt = {
  id: "12345678-1234-1234-1234-123456789abc",
  projectId: "22345678-1234-1234-1234-123456789abc",
  taskId: "32345678-1234-1234-1234-123456789abc",
  startedAt: "2026-09-06T10:00:00.123456Z",
  plannedMinutes: 25,
  plannedEndAt: "2026-09-06T10:25:00.123456Z",
  zoneId: "UTC",
};
const props = {
  projectId: receipt.projectId,
  taskId: receipt.taskId,
  taskTitle: "Leer el capítulo",
  taskStatus: "pending" as const,
  projectStatus: "active" as const,
  onAccessFailure: vi.fn(),
};
function sessionReadResponse(url: string) {
  const state = {
    session: receipt,
    status: "running",
    revision: "1",
    changedAt: receipt.startedAt,
    workedMicroseconds: "0",
    runningSince: receipt.startedAt,
  };
  const headers = { "Work-Session-Revision": `work-session-${receipt.id}-1` };
  if (url === `/api/v1/work-sessions/${receipt.id}/state`)
    return Promise.resolve(
      Response.json(
        { state, serverNow: receipt.startedAt, netMicroseconds: "0" },
        { headers },
      ),
    );
  if (url === `/api/v1/work-sessions/${receipt.id}/end-time`)
    return Promise.resolve(
      Response.json(
        {
          state,
          serverNow: receipt.startedAt,
          effectiveEndAt: receipt.plannedEndAt,
        },
        { headers },
      ),
    );
  throw new Error(`Unexpected session read: ${url}`);
}
function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}
afterEach(() => {
  vi.unstubAllGlobals();
  vi.clearAllMocks();
  observeAccess();
  setCsrfToken();
});
it("@s28 feature17 exposes the agreed-end notice and deliberate extension from the task's active session", async () => {
  const state = {
    session: receipt,
    status: "running",
    revision: "1",
    changedAt: receipt.startedAt,
    workedMicroseconds: "0",
    runningSince: receipt.startedAt,
  };
  const headers = { "Work-Session-Revision": `work-session-${receipt.id}-1` };
  const fetcher = vi.fn((url: string) => {
    if (url.endsWith("/active"))
      return Promise.resolve(Response.json({ session: receipt }));
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          {
            state,
            serverNow: receipt.plannedEndAt,
            netMicroseconds: "1500000000",
          },
          { headers },
        ),
      );
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        Response.json(
          {
            state,
            serverNow: receipt.plannedEndAt,
            effectiveEndAt: receipt.plannedEndAt,
          },
          { headers },
        ),
      );
    throw new Error(`Unexpected request ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  expect(await screen.findByText("Ha llegado el fin acordado")).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Ampliar tiempo" }));
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(null);
  expect(
    screen.getByRole("button", { name: "Confirmar ampliación" }),
  ).toBeVisible();
  expect(screen.getByRole("button", { name: "Pausar" })).toBeVisible();
  expect(fetcher.mock.calls.map(([url]) => url).sort()).toEqual(
    [
      `/api/v1/work-sessions/${receipt.id}/end-time`,
      `/api/v1/work-sessions/${receipt.id}/state`,
      "/api/v1/work-sessions/active",
    ].sort(),
  );
});

it("@s36 feature17 shares a pending pause with the task's extension control", async () => {
  const fetcher = vi.fn((url: string, init?: RequestInit) => {
    if (url.endsWith("/active"))
      return Promise.resolve(Response.json({ session: receipt }));
    if (init?.method === "POST") return new Promise<Response>(() => {});
    return sessionReadResponse(url);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/extend")),
  ).toHaveLength(0);
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/pause")),
  ).toHaveLength(1);
  expect(
    screen.getByRole("button", { name: "Confirmar ampliación" }),
  ).toHaveAttribute("aria-disabled", "true");
});

it("@s41 feature17 aborts the parent's pending active lookup before a child's pause and late HTTP 401", async () => {
  const oldActive = deferred<Response>();
  let activeReads = 0;
  const fetcher = vi.fn((url: string, init?: RequestInit) => {
    if (url.endsWith("/active"))
      return ++activeReads === 1
        ? Promise.resolve(Response.json({ session: receipt }))
        : oldActive.promise;
    if (init?.method === "POST") return new Promise<Response>(() => {});
    return sessionReadResponse(url);
  });
  vi.stubGlobal("fetch", fetcher);
  const observer = vi.fn();
  observeAccess(observer);
  render(<WorkSession {...props} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar sesión activa" }),
  );
  await waitFor(() => expect(activeReads).toBe(2));
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  await act(async () => {
    oldActive.resolve(new Response(null, { status: 401 }));
  });
  expect(observer).not.toHaveBeenCalled();
  expect(props.onAccessFailure).not.toHaveBeenCalled();
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(5);
  expect(screen.getByText("Procesando cambio de sesión")).toBeVisible();
});

it("@s40 @s41 feature17 invalidates a parent lookup started during a pause when its receipt is confirmed", async () => {
  const post = deferred<Response>();
  const oldActive = deferred<Response>();
  const before = {
    session: receipt,
    status: "running",
    revision: "1",
    changedAt: receipt.startedAt,
    workedMicroseconds: "0",
    runningSince: receipt.startedAt as string | null,
  };
  const after = {
    ...before,
    status: "paused",
    revision: "2",
    runningSince: null,
  };
  let current = before;
  let activeReads = 0;
  const fetcher = vi.fn((url: string) => {
    if (url.endsWith("/active"))
      return ++activeReads === 2
        ? oldActive.promise
        : Promise.resolve(Response.json({ session: receipt }));
    if (url.endsWith("/pause")) return post.promise;
    const headers = {
      "Work-Session-Revision": `work-session-${receipt.id}-${current.revision}`,
    };
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          {
            state: current,
            serverNow: receipt.startedAt,
            netMicroseconds: "0",
          },
          { headers },
        ),
      );
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        Response.json(
          {
            state: current,
            serverNow: receipt.startedAt,
            effectiveEndAt: receipt.plannedEndAt,
          },
          { headers },
        ),
      );
    throw new Error(`Unexpected request ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  const observer = vi.fn();
  observeAccess(observer);
  render(<WorkSession {...props} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar sesión activa" }),
  );
  await waitFor(() => expect(activeReads).toBe(2));
  const id = "42345678-1234-1234-1234-123456789abc";
  await act(async () => {
    current = after;
    post.resolve(
      Response.json(
        {
          id,
          sessionId: receipt.id,
          action: "PAUSE",
          occurredAt: receipt.startedAt,
          before,
          after,
        },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${id}` },
        },
      ),
    );
  });
  expect(await screen.findByText("Pausa confirmada")).toBeVisible();
  await act(async () => {
    oldActive.resolve(new Response(null, { status: 401 }));
  });
  expect(observer).not.toHaveBeenCalled();
  expect(props.onAccessFailure).not.toHaveBeenCalled();
  expect(await screen.findByText("En pausa")).toBeVisible();
  expect(activeReads).toBe(3);
});

it("@s32 withdraws earlier absence while a transmitted start is pending or uncertain", async () => {
  const pending = deferred<Response>();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockReturnValueOnce(pending.promise);
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  expect(
    await screen.findByText("No hay una sesión de trabajo activa."),
  ).toBeVisible();
  fireEvent.change(screen.getByLabelText("Duración prevista (minutos)"), {
    target: { value: "25" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  expect(screen.getByText("Iniciando sesión de trabajo")).toBeVisible();
  expect(
    screen.queryByText("No hay una sesión de trabajo activa."),
  ).not.toBeInTheDocument();
  await act(async () => pending.resolve(new Response(null, { status: 503 })));
  expect(
    await screen.findByRole("button", { name: "Comprobar inicio" }),
  ).toBeVisible();
  expect(
    screen.queryByText("No hay una sesión de trabajo activa."),
  ).not.toBeInTheDocument();
  expect(screen.getByLabelText("Duración prevista (minutos)")).toHaveValue(25);
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s38 ignores an aborted active lookup's 401 and finally while a newer lookup is pending", async () => {
  const old = deferred<Response>();
  const current = deferred<Response>();
  const fetcher = vi
    .fn()
    .mockReturnValueOnce(old.promise)
    .mockReturnValueOnce(current.promise);
  vi.stubGlobal("fetch", fetcher);
  render(
    <div>
      <button>Control del padre</button>
      <WorkSession {...props} />
    </div>,
  );
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(1));
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar sesión activa" }),
  );
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  await act(async () => old.resolve(new Response(null, { status: 401 })));
  expect(props.onAccessFailure).not.toHaveBeenCalled();
  expect(
    screen.getByRole("button", { name: "Control del padre" }),
  ).toBeVisible();
  expect(screen.getByText("Consultando sesión activa")).toBeVisible();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(
    screen.queryByText("No hay una sesión de trabajo activa."),
  ).not.toBeInTheDocument();
  await act(async () => current.resolve(Response.json({ session: null })));
  expect(
    await screen.findByText("No hay una sesión de trabajo activa."),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s32 Enter and a submit event during uncertainty cannot resend the intention", async () => {
  const user = userEvent.setup();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  const input = await screen.findByLabelText("Duración prevista (minutos)");
  await user.type(input, "25");
  await user.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  await screen.findByRole("button", { name: "Comprobar inicio" });
  await user.click(input);
  await user.keyboard("{Enter}");
  expect(fetcher).toHaveBeenCalledTimes(2);
  const submit = new Event("submit", { bubbles: true, cancelable: true });
  fireEvent(input.closest("form")!, submit);
  expect(submit.defaultPrevented).toBe(true);
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(input).toHaveValue(25);
  expect(
    screen.getByRole("button", { name: "Comprobar inicio" }),
  ).toBeVisible();
});

it("@s33 recovers a retained start after the task becomes completed", async () => {
  const fetcher = vi
    .fn()
    .mockImplementation((url) => sessionReadResponse(url))
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  const view = render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  await screen.findByRole("button", { name: "Comprobar inicio" });
  view.rerender(
    <WorkSession {...props} taskStatus="completed" projectStatus="completed" />,
  );
  fireEvent.click(screen.getByRole("button", { name: "Comprobar inicio" }));
  expect(await screen.findByText("Sesión iniciada")).toBeVisible();
  expect(fetcher.mock.calls[2][0]).toContain("/by-request/");
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(5));
  expect(fetcher.mock.calls[3][0]).toBe(
    `/api/v1/work-sessions/${receipt.id}/state`,
  );
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/end-time")),
  ).toHaveLength(1);
});
it("@s36 rediscovers active work after remount without recovering an in-memory key", async () => {
  const fetcher = vi
    .fn()
    .mockImplementation((url) => sessionReadResponse(url))
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json({ session: receipt }));
  vi.stubGlobal("fetch", fetcher);
  const view = render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  await screen.findByRole("button", { name: "Comprobar inicio" });
  view.unmount();
  render(<WorkSession {...props} />);
  expect(
    await screen.findByText("Duración prevista: 25 minutos"),
  ).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Comprobar inicio" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Empezar a trabajar" }),
  ).not.toBeInTheDocument();
  expect(fetcher.mock.calls[2][0]).toBe("/api/v1/work-sessions/active");
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(5));
  expect(fetcher.mock.calls[3][0]).toBe(
    `/api/v1/work-sessions/${receipt.id}/state`,
  );
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/end-time")),
  ).toHaveLength(1);
});
it("@s30 keeps an incompatible success uncertain and checks the original intention", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(
      Response.json(
        { ...receipt, plannedEndAt: "2026-09-06T10:25:00.123457Z" },
        {
          status: 201,
          headers: { Location: `/api/v1/work-sessions/${receipt.id}` },
        },
      ),
    )
    .mockResolvedValueOnce(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  const check = await screen.findByRole("button", { name: "Comprobar inicio" });
  expect(screen.queryByText("Sesión iniciada")).not.toBeInTheDocument();
  expect(screen.getByLabelText("Duración prevista (minutos)")).toHaveAttribute(
    "readonly",
  );
  fireEvent.click(check);
  expect(await screen.findByText("Sesión iniciada")).toBeVisible();
  expect(fetcher.mock.calls[2][0]).toBe(
    `/api/v1/work-sessions/by-request/${new Headers(fetcher.mock.calls[1][1].headers).get("Idempotency-Key")}`,
  );
});
it("@s32 treats an unknown error code as uncertainty rather than editable rejection", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:unknown",
          title: "Unknown",
          status: 409,
          code: "UNKNOWN",
        },
        { status: 409 },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  expect(
    await screen.findByRole("button", { name: "Comprobar inicio" }),
  ).toBeVisible();
  expect(screen.getByLabelText("Duración prevista (minutos)")).toHaveAttribute(
    "readonly",
  );
  expect(screen.queryByText("Sesión iniciada")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s32 offers a check after network failure without automatically repeating the POST", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockRejectedValueOnce(new TypeError("Network error"));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  expect(
    await screen.findByRole("button", { name: "Comprobar inicio" }),
  ).toBeVisible();
  expect(screen.getByLabelText("Duración prevista (minutos)")).toHaveAttribute(
    "readonly",
  );
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo inicio" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s38 ignores an error classified after the original context was retired", async () => {
  const json = deferred<unknown>();
  const response = new Response(null, { status: 404 });
  const clone = Response.json({});
  vi.spyOn(clone, "json").mockReturnValue(json.promise);
  vi.spyOn(response, "clone").mockReturnValue(clone);
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(response)
    .mockResolvedValueOnce(Response.json({ session: null }));
  vi.stubGlobal("fetch", fetcher);
  const view = render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  await waitFor(() => expect(clone.json).toHaveBeenCalled());
  view.rerender(
    <WorkSession {...props} taskId={receipt.id} taskTitle="Otra tarea" />,
  );
  await act(async () =>
    json.resolve({
      type: "urn:organization:problem:resource_not_found",
      title: "Contexto retirado",
      status: 404,
      code: "RESOURCE_NOT_FOUND",
    }),
  );
  expect(await screen.findByText("Otra tarea")).toBeVisible();
  expect(props.onAccessFailure).not.toHaveBeenCalled();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("@s40 supports keyboard submission without stealing focus from another control", async () => {
  const post = deferred<Response>();
  const fetcher = vi
    .fn()
    .mockImplementation((url) => sessionReadResponse(url))
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockReturnValueOnce(post.promise);
  vi.stubGlobal("fetch", fetcher);
  const user = userEvent.setup();
  render(<WorkSession {...props} />);
  const input = await screen.findByLabelText("Duración prevista (minutos)");
  await user.click(input);
  await user.type(input, "25");
  await user.tab();
  expect(
    screen.getByRole("button", { name: "Empezar a trabajar" }),
  ).toHaveFocus();
  await user.keyboard("{Enter}");
  expect(screen.getByText("Iniciando sesión de trabajo")).toBeVisible();
  await user.tab({ shift: true });
  expect(input).toHaveFocus();
  const refreshButton = screen.getByRole("button", {
    name: "Actualizar sesión activa",
  });
  await user.tab({ shift: true });
  expect(refreshButton).toHaveFocus();
  await act(async () =>
    post.resolve(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-sessions/${receipt.id}` },
      }),
    ),
  );
  expect(await screen.findByText("Sesión iniciada")).toBeVisible();
  expect(refreshButton).toHaveFocus();
  expect(fetcher).toHaveBeenCalledTimes(4);
  expect(fetcher.mock.calls[2][0]).toBe(
    `/api/v1/work-sessions/${receipt.id}/state`,
  );
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/end-time")),
  ).toHaveLength(1);
});
it("@s38 ignores recovered JSON after the task route changes", async () => {
  const json = deferred<unknown>();
  const response = Response.json(receipt);
  vi.spyOn(response, "json").mockReturnValue(json.promise);
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(response)
    .mockResolvedValueOnce(Response.json({ session: null }));
  vi.stubGlobal("fetch", fetcher);
  const view = render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar inicio" }),
  );
  await waitFor(() => expect(response.json).toHaveBeenCalledOnce());
  view.rerender(
    <WorkSession {...props} taskId={receipt.id} taskTitle="Otra tarea" />,
  );
  await act(async () => json.resolve(receipt));
  expect(await screen.findByText("Otra tarea")).toBeVisible();
  expect(screen.queryByText("Sesión iniciada")).not.toBeInTheDocument();
  expect(screen.getByLabelText("Duración prevista (minutos)")).toHaveValue(
    null,
  );
  expect(fetcher.mock.calls[2][1].signal.aborted).toBe(true);
});
it("@s33 keeps uncertainty after a failed repeated check without authorizing resend", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:work_session_not_found",
          title: "No encontrado",
          status: 404,
          code: "WORK_SESSION_NOT_FOUND",
        },
        { status: 404 },
      ),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar inicio" }),
  );
  expect(
    await screen.findByRole("button", { name: "Reenviar el mismo inicio" }),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Comprobar inicio" }));
  await waitFor(() =>
    expect(
      screen.queryByRole("button", { name: "Reenviar el mismo inicio" }),
    ).not.toBeInTheDocument(),
  );
  expect(screen.getByLabelText("Duración prevista (minutos)")).toHaveValue(25);
  expect(screen.getByLabelText("Duración prevista (minutos)")).toHaveAttribute(
    "readonly",
  );
  expect(fetcher.mock.calls[3][0]).toBe(fetcher.mock.calls[2][0]);
  expect(fetcher).toHaveBeenCalledTimes(4);
});
it("@s32 retains the exact intention after an idempotency conflict", async () => {
  const fetcher = vi
    .fn()
    .mockImplementation((url) => sessionReadResponse(url))
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:idempotency_conflict",
          title: "Otra intención",
          status: 409,
          code: "IDEMPOTENCY_CONFLICT",
        },
        { status: 409 },
      ),
    )
    .mockResolvedValueOnce(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar inicio" }),
  );
  expect(await screen.findByText("Sesión iniciada")).toBeVisible();
  const requestKey = new Headers(fetcher.mock.calls[1][1].headers).get(
    "Idempotency-Key",
  );
  expect(fetcher.mock.calls[2][0]).toBe(
    `/api/v1/work-sessions/by-request/${requestKey}`,
  );
  expect(fetcher.mock.calls[2][1].method).toBeUndefined();
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(5));
  expect(fetcher.mock.calls[3][0]).toBe(
    `/api/v1/work-sessions/${receipt.id}/state`,
  );
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/end-time")),
  ).toHaveLength(1);
});
it("@s42 rejects an incompatible active envelope without offering a new start", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ session: null, count: 0 })),
  );
  render(<WorkSession {...props} />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se ha podido consultar la sesión activa.",
  );
  expect(
    screen.queryByText("No hay una sesión de trabajo activa."),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Empezar a trabajar" }),
  ).not.toBeInTheDocument();
});
it("@s36 ignores an older active lookup after a start confirmation", async () => {
  const post = deferred<Response>();
  const lookup = deferred<Response>();
  const fetcher = vi
    .fn()
    .mockImplementation((url) => sessionReadResponse(url))
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockReturnValueOnce(post.promise)
    .mockReturnValueOnce(lookup.promise);
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar sesión activa" }),
  );
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(3));
  await act(async () =>
    post.resolve(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-sessions/${receipt.id}` },
      }),
    ),
  );
  expect(await screen.findByText("Sesión iniciada")).toBeVisible();
  await act(async () => lookup.resolve(Response.json({ session: null })));
  expect(
    screen.queryByText("No hay una sesión de trabajo activa."),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Empezar a trabajar" }),
  ).not.toBeInTheDocument();
  expect(screen.getByText("Sesión iniciada")).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(5);
  expect(fetcher.mock.calls[3][0]).toBe(
    `/api/v1/work-sessions/${receipt.id}/state`,
  );
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/end-time")),
  ).toHaveLength(1);
});
it("@s35 removes a task context rejected as RESOURCE_NOT_FOUND", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json({ session: null }))
      .mockResolvedValueOnce(
        Response.json(
          {
            type: "urn:organization:problem:resource_not_found",
            title: "Contexto no disponible",
            status: 404,
            code: "RESOURCE_NOT_FOUND",
          },
          { status: 404 },
        ),
      ),
  );
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  await waitFor(() =>
    expect(props.onAccessFailure).toHaveBeenCalledExactlyOnceWith(404),
  );
});
it("@s39 propagates a current unauthorized start to the owning view", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json({ session: null }))
      .mockResolvedValueOnce(new Response(null, { status: 401 })),
  );
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  await waitFor(() =>
    expect(props.onAccessFailure).toHaveBeenCalledExactlyOnceWith(401),
  );
});
it("@s40 announces checking separately and preserves focus without duplicate checks", async () => {
  const pending = deferred<Response>();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockReturnValueOnce(pending.promise);
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  const check = await screen.findByRole("button", { name: "Comprobar inicio" });
  check.focus();
  fireEvent.click(check);
  fireEvent.click(check);
  expect(screen.getByRole("status")).toHaveTextContent("Comprobando inicio");
  expect(check).toHaveFocus();
  expect(fetcher).toHaveBeenCalledTimes(3);
  await act(async () => pending.resolve(new Response(null, { status: 503 })));
  expect(check).toHaveFocus();
  expect(check).toHaveAttribute("aria-disabled", "false");
});
it("@s31 presents an unsupported historical zone in labelled UTC without changing the receipt", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ session: { ...receipt, zoneId: "Retired/Zone" } }),
      ),
  );
  render(<WorkSession {...props} />);
  expect(await screen.findByText(/Zona: Retired\/Zone/)).toHaveTextContent(
    "horas mostradas en UTC",
  );
  expect(screen.getByText(/Fin previsto:/)).toHaveTextContent("10:25");
  expect(
    screen.getByText(/Fin previsto:/).querySelector("time"),
  ).toHaveAttribute("dateTime", receipt.plannedEndAt);
});
it("@s36 explains that closing does not revoke a transmitted start and preserves its recovery", async () => {
  const pending = deferred<Response>();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockReturnValueOnce(pending.promise);
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  expect(screen.getByText(/Cerrar este formulario no revoca/)).toBeVisible();
  const close = screen.getByRole("button", { name: "Cerrar formulario" });
  close.focus();
  fireEvent.click(close);
  expect(
    screen.queryByLabelText("Duración prevista (minutos)"),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("heading", { name: "Sesión de trabajo" }),
  ).toHaveFocus();
  await act(async () => pending.resolve(new Response(null, { status: 503 })));
  fireEvent.click(screen.getByRole("button", { name: "Mostrar formulario" }));
  expect(
    screen.getByRole("button", { name: "Comprobar inicio" }),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s40 moves focus to the session heading when confirmation removes its initiator", async () => {
  const pending = deferred<Response>();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json({ session: null }))
      .mockReturnValueOnce(pending.promise),
  );
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  const button = screen.getByRole("button", { name: "Empezar a trabajar" });
  button.focus();
  fireEvent.click(button);
  await act(async () =>
    pending.resolve(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-sessions/${receipt.id}` },
      }),
    ),
  );
  expect(
    screen.getByRole("heading", { name: "Sesión de trabajo" }),
  ).toHaveFocus();
});
it("@s28 waits for confirmed eligible task and project states before a new start", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  const view = render(
    <WorkSession {...props} taskStatus={undefined} projectStatus={undefined} />,
  );
  const input = await screen.findByLabelText("Duración prevista (minutos)");
  fireEvent.change(input, { target: { value: "25" } });
  fireEvent.submit(input.closest("form")!);
  expect(fetcher).toHaveBeenCalledTimes(1);
  view.rerender(<WorkSession {...props} taskStatus="completed" />);
  fireEvent.submit(input.closest("form")!);
  expect(fetcher).toHaveBeenCalledTimes(1);
  view.rerender(<WorkSession {...props} projectStatus={undefined} />);
  fireEvent.submit(input.closest("form")!);
  expect(fetcher).toHaveBeenCalledTimes(1);
  view.rerender(<WorkSession {...props} projectStatus="completed" />);
  fireEvent.submit(input.closest("form")!);
  expect(fetcher).toHaveBeenCalledTimes(1);
  view.rerender(<WorkSession {...props} />);
  fireEvent.submit(input.closest("form")!);
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s29 does not reuse old absence to start during a pending or failed refresh", async () => {
  const pending = deferred<Response>();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockReturnValueOnce(pending.promise);
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  const input = await screen.findByLabelText("Duración prevista (minutos)");
  fireEvent.change(input, { target: { value: "25" } });
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar sesión activa" }),
  );
  fireEvent.submit(input.closest("form")!);
  expect(
    screen.queryByText("No hay una sesión de trabajo activa."),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Empezar a trabajar" }),
  ).toHaveAttribute("aria-disabled", "true");
  expect(fetcher).toHaveBeenCalledTimes(2);
  await act(async () => pending.resolve(new Response(null, { status: 503 })));
  expect(
    screen.queryByText("No hay una sesión de trabajo activa."),
  ).not.toBeInTheDocument();
  fireEvent.submit(input.closest("form")!);
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s35 keeps checking the retained key when active refresh discovers another task", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json({ session: null }))
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockResolvedValueOnce(
        Response.json({ session: { ...receipt, taskId: receipt.id } }),
      ),
  );
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  await screen.findByRole("button", { name: "Comprobar inicio" });
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar sesión activa" }),
  );
  await screen.findByRole("link", { name: "Ir a la tarea de esta sesión" });
  expect(
    screen.getByRole("button", { name: "Comprobar inicio" }),
  ).toBeVisible();
  expect(screen.getByLabelText("Duración prevista (minutos)")).toHaveValue(25);
});
it("@s38 aborts a submitted intent before a late 401 can invalidate access", async () => {
  const pending = deferred<Response>();
  const observer = vi.fn();
  observeAccess(observer);
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockReturnValueOnce(pending.promise);
  vi.stubGlobal("fetch", fetcher);
  const view = render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  view.unmount();
  expect(fetcher.mock.calls[1][1].signal.aborted).toBe(true);
  await act(async () => pending.resolve(new Response(null, { status: 401 })));
  expect(observer).not.toHaveBeenCalled();
  expect(props.onAccessFailure).not.toHaveBeenCalled();
});
it("@s39 removes visible work data on a current unauthorized lookup", async () => {
  function Gate() {
    const [lost, setLost] = useState(false);
    return lost ? (
      <p>Acceso retirado</p>
    ) : (
      <WorkSession {...props} onAccessFailure={() => setLost(true)} />
    );
  }
  let activeReads = 0;
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) => {
      if (url === "/api/v1/work-sessions/active") {
        activeReads += 1;
        return Promise.resolve(
          activeReads === 1
            ? Response.json({ session: receipt })
            : new Response(null, { status: 401 }),
        );
      }
      return sessionReadResponse(url);
    }),
  );
  render(<Gate />);
  await screen.findByText(/Fin previsto:/);
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar sesión activa" }),
  );
  await screen.findByText("Acceso retirado");
  expect(screen.queryByText(/Fin previsto:/)).not.toBeInTheDocument();
});
it("@s38 aborts the old task lookup and discards its late active session", async () => {
  const old = deferred<Response>();
  const fetcher = vi
    .fn()
    .mockReturnValueOnce(old.promise)
    .mockResolvedValueOnce(Response.json({ session: null }));
  vi.stubGlobal("fetch", fetcher);
  const view = render(<WorkSession {...props} />);
  view.rerender(
    <WorkSession {...props} taskId={receipt.id} taskTitle="Otra tarea" />,
  );
  await screen.findByText("Otra tarea");
  expect(fetcher.mock.calls[0][1].signal.aborted).toBe(true);
  await act(async () => old.resolve(Response.json({ session: receipt })));
  expect(screen.queryByText(/Fin previsto:/)).not.toBeInTheDocument();
  expect(screen.getByText("Otra tarea")).toBeVisible();
});
it("@s35 offers the owner's active lookup after a recognized active conflict", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:work_session_already_active",
          title: "Ya existe una sesión de trabajo activa.",
          status: 409,
          code: "WORK_SESSION_ALREADY_ACTIVE",
          sessionId: receipt.id,
        },
        { status: 409 },
      ),
    )
    .mockResolvedValueOnce(Response.json({ session: receipt }));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Ya existe una sesión de trabajo activa.",
  );
  expect(
    screen.queryByText("No hay una sesión de trabajo activa."),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Empezar a trabajar" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(
    screen.queryByRole("button", { name: "Comprobar inicio" }),
  ).not.toBeInTheDocument();
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar sesión activa" }),
  );
  await screen.findByRole("link", { name: "Ir a la tarea de esta sesión" });
  expect(screen.queryByText("Sesión iniciada")).not.toBeInTheDocument();
});
it("@s35 explains an unrepresentable server time without blaming the duration field", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json({ session: null }))
      .mockResolvedValueOnce(
        Response.json(
          {
            type: "urn:organization:problem:work_session_time_out_of_range",
            title:
              "No se puede representar el inicio y el fin previsto de la sesión.",
            status: 409,
            code: "WORK_SESSION_TIME_OUT_OF_RANGE",
          },
          { status: 409 },
        ),
      ),
  );
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se puede representar el inicio y el fin previsto de la sesión.",
  );
  expect(screen.getByLabelText("Duración prevista (minutos)")).toHaveAttribute(
    "aria-invalid",
    "false",
  );
  expect(
    screen.queryByRole("button", { name: "Comprobar inicio" }),
  ).not.toBeInTheDocument();
});
it("@s35 explains a completed-task rejection as definitive", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json({ session: null }))
      .mockResolvedValueOnce(
        Response.json(
          {
            type: "urn:organization:problem:task_completed",
            title: "La tarea está completada.",
            status: 409,
            code: "TASK_COMPLETED",
          },
          { status: 409 },
        ),
      ),
  );
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "La tarea está completada.",
  );
  expect(
    screen.queryByRole("button", { name: "Comprobar inicio" }),
  ).not.toBeInTheDocument();
});
it("@s35 explains a completed-project rejection without treating it as uncertain", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:project_completed",
          title: "No se puede iniciar trabajo en un proyecto completado.",
          status: 409,
          code: "PROJECT_COMPLETED",
        },
        { status: 409 },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se puede iniciar trabajo en un proyecto completado.",
  );
  expect(
    screen.queryByRole("button", { name: "Comprobar inicio" }),
  ).not.toBeInTheDocument();
});
it("@s35 permits correction after a definitive validation rejection with a new intention", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:validation_error",
          title: "Revisa la duración",
          status: 400,
          code: "VALIDATION_ERROR",
          errors: [
            {
              field: "plannedMinutes",
              code: "OUT_OF_RANGE",
              message: "Revisa la duración elegida.",
            },
          ],
        },
        { status: 400 },
      ),
    )
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  const input = await screen.findByLabelText("Duración prevista (minutos)");
  fireEvent.change(input, { target: { value: "25" } });
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  await screen.findByText("Revisa la duración elegida.");
  expect(input).not.toHaveAttribute("readonly");
  fireEvent.change(input, { target: { value: "30" } });
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  expect(fetcher.mock.calls[2][1].body).toBe('{"plannedMinutes":30}');
  expect(
    new Headers(fetcher.mock.calls[2][1].headers).get("Idempotency-Key"),
  ).not.toBe(
    new Headers(fetcher.mock.calls[1][1].headers).get("Idempotency-Key"),
  );
  expect(
    screen.queryByText("Revisa la duración elegida."),
  ).not.toBeInTheDocument();
});
it("@s28 does not start without an explicit valid duration and associates its error", async () => {
  const fetcher = vi.fn().mockResolvedValue(Response.json({ session: null }));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  const input = await screen.findByLabelText("Duración prevista (minutos)");
  fireEvent.submit(input.closest("form")!);
  expect(input).toHaveAttribute("aria-invalid", "true");
  expect(input).toHaveAccessibleDescription(
    "Elige una duración entera entre 1 y 1440 minutos.",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s34 renews SessionGate access manually before a separate identical resend", async () => {
  function Flow() {
    const session = useSession();
    return session.session?.authenticated ? (
      <>
        <button
          hidden={!session.csrfExpired}
          onClick={() => void session.refresh(true)}
        >
          Recuperar acceso
        </button>
        <WorkSession {...props} />
      </>
    ) : (
      <p>Comprobando acceso</p>
    );
  }
  const session = (csrfToken: string) =>
    Response.json({
      authenticated: true,
      username: "persona",
      csrfToken,
      csrfHeaderName: "X-CSRF-TOKEN",
    });
  const renew = deferred<Response>();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(session("old-token"))
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:csrf_invalid",
          title: "Recupera acceso",
          status: 403,
          code: "CSRF_INVALID",
        },
        { status: 403 },
      ),
    )
    .mockReturnValueOnce(renew.promise)
    .mockResolvedValueOnce(new Response(null, { status: 503 }));
  vi.stubGlobal("fetch", fetcher);
  render(<Flow />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  const resend = await screen.findByRole("button", {
    name: "Reenviar el mismo inicio",
  });
  fireEvent.click(screen.getByRole("button", { name: "Recuperar acceso" }));
  await act(async () => renew.resolve(session("new-token")));
  expect(fetcher).toHaveBeenCalledTimes(4);
  fireEvent.click(resend);
  await screen.findByRole("button", { name: "Comprobar inicio" });
  expect(fetcher.mock.calls[4][1].body).toBe(fetcher.mock.calls[2][1].body);
  expect(
    new Headers(fetcher.mock.calls[4][1].headers).get("Idempotency-Key"),
  ).toBe(new Headers(fetcher.mock.calls[2][1].headers).get("Idempotency-Key"));
  expect(
    new Headers(fetcher.mock.calls[4][1].headers).get("X-CSRF-TOKEN"),
  ).toBe("new-token");
});
it("@s33 resends only after a missing receipt and preserves its original key and body", async () => {
  const missing = Response.json(
    {
      type: "urn:organization:problem:work_session_not_found",
      title: "No se ha encontrado la sesión de trabajo.",
      status: 404,
      code: "WORK_SESSION_NOT_FOUND",
    },
    { status: 404 },
  );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(missing)
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar inicio" }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Reenviar el mismo inicio" }),
  );
  expect(fetcher.mock.calls[3][1].body).toBe(fetcher.mock.calls[1][1].body);
  expect(
    new Headers(fetcher.mock.calls[3][1].headers).get("Idempotency-Key"),
  ).toBe(new Headers(fetcher.mock.calls[1][1].headers).get("Idempotency-Key"));
});
it("@s32 @s33 keeps an uncertain intention and confirms it only through its key", async () => {
  const fetcher = vi
    .fn()
    .mockImplementation((url) => sessionReadResponse(url))
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  const input = await screen.findByLabelText("Duración prevista (minutos)");
  fireEvent.change(input, { target: { value: "25" } });
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  const check = await screen.findByRole("button", { name: "Comprobar inicio" });
  expect(input).toHaveAttribute("readonly");
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo inicio" }),
  ).not.toBeInTheDocument();
  const key = new Headers(fetcher.mock.calls[1][1].headers).get(
    "Idempotency-Key",
  );
  fireEvent.click(check);
  await screen.findByText("Sesión iniciada");
  expect(fetcher.mock.calls[2][0]).toBe(
    `/api/v1/work-sessions/by-request/${key}`,
  );
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(5));
  expect(fetcher.mock.calls[3][0]).toBe(
    `/api/v1/work-sessions/${receipt.id}/state`,
  );
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/end-time")),
  ).toHaveLength(1);
});
it("@s37 confirms the original end and preserves the receipt if active refresh fails", async () => {
  let activeReads = 0;
  const fetcher = vi
    .fn()
    .mockImplementation((url: string, init: RequestInit) => {
      if (url === `/api/v1/work-sessions/${receipt.id}/end-time`)
        return sessionReadResponse(url);
      if (url === "/api/v1/work-sessions/active") {
        activeReads += 1;
        return Promise.resolve(
          activeReads === 1
            ? Response.json({ session: null })
            : new Response(null, { status: 503 }),
        );
      }
      if (url === `/api/v1/work-sessions/${receipt.id}/state`) {
        return Promise.resolve(
          Response.json(
            {
              state: {
                session: receipt,
                status: "running",
                revision: "1",
                changedAt: receipt.startedAt,
                workedMicroseconds: "0",
                runningSince: receipt.startedAt,
              },
              serverNow: receipt.startedAt,
              netMicroseconds: "0",
            },
            {
              headers: {
                "Work-Session-Revision": `work-session-${receipt.id}-1`,
              },
            },
          ),
        );
      }
      if (
        url ===
          `/api/v1/projects/${props.projectId}/tasks/${props.taskId}/work-sessions` &&
        init.method === "POST"
      ) {
        return Promise.resolve(
          Response.json(receipt, {
            status: 201,
            headers: { Location: `/api/v1/work-sessions/${receipt.id}` },
          }),
        );
      }
      throw new Error(`Unexpected request: ${init.method ?? "GET"} ${url}`);
    });
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  fireEvent.change(
    await screen.findByLabelText("Duración prevista (minutos)"),
    { target: { value: "25" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Empezar a trabajar" }));
  expect(await screen.findByText("Sesión iniciada")).toHaveAttribute(
    "role",
    "status",
  );
  expect(
    screen.getByText(/Fin previsto:/).querySelector("time"),
  ).toHaveAttribute("dateTime", receipt.plannedEndAt);
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar sesión activa" }),
  );
  await screen.findByText("No se ha podido consultar la sesión activa.");
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No se ha podido consultar la sesión activa.",
  );
  expect(await screen.findByText("En curso")).toBeVisible();
  expect(activeReads).toBe(2);
  expect(
    fetcher.mock.calls.filter(([, init]) => init.method === "POST"),
  ).toHaveLength(1);
  expect(screen.getByText("Sesión iniciada")).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Empezar a trabajar" }),
  ).not.toBeInTheDocument();
});
it("@s32 retains the explicit duration while one start is pending", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: null }))
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  const input = await screen.findByLabelText("Duración prevista (minutos)");
  fireEvent.change(input, { target: { value: "25" } });
  const button = screen.getByRole("button", { name: "Empezar a trabajar" });
  fireEvent.click(button);
  fireEvent.click(button);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Iniciando sesión de trabajo",
  );
  expect(input).toHaveAttribute("readonly");
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    plannedMinutes: 25,
  });
  expect(
    new Headers(fetcher.mock.calls[1][1].headers).get("Idempotency-Key"),
  ).toMatch(/^[\da-f-]{36}$/);
});
it("@s29 retries a failed active lookup without claiming absence", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json({ session: null }));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se ha podido consultar la sesión activa.",
  );
  expect(
    screen.queryByLabelText("Duración prevista (minutos)"),
  ).not.toBeInTheDocument();
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar sesión activa" }),
  );
  expect(
    await screen.findByLabelText("Duración prevista (minutos)"),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s29 shows an active session from another task with a fixed end and task link", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ session: { ...receipt, taskId: receipt.id } }),
      ),
  );
  render(<WorkSession {...props} />);
  expect(
    await screen.findByRole("link", { name: "Ir a la tarea de esta sesión" }),
  ).toHaveAttribute(
    "href",
    `/proyectos/${receipt.projectId}/tareas/${receipt.id}`,
  );
  expect(screen.getByText("Duración prevista: 25 minutos")).toBeVisible();
  expect(screen.getByText(/Fin previsto:/)).toHaveTextContent("10:25");
  expect(
    screen.queryByRole("button", { name: "Empezar a trabajar" }),
  ).not.toBeInTheDocument();
});
it("@s28 shows task context and an empty explicit duration after confirmed absence", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ session: null })),
  );
  render(<WorkSession {...props} />);
  expect(
    await screen.findByText("No hay una sesión de trabajo activa."),
  ).toBeVisible();
  expect(screen.getByText(props.taskTitle)).toBeVisible();
  expect(screen.getByLabelText("Duración prevista (minutos)")).toHaveValue(
    null,
  );
  expect(
    screen.getByRole("button", { name: "Empezar a trabajar" }),
  ).toBeVisible();
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
});

it("@s29 announces an active lookup while pending without starting work", () => {
  const fetcher = vi.fn().mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSession {...props} />);
  expect(
    screen.getByRole("heading", { name: "Sesión de trabajo" }),
  ).toBeVisible();
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando sesión activa",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(fetcher.mock.calls[0][0]).toBe("/api/v1/work-sessions/active");
  expect(fetcher.mock.calls[0][1].method).toBeUndefined();
});
it("@s29 integrates paused state from active discovery even for a completed task", async () => {
  const state = {
    session: receipt,
    status: "paused",
    revision: "2",
    changedAt: "2026-09-06T10:01:00.123456Z",
    workedMicroseconds: "60000000",
    runningSince: null,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ session: receipt }))
    .mockResolvedValueOnce(
      Response.json(
        {
          state,
          serverNow: "2026-09-06T10:02:00.123456Z",
          netMicroseconds: "60000000",
        },
        {
          headers: { "Work-Session-Revision": `work-session-${receipt.id}-2` },
        },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(
    <WorkSession {...props} taskStatus="completed" projectStatus="completed" />,
  );
  expect(await screen.findByText("En pausa", {})).toBeVisible();
  expect(screen.getByRole("button", { name: "Reanudar" })).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Pausar" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByText("La pausa no desplaza el fin previsto de la sesión."),
  ).toBeVisible();
  expect(fetcher.mock.calls[1][0]).toBe(
    `/api/v1/work-sessions/${receipt.id}/state`,
  );
});
