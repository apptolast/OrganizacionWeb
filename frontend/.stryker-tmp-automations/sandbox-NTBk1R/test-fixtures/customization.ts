// @ts-nocheck
import { vi } from "vitest";

// Only these legacy suites simulate feature21 reads separately. Business
// responses, errors and call counts remain owned by their original mocks.
export function customizationFixtureResponse(
  input: RequestInfo | URL,
  init?: RequestInit,
): Response | undefined {
  if (
    typeof input !== "string" ||
    (init?.method !== undefined && init.method !== "GET")
  )
    return;
  const scopeMatch = /^\/api\/v1\/me\/customization\/(PROJECT|TASK)$/.exec(
    input,
  );
  const scope = scopeMatch?.[0] === input ? scopeMatch[1] : undefined;
  if (scope)
    return Response.json(
      {
        configured: false,
        visibleFields:
          scope === "PROJECT"
            ? ["createdAt"]
            : ["completionCriterion", "estimatedMinutes"],
        customFields: [],
        updatedAt: null,
      },
      { headers: { ETag: `"customization:${scope}:unconfigured"` } },
    );
  const uuid = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  const match = new RegExp(
    `^/api/v1/projects/(${uuid})(?:/tasks/(${uuid}))?/custom-fields$`,
  ).exec(input);
  if (match && match[0] === input)
    return Response.json(
      { configured: false, values: [], updatedAt: null },
      {
        headers: {
          ETag: `"custom-values:${match[2] ? "TASK" : "PROJECT"}:${match[2] ?? match[1]}:schema:unconfigured:values:unconfigured"`,
        },
      },
    );
}

export function mockLegacyCustomizationFetch() {
  const business = vi.fn<typeof fetch>(globalThis.fetch);
  vi.spyOn(globalThis, "fetch").mockImplementation((input, init) => {
    const response = customizationFixtureResponse(input, init);
    return response ? Promise.resolve(response) : business(input, init);
  });
  return business;
}
