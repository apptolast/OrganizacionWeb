// @ts-nocheck
import { useState } from "react";
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { ImportData } from "./import-data";
import { CustomizationControls } from "./customization";
import { useCustomizationSession } from "./customization-state";
import { CustomFieldsPanel } from "./custom-fields";
import { App } from "./App";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  sessionStorage.removeItem("organizationweb.import.pending.v1");
  window.history.replaceState(null, "", "/");
});

it.each([
  ["PROJECT", false, false, false],
  ["TASK", false, false, false],
  ["PROJECT", true, false, false],
  ["PROJECT", false, true, false],
  ["PROJECT", false, false, true],
] as const)(
  "@s41 refreshes cached %s fields (dirty=%s, schemaOnly=%s, pending=%s)",
  async (scope, dirty, schemaOnly, pendingRead) => {
    let finish!: () => void;
    let previousSignal: AbortSignal | undefined;
    const projectId = "00000000-0000-4000-8000-000000000010";
    const taskId =
      scope === "TASK" ? "00000000-0000-4000-8000-000000000011" : undefined;
    const intent = {
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: "a".repeat(64),
    };
    sessionStorage.setItem(
      "organizationweb.import.pending.v1",
      JSON.stringify(intent),
    );
    const counts = Object.fromEntries(
      "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues"
        .split(" ")
        .map((key) => [key, 0]),
    );
    let reads = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string, init?: RequestInit) => {
        if (
          url ===
          `/api/v1/projects/${projectId}${taskId ? `/tasks/${taskId}` : ""}/custom-fields`
        ) {
          reads++;
          const response = Response.json(
            {
              configured: !schemaOnly && reads > 1,
              values:
                schemaOnly && reads === 1
                  ? []
                  : [
                      {
                        fieldId: "00000000-0000-4000-8000-000000000003",
                        label: "Referencia",
                        type: "TEXT",
                        value: schemaOnly || reads === 1 ? null : "importada",
                      },
                    ],
              updatedAt:
                schemaOnly || reads === 1 ? null : "2026-09-08T10:30:00Z",
            },
            {
              headers: {
                ETag: `"custom-values:${scope}:${taskId ?? projectId}:schema:${schemaOnly && reads === 1 ? "unconfigured" : "00000000-0000-4000-8000-000000000004:0"}:values:${schemaOnly || reads === 1 ? "unconfigured" : "00000000-0000-4000-8000-000000000002:0"}"`,
              },
            },
          );
          if (reads === 1 && pendingRead) {
            previousSignal = init?.signal as AbortSignal;
            return new Promise<Response>((resolve) => {
              finish = () => resolve(response);
            });
          }
          return response;
        }
        if (url === `/api/v1/me/imports/by-key/${intent.requestKey}`)
          return Response.json({
            requestKey: intent.requestKey,
            fileSha256: intent.fileSha256,
            byteLength: 200,
            recordedAt: "2026-09-08T10:30:00.000001Z",
            outcome: "IMPORTED",
            insertedCounts: {
              ...counts,
              [schemaOnly
                ? "customization"
                : scope === "TASK"
                  ? "taskCustomFieldValues"
                  : "projectCustomFieldValues"]: 1,
            },
            identicalCounts: counts,
          });
        throw new Error(`Unexpected request ${url}`);
      }),
    );
    function Surface() {
      const session = useCustomizationSession();
      const [importing, setImporting] = useState(false);
      return (
        <>
          <button onClick={() => setImporting(!importing)}>
            {importing ? "Volver al detalle" : "Abrir importación"}
          </button>
          {importing && (
            <ImportData
              owner="Ana"
              onImported={(receipt) =>
                session.refreshAfterImport(receipt.insertedCounts)
              }
            />
          )}
          {(!importing || pendingRead) && (
            <CustomFieldsPanel
              projectId={projectId}
              taskId={taskId}
              session={session}
            />
          )}
        </>
      );
    }
    render(<Surface />);
    if (pendingRead)
      expect(screen.getByRole("status")).toHaveTextContent(
        "Consultando campos personales",
      );
    else if (schemaOnly)
      await screen.findByText("No hay campos personales activos");
    else await screen.findByRole("button", { name: "Guardar campos" });
    if (dirty)
      fireEvent.change(screen.getByRole("textbox", { name: "Referencia" }), {
        target: { value: "borrador ajeno" },
      });
    fireEvent.click(screen.getByRole("button", { name: "Abrir importación" }));
    fireEvent.click(
      screen.getByRole("button", { name: "Comprobar resultado" }),
    );
    await screen.findByRole("table", { name: "Resultado confirmado" });
    if (pendingRead) {
      await screen.findByRole("textbox", { name: "Referencia" });
      await act(async () => finish());
      expect(previousSignal?.aborted).toBe(true);
    } else expect(reads).toBe(1);
    fireEvent.click(screen.getByRole("button", { name: "Volver al detalle" }));
    await screen.findByRole("button", { name: "Guardar campos" });
    if (dirty) {
      expect(reads).toBe(1);
      expect(screen.getByRole("alert")).toHaveTextContent(
        "Recargar guardado reemplazará este borrador",
      );
      expect(screen.getByRole("textbox", { name: "Referencia" })).toHaveValue(
        "borrador ajeno",
      );
      expect(
        screen.getByRole("button", { name: "Guardar campos" }),
      ).toBeDisabled();
    } else {
      expect(reads).toBe(2);
      expect(screen.getByRole("textbox", { name: "Referencia" })).toHaveValue(
        schemaOnly ? "" : "importada",
      );
    }
  },
);

