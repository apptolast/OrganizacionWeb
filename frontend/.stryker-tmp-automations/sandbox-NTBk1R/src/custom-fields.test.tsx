// @ts-nocheck
import { act, fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { observeAccess } from "./api-client";
import { CustomFieldsPanel } from "./custom-fields";
import { useCustomizationSession } from "./customization-state";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
});
const projectId = "12345678-1234-1234-1234-123456789abc";
const fieldId = "22345678-1234-1234-1234-123456789abc";
const schemaId = "32345678-1234-1234-1234-123456789abc";
function Surface() {
  const session = useCustomizationSession();
  return <CustomFieldsPanel projectId={projectId} session={session} />;
}

it("@s32 reads typed project values lazily and keeps false distinct from no value", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      {
        configured: true,
        values: [{ fieldId, label: "Revisado", type: "BOOLEAN", value: false }],
        updatedAt: "2026-09-07T10:00:00Z",
      },
      {
        headers: {
          ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
        },
      },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  expect(await screen.findByRole("combobox", { name: "Revisado" })).toHaveValue(
    "false",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(fetcher.mock.calls[0][0]).toBe(
    `/api/v1/projects/${projectId}/custom-fields`,
  );
});

it("@s32 explicitly saves a boolean choice as a typed value", async () => {
  const initial = {
    configured: true,
    values: [{ fieldId, label: "Revisado", type: "BOOLEAN", value: false }],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const tag = `custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:`;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, { headers: { ETag: `"${tag}0"` } }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...initial, values: [{ ...initial.values[0], value: true }] },
        { headers: { ETag: `"${tag}1"` } },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.selectOptions(
    await screen.findByRole("combobox", { name: "Revisado" }),
    "true",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByRole("status")).toHaveTextContent(
    "Campos guardados",
  );
  await user.selectOptions(
    screen.getByRole("combobox", { name: "Revisado" }),
    "false",
  );
  expect(screen.queryByText("Campos guardados")).not.toBeInTheDocument();
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    values: [{ fieldId, value: true }],
  });
});

it("@s32 shows zero and saves a numeric entry as an integer", async () => {
  const initial = {
    configured: true,
    values: [{ fieldId, label: "Puntos", type: "NUMBER", value: 0 }],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const tag = `custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:`;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, { headers: { ETag: `"${tag}0"` } }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...initial, values: [{ ...initial.values[0], value: -12 }] },
        { headers: { ETag: `"${tag}1"` } },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  const number = await screen.findByRole("textbox", { name: "Puntos" });
  expect(number).toHaveValue("0");
  await user.clear(number);
  await user.type(number, "-12");
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByRole("status")).toHaveTextContent(
    "Campos guardados",
  );
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    values: [{ fieldId, value: -12 }],
  });
});

it("@s32 rejects a fractional numeric intention before rounding or sending it", async () => {
  const fetcher = vi.fn().mockImplementation(() =>
    Response.json(
      {
        configured: true,
        values: [{ fieldId, label: "Puntos", type: "NUMBER", value: 0 }],
        updatedAt: "2026-09-07T10:00:00Z",
      },
      {
        headers: {
          ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
        },
      },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  const number = await screen.findByRole("textbox", { name: "Puntos" });
  await user.clear(number);
  await user.type(number, "1.0000000000000000000001");
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Escribe un número entero entre -1000000000 y 1000000000",
  );
  expect(number).toHaveValue("1.0000000000000000000001");
  expect(number).toHaveAttribute("aria-invalid", "true");
  expect(number).toHaveAccessibleDescription(
    "Escribe un número entero entre -1000000000 y 1000000000.",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s32 rejects an integer outside the allowed range before constructing the request", async () => {
  const fetcher = vi.fn().mockImplementation(() =>
    Response.json(
      {
        configured: true,
        values: [{ fieldId, label: "Puntos", type: "NUMBER", value: 0 }],
        updatedAt: "2026-09-07T10:00:00Z",
      },
      {
        headers: {
          ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
        },
      },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  const number = await screen.findByRole("textbox", { name: "Puntos" });
  await user.clear(number);
  await user.type(number, "1000000001");
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Escribe un número entero entre -1000000000 y 1000000000",
  );
  expect(number).toHaveValue("1000000001");
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s32 preserves whitespace and line breaks in a text field", async () => {
  const initial = {
    configured: true,
    values: [{ fieldId, label: "Nota personal", type: "TEXT", value: null }],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const tag = `custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:`;
  const note = "  Primera línea\nSegunda línea  ";
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, { headers: { ETag: `"${tag}0"` } }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...initial, values: [{ ...initial.values[0], value: note }] },
        { headers: { ETag: `"${tag}1"` } },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  const input = await screen.findByRole("textbox", { name: "Nota personal" });
  expect(input.tagName).toBe("TEXTAREA");
  expect(input).not.toHaveAttribute("maxlength");
  await user.type(input, note);
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByRole("status")).toHaveTextContent(
    "Campos guardados",
  );
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    values: [{ fieldId, value: note }],
  });
});

