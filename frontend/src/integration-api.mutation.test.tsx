import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { IntegrationApi } from "./integration-api";

const id = "12345678-1234-4234-8234-123456789abc";
const key = "organizationweb.api-credential.pending.v1";
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  sessionStorage.clear();
});
function stubApi(fetcher: typeof fetch) {
  vi.spyOn(crypto, "randomUUID").mockReturnValue(id);
  vi.stubGlobal("fetch", (url: RequestInfo | URL, options?: RequestInit) =>
    url === "/api/v1/me/api-credentials" &&
    (!options?.method || options.method === "GET")
      ? Promise.resolve(Response.json({ items: [], nextCursor: null }))
      : fetcher(url, options),
  );
}

it("@s33 sends the selected permissions after deselection and the deliberately changed lifetime", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(new Response(null, { status: 503 }));
  stubApi(fetcher);
  const user = userEvent.setup();
  render(<IntegrationApi owner="Ana" />);
  await user.type(
    screen.getByRole("textbox", { name: "Nombre" }),
    "Agenda propia",
  );
  await user.click(screen.getByRole("checkbox", { name: "Leer proyectos" }));
  await user.click(screen.getByRole("checkbox", { name: "Leer agenda" }));
  await user.click(screen.getByRole("checkbox", { name: "Leer proyectos" }));
  expect(
    screen.getByRole("checkbox", { name: "Leer proyectos" }),
  ).not.toBeChecked();
  await user.selectOptions(
    screen.getByRole("combobox", { name: "Caducidad" }),
    "7",
  );
  await user.selectOptions(
    screen.getByRole("combobox", { name: "Caducidad" }),
    "90",
  );
  await user.click(screen.getByRole("button", { name: "Crear" }));
  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(JSON.parse(fetcher.mock.calls[0][1].body)).toEqual({
    name: "Agenda propia",
    scopes: ["agenda:read"],
    expiresInDays: 90,
  });
  expect(JSON.parse(sessionStorage.getItem(key)!)).toEqual({
    owner: "Ana",
    id,
  });
});

it("@s33 counts Unicode points with internal spaces and strips all lateral White_Space at the inclusive name boundary", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(new Response(null, { status: 503 }));
  stubApi(fetcher);
  const user = userEvent.setup();
  render(<IntegrationApi owner="Ana" />);
  const name = screen.getByRole("textbox", { name: "Nombre" });
  await user.click(screen.getByRole("checkbox", { name: "Leer proyectos" }));
  await user.click(name);
  await user.paste(
    "\u00a0\u0085" + "A".repeat(39) + " " + "😀".repeat(41) + "\u0085\u00a0",
  );
  expect(screen.getByRole("button", { name: "Crear" })).toBeDisabled();
  expect(fetcher).not.toHaveBeenCalled();
  await user.clear(name);
  const boundary =
    "\u00a0\u0085" + "A".repeat(39) + " " + "😀".repeat(40) + "\u0085\u00a0";
  await user.paste(boundary);
  expect(screen.getByRole("button", { name: "Crear" })).toBeEnabled();
  await user.click(screen.getByRole("button", { name: "Crear" }));
  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(JSON.parse(fetcher.mock.calls[0][1].body).name).toBe(boundary);
});

it("@s37 copies the complete one-time secret only on the clipboard gesture and announces success", async () => {
  const secret = `owp_${id}.${"A".repeat(43)}`;
  stubApi(
    vi.fn().mockResolvedValue(
      Response.json(
        {
          credential: {
            id,
            name: "Propia",
            scopes: ["projects:read"],
            createdAt: "2026-09-08T10:00:00Z",
            expiresAt: "2026-10-08T10:00:00Z",
            revokedAt: null,
          },
          secret,
        },
        { status: 201 },
      ),
    ),
  );
  const user = userEvent.setup();
  const write = vi.spyOn(navigator.clipboard, "writeText").mockResolvedValue();
  render(<IntegrationApi owner="Ana" />);
  await user.type(screen.getByRole("textbox", { name: "Nombre" }), "Propia");
  await user.click(screen.getByRole("checkbox", { name: "Leer proyectos" }));
  await user.click(screen.getByRole("button", { name: "Crear" }));
  const copy = await screen.findByRole("button", { name: "Copiar" });
  expect(write).not.toHaveBeenCalled();
  await user.click(copy);
  expect(write).toHaveBeenCalledExactlyOnceWith(secret);
  expect(await screen.findByText("Secreto copiado.")).toBeVisible();
  expect(sessionStorage.getItem(key)).toBeNull();
  expect(Object.values(sessionStorage).join(" ")).not.toContain(secret);
  expect(Object.values(localStorage).join(" ")).not.toContain(secret);
});

it.each([
  { status: 400, code: "API_CREDENTIAL_INVALID", mismatch: "status" },
  { status: 400, code: "API_CREDENTIAL_INVALID", mismatch: "type" },
  { status: 409, code: "API_CREDENTIAL_LIMIT", mismatch: "status" },
  { status: 409, code: "API_CREDENTIAL_LIMIT", mismatch: "type" },
  { status: 409, code: "API_CREDENTIAL_CONFLICT", mismatch: "status" },
  { status: 409, code: "API_CREDENTIAL_CONFLICT", mismatch: "type" },
])(
  "@s35 keeps uncertainty for $code with inconsistent $mismatch",
  async ({ status, code, mismatch }) => {
    const fetcher = vi.fn().mockResolvedValue(
      Response.json(
        {
          status: mismatch === "status" ? 200 : status,
          code,
          type:
            mismatch === "type"
              ? "urn:organization:problem:other"
              : `urn:organization:problem:${code.toLowerCase()}`,
        },
        { status },
      ),
    );
    stubApi(fetcher);
    const user = userEvent.setup();
    render(<IntegrationApi owner="Ana" />);
    await user.type(screen.getByRole("textbox", { name: "Nombre" }), "Propia");
    await user.click(screen.getByRole("checkbox", { name: "Leer proyectos" }));
    await user.click(screen.getByRole("button", { name: "Crear" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No podemos confirmar la creación",
    );
    expect(screen.getByRole("button", { name: "Crear" })).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Comprobar creación" }),
    ).toBeEnabled();
    expect(
      screen.queryByRole("button", {
        name: "Corregir y reenviar el mismo intento",
      }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Reenviar el mismo intento" }),
    ).not.toBeInTheDocument();
    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(JSON.parse(sessionStorage.getItem(key)!)).toEqual({
      owner: "Ana",
      id,
    });
  },
);
