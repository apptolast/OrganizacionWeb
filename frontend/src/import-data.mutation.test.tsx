import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  within,
  waitFor,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { File as NodeFile } from "node:buffer";
import { webcrypto } from "node:crypto";
import { ImportData } from "./import-data";
import { previewImportData, readImportReceipt } from "./import-data-api";
import { App } from "./App";
import { AppearanceProvider } from "./appearance-state";

const names = [
  ["projects", "Proyectos"],
  ["tasks", "Tareas"],
  ["taskStatusHistory", "Historial de tareas"],
  ["availability", "Disponibilidad"],
  ["plannedBlocks", "Reservas originales"],
  ["blockProjections", "Estado de reservas"],
  ["blockChanges", "Cambios de reservas"],
  ["workSessions", "Sesiones de trabajo"],
  ["workSessionIntervals", "Intervalos de trabajo"],
  ["workSessionChanges", "Cambios de sesiones"],
  ["appearance", "Apariencia"],
  ["customization", "Personalización"],
  ["projectCustomFieldValues", "Campos de proyectos"],
  ["taskCustomFieldValues", "Campos de tareas"],
] as const;
const emptyCounts = () => Object.fromEntries(names.map(([key]) => [key, 0]));
const storageKey = "organizationweb.import.pending.v1";
const intent = {
  owner: "Ana",
  requestKey: "00000000-0000-4000-8000-000000000001",
  fileSha256: "a".repeat(64),
};
function receipt(byteLength = 100) {
  return {
    requestKey: intent.requestKey,
    fileSha256: intent.fileSha256,
    byteLength,
    recordedAt: "2026-09-08T12:00:00.123456Z",
    outcome: "NO_CHANGE",
    insertedCounts: emptyCounts(),
    identicalCounts: emptyCounts(),
  };
}
async function fixture() {
  vi.stubGlobal("crypto", webcrypto);
  const file = new NodeFile(
    [
      JSON.stringify({
        format: "organizationweb-export",
        schemaVersion: 1,
        owner: "Ana",
        exportedAt: "2026-09-08T10:00:00.123456Z",
        counts: emptyCounts(),
        data: Object.fromEntries(names.map(([key]) => [key, []])),
      }),
    ],
    "copy.json",
    { type: "application/json" },
  ) as unknown as File;
  const hash = Buffer.from(
    await webcrypto.subtle.digest("SHA-256", await file.arrayBuffer()),
  ).toString("hex");
  return {
    file,
    preview: {
      format: "organizationweb-import-preview",
      schemaVersion: 1,
      owner: "Ana",
      fileSha256: hash,
      byteLength: file.size,
      exportedAt: "2026-09-08T10:00:00.123456Z",
      counts: emptyCounts(),
      insertCounts: emptyCounts(),
      identicalCounts: emptyCounts(),
      runningSessions: [] as {
        sessionId: string;
        runningSince: string | null;
      }[],
    },
  };
}
afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  sessionStorage.removeItem(storageKey);
  window.history.replaceState(null, "", "/");
});

it("@s41 a no-change receipt does not reload the unaffected appearance", async () => {
  sessionStorage.setItem(storageKey, JSON.stringify(intent));
  window.history.replaceState(null, "", "/importacion");
  let appearanceReads = 0;
  const fetcher = vi.fn(async (url: string) => {
    if (url === "/api/v1/me/appearance") {
      appearanceReads++;
      return Response.json(
        {
          configured: false,
          theme: "SYSTEM",
          accentLight: "#244C3C",
          accentDark: "#B7E4C7",
          updatedAt: null,
        },
        { headers: { ETag: '"appearance:unconfigured"' } },
      );
    }
    if (url === `/api/v1/me/imports/by-key/${intent.requestKey}`)
      return Response.json(receipt());
    throw new Error("Unexpected request " + url);
  });
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <App username="Ana" />
    </AppearanceProvider>,
  );
  await waitFor(() => expect(appearanceReads).toBe(1));
  fireEvent.click(screen.getByRole("button", { name: "Comprobar resultado" }));
  await screen.findByRole("table", { name: "Resultado confirmado" });
  expect(appearanceReads).toBe(1);
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s18 rejects coherent negative counts rather than presenting a negative import", async () => {
  const { file, preview } = await fixture();
  preview.counts.projects = -1;
  preview.insertCounts.projects = -1;
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(preview)));
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it.each([0, 1.5, 33_554_433, "100"])(
  "@s22 refuses a recovered receipt with byteLength %s",
  async (byteLength) => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(Response.json({ ...receipt(), byteLength })),
    );
    await expect(
      readImportReceipt(intent, new AbortController().signal),
    ).rejects.toThrow();
  },
);

