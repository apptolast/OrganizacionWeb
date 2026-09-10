// @ts-nocheck
import {
  fireEvent,
  render,
  screen,
  cleanup,
  within,
  waitFor,
  act,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { ImportData } from "./import-data";
import { File as NodeFile } from "node:buffer";
import { webcrypto } from "node:crypto";
import { useLayoutEffect } from "react";
import { App } from "./App";
import { AppearanceProvider } from "./appearance-state";

async function fileFixture() {
  const collections =
    "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues".split(
      " ",
    );
  const counts = Object.fromEntries(collections.map((key) => [key, 0]));
  const document = {
    format: "organizationweb-export",
    schemaVersion: 1,
    owner: "Ana",
    exportedAt: "2026-09-08T10:20:30.123456Z",
    data: Object.fromEntries(collections.map((key) => [key, []])),
    counts,
  };
  const file = new NodeFile([JSON.stringify(document)], "copy.json", {
    type: "application/json",
  });
  const hash = Buffer.from(
    await webcrypto.subtle.digest("SHA-256", await file.arrayBuffer()),
  ).toString("hex");
  const preview = {
    format: "organizationweb-import-preview",
    schemaVersion: 1,
    fileSha256: hash,
    byteLength: file.size,
    owner: "Ana",
    exportedAt: document.exportedAt,
    counts,
    insertCounts: counts,
    identicalCounts: counts,
    runningSessions: [],
  };
  vi.stubGlobal("crypto", webcrypto);
  return { file, preview };
}

function problem(
  status: number,
  code: string,
  fields: Record<string, unknown> = {},
) {
  return Response.json(
    {
      status,
      code,
      type: `urn:organization:problem:${code.toLowerCase()}`,
      ...fields,
    },
    { status },
  );
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  sessionStorage.removeItem("organizationweb.import.pending.v1");
  window.history.replaceState(null, "", "/");
});

it.each([
  [200, "urn:organization:problem:import_conflict"],
  [409, "urn:organization:problem:other"],
] as const)(
  "@s37 keeps uncertainty for contradictory problem status=%s type=%s",
  async (status, type) => {
    const { file, preview } = await fileFixture();
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(Response.json(preview))
      .mockResolvedValueOnce(
        Response.json(
          { code: "IMPORT_CONFLICT", type, status },
          { status: 409 },
        ),
      );
    vi.stubGlobal("fetch", fetcher);
    render(<ImportData owner="Ana" />);
    fireEvent.change(screen.getByLabelText("Archivo JSON"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
    fireEvent.click(
      await screen.findByRole("button", { name: "Confirmar importación" }),
    );
    await screen.findByRole("button", { name: "Comprobar resultado" });
    expect(
      screen.queryByRole("button", { name: "Elegir otra copia" }),
    ).not.toBeInTheDocument();
    expect(
      sessionStorage.getItem("organizationweb.import.pending.v1"),
    ).not.toBeNull();
    expect(fetcher).toHaveBeenCalledTimes(2);
  },
);

it("@s38 does not reuse an earlier missing-receipt permission for a new rejected intention", async () => {
  const { file, preview } = await fileFixture();
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: preview.fileSha256,
  };
  sessionStorage.setItem(
    "organizationweb.import.pending.v1",
    JSON.stringify(intent),
  );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ code: "IMPORT_NOT_FOUND" }, { status: 404 }),
    )
    .mockResolvedValueOnce(
      Response.json({
        requestKey: intent.requestKey,
        fileSha256: intent.fileSha256,
        byteLength: file.size,
        recordedAt: "2026-09-08T10:30:00.000001Z",
        outcome: "NO_CHANGE",
        insertedCounts: preview.counts,
        identicalCounts: preview.counts,
      }),
    )
    .mockResolvedValueOnce(Response.json(preview))
    .mockResolvedValueOnce(problem(409, "IMPORT_CONFLICT"));
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.click(screen.getByRole("button", { name: "Comprobar resultado" }));
  await screen.findByRole("button", { name: "Reenviar la misma importación" });
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Reenviar la misma importación" }),
  );
  await screen.findByRole("table", { name: "Resultado confirmado" });
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar importación" }),
  );
  await screen.findByRole("button", { name: "Elegir otra copia" });
  expect(
    screen.queryByRole("button", { name: "Reenviar la misma importación" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(4);
  const current = JSON.parse(
    sessionStorage.getItem("organizationweb.import.pending.v1")!,
  );
  expect(current.requestKey).not.toBe(intent.requestKey);
});

it("@s42 restores focus when cancelling an already prepared preview", async () => {
  const { file, preview } = await fileFixture();
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(preview)));
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  await screen.findByRole("button", { name: "Confirmar importación" });
  const cancel = screen.getByRole("button", { name: "Cancelar preparación" });
  cancel.focus();
  fireEvent.click(cancel);
  expect(
    screen.getByRole("heading", { name: "Importar mis datos" }),
  ).toHaveFocus();
  expect(
    screen.queryByRole("button", { name: "Confirmar importación" }),
  ).not.toBeInTheDocument();
});

