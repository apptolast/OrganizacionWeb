// @ts-nocheck
import { act, fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { observeAccess } from "./api-client";
import { CustomFieldsPanel } from "./custom-fields";
import { useCustomizationSession } from "./customization-state";

const projectId = "12345678-1234-1234-1234-123456789abc";
const schemaId = "32345678-1234-1234-1234-123456789abc";
const fieldId = "22345678-1234-1234-1234-123456789abc";
type Entry = {
  fieldId: string;
  label: string;
  type: "TEXT" | "NUMBER" | "BOOLEAN" | "DATE";
  value: string | number | boolean | null;
};
function response(values: Entry[], revision = 0) {
  return Response.json(
    { configured: true, values, updatedAt: "2026-09-07T10:00:00Z" },
    {
      headers: {
        ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:${revision}"`,
      },
    },
  );
}
function Surface({
  onAccessFailure,
}: {
  onAccessFailure?: (status: number) => void;
}) {
  const session = useCustomizationSession();
  return (
    <>
      <button>Otro control</button>
      <CustomFieldsPanel
        projectId={projectId}
        session={session}
        onAccessFailure={onAccessFailure}
      />
    </>
  );
}
afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
});

it("@s32 accepts exactly1000 Unicode code points and confirms the exact text", async () => {
  const text = "🧭".repeat(1000);
  const entry: Entry = { fieldId, label: "Nota", type: "TEXT", value: null };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response([entry]))
    .mockResolvedValueOnce(response([{ ...entry, value: text }], 1));
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const input = await screen.findByRole("textbox", { name: "Nota" });
  fireEvent.change(input, { target: { value: text } });
  await userEvent.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByText("Campos guardados")).toBeInTheDocument();
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    values: [{ fieldId, value: text }],
  });
  expect(input).toHaveValue(text);
});

it("@s32 accepts positive boundary without a sign", async () => {
  const entry: Entry = {
    fieldId,
    label: "Puntos",
    type: "NUMBER",
    value: null,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response([entry]))
    .mockResolvedValueOnce(response([{ ...entry, value: 1000000000 }], 1));
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  await userEvent.type(
    await screen.findByRole("textbox", { name: "Puntos" }),
    "1000000000",
  );
  await userEvent.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByText("Campos guardados")).toBeInTheDocument();
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    values: [{ fieldId, value: 1000000000 }],
  });
});

it("@s32 accepts negative boundary", async () => {
  const entry: Entry = {
    fieldId,
    label: "Puntos",
    type: "NUMBER",
    value: null,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response([entry]))
    .mockResolvedValueOnce(response([{ ...entry, value: -1000000000 }], 1));
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  await userEvent.type(
    await screen.findByRole("textbox", { name: "Puntos" }),
    "-1000000000",
  );
  await userEvent.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByText("Campos guardados")).toBeInTheDocument();
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    values: [{ fieldId, value: -1000000000 }],
  });
});

it("@s32 saves an edited false instead of coercing it to true", async () => {
  const entry: Entry = {
    fieldId,
    label: "Revisado",
    type: "BOOLEAN",
    value: true,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response([entry]))
    .mockResolvedValueOnce(response([{ ...entry, value: false }], 1));
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  await userEvent.selectOptions(
    await screen.findByRole("combobox", { name: "Revisado" }),
    "false",
  );
  await userEvent.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByText("Campos guardados")).toBeInTheDocument();
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    values: [{ fieldId, value: false }],
  });
});

