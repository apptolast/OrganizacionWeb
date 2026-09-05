import { defineConfig } from "@playwright/test";
import base from "../playwright.config.mjs";

export default defineConfig({
  ...base,
  testDir: ".",
  testMatch: ["read-projects.spec.mjs", "edit-project.spec.mjs"],
  grep: /real creation survives|real update persists/,
  projects: [
    { name: "firefox", use: { browserName: "firefox" } },
    { name: "webkit", use: { browserName: "webkit" } },
  ],
});