it("@s41 uses the current snapshot refresh callback when a receipt finishes later", async () => {
  const { preview } = await fileFixture();
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: preview.fileSha256,
  };
  sessionStorage.setItem(
    "organizationweb.import.pending.v1",
    JSON.stringify(intent),
  );
  let finish!: (response: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi.fn(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    ),
  );
  const previous = vi.fn();
  const current = vi.fn();
  const view = render(<ImportData owner="Ana" onImported={previous} />);
  fireEvent.click(screen.getByRole("button", { name: "Comprobar resultado" }));
  view.rerender(<ImportData owner="Ana" onImported={current} />);
  await act(async () =>
    finish(
      Response.json({
        requestKey: intent.requestKey,
        fileSha256: intent.fileSha256,
        byteLength: preview.byteLength,
        recordedAt: "2026-09-08T10:30:00.000001Z",
        outcome: "NO_CHANGE",
        insertedCounts: preview.counts,
        identicalCounts: preview.counts,
      }),
    ),
  );
  expect(current).toHaveBeenCalledOnce();
  expect(previous).not.toHaveBeenCalled();
});

it.each(["clean", "uncertain", "pending"])(
  "@s41 refreshes imported appearance preserving prior operation (%s)",
  async (state) => {
    const uncertain = state !== "clean";
    const { preview } = await fileFixture();
    const intent = {
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: preview.fileSha256,
    };
    sessionStorage.setItem(
      "organizationweb.import.pending.v1",
      JSON.stringify(intent),
    );
    window.history.replaceState(
      null,
      "",
      uncertain ? "/apariencia" : "/importacion",
    );
    let appearanceReads = 0;
    const fetcher = vi
      .fn()
      .mockImplementation((url: string, init?: RequestInit) => {
        if (url === "/api/v1/me/appearance") {
          if (init?.method === "PUT")
            return state === "pending"
              ? new Promise<Response>(() => {})
              : Promise.resolve(new Response(null, { status: 503 }));
          appearanceReads++;
          return Promise.resolve(
            appearanceReads === 1
              ? Response.json(
                  {
                    configured: false,
                    theme: "SYSTEM",
                    accentLight: "#244C3C",
                    accentDark: "#B7E4C7",
                    updatedAt: null,
                  },
                  { headers: { ETag: '"appearance:unconfigured"' } },
                )
              : Response.json(
                  {
                    configured: true,
                    theme: "DARK",
                    accentLight: "#0000FF",
                    accentDark: "#00FFFF",
                    updatedAt: "2026-09-08T10:30:00Z",
                  },
                  {
                    headers: {
                      ETag: '"appearance:00000000-0000-4000-8000-000000000002:0"',
                    },
                  },
                ),
          );
        }
        if (url === `/api/v1/me/imports/by-key/${intent.requestKey}`)
          return Promise.resolve(
            Response.json({
              requestKey: intent.requestKey,
              fileSha256: intent.fileSha256,
              byteLength: preview.byteLength,
              recordedAt: "2026-09-08T10:30:00.000001Z",
              outcome: "IMPORTED",
              insertedCounts: { ...preview.counts, appearance: 1 },
              identicalCounts: preview.counts,
            }),
          );
        throw new Error(`Unexpected request ${url}`);
      });
    vi.stubGlobal("fetch", fetcher);
    render(
      <AppearanceProvider>
        <App username="Ana" />
      </AppearanceProvider>,
    );
    await waitFor(() =>
      expect(document.documentElement.style.colorScheme).toBe("light"),
    );
    expect(appearanceReads).toBe(1);
    if (uncertain) {
      fireEvent.click(screen.getByRole("radio", { name: "Oscuro" }));
      fireEvent.click(
        screen.getByRole("button", { name: "Guardar apariencia" }),
      );
      if (state === "uncertain")
        await screen.findByText(/No podemos confirmar el guardado/);
      fireEvent.click(screen.getByRole("link", { name: "Importación" }));
    }
    fireEvent.click(
      screen.getByRole("button", { name: "Comprobar resultado" }),
    );
    await screen.findByRole("table", { name: "Resultado confirmado" });
    if (uncertain) {
      await screen.findByText(/La importación está confirmada, pero/);
      expect(appearanceReads).toBe(1);
      if (state === "uncertain") {
        fireEvent.click(screen.getByRole("link", { name: "Apariencia" }));
        expect(
          screen.getByRole("button", { name: "Guardar apariencia" }),
        ).toHaveAttribute("aria-disabled", "true");
      }
    } else {
      await waitFor(() =>
        expect(document.documentElement.style.colorScheme).toBe("dark"),
      );
      expect(appearanceReads).toBe(2);
    }
  },
);

