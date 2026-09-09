import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { isAbsolute, resolve } from "node:path";
const root = fileURLToPath(new URL("../", import.meta.url));
export function run(command, args, options = {}) {
  const windowsBatch =
    process.platform === "win32" && /(?:pnpm|gradlew)$/.test(command);
  // cmd.exe no busca .bat en el directorio de trabajo cuando
  // NoDefaultCurrentDirectoryInExePath está definida: resolver ruta absoluta.
  const localBatch =
    process.platform === "win32" &&
    /\.bat$/.test(command) &&
    !isAbsolute(command) &&
    options.cwd !== undefined;
  const executable = windowsBatch
    ? `${command}.cmd`
    : localBatch
      ? resolve(options.cwd, command)
      : command;
  const result = spawnSync(executable, args, {
    cwd: root,
    stdio: "inherit",
    shell: windowsBatch,
    ...options,
  });
  if (result.error) throw result.error;
  if (result.status !== 0)
    throw new Error(`${command} exited with ${result.status}`);
}
export function createProject(runner = run) {
  return function project(task, target) {
    if (
      target !== undefined &&
      target !== "" &&
      (task !== "mutate" ||
        ![
          "integration_api-frontend",
          "integration_api-backend",
          "integration_api-http-backend",
          "ics_calendar-frontend",
          "ics_calendar-backend",
          "github_connector-frontend",
          "github_connector-backend",
          "import_data-frontend",
          "import_data-reader-backend",
          "import_data-http-backend",
          "import_data-persistence-backend",
          "export_data-frontend",
          "custom_views_fields-backend",
          "custom_views_fields-frontend",
          "appearance-backend",
          "appearance-frontend",
          "weekly_review-backend",
          "weekly_review-frontend",
          "history-backend",
          "history-frontend",
          "end_time_notification-backend",
          "end_time_notification-frontend",
          "close_work_session-backend",
          "close_work_session-frontend",
          "pause_resume_session-backend",
          "pause_resume_session-frontend",
          "start_work_session-frontend",
          "start_work_session-backend",
          "start_work_session-backend-replay",
          "reschedule-frontend",
          "reschedule-backend",
          "today-backend",
          "today-frontend",
          "today-frontend-replay",
          "today-frontend-final",
          "schedule_block-backend",
          "schedule_block-frontend",
          "schedule_block-frontend-replay",
          "export_data-persistence-backend",
          "external_calendar-backend",
          "external_calendar-frontend",
          "automations-backend",
          "automations-frontend",
          "webhooks-backend",
          "webhooks-frontend",
        ].includes(target))
    ) {
      throw new Error(`Invalid target: ${target}`);
    }
    const backend = (taskName, args = []) =>
      runner(
        process.platform === "win32" ? "gradlew.bat" : "./gradlew",
        [taskName, "--no-daemon", ...args],
        { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
      );
    if (task === "mutate" && target === "automations-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.automations.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "automations-backend") {
      backend("pitest", ["-PmutationScope=automations"]);
      return;
    }
    if (task === "mutate" && target === "github_connector-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.github-connector.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "appearance-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.appearance.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "integration_api-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.integration-api.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "import_data-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.import-data.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "ics_calendar-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.ics-calendar.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "ics_calendar-backend") {
      backend("pitest", ["-PmutationScope=ics_calendar"]);
      return;
    }
    if (task === "mutate" && target === "export_data-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.export-data.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "custom_views_fields-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.custom-views-fields.config.json",
      ]);
      return;
    }
    if (task === "install") {
      runner("pnpm", ["install", "--frozen-lockfile"]);
      runner("pnpm", ["--dir", "frontend", "install", "--frozen-lockfile"]);
      return;
    }
    const commands = {
      test: "test",
      build: "bootJar",
      lint: "spotlessCheck",
      mutate: "pitest",
    };
    if (!commands[task]) throw new Error(`Unknown task: ${task}`);
    if (task === "mutate" && target === "integration_api-backend") {
      backend("pitest", ["-PmutationScope=integration_api"]);
      return;
    }
    if (task === "mutate" && target === "integration_api-http-backend") {
      backend("pitest", ["-PmutationScope=integration_api_http"]);
      return;
    }
    if (task === "mutate" && target === "github_connector-backend") {
      backend("pitest", ["-PmutationScope=github_connector"]);
      return;
    }
    if (task === "mutate" && target === "import_data-reader-backend") {
      backend("pitest", ["-PmutationScope=import_data_reader"]);
      return;
    }
    if (task === "mutate" && target === "import_data-http-backend") {
      backend("pitest", ["-PmutationScope=import_data_http"]);
      return;
    }
    if (task === "mutate" && target === "import_data-persistence-backend") {
      backend("pitest", ["-PmutationScope=import_data_persistence"]);
      return;
    }
    if (task === "mutate" && target === "webhooks-backend") {
      backend("pitest", ["-PmutationScope=webhooks"]);
      return;
    }
    if (task === "mutate" && target === "webhooks-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.webhooks.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "external_calendar-backend") {
      backend("pitest", ["-PmutationScope=external_calendar"]);
      return;
    }
    if (task === "mutate" && target === "external_calendar-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.external-calendar.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "export_data-persistence-backend") {
      backend("pitest", ["-PmutationScope=export_data_persistence"]);
      return;
    }
    if (task === "mutate" && target === "custom_views_fields-backend") {
      backend("pitest", ["-PmutationScope=custom_views_fields"]);
      return;
    }
    if (task === "mutate" && target === "appearance-backend") {
      backend("pitest", ["-PmutationScope=appearance"]);
      return;
    }
    if (task === "mutate" && target === "weekly_review-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.weekly-review.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "weekly_review-backend") {
      backend("pitest", ["-PmutationScope=weekly_review"]);
      return;
    }
    if (task === "mutate" && target === "end_time_notification-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.end-time-notification.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "history-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.history.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "history-backend") {
      backend("pitest", ["-PmutationScope=history"]);
      return;
    }
    if (task === "mutate" && target === "end_time_notification-backend") {
      backend("pitest", ["-PmutationScope=end_time_notification"]);
      return;
    }
    if (task === "mutate" && target === "close_work_session-backend") {
      backend("pitest", ["-PmutationScope=close_work_session"]);
      return;
    }
    if (task === "mutate" && target === "close_work_session-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.close-work-session.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "pause_resume_session-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.pause-resume-session.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "pause_resume_session-backend") {
      backend("pitest", ["-PmutationScope=pause_resume_session"]);
      return;
    }
    if (task === "mutate" && target === "start_work_session-backend-replay") {
      backend("pitest", ["-PmutationScope=start_work_session_replay"]);
      return;
    }
    if (task === "mutate" && target === "start_work_session-backend") {
      backend("pitest", ["-PmutationScope=start_work_session"]);
      return;
    }
    if (task === "mutate" && target === "start_work_session-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.start-work-session.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "reschedule-backend") {
      backend("pitest", ["-PmutationScope=reschedule"]);
      return;
    }
    if (task === "mutate" && target === "reschedule-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.reschedule.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "today-backend") {
      backend("pitest", ["-PmutationScope=today"]);
      return;
    }
    if (task === "mutate" && target === "today-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.today.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "today-frontend-replay") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.today.replay.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "today-frontend-final") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.today.final.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "schedule_block-backend") {
      backend("pitest", ["-PmutationScope=schedule_block"]);
      return;
    }
    if (task === "mutate" && target === "schedule_block-frontend-replay") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.schedule-block.replay.config.json",
      ]);
      return;
    }
    if (task === "mutate" && target === "schedule_block-frontend") {
      runner("pnpm", [
        "--dir",
        "frontend",
        "mutate",
        "--mutate",
        "src/schedule-block-api.ts,src/task-blocks.tsx,src/task-reader.tsx,src/task-state.tsx",
      ]);
      return;
    }
    if (task === "lint") {
      for (const file of [
        "scripts/project.mjs",
        "scripts/project.test.mjs",
        "scripts/e2e.mjs",
        "playwright.config.mjs",
        "e2e/create-project.spec.mjs",
        "e2e/github-connector.spec.mjs",
        "e2e/github-connector-native-zoom.spec.mjs",
        "e2e/webhooks-native-zoom.spec.mjs",
        "e2e/external-calendar-native-zoom.spec.mjs",
        "e2e/automations-native-zoom.spec.mjs",
        "e2e/support/connector.mjs",
        "e2e/fake-github/server.mjs",
        "e2e/automations.spec.mjs",
        "e2e/automations-ux.spec.mjs",
      ]) {
        runner(process.execPath, ["--check", file]);
      }
    }
    if (task === "test")
      runner(process.execPath, ["--test", "scripts/project.test.mjs"]);
    backend(commands[task]);
    runner("pnpm", ["--dir", "frontend", task]);
  };
}
export const project = createProject();
export function main(args, projectRunner = project) {
  if (args.length > 2) throw new Error("Expected task and optional target");
  projectRunner(args[0], args[1]);
}
if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  try {
    main(process.argv.slice(2));
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
