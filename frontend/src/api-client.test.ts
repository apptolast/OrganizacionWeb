import { afterEach, expect, it, vi } from "vitest";
import { apiRequest, setCsrfToken, observeAccess } from "./api-client";
afterEach(() => {
  setCsrfToken();
  observeAccess();
});
it("@s9 sustituye el token previo y conserva cabeceras nativas sin duplicados", async () => {
  setCsrfToken("current");
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(new Response(null, { status: 204 }));
  await apiRequest("/api/v1/projects/example", {
    method: "PUT",
    headers: new Headers({ "x-csrf-token": "old", "If-Match": '"version"' }),
  });
  const headers = new Headers(fetcher.mock.calls[0][1]?.headers);
  expect(headers.get("X-CSRF-TOKEN")).toBe("current");
  expect(headers.get("If-Match")).toBe('"version"');
});
it.each([401, 403])(
  "@s14 la respuesta HTTP %i abortada no notifica al gate",
  async (status) => {
    const controller = new AbortController();
    const notify = vi.fn();
    observeAccess(notify);
    let finish!: (response: Response) => void;
    vi.spyOn(globalThis, "fetch").mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
    const pending = apiRequest("/api/v1/projects", {
      signal: controller.signal,
    });
    controller.abort();
    finish(Response.json({ code: "CSRF_INVALID" }, { status }));
    await pending;
    expect(notify).not.toHaveBeenCalled();
  },
);
it.each([{ code: "UNTRUSTED_ORIGIN" }, null, 42, { code: 403 }])(
  "@s11 un rechazo ajeno a CSRF no renueva token %j",
  async (body) => {
    const notify = vi.fn();
    observeAccess(notify);
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(body, { status: 403 }),
    );
    await apiRequest("/api/v1/projects");
    expect(notify).not.toHaveBeenCalled();
  },
);
it("@s9 GET no expone el token CSRF de escritura", async () => {
  setCsrfToken("private");
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(new Response(null, { status: 200 }));
  await apiRequest("/api/v1/projects", {
    method: "GET",
    headers: [["Accept", "application/json"]],
  });
  const headers = new Headers(fetcher.mock.calls[0][1]?.headers);
  expect(headers.has("X-CSRF-TOKEN")).toBe(false);
  expect(headers.get("Accept")).toBe("application/json");
});
it.each([true, false])(
  "@s17 CSRF reconocido sin AbortSignal y listener presente %s",
  async (listening) => {
    const notify = vi.fn();
    if (listening) observeAccess(notify);
    const response = Response.json({ code: "CSRF_INVALID" }, { status: 403 });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    expect(await apiRequest("/api/v1/projects")).toBe(response);
    if (listening) expect(notify).toHaveBeenCalledExactlyOnceWith(403);
  },
);
it("@s17 un cuerpo CSRF en HTTP 503 no se interpreta como rechazo de token", async () => {
  const notify = vi.fn();
  observeAccess(notify);
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json({ code: "CSRF_INVALID" }, { status: 503 }),
  );
  await apiRequest("/api/v1/projects");
  expect(notify).not.toHaveBeenCalled();
});