it.each([false, true])(
  "@s42 restores lost recovery focus except after voluntary movement (moved=%s)",
  async (moved) => {
    const { preview } = await fileFixture();
    const intent = {
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: preview.fileSha256,
    };
    sessionStorage.setItem(
      "organizationweb.import.pending.v1",
      JSON.stringify(intent),
    );
    let finish!: (response: Response) => void;
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(
        () =>
          new Promise<Response>((resolve) => {
            finish = resolve;
          }),
      ),
    );
    render(<ImportData owner="Ana" />);
    const button = screen.getByRole("button", { name: "Comprobar resultado" });
    button.focus();
    fireEvent.click(button);
    expect(button).toBeDisabled();
    if (moved) {
      const chosen = document.createElement("button");
      document.body.append(chosen);
      chosen.focus();
      chosen.blur();
      chosen.remove();
    }
    await act(async () =>
      finish(
        Response.json({
          requestKey: intent.requestKey,
          fileSha256: intent.fileSha256,
          byteLength: preview.byteLength,
          recordedAt: "2026-09-08T10:30:00.000001Z",
          outcome: "NO_CHANGE",
          insertedCounts: preview.counts,
          identicalCounts: preview.counts,
        }),
      ),
    );
    if (moved) expect(document.body).toHaveFocus();
    else
      expect(
        screen.getByRole("heading", { name: "Importar mis datos" }),
      ).toHaveFocus();
  },
);

it("@s42 places initial focus in the import heading and retains the workspace skip target", () => {
  render(<ImportData owner="Ana" />);
  expect(
    screen.getByRole("heading", { name: "Importar mis datos" }),
  ).toHaveFocus();
  expect(screen.getByRole("main")).toHaveAttribute("id", "proyectos");
});

it("@s38 shows exact confirmed counts when recovering without the original file", async () => {
  const { preview } = await fileFixture();
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: preview.fileSha256,
  };
  sessionStorage.setItem(
    "organizationweb.import.pending.v1",
    JSON.stringify(intent),
  );
  const fetcher = vi.fn().mockResolvedValue(
    Response.json({
      requestKey: intent.requestKey,
      fileSha256: intent.fileSha256,
      byteLength: preview.byteLength,
      recordedAt: "2026-09-08T10:30:00.000001Z",
      outcome: "IMPORTED",
      insertedCounts: { ...preview.counts, projects: 2 },
      identicalCounts: { ...preview.counts, tasks: 3 },
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.click(screen.getByRole("button", { name: "Comprobar resultado" }));
  const table = await screen.findByRole("table", {
    name: "Resultado confirmado",
  });
  expect(within(table).getAllByRole("row")).toHaveLength(15);
  expect(
    within(table).getByRole("row", { name: "Proyectos 2 0" }),
  ).toBeVisible();
  expect(within(table).getByRole("row", { name: "Tareas 0 3" })).toBeVisible();
  expect(
    screen.getByText("Registrado: 2026-09-08T10:30:00.000001Z"),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledOnce();
});

it("@s34 warns that an imported running session keeps its historical clock", async () => {
  const { file, preview } = await fileFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...preview,
        counts: { ...preview.counts, workSessions: 1 },
        insertCounts: { ...preview.insertCounts, workSessions: 1 },
        runningSessions: [
          {
            sessionId: "00000000-0000-4000-8000-000000000001",
            runningSince: "2020-01-01T00:00:00.123456Z",
          },
        ],
      }),
    ),
  );
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  expect(await screen.findByText(/2020-01-01T00:00:00.123456Z/)).toBeVisible();
  expect(
    screen.getByText(
      "La sesión seguirá en curso. Se conserva su inicio histórico y el cómputo puede incluir el tiempo transcurrido; no se pausa al importar.",
    ),
  ).toBeVisible();
  expect(
    screen.getByRole("button", { name: "Confirmar importación" }),
  ).toBeEnabled();
});

