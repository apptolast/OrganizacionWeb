import { defineConfig } from "@playwright/test";
import base from "../playwright.config.mjs";

export default defineConfig({
  ...base,
  testDir: ".",
  testMatch: [
    "read-projects.spec.mjs",
    "edit-project.spec.mjs",
    "project-states.spec.mjs",
    "authentication.spec.mjs",
    "create-task.spec.mjs",
    "split-task.spec.mjs",
  ],
  grep: /real creation survives|real update persists|explicit transitions persist|public access screen|confirmed task survives|nested children preserve/,
  projects: [
    { name: "firefox", use: { browserName: "firefox" } },
    { name: "webkit", use: { browserName: "webkit" } },
  ],
});