it("@s32 edits a civil date with a native control without timezone conversion", async () => {
  const initial = {
    configured: true,
    values: [
      { fieldId, label: "Fecha personal", type: "DATE", value: "2026-09-07" },
    ],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const tag = `custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:`;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, { headers: { ETag: `"${tag}0"` } }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...initial, values: [{ ...initial.values[0], value: "2024-02-29" }] },
        { headers: { ETag: `"${tag}1"` } },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const date = await screen.findByLabelText("Fecha personal");
  expect(date).toHaveAttribute("type", "date");
  fireEvent.change(date, { target: { value: "10000-01-01" } });
  expect(date).toBeInvalid();
  await userEvent.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(fetcher).toHaveBeenCalledTimes(1);
  fireEvent.change(date, { target: { value: "2024-02-29" } });
  await userEvent.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByRole("status")).toHaveTextContent(
    "Campos guardados",
  );
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    values: [{ fieldId, value: "2024-02-29" }],
  });
});

it("@s35 keeps a value write uncertain after navigating away and requires a valid manual read", async () => {
  const initial = {
    configured: true,
    values: [{ fieldId, label: "Revisado", type: "BOOLEAN", value: false }],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const response = () =>
    Response.json(initial, {
      headers: {
        ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
      },
    });
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(response());
  vi.stubGlobal("fetch", fetcher);
  function Navigable({ visible }: { visible: boolean }) {
    const session = useCustomizationSession();
    return visible ? (
      <CustomFieldsPanel projectId={projectId} session={session} />
    ) : (
      <h1>Otra vista</h1>
    );
  }
  const view = render(<Navigable visible />);
  const user = userEvent.setup();
  await user.selectOptions(
    await screen.findByRole("combobox", { name: "Revisado" }),
    "true",
  );
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pudo confirmar el guardado",
  );
  view.rerender(<Navigable visible={false} />);
  view.rerender(<Navigable visible />);
  expect(
    screen.queryByRole("button", { name: "Guardar campos" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
  await user.click(screen.getByRole("button", { name: "Recargar guardado" }));
  expect(await screen.findByRole("combobox", { name: "Revisado" })).toHaveValue(
    "false",
  );
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s33 explains a failed values read without presenting empty fields as confirmed", async () => {
  const response = () =>
    Response.json(
      { configured: false, values: [], updatedAt: null },
      {
        headers: {
          ETag: `"custom-values:PROJECT:${projectId}:schema:unconfigured:values:unconfigured"`,
        },
      },
    );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(response());
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pudieron consultar los campos personales",
  );
  expect(
    screen.queryByRole("button", { name: "Guardar campos" }),
  ).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(
    await screen.findByText("No hay campos personales activos"),
  ).toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s34 maps an indexed validation error to the sent field and preserves every draft value", async () => {
  const booleanId = "52345678-1234-1234-1234-123456789abc";
  const initial = {
    configured: true,
    values: [
      { fieldId, label: "Nota", type: "TEXT", value: null },
      { fieldId: booleanId, label: "Revisado", type: "BOOLEAN", value: false },
    ],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, {
        headers: {
          ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
        },
      }),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:validation_error",
          title: "Campos invalidos",
          status: 400,
          code: "VALIDATION_ERROR",
          errors: [
            {
              field: "values[1].value",
              code: "INVALID_VALUE",
              message: "Revisa esta elección.",
            },
          ],
        },
        { status: 400 },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  const note = await screen.findByRole("textbox", { name: "Nota" });
  const choice = screen.getByRole("combobox", { name: "Revisado" });
  await user.type(note, "Conservar borrador");
  await user.selectOptions(choice, "true");
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Revisa esta elección.",
  );
  expect(choice).toHaveAttribute("aria-invalid", "true");
  expect(choice).toHaveAccessibleDescription("Revisa esta elección.");
  expect(note).toHaveAttribute("aria-invalid", "false");
  expect(note).toHaveValue("Conservar borrador");
  expect(choice).toHaveValue("true");
  expect(screen.getByRole("button", { name: "Guardar campos" })).toBeEnabled();
});

