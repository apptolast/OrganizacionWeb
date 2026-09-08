import base from "../playwright.config.mjs";

export default {
  ...base,
  testDir: import.meta.dirname,
  testMatch: "**/customization-ux.spec.mjs",
  projects: ["chromium", "firefox", "webkit"].map((browserName) => ({
    name: browserName,
    use: { ...base.use, browserName },
  })),
};