it.each(["clean", "dirty", "uncertain", "pending", "route"])(
  "@s41 refreshes configuration preserving draft and recovery (%s)",
  async (state) => {
    const dirty = state === "dirty";
    const retained = dirty || state === "uncertain";
    let finish!: () => void;
    let previousSignal: AbortSignal | undefined;
    const intent = {
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: "a".repeat(64),
    };
    sessionStorage.setItem(
      "organizationweb.import.pending.v1",
      JSON.stringify(intent),
    );
    const counts = Object.fromEntries(
      "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues"
        .split(" ")
        .map((key) => [key, 0]),
    );
    let reads = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string, init?: RequestInit) => {
        if (url === "/api/v1/projects")
          return Response.json({ items: [], nextCursor: null });
        if (init?.method === "PUT") return new Response(null, { status: 503 });
        if (url === "/api/v1/me/customization/PROJECT") {
          reads++;
          const response = Response.json(
            {
              configured: reads > 1,
              visibleFields: reads === 1 ? ["createdAt"] : ["updatedAt"],
              customFields: [],
              updatedAt: reads === 1 ? null : "2026-09-08T10:30:00Z",
            },
            {
              headers: {
                ETag:
                  reads === 1
                    ? '"customization:PROJECT:unconfigured"'
                    : '"customization:PROJECT:00000000-0000-4000-8000-000000000002:0"',
              },
            },
          );
          if (reads === 1 && state === "pending") {
            previousSignal = init?.signal as AbortSignal;
            return new Promise<Response>((resolve) => {
              finish = () => resolve(response);
            });
          }
          return response;
        }
        if (url === `/api/v1/me/imports/by-key/${intent.requestKey}`)
          return Response.json({
            requestKey: intent.requestKey,
            fileSha256: intent.fileSha256,
            byteLength: 200,
            recordedAt: "2026-09-08T10:30:00.000001Z",
            outcome: "IMPORTED",
            insertedCounts: { ...counts, customization: 1 },
            identicalCounts: counts,
          });
        throw new Error(`Unexpected request ${url}`);
      }),
    );
    function Surface() {
      const session = useCustomizationSession();
      const [importing, setImporting] = useState(false);
      return (
        <>
          <button onClick={() => setImporting(!importing)}>
            {importing ? "Volver a vista" : "Abrir importación"}
          </button>
          {importing ? (
            <ImportData
              owner="Ana"
              onImported={(receipt) =>
                session.refreshAfterImport(receipt.insertedCounts)
              }
            />
          ) : (
            <CustomizationControls scope="PROJECT" session={session} />
          )}
        </>
      );
    }
    if (state === "route") {
      window.history.replaceState(null, "", "/proyectos");
      render(<App username="Ana" />);
    } else render(<Surface />);
    if (state !== "pending") {
      fireEvent.click(
        await screen.findByRole("button", { name: "Personalizar vista" }),
      );
      expect(screen.getByRole("checkbox", { name: "Creado" })).toBeChecked();
    }
    if (dirty)
      fireEvent.click(screen.getByRole("checkbox", { name: "Creado" }));
    if (state === "uncertain") {
      fireEvent.click(screen.getByRole("button", { name: "Guardar vista" }));
      await screen.findByText(
        "No se pudo confirmar el guardado. Recarga lo guardado antes de continuar.",
      );
    }
    fireEvent.click(
      state === "route"
        ? screen.getByRole("link", { name: "Importación" })
        : screen.getByRole("button", { name: "Abrir importación" }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Comprobar resultado" }),
    );
    await screen.findByRole("table", { name: "Resultado confirmado" });
    expect(reads).toBe(1);
    fireEvent.click(
      state === "route"
        ? screen.getByRole("link", { name: "Proyectos" })
        : screen.getByRole("button", { name: "Volver a vista" }),
    );
    if (retained) {
      expect(await screen.findByRole("alert")).toHaveTextContent(
        state === "dirty"
          ? "La importación puede haber cambiado la vista. Conservamos tu borrador; recarga lo guardado antes de continuar."
          : "No se pudo confirmar el guardado. Recarga lo guardado antes de continuar.",
      );
      expect(reads).toBe(1);
      expect(
        screen.queryByRole("button", { name: "Guardar vista" }),
      ).not.toBeInTheDocument();
      fireEvent.click(
        screen.getByRole("button", { name: "Recargar guardado" }),
      );
    }
    if (retained) {
      await screen.findByRole("button", { name: "Personalizar vista" });
      if (dirty)
        expect(
          screen.getByRole("checkbox", { name: "Creado" }),
        ).not.toBeChecked();
      else
        expect(screen.getByRole("checkbox", { name: "Creado" })).toBeChecked();
      expect(
        screen.getByRole("checkbox", { name: "Actualizado" }),
      ).not.toBeChecked();
      expect(reads).toBe(2);
    } else {
      if (state === "pending") {
        await screen.findByRole("button", { name: "Personalizar vista" });
        await act(async () => finish());
        expect(previousSignal?.aborted).toBe(true);
      }
      fireEvent.click(
        await screen.findByRole("button", { name: "Personalizar vista" }),
      );
      expect(
        screen.getByRole("checkbox", { name: "Actualizado" }),
      ).toBeChecked();
      expect(reads).toBe(2);
    }
  },
);