it("retains a personal value draft across internal navigation within the session", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      {
        configured: true,
        values: [{ fieldId, label: "Nota", type: "TEXT", value: "Guardada" }],
        updatedAt: "2026-09-07T10:00:00Z",
      },
      {
        headers: {
          ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
        },
      },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  function Navigable({ visible }: { visible: boolean }) {
    const session = useCustomizationSession();
    return visible ? (
      <CustomFieldsPanel projectId={projectId} session={session} />
    ) : (
      <h1>Otra vista</h1>
    );
  }
  const view = render(<Navigable visible />);
  const user = userEvent.setup();
  const input = await screen.findByRole("textbox", { name: "Nota" });
  await user.clear(input);
  await user.type(input, "Borrador propio");
  view.rerender(<Navigable visible={false} />);
  view.rerender(<Navigable visible />);
  expect(screen.getByRole("textbox", { name: "Nota" })).toHaveValue(
    "Borrador propio",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s32 clears only the chosen field and saves explicit null with the remaining false value", async () => {
  const booleanId = "52345678-1234-1234-1234-123456789abc";
  const initial = {
    configured: true,
    values: [
      { fieldId, label: "Puntos", type: "NUMBER", value: 0 },
      { fieldId: booleanId, label: "Revisado", type: "BOOLEAN", value: false },
    ],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const tag = `custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:`;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, { headers: { ETag: `"${tag}0"` } }),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          ...initial,
          values: [{ ...initial.values[0], value: null }, initial.values[1]],
        },
        { headers: { ETag: `"${tag}1"` } },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Vaciar Puntos" }),
  );
  expect(screen.getByRole("textbox", { name: "Puntos" })).toHaveValue("");
  expect(screen.getByRole("combobox", { name: "Revisado" })).toHaveValue(
    "false",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByRole("status")).toHaveTextContent(
    "Campos guardados",
  );
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    values: [
      { fieldId, value: null },
      { fieldId: booleanId, value: false },
    ],
  });
});

it("@s32 cancels changed values and restores the confirmed snapshot without writing", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      {
        configured: true,
        values: [{ fieldId, label: "Nota", type: "TEXT", value: "Guardada" }],
        updatedAt: "2026-09-07T10:00:00Z",
      },
      {
        headers: {
          ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
        },
      },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  const input = await screen.findByRole("textbox", { name: "Nota" });
  await user.clear(input);
  await user.type(input, "Sin guardar");
  await user.click(screen.getByRole("button", { name: "Cancelar" }));
  expect(input).toHaveValue("Guardada");
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s37 retires a previous project read before its late 401 observes the current access", async () => {
  const nextProject = "62345678-1234-1234-1234-123456789abc";
  let finish!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { configured: false, values: [], updatedAt: null },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${nextProject}:schema:unconfigured:values:unconfigured"`,
          },
        },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  const observer = vi.fn();
  observeAccess(observer);
  function Context({ id }: { id: string }) {
    const session = useCustomizationSession();
    return <CustomFieldsPanel projectId={id} session={session} />;
  }
  const view = render(<Context id={projectId} />);
  view.rerender(<Context id={nextProject} />);
  expect(
    await screen.findByText("No hay campos personales activos"),
  ).toBeInTheDocument();
  await act(async () => finish(new Response(null, { status: 401 })));
  expect(observer).not.toHaveBeenCalled();
  expect(
    screen.getByText("No hay campos personales activos"),
  ).toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s37 can consult a resource again after leaving before its initial read completed", async () => {
  const fetcher = vi
    .fn()
    .mockImplementationOnce(() => new Promise<Response>(() => {}))
    .mockResolvedValueOnce(
      Response.json(
        { configured: false, values: [], updatedAt: null },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:unconfigured:values:unconfigured"`,
          },
        },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  function Navigable({ visible }: { visible: boolean }) {
    const session = useCustomizationSession();
    return visible ? (
      <CustomFieldsPanel projectId={projectId} session={session} />
    ) : (
      <h1>Otra vista</h1>
    );
  }
  const view = render(<Navigable visible />);
  view.rerender(<Navigable visible={false} />);
  view.rerender(<Navigable visible />);
  expect(
    await screen.findByText("No hay campos personales activos"),
  ).toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s37 aborts a personal values write when its authenticated session is removed", async () => {
  let finish!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        {
          configured: true,
          values: [
            { fieldId, label: "Revisado", type: "BOOLEAN", value: false },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
          },
        },
      ),
    )
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    );
  vi.stubGlobal("fetch", fetcher);
  const observer = vi.fn();
  observeAccess(observer);
  const view = render(<Surface />);
  const user = userEvent.setup();
  await user.selectOptions(
    await screen.findByRole("combobox", { name: "Revisado" }),
    "true",
  );
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(fetcher).toHaveBeenCalledTimes(2);
  view.unmount();
  await act(async () => finish(new Response(null, { status: 401 })));
  expect(observer).not.toHaveBeenCalled();
});

