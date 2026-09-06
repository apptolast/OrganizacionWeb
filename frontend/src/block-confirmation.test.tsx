import { afterEach, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { BlockConfirmation } from "./block-confirmation";
import { agendaToday } from "./today-fixture";
import { observeAccess } from "./api-client";
import type { BlockChange } from "./reschedule-api";

const block = { ...agendaToday().items[0].block, zoneId: "UTC" };
const base = `/api/v1/projects/${block.projectId}/tasks/${block.taskId}/blocks`;
const onAccessFailure = vi.fn();
function change(kind: BlockChange["kind"] = "RESCHEDULED"): BlockChange {
  return {
    id: "00000000-0000-0000-0000-000000000009",
    blockId: block.id,
    kind,
    revision: `"block:${block.id}:2"`,
    occurredAt: "2030-01-08T09:00:00Z",
    before: block,
    after:
      kind === "CANCELLED"
        ? null
        : {
            ...block,
            startAt: "2030-01-08T10:00:00Z",
            endAt: "2030-01-08T11:30:00Z",
            durationMinutes: 90,
          },
  };
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
  observeAccess();
  onAccessFailure.mockClear();
});
it.each([false, true])(
  "@s36 announces the historical confirmation accessibly (change=%s)",
  (receipt) => {
    vi.stubGlobal("fetch", vi.fn().mockReturnValue(new Promise(() => {})));
    render(
      <BlockConfirmation
        block={block}
        change={receipt ? change() : undefined}
        onAccessFailure={onAccessFailure}
      />,
    );
    expect(
      screen.getByText(
        receipt ? "Cambio confirmado (hecho histórico)" : "Bloque guardado",
      ),
    ).toHaveAttribute("role", "status");
  },
);
it.each(["projectId", "taskId", "id"] as const)(
  "@s38 isolates a changed %s while another control owns focus",
  async (field) => {
    const pending = deferred<Response>();
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { block, status: "planned", updatedAt: block.createdAt },
          { headers: { ETag: `"block:${block.id}:1"` } },
        ),
      )
      .mockReturnValueOnce(pending.promise);
    vi.stubGlobal("fetch", fetch);
    const view = render(
      <>
        <button>Exterior</button>
        <BlockConfirmation block={block} onAccessFailure={onAccessFailure} />
      </>,
    );
    screen.getByRole("button", { name: "Exterior" }).focus();
    await screen.findByText("Planificado");
    expect(screen.getByRole("button", { name: "Exterior" })).toHaveFocus();
    const next = { ...block, [field]: "00000000-0000-0000-0000-000000000099" };
    view.rerender(
      <>
        <button>Exterior</button>
        <BlockConfirmation block={next} onAccessFailure={onAccessFailure} />
      </>,
    );
    expect(screen.queryByText("Planificado")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Exterior" })).toHaveFocus();
    expect(fetch.mock.calls[0][1].signal.aborted).toBe(true);
    expect(fetch.mock.calls[1][0]).toBe(
      `/api/v1/projects/${next.projectId}/tasks/${next.taskId}/blocks/${next.id}/state`,
    );
  },
);
it("@s38 discards access classification completed after unmount", async () => {
  let body!: ReadableStreamDefaultController<Uint8Array>;
  const response = new Response(
    new ReadableStream({
      start(controller) {
        body = controller;
      },
    }),
    { status: 404 },
  );
  const completed = response.clone();
  const fetch = vi.fn().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetch);
  const view = render(
    <BlockConfirmation block={block} onAccessFailure={onAccessFailure} />,
  );
  await act(async () => {});
  view.unmount();
  expect(fetch.mock.calls[0][1].signal.aborted).toBe(true);
  await act(async () => {
    body.enqueue(
      new TextEncoder().encode(
        JSON.stringify({
          type: "urn:organization:problem:resource_not_found",
          title: "No disponible",
          status: 404,
          code: "RESOURCE_NOT_FOUND",
        }),
      ),
    );
    body.close();
    await completed.json();
  });
  expect(onAccessFailure).not.toHaveBeenCalled();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it.each(["success", "unauthorized"])(
  "@s38 ignores stale %s after receipt replacement",
  async (outcome) => {
    const old = deferred<Response>();
    const next = deferred<Response>();
    const fetch = vi
      .fn()
      .mockReturnValueOnce(old.promise)
      .mockReturnValueOnce(next.promise);
    const observer = vi.fn();
    observeAccess(observer);
    vi.stubGlobal("fetch", fetch);
    const view = render(
      <BlockConfirmation block={block} onAccessFailure={onAccessFailure} />,
    );
    const signal = fetch.mock.calls[0][1].signal as AbortSignal;
    view.rerender(
      <BlockConfirmation
        block={block}
        change={change("CANCELLED")}
        onAccessFailure={onAccessFailure}
      />,
    );
    expect(signal.aborted).toBe(true);
    await act(async () =>
      old.resolve(
        outcome === "unauthorized"
          ? new Response(null, { status: 401 })
          : Response.json(
              { block, status: "planned", updatedAt: block.createdAt },
              { headers: { ETag: `"block:${block.id}:1"` } },
            ),
      ),
    );
    expect(screen.queryByText("Planificado")).not.toBeInTheDocument();
    expect(screen.getByText("Consultando estado actual")).toBeVisible();
    expect(observer).not.toHaveBeenCalled();
    expect(onAccessFailure).not.toHaveBeenCalled();
  },
);
it("@s40 keeps retry focus local during loading and does not steal another control", async () => {
  const pending = deferred<Response>();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockReturnValueOnce(pending.promise),
  );
  render(
    <>
      <button>Otro control</button>
      <BlockConfirmation block={block} onAccessFailure={onAccessFailure} />
    </>,
  );
  const retry = await screen.findByRole("button", {
    name: "Reintentar estado actual",
  });
  retry.focus();
  fireEvent.click(retry);
  expect(screen.getByRole("heading", { name: block.objective })).toHaveFocus();
  screen.getByRole("button", { name: "Otro control" }).focus();
  await act(async () =>
    pending.resolve(
      Response.json(
        { block, status: "planned", updatedAt: block.createdAt },
        { headers: { ETag: `"block:${block.id}:1"` } },
      ),
    ),
  );
  expect(screen.getByRole("button", { name: "Otro control" })).toHaveFocus();
});
it("@s38 replaces prior state when a new receipt arrives for the same block", async () => {
  const pending = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { block, status: "planned", updatedAt: block.createdAt },
        { headers: { ETag: `"block:${block.id}:1"` } },
      ),
    )
    .mockReturnValueOnce(pending.promise);
  vi.stubGlobal("fetch", fetch);
  const view = render(
    <BlockConfirmation block={block} onAccessFailure={onAccessFailure} />,
  );
  await screen.findByText("Planificado");
  view.rerender(
    <BlockConfirmation
      block={block}
      change={change("CANCELLED")}
      onAccessFailure={onAccessFailure}
    />,
  );
  expect(screen.queryByText("Planificado")).not.toBeInTheDocument();
  expect(screen.getByText("Consultando estado actual")).toHaveTextContent(
    "Consultando estado actual",
  );
  await act(async () =>
    pending.resolve(
      Response.json(
        { block, status: "cancelled", updatedAt: change().occurredAt },
        { headers: { ETag: `"block:${block.id}:2"` } },
      ),
    ),
  );
  expect(screen.getByText("Cancelado")).toBeVisible();
  expect(fetch).toHaveBeenCalledTimes(2);
});
it.each([401, 404])(
  "@s38 reports current access failure %s",
  async (status) => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json(
          {
            type:
              "urn:organization:problem:" +
              (status === 401
                ? "authentication_required"
                : "resource_not_found"),
            title: "No disponible",
            status,
            code:
              status === 401 ? "AUTHENTICATION_REQUIRED" : "RESOURCE_NOT_FOUND",
          },
          { status },
        ),
      ),
    );
    render(
      <BlockConfirmation block={block} onAccessFailure={onAccessFailure} />,
    );
    await screen.findByRole("alert");
    expect(onAccessFailure).toHaveBeenCalledExactlyOnceWith(status);
    expect(screen.getByText("Bloque guardado")).toBeVisible();
  },
);

