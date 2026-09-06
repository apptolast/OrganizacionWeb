import { test as base, expect } from "@playwright/test";
import { loginSession } from "../../scripts/session-client.mjs";

export const test = base.extend({
  authenticated: [
    async ({ context }, use) => {
      await loginSession(context.request, {
        username: "e2e-user",
        password: "e2e-only-password",
      });
      await use(true);
    },
    { auto: true },
  ],
  request: async ({ context, authenticated }, use) => {
    if (!authenticated) throw new Error("Expected authenticated fixture");
    await use(context.request);
  },
});
export { expect };
