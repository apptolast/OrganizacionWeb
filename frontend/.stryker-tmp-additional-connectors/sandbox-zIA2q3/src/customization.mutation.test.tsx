// @ts-nocheck
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { CustomizationControls } from "./customization";
import { useCustomizationSession } from "./customization-state";

afterEach(() => vi.unstubAllGlobals());

function Surface() {
  const session = useCustomizationSession();
  return <CustomizationControls scope="PROJECT" session={session} />;
}

const configurationId = "12345678-1234-1234-1234-123456789abc";
function configured(body: object, version = 0) {
  return Response.json(
    {
      configured: true,
      visibleFields: ["createdAt", "updatedAt"],
      customFields: [],
      updatedAt: "2026-09-07T10:00:00Z",
      ...body,
    },
    {
      headers: {
        ETag: `"customization:PROJECT:${configurationId}:${version}"`,
      },
    },
  );
}

it("@s31 accepts a sixty-code-point label with Unicode edge spaces and permits its own unchanged rename", async () => {
  const label = "😀".repeat(56) + "\u2003Fin";
  const padded = "\u2002\u2009" + label + "\u00a0\u3000";
  const field = {
    id: "22345678-1234-1234-1234-123456789abc",
    label,
    type: "TEXT",
    active: true,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(configured({}))
    .mockResolvedValueOnce(configured({ customFields: [field] }, 1))
    .mockResolvedValueOnce(configured({ customFields: [field] }, 1));
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Gestionar campos personales" }),
  );
  await user.click(screen.getByRole("textbox", { name: "Etiqueta" }));
  await user.paste(padded);
  await user.click(screen.getByRole("button", { name: "Crear campo" }));
  expect(await screen.findByText("Campo creado")).toBeInTheDocument();
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    label,
    type: "TEXT",
  });
  await user.click(screen.getByRole("button", { name: `Editar ${label}` }));
  expect(screen.getByRole("textbox", { name: "Etiqueta" })).toHaveValue(label);
  await user.clear(screen.getByRole("textbox", { name: "Etiqueta" }));
  await user.paste(padded);
  await user.click(screen.getByRole("button", { name: "Guardar campo" }));
  expect(await screen.findByText("Campo guardado")).toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(3);
  expect(fetcher.mock.calls[2][0]).toBe(
    `/api/v1/me/customization/PROJECT/fields/${field.id}`,
  );
  expect(fetcher.mock.calls[2][1].method).toBe("PUT");
  expect(JSON.parse(fetcher.mock.calls[2][1].body)).toEqual({
    label,
    active: true,
  });
});

it("@s29 @s30 lowers a selected column and saves after removing another column", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(configured({}))
    .mockResolvedValueOnce(configured({ visibleFields: ["updatedAt"] }, 1));
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  await user.click(screen.getByRole("button", { name: "Bajar Creado" }));
  expect(
    screen.getAllByRole("listitem").map((item) => item.textContent),
  ).toEqual(["ActualizadoSubirBajar", "CreadoSubirBajar"]);
  expect(screen.getByRole("button", { name: "Bajar Creado" })).toBeDisabled();
  expect(
    screen.getByRole("button", { name: "Subir Actualizado" }),
  ).toBeDisabled();
  await user.click(screen.getByRole("checkbox", { name: "Creado" }));
  expect(screen.getByRole("checkbox", { name: "Creado" })).not.toBeChecked();
  expect(screen.getByRole("checkbox", { name: "Actualizado" })).toBeChecked();
  expect(
    screen.queryByRole("button", { name: "Subir Creado" }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Guardar vista" }));
  expect(await screen.findByText("Vista guardada")).toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    visibleFields: ["updatedAt"],
  });
});

it("@s31 cancels editing an inactive numeric field before creating a text field", async () => {
  const field = {
    id: "22345678-1234-1234-1234-123456789abc",
    label: "Prioridad",
    type: "NUMBER",
    active: false,
  };
  const created = {
    id: "32345678-1234-1234-1234-123456789abc",
    label: "Nota",
    type: "TEXT",
    active: true,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(configured({ customFields: [field] }))
    .mockResolvedValueOnce(configured({ customFields: [field, created] }, 1));
  vi.stubGlobal("fetch", fetcher);
  render(<Surface />);
  const user = userEvent.setup();
  await user.click(
    await screen.findByRole("button", { name: "Gestionar campos personales" }),
  );
  await user.click(screen.getByRole("button", { name: "Editar Prioridad" }));
  expect(screen.getByRole("textbox", { name: "Etiqueta" })).toHaveValue(
    "Prioridad",
  );
  expect(screen.getByRole("checkbox", { name: "Activo" })).not.toBeChecked();
  expect(
    screen.queryByRole("combobox", { name: "Tipo" }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Cancelar edición" }));
  expect(screen.getByRole("textbox", { name: "Etiqueta" })).toHaveValue("");
  expect(screen.getByRole("combobox", { name: "Tipo" })).toHaveValue("TEXT");
  expect(
    screen.queryByRole("checkbox", { name: "Activo" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(1);
  await user.type(screen.getByRole("textbox", { name: "Etiqueta" }), "Nota");
  await user.click(screen.getByRole("button", { name: "Crear campo" }));
  expect(await screen.findByText("Campo creado")).toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(fetcher.mock.calls[1][0]).toBe(
    "/api/v1/me/customization/PROJECT/fields",
  );
  expect(fetcher.mock.calls[1][1].method).toBe("POST");
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    label: "Nota",
    type: "TEXT",
  });
  await user.click(screen.getByRole("button", { name: "Editar Prioridad" }));
  expect(screen.getByRole("checkbox", { name: "Activo" })).not.toBeChecked();
});
