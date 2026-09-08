import { useState } from "react";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { ImportData } from "./import-data";
import { CustomizationControls } from "./customization";
import { CustomFieldsPanel } from "./custom-fields";
import { useCustomizationSession } from "./customization-state";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  sessionStorage.removeItem("organizationweb.import.pending.v1");
});

it.each(["NONE", "PROJECT", "TASK"] as const)(
  "@s41 a %s import preserves unrelated cached scopes and values",
  async (changed) => {
    const projectId = "00000000-0000-4000-8000-000000000010";
    const taskId = "00000000-0000-4000-8000-000000000011";
    const intent = {
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: "a".repeat(64),
    };
    const counts = Object.fromEntries(
      "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues"
        .split(" ")
        .map((key) => [key, 0]),
    );
    if (changed !== "NONE")
      counts[
        changed === "PROJECT"
          ? "projectCustomFieldValues"
          : "taskCustomFieldValues"
      ] = 1;
    sessionStorage.setItem(
      "organizationweb.import.pending.v1",
      JSON.stringify(intent),
    );
    const reads: Record<string, number> = {};
    const urls = {
      PROJECT: `/api/v1/projects/${projectId}/custom-fields`,
      TASK: `/api/v1/projects/${projectId}/tasks/${taskId}/custom-fields`,
    };
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string) => {
        reads[url] = (reads[url] ?? 0) + 1;
        if (url === `/api/v1/me/imports/by-key/${intent.requestKey}`)
          return Response.json({
            requestKey: intent.requestKey,
            fileSha256: intent.fileSha256,
            byteLength: 100,
            recordedAt: "2026-09-08T12:00:00.123456Z",
            outcome: changed === "NONE" ? "NO_CHANGE" : "IMPORTED",
            insertedCounts: counts,
            identicalCounts: {
              ...counts,
              projectCustomFieldValues: 0,
              taskCustomFieldValues: 0,
            },
          });
        for (const scope of ["PROJECT", "TASK"] as const) {
          const schemaId =
            scope === "PROJECT"
              ? "00000000-0000-4000-8000-000000000020"
              : "00000000-0000-4000-8000-000000000021";
          const fieldId =
            scope === "PROJECT"
              ? "00000000-0000-4000-8000-000000000030"
              : "00000000-0000-4000-8000-000000000031";
          if (url === `/api/v1/me/customization/${scope}`)
            return Response.json(
              {
                configured: true,
                visibleFields:
                  scope === "PROJECT"
                    ? ["createdAt"]
                    : ["completionCriterion", "estimatedMinutes"],
                customFields: [
                  {
                    id: fieldId,
                    label: "Referencia " + scope,
                    type: "TEXT",
                    active: true,
                  },
                ],
                updatedAt: "2026-09-08T10:00:00Z",
              },
              { headers: { ETag: `"customization:${scope}:${schemaId}:0"` } },
            );
          if (url === urls[scope])
            return Response.json(
              {
                configured: true,
                values: [
                  {
                    fieldId,
                    label: "Referencia " + scope,
                    type: "TEXT",
                    value:
                      reads[url] === 1
                        ? "original " + scope
                        : "importado " + scope,
                  },
                ],
                updatedAt: "2026-09-08T10:00:00Z",
              },
              {
                headers: {
                  ETag: `"custom-values:${scope}:${scope === "PROJECT" ? projectId : taskId}:schema:${schemaId}:0:values:00000000-0000-4000-8000-000000000040:0"`,
                },
              },
            );
        }
        throw new Error("Unexpected request " + url);
      }),
    );
    function Surface() {
      const session = useCustomizationSession();
      const [importing, setImporting] = useState(false);
      return (
        <>
          <button onClick={() => setImporting(!importing)}>
            {importing ? "Volver a detalles" : "Abrir importación"}
          </button>
          {importing ? (
            <ImportData
              owner="Ana"
              onImported={(result) =>
                session.refreshAfterImport(result.insertedCounts)
              }
            />
          ) : (
            <>
              {(["PROJECT", "TASK"] as const).map((scope) => (
                <section aria-label={scope} key={scope}>
                  <CustomizationControls scope={scope} session={session} />
                  <CustomFieldsPanel
                    projectId={projectId}
                    taskId={scope === "TASK" ? taskId : undefined}
                    session={session}
                  />
                </section>
              ))}
            </>
          )}
        </>
      );
    }
    render(<Surface />);
    for (const scope of ["PROJECT", "TASK"] as const) {
      await screen.findByRole("textbox", { name: "Referencia " + scope });
      await waitFor(() =>
        expect(
          within(screen.getByRole("region", { name: scope })).getByRole(
            "button",
            { name: "Personalizar vista" },
          ),
        ).toBeEnabled(),
      );
    }
    fireEvent.click(screen.getByRole("button", { name: "Abrir importación" }));
    fireEvent.click(
      screen.getByRole("button", { name: "Comprobar resultado" }),
    );
    await screen.findByRole("table", { name: "Resultado confirmado" });
    fireEvent.click(screen.getByRole("button", { name: "Volver a detalles" }));
    for (const scope of ["PROJECT", "TASK"] as const) {
      await waitFor(() =>
        expect(
          screen.getByRole("textbox", { name: "Referencia " + scope }),
        ).toHaveValue((changed === scope ? "importado " : "original ") + scope),
      );
      expect(reads[urls[scope]]).toBe(changed === scope ? 2 : 1);
      expect(reads[`/api/v1/me/customization/${scope}`]).toBe(1);
    }
  },
);