it("@s36 retains confirmation across two failed state reads and a third attempt", async () => {
  const pending = deferred<Response>();
  const third = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockReturnValueOnce(pending.promise)
    .mockReturnValueOnce(third.promise);
  vi.stubGlobal("fetch", fetch);
  render(<BlockConfirmation block={block} onAccessFailure={onAccessFailure} />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Operación confirmada; estado actual sin comprobar",
  );
  expect(screen.getByText("Bloque guardado")).toBeVisible();
  fireEvent.click(
    screen.getByRole("button", { name: "Reintentar estado actual" }),
  );
  expect(screen.getByText("Consultando estado actual")).toHaveTextContent(
    "Consultando estado actual",
  );
  await act(async () => pending.resolve(new Response(null, { status: 503 })));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Operación confirmada; estado actual sin comprobar",
  );
  expect(screen.getByText("Bloque guardado")).toBeVisible();
  fireEvent.click(
    screen.getByRole("button", { name: "Reintentar estado actual" }),
  );
  expect(screen.getByText("Consultando estado actual")).toBeVisible();
  await act(async () =>
    third.resolve(
      Response.json(
        { block, status: "planned", updatedAt: block.createdAt },
        { headers: { ETag: `"block:${block.id}:1"` } },
      ),
    ),
  );
  expect(screen.getByText("Planificado")).toBeVisible();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(3);
  expect(fetch.mock.calls.map(([url]) => url)).toEqual([
    `${base}/${block.id}/state`,
    `${base}/${block.id}/state`,
    `${base}/${block.id}/state`,
  ]);
  expect(onAccessFailure).not.toHaveBeenCalled();
});