it("@s39 explains a changed reupload without sending it or changing the pending key", async () => {
  const { preview } = await fileFixture();
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: preview.fileSha256,
  };
  sessionStorage.setItem(
    "organizationweb.import.pending.v1",
    JSON.stringify(intent),
  );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ code: "IMPORT_NOT_FOUND" }, { status: 404 }),
    )
    .mockResolvedValueOnce(
      Response.json({
        requestKey: intent.requestKey,
        fileSha256: intent.fileSha256,
        byteLength: preview.byteLength,
        recordedAt: "2026-09-08T10:30:00.000001Z",
        outcome: "NO_CHANGE",
        insertedCounts: preview.counts,
        identicalCounts: preview.counts,
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.click(screen.getByRole("button", { name: "Comprobar resultado" }));
  await screen.findByRole("button", { name: "Reenviar la misma importación" });
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [new NodeFile(["{}"], "different.json")] },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Reenviar la misma importación" }),
  );
  expect(
    await screen.findByText(
      "El archivo no coincide con la intención pendiente. Selecciona los mismos bytes.",
    ),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledOnce();
  expect(sessionStorage.getItem("organizationweb.import.pending.v1")).toBe(
    JSON.stringify(intent),
  );
  fireEvent.click(screen.getByRole("button", { name: "Comprobar resultado" }));
  await screen.findByRole("table", { name: "Resultado confirmado" });
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it.each([false, true])(
  "@s38 resends the same File and retains prior uncertainty if rejected (rejected=%s)",
  async (rejected) => {
    const { file, preview } = await fileFixture();
    const intent = {
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: preview.fileSha256,
    };
    sessionStorage.setItem(
      "organizationweb.import.pending.v1",
      JSON.stringify(intent),
    );
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        Response.json({ code: "IMPORT_NOT_FOUND" }, { status: 404 }),
      )
      .mockResolvedValueOnce(
        rejected
          ? problem(409, "IMPORT_CONFLICT")
          : Response.json({
              requestKey: intent.requestKey,
              fileSha256: intent.fileSha256,
              byteLength: file.size,
              recordedAt: "2026-09-08T10:21:30.123456Z",
              outcome: "NO_CHANGE",
              insertedCounts: preview.insertCounts,
              identicalCounts: preview.identicalCounts,
            }),
      );
    vi.stubGlobal("fetch", fetcher);
    render(<ImportData owner="Ana" />);
    fireEvent.click(
      screen.getByRole("button", { name: "Comprobar resultado" }),
    );
    await screen.findByRole("button", {
      name: "Reenviar la misma importación",
    });
    fireEvent.change(screen.getByLabelText("Archivo JSON"), {
      target: { files: [file] },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Reenviar la misma importación" }),
    );
    if (rejected) {
      await waitFor(() =>
        expect(
          screen.getByRole("button", { name: "Comprobar resultado" }),
        ).toBeEnabled(),
      );
      expect(
        screen.queryByRole("button", { name: "Elegir otra copia" }),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByText("Importación confirmada."),
      ).not.toBeInTheDocument();
    } else
      await screen.findByText(
        "Importación confirmada. No había registros nuevos que añadir.",
      );
    expect(fetcher).toHaveBeenCalledTimes(2);
    expect(fetcher.mock.calls[1][0]).toBe("/api/v1/me/import");
    expect(
      new Headers(fetcher.mock.calls[1][1].headers).get("Idempotency-Key"),
    ).toBe(intent.requestKey);
    expect(fetcher.mock.calls[1][1].body).toBe(file);
    expect(sessionStorage.getItem("organizationweb.import.pending.v1")).toBe(
      rejected ? JSON.stringify(intent) : null,
    );
  },
);

it("@s38 retains a missing receipt and asks for the same File before deliberate resend", async () => {
  const { preview } = await fileFixture();
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: preview.fileSha256,
  };
  sessionStorage.setItem(
    "organizationweb.import.pending.v1",
    JSON.stringify(intent),
  );
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      Response.json({ code: "IMPORT_NOT_FOUND" }, { status: 404 }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.click(screen.getByRole("button", { name: "Comprobar resultado" }));
  expect(
    await screen.findByRole("button", {
      name: "Reenviar la misma importación",
    }),
  ).toBeDisabled();
  expect(screen.getByLabelText("Archivo JSON")).toBeEnabled();
  expect(
    screen.getByText(
      "Todavía no hay recibo. La petición anterior puede seguir en curso; selecciona los mismos bytes para reenviar deliberadamente la misma intención.",
    ),
  ).toBeVisible();
  expect(sessionStorage.getItem("organizationweb.import.pending.v1")).toBe(
    JSON.stringify(intent),
  );
  expect(fetcher).toHaveBeenCalledOnce();
});

it("@s35 cancels preparation and discards the File without accepting its late response", async () => {
  const { file, preview } = await fileFixture();
  let finish!: (response: Response) => void;
  let signal!: AbortSignal;
  const fetcher = vi.fn((_url, options) => {
    signal = options.signal;
    return new Promise<Response>((resolve) => {
      finish = resolve;
    });
  });
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  fireEvent.click(screen.getByRole("button", { name: "Cancelar preparación" }));
  expect(signal.aborted).toBe(true);
  await act(async () => finish(Response.json(preview)));
  expect(
    screen.queryByRole("heading", { name: "Vista previa" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Validar archivo" }),
  ).toBeDisabled();
  expect(
    sessionStorage.getItem("organizationweb.import.pending.v1"),
  ).toBeNull();
  expect(fetcher).toHaveBeenCalledOnce();
});

it("@s36 never sends confirmation if saving its intention fails", async () => {
  const { file, preview } = await fileFixture();
  const fetcher = vi.fn().mockResolvedValue(Response.json(preview));
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  const confirm = await screen.findByRole("button", {
    name: "Confirmar importación",
  });
  vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
    throw new Error("quota");
  });
  fireEvent.click(confirm);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se puede acceder al almacenamiento de recuperación.",
  );
  expect(confirm).toBeDisabled();
  expect(fetcher).toHaveBeenCalledOnce();
});

it("@s36 explains unavailable recovery storage without crashing or sending", () => {
  vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
    throw new Error("blocked storage");
  });
  const fetcher = vi.fn();
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No se puede acceder al almacenamiento de recuperación. No se enviará la importación.",
  );
  expect(fetcher).not.toHaveBeenCalled();
});