it.each([
  [503, "IMPORT_NOT_FOUND"],
  [404, "OTHER_RESOURCE_NOT_FOUND"],
] as const)(
  "@s37 lookup %s/%s does not authorize resending or choosing another file",
  async (status, code) => {
    sessionStorage.setItem(storageKey, JSON.stringify(intent));
    const fetcher = vi.fn().mockResolvedValue(
      Response.json(
        {
          status,
          code,
          type: `urn:organization:problem:${code.toLowerCase()}`,
        },
        { status },
      ),
    );
    vi.stubGlobal("fetch", fetcher);
    render(<ImportData owner="Ana" />);
    expect(screen.getByLabelText("Archivo JSON")).toBeDisabled();
    expect(
      screen.queryByRole("button", { name: "Reenviar la misma importación" }),
    ).not.toBeInTheDocument();
    const check = screen.getByRole("button", { name: "Comprobar resultado" });
    fireEvent.click(check);
    await waitFor(() => expect(check).toBeEnabled());
    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(
      screen.queryByRole("button", { name: "Reenviar la misma importación" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Elegir otra copia" }),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Archivo JSON")).toBeDisabled();
    expect(JSON.parse(sessionStorage.getItem(storageKey)!)).toEqual(intent);
  },
);

it("@s39 cannot start a replacement intention when storage refuses to retire a rejected one", async () => {
  const { file, preview } = await fixture();
  let writes = 0;
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string) => {
      if (url.endsWith("/preview")) return Response.json(preview);
      writes++;
      return Response.json(
        {
          status: 409,
          code: "IMPORT_CONFLICT",
          type: "urn:organization:problem:import_conflict",
        },
        { status: 409 },
      );
    }),
  );
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar importación" }),
  );
  const correction = await screen.findByRole("button", {
    name: "Elegir otra copia",
  });
  const pending = sessionStorage.getItem(storageKey);
  vi.spyOn(Storage.prototype, "removeItem").mockImplementation(() => {
    throw new DOMException("Storage unavailable");
  });
  fireEvent.click(correction);
  expect(
    screen.getByText(/No se puede acceder al almacenamiento de recuperación/),
  ).toHaveAttribute("role", "alert");
  expect(screen.getByLabelText("Archivo JSON")).toBeDisabled();
  expect(
    screen.getByRole("button", { name: "Validar archivo" }),
  ).toBeDisabled();
  expect(
    screen.queryByRole("button", { name: "Confirmar importación" }),
  ).not.toBeInTheDocument();
  expect(writes).toBe(1);
  expect(sessionStorage.getItem(storageKey)).toBe(pending);
});

