import { render, screen, act, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import { App } from "./App";
describe("crear proyecto", () => {
  it("@s27 ofrece campos etiquetados y una acción de crear", () => {
    render(<App />);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(
      screen.getByRole("textbox", { name: /Nombre del proyecto/ }),
    ).toBeVisible();
    expect(screen.getByRole("textbox", { name: /Descripción/ })).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Crear proyecto" }),
    ).toBeEnabled();
  });
  it("@s22 confirma solo tras HTTP 201 usando los datos del servidor", async () => {
    let resolve!: (response: Response) => void;
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation(
      () =>
        new Promise((r) => {
          resolve = r;
        }),
    );
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByLabelText(/Nombre del proyecto/), "  Idea  ");
    await user.type(screen.getByLabelText(/Descripción/), "Avanzar");
    await user.click(screen.getByRole("button", { name: "Crear proyecto" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/projects", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: "  Idea  ", description: "Avanzar" }),
    });
    expect(screen.getByRole("status")).toHaveTextContent("Guardando…");
    expect(screen.queryByText("Proyecto guardado.")).not.toBeInTheDocument();
    await act(async () =>
      resolve(
        Response.json(
          {
            id: "project-1",
            ownerId: "owner-1",
            name: "Idea",
            description: "Avanzar",
            status: "idea",
            createdAt: "2026-09-05T12:00:00Z",
            updatedAt: "2026-09-05T12:00:00Z",
          },
          { status: 201 },
        ),
      ),
    );
    expect(screen.getByRole("status")).toHaveTextContent("Proyecto guardado.");
    expect(screen.getByRole("heading", { name: "Idea" })).toBeVisible();
    expect(screen.getByText("project-1")).toBeVisible();
  });
  it("@s23 bloquea clics y eventos de envío repetidos mientras guarda", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockImplementation(() => new Promise(() => {}));
    const user = userEvent.setup();
    render(<App />);
    const button = screen.getByRole("button", { name: "Crear proyecto" });
    await user.type(screen.getByLabelText(/Nombre del proyecto/), "Idea");
    await user.dblClick(button);
    expect(fireEvent.submit(button.closest("form")!)).toBe(false);
    expect(button).toBeDisabled();
    expect(screen.getByLabelText(/Nombre del proyecto/)).toHaveAttribute(
      "readonly",
    );
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("status")).toHaveTextContent("Guardando…");
  });
  it("@s24 conserva exactamente valores, asocia el error y permite corregir", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json(
        {
          code: "VALIDATION_ERROR",
          title: "Revisa los campos.",
          errors: [
            {
              field: "description",
              code: "TOO_LONG",
              message: "La descripción no puede superar los 4000 caracteres.",
            },
          ],
        },
        { status: 400 },
      ),
    );
    const user = userEvent.setup();
    render(<App />);
    const name = screen.getByLabelText(/Nombre del proyecto/);
    const description = screen.getByLabelText(/Descripción/);
    fireEvent.change(name, { target: { value: "  Idea  " } });
    fireEvent.change(description, { target: { value: "x".repeat(4001) } });
    await user.click(screen.getByRole("button", { name: "Crear proyecto" }));
    expect(name).toHaveValue("  Idea  ");
    expect(name).toHaveAttribute("aria-invalid", "false");
    expect(description).toHaveValue("x".repeat(4001));
    expect(description).toHaveAttribute("aria-invalid", "true");
    expect(description).toHaveAccessibleDescription(
      /La descripción no puede superar/,
    );
    expect(description).toHaveFocus();
    expect(screen.getByRole("status")).not.toHaveTextContent(
      "Proyecto guardado.",
    );
    expect(
      screen.getByRole("button", { name: "Crear proyecto" }),
    ).toBeEnabled();
    await user.clear(description);
    await user.type(description, "Corregido");
    expect(description).toHaveValue("Corregido");
  });
  it("@s25 informa de incertidumbre de red, conserva el formulario y no reintenta", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockRejectedValue(new TypeError("network"));
    const user = userEvent.setup();
    render(<App />);
    fireEvent.change(screen.getByLabelText(/Nombre del proyecto/), {
      target: { value: "Idea" },
    });
    fireEvent.change(screen.getByLabelText(/Descripción/), {
      target: { value: "Continuar mañana" },
    });
    await user.click(screen.getByRole("button", { name: "Crear proyecto" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No podemos confirmar si el proyecto se guardó.",
    );
    expect(screen.getByLabelText(/Nombre del proyecto/)).toHaveValue("Idea");
    expect(screen.getByLabelText(/Descripción/)).toHaveValue(
      "Continuar mañana",
    );
    expect(screen.getByLabelText(/Descripción/)).not.toHaveAttribute(
      "readonly",
    );
    expect(
      screen.getByRole("button", { name: "Crear proyecto" }),
    ).toBeEnabled();
    expect(screen.getByRole("status")).not.toHaveTextContent(
      "Proyecto guardado.",
    );
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
  it("@s21 muestra fallo confirmado del servicio sin confundirlo con fallo de red", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json(
        {
          code: "STORAGE_UNAVAILABLE",
          title: "El almacenamiento no está disponible.",
        },
        { status: 503 },
      ),
    );
    render(<App />);
    await userEvent.click(
      screen.getByRole("button", { name: "Crear proyecto" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "El almacenamiento no está disponible.",
    );
    expect(screen.getByRole("alert")).not.toHaveTextContent(
      "No podemos confirmar",
    );
    expect(
      screen.getByRole("button", { name: "Crear proyecto" }),
    ).toBeEnabled();
  });
  it("@s26 representa etiquetas literalmente como texto plano", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json(
        {
          id: "safe-id",
          ownerId: "owner",
          name: "<b>Idea</b>",
          description: "<script>alert(1)</script>",
          status: "idea",
          createdAt: "2026-09-05T12:00:00Z",
          updatedAt: "2026-09-05T12:00:00Z",
        },
        { status: 201 },
      ),
    );
    const { container } = render(<App />);
    await userEvent.click(
      screen.getByRole("button", { name: "Crear proyecto" }),
    );
    expect(
      await screen.findByRole("heading", { name: "<b>Idea</b>" }),
    ).toBeVisible();
    expect(screen.getByText("<script>alert(1)</script>")).toBeVisible();
    expect(container.querySelector("script, b")).toBeNull();
  });
  it("@s28 envía con teclado y mantiene foco en el control tras anunciar éxito", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json(
        {
          id: "keyboard-id",
          ownerId: "owner",
          name: "Teclado",
          description: "",
          status: "idea",
          createdAt: "2026-09-05T12:00:00Z",
          updatedAt: "2026-09-05T12:00:00Z",
        },
        { status: 201 },
      ),
    );
    const user = userEvent.setup();
    render(<App />);
    screen.getByLabelText(/Nombre del proyecto/).focus();
    await user.keyboard("Teclado");
    await user.tab();
    await user.tab();
    const button = screen.getByRole("button", { name: "Crear proyecto" });
    expect(button).toHaveFocus();
    await user.keyboard("{Enter}");
    expect(screen.getByRole("status")).toHaveTextContent("Proyecto guardado.");
    expect(button).toHaveFocus();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
  it("una respuesta 201 incompleta no puede confirmar el guardado", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json({ name: "Incompleto" }, { status: 201 }),
    );
    render(<App />);
    await userEvent.click(
      screen.getByRole("button", { name: "Crear proyecto" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No podemos confirmar si el proyecto se guardó.",
    );
    expect(screen.getByRole("status")).not.toHaveTextContent(
      "Proyecto guardado.",
    );
  });
  it("un proxy con respuesta no JSON no muestra detalles ni falsa confirmación", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response("<html>private stack trace</html>", { status: 502 }),
    );
    render(<App />);
    await userEvent.click(
      screen.getByRole("button", { name: "Crear proyecto" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "El servicio no ha confirmado la creación. Conservamos tus datos.",
    );
    expect(screen.getByRole("alert")).not.toHaveTextContent(
      "private stack trace",
    );
  });
  it("ofrece navegación al formulario y explica la captura sin inventar proyectos", () => {
    render(<App />);
    expect(
      screen.getByRole("heading", {
        level: 1,
        name: "Dale espacio a tu próxima idea.",
      }),
    ).toBeVisible();
    expect(screen.getByRole("navigation")).toHaveAccessibleName("Principal");
    expect(screen.getByRole("link", { name: "Proyectos" })).toHaveAttribute(
      "href",
      "/proyectos",
    );
    expect(
      screen.getByRole("link", { name: "Saltar al contenido" }),
    ).toHaveAttribute("href", "#proyectos");
    expect(screen.getByText("Tu idea empieza aquí")).toBeVisible();
    expect(
      screen.getByLabelText(/Nombre del proyecto/),
    ).toHaveAccessibleDescription(/Hasta 120 caracteres/);
  });
});
it("asocia error de nombre y permite recuperar el envío sin mensajes antiguos", async () => {
  const fetchMock = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(
        {
          title: "Revisa los campos.",
          errors: [
            { field: "name", code: "REQUIRED", message: "Escribe un nombre." },
          ],
        },
        { status: 400 },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          id: "retry-id",
          ownerId: "owner-id",
          name: "Nuevo",
          description: "",
          status: "idea",
          createdAt: "2026-09-05T12:00:00Z",
          updatedAt: "2026-09-05T12:00:00Z",
        },
        { status: 201 },
      ),
    );
  render(<App />);
  const user = userEvent.setup();
  await user.click(screen.getByRole("button", { name: "Crear proyecto" }));
  const name = screen.getByLabelText(/Nombre del proyecto/);
  expect(name).toHaveAccessibleDescription(/Escribe un nombre/);
  expect(screen.getByLabelText(/Descripción/)).toHaveAttribute(
    "aria-invalid",
    "false",
  );
  expect(name).toHaveAttribute("aria-invalid", "true");
  expect(name).toHaveFocus();
  await user.type(name, "Nuevo");
  await user.click(screen.getByRole("button", { name: "Crear proyecto" }));
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(name).toHaveAttribute("aria-invalid", "false");
  expect(screen.getByRole("status")).toHaveTextContent("Proyecto guardado.");
  expect(fetchMock).toHaveBeenCalledTimes(2);
});
it("una segunda captura no muestra la confirmación antigua mientras espera", async () => {
  const fetchMock = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(
        {
          id: "first-id",
          ownerId: "owner",
          name: "Primero",
          description: "",
          status: "idea",
          createdAt: "2026-09-05T12:00:00Z",
          updatedAt: "2026-09-05T12:00:00Z",
        },
        { status: 201 },
      ),
    )
    .mockImplementationOnce(() => new Promise(() => {}));
  render(<App />);
  const user = userEvent.setup();
  await user.click(screen.getByRole("button", { name: "Crear proyecto" }));
  expect(screen.getByText("first-id")).toBeVisible();
  await user.type(screen.getByLabelText(/Nombre del proyecto/), "Segundo");
  await user.click(screen.getByRole("button", { name: "Crear proyecto" }));
  expect(screen.queryByText("first-id")).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent("Guardando…");
  expect(screen.getByRole("status")).not.toHaveTextContent(
    "Proyecto guardado.",
  );
  expect(fetchMock).toHaveBeenCalledTimes(2);
});