it("@s40 aborts receipt recovery on route exit and does not clear a newer intention", async () => {
  const { file, preview } = await fileFixture();
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: preview.fileSha256,
  };
  sessionStorage.setItem(
    "organizationweb.import.pending.v1",
    JSON.stringify(intent),
  );
  let finish!: (response: Response) => void;
  let signal!: AbortSignal;
  vi.stubGlobal(
    "fetch",
    vi.fn((_url, options) => {
      signal = options.signal;
      return new Promise<Response>((resolve) => {
        finish = resolve;
      });
    }),
  );
  const imported = vi.fn();
  const view = render(<ImportData owner="Ana" onImported={imported} />);
  fireEvent.click(screen.getByRole("button", { name: "Comprobar resultado" }));
  view.unmount();
  expect(signal.aborted).toBe(true);
  const newer = {
    ...intent,
    owner: "Bruno",
    requestKey: "00000000-0000-4000-8000-000000000002",
  };
  sessionStorage.setItem(
    "organizationweb.import.pending.v1",
    JSON.stringify(newer),
  );
  finish(
    Response.json({
      requestKey: intent.requestKey,
      fileSha256: preview.fileSha256,
      byteLength: file.size,
      recordedAt: "2026-09-08T10:21:30.123456Z",
      outcome: "NO_CHANGE",
      insertedCounts: preview.insertCounts,
      identicalCounts: preview.identicalCounts,
    }),
  );
  await waitFor(() =>
    expect(sessionStorage.getItem("organizationweb.import.pending.v1")).toBe(
      JSON.stringify(newer),
    ),
  );
  expect(imported).not.toHaveBeenCalled();
});

it("@s40 aborts the old identity before the new owner's layout and ignores late preview", async () => {
  const { file, preview } = await fileFixture();
  let finish!: (response: Response) => void;
  let oldSignal!: AbortSignal;
  vi.stubGlobal(
    "fetch",
    vi.fn((_url, options) => {
      oldSignal = options.signal;
      return new Promise<Response>((resolve) => {
        finish = resolve;
      });
    }),
  );
  const layout = vi.fn();
  function Probe({ owner }: { owner: string }) {
    useLayoutEffect(() => {
      if (owner === "Bruno") layout(oldSignal.aborted);
    }, [owner]);
    return null;
  }
  const view = render(
    <>
      <ImportData owner="Ana" />
      <Probe owner="Ana" />
    </>,
  );
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  view.rerender(
    <>
      <ImportData owner="Bruno" />
      <Probe owner="Bruno" />
    </>,
  );
  expect(layout).toHaveBeenCalledWith(true);
  finish(Response.json(preview));
  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Validar archivo" }),
    ).toBeDisabled(),
  );
  expect(
    screen.queryByRole("heading", { name: "Vista previa" }),
  ).not.toBeInTheDocument();
});

