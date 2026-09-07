import { test as base, expect } from "@playwright/test";
import { loginSession } from "../../scripts/session-client.mjs";
import { sql } from "./projects.mjs";

function clearOwnCustomization() {
  sql(
    "DELETE FROM task_custom_field_values WHERE owner_id='e2e-user'; DELETE FROM project_custom_field_values WHERE owner_id='e2e-user'; DELETE FROM customization_preferences WHERE owner_id='e2e-user'",
  );
}

export const test = base.extend({
  authenticated: [
    async ({ context }, use) => {
      clearOwnCustomization();
      try {
        await loginSession(context.request, {
          username: "e2e-user",
          password: "e2e-only-password",
        });
        await use(true);
      } finally {
        clearOwnCustomization();
      }
    },
    { auto: true },
  ],
  request: async ({ context, authenticated }, use) => {
    if (!authenticated) throw new Error("Expected authenticated fixture");
    await use(context.request);
  },
});
export { expect };