it("restaura el foco de teclado perdido al deshabilitar el botón durante el POST", async () => {
  let resolve!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch").mockImplementation(
    () =>
      new Promise((r) => {
        resolve = r;
      }),
  );
  render(<App />);
  const button = screen.getByRole("button", { name: "Crear proyecto" });
  button.focus();
  fireEvent.submit(button.closest("form")!);
  document.body.tabIndex = -1;
  document.body.focus();
  document.body.removeAttribute("tabindex");
  expect(document.body).toHaveFocus();
  await act(async () =>
    resolve(
      Response.json(
        {
          id: "focus-id",
          ownerId: "owner",
          name: "Idea",
          description: "",
          status: "idea",
          createdAt: "2026-09-05T12:00:00Z",
          updatedAt: "2026-09-05T12:00:00Z",
        },
        { status: 201 },
      ),
    ),
  );
  expect(button).toBeEnabled();
  expect(button).toHaveFocus();
});
it("no roba el foco si la persona cambia de control durante la petición", async () => {
  let resolve!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch").mockImplementation(
    () =>
      new Promise((r) => {
        resolve = r;
      }),
  );
  render(<App />);
  const button = screen.getByRole("button", { name: "Crear proyecto" });
  button.focus();
  fireEvent.submit(button.closest("form")!);
  const link = screen.getByRole("link", { name: "Proyectos" });
  link.focus();
  await act(async () =>
    resolve(
      Response.json(
        {
          id: "focus-id",
          ownerId: "owner",
          name: "Idea",
          description: "",
          status: "idea",
          createdAt: "2026-09-05T12:00:00Z",
          updatedAt: "2026-09-05T12:00:00Z",
        },
        { status: 201 },
      ),
    ),
  );
  expect(link).toHaveFocus();
});