it("@s39 announces a pending values write and prevents duplicate submissions and edits", async () => {
  const initial = {
    configured: true,
    values: [{ fieldId, label: "Revisado", type: "BOOLEAN", value: false }],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const tag = `custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0`;
  let finish!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, { headers: { ETag: `"${tag}"` } }),
    )
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await screen.findByRole("combobox", { name: "Revisado" });
  const save = screen.getByRole("button", { name: "Guardar campos" });
  await user.dblClick(save);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Guardando campos personales",
  );
  expect(screen.getByRole("combobox", { name: "Revisado" })).toBeDisabled();
  expect(save).toBeDisabled();
  expect(fetcher).toHaveBeenCalledTimes(2);
  await act(async () =>
    finish(Response.json(initial, { headers: { ETag: `"${tag}"` } })),
  );
  expect(await screen.findByText("Campos guardados")).toBeInTheDocument();
});

it("@s38 discards a retained values read after a later confirmed write", async () => {
  const initial = {
    configured: true,
    values: [{ fieldId, label: "Revisado", type: "BOOLEAN", value: false }],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const tag = `custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:`;
  let finish!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, { headers: { ETag: `"${tag}0"` } }),
    )
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...initial, values: [{ ...initial.values[0], value: true }] },
        { headers: { ETag: `"${tag}1"` } },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  function Consumers() {
    const session = useCustomizationSession();
    return (
      <>
        <CustomFieldsPanel projectId={projectId} session={session} />
        <button onClick={() => void session.reloadValues(projectId)}>
          Consultar desde otro consumidor
        </button>
      </>
    );
  }
  render(<Consumers />);
  const user = userEvent.setup();
  await screen.findByRole("combobox", { name: "Revisado" });
  await user.click(
    screen.getByRole("button", { name: "Consultar desde otro consumidor" }),
  );
  await user.selectOptions(
    screen.getByRole("combobox", { name: "Revisado" }),
    "true",
  );
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByText("Campos guardados")).toBeInTheDocument();
  await act(async () =>
    finish(Response.json(initial, { headers: { ETag: `"${tag}0"` } })),
  );
  expect(screen.getByRole("combobox", { name: "Revisado" })).toHaveValue(
    "true",
  );
});