it("@s36 keeps the original creation visible while automatically checking current state", async () => {
  const pending = deferred<Response>();
  const fetch = vi.fn().mockReturnValue(pending.promise);
  vi.stubGlobal("fetch", fetch);
  render(<BlockConfirmation block={block} onAccessFailure={onAccessFailure} />);
  expect(screen.getByText("Bloque guardado")).toBeVisible();
  expect(
    screen.getByText("Confirmación original de creación (hecho histórico)"),
  ).toBeVisible();
  expect(screen.getByText("60 minutos planificados")).toBeVisible();
  expect(screen.getByText("Consultando estado actual")).toHaveTextContent(
    "Consultando estado actual",
  );
  expect(screen.queryByText("Planificado")).not.toBeInTheDocument();
  await act(async () =>
    pending.resolve(
      Response.json(
        { block, status: "planned", updatedAt: block.createdAt },
        { headers: { ETag: `"block:${block.id}:1"` } },
      ),
    ),
  );
  const state = screen.getByRole("region", {
    name: "Estado actual del bloque",
  });
  expect(within(state).getByText("Planificado")).toBeVisible();
  expect(
    screen.getByText("Confirmación original de creación (hecho histórico)"),
  ).toBeVisible();
  expect(fetch).toHaveBeenCalledExactlyOnceWith(
    base + "/" + block.id + "/state",
    expect.objectContaining({
      signal: expect.any(AbortSignal),
      credentials: "same-origin",
      cache: "no-store",
    }),
  );
});
it.each(["RESCHEDULED", "CANCELLED"] as const)(
  "@s36 shows the %s receipt independently of a later cancelled state",
  async (kind) => {
    const receipt = change(kind);
    const current =
      kind === "CANCELLED"
        ? block
        : {
            ...block,
            startAt: "2030-01-08T14:00:00Z",
            endAt: "2030-01-08T16:00:00Z",
            durationMinutes: 120,
          };
    const fetch = vi.fn().mockResolvedValue(
      Response.json(
        {
          block: current,
          status: "cancelled",
          updatedAt: receipt.occurredAt,
        },
        {
          headers: {
            ETag: `"block:${block.id}:${kind === "CANCELLED" ? 2 : 4}"`,
          },
        },
      ),
    );
    vi.stubGlobal("fetch", fetch);
    render(
      <BlockConfirmation
        block={block}
        change={receipt}
        onAccessFailure={onAccessFailure}
      />,
    );
    expect(
      screen.getByText("Cambio confirmado (hecho histórico)"),
    ).toBeVisible();
    expect(
      screen.getByText(kind === "CANCELLED" ? "Cancelación" : "Movimiento"),
    ).toBeVisible();
    expect(screen.getByText(receipt.id)).toBeVisible();
    expect(screen.getByText("60 minutos planificados")).toBeVisible();
    if (kind === "RESCHEDULED")
      expect(screen.getByText("90 minutos planificados")).toBeVisible();
    else
      expect(
        screen.getByText("Reserva cancelada; historial conservado."),
      ).toBeVisible();
    const state = await screen.findByRole("region", {
      name: "Estado actual del bloque",
    });
    expect(within(state).getByText("Cancelado")).toBeVisible();
    expect(
      within(state).getByText(
        `${current.durationMinutes} minutos planificados`,
      ),
    ).toBeVisible();
    expect(screen.getAllByText("60 minutos planificados")[0]).toBeVisible();
    expect(fetch).toHaveBeenCalledTimes(1);
  },
);
