import base from "../playwright.config.mjs";
export default {
  ...base,
  testDir: import.meta.dirname,
  testMatch: "**/integration-api-browser.spec.mjs",
  projects: ["chromium", "firefox", "webkit"].map((browserName) => ({
    name: browserName,
    use: { ...base.use, browserName },
  })),
};