it("@s35 keeps a503 write uncertain without withdrawing the resource", async () => {
  const callback = vi.fn();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      response([{ fieldId, label: "Nota", type: "TEXT", value: "Anterior" }]),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }));
  vi.stubGlobal("fetch", fetcher);
  render(<Surface onAccessFailure={callback} />);
  fireEvent.change(await screen.findByRole("textbox", { name: "Nota" }), {
    target: { value: "Borrador" },
  });
  await userEvent.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(
    await screen.findByRole("button", { name: "Recargar guardado" }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("heading", { name: "Campos personales" }),
  ).toBeInTheDocument();
  expect(callback).not.toHaveBeenCalled();
  expect(screen.queryByText("Campos guardados")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s37 delivers a current401 read to the latest callback", async () => {
  let finish!: (response: Response) => void;
  const oldCallback = vi.fn(),
    currentCallback = vi.fn();
  const fetcher = vi.fn().mockImplementation(
    () =>
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
  );
  vi.stubGlobal("fetch", fetcher);
  const view = render(<Surface onAccessFailure={oldCallback} />);
  view.rerender(<Surface onAccessFailure={currentCallback} />);
  await act(async () => finish(new Response(null, { status: 401 })));
  expect(currentCallback).toHaveBeenCalledExactlyOnceWith(401);
  expect(oldCallback).not.toHaveBeenCalled();
  expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s37 delivers a current404 recovery to the latest callback", async () => {
  let finish!: (response: Response) => void;
  const oldCallback = vi.fn(),
    currentCallback = vi.fn();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      response([{ fieldId, label: "Nota", type: "TEXT", value: null }]),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    );
  vi.stubGlobal("fetch", fetcher);
  const view = render(<Surface onAccessFailure={oldCallback} />);
  await screen.findByRole("textbox", { name: "Nota" });
  await userEvent.click(screen.getByRole("button", { name: "Guardar campos" }));
  await userEvent.click(
    await screen.findByRole("button", { name: "Recargar guardado" }),
  );
  view.rerender(<Surface onAccessFailure={currentCallback} />);
  await act(async () => finish(new Response(null, { status: 404 })));
  expect(currentCallback).toHaveBeenCalledExactlyOnceWith(404);
  expect(oldCallback).not.toHaveBeenCalled();
  expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s33 does not withdraw on503 initial read", async () => {
  const callback = vi.fn();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValueOnce(new Response(null, { status: 503 })),
  );
  render(<Surface onAccessFailure={callback} />);
  expect(
    await screen.findByRole("button", { name: "Reintentar" }),
  ).toBeInTheDocument();
  expect(callback).not.toHaveBeenCalled();
  expect(
    screen.queryByText("No hay campos personales activos"),
  ).not.toBeInTheDocument();
});

it("@s36 does not withdraw on503 manual recovery", async () => {
  const callback = vi.fn();
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      response([{ fieldId, label: "Nota", type: "TEXT", value: null }]),
    )
    .mockResolvedValue(new Response(null, { status: 503 }));
  vi.stubGlobal("fetch", fetcher);
  render(<Surface onAccessFailure={callback} />);
  await screen.findByRole("textbox", { name: "Nota" });
  await userEvent.click(screen.getByRole("button", { name: "Guardar campos" }));
  await userEvent.click(
    await screen.findByRole("button", { name: "Recargar guardado" }),
  );
  expect(
    await screen.findByRole("button", { name: "Recargar guardado" }),
  ).toBeEnabled();
  expect(callback).not.toHaveBeenCalled();
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s39 respects voluntary focus during values recovery", async () => {
  const values: Entry[] = [
    { fieldId, label: "Nota", type: "TEXT", value: "Durable" },
  ];
  let finish!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response(values))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  await screen.findByRole("textbox", { name: "Nota" });
  await userEvent.click(screen.getByRole("button", { name: "Guardar campos" }));
  await userEvent.click(
    await screen.findByRole("button", { name: "Recargar guardado" }),
  );
  const other = screen.getByRole("button", { name: "Otro control" });
  await userEvent.click(other);
  other.blur();
  expect(document.body).toHaveFocus();
  await act(async () => finish(response(values)));
  expect(await screen.findByRole("textbox", { name: "Nota" })).toHaveValue(
    "Durable",
  );
  expect(document.body).toHaveFocus();
  expect(
    screen.getByRole("heading", { name: "Campos personales" }),
  ).not.toHaveFocus();
});

it("@s34 associates index10 with the eleventh of twelve fields", async () => {
  const values: Entry[] = Array.from({ length: 12 }, (_, index) => ({
    fieldId: `${String(index + 1).padStart(8, "0")}-1234-1234-1234-123456789abc`,
    label: `Nota ${index + 1}`,
    type: "TEXT",
    value: `Previo ${index + 1}`,
  }));
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response(values))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:validation_error",
          title: "Campos invalidos",
          status: 400,
          code: "VALIDATION_ERROR",
          errors: [
            {
              field: "values[10].value",
              code: "INVALID_VALUE",
              message: "Revisa el undécimo campo.",
            },
          ],
        },
        { status: 400 },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  await screen.findByRole("textbox", { name: "Nota 1" });
  for (const [index, entry] of values.entries())
    fireEvent.change(screen.getByRole("textbox", { name: entry.label }), {
      target: { value: `Borrador ${index + 1}` },
    });
  await userEvent.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Revisa el undécimo campo.",
  );
  for (const [index, entry] of values.entries()) {
    const control = screen.getByRole("textbox", {
      name: entry.label,
    });
    expect(control).toHaveValue(`Borrador ${index + 1}`);
    expect(control).toHaveAttribute(
      "aria-invalid",
      index === 10 ? "true" : "false",
    );
  }
  expect(
    screen.getByRole("textbox", { name: "Nota 11" }),
  ).toHaveAccessibleDescription("Revisa el undécimo campo.");
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    values: values.map((entry, index) => ({
      fieldId: entry.fieldId,
      value: `Borrador ${index + 1}`,
    })),
  });
});
