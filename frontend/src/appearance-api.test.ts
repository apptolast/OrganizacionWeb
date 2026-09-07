import { afterEach, expect, it, vi } from "vitest";
import { readAppearance, saveAppearance } from "./appearance-api";
import { observeAccess, setCsrfToken } from "./api-client";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
  setCsrfToken();
});
const defaults = {
  configured: false,
  theme: "SYSTEM",
  accentLight: "#244C3C",
  accentDark: "#B7E4C7",
  updatedAt: null,
};
const emptyTag = '"appearance:unconfigured"';
it("@s28 preserves a PUT conflict for manual recovery instead of confirming it", async () => {
  const response = Response.json(
    { code: "APPEARANCE_CONFLICT" },
    { status: 412 },
  );
  const fetcher = vi.fn().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetcher);
  await expect(
    saveAppearance(
      { theme: "DARK", accentLight: "#244C3C", accentDark: "#B7E4C7" },
      emptyTag,
    ),
  ).rejects.toBe(response);
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s28 refuses an absent resource as a successful PUT confirmation", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(defaults, { headers: { ETag: emptyTag } }),
      ),
  );
  await expect(
    saveAppearance(
      { theme: "SYSTEM", accentLight: "#244C3C", accentDark: "#B7E4C7" },
      emptyTag,
    ),
  ).rejects.toThrow("Confirmación de apariencia incompatible");
});
it("@s19 compares confirmation with the captured input despite caller edits during the request", async () => {
  let deliver!: (response: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockReturnValue(
      new Promise<Response>((resolve) => {
        deliver = resolve;
      }),
    ),
  );
  const input = {
    theme: "DARK" as const,
    accentLight: "#244C3C",
    accentDark: "#B7E4C7",
  };
  const pending = saveAppearance(input, emptyTag);
  input.accentLight = "#0000FF";
  deliver(Response.json(configured, { headers: { ETag: configuredTag } }));
  await expect(pending).resolves.toEqual({
    ...configured,
    etag: configuredTag,
  });
});
it("@s19 rejects a valid confirmation for a different dark accent", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, accentDark: "#00FFFF" },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(
    saveAppearance(
      { theme: "DARK", accentLight: "#244C3C", accentDark: "#B7E4C7" },
      emptyTag,
    ),
  ).rejects.toThrow("Confirmación de apariencia incompatible");
});
it("@s19 rejects a valid confirmation for a different light accent", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, accentLight: "#0000FF" },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(
    saveAppearance(
      { theme: "DARK", accentLight: "#244C3C", accentDark: "#B7E4C7" },
      emptyTag,
    ),
  ).rejects.toThrow("Confirmación de apariencia incompatible");
});
it("@s18 rejects the appearance resource with a missing field", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          theme: "DARK",
          accentLight: "#244C3C",
          updatedAt: configured.updatedAt,
        },
        { headers: { ETag: configuredTag } },
      ),
    ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s18 rejects a dark color encoded as a number", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, accentDark: 123456 },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s18 rejects noncanonical lowercase colors in a resource", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, accentDark: "#b7e4c7" },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s18 rejects a configured snapshot without its timestamp", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, updatedAt: null },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s18 rejects an unconfigured snapshot timestamp", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...defaults, updatedAt: configured.updatedAt },
          { headers: { ETag: emptyTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s18 rejects a weak configured appearance ETag", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(configured, { headers: { ETag: `W/${configuredTag}` } }),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
const configured = {
  ...defaults,
  configured: true,
  theme: "DARK",
  updatedAt: "2026-09-07T18:00:00.123456Z",
};
const configuredTag =
  '"appearance:12345678-1234-1234-1234-123456789abc:9223372036854775807"';
it("@s19 rejects a valid confirmation for a different theme intention", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, theme: "LIGHT" },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(
    saveAppearance(
      { theme: "DARK", accentLight: "#244C3C", accentDark: "#B7E4C7" },
      emptyTag,
    ),
  ).rejects.toThrow("Confirmación de apariencia incompatible");
});
it("@s2 saves exactly three preferences with revision and session CSRF", async () => {
  const input = {
    theme: "DARK" as const,
    accentLight: "#0000ff",
    accentDark: "#00ffff",
  };
  const value = {
    ...configured,
    accentLight: "#0000FF",
    accentDark: "#00FFFF",
  };
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      Response.json(value, { headers: { ETag: configuredTag } }),
    );
  vi.stubGlobal("fetch", fetcher);
  setCsrfToken("csrf-test");
  const signal = new AbortController().signal;
  await expect(saveAppearance(input, emptyTag, signal)).resolves.toEqual({
    ...value,
    etag: configuredTag,
  });
  expect(fetcher).toHaveBeenCalledTimes(1);
  const [url, request] = fetcher.mock.calls[0];
  expect(url).toBe("/api/v1/me/appearance");
  expect(request).toMatchObject({
    method: "PUT",
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  expect(JSON.parse(request.body)).toEqual(input);
  const headers = new Headers(request.headers);
  expect(headers.get("If-Match")).toBe(emptyTag);
  expect(headers.get("Content-Type")).toBe("application/json");
  expect(headers.get("X-CSRF-TOKEN")).toBe("csrf-test");
});
it("@s31 discards JSON that finishes after the read was aborted", async () => {
  let deliver!: (value: unknown) => void;
  const response = Response.json(defaults, { headers: { ETag: emptyTag } });
  const entered = new Promise<void>((ready) => {
    vi.spyOn(response, "json").mockImplementation(() => {
      ready();
      return new Promise((resolve) => {
        deliver = resolve;
      });
    });
  });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  const controller = new AbortController();
  const pending = readAppearance(controller.signal);
  const assertion = expect(pending).rejects.toMatchObject({
    name: "AbortError",
  });
  await entered;
  controller.abort();
  deliver(defaults);
  await assertion;
});
it.each(["GET", "PUT"])(
  "@s31 discards a late HTTP401 from %s before it can revoke current access",
  async (method) => {
    let deliver!: (response: Response) => void;
    vi.stubGlobal(
      "fetch",
      vi.fn().mockReturnValue(
        new Promise<Response>((resolve) => {
          deliver = resolve;
        }),
      ),
    );
    const observer = vi.fn();
    observeAccess(observer);
    const controller = new AbortController();
    const pending =
      method === "GET"
        ? readAppearance(controller.signal)
        : saveAppearance(
            { theme: "DARK", accentLight: "#244C3C", accentDark: "#B7E4C7" },
            emptyTag,
            controller.signal,
          );
    const assertion = expect(pending).rejects.toMatchObject({
      name: "AbortError",
    });
    controller.abort();
    deliver(new Response(null, { status: 401 }));
    await assertion;
    expect(observer).not.toHaveBeenCalled();
  },
);
it("@s2 accepts arbitrary accessible accents rather than only defaults", async () => {
  const value = {
    ...configured,
    accentLight: "#0000FF",
    accentDark: "#00FFFF",
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(value, { headers: { ETag: configuredTag } }),
      ),
  );
  await expect(readAppearance()).resolves.toEqual({
    ...value,
    etag: configuredTag,
  });
});
it("@s11 does not round a contrast below the required threshold upward", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, accentLight: "#645F61" },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s11 rejects a color legible on white but not on the light hover surface", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, accentLight: "#666666" },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s11 rejects insufficient dark contrast even while LIGHT is selected", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, theme: "LIGHT", accentDark: "#000000" },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s11 rejects insufficient light contrast even while DARK is selected", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, accentLight: "#FFFFFF" },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s18 rejects a configured revision beyond BIGINT before applying it", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(configured, {
        headers: {
          ETag: configuredTag.replace(
            "9223372036854775807",
            "9223372036854775808",
          ),
        },
      }),
    ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s18 rejects a configured timestamp beyond microsecond precision", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, updatedAt: "2026-09-07T18:00:00.1234567Z" },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s18 rejects an unknown configured theme", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, theme: "SEPIA" },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s18 rejects a string configured flag", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...configured, configured: "true" },
          { headers: { ETag: configuredTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s2 reads configured appearance with an exact textual BIGINT revision", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(configured, { headers: { ETag: configuredTag } }),
      ),
  );
  await expect(readAppearance()).resolves.toEqual({
    ...configured,
    etag: configuredTag,
  });
});
it("@s18 rejects an unconfigured snapshot with non-default values", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...defaults, theme: "DARK" },
          { headers: { ETag: emptyTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s18 rejects an extra field in a successful appearance resource", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...defaults, extra: true },
          { headers: { ETag: emptyTag } },
        ),
      ),
  );
  await expect(readAppearance()).rejects.toThrow(
    "Respuesta de apariencia inválida",
  );
});
it("@s1 reads the private default snapshot and its strong revision without writing", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      Response.json(defaults, { headers: { ETag: emptyTag } }),
    );
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  await expect(readAppearance(signal)).resolves.toEqual({
    ...defaults,
    etag: emptyTag,
  });
  expect(fetcher).toHaveBeenCalledExactlyOnceWith("/api/v1/me/appearance", {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
});
it("@s16 preserves a storage failure instead of treating it as default appearance", async () => {
  const response = Response.json(defaults, {
    status: 503,
    headers: { ETag: emptyTag },
  });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readAppearance()).rejects.toBe(response);
});