it("@s35 cancelling preview A and starting B prevents A from releasing B's busy state", async () => {
  const { file, preview } = await fixture();
  const finish: ((response: Response) => void)[] = [];
  const signals: AbortSignal[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((_url: string, init?: RequestInit) => {
      signals.push(init?.signal as AbortSignal);
      return new Promise<Response>((resolve) => finish.push(resolve));
    }),
  );
  render(<ImportData owner="Ana" />);
  const choose = () =>
    fireEvent.change(screen.getByLabelText("Archivo JSON"), {
      target: { files: [file] },
    });
  choose();
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  await waitFor(() => expect(finish).toHaveLength(1));
  fireEvent.click(screen.getByRole("button", { name: "Cancelar preparación" }));
  expect(signals[0].aborted).toBe(true);
  choose();
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  await waitFor(() => expect(finish).toHaveLength(2));
  await act(async () => finish[0](Response.json(preview)));
  expect(screen.getByRole("status")).toHaveTextContent("Validando archivo");
  expect(
    screen.getByRole("button", { name: "Validar archivo" }),
  ).toBeDisabled();
  expect(
    screen.queryByRole("button", { name: "Confirmar importación" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(signals[1].aborted).toBe(false);
  await act(async () => finish[1](Response.json(preview)));
  await screen.findByRole("button", { name: "Confirmar importación" });
  expect(finish).toHaveLength(2);
});

it.each([
  { fileSha256: "prefix" + intent.fileSha256 },
  { fileSha256: [intent.fileSha256] },
])(
  "@s39 discards malformed stored digest %j without a receipt GET",
  ({ fileSha256 }) => {
    sessionStorage.setItem(
      storageKey,
      JSON.stringify({ ...intent, fileSha256 }),
    );
    const fetcher = vi.fn();
    vi.stubGlobal("fetch", fetcher);
    render(<ImportData owner="Ana" />);
    expect(sessionStorage.getItem(storageKey)).toBeNull();
    expect(
      screen.queryByRole("button", { name: "Comprobar resultado" }),
    ).not.toBeInTheDocument();
    expect(fetcher).not.toHaveBeenCalled();
  },
);

it("@s34 @s36 presents each collection with its own counts and replaces preview with an imported receipt", async () => {
  const { file, preview } = await fixture();
  // This consumer accepts the server's closed summary; business-record validation
  // belongs to the real backend, not a second fourteen-family parser in React.
  names.forEach(([key], index) => {
    preview.insertCounts[key] = index + 1;
    preview.identicalCounts[key] = 2 * (index + 1);
    preview.counts[key] = 3 * (index + 1);
  });
  preview.runningSessions = [
    { sessionId: "00000000-0000-4000-8000-000000000002", runningSince: null },
  ];
  const fetcher = vi.fn(async (url: string, init?: RequestInit) => {
    if (url.endsWith("/preview")) return Response.json(preview);
    return Response.json({
      ...receipt(file.size),
      requestKey: new Headers(init?.headers).get("Idempotency-Key"),
      fileSha256: preview.fileSha256,
      outcome: "IMPORTED",
      insertedCounts: preview.insertCounts,
      identicalCounts: preview.identicalCounts,
    });
  });
  vi.stubGlobal("fetch", fetcher);
  render(<ImportData owner="Ana" />);
  fireEvent.change(screen.getByLabelText("Archivo JSON"), {
    target: { files: [file] },
  });
  fireEvent.click(screen.getByRole("button", { name: "Validar archivo" }));
  const table = await screen.findByRole("table", {
    name: "Registros del archivo",
  });
  for (const [key, label] of names) {
    const row = within(table)
      .getByRole("rowheader", { name: label })
      .closest("tr")!;
    expect(
      within(row)
        .getAllByRole("cell")
        .map((cell) => cell.textContent),
    ).toEqual(
      [
        preview.counts[key],
        preview.insertCounts[key],
        preview.identicalCounts[key],
      ].map(String),
    );
  }
  expect(
    screen.getByText(/Inicio del cómputo: no disponible en la copia/),
  ).toBeVisible();
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmar importación" }),
  );
  const result = await screen.findByRole("table", {
    name: "Resultado confirmado",
  });
  expect(screen.getByRole("status")).toHaveTextContent(
    /^Importación confirmada\.$/,
  );
  expect(
    screen.queryByRole("table", { name: "Registros del archivo" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Comprobar resultado" }),
  ).not.toBeInTheDocument();
  for (const [key, label] of names) {
    const row = within(result)
      .getByRole("rowheader", { name: label })
      .closest("tr")!;
    expect(
      within(row)
        .getAllByRole("cell")
        .map((cell) => cell.textContent),
    ).toEqual(
      [preview.insertCounts[key], preview.identicalCounts[key]].map(String),
    );
  }
  expect(sessionStorage.getItem(storageKey)).toBeNull();
  expect(fetcher).toHaveBeenCalledTimes(2);
});
