import base from "./stryker.project-states.config.json" with { type: "json" };
export default {
  ...base,
  mutate: [
    "src/project-status.ts:9:0-9:100",
    "src/use-project-status.ts:23:0-23:100",
    "src/use-project-status.ts:47:0-47:100",
    "src/use-read-projects.ts:31:0-31:100",
    "src/use-read-projects.ts:36:0-36:100",
  ],
  thresholds: { high: 90, low: 80, break: 80 },
  jsonReporter: {
    fileName: "reports/mutation-project-states-replay/mutation.json",
  },
  htmlReporter: {
    fileName: "reports/mutation-project-states-replay/mutation.html",
  },
};