it.each([
  [
    409,
    "IMPORT_CONFLICT",
    "La copia entra en conflicto con los datos actuales.",
  ],
  [
    400,
    "IMPORT_INVALID_REQUEST",
    "La solicitud de importación no es válida. Vuelve a elegir y validar la copia.",
  ],
  [
    412,
    "IMPORT_FILE_CHANGED",
    "El archivo enviado no coincide con el validado. Vuelve a elegir y validar la copia.",
  ],
  [
    409,
    "IMPORT_KEY_REUSED",
    "La clave ya se utilizó con otra copia. Esta solicitud no se ha aplicado; vuelve a elegir y validar el archivo.",
  ],
] as const)(
  "@s30 allows deliberate correction after first confirmation rejection %s %s",
  async (status, code, message) => {
    const { file, preview } = await fileFixture();
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(Response.json(preview))
      .mockResolvedValueOnce(problem(status, code));
    vi.stubGlobal("fetch", fetcher);
    render(<ImportData owner="Ana" />);
    fireEvent.change(screen.getByLabelText("Archivo JSON"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
    fireEvent.click(
      await screen.findByRole("button", { name: "Confirmar importación" }),
    );
    const correct = await screen.findByRole("button", {
      name: "Elegir otra copia",
    });
    expect(screen.getByRole("alert")).toHaveTextContent(message);
    expect(fetcher).toHaveBeenCalledTimes(2);
    fireEvent.click(correct);
    expect(
      sessionStorage.getItem("organizationweb.import.pending.v1"),
    ).toBeNull();
    expect(screen.getByLabelText("Archivo JSON")).toBeEnabled();
    expect(
      screen.getByRole("button", { name: "Validar archivo" }),
    ).toBeDisabled();
    expect(
      screen.queryByRole("button", { name: "Comprobar resultado" }),
    ).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Archivo JSON"), {
      target: { files: [file] },
    });
    expect(
      screen.getByRole("button", { name: "Validar archivo" }),
    ).toBeEnabled();
    expect(fetcher).toHaveBeenCalledTimes(2);
  },
);

it.each([
  [
    413,
    "IMPORT_TOO_LARGE",
    "El archivo supera el límite de 32 MiB o 100.000 registros.",
  ],
  [
    400,
    "IMPORT_INVALID_FILE",
    "El archivo no es una copia propia válida y compatible. Selecciona otra copia JSON v1.",
  ],
  [
    409,
    "IMPORT_CONFLICT",
    "La copia entra en conflicto con los datos actuales. No se ha importado; selecciona una copia compatible.",
  ],
] as const)(
  "@s30 explains preview rejection %s %s without private details",
  async (status, code, message) => {
    const { file } = await fileFixture();
    const fetcher = vi
      .fn()
      .mockResolvedValue(problem(status, code, { detail: "private stack" }));
    vi.stubGlobal("fetch", fetcher);
    render(<ImportData owner="Ana" />);
    fireEvent.change(screen.getByLabelText("Archivo JSON"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.queryByText(/private stack/)).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Validar archivo" }),
    ).toBeEnabled();
    expect(fetcher).toHaveBeenCalledOnce();
    fireEvent.change(screen.getByLabelText("Archivo JSON"), {
      target: { files: [new File(["{}"], "smaller.json")] },
    });
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  },
);

