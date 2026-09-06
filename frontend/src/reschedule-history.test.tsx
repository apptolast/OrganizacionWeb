import { afterEach, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { BlockChangeHistory } from "./reschedule-history";
import { agendaToday } from "./today-fixture";
import { observeAccess } from "./api-client";
import type { BlockChange } from "./reschedule-api";

const block = { ...agendaToday().items[0].block, zoneId: "UTC" };
const base = `/api/v1/projects/${block.projectId}/tasks/${block.taskId}/blocks`;
const props = {
  projectId: block.projectId,
  taskId: block.taskId,
  onAccessFailure: vi.fn(),
};
function moved(): BlockChange {
  return {
    id: "00000000-0000-0000-0000-000000000009",
    blockId: block.id,
    kind: "RESCHEDULED",
    revision: `"block:${block.id}:2"`,
    occurredAt: "2030-01-07T14:00:00Z",
    before: block,
    after: {
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
  props.onAccessFailure.mockClear();
});

it("@s16 @s39 reads an empty history independently of active blocks", async () => {
  const pending = deferred<Response>();
  const fetch = vi.fn().mockReturnValue(pending.promise);
  vi.stubGlobal("fetch", fetch);
  render(<BlockChangeHistory {...props} />);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando cambios de bloques",
  );
  expect(
    screen.queryByText("Todavía no hay cambios de bloques."),
  ).not.toBeInTheDocument();
  await act(async () =>
    pending.resolve(Response.json({ items: [], nextCursor: null })),
  );
  expect(screen.getByText("Todavía no hay cambios de bloques.")).toBeVisible();
  expect(
    screen.getByRole("region", { name: "Cambios de bloques" }),
  ).toBeVisible();
  expect(fetch).toHaveBeenCalledExactlyOnceWith(
    base + "/changes",
    expect.objectContaining({
      cache: "no-store",
      credentials: "same-origin",
      signal: expect.any(AbortSignal),
    }),
  );
});
it("@s16 @s36 @s39 presents dated historical moves and cancellations without reads per row", async () => {
  const movement = moved();
  const cancellation = {
    ...movement,
    id: "00000000-0000-0000-0000-000000000010",
    kind: "CANCELLED",
    revision: `"block:${block.id}:3"`,
    occurredAt: "2030-01-08T09:00:00Z",
    before: movement.after,
    after: null,
  };
  const fetch = vi
    .fn()
    .mockResolvedValue(
      Response.json({ items: [cancellation, movement], nextCursor: null }),
    );
  vi.stubGlobal("fetch", fetch);
  render(<BlockChangeHistory {...props} />);
  const list = await screen.findByRole("list", {
    name: "Historial de bloques",
  });
  const rows = within(list).getAllByRole("listitem");
  expect(rows).toHaveLength(2);
  expect(
    within(rows[0]).getByRole("heading", { name: "Cancelación" }),
  ).toBeVisible();
  expect(
    within(rows[0]).getByText("Reserva cancelada; historial conservado."),
  ).toBeVisible();
  expect(within(rows[0]).getByText("90 minutos planificados")).toBeVisible();
  expect(
    within(rows[1]).getByRole("heading", { name: "Movimiento" }),
  ).toBeVisible();
  expect(within(rows[1]).getByRole("heading", { name: "Antes" })).toBeVisible();
  expect(
    within(rows[1]).getByRole("heading", { name: "Después" }),
  ).toBeVisible();
  expect(within(rows[1]).getByText("60 minutos planificados")).toBeVisible();
  expect(within(rows[1]).getByText("90 minutos planificados")).toBeVisible();
  expect(
    rows[1].querySelector(`time[datetime="${movement.occurredAt}"]`),
  ).toHaveTextContent("UTC");
  expect(within(list).getAllByText("Confirmación histórica")).toHaveLength(2);
  expect(
    screen.queryByText("Todavía no hay cambios de bloques."),
  ).not.toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(1);
});
it("@s16 navigates older pages and returns to the recent history without appending stale rows", async () => {
  const first = { items: [moved()], nextCursor: "opaque_cursor" };
  const older = { ...moved(), kind: "CANCELLED", after: null };
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(Response.json(first))
    .mockResolvedValueOnce(Response.json({ items: [older], nextCursor: null }))
    .mockResolvedValueOnce(Response.json(first));
  vi.stubGlobal("fetch", fetch);
  render(<BlockChangeHistory {...props} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Más cambios anteriores" }),
  );
  expect(
    await screen.findByRole("heading", { name: "Cancelación" }),
  ).toBeVisible();
  expect(
    screen.queryByRole("heading", { name: "Movimiento" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Más cambios anteriores" }),
  ).not.toBeInTheDocument();
  expect(fetch.mock.calls[1][0]).toBe(base + "/changes?cursor=opaque_cursor");
  fireEvent.click(
    screen.getByRole("button", { name: "Volver a cambios recientes" }),
  );
  expect(
    await screen.findByRole("heading", { name: "Movimiento" }),
  ).toBeVisible();
  expect(
    screen.queryByRole("heading", { name: "Cancelación" }),
  ).not.toBeInTheDocument();
  expect(fetch.mock.calls[2][0]).toBe(base + "/changes");
  expect(fetch).toHaveBeenCalledTimes(3);
});
it("@s18 @s39 retries a failed history without writing or claiming an empty result", async () => {
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
  vi.stubGlobal("fetch", fetch);
  render(<BlockChangeHistory {...props} />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pudieron consultar los cambios de bloques.",
  );
  expect(
    screen.queryByText("Todavía no hay cambios de bloques."),
  ).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Reintentar cambios" }));
  expect(
    await screen.findByText("Todavía no hay cambios de bloques."),
  ).toBeVisible();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(2);
  expect(
    fetch.mock.calls.every(
      ([url, options]) => url === base + "/changes" && !options.method,
    ),
  ).toBe(true);
  expect(props.onAccessFailure).not.toHaveBeenCalled();
});
it.each(["refresh", "task", "project"])(
  "@s25 @s37 resets cursor and aborts obsolete reads on %s",
  async (change) => {
    const old = deferred<Response>();
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(
        Response.json({ items: [moved()], nextCursor: "old_cursor" }),
      )
      .mockReturnValueOnce(old.promise)
      .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
    vi.stubGlobal("fetch", fetch);
    const access = vi.fn();
    observeAccess(access);
    const view = render(<BlockChangeHistory {...props} refreshToken={0} />);
    fireEvent.click(
      await screen.findByRole("button", { name: "Más cambios anteriores" }),
    );
    const signal = fetch.mock.calls[1][1].signal as AbortSignal;
    const next = {
      ...props,
      projectId: change === "project" ? block.id : props.projectId,
      taskId: change === "task" ? block.id : props.taskId,
    };
    view.rerender(
      <BlockChangeHistory
        {...next}
        refreshToken={change === "refresh" ? 1 : 0}
      />,
    );
    expect(signal.aborted).toBe(true);
    expect(
      await screen.findByText("Todavía no hay cambios de bloques."),
    ).toBeVisible();
    expect(fetch.mock.calls[2][0]).toBe(
      `/api/v1/projects/${next.projectId}/tasks/${next.taskId}/blocks/changes`,
    );
    await act(async () => {
      old.resolve(new Response(null, { status: 401 }));
      await old.promise;
    });
    expect(access).not.toHaveBeenCalled();
    expect(props.onAccessFailure).not.toHaveBeenCalled();
    expect(
      screen.queryByRole("button", { name: "Volver a cambios recientes" }),
    ).not.toBeInTheDocument();
    expect(fetch).toHaveBeenCalledTimes(3);
  },
);
it.each([
  [401, "AUTHENTICATION_REQUIRED", true],
  [404, "RESOURCE_NOT_FOUND", true],
  [404, "BLOCK_CHANGE_NOT_FOUND", false],
])(
  "@s18 distinguishes access loss from receipt absence: %s %s",
  async (status, code, losesAccess) => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json(
          {
            type: "urn:organization:problem:" + String(code).toLowerCase(),
            title: "No disponible",
            status,
            code,
          },
          { status: Number(status) },
        ),
      ),
    );
    render(<BlockChangeHistory {...props} />);
    await screen.findByRole("alert");
    if (losesAccess)
      expect(props.onAccessFailure).toHaveBeenCalledExactlyOnceWith(status);
    else expect(props.onAccessFailure).not.toHaveBeenCalled();
    expect(
      screen.queryByRole("list", { name: "Historial de bloques" }),
    ).not.toBeInTheDocument();
  },
);
it("@s18 @s36 consults cancelled current state explicitly while retaining the historical move", async () => {
  const entry = moved();
  const current = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ items: [entry], nextCursor: null }))
    .mockReturnValueOnce(current.promise);
  vi.stubGlobal("fetch", fetch);
  render(<BlockChangeHistory {...props} />);
  const button = await screen.findByRole("button", {
    name: "Consultar estado actual",
  });
  expect(fetch).toHaveBeenCalledTimes(1);
  fireEvent.click(button);
  expect(screen.getByText("Consultando estado actual")).toBeVisible();
  expect(screen.getByRole("heading", { name: "Movimiento" })).toBeVisible();
  await act(async () =>
    current.resolve(
      Response.json(
        {
          block: entry.after,
          status: "cancelled",
          updatedAt: "2030-01-08T09:00:00Z",
        },
        { headers: { ETag: `"block:${block.id}:3"` } },
      ),
    ),
  );
  const state = screen.getByRole("region", {
    name: "Estado actual del bloque",
  });
  expect(within(state).getByText("Cancelado")).toBeVisible();
  expect(within(state).getByText("90 minutos planificados")).toBeVisible();
  expect(screen.getByText("Confirmación histórica")).toBeVisible();
  expect(screen.getByText("60 minutos planificados")).toBeVisible();
  expect(fetch.mock.calls[1][0]).toBe(base + "/" + block.id + "/state");
  expect(fetch).toHaveBeenCalledTimes(2);
});
it("@s36 @s39 keeps the receipt while failed current state is withdrawn and retried", async () => {
  const entry = moved();
  const state = (status: string) =>
    Response.json(
      { block: entry.after, status, updatedAt: entry.occurredAt },
      { headers: { ETag: `"block:${block.id}:3"` } },
    );
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ items: [entry], nextCursor: null }))
    .mockResolvedValueOnce(state("planned"))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(state("cancelled"));
  vi.stubGlobal("fetch", fetch);
  render(<BlockChangeHistory {...props} />);
  const button = await screen.findByRole("button", {
    name: "Consultar estado actual",
  });
  fireEvent.click(button);
  expect(await screen.findByText("Planificado")).toBeVisible();
  fireEvent.click(button);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Operación confirmada; estado actual sin comprobar.",
  );
  expect(
    screen.queryByRole("region", { name: "Estado actual del bloque" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "Movimiento" })).toBeVisible();
  expect(screen.getByText("Confirmación histórica")).toBeVisible();
  fireEvent.click(button);
  expect(await screen.findByText("Cancelado")).toBeVisible();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(4);
  expect(props.onAccessFailure).not.toHaveBeenCalled();
});
it.each([
  [401, "AUTHENTICATION_REQUIRED", true],
  [404, "RESOURCE_NOT_FOUND", true],
  [404, "BLOCK_NOT_FOUND", false],
])(
  "@s18 checks access on explicit state reads: %s %s",
  async (status, code, losesAccess) => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(
          Response.json({ items: [moved()], nextCursor: null }),
        )
        .mockResolvedValueOnce(
          Response.json(
            {
              type: "urn:organization:problem:" + String(code).toLowerCase(),
              title: "No disponible",
              status,
              code,
            },
            { status: Number(status) },
          ),
        ),
    );
    render(<BlockChangeHistory {...props} />);
    fireEvent.click(
      await screen.findByRole("button", { name: "Consultar estado actual" }),
    );
    await screen.findByRole("alert");
    if (losesAccess)
      expect(props.onAccessFailure).toHaveBeenCalledExactlyOnceWith(status);
    else expect(props.onAccessFailure).not.toHaveBeenCalled();
  },
);
it("@s38 keeps focus and coalesces repeated consultation while state is pending", async () => {
  const pending = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ items: [moved()], nextCursor: null }),
    )
    .mockReturnValue(pending.promise);
  vi.stubGlobal("fetch", fetch);
  render(<BlockChangeHistory {...props} />);
  const button = await screen.findByRole("button", {
    name: "Consultar estado actual",
  });
  button.focus();
  await act(async () => {
    fireEvent.click(button);
    fireEvent.click(button);
  });
  expect(fetch).toHaveBeenCalledTimes(2);
  expect(button).toHaveAttribute("aria-disabled", "true");
  expect(button).toHaveFocus();
  await act(async () =>
    pending.resolve(
      Response.json(
        { block, status: "planned", updatedAt: block.createdAt },
        { headers: { ETag: `"block:${block.id}:1"` } },
      ),
    ),
  );
  expect(button).toHaveAttribute("aria-disabled", "false");
  expect(button).toHaveFocus();
});
it.each([false, true])(
  "@s38 restores pagination focus only when the user did not choose another control: %s",
  async (movedFocus) => {
    const pending = deferred<Response>();
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(
          Response.json({ items: [moved()], nextCursor: "older" }),
        )
        .mockReturnValueOnce(pending.promise),
    );
    render(
      <>
        <button>Otro control</button>
        <BlockChangeHistory {...props} />
      </>,
    );
    const next = await screen.findByRole("button", {
      name: "Más cambios anteriores",
    });
    next.focus();
    fireEvent.click(next);
    const other = screen.getByRole("button", { name: "Otro control" });
    if (movedFocus) other.focus();
    await act(async () =>
      pending.resolve(Response.json({ items: [], nextCursor: null })),
    );
    expect(
      movedFocus
        ? other
        : screen.getByRole("heading", { name: "Cambios de bloques" }),
    ).toHaveFocus();
  },
);
it("@s37 aborts a row read in the pagination turn before obsolete 401 can reach the observer", async () => {
  const old = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ items: [moved()], nextCursor: "older" }),
    )
    .mockReturnValueOnce(old.promise)
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
  vi.stubGlobal("fetch", fetch);
  const access = vi.fn();
  observeAccess(access);
  render(<BlockChangeHistory {...props} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado actual" }),
  );
  await act(async () => {
    fireEvent.click(
      screen.getByRole("button", { name: "Más cambios anteriores" }),
    );
    old.resolve(new Response(null, { status: 401 }));
    await old.promise;
    expect(access).not.toHaveBeenCalled();
  });
  expect(
    await screen.findByText("Todavía no hay cambios de bloques."),
  ).toBeVisible();
  expect(props.onAccessFailure).not.toHaveBeenCalled();
});
it.each(["history", "state"])(
  "@s37 discards delayed %s error classification after refresh",
  async (source) => {
    let body!: ReadableStreamDefaultController<Uint8Array>;
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        body = controller;
      },
    });
    const response = new Response(stream, { status: 404 });
    const finished = response.clone();
    const fetch = vi.fn();
    if (source === "state")
      fetch.mockResolvedValueOnce(
        Response.json({ items: [moved()], nextCursor: null }),
      );
    fetch
      .mockResolvedValueOnce(response)
      .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
    vi.stubGlobal("fetch", fetch);
    const view = render(<BlockChangeHistory {...props} refreshToken={0} />);
    if (source === "state")
      fireEvent.click(
        await screen.findByRole("button", { name: "Consultar estado actual" }),
      );
    await act(async () => {});
    view.rerender(<BlockChangeHistory {...props} refreshToken={1} />);
    expect(
      await screen.findByText("Todavía no hay cambios de bloques."),
    ).toBeVisible();
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
      await finished.json();
    });
    expect(props.onAccessFailure).not.toHaveBeenCalled();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(
      screen.getByText("Todavía no hay cambios de bloques."),
    ).toBeVisible();
  },
);
it.each(["history", "state"])(
  "@s37 a late successful %s read cannot restore the prior context",
  async (source) => {
    const old = deferred<Response>();
    const fetch = vi.fn();
    if (source === "state")
      fetch.mockResolvedValueOnce(
        Response.json({ items: [moved()], nextCursor: null }),
      );
    fetch
      .mockReturnValueOnce(old.promise)
      .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
    vi.stubGlobal("fetch", fetch);
    const view = render(<BlockChangeHistory {...props} />);
    if (source === "state")
      fireEvent.click(
        await screen.findByRole("button", { name: "Consultar estado actual" }),
      );
    view.rerender(<BlockChangeHistory {...props} taskId={block.id} />);
    expect(
      await screen.findByText("Todavía no hay cambios de bloques."),
    ).toBeVisible();
    await act(async () =>
      old.resolve(
        source === "history"
          ? Response.json({ items: [moved()], nextCursor: null })
          : Response.json(
              { block, status: "planned", updatedAt: block.createdAt },
              { headers: { ETag: `"block:${block.id}:1"` } },
            ),
      ),
    );
    expect(screen.queryByText("Preparar borrador")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("region", { name: "Estado actual del bloque" }),
    ).not.toBeInTheDocument();
    expect(props.onAccessFailure).not.toHaveBeenCalled();
  },
);

it.each(["Más cambios anteriores", "Reintentar cambios"])(
  "@s38 immediately restores focus during the pending read after %s",
  async (action) => {
    const pending = deferred<Response>();
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(
          action === "Reintentar cambios"
            ? new Response(null, { status: 503 })
            : Response.json({ items: [moved()], nextCursor: "older" }),
        )
        .mockReturnValueOnce(pending.promise),
    );
    render(
      <>
        <button>Otro control</button>
        <BlockChangeHistory {...props} />
      </>,
    );
    const trigger = await screen.findByRole("button", { name: action });
    trigger.focus();
    fireEvent.click(trigger);
    expect(screen.getByRole("status")).toHaveTextContent(
      "Consultando cambios de bloques",
    );
    expect(
      screen.getByRole("heading", { name: "Cambios de bloques" }),
    ).toHaveFocus();
    const other = screen.getByRole("button", { name: "Otro control" });
    other.focus();
    await act(async () =>
      pending.resolve(Response.json({ items: [], nextCursor: null })),
    );
    expect(other).toHaveFocus();
  },
);