it("@s35 does not automatically recover an uncertain write after it retired another read", async () => {
  const response = () =>
    Response.json(
      {
        configured: true,
        values: [{ fieldId, label: "Revisado", type: "BOOLEAN", value: false }],
        updatedAt: "2026-09-07T10:00:00Z",
      },
      {
        headers: {
          ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
        },
      },
    );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockImplementationOnce(() => new Promise<Response>(() => {}))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockImplementation(response);
  vi.stubGlobal("fetch", fetcher);
  function Navigable({ visible }: { visible: boolean }) {
    const session = useCustomizationSession();
    return visible ? (
      <>
        <CustomFieldsPanel projectId={projectId} session={session} />
        <button onClick={() => void session.reloadValues(projectId)}>
          Consultar desde otro consumidor
        </button>
      </>
    ) : (
      <h1>Otra vista</h1>
    );
  }
  const view = render(<Navigable visible />);
  const user = userEvent.setup();
  await screen.findByRole("combobox", { name: "Revisado" });
  await user.click(
    screen.getByRole("button", { name: "Consultar desde otro consumidor" }),
  );
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  await screen.findByRole("button", { name: "Recargar guardado" });
  view.rerender(<Navigable visible={false} />);
  view.rerender(<Navigable visible />);
  expect(
    screen.getByRole("button", { name: "Recargar guardado" }),
  ).toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s36 announces a pending manual recovery without issuing a second GET", async () => {
  const response = () =>
    Response.json(
      {
        configured: true,
        values: [{ fieldId, label: "Revisado", type: "BOOLEAN", value: false }],
        updatedAt: "2026-09-07T10:00:00Z",
      },
      {
        headers: {
          ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
        },
      },
    );
  let finish!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await screen.findByRole("combobox", { name: "Revisado" });
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  const retry = await screen.findByRole("button", {
    name: "Recargar guardado",
  });
  await user.dblClick(retry);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando campos personales",
  );
  expect(retry).toBeDisabled();
  fireEvent.focusOut(retry, { relatedTarget: null });
  expect(fetcher).toHaveBeenCalledTimes(3);
  await act(async () => finish(response()));
  expect(
    screen.getByRole("heading", { name: "Campos personales" }),
  ).toHaveFocus();
  expect(await screen.findByRole("combobox", { name: "Revisado" })).toHaveValue(
    "false",
  );
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

it("@s37 withdraws the resource on a current 404 without allowing an older read to restore it", async () => {
  const initial = {
    configured: true,
    values: [
      { fieldId, label: "Nota privada", type: "TEXT", value: "Dato privado" },
    ],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const response = () =>
    Response.json(initial, {
      headers: {
        ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
      },
    });
  let finish!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    )
    .mockResolvedValueOnce(new Response(null, { status: 404 }));
  vi.stubGlobal("fetch", fetcher);
  const withdrawn = vi.fn();
  function Consumers() {
    const session = useCustomizationSession();
    return (
      <>
        <CustomFieldsPanel
          projectId={projectId}
          session={session}
          onAccessFailure={withdrawn}
        />
        <button onClick={() => void session.reloadValues(projectId)}>
          Consultar desde otro consumidor
        </button>
      </>
    );
  }
  render(<Consumers />);
  const user = userEvent.setup();
  await screen.findByRole("textbox", { name: "Nota privada" });
  await user.click(
    screen.getByRole("button", { name: "Consultar desde otro consumidor" }),
  );
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(withdrawn).toHaveBeenCalledWith(404);
  await act(async () => finish(response()));
  expect(
    screen.queryByRole("textbox", { name: "Nota privada" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Guardar campos" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Consultando campos personales"),
  ).not.toBeInTheDocument();
});

it("@s37 withdraws the resource when manual recovery returns a current 404", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        {
          configured: true,
          values: [
            { fieldId, label: "Revisado", type: "BOOLEAN", value: false },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
          },
        },
      ),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(new Response(null, { status: 404 }));
  vi.stubGlobal("fetch", fetcher);
  const withdrawn = vi.fn();
  function Fields() {
    const session = useCustomizationSession();
    return (
      <CustomFieldsPanel
        projectId={projectId}
        session={session}
        onAccessFailure={withdrawn}
      />
    );
  }
  render(<Fields />);
  const user = userEvent.setup();
  await screen.findByRole("combobox", { name: "Revisado" });
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  await user.click(
    await screen.findByRole("button", { name: "Recargar guardado" }),
  );
  expect(withdrawn).toHaveBeenCalledWith(404);
  expect(
    screen.queryByRole("button", { name: "Guardar campos" }),
  ).not.toBeInTheDocument();
});

it("@s34 explains a valid whole-set validation error without losing the draft", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        {
          configured: true,
          values: [{ fieldId, label: "Nota", type: "TEXT", value: "Guardada" }],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:0"`,
          },
        },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:validation_error",
          title: "Campos invalidos",
          status: 400,
          code: "VALIDATION_ERROR",
          errors: [
            {
              field: "values",
              code: "INVALID_VALUE",
              message: "Revisa el conjunto de campos.",
            },
          ],
        },
        { status: 400 },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  const input = await screen.findByRole("textbox", { name: "Nota" });
  await user.clear(input);
  await user.type(input, "Mi edición");
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Revisa el conjunto de campos.",
  );
  expect(input).toHaveValue("Mi edición");
  expect(screen.getByRole("button", { name: "Guardar campos" })).toBeEnabled();
});

it.each(["a".repeat(1001), "Texto\u0000", "Texto\ud800"])(
  "@s32 rejects invalid text locally without losing the draft: %j",
  async (text) => {
    const fetcher = vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: false,
          values: [{ fieldId, label: "Nota", type: "TEXT", value: null }],
          updatedAt: null,
        },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:unconfigured"`,
          },
        },
      ),
    );
    vi.stubGlobal("fetch", fetcher);
    render(<Surface />);
    const input = await screen.findByRole("textbox", { name: "Nota" });
    fireEvent.change(input, { target: { value: text } });
    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Guardar campos" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "hasta 1000 caracteres",
    );
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveAccessibleDescription(
      "Escribe hasta 1000 caracteres válidos.",
    );
    expect(input).toHaveValue(text);
    expect(fetcher).toHaveBeenCalledTimes(1);
  },
);