it("@s36 disables a pending confirmation and never creates a second request key", async () => {
  const { file, preview } = await fileFixture();
  let finish!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json(preview))
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  const confirm = await screen.findByRole("button", {
    name: "Confirmar importación",
  });
  fireEvent.click(confirm);
  expect(confirm).toBeDisabled();
  expect(screen.getByRole("status")).toHaveTextContent(
    "Confirmando importación",
  );
  fireEvent.click(confirm);
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  const intent = JSON.parse(
    sessionStorage.getItem("organizationweb.import.pending.v1")!,
  );
  finish(
    Response.json({
      requestKey: intent.requestKey,
      fileSha256: preview.fileSha256,
      byteLength: file.size,
      recordedAt: "2026-09-08T10:21:30.123456Z",
      outcome: "NO_CHANGE",
      insertedCounts: preview.insertCounts,
      identicalCounts: preview.identicalCounts,
    }),
  );
  await screen.findByText(
    "Importación confirmada. No había registros nuevos que añadir.",
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s34 blocks duplicate validation and announces the outstanding request", async () => {
  const { file, preview } = await fileFixture();
  let finish!: (response: Response) => void;
  const fetcher = vi.fn(
    () =>
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  const validate = screen.getByRole("button", { name: "Validar archivo" });
  fireEvent.click(validate);
  expect(screen.getByRole("status")).toHaveTextContent("Validando archivo");
  expect(validate).toBeDisabled();
  expect(screen.getByLabelText("Archivo JSON")).toBeDisabled();
  fireEvent.click(validate);
  expect(fetcher).toHaveBeenCalledOnce();
  finish(Response.json(preview));
  expect(
    await screen.findByRole("heading", { name: "Vista previa" }),
  ).toBeVisible();
  expect(validate).toBeEnabled();
});

it("@s41 keeps the receipt confirmed if refreshing dependent snapshots fails", async () => {
  const { file, preview } = await fileFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json(preview))
      .mockImplementationOnce(async () => {
        const intent = JSON.parse(
          sessionStorage.getItem("organizationweb.import.pending.v1")!,
        );
        return Response.json({
          requestKey: intent.requestKey,
          fileSha256: preview.fileSha256,
          byteLength: file.size,
          recordedAt: "2026-09-08T10:21:30.123456Z",
          outcome: "NO_CHANGE",
          insertedCounts: preview.insertCounts,
          identicalCounts: preview.identicalCounts,
        });
      }),
  );
  render(
    <ImportData
      owner="Ana"
      onImported={() => {
        throw new Error("refresh failed");
      }}
    />,
  );
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar importación" }),
  );
  expect(
    await screen.findByText(
      "Importación confirmada. No había registros nuevos que añadir.",
    ),
  ).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Comprobar resultado" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByText(
      "La importación está confirmada, pero no se han podido actualizar todas las vistas.",
    ),
  ).toBeVisible();
});

it("@s38 recovers after reload only by a deliberate GET without the File", async () => {
  const { file, preview } = await fileFixture();
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: preview.fileSha256,
  };
  sessionStorage.setItem(
    "organizationweb.import.pending.v1",
    JSON.stringify(intent),
  );
  const fetcher = vi.fn().mockResolvedValue(
    Response.json({
      requestKey: intent.requestKey,
      fileSha256: intent.fileSha256,
      byteLength: file.size,
      recordedAt: "2026-09-08T10:21:30.123456Z",
      outcome: "NO_CHANGE",
      insertedCounts: preview.insertCounts,
      identicalCounts: preview.identicalCounts,
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  expect(
    screen.getByRole("button", { name: "Comprobar resultado" }),
  ).toBeEnabled();
  expect(fetcher).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole("button", { name: "Comprobar resultado" }));
  expect(
    await screen.findByText(
      "Importación confirmada. No había registros nuevos que añadir.",
    ),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledOnce();
  expect(fetcher.mock.calls[0][0]).toBe(
    `/api/v1/me/imports/by-key/${intent.requestKey}`,
  );
  expect(fetcher.mock.calls[0][1].body).toBeUndefined();
  expect(
    sessionStorage.getItem("organizationweb.import.pending.v1"),
  ).toBeNull();
});

it("@s36 keeps confirmation successful when retiring its metadata fails", async () => {
  const { file, preview } = await fileFixture();
  const imported = vi.fn();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json(preview))
      .mockImplementationOnce(async () => {
        const intent = JSON.parse(
          sessionStorage.getItem("organizationweb.import.pending.v1")!,
        );
        vi.spyOn(Storage.prototype, "removeItem").mockImplementationOnce(() => {
          throw new Error("denied");
        });
        return Response.json({
          ...intent,
          byteLength: file.size,
          recordedAt: "2026-09-08T10:21:30.123456Z",
          outcome: "NO_CHANGE",
          insertedCounts: preview.insertCounts,
          identicalCounts: preview.identicalCounts,
          owner: undefined,
        });
      }),
  );
  render(<ImportData owner="Ana" onImported={imported} />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar importación" }),
  );
  expect(
    await screen.findByText(
      "Importación confirmada. No había registros nuevos que añadir.",
    ),
  ).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Comprobar resultado" }),
  ).not.toBeInTheDocument();
  expect(imported).toHaveBeenCalledOnce();
});

