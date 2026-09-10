// @ts-nocheck
import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { observeAccess } from "./api-client";
import { CustomFieldsPanel } from "./custom-fields";
import { CustomizationControls } from "./customization";
import { useCustomizationSession } from "./customization-state";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
});
function Surface() {
  const session = useCustomizationSession();
  return <CustomizationControls scope="PROJECT" session={session} />;
}

it("@s29 partial opens the project view and edits only a local draft", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      {
        configured: false,
        visibleFields: ["createdAt"],
        customFields: [],
        updatedAt: null,
      },
      { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  expect(screen.getByRole("checkbox", { name: "Creado" })).toBeChecked();
  expect(
    screen.getByRole("checkbox", { name: "Actualizado" }),
  ).not.toBeChecked();
  await user.click(screen.getByRole("checkbox", { name: "Creado" }));
  expect(screen.getByRole("checkbox", { name: "Creado" })).not.toBeChecked();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s29 shares one lazy configuration read between consumers of the same scope", async () => {
  const fetcher = vi.fn().mockImplementation(() =>
    Response.json(
      {
        configured: false,
        visibleFields: ["createdAt"],
        customFields: [],
        updatedAt: null,
      },
      { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  function SharedSurface() {
    const session = useCustomizationSession();
    return (
      <>
        <CustomizationControls scope="PROJECT" session={session} />
        <CustomizationControls scope="PROJECT" session={session} />
      </>
    );
  }
  render(<SharedSurface />);
  expect(
    await screen.findAllByRole("button", { name: "Personalizar vista" }),
  ).toHaveLength(2);
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s30 saves the explicitly selected view and confirms it", async () => {
  const initial = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, {
        headers: { ETag: '"customization:PROJECT:unconfigured"' },
      }),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          ...initial,
          configured: true,
          visibleFields: ["createdAt", "updatedAt"],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  await user.click(screen.getByRole("checkbox", { name: "Actualizado" }));
  await user.click(screen.getByRole("button", { name: "Guardar vista" }));
  expect(await screen.findByText("Vista guardada")).toHaveAttribute(
    "role",
    "status",
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    visibleFields: ["createdAt", "updatedAt"],
  });
});

it("@s30 restores project defaults only in the draft until explicitly saved", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      {
        configured: true,
        visibleFields: ["updatedAt"],
        customFields: [],
        updatedAt: "2026-09-07T10:00:00Z",
      },
      {
        headers: {
          ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
        },
      },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  await user.click(screen.getByRole("button", { name: "Restaurar vista" }));
  expect(screen.getByRole("checkbox", { name: "Creado" })).toBeChecked();
  expect(
    screen.getByRole("checkbox", { name: "Actualizado" }),
  ).not.toBeChecked();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s29 exposes task metadata and restores task defaults without a write", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      {
        configured: true,
        visibleFields: [],
        customFields: [],
        updatedAt: "2026-09-07T10:00:00Z",
      },
      {
        headers: {
          ETag: '"customization:TASK:12345678-1234-1234-1234-123456789abc:0"',
        },
      },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  function TaskSurface() {
    const session = useCustomizationSession();
    return <CustomizationControls scope="TASK" session={session} />;
  }
  render(<TaskSurface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  await user.click(screen.getByRole("button", { name: "Restaurar vista" }));
  expect(
    screen.getByRole("checkbox", { name: "Criterio de finalización" }),
  ).toBeChecked();
  expect(screen.getByRole("checkbox", { name: "Estimación" })).toBeChecked();
  expect(screen.getByRole("checkbox", { name: "Creado" })).not.toBeChecked();
  expect(
    screen.getByRole("checkbox", { name: "Actualizado" }),
  ).not.toBeChecked();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s30 cancels the local view draft and reopens the saved selection", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      {
        configured: false,
        visibleFields: ["createdAt"],
        customFields: [],
        updatedAt: null,
      },
      { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  await user.click(screen.getByRole("checkbox", { name: "Creado" }));
  await user.click(screen.getByRole("button", { name: "Cancelar" }));
  expect(
    screen.queryByRole("group", { name: "Metadatos visibles" }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Personalizar vista" }));
  expect(screen.getByRole("checkbox", { name: "Creado" })).toBeChecked();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s29 reorders selected metadata through named keyboard-accessible controls", async () => {
  const initial = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, {
        headers: { ETag: '"customization:PROJECT:unconfigured"' },
      }),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          ...initial,
          configured: true,
          visibleFields: ["updatedAt", "createdAt"],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  await user.click(screen.getByRole("checkbox", { name: "Actualizado" }));
  const up = screen.getByRole("button", { name: "Subir Actualizado" });
  up.focus();
  await user.keyboard("{Enter}");
  expect(
    screen.getAllByRole("listitem").map((item) => item.textContent),
  ).toEqual(["ActualizadoSubirBajar", "CreadoSubirBajar"]);
  expect(
    screen.getByRole("button", { name: "Subir Actualizado" }),
  ).toBeDisabled();
  expect(screen.getByRole("button", { name: "Bajar Creado" })).toBeDisabled();
  await user.click(screen.getByRole("button", { name: "Guardar vista" }));
  expect(await screen.findByText("Vista guardada")).toBeInTheDocument();
  expect(
    screen.getByRole("region", { name: "Personalización de vista" }),
  ).toHaveFocus();
  await user.click(screen.getByRole("button", { name: "Personalizar vista" }));
  expect(screen.queryByText("Vista guardada")).not.toBeInTheDocument();
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    visibleFields: ["updatedAt", "createdAt"],
  });
});

