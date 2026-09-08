import { render, screen, fireEvent, act, within } from "@testing-library/react";
import { App } from "./App";
import { SessionGate } from "./session-gate";
import { useLayoutEffect } from "react";
import { afterEach, expect, it, vi } from "vitest";
import { ExportData } from "./export-data";
import userEvent from "@testing-library/user-event";

const names =
  "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues".split(
    " ",
  );
const payload =
  JSON.stringify(
    {
      format: "organizationweb-export",
      schemaVersion: 1,
      exportedAt: "2026-09-08T10:20:30.123456Z",
      owner: "ana",
      data: Object.fromEntries(names.map((name) => [name, []])),
      counts: Object.fromEntries(names.map((name) => [name, 0])),
    },
    null,
    2,
  ) + "\n";
const fileName = "organizationweb-export-v1-20260908T102030123456Z.json";
function archiveResponse() {
  return new Response(new TextEncoder().encode(payload), {
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Content-Length": String(new TextEncoder().encode(payload).length),
      "Content-Disposition": `attachment; filename="${fileName}"`,
    },
  });
}
const session = {
  authenticated: true,
  username: "ana",
  csrfToken: "private-token",
  csrfHeaderName: "X-CSRF-TOKEN",
};
function appearanceResponse() {
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

afterEach(() => {
  vi.unstubAllGlobals();
  window.history.replaceState(null, "", "/");
});
it("@s29 ignores a rejected response body resolved after cancellation", async () => {
  let finish!: (value: unknown) => void;
  const rejected = Response.json({ code: "EXPORT_TOO_LARGE" }, { status: 413 });
  const json = vi.spyOn(rejected, "json").mockReturnValue(
    new Promise((resolve) => {
      finish = resolve;
    }),
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(rejected));
  render(<ExportData owner="ana" />);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  expect(json).toHaveBeenCalledOnce();
  await userEvent.click(
    screen.getByRole("button", { name: "Cancelar preparación" }),
  );
  await act(async () => {
    finish({ code: "EXPORT_TOO_LARGE" });
  });
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Preparar exportación" }),
  ).toBeEnabled();
});
it("@s26 reuses the native download without fetching or creating another file", async () => {
  const create = vi.fn().mockReturnValue("blob:reuse");
  vi.stubGlobal(
    "URL",
    class extends URL {
      static createObjectURL = create;
      static revokeObjectURL = vi.fn();
    },
  );
  const fetcher = vi.fn().mockResolvedValue(archiveResponse());
  vi.stubGlobal("fetch", fetcher);
  render(<ExportData owner="ana" />);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  const link = await screen.findByRole("link", {
    name: "Descargar archivo JSON",
  });
  const native = vi.fn((event: Event) => {
    expect(event.defaultPrevented).toBe(false);
    event.preventDefault();
  });
  link.addEventListener("click", native);
  await userEvent.click(link);
  await userEvent.click(link);
  expect(native).toHaveBeenCalledTimes(2);
  expect(fetcher).toHaveBeenCalledOnce();
  expect(create).toHaveBeenCalledOnce();
  expect(link).toHaveAttribute("href", "blob:reuse");
});
it("@s25 never creates a downloadable Blob from an incompatible response", async () => {
  const file = archiveResponse();
  file.headers.set("Content-Length", "0");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  const create = vi.fn();
  vi.stubGlobal(
    "URL",
    class extends URL {
      static createObjectURL = create;
    },
  );
  render(<ExportData owner="ana" />);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  expect(await screen.findByRole("alert")).toBeVisible();
  expect(create).not.toHaveBeenCalled();
  expect(
    screen.queryByRole("link", { name: "Descargar archivo JSON" }),
  ).not.toBeInTheDocument();
});
it("@s28 removes access through SessionGate for a current export 401", async () => {
  window.history.replaceState(null, "", "/exportacion");
  let authenticated = true;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((url: string) => {
      if (url === "/api/session")
        return Promise.resolve(
          Response.json({
            ...session,
            authenticated,
            username: authenticated ? "ana" : null,
          }),
        );
      if (url === "/api/v1/me/appearance")
        return Promise.resolve(appearanceResponse());
      if (url === "/api/v1/me/export") {
        authenticated = false;
        return Promise.resolve(
          Response.json({ code: "UNAUTHENTICATED" }, { status: 401 }),
        );
      }
      throw new Error(`Unexpected request ${url}`);
    }),
  );
  render(<SessionGate />);
  await userEvent.click(
    await screen.findByRole("button", { name: "Preparar exportación" }),
  );
  expect(await screen.findByLabelText("Usuario")).toBeVisible();
  expect(
    screen.queryByRole("heading", { name: "Exportar mis datos" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("link", { name: "Descargar archivo JSON" }),
  ).not.toBeInTheDocument();
});
it("@s31 retains the initiating control when it remains focused through preparation", async () => {
  let finish!: (value: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockReturnValue(
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
    ),
  );
  vi.stubGlobal(
    "URL",
    class extends URL {
      static createObjectURL = vi.fn().mockReturnValue("blob:focus");
      static revokeObjectURL = vi.fn();
    },
  );
  render(<ExportData owner="ana" />);
  const prepare = screen.getByRole("button", { name: "Preparar exportación" });
  await userEvent.click(prepare);
  expect(prepare).toBeDisabled();
  expect(prepare).toHaveFocus();
  await act(async () => {
    finish(archiveResponse());
  });
  expect(
    screen.getByRole("button", { name: "Preparar de nuevo" }),
  ).toHaveFocus();
});
it("@s29 keeps Bruno's session when Ana's cancelled export returns a late 401 after logout", async () => {
  window.history.replaceState(null, "", "/exportacion");
  let current: string | null = "ana";
  let finish!: (value: Response) => void;
  const fetcher = vi
    .fn()
    .mockImplementation((url: string, options?: RequestInit) => {
      if (url === "/api/session/logout") {
        current = null;
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (url === "/api/session" && options?.method === "POST") {
        current = "bruno";
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (url === "/api/session")
        return Promise.resolve(
          Response.json({
            ...session,
            authenticated: current !== null,
            username: current,
          }),
        );
      if (url === "/api/v1/me/appearance")
        return Promise.resolve(appearanceResponse());
      if (url === "/api/v1/me/export")
        return new Promise<Response>((resolve) => {
          finish = resolve;
        });
      throw new Error(`Unexpected request ${url}`);
    });
  vi.stubGlobal("fetch", fetcher);
  render(<SessionGate />);
  await userEvent.click(
    await screen.findByRole("button", { name: "Preparar exportación" }),
  );
  const signal = fetcher.mock.calls.find(
    ([url]) => url === "/api/v1/me/export",
  )![1].signal as AbortSignal;
  await userEvent.click(screen.getByRole("button", { name: "Cerrar sesión" }));
  expect(signal.aborted).toBe(true);
  await userEvent.type(await screen.findByLabelText("Usuario"), "bruno");
  await userEvent.type(screen.getByLabelText("Contraseña"), "test-password");
  await userEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));
  await screen.findByText(/datos de bruno/);
  const calls = fetcher.mock.calls.length;
  await act(async () => {
    finish(Response.json({ code: "UNAUTHENTICATED" }, { status: 401 }));
  });
  expect(screen.getByText(/datos de bruno/)).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(calls);
  expect(
    screen.queryByRole("link", { name: "Descargar archivo JSON" }),
  ).not.toBeInTheDocument();
});
it("@s31 does not restore focus after a voluntary move followed by blur to the body", async () => {
  let finish!: (value: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockReturnValue(
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
    ),
  );
  render(
    <>
      <button>Otro destino</button>
      <ExportData owner="ana" />
    </>,
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  const other = screen.getByRole("button", { name: "Otro destino" });
  other.focus();
  other.blur();
  await act(async () => {
    finish(Response.json({ code: "EXPORT_TOO_LARGE" }, { status: 413 }));
  });
  expect(screen.getByRole("alert")).toBeVisible();
  expect(document.body).toHaveFocus();
});
it("@s31 restores focus when a limit response removes the preparation initiator", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ code: "EXPORT_TOO_LARGE" }, { status: 413 }),
      ),
  );
  render(<ExportData owner="ana" />);
  const prepare = screen.getByRole("button", { name: "Preparar exportación" });
  prepare.focus();
  await userEvent.keyboard("{Enter}");
  await screen.findByRole("alert");
  expect(
    screen.getByRole("heading", { level: 1, name: "Exportar mis datos" }),
  ).toHaveFocus();
});
it("@s31 restores logical focus when the cancel initiator disappears", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockReturnValue(new Promise<Response>(() => {})),
  );
  render(<ExportData owner="ana" />);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  const cancel = screen.getByRole("button", { name: "Cancelar preparación" });
  cancel.focus();
  await userEvent.keyboard("{Enter}");
  expect(
    screen.queryByRole("button", { name: "Cancelar preparación" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("heading", { level: 1, name: "Exportar mis datos" }),
  ).toHaveFocus();
});
it("@s22 restores the export route after signing in without preparing automatically", async () => {
  window.history.replaceState(null, "", "/exportacion");
  let authenticated = false;
  const fetcher = vi
    .fn()
    .mockImplementation((url: string, options?: RequestInit) => {
      if (url === "/api/session" && options?.method === "POST") {
        authenticated = true;
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (url === "/api/session")
        return Promise.resolve(
          Response.json(
            authenticated
              ? session
              : { ...session, authenticated: false, username: null },
          ),
        );
      if (url === "/api/v1/me/appearance")
        return Promise.resolve(appearanceResponse());
      throw new Error(`Unexpected request ${url}`);
    });
  vi.stubGlobal("fetch", fetcher);
  render(<SessionGate />);
  await userEvent.type(await screen.findByLabelText("Usuario"), "ana");
  await userEvent.type(
    screen.getByLabelText("Contraseña"),
    "private-test-password",
  );
  fireEvent.submit(screen.getByLabelText("Usuario").closest("form")!);
  expect(
    await screen.findByRole("heading", {
      name: "Exportar mis datos",
      level: 1,
    }),
  ).toBeVisible();
  expect(window.location.pathname).toBe("/exportacion");
  expect(
    fetcher.mock.calls.filter(([url]) => url === "/api/v1/me/export"),
  ).toHaveLength(0);
});
it("@s22 @s24 obtains the export identity from the real authenticated session", async () => {
  window.history.replaceState(null, "", "/exportacion");
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url === "/api/session") return Promise.resolve(Response.json(session));
    if (url === "/api/v1/me/appearance")
      return Promise.resolve(appearanceResponse());
    if (url === "/api/v1/me/export") return Promise.resolve(archiveResponse());
    throw new Error(`Unexpected request ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  vi.stubGlobal(
    "URL",
    class extends URL {
      static createObjectURL = vi.fn().mockReturnValue("blob:session");
      static revokeObjectURL = vi.fn();
    },
  );
  render(<SessionGate />);
  await screen.findByRole("heading", { level: 1, name: "Exportar mis datos" });
  expect(
    fetcher.mock.calls.filter(([url]) => url === "/api/v1/me/export"),
  ).toHaveLength(0);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  expect(
    await screen.findByRole("link", { name: "Descargar archivo JSON" }),
  ).toHaveAttribute("href", "blob:session");
});
it("@s22 opens Exportación from the final navigation link without fetching an archive", async () => {
  window.history.replaceState(null, "", "/no-existe");
  const fetcher = vi.fn();
  vi.stubGlobal("fetch", fetcher);
  render(<App username="ana" />);
  const navigation = screen.getByRole("navigation", { name: "Principal" });
  const links = within(navigation).getAllByRole("link");
  expect(links[0]).toHaveAccessibleName("Hoy");
  const link = within(navigation).getByRole("link", { name: "Exportación" });
  expect(links.at(-1)).toBe(link);
  await userEvent.click(link);
  expect(window.location.pathname).toBe("/exportacion");
  expect(
    screen.getByRole("heading", { name: "Exportar mis datos", level: 1 }),
  ).toBeVisible();
  expect(
    screen.getByRole("heading", { name: "Exportar mis datos", level: 1 }),
  ).toHaveFocus();
  expect(link).toHaveAttribute("aria-current", "page");
  expect(screen.getByText(/JSON versionado/)).toBeVisible();
  expect(screen.getByText(/archivo personal/)).toBeVisible();
  expect(screen.getByText(/importación aún no está disponible/)).toBeVisible();
  expect(fetcher).not.toHaveBeenCalled();
});
it("@s28 explains an oversized export without offering an indefinite retry", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      Response.json({ code: "EXPORT_TOO_LARGE" }, { status: 413 }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<ExportData owner="ana" />);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "100000 registros o 32 MiB",
  );
  expect(
    screen.queryByRole("button", { name: "Reintentar preparación" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("link", { name: "Descargar archivo JSON" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledOnce();
});
it("@s29 removes a previous owner's prepared file when the authenticated identity changes", async () => {
  const revoke = vi.fn();
  const revokedDuringLayout: number[] = [];
  function Probe({ owner }: { owner: string }) {
    useLayoutEffect(() => {
      if (owner === "bruno") revokedDuringLayout.push(revoke.mock.calls.length);
    }, [owner]);
    return null;
  }
  vi.stubGlobal(
    "URL",
    class extends URL {
      static createObjectURL = vi.fn().mockReturnValue("blob:ana");
      static revokeObjectURL = revoke;
    },
  );
  const fetcher = vi.fn().mockResolvedValue(archiveResponse());
  vi.stubGlobal("fetch", fetcher);
  const view = render(
    <>
      <ExportData owner="ana" />
      <Probe owner="ana" />
    </>,
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  await screen.findByRole("link", { name: "Descargar archivo JSON" });
  view.rerender(
    <>
      <ExportData owner="bruno" />
      <Probe owner="bruno" />
    </>,
  );
  expect(revokedDuringLayout).toEqual([1]);
  expect(
    screen.queryByRole("link", { name: "Descargar archivo JSON" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByText(/datos de ana/)).not.toBeInTheDocument();
  expect(revoke).toHaveBeenCalledExactlyOnceWith("blob:ana");
  expect(fetcher).toHaveBeenCalledOnce();
});
it("@s27 withdraws the previous file immediately before preparing a fresh snapshot", async () => {
  const revoke = vi.fn();
  vi.stubGlobal(
    "URL",
    class extends URL {
      static createObjectURL = vi.fn().mockReturnValue("blob:old");
      static revokeObjectURL = revoke;
    },
  );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(archiveResponse())
    .mockReturnValueOnce(new Promise<Response>(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<ExportData owner="ana" />);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  await screen.findByRole("link", { name: "Descargar archivo JSON" });
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar de nuevo" }),
  );
  expect(
    screen.queryByRole("link", { name: "Descargar archivo JSON" }),
  ).not.toBeInTheDocument();
  expect(revoke).toHaveBeenCalledExactlyOnceWith("blob:old");
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Preparando exportación…",
  );
});
it("@s29 revokes the prepared object URL when leaving the view", async () => {
  const revoke = vi.fn();
  vi.stubGlobal(
    "URL",
    class extends URL {
      static createObjectURL = vi.fn().mockReturnValue("blob:private");
      static revokeObjectURL = revoke;
    },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(archiveResponse()));
  const view = render(<ExportData owner="ana" />);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  await screen.findByRole("link", { name: "Descargar archivo JSON" });
  view.unmount();
  expect(revoke).toHaveBeenCalledExactlyOnceWith("blob:private");
});
it("@s29 aborts the request on leaving the view before an old response can publish bytes", async () => {
  let finish!: (value: Response) => void;
  const fetcher = vi.fn().mockReturnValue(
    new Promise<Response>((resolve) => {
      finish = resolve;
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  const create = vi.fn();
  vi.stubGlobal(
    "URL",
    class extends URL {
      static createObjectURL = create;
    },
  );
  const view = render(<ExportData owner="ana" />);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  const signal = fetcher.mock.calls[0][1].signal as AbortSignal;
  view.unmount();
  expect(signal.aborted).toBe(true);
  await act(async () => {
    finish(archiveResponse());
  });
  expect(create).not.toHaveBeenCalled();
});
it("@s28 reports temporary failure and retries only after a new deliberate gesture", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      new Response(JSON.stringify({ code: "SESSION_UNAVAILABLE" }), {
        status: 503,
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<ExportData owner="ana" />);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pudo preparar la exportación",
  );
  expect(fetcher).toHaveBeenCalledOnce();
  await userEvent.click(
    screen.getByRole("button", { name: "Reintentar preparación" }),
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pudo preparar la exportación",
  );
});
it("@s29 cancels preparation and ignores its late complete response", async () => {
  let finish!: (value: Response) => void;
  const fetcher = vi.fn().mockReturnValue(
    new Promise<Response>((resolve) => {
      finish = resolve;
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  const create = vi.fn();
  vi.stubGlobal(
    "URL",
    class extends URL {
      static createObjectURL = create;
    },
  );
  render(<ExportData owner="ana" />);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  const signal = fetcher.mock.calls[0][1].signal as AbortSignal;
  await userEvent.click(
    screen.getByRole("button", { name: "Cancelar preparación" }),
  );
  expect(signal.aborted).toBe(true);
  expect(
    screen.getByRole("button", { name: "Preparar exportación" }),
  ).toBeEnabled();
  await act(async () => {
    finish(archiveResponse());
  });
  expect(create).not.toHaveBeenCalled();
  expect(
    screen.queryByRole("link", { name: "Descargar archivo JSON" }),
  ).not.toBeInTheDocument();
});
it("@s23 synchronously blocks repeated activation while announcing preparation", () => {
  const fetcher = vi.fn().mockReturnValue(new Promise<Response>(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<ExportData owner="ana" />);
  const prepare = screen.getByRole("button", { name: "Preparar exportación" });
  fireEvent.click(prepare);
  fireEvent.click(prepare);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Preparando exportación…",
  );
  expect(prepare).toBeDisabled();
  expect(fetcher).toHaveBeenCalledOnce();
  expect(
    screen.getByRole("button", { name: "Cancelar preparación" }),
  ).toBeEnabled();
});

it("@s24 prepares original bytes and offers a native download for a second deliberate gesture", async () => {
  const create = vi.fn().mockReturnValue("blob:prepared");
  vi.stubGlobal(
    "URL",
    class extends URL {
      static createObjectURL = create;
      static revokeObjectURL = vi.fn();
    },
  );
  const fetcher = vi.fn().mockResolvedValue(archiveResponse());
  vi.stubGlobal("fetch", fetcher);
  render(<ExportData owner="ana" />);
  await userEvent.click(
    screen.getByRole("button", { name: "Preparar exportación" }),
  );
  const download = await screen.findByRole("link", {
    name: "Descargar archivo JSON",
  });
  expect(download.tagName).toBe("A");
  expect(download).toHaveAttribute("href", "blob:prepared");
  expect(download).toHaveAttribute("download", fileName);
  expect(screen.getByRole("status")).toHaveTextContent("Archivo preparado");
  expect(create).toHaveBeenCalledOnce();
  const blob = create.mock.calls[0][0] as Blob;
  expect(blob.type).toBe("application/json;charset=utf-8");
  const read = new FileReader();
  const result = new Promise<ArrayBuffer>((resolve) => {
    read.onload = () => resolve(read.result as ArrayBuffer);
  });
  read.readAsArrayBuffer(blob);
  expect(Array.from(new Uint8Array(await result))).toEqual(
    Array.from(new TextEncoder().encode(payload)),
  );
  expect(fetcher).toHaveBeenCalledOnce();
});

it("@s22 opens export without requesting or modifying private data", () => {
  const fetcher = vi.fn();
  vi.stubGlobal("fetch", fetcher);
  render(<ExportData owner="ana" />);
  expect(
    screen.getByRole("heading", { level: 1, name: "Exportar mis datos" }),
  ).toBeVisible();
  expect(
    screen.getByRole("button", { name: "Preparar exportación" }),
  ).toBeEnabled();
  expect(
    screen.queryByRole("link", { name: "Descargar archivo JSON" }),
  ).not.toBeInTheDocument();
  expect(fetcher).not.toHaveBeenCalled();
});