it("@s37 preserves an uncertain confirmation instead of offering a fresh import", async () => {
  const { file, preview } = await fileFixture();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json(preview))
    .mockRejectedValueOnce(new TypeError("private network details"));
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar importación" }),
  );
  expect(
    await screen.findByRole("button", { name: "Comprobar resultado" }),
  ).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Confirmar importación" }),
  ).not.toBeInTheDocument();
  expect(screen.getByLabelText("Archivo JSON")).toBeDisabled();
  expect(
    screen.getByRole("button", { name: "Validar archivo" }),
  ).toBeDisabled();
  expect(screen.queryByText(/private network/)).not.toBeInTheDocument();
  expect(
    sessionStorage.getItem("organizationweb.import.pending.v1"),
  ).not.toBeNull();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s36 stores the exact intention before POST and announces only its confirmed receipt", async () => {
  const { file, preview } = await fileFixture();
  const imported = vi.fn();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json(preview))
    .mockImplementationOnce(async (_url, options: RequestInit) => {
      const stored = JSON.parse(
        sessionStorage.getItem("organizationweb.import.pending.v1")!,
      );
      const headers = new Headers(options.headers);
      expect(stored).toEqual({
        owner: "Ana",
        requestKey: headers.get("Idempotency-Key"),
        fileSha256: preview.fileSha256,
      });
      expect(options.body).toBe(file);
      return Response.json({
        requestKey: stored.requestKey,
        fileSha256: preview.fileSha256,
        byteLength: file.size,
        recordedAt: "2026-09-08T10:21:30.123456Z",
        outcome: "NO_CHANGE",
        insertedCounts: preview.insertCounts,
        identicalCounts: preview.identicalCounts,
      });
    });
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" onImported={imported} />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar importación" }),
  );
  expect(
    await screen.findByText(
      "Importación confirmada. No había registros nuevos que añadir.",
    ),
  ).toBeVisible();
  expect(
    sessionStorage.getItem("organizationweb.import.pending.v1"),
  ).toBeNull();
  expect(imported).toHaveBeenCalledOnce();
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(fetcher.mock.calls[1][0]).toBe("/api/v1/me/import");
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [new File(["{}"], "next.json")] },
  });
  expect(
    screen.queryByText(
      "Importación confirmada. No había registros nuevos que añadir.",
    ),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s35 changing the File retires its previous confirmation without another request", async () => {
  const { file, preview } = await fileFixture();
  const fetcher = vi.fn().mockResolvedValue(Response.json(preview));
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  await screen.findByRole("button", { name: "Confirmar importación" });
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [new File(["{}"], "otra.json")] },
  });
  expect(
    screen.queryByRole("heading", { name: "Vista previa" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Confirmar importación" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledOnce();
});

it("@s34 displays the fourteen collection counts and source metadata without inventing work totals", async () => {
  const { file, preview } = await fileFixture();
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(preview)));
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  const table = await screen.findByRole("table", {
    name: "Registros del archivo",
  });
  expect(within(table).getAllByRole("row")).toHaveLength(15);
  expect(
    within(table).getByRole("row", { name: "Reservas originales 0 0 0" }),
  ).toBeVisible();
  expect(screen.getByText(`Propietario: Ana`)).toBeVisible();
  expect(screen.getByText(`Exportado: ${preview.exportedAt}`)).toBeVisible();
  expect(screen.getByText(`Tamaño: ${file.size} bytes`)).toBeVisible();
});

it("@s34 shows a validated preview and waits for the separate confirmation gesture", async () => {
  const { file, preview } = await fileFixture();
  const fetcher = vi.fn().mockResolvedValue(Response.json(preview));
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  expect(
    await screen.findByRole("heading", { name: "Vista previa" }),
  ).toBeVisible();
  expect(
    screen.getByRole("button", { name: "Confirmar importación" }),
  ).toBeEnabled();
  expect(
    screen.getByText(
      "No se sobrescriben datos. Un conflicto impide toda la importación.",
    ),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledOnce();
  expect(fetcher.mock.calls[0][0]).toBe("/api/v1/me/import/preview");
  expect(
    sessionStorage.getItem("organizationweb.import.pending.v1"),
  ).toBeNull();
});

it("@s33 selects a private JSON File without sending it automatically", () => {
  const fetcher = vi.fn();
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  expect(
    screen.getByRole("heading", { level: 1, name: "Importar mis datos" }),
  ).toBeVisible();
  const file = new File(['{"format":"organizationweb-export"}'], "copia.json", {
    type: "application/json",
  });
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  expect(screen.getByRole("button", { name: "Validar archivo" })).toBeEnabled();
  expect(fetcher).not.toHaveBeenCalled();
  expect(
    screen.queryByRole("button", { name: "Confirmar importación" }),
  ).not.toBeInTheDocument();
});