it("@s33 labels the provisional base after a failed read and retries only deliberately", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(
      Response.json(
        {
          configured: false,
          visibleFields: ["createdAt"],
          customFields: [],
          updatedAt: null,
        },
        { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Vista base provisional: no se pudo consultar lo guardado",
  );
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Personalizar vista" }),
  ).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  ).toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s35 retains an uncertain save across navigation until a deliberate valid read", async () => {
  const base = () =>
    Response.json(
      {
        configured: false,
        visibleFields: ["createdAt"],
        customFields: [],
        updatedAt: null,
      },
      { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
    );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(base())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(base());
  vi.stubGlobal("fetch", fetcher);
  function Navigable({ visible }: { visible: boolean }) {
    const session = useCustomizationSession();
    return visible ? (
      <CustomizationControls scope="PROJECT" session={session} />
    ) : (
      <h1>Otra vista</h1>
    );
  }
  const view = render(<Navigable visible />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  await user.click(screen.getByRole("checkbox", { name: "Actualizado" }));
  await user.click(screen.getByRole("button", { name: "Guardar vista" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pudo confirmar el guardado",
  );
  view.rerender(<Navigable visible={false} />);
  view.rerender(<Navigable visible />);
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No se pudo confirmar el guardado",
  );
  expect(
    screen.queryByRole("button", { name: "Guardar vista" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
  await user.click(screen.getByRole("button", { name: "Recargar guardado" }));
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  expect(
    screen.getByRole("checkbox", { name: "Actualizado" }),
  ).not.toBeChecked();
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s34 announces an in-flight save and prevents a duplicate or draft edits", async () => {
  const initial = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
  };
  let finish!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, {
        headers: { ETag: '"customization:PROJECT:unconfigured"' },
      }),
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
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  const save = screen.getByRole("button", { name: "Guardar vista" });
  await user.dblClick(save);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Guardando personalización",
  );
  expect(save).toBeDisabled();
  expect(screen.getByRole("checkbox", { name: "Creado" })).toBeDisabled();
  expect(fetcher).toHaveBeenCalledTimes(2);
  const chosen = screen.getByRole("button", { name: "Personalizar vista" });
  chosen.focus();
  chosen.blur();
  expect(document.body).toHaveFocus();
  await act(async () =>
    finish(
      Response.json(
        { ...initial, configured: true, updatedAt: "2026-09-07T10:00:00Z" },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  expect(await screen.findByText("Vista guardada")).toBeInTheDocument();
  expect(document.body).toHaveFocus();
});

it("@s35 keeps the saved draft inaccessible while the recovery read is pending", async () => {
  const base = () =>
    Response.json(
      {
        configured: false,
        visibleFields: ["createdAt"],
        customFields: [],
        updatedAt: null,
      },
      { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
    );
  let finish!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(base())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    );
  vi.stubGlobal("fetch", fetcher);
  function DraftConsumer() {
    const session = useCustomizationSession();
    return (
      <>
        <CustomizationControls scope="PROJECT" session={session} />
        <p>
          Borrador retenido:{" "}
          {session.viewDrafts.PROJECT?.join(",") ?? "ninguno"}
        </p>
      </>
    );
  }
  render(<DraftConsumer />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  await user.click(screen.getByRole("checkbox", { name: "Actualizado" }));
  await user.click(screen.getByRole("button", { name: "Guardar vista" }));
  await user.dblClick(
    await screen.findByRole("button", { name: "Recargar guardado" }),
  );
  expect(screen.getByRole("status")).toHaveTextContent("Consultando vista");
  expect(
    screen.queryByRole("button", { name: "Guardar vista" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(3);
  expect(
    screen.getByText("Borrador retenido: createdAt,updatedAt"),
  ).toBeInTheDocument();
  await act(async () => finish(base()));
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  expect(
    screen.getByRole("checkbox", { name: "Actualizado" }),
  ).not.toBeChecked();
});

it("@s37 discards a previous session read before a late HTTP 401 reaches the access observer", async () => {
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
  const observer = vi.fn();
  observeAccess(observer);
  const view = render(<Surface />);
  expect(screen.getByRole("status")).toHaveTextContent("Consultando vista");
  view.unmount();
  await act(async () => finish(new Response(null, { status: 401 })));
  expect(observer).not.toHaveBeenCalled();
});

it("@s34 preserves a rejected view draft for correction without requiring recovery", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        {
          configured: false,
          visibleFields: ["createdAt"],
          customFields: [],
          updatedAt: null,
        },
        { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
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
              field: "visibleFields",
              code: "INVALID_VALUE",
              message: "Revisa los metadatos elegidos.",
            },
          ],
        },
        { status: 400 },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  await user.click(screen.getByRole("checkbox", { name: "Actualizado" }));
  await user.click(screen.getByRole("button", { name: "Guardar vista" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Revisa los metadatos elegidos.",
  );
  expect(screen.getByRole("checkbox", { name: "Actualizado" })).toBeChecked();
  expect(screen.getByRole("button", { name: "Guardar vista" })).toBeEnabled();
  expect(
    screen.queryByRole("button", { name: "Recargar guardado" }),
  ).not.toBeInTheDocument();
});

it("@s31 creates a typed personal field without changing the saved view", async () => {
  const initial = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, {
        headers: { ETag: '"customization:PROJECT:unconfigured"' },
      }),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          ...initial,
          configured: true,
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "Prioridad personal",
              type: "NUMBER",
              active: true,
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Gestionar campos personales" }),
  );
  expect(
    screen.getByText(/12 campos, incluidos los desactivados/),
  ).toBeInTheDocument();
  await user.type(
    screen.getByRole("textbox", { name: "Etiqueta" }),
    "Prioridad personal",
  );
  await user.selectOptions(
    screen.getByRole("combobox", { name: "Tipo" }),
    "NUMBER",
  );
  await user.click(screen.getByRole("button", { name: "Crear campo" }));
  expect(await screen.findByRole("status")).toHaveTextContent("Campo creado");
  expect(screen.getByText("Prioridad personal")).toBeInTheDocument();
  await user.type(
    screen.getByRole("textbox", { name: "Etiqueta" }),
    "Otra nota",
  );
  expect(screen.queryByText("Campo creado")).not.toBeInTheDocument();
  expect(fetcher.mock.calls[1][0]).toBe(
    "/api/v1/me/customization/PROJECT/fields",
  );
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    label: "Prioridad personal",
    type: "NUMBER",
  });
});

it("@s31 explains a blank field label locally without sending a creation request", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      {
        configured: false,
        visibleFields: ["createdAt"],
        customFields: [],
        updatedAt: null,
      },
      { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Gestionar campos personales" }),
  );
  const label = screen.getByRole("textbox", { name: "Etiqueta" });
  await user.type(label, "   ");
  await user.click(screen.getByRole("button", { name: "Crear campo" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Escribe una etiqueta de 1 a 60 caracteres válidos",
  );
  expect(label).toHaveAttribute("aria-invalid", "true");
  expect(label).toHaveAccessibleDescription(
    "Escribe una etiqueta de 1 a 60 caracteres válidos.",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s31 renames and deactivates a field while keeping its immutable type", async () => {
  const field = {
    id: "22345678-1234-1234-1234-123456789abc",
    label: "Prioridad",
    type: "NUMBER",
    active: true,
  };
  const initial = {
    configured: true,
    visibleFields: ["createdAt"],
    customFields: [field],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const tag = "customization:PROJECT:12345678-1234-1234-1234-123456789abc:";
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(initial, { headers: { ETag: `"${tag}0"` } }),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          ...initial,
          customFields: [{ ...field, label: "Orden personal", active: false }],
        },
        { headers: { ETag: `"${tag}1"` } },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Gestionar campos personales" }),
  );
  await user.click(screen.getByRole("button", { name: "Editar Prioridad" }));
  expect(
    screen.queryByRole("combobox", { name: "Tipo" }),
  ).not.toBeInTheDocument();
  const label = screen.getByRole("textbox", { name: "Etiqueta" });
  await user.clear(label);
  await user.type(label, "Orden personal");
  await user.click(screen.getByRole("button", { name: "Cancelar edición" }));
  expect(screen.getByRole("textbox", { name: "Etiqueta" })).toHaveValue("");
  expect(
    screen.getByRole("group", { name: "Nuevo campo" }),
  ).toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(1);
  await user.click(screen.getByRole("button", { name: "Editar Prioridad" }));
  await user.clear(screen.getByRole("textbox", { name: "Etiqueta" }));
  await user.type(
    screen.getByRole("textbox", { name: "Etiqueta" }),
    "Orden personal",
  );
  await user.click(screen.getByRole("checkbox", { name: "Activo" }));
  await user.click(screen.getByRole("button", { name: "Guardar campo" }));
  expect(await screen.findByRole("status")).toHaveTextContent("Campo guardado");
  expect(
    screen.getByRole("button", { name: "Editar Orden personal" }).closest("li"),
  ).toHaveTextContent("Desactivado");
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    label: "Orden personal",
    active: false,
  });
  expect(fetcher.mock.calls[1][0]).toBe(
    `/api/v1/me/customization/PROJECT/fields/${field.id}`,
  );
});

it("@s37 aborts a pending configuration write before a late HTTP 401 observes access", async () => {
  let finish!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        {
          configured: false,
          visibleFields: ["createdAt"],
          customFields: [],
          updatedAt: null,
        },
        { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
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
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  await user.click(screen.getByRole("button", { name: "Guardar vista" }));
  expect(fetcher).toHaveBeenCalledTimes(2);
  view.unmount();
  await act(async () => finish(new Response(null, { status: 401 })));
  expect(observer).not.toHaveBeenCalled();
});

it("retains the unsaved view selection across internal navigation in the same session", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      {
        configured: false,
        visibleFields: ["createdAt"],
        customFields: [],
        updatedAt: null,
      },
      { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  function Navigable({ visible }: { visible: boolean }) {
    const session = useCustomizationSession();
    return visible ? (
      <CustomizationControls scope="PROJECT" session={session} />
    ) : (
      <h1>Otra vista</h1>
    );
  }
  const view = render(<Navigable visible />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  await user.click(screen.getByRole("checkbox", { name: "Actualizado" }));
  view.rerender(<Navigable visible={false} />);
  view.rerender(<Navigable visible />);
  expect(screen.getByRole("checkbox", { name: "Actualizado" })).toBeChecked();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s38 keeps a confirmed configuration when an older read from another consumer arrives", async () => {
  const initial = {
    configured: true,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const tag = "customization:PROJECT:12345678-1234-1234-1234-123456789abc:";
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
        { ...initial, visibleFields: [] },
        { headers: { ETag: `"${tag}1"` } },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  function Consumers() {
    const session = useCustomizationSession();
    return (
      <>
        <CustomizationControls scope="PROJECT" session={session} />
        <button onClick={() => void session.reloadConfig("PROJECT")}>
          Consultar desde otro consumidor
        </button>
        <button onClick={() => void session.saveView("PROJECT", [])}>
          Guardar desde otro consumidor
        </button>
        <p>
          Metadatos confirmados: {session.configs.PROJECT?.visibleFields.length}
        </p>
      </>
    );
  }
  render(<Consumers />);
  const user = userEvent.setup();
  await screen.findByRole("button", { name: "Personalizar vista" });
  await user.click(
    screen.getByRole("button", { name: "Consultar desde otro consumidor" }),
  );
  await user.click(
    screen.getByRole("button", { name: "Guardar desde otro consumidor" }),
  );
  expect(
    await screen.findByText("Metadatos confirmados: 0"),
  ).toBeInTheDocument();
  await act(async () =>
    finish(Response.json(initial, { headers: { ETag: `"${tag}0"` } })),
  );
  expect(screen.getByText("Metadatos confirmados: 0")).toBeInTheDocument();
  expect(screen.queryByText("Consultando vista")).not.toBeInTheDocument();
});

it.each([
  { dirty: false, mounted: false },
  { dirty: true, mounted: false },
  { dirty: false, mounted: true },
  { dirty: true, mounted: true, late: true },
  { dirty: false, mounted: false, recover: true },
  { dirty: false, mounted: false, external: true },
])(
  "@s38 applies a known schema change (dirty=$dirty, mounted=$mounted)",
  async ({ dirty, mounted, late, recover, external }) => {
    const projectId = "62345678-1234-1234-1234-123456789abc";
    const fieldId = "22345678-1234-1234-1234-123456789abc";
    const schemaId = "12345678-1234-1234-1234-123456789abc";
    let renamed = false;
    let releaseRename!: () => void;
    let valueReads = 0;
    const fetcher = vi
      .fn()
      .mockImplementation((url: string, options?: RequestInit) => {
        if (url === `/api/v1/projects/${projectId}/custom-fields`) {
          valueReads++;
          return Promise.resolve(
            Response.json(
              {
                configured: true,
                values: [
                  {
                    fieldId,
                    label: renamed ? "Etiqueta nueva" : "Etiqueta anterior",
                    type: "TEXT",
                    value: "Dato guardado",
                  },
                ],
                updatedAt: "2026-09-07T10:00:00Z",
              },
              {
                headers: {
                  ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:${renamed ? 1 : 0}:values:42345678-1234-1234-1234-123456789abc:0"`,
                },
              },
            ),
          );
        }
        if (
          url !== "/api/v1/me/customization/PROJECT" &&
          !(
            url === `/api/v1/me/customization/PROJECT/fields/${fieldId}` &&
            options?.method === "PUT"
          )
        )
          return Promise.reject(new Error("Ruta no prevista"));
        if (options?.method === "PUT") {
          renamed = true;
          if (recover)
            return Promise.resolve(new Response(null, { status: 412 }));
        }
        const result = Response.json(
          {
            configured: true,
            visibleFields: ["createdAt"],
            customFields: [
              {
                id: fieldId,
                label: renamed ? "Etiqueta nueva" : "Etiqueta anterior",
                type: "TEXT",
                active: true,
              },
            ],
            updatedAt: "2026-09-07T10:00:00Z",
          },
          {
            headers: {
              ETag: `"customization:PROJECT:${schemaId}:${renamed ? 1 : 0}"`,
            },
          },
        );
        if (late && options?.method === "PUT")
          return new Promise<Response>((resolve) => {
            releaseRename = () => resolve(result);
          });
        return Promise.resolve(result);
      });
    vi.stubGlobal("fetch", fetcher);
    function Context({ detail }: { detail: boolean }) {
      const session = useCustomizationSession();
      return (
        <>
          {(detail || mounted) && (
            <CustomFieldsPanel projectId={projectId} session={session} />
          )}
          {(!detail || mounted) && (
            <CustomizationControls scope="PROJECT" session={session} />
          )}
        </>
      );
    }
    const view = render(<Context detail />);
    const user = userEvent.setup();
    expect(
      await screen.findByRole("textbox", { name: "Etiqueta anterior" }),
    ).toHaveValue("Dato guardado");
    if (dirty && !late) {
      const value = screen.getByRole("textbox", { name: "Etiqueta anterior" });
      await user.clear(value);
      await user.type(value, "Borrador conservado");
    }
    if (external) renamed = true;
    view.rerender(<Context detail={false} />);
    if (external) {
      await screen.findByRole("button", { name: "Personalizar vista" });
      view.rerender(<Context detail />);
      expect(
        await screen.findByRole("textbox", { name: "Etiqueta nueva" }),
      ).toHaveValue("Dato guardado");
      expect(valueReads).toBe(2);
      return;
    }
    await user.click(
      await screen.findByRole("button", {
        name: "Gestionar campos personales",
      }),
    );
    await user.click(
      screen.getByRole("button", { name: "Editar Etiqueta anterior" }),
    );
    const label = screen.getByRole("textbox", { name: "Etiqueta" });
    await user.clear(label);
    await user.type(label, "Etiqueta nueva");
    await user.click(screen.getByRole("button", { name: "Guardar campo" }));
    if (late) {
      const value = screen.getByRole("textbox", { name: "Etiqueta anterior" });
      await user.clear(value);
      await user.type(value, "Borrador conservado");
      await act(async () => releaseRename());
    }
    if (recover) {
      await user.click(
        await screen.findByRole("button", { name: "Recargar guardado" }),
      );
      await screen.findByRole("button", { name: "Personalizar vista" });
    } else await screen.findByText("Campo guardado");
    view.rerender(<Context detail />);
    if (dirty) {
      expect(await screen.findByRole("alert")).toHaveTextContent(
        "Los campos han cambiado",
      );
      expect(
        screen.getByRole("textbox", { name: "Etiqueta anterior" }),
      ).toHaveValue("Borrador conservado");
      expect(
        screen.getByRole("button", { name: "Guardar campos" }),
      ).toBeDisabled();
      expect(valueReads).toBe(1);
      await user.click(
        screen.getByRole("button", { name: "Recargar guardado" }),
      );
    }
    expect(
      await screen.findByRole("textbox", { name: "Etiqueta nueva" }),
    ).toHaveValue("Dato guardado");
    expect(valueReads).toBe(2);
  },
);

it("@s31 prevents reusing an inactive field label before sending", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      {
        configured: true,
        visibleFields: ["createdAt"],
        updatedAt: "2026-09-07T10:00:00Z",
        customFields: [
          {
            id: "22345678-1234-1234-1234-123456789abc",
            label: "Reservada",
            type: "TEXT",
            active: false,
          },
        ],
      },
      {
        headers: {
          ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
        },
      },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Gestionar campos personales" }),
  );
  await user.type(
    screen.getByRole("textbox", { name: "Etiqueta" }),
    "Reservada",
  );
  await user.click(screen.getByRole("button", { name: "Crear campo" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Ya existe un campo con esa etiqueta",
  );
  expect(screen.getByRole("textbox", { name: "Etiqueta" })).toHaveValue(
    "Reservada",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s31 keeps editing available at the twelve-field limit including inactive fields", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: ["createdAt"],
          updatedAt: "2026-09-07T10:00:00Z",
          customFields: Array.from({ length: 12 }, (_, index) => ({
            id: `22345678-1234-1234-1234-${String(index).padStart(12, "0")}`,
            label: `Campo ${index}`,
            type: "TEXT",
            active: false,
          })),
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Gestionar campos personales" }),
  );
  expect(screen.getByRole("button", { name: "Crear campo" })).toBeDisabled();
  await user.click(screen.getByRole("button", { name: "Editar Campo 0" }));
  expect(screen.getByRole("button", { name: "Guardar campo" })).toBeEnabled();
});

it("@s38 retires an old value ACK after observing a changed schema", async () => {
  const projectId = "62345678-1234-1234-1234-123456789abc";
  const fieldId = "22345678-1234-1234-1234-123456789abc";
  const schemaId = "12345678-1234-1234-1234-123456789abc";
  let finish!: () => void;
  let renamed = false;
  const fetcher = vi
    .fn()
    .mockImplementation((url: string, options?: RequestInit) => {
      if (url.endsWith("/custom-fields")) {
        const result = Response.json(
          {
            configured: true,
            values: [
              {
                fieldId,
                label: "Nota",
                type: "TEXT",
                value: options?.method === "PUT" ? "Borrador" : "Guardada",
              },
            ],
            updatedAt: "2026-09-07T10:00:00Z",
          },
          {
            headers: {
              ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:42345678-1234-1234-1234-123456789abc:${options?.method === "PUT" ? 1 : 0}"`,
            },
          },
        );
        if (options?.method === "PUT")
          return new Promise<Response>((resolve) => {
            finish = () => resolve(result);
          });
        return Promise.resolve(result);
      }
      if (options?.method === "PUT") renamed = true;
      return Promise.resolve(
        Response.json(
          {
            configured: true,
            visibleFields: ["createdAt"],
            customFields: [
              {
                id: fieldId,
                label: renamed ? "Nota nueva" : "Nota",
                type: "TEXT",
                active: true,
              },
            ],
            updatedAt: "2026-09-07T10:00:00Z",
          },
          {
            headers: {
              ETag: `"customization:PROJECT:${schemaId}:${renamed ? 1 : 0}"`,
            },
          },
        ),
      );
    });
  vi.stubGlobal("fetch", fetcher);
  function Both() {
    const session = useCustomizationSession();
    return (
      <>
        <CustomizationControls scope="PROJECT" session={session} />
        <CustomFieldsPanel projectId={projectId} session={session} />
      </>
    );
  }
  render(<Both />);
  const user = userEvent.setup();
  const input = await screen.findByRole("textbox", { name: "Nota" });
  await user.clear(input);
  await user.type(input, "Borrador");
  await user.click(screen.getByRole("button", { name: "Guardar campos" }));
  await user.click(
    screen.getByRole("button", { name: "Gestionar campos personales" }),
  );
  await user.click(screen.getByRole("button", { name: "Editar Nota" }));
  await user.clear(screen.getByRole("textbox", { name: "Etiqueta" }));
  await user.type(
    screen.getByRole("textbox", { name: "Etiqueta" }),
    "Nota nueva",
  );
  await user.click(screen.getByRole("button", { name: "Guardar campo" }));
  await screen.findByText("Campo guardado");
  await act(async () => finish());
  expect(screen.queryByText("Campos guardados")).not.toBeInTheDocument();
  expect(screen.getByRole("textbox", { name: "Nota" })).toHaveValue("Borrador");
  expect(screen.getByRole("button", { name: "Guardar campos" })).toBeDisabled();
  expect(
    screen.getByRole("button", { name: "Recargar guardado" }),
  ).toBeEnabled();
});

it("@s38 does not publish an initial value response older than the observed configuration", async () => {
  const projectId = "62345678-1234-1234-1234-123456789abc";
  const schemaId = "12345678-1234-1234-1234-123456789abc";
  let finish!: () => void;
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url.endsWith("/custom-fields"))
      return new Promise<Response>((resolve) => {
        finish = () =>
          resolve(
            Response.json(
              {
                configured: false,
                values: [
                  {
                    fieldId: "22345678-1234-1234-1234-123456789abc",
                    label: "Anterior",
                    type: "TEXT",
                    value: null,
                  },
                ],
                updatedAt: null,
              },
              {
                headers: {
                  ETag: `"custom-values:PROJECT:${projectId}:schema:${schemaId}:0:values:unconfigured"`,
                },
              },
            ),
          );
      });
    return Promise.resolve(
      Response.json(
        {
          configured: true,
          visibleFields: ["createdAt"],
          customFields: [],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        { headers: { ETag: `"customization:PROJECT:${schemaId}:1"` } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetcher);
  function Both() {
    const session = useCustomizationSession();
    return (
      <>
        <CustomFieldsPanel projectId={projectId} session={session} />
        <CustomizationControls scope="PROJECT" session={session} />
      </>
    );
  }
  render(<Both />);
  await screen.findByRole("button", { name: "Personalizar vista" });
  await act(async () => finish());
  expect(
    screen.queryByRole("textbox", { name: "Anterior" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No se pudieron consultar",
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
});
