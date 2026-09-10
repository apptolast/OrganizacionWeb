import assert from "node:assert/strict";
import test from "node:test";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { existsSync, readFileSync } from "node:fs";
import { createHash } from "node:crypto";
import * as commands from "./project.mjs";

const root = fileURLToPath(new URL("../", import.meta.url));
test("import PIT targets reject other tasks and injected options before execution", () => {
  const { calls, project } = capture();
  for (const part of ["reader", "http", "persistence"]) {
    assert.throws(
      () => project("test", `import_data-${part}-backend`),
      /Invalid target/,
    );
    assert.throws(
      () =>
        project("mutate", `import_data-${part}-backend -PmutationScope=other`),
      /Invalid target/,
    );
  }
  assert.deepEqual(calls, []);
});
test("import PIT keeps complete disjoint classes and dedicated tests while extending the default", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const scopes = {
    Reader: {
      classes: [
        "adapter.persistence.ImportJsonReader*",
        "adapter.persistence.ImportReceiptDecoder*",
      ],
      tests: [
        "adapter.persistence.ImportJsonReaderTest",
        "adapter.persistence.ImportReceiptDecoderTest",
      ],
    },
    Http: {
      classes: [
        "adapter.http.ImportDataController*",
        "application.PreviewImportData*",
        "application.ApplyImportData*",
        "application.ReadImportReceipt*",
      ],
      tests: [
        "adapter.ImportDataApiTest",
        "application.PreviewImportDataTest",
        "application.ApplyImportDataTest",
        "application.ReadImportReceiptTest",
      ],
    },
    Persistence: {
      classes: [
        "adapter.persistence.PostgresImportDataStore*",
        "adapter.persistence.ImportRecordValidator*",
        "adapter.persistence.ImportCustomizationValidator*",
        "application.ImportCounts*",
      ],
      tests: [
        "adapter.persistence.ImportPersistenceTest",
        "adapter.persistence.ImportCustomizationValidatorTest",
        "adapter.persistence.ImportConcurrencyTest",
        "adapter.config.ImportScaleTest",
        "adapter.config.ImportWiringTest",
      ],
    },
  };
  for (const [name, selected] of Object.entries(scopes)) {
    const part = name.toLowerCase();
    assert.ok(
      build.includes(`val import${name}Only = scope == "import_data_${part}"`),
    );
    for (const [suffix, expected] of [
      ["Classes", selected.classes],
      ["Tests", selected.tests],
    ]) {
      const body = build.match(
        new RegExp(
          `val import${name}${suffix} = setOf\\(([\\s\\S]*?)\\n    \\)`,
        ),
      )?.[1];
      assert.ok(body, name + suffix);
      assert.deepEqual(
        [...body.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
        expected.map((value) => "com.apptolast.organization." + value),
      );
      assert.ok(build.includes(`import${name}Only -> import${name}${suffix}`));
    }
    assert.ok(
      build.includes(
        `if (import${name}Only) reportDir.set(layout.buildDirectory.dir("reports/pitest-import-data-${part}"))`,
      ),
    );
  }
  assert.match(
    build,
    /else -> core \+ authenticationClasses[^\n]+ \+ exportHttpClasses \+ importReaderClasses \+ importHttpClasses \+ importPersistenceClasses/,
  );
  assert.match(
    build,
    /else -> core \+ authenticationTests[^\n]+ \+ exportAdapterTests \+ importAdapterTests/,
  );
  assert.match(
    build,
    /"com\.apptolast\.organization\.adapter\.config\.Import\*Test"/,
  );
  assert.match(build, /mutationThreshold\.set\(80\)/);
  assert.match(
    build,
    /threads\.set\(if \(integrationApiOnly \|\| integrationApiHttpOnly\) 8 else 4\)/,
  );
});
test("import backend targets invoke only their fixed PIT scopes", () => {
  for (const part of ["reader", "http", "persistence"]) {
    const { calls, project } = capture();
    project("mutate", `import_data-${part}-backend`);
    assert.deepEqual(calls, [
      [
        process.platform === "win32" ? "gradlew.bat" : "./gradlew",
        ["pitest", "--no-daemon", `-PmutationScope=import_data_${part}`],
        { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
      ],
    ]);
  }
});
test("integration backend targets invoke only their fixed PIT scopes", () => {
  for (const [target, scope] of [
    ["integration_api-backend", "integration_api"],
    ["integration_api-http-backend", "integration_api_http"],
  ]) {
    const { calls, project } = capture();
    project("mutate", target);
    assert.deepEqual(calls, [
      [
        process.platform === "win32" ? "gradlew.bat" : "./gradlew",
        ["pitest", "--no-daemon", `-PmutationScope=${scope}`],
        { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
      ],
    ]);
  }
});
test("integration frontend invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "integration_api-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.integration-api.config.json",
      ],
    ],
  ]);
});
test("import frontend invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "import_data-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.import-data.config.json",
      ],
    ],
  ]);
});
test("export Stryker keeps its complete modules and reviewed integration nodes with inherited gates", () => {
  const config = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.export-data.config.json"),
      "utf8",
    ),
  );
  const prior = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.custom-views-fields.config.json"),
      "utf8",
    ),
  );
  const nodes = JSON.parse(
    readFileSync(
      resolve(root, "progress/export_frontend_scope_nodes.json"),
      "utf8",
    ),
  );
  assert.deepEqual(
    config.mutate,
    nodes.flatMap((file) =>
      file.scope === "full"
        ? [file.path.replace("frontend/", "")]
        : file.nodes.map(
            (node) => file.path.replace("frontend/", "") + ":" + node.range,
          ),
    ),
  );
  assert.equal(config.concurrency, 8);
  assert.equal(config.coverageAnalysis, "perTest");
  assert.deepEqual(config.thresholds, prior.thresholds);
  assert.equal(config.thresholds.break, 80);
  assert.deepEqual(config.vitest, prior.vitest);
  assert.deepEqual(config.ignorePatterns, prior.ignorePatterns);
  assert.deepEqual(config.plugins, prior.plugins);
  assert.equal(config.mutator, undefined);
  assert.equal(config.incremental, undefined);
  assert.equal(config.tempDirName, ".stryker-tmp-export-data");
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-export-data/mutation.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-export-data/mutation.html",
  );
});
test("ics calendar Stryker selects its own nodes of the shared files", () => {
  const config = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.ics-calendar.config.json"),
      "utf8",
    ),
  );
  // La FORMA del ambito, sin repetir las coordenadas. Una lista literal de rangos
  // linea:columna dice lo mismo que la validacion por contenido de mas abajo, pero
  // se desplaza cada vez que alguien anade o quita una linea en App.tsx o en
  // workspace.tsx: la guarda se quedaba roja sin que nada estuviera mal. Paso el 10
  // de septiembre de 2026 al retirar tres features. Lo que de verdad hay que fijar
  // es que los tramos apunten al texto correcto, y de eso se encarga el bucle de
  // abajo.
  assert.equal(config.mutate.length, 6);
  for (const selector of config.mutate.slice(0, 4))
    assert.match(
      selector,
      /^src\/(App|workspace)\.tsx:\d+:\d+-\d+:\d+$/,
      selector,
    );
  assert.deepEqual(config.mutate.slice(4), [
    "src/calendar-feed-api.ts",
    "src/calendar.tsx",
  ]);
  assert.deepEqual(config.thresholds, { high: 90, low: 80, break: 80 });
  assert.equal(config.tempDirName, ".stryker-tmp-ics-calendar");
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-ics-calendar/mutation.json",
  );
  const expected = [
    ["calendar = route", "/calendario"],
    ["calendar", "Calendario"],
    ["calendar && username", "<Calendar owner={username} />"],
    ["<RouteLink", "/calendario"],
  ];
  for (const [index, selector] of config.mutate.slice(0, 4).entries()) {
    const [, path, startLine, startColumn, endLine, endColumn] = selector.match(
      /^(.+):(\d+):(\d+)-(\d+):(\d+)$/,
    );
    const lines = readFileSync(resolve(root, "frontend", path), "utf8").split(
      /\r?\n/,
    );
    const selected = lines.slice(Number(startLine) - 1, Number(endLine));
    selected[selected.length - 1] = selected.at(-1).slice(0, Number(endColumn));
    selected[0] = selected[0].slice(Number(startColumn));
    assert.ok(selected.join("\n").startsWith(expected[index][0]), selector);
    assert.ok(selected.join("\n").includes(expected[index][1]), selector);
  }
});
test("ics calendar frontend invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "ics_calendar-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.ics-calendar.config.json",
      ],
    ],
  ]);
});
test("ics calendar backend runs PIT on its own scope and nothing else", () => {
  const { calls, project } = capture();
  project("mutate", "ics_calendar-backend");
  assert.equal(calls.length, 1);
  assert.deepEqual(calls[0][1], [
    "pitest",
    "--no-daemon",
    "-PmutationScope=ics_calendar",
  ]);
});
test("ics calendar targets reject other tasks and injected options", () => {
  const { calls, project } = capture();
  for (const target of ["ics_calendar-frontend", "ics_calendar-backend"]) {
    assert.throws(() => project("test", target), /Invalid target/);
    assert.throws(
      () => project("mutate", `${target} -PmutationScope=other`),
      /Invalid target/,
    );
    assert.throws(() => project("mutate", `${target}-extra`), /Invalid target/);
  }
  assert.deepEqual(calls, []);
});
test("export frontend invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "export_data-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.export-data.config.json",
      ],
    ],
  ]);
});
test("export persistence target rejects other tasks and injected options before execution", () => {
  const { calls, project } = capture();
  assert.throws(
    () => project("test", "export_data-persistence-backend"),
    /Invalid target/,
  );
  assert.throws(
    () =>
      project(
        "mutate",
        "export_data-persistence-backend -PmutationScope=other",
      ),
    /Invalid target/,
  );
  assert.throws(
    () => project("mutate", "export_data-persistence-backend-extra"),
    /Invalid target/,
  );
  assert.deepEqual(calls, []);
});

test("export persistence PIT preserves complete classes and extends the default", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const selected = build.match(
    /val exportPersistenceClasses = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(selected);
  assert.deepEqual(
    [...selected.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
    [
      "adapter.persistence.PostgresExportDataQueries*",
      "adapter.persistence.ExportJsonWriter*",
      "adapter.persistence.ExportBuffer*",
      "application.PrepareExportData*",
      "application.ExportDataUseCase*",
      "application.ExportDataQueries*",
      "application.PreparedExport*",
      "application.ExportTooLargeException*",
      "adapter.config.ApplicationConfiguration*",
    ].map((name) => `com.apptolast.organization.${name}`),
  );
  assert.match(
    build,
    /val exportPersistenceOnly = scope == "export_data_persistence"/,
  );
  assert.match(build, /exportPersistenceOnly -> exportPersistenceClasses/);
  assert.match(
    build,
    /exportPersistenceOnly -> setOf\("com\.apptolast\.organization\.\*"\)/,
  );
  assert.match(
    build,
    /else -> core \+ authenticationClasses[^\n]+ \+ customizationClasses \+ exportPersistenceClasses \+ exportHttpClasses/,
  );
  const http = build.match(
    /val exportHttpClasses = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(http);
  assert.deepEqual(
    [...http.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
    [
      "adapter.http.ExportDataController*",
      "adapter.http.ExportHeadersFilter*",
      "adapter.persistence.ExportReceiptWriter*",
    ].map((name) => `com.apptolast.organization.${name}`),
  );
  const tests = build.match(
    /val exportAdapterTests = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(tests);
  assert.deepEqual(
    [...tests.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
    [
      "application.*Export*Test",
      "adapter.Export*Test",
      "adapter.http.Export*Test",
      "adapter.persistence.Export*Test",
      "adapter.config.Export*Test",
    ].map((name) => `com.apptolast.organization.${name}`),
  );
  assert.match(
    build,
    /else -> core \+ authenticationTests[^\n]+ \+ customizationAdapterTests \+ exportAdapterTests/,
  );
  assert.match(
    build,
    /if \(exportPersistenceOnly\) reportDir\.set\(layout\.buildDirectory\.dir\("reports\/pitest-export-data-persistence"\)\)/,
  );
  assert.match(build, /mutationThreshold\.set\(80\)/);
  assert.match(
    build,
    /threads\.set\(if \(integrationApiOnly \|\| integrationApiHttpOnly\) 8 else 4\)/,
  );
});

test("export persistence backend invokes only its fixed PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "export_data-persistence-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=export_data_persistence"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("customization default PIT exposes the real feature wiring test", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const tests = build.match(
    /val customizationAdapterTests = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(
    tests?.includes(
      '"com.apptolast.organization.adapter.config.CustomizationWiringTest"',
    ),
  );
});
test("customization targets fail closed outside mutation", () => {
  const { calls, project } = capture();
  for (const target of [
    "custom_views_fields-backend",
    "custom_views_fields-frontend",
  ])
    assert.throws(() => project("test", target), /Invalid target/);
  assert.throws(
    () => project("mutate", "custom_views_fields-unknown"),
    /Invalid target/,
  );
  assert.deepEqual(calls, []);
});
test("customization Stryker includes new modules without weakening inherited gates", () => {
  const config = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.custom-views-fields.config.json"),
      "utf8",
    ),
  );
  const prior = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.appearance.config.json"),
      "utf8",
    ),
  );
  for (const file of [
    "src/customization-api.ts",
    "src/custom-fields-api.ts",
    "src/customization-state.ts",
    "src/customization.tsx",
    "src/custom-fields.tsx",
  ])
    assert.ok(config.mutate.includes(file), file);
  assert.ok(config.mutate.every((file) => !file.startsWith("!")));
  assert.deepEqual(config.thresholds, prior.thresholds);
  assert.equal(config.thresholds.break, 80);
  assert.equal(config.concurrency, 8);
  assert.equal(config.coverageAnalysis, "perTest");
  assert.deepEqual(config.vitest, prior.vitest);
  assert.deepEqual(config.ignorePatterns, prior.ignorePatterns);
  assert.deepEqual(config.plugins, prior.plugins);
  assert.equal(config.tempDirName, ".stryker-tmp-custom-views-fields-refined");
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-custom-views-fields-refined/mutation.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-custom-views-fields-refined/mutation.html",
  );
});
test("customization frontend invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "custom_views_fields-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.custom-views-fields.config.json",
      ],
    ],
  ]);
});
test("customization PIT includes complete families and exposes their adapter tests by default", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const selected = build.match(
    /val customizationClasses = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(selected);
  for (const name of [
    "domain.Customization*",
    "domain.CustomField*",
    "application.ReadCustomization*",
    "application.SaveCustomization*",
    "application.CreateCustomField*",
    "application.UpdateCustomField*",
    "application.ReadCustomFieldValues*",
    "application.SaveCustomFieldValues*",
    "application.Customization*",
    "application.CustomFieldValues*",
    "adapter.persistence.PostgresCustomizationStore*",
    "adapter.http.CustomizationController*",
    "adapter.config.ApplicationConfiguration",
  ])
    assert.ok(selected.includes(`"com.apptolast.organization.${name}"`), name);
  assert.match(build, /val customizationOnly = scope == "custom_views_fields"/);
  assert.match(build, /customizationOnly -> customizationClasses/);
  assert.match(
    build,
    /customizationOnly -> setOf\("com\.apptolast\.organization\.\*"\)/,
  );
  assert.match(build, /else -> [^\n]+ \+ customizationClasses/);
  const tests = build.match(
    /val customizationAdapterTests = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(tests);
  for (const name of [
    "adapter.CustomizationApiTest",
    "adapter.persistence.Customization*Test",
    "adapter.persistence.CustomField*Test",
    "adapter.config.ApplicationWiringTest",
  ])
    assert.ok(tests.includes(`"com.apptolast.organization.${name}"`), name);
  assert.match(
    build,
    /else -> core \+ authenticationTests[^\n]+ \+ customizationAdapterTests/,
  );
  assert.match(
    build,
    /if \(customizationOnly\) reportDir\.set\(layout\.buildDirectory\.dir\("reports\/pitest-custom-views-fields"\)\)/,
  );
  assert.match(build, /mutationThreshold\.set\(80\)/);
  assert.match(
    build,
    /threads\.set\(if \(integrationApiOnly \|\| integrationApiHttpOnly\) 8 else 4\)/,
  );
});
test("customization backend invokes only its fixed PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "custom_views_fields-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=custom_views_fields"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});
test("end time Stryker preserves protection and measures both integrated surfaces", () => {
  const config = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.end-time-notification.config.json"),
      "utf8",
    ),
  );
  const prior = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.close-work-session.config.json"),
      "utf8",
    ),
  );
  assert.deepEqual(config.mutate, [
    "src/work-session-end-api.ts",
    "src/work-session-end.tsx",
    "src/use-work-session-decision.ts",
    "src/work-session-state-api.ts",
    "src/work-session-state.tsx",
    "src/work-session-reader.tsx",
    "src/work-session.tsx",
  ]);
  assert.deepEqual(config.thresholds, prior.thresholds);
  assert.equal(config.thresholds.break, 80);
  assert.equal(config.concurrency, 8);
  assert.equal(config.coverageAnalysis, "perTest");
  assert.deepEqual(config.ignorePatterns, prior.ignorePatterns);
  assert.ok(config.ignorePatterns.includes(".stryker-tmp-*"));
  assert.deepEqual(config.vitest, prior.vitest);
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-end-time-notification/mutation.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-end-time-notification/mutation.html",
  );
  assert.equal(config.tempDirName, ".stryker-tmp-end-time-notification");
});
test("end time PIT preserves shared guards, all JUnit candidates and threshold", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  assert.match(
    build,
    /val endTimeNotificationOnly = scope == "end_time_notification"/,
  );
  assert.match(build, /endTimeNotificationOnly -> endTimeNotificationClasses/);
  assert.match(
    build,
    /endTimeNotificationOnly -> setOf\("com\.apptolast\.organization\.\*"\)/,
  );
  const selected = build.match(
    /val endTimeNotificationClasses = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(selected);
  for (const name of [
    "application.ExtendWorkSession",
    "application.ReadWorkSessionEnd",
    "application.WorkSessionEnd",
    "application.WorkSessionEndSnapshot",
    "application.WorkSessionExtension",
    "application.WorkSessionExtensionTransition",
    "application.WorkSessionExtended",
    "application.WorkSessionTransitionReceipt",
    "application.WorkSessionTransition",
    "application.WorkSessionChanging",
    "application.ChangeWorkSession",
    "application.ReadWorkSessionState",
    "application.ReadWorkSessionChanges",
    "domain.WorkSessionState",
    "domain.OutboxMessage",
    "adapter.persistence.PostgresWorkSessionStore*",
    "adapter.http.WorkSessionStateController*",
    "adapter.broker.RabbitBrokerPublisher",
    "adapter.config.ApplicationConfiguration",
  ]) {
    assert.ok(selected.includes(`"com.apptolast.organization.${name}"`), name);
  }
  assert.match(build, /else -> [^\n]+ \+ endTimeNotificationClasses/);
  assert.match(
    build,
    /if \(endTimeNotificationOnly\) reportDir\.set\(layout\.buildDirectory\.dir\("reports\/pitest-end-time-notification"\)\)/,
  );
  assert.match(build, /mutationThreshold\.set\(80\)/);
  assert.match(
    build,
    /threads\.set\(if \(integrationApiOnly \|\| integrationApiHttpOnly\) 8 else 4\)/,
  );
});
test("end time frontend invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "end_time_notification-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.end-time-notification.config.json",
      ],
    ],
  ]);
});
test("end time backend invokes only its fixed PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "end_time_notification-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=end_time_notification"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});
function historicalTodayFile(file) {
  const snapshot = JSON.parse(
    readFileSync(
      resolve(root, "progress/today_frontend_historical_snapshot.json"),
      "utf8",
    ),
  );
  assert.equal(
    snapshot.sourceCommit,
    "1f7090eb6a114194622d45184a84be4904f302bb",
  );
  assert.equal(snapshot.encoding, "base64");
  const entry = snapshot.files[file];
  assert.equal(entry.path, `frontend/${file}`);
  if (file === "stryker.config.json")
    assert.equal(
      entry.sha256,
      "42f5684c14952730c9af830a2aed9c6535ed36bd2bb2c07dc3523e3cde9ac017",
    );
  const bytes = Buffer.from(entry.base64, "base64");
  assert.equal(createHash("sha256").update(bytes).digest("hex"), entry.sha256);
  return bytes;
}
function capture() {
  const calls = [];
  // An injected runner keeps these tests from launching Gradle or Stryker.
  const project = commands.createProject((...args) => calls.push(args));
  return { calls, project };
}

test("close work Stryker includes shared navigation and protects previous reports", () => {
  const config = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.close-work-session.config.json"),
      "utf8",
    ),
  );
  const prior = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.pause-resume-session.config.json"),
      "utf8",
    ),
  );
  assert.deepEqual(config.mutate, [
    "src/work-session-state-api.ts",
    "src/work-session-state.tsx",
    "src/work-session-reader.tsx",
    "src/App.tsx",
    "src/use-session.ts",
  ]);
  assert.deepEqual(config.thresholds, prior.thresholds);
  assert.equal(config.thresholds.break, 80);
  assert.equal(config.concurrency, 8);
  assert.equal(config.coverageAnalysis, "perTest");
  assert.deepEqual(config.ignorePatterns, prior.ignorePatterns);
  assert.ok(config.ignorePatterns.includes(".stryker-tmp-*"));
  assert.deepEqual(config.vitest, prior.vitest);
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-close-work-session/mutation.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-close-work-session/mutation.html",
  );
  assert.equal(config.tempDirName, ".stryker-tmp-close-work-session");
});

test("close work PIT scope includes changed core and shared adapters with all JUnit candidates", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  assert.match(
    build,
    /val closeWorkSessionOnly = scope == "close_work_session"/,
  );
  assert.match(build, /closeWorkSessionOnly -> closeWorkSessionClasses/);
  assert.match(
    build,
    /closeWorkSessionOnly -> setOf\("com\.apptolast\.organization\.\*"\)/,
  );
  const selected = build.match(
    /val closeWorkSessionClasses = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(selected);
  for (const name of [
    "domain.WorkSessionCloseNotes",
    "domain.WorkSessionState",
    "domain.OutboxMessage",
    "application.ChangeWorkSession",
    "application.ReadWorkSessionState",
    "application.ReadWorkSessionChanges",
    "application.WorkSessionTransitionReceipt",
    "application.WorkSessionTransition",
    "application.WorkSessionChanging",
    "application.WorkSessionClosed",
    "application.WorkSessionClosure",
    "adapter.persistence.PostgresWorkSessionStore*",
    "adapter.http.WorkSessionStateController*",
    "adapter.broker.RabbitBrokerPublisher",
    "adapter.config.ApplicationConfiguration",
  ]) {
    assert.ok(selected.includes(`"com.apptolast.organization.${name}"`), name);
  }
  assert.match(
    build,
    /if \(closeWorkSessionOnly\) reportDir\.set\(layout\.buildDirectory\.dir\("reports\/pitest-close-work-session"\)\)/,
  );
  assert.match(build, /mutationThreshold\.set\(80\)/);
  assert.match(
    build,
    /threads\.set\(if \(integrationApiOnly \|\| integrationApiHttpOnly\) 8 else 4\)/,
  );
});

test("close work frontend invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "close_work_session-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.close-work-session.config.json",
      ],
    ],
  ]);
});

test("close work backend invokes only its fixed PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "close_work_session-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=close_work_session"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("pause resume frontend invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "pause_resume_session-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.pause-resume-session.config.json",
      ],
    ],
  ]);
});

test("pause resume backend invokes only its fixed PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "pause_resume_session-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=pause_resume_session"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("start work PIT selects the complete feature and shared publication with all JUnit candidates", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  assert.match(
    build,
    /val startWorkSessionOnly = scope == "start_work_session"/,
  );
  assert.match(build, /startWorkSessionOnly -> startWorkSessionClasses/);
  assert.match(
    build,
    /startWorkSessionOnly -> setOf\("com\.apptolast\.organization\.\*"\)/,
  );
  assert.match(
    build,
    /if \(startWorkSessionOnly\) reportDir\.set\(layout\.buildDirectory\.dir\("reports\/pitest-start-work-session"\)\)/,
  );
  const selection =
    /val startWorkSessionClasses = setOf\(([\s\S]*?)\n    \)/.exec(build)?.[1];
  assert.ok(selection);
  assert.deepEqual(
    [...selection.matchAll(/"([^"]+)"/g)].map((match) => match[1]),
    [
      "com.apptolast.organization.domain.SessionStart",
      "com.apptolast.organization.domain.OutboxMessage",
      "com.apptolast.organization.application.StartWorkSession*",
      "com.apptolast.organization.application.ReadWorkSessions*",
      "com.apptolast.organization.application.WorkSession*",
      "com.apptolast.organization.application.PublishOutbox",
      "com.apptolast.organization.adapter.persistence.PostgresWorkSessionStore*",
      "com.apptolast.organization.adapter.http.WorkSessionController*",
      "com.apptolast.organization.adapter.config.ApplicationConfiguration",
      "com.apptolast.organization.adapter.broker.RabbitBrokerPublisher",
    ],
  );
  assert.match(
    build,
    /threads\.set\(if \(integrationApiOnly \|\| integrationApiHttpOnly\) 8 else 4\)/,
  );
  assert.match(build, /mutationThreshold\.set\(80\)/);
});

test("start work backend rejects a non-mutation task before execution", () => {
  const { calls, project } = capture();
  assert.throws(
    () => project("test", "start_work_session-backend"),
    /Invalid target/,
  );
  assert.deepEqual(calls, []);
});

test("start work frontend rejects injected configuration before execution", () => {
  const { calls, project } = capture();
  assert.throws(
    () => project("mutate", "start_work_session-frontend --config other.json"),
    /Invalid target/,
  );
  assert.deepEqual(calls, []);
});

test("default frontend scope retains the complete new work session sources", () => {
  const config = JSON.parse(
    readFileSync(resolve(root, "frontend/stryker.config.json"), "utf8"),
  );
  assert.ok(config.mutate.includes("src/work-session-api.ts"));
  assert.ok(config.mutate.includes("src/work-session.tsx"));
  assert.ok(config.mutate.includes("src/task-reader.tsx"));
});

test("start work frontend scope includes the session flow and its task reader integration", () => {
  const config = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.start-work-session.config.json"),
      "utf8",
    ),
  );
  assert.deepEqual(config.mutate, [
    "src/work-session-api.ts",
    "src/work-session.tsx",
    "src/task-reader.tsx:124:0-135:12",
  ]);
  assert.equal(config.thresholds.break, 80);
  assert.equal(config.coverageAnalysis, "perTest");
  assert.equal(config.concurrency, 8);
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-*"]);
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-start-work-session/mutation.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-start-work-session/mutation.html",
  );
  const snapshot = JSON.parse(
    readFileSync(
      resolve(root, "progress/start_work_frontend_historical_snapshot.json"),
      "utf8",
    ),
  );
  assert.equal(
    snapshot.sourceCommit,
    "46913a71c1a582357fa8d50053766a50a5793299",
  );
  assert.equal(snapshot.encoding, "base64");
  const entry = snapshot.files["src/task-reader.tsx"];
  assert.equal(entry.path, "frontend/src/task-reader.tsx");
  const bytes = Buffer.from(entry.base64, "base64");
  assert.equal(
    entry.sha256,
    "0CC6ED956F083FE7262F512DA2830F11FB46596191DDD4E6C2DA5945E187402A",
  );
  assert.equal(
    createHash("sha256").update(bytes).digest("hex").toUpperCase(),
    entry.sha256,
  );
  const lines = bytes.toString("utf8").split(/\r?\n/);
  assert.equal(lines[123].trim(), "<WorkSession");
  assert.equal(lines[134].trim(), "/>");
});

test("start work backend invokes only its fixed PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "start_work_session-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=start_work_session"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("start work frontend invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "start_work_session-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.start-work-session.config.json",
      ],
    ],
  ]);
});

test("today backend scope runs only its fixed PIT target", () => {
  const { calls, project } = capture();
  project("mutate", "today-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=today"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("reschedule frontend scope runs only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "reschedule-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.reschedule.config.json",
      ],
    ],
  ]);
});

test("today frontend scope runs only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "today-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.today.config.json",
      ],
    ],
  ]);
});

test("today replay runs only its fixed separate Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "today-frontend-replay");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.today.replay.config.json",
      ],
    ],
  ]);
});

test("today replay preserves 63 reviewed identities plus the new focus region without rewriting initial measurement", () => {
  const read = (path) => JSON.parse(readFileSync(resolve(root, path), "utf8"));
  const manifest = read("progress/today_frontend_replay_selection.json");
  const inventory = read("progress/mutation_today_frontend_inventory.json");
  const config = read("frontend/stryker.today.replay.config.json");
  assert.equal(
    manifest.originalReportSha256,
    "5cc335b97919aaa1bcd3cf4cf956af54ee55db3a02fdb23a060c77508f5c47a3",
  );
  assert.equal(manifest.selection.length, 63);
  assert.equal(
    new Set(manifest.selection.map((item) => item.originalId)).size,
    63,
  );
  assert.deepEqual(
    [...manifest.excludedIds].sort(),
    "78,85,89,93,209,215,216,219,222,253,259,291,336,338,350,351,371,411,435,487,493,506,507,508,509,510,511,517,518,519,520,521,522,523,529,530,531,532,533,534"
      .split(",")
      .sort(),
  );
  assert.equal(
    new Set([
      ...manifest.selection.map((item) => item.originalId),
      ...manifest.excludedIds,
    ]).size,
    103,
  );
  assert.deepEqual(
    manifest.selection.map((item) => item.originalId),
    inventory.pending
      .filter((item) => !manifest.excludedIds.includes(item.id))
      .map((item) => item.id),
  );
  assert.deepEqual(config.mutate, [
    ...new Set([
      ...manifest.selection.map((item) => item.range),
      manifest.newFocusRegion.range,
    ]),
  ]);
  assert.equal(manifest.newFocusRegion.originalId, null);
  assert.equal(manifest.newFocusRegion.range, "src/today.tsx:124:0-124:78");
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-today/replay.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-today/replay.html",
  );
  assert.deepEqual(config.thresholds, { high: 90, low: 80, break: 80 });
  assert.equal(config.coverageAnalysis, "perTest");
  assert.equal(config.concurrency, 2);
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-*"]);
  for (const item of manifest.selection) {
    assert.ok(["Survived", "NoCoverage"].includes(item.originalStatus));
    const original = inventory.pending.find(
      (entry) => entry.id === item.originalId,
    );
    assert.equal(item.sourceExpression, original.sourceExpression);
    assert.equal(item.replacement, original.replacement);
    assert.equal(item.operator, original.operator);
    assert.equal(item.originalStatus, original.status);
    assert.deepEqual(item.originalLocation, original.location);
    const source = historicalTodayFile(item.file)
      .toString("utf8")
      .replace(/\r\n/g, "\n");
    const offset = ({ line, column }) =>
      source
        .split("\n")
        .slice(0, line - 1)
        .reduce((sum, text) => sum + text.length + 1, 0) + column;
    assert.equal(
      source.slice(
        offset(item.mappedLocation.start),
        offset(item.mappedLocation.end),
      ),
      item.sourceExpression,
      item.originalId,
    );
  }
  for (const [file, hash] of Object.entries(manifest.sourceSha256))
    assert.equal(
      createHash("sha256").update(historicalTodayFile(file)).digest("hex"),
      hash,
      file,
    );
});

test("today configuration preserves its historical source and shared-region selection", () => {
  const read = (path) => JSON.parse(readFileSync(resolve(root, path), "utf8"));
  const config = read("frontend/stryker.today.config.json");
  const full = JSON.parse(
    historicalTodayFile("stryker.config.json").toString("utf8"),
  );
  const manifest = read("progress/today_frontend_mutation_scope.json");
  assert.deepEqual(config.thresholds, { high: 90, low: 80, break: 80 });
  assert.equal(config.coverageAnalysis, "perTest");
  assert.equal(config.concurrency, 2);
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-*"]);
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-today/mutation.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-today/mutation.html",
  );
  assert.deepEqual(config.mutate, manifest.mutate);
  assert.deepEqual(config.mutate, [
    "src/today-api.ts",
    "src/today.tsx",
    "src/App.tsx:10:0-49:1",
    "src/workspace.tsx:27:0-54:22",
    "src/workspace.tsx:85:0-85:64",
    "src/project-reader.tsx:61:0-61:70",
    "src/project-reader.tsx:157:0-157:76",
    "src/use-session.ts:29:0-29:43",
    "src/use-session.ts:189:0-189:36",
  ]);
  for (const [path, hash] of Object.entries(manifest.sourceSha256)) {
    assert.equal(
      createHash("sha256").update(historicalTodayFile(path)).digest("hex"),
      hash,
      path,
    );
  }
  for (const range of config.mutate) {
    const file = range.split(":")[0];
    assert.ok(full.mutate.includes(range) || full.mutate.includes(file), range);
  }
  assert.ok(!full.mutate.includes("src/App.tsx"));
  assert.ok(!full.mutate.includes("src/App.tsx:10:0-18:200"));
  assert.ok(!full.mutate.includes("src/workspace.tsx:72:0-72:200"));
});

test("backend scope runs only the configured schedule_block PIT target", () => {
  const { calls, project } = capture();
  project("mutate", "schedule_block-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=schedule_block"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("frontend scope includes new code and both shared guards without changing thresholds", () => {
  const { calls, project } = capture();
  project("mutate", "schedule_block-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "mutate",
        "--mutate",
        "src/schedule-block-api.ts,src/task-blocks.tsx,src/task-reader.tsx,src/task-state.tsx",
      ],
    ],
  ]);
});

test("unknown or misplaced targets fail before starting any subprocess", () => {
  for (const [task, target] of [
    ["mutate", "other"],
    ["mutate", "schedule_block-backend;echo unsafe"],
    ["mutate", "schedule_block-frontend-replay --config other.json"],
    ["test", "schedule_block-backend"],
    ["install", "schedule_block-frontend"],
    ["test", "schedule_block-frontend-replay"],
    ["mutate", "today-backend;echo unsafe"],
    ["mutate", "today-frontend --config other.json"],
    ["test", "today-backend"],
    ["install", "today-frontend"],
    ["mutate", "today-frontend-replay --config other.json"],
    ["mutate", "today-frontend-final --config other.json"],
    ["test", "today-frontend-final"],
    ["test", "today-frontend-replay"],
  ]) {
    const { calls, project } = capture();
    assert.throws(() => project(task, target), /Invalid target/);
    assert.deepEqual(calls, []);
  }
});
test("frontend replay invokes only its fixed isolated report configuration", () => {
  const { calls, project } = capture();
  project("mutate", "schedule_block-frontend-replay");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.schedule-block.replay.config.json",
      ],
    ],
  ]);
});
test("frontend replay keeps measurement policy and writes separate reports", () => {
  const config = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.schedule-block.replay.config.json"),
      "utf8",
    ),
  );
  assert.deepEqual(config.thresholds, { high: 90, low: 80, break: 80 });
  assert.equal(config.coverageAnalysis, "perTest");
  assert.equal(config.concurrency, 2);
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-*"]);
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-schedule-block/final.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-schedule-block/final.html",
  );
  assert.deepEqual(config.reporters, ["clear-text", "json", "html"]);
  assert.ok(config.mutate.length > 0);
  for (const range of config.mutate)
    assert.match(
      range,
      /^src\/(schedule-block-api\.ts|task-blocks\.tsx|task-reader\.tsx|task-state\.tsx):\d+:\d+-\d+:\d+$/,
    );
});

test("absent or empty target preserves both full mutation suites", () => {
  for (const target of [undefined, ""]) {
    const { calls, project } = capture();
    project("mutate", target);
    assert.deepEqual(calls, [
      [
        process.platform === "win32" ? "gradlew.bat" : "./gradlew",
        ["pitest", "--no-daemon"],
        { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
      ],
      ["pnpm", ["--dir", "frontend", "mutate"]],
    ]);
  }
});
test("final replay selects the reviewed outstanding identities without changing history", () => {
  const manifest = JSON.parse(
    readFileSync(
      resolve(root, "progress/schedule_block_frontend_final_selection.json"),
      "utf8",
    ),
  );
  const config = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.schedule-block.replay.config.json"),
      "utf8",
    ),
  );
  const ids = manifest.selection.map((item) => item.originalId);
  assert.equal(new Set(ids).size, 29);
  assert.deepEqual(
    [...ids].sort(),
    "746,931,1070,1303,1304,1401,1403,750,757,814,816,829,887,894,897,898,996,1124,1143,1154,1157,1186,1241,1248,1250,1267,1268,1269,1273"
      .split(",")
      .sort(),
  );
  assert.ok(
    manifest.selection.every(
      (item) =>
        item.replayStatus === "Survived" && item.replayId && item.sourceSha256,
    ),
  );
  assert.deepEqual(
    [...new Set(manifest.selection.map((item) => item.range))].sort(),
    [...config.mutate].sort(),
  );
  assert.notEqual(
    config.jsonReporter.fileName,
    manifest.previousReplayReport.replace(/^frontend\//, ""),
  );
  assert.equal(
    manifest.previousSelectionManifest,
    "progress/schedule_block_frontend_replay_selection.json",
  );
});

test("harness placeholder and CLI deliver the selected target unchanged", () => {
  const config = JSON.parse(
    readFileSync(resolve(root, "harness.config.json"), "utf8"),
  );
  assert.equal(
    config.commands.mutate,
    "node scripts/project.mjs mutate {{target}}",
  );
  for (const args of [
    ["mutate"],
    ["mutate", "schedule_block-backend"],
    ["mutate", "schedule_block-frontend"],
    ["mutate", "schedule_block-frontend-replay"],
    ["mutate", "today-backend"],
    ["mutate", "today-frontend"],
    ["mutate", "today-frontend-replay"],
    ["mutate", "today-frontend-final"],
  ]) {
    const calls = [];
    commands.main(args, (...values) => calls.push(values));
    assert.deepEqual(calls, [[args[0], args[1]]]);
  }
});

test("normal CI test and lint include the script regression", () => {
  const testing = capture();
  testing.project("test");
  assert.deepEqual(testing.calls[0], [
    process.execPath,
    ["--test", "scripts/project.test.mjs"],
  ]);
  assert.deepEqual(
    testing.calls.slice(1).map((call) => call[1]),
    [
      ["test", "--no-daemon"],
      ["--dir", "frontend", "test"],
    ],
  );
  const linting = capture();
  linting.project("lint");
  assert.ok(
    linting.calls.some(
      (call) =>
        call[0] === process.execPath &&
        call[1][0] === "--check" &&
        call[1][1] === "scripts/project.test.mjs",
    ),
  );
});

test("CLI rejects extra arguments instead of silently discarding them", () => {
  const calls = [];
  assert.throws(
    () =>
      commands.main(
        ["mutate", "schedule_block-backend", "unexpected"],
        (...args) => calls.push(args),
      ),
    /Expected task and optional target/,
  );
  assert.deepEqual(calls, []);
});

test("today final runs only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "today-frontend-final");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.today.final.config.json",
      ],
    ],
  ]);
});

test("today final selects only the two unresolved exact identities", () => {
  const read = (p) => JSON.parse(readFileSync(resolve(root, p), "utf8"));
  const config = read("frontend/stryker.today.final.config.json"),
    manifest = read("progress/today_frontend_final_selection.json"),
    prior = read("progress/today_frontend_replay_selection.json");
  assert.deepEqual(
    manifest.selection,
    prior.selection.filter((x) => ["337", "412"].includes(x.originalId)),
  );
  assert.deepEqual(manifest.priorReplayIds, ["74", "88"]);
  assert.deepEqual(config.mutate, [
    "src/today.tsx:61:8-61:13",
    "src/today.tsx:111:30-111:34",
  ]);
  assert.deepEqual(
    config.mutate,
    manifest.selection.map((x) => x.range),
  );
  assert.deepEqual(config.thresholds, { high: 90, low: 80, break: 80 });
  assert.equal(config.coverageAnalysis, "perTest");
  assert.equal(config.concurrency, 2);
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-*"]);
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-today/final.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-today/final.html",
  );
  assert.equal(
    manifest.sourceSha256,
    createHash("sha256")
      .update(historicalTodayFile("src/today.tsx"))
      .digest("hex"),
  );
  for (const x of manifest.selection) {
    const lines = historicalTodayFile(x.file).toString("utf8").split(/\r?\n/);
    assert.equal(
      lines[x.mappedLocation.start.line - 1].slice(
        x.mappedLocation.start.column,
        x.mappedLocation.end.column,
      ),
      x.sourceExpression,
    );
  }
});

test("reschedule backend invokes only its fixed PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "reschedule-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=reschedule"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("reschedule backend rejects shell suffix before execution", () => {
  const { calls, project } = capture();
  assert.throws(
    () => project("mutate", "reschedule-backend;echo unsafe"),
    /Invalid target/,
  );
  assert.deepEqual(calls, []);
});

test("reschedule backend rejects extra Gradle flags before execution", () => {
  const { calls, project } = capture();
  assert.throws(
    () => project("mutate", "reschedule-backend -PmutationScope=other"),
    /Invalid target/,
  );
  assert.deepEqual(calls, []);
});

test("reschedule backend rejects non-mutation task before execution", () => {
  const { calls, project } = capture();
  assert.throws(() => project("test", "reschedule-backend"), /Invalid target/);
  assert.deepEqual(calls, []);
});

test("start work replay limits PIT to its event record without replacing the original report", () => {
  const { calls, project } = capture();
  project("mutate", "start_work_session-backend-replay");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=start_work_session_replay"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  assert.match(
    build,
    /val startWorkSessionReplayOnly = scope == "start_work_session_replay"/,
  );
  assert.match(
    build,
    /startWorkSessionReplayOnly -> setOf\("com\.apptolast\.organization\.application\.WorkSessionStarted"\)/,
  );
  assert.match(
    build,
    /startWorkSessionReplayOnly -> setOf\("com\.apptolast\.organization\.\*"\)/,
  );
  assert.match(
    build,
    /if \(startWorkSessionReplayOnly\) reportDir\.set\(layout\.buildDirectory\.dir\("reports\/pitest-start-work-session-replay"\)\)/,
  );
  assert.match(build, /mutationThreshold\.set\(80\)/);
});

test("history backend invokes only its fixed PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "history-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=history"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("history PIT includes complete new modules and wiring with all JUnit candidates", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const selected = build.match(
    /val historyClasses = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(selected);
  assert.deepEqual(
    [...selected.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
    [
      "application.ReadHistory",
      "application.ReadHistoryUseCase",
      "application.HistoryQueries",
      "application.HistoryEntry",
      "application.HistoryFilters",
      "application.HistoryCursor",
      "application.HistoryPosition",
      "application.HistoryPage",
      "adapter.persistence.PostgresHistoryQueries*",
      "adapter.http.HistoryController*",
      "adapter.http.HistoryCursorCodec*",
      "adapter.config.ApplicationConfiguration",
    ].map((name) => `com.apptolast.organization.${name}`),
  );
  assert.match(build, /val historyOnly = scope == "history"/);
  assert.match(build, /historyOnly -> historyClasses/);
  assert.match(
    build,
    /historyOnly -> setOf\("com\.apptolast\.organization\.\*"\)/,
  );
  assert.match(build, /else -> [^\n]+ \+ historyClasses/);
  assert.match(
    build,
    /if \(historyOnly\) reportDir\.set\(layout\.buildDirectory\.dir\("reports\/pitest-history"\)\)/,
  );
  assert.match(build, /mutationThreshold\.set\(80\)/);
  assert.match(
    build,
    /threads\.set\(if \(integrationApiOnly \|\| integrationApiHttpOnly\) 8 else 4\)/,
  );
});

test("default PIT exposes history adapter tests for the newly selected classes", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const selected = build.match(
    /val historyAdapterTests = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(selected);
  assert.deepEqual(
    [...selected.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
    [
      "com.apptolast.organization.adapter.HistoryApiTest",
      "com.apptolast.organization.adapter.persistence.History*Test",
    ],
  );
  assert.match(
    build,
    /else -> core \+ authenticationTests[^\n]+ \+ historyAdapterTests/,
  );
});

test("history frontend invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "history-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.history.config.json",
      ],
    ],
  ]);
});

test("history frontend rejects use outside mutation before any runner call", () => {
  const { calls, project } = capture();
  assert.throws(
    () => project("test", "history-frontend"),
    /Invalid target: history-frontend/,
  );
  assert.deepEqual(calls, []);
});

test("history Stryker covers new modules and complete integration nodes with inherited gates", () => {
  const config = JSON.parse(
    readFileSync(resolve(root, "frontend/stryker.history.config.json"), "utf8"),
  );
  assert.deepEqual(config.mutate, [
    "src/history-api.ts",
    "src/history.tsx",
    "src/App.tsx:36:8-36:57",
    "src/App.tsx:75:30-81:40",
    "src/App.tsx:118:10-151:7",
    "src/workspace.tsx:69:10-74:22",
    "src/project-reader.tsx:111:10-116:22",
    "src/task-reader.tsx:112:10-117:22",
  ]);
  assert.equal(config.testRunner, "vitest");
  assert.deepEqual(config.vitest, { configFile: "vite.config.ts" });
  assert.equal(config.coverageAnalysis, "perTest");
  assert.equal(config.concurrency, 8);
  assert.deepEqual(config.thresholds, { high: 90, low: 80, break: 80 });
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-*"]);
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-history/mutation.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-history/mutation.html",
  );
  assert.equal(config.tempDirName, ".stryker-tmp-history");
  assert.equal(config.mutator, undefined);
});

test("default Stryker retains history modules and full new integration nodes", () => {
  const config = JSON.parse(
    readFileSync(resolve(root, "frontend/stryker.config.json"), "utf8"),
  );
  for (const entry of [
    "src/history-api.ts",
    "src/history.tsx",
    "src/App.tsx:25:12-31:22",
    "src/App.tsx:38:10-62:7",
    "src/workspace.tsx:55:10-60:22",
    "src/project-reader.tsx:111:10-116:22",
  ])
    assert.ok(
      config.mutate.includes(entry) ||
        config.mutate.includes(entry.split(":")[0]),
      entry,
    );
  assert.ok(config.mutate.includes("src/task-reader.tsx"));
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-*"]);
  assert.equal(config.thresholds.break, 80);
});

test("weekly review backend invokes only its fixed PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "weekly_review-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=weekly_review"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("weekly review frontend invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "weekly_review-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.weekly-review.config.json",
      ],
    ],
  ]);
});

test("weekly review PIT includes complete new modules and wiring with all JUnit candidates", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const selected = build.match(
    /val weeklyReviewClasses = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(selected);
  assert.deepEqual(
    [...selected.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
    [
      "application.ReadWeeklyReview",
      "application.ReadWeeklyReviewUseCase",
      "application.WeeklyReviewQueries",
      "domain.WeeklyReview*",
      "adapter.persistence.PostgresWeeklyReviewQueries*",
      "adapter.http.WeeklyReviewController*",
      "adapter.config.ApplicationConfiguration",
    ].map((name) => `com.apptolast.organization.${name}`),
  );
  assert.match(build, /val weeklyReviewOnly = scope == "weekly_review"/);
  assert.match(build, /weeklyReviewOnly -> weeklyReviewClasses/);
  assert.match(
    build,
    /weeklyReviewOnly -> setOf\("com\.apptolast\.organization\.\*"\)/,
  );
  assert.match(build, /else -> [^\n]+ \+ weeklyReviewClasses/);
  assert.match(
    build,
    /if \(weeklyReviewOnly\) reportDir\.set\(layout\.buildDirectory\.dir\("reports\/pitest-weekly-review"\)\)/,
  );
  assert.match(build, /mutationThreshold\.set\(80\)/);
  assert.match(
    build,
    /threads\.set\(if \(integrationApiOnly \|\| integrationApiHttpOnly\) 8 else 4\)/,
  );
});

test("default PIT exposes weekly review adapter tests for the newly selected classes", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const selected = build.match(
    /val weeklyReviewAdapterTests = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(selected);
  assert.deepEqual(
    [...selected.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
    [
      "com.apptolast.organization.adapter.WeeklyReviewApiTest",
      "com.apptolast.organization.adapter.persistence.WeeklyReview*Test",
      "com.apptolast.organization.adapter.config.ApplicationWiringTest",
    ],
  );
  assert.match(
    build,
    /else -> core \+ authenticationTests[^\n]+ \+ weeklyReviewAdapterTests/,
  );
});

test("appearance backend invokes only its fixed PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "appearance-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=appearance"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("appearance PIT includes all new modules and makes their tests available by default", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const selected = build.match(
    /val appearanceClasses = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(selected);
  assert.deepEqual(
    [...selected.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
    [
      "application.ReadAppearance",
      "application.ReadAppearanceUseCase",
      "application.SaveAppearance",
      "application.SaveAppearanceUseCase",
      "application.AppearanceQueries",
      "application.AppearanceEditing",
      "application.AppearanceConflictException",
      "domain.Appearance*",
      "adapter.persistence.PostgresAppearanceStore*",
      "adapter.http.AppearanceController*",
      "adapter.config.ApplicationConfiguration",
    ].map((name) => `com.apptolast.organization.${name}`),
  );
  assert.match(build, /val appearanceOnly = scope == "appearance"/);
  assert.match(build, /appearanceOnly -> appearanceClasses/);
  assert.match(
    build,
    /appearanceOnly -> setOf\("com\.apptolast\.organization\.\*"\)/,
  );
  assert.match(build, /else -> [^\n]+ \+ appearanceClasses/);
  const tests = build.match(
    /val appearanceAdapterTests = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(tests);
  assert.deepEqual(
    [...tests.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
    [
      "com.apptolast.organization.adapter.AppearanceApiTest",
      "com.apptolast.organization.adapter.persistence.Appearance*Test",
      "com.apptolast.organization.adapter.config.ApplicationWiringTest",
    ],
  );
  assert.match(
    build,
    /else -> core \+ authenticationTests[^\n]+ \+ appearanceAdapterTests/,
  );
  assert.match(
    build,
    /if \(appearanceOnly\) reportDir\.set\(layout\.buildDirectory\.dir\("reports\/pitest-appearance"\)\)/,
  );
  assert.match(build, /mutationThreshold\.set\(80\)/);
  assert.match(
    build,
    /threads\.set\(if \(integrationApiOnly \|\| integrationApiHttpOnly\) 8 else 4\)/,
  );
});

test("appearance frontend invokes only its fixed Stryker configuration", () => {
  const { project, calls } = capture();
  project("mutate", "appearance-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.appearance.config.json",
      ],
    ],
  ]);
});

test("appearance Stryker preserves all candidates and reviewed integration nodes", () => {
  const config = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.appearance.config.json"),
      "utf8",
    ),
  );
  // La FORMA del ambito, sin repetir las coordenadas. Una lista literal de rangos
  // linea:columna dice lo mismo que la validacion por contenido de mas abajo, pero
  // se desplaza cada vez que alguien anade o quita una linea en App.tsx o en
  // workspace.tsx: la guarda se quedaba roja sin que nada estuviera mal. Paso el 10
  // de septiembre de 2026 al retirar tres features. Lo que de verdad hay que fijar
  // es que los tramos apunten al texto correcto, y de eso se encarga el bucle de
  // abajo.
  assert.equal(config.mutate.length, 9);
  assert.deepEqual(config.mutate.slice(0, 3), [
    "src/appearance-api.ts",
    "src/appearance-state.tsx",
    "src/appearance.tsx",
  ]);
  const ficherosDeRango = [
    "src/App.tsx",
    "src/App.tsx",
    "src/App.tsx",
    "src/workspace.tsx",
    "src/session-gate.tsx",
    "src/use-session.ts",
  ];
  for (const [posicion, selector] of config.mutate.slice(3).entries()) {
    assert.match(selector, /:\d+:\d+-\d+:\d+$/, selector);
    assert.ok(
      selector.startsWith(ficherosDeRango[posicion] + ":"),
      `${selector} deberia apuntar a ${ficherosDeRango[posicion]}`,
    );
  }
  assert.deepEqual(config.thresholds, { high: 90, low: 80, break: 80 });
  assert.equal(config.concurrency, 8);
  assert.equal(config.coverageAnalysis, "perTest");
  assert.deepEqual(config.vitest, { configFile: "vite.config.ts" });
  assert.deepEqual(config.plugins, ["@stryker-mutator/vitest-runner"]);
  assert.equal(config.tempDirName, ".stryker-tmp-appearance");
  assert.equal(
    config.jsonReporter.fileName,
    "reports/mutation-appearance/mutation.json",
  );
  assert.equal(
    config.htmlReporter.fileName,
    "reports/mutation-appearance/mutation.html",
  );
  assert.deepEqual(config.ignorePatterns, [".stryker-tmp-*"]);
  assert.match(
    readFileSync(resolve(root, "frontend/vite.config.ts"), "utf8"),
    /src\/\*\*\/\*\.test\.\{ts,tsx\}/,
  );
  const expectedStarts = [
    "appearance =",
    "appearance",
    "appearance",
    "<RouteLink",
    "if (session?.authenticated",
    "function isPrivateRoute",
  ];
  for (const [index, selector] of config.mutate.slice(3).entries()) {
    const [, path, startLine, startColumn, endLine, endColumn] = selector.match(
      /^(.+):(\d+):(\d+)-(\d+):(\d+)$/,
    );
    const lines = readFileSync(resolve(root, "frontend", path), "utf8").split(
      /\r?\n/,
    );
    const selected = lines.slice(Number(startLine) - 1, Number(endLine));
    selected[selected.length - 1] = selected.at(-1).slice(0, Number(endColumn));
    selected[0] = selected[0].slice(Number(startColumn));
    assert.ok(selected.join("\n").startsWith(expectedStarts[index]), selector);
    assert.ok(
      selected
        .join("\n")
        .includes(
          index === 4
            ? "AppearanceProvider"
            : index === 5
              ? "/apariencia"
              : index === 3
                ? "/apariencia"
                : index === 0
                  ? "/apariencia"
                  : index === 1
                    ? "Apariencia"
                    : "<Appearance />",
        ),
      selector,
    );
  }
});

test("external calendar targets reject other tasks and injected options before execution", () => {
  const { calls, project } = capture();
  for (const target of [
    "external_calendar-backend",
    "external_calendar-frontend",
  ]) {
    assert.throws(() => project("test", target), /Invalid target/);
    assert.throws(
      () => project("mutate", `${target} -PmutationScope=other`),
      /Invalid target/,
    );
    assert.throws(() => project("mutate", `${target}-extra`), /Invalid target/);
  }
  assert.deepEqual(calls, []);
});

test("external calendar backend mutation runs only its PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "external_calendar-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=external_calendar"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("external calendar frontend mutation invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "external_calendar-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.external-calendar.config.json",
      ],
    ],
  ]);
});

test("external calendar PIT scope covers the whole slice and extends the default", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const selected = build.match(
    /val externalCalendarClasses = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(selected);
  assert.deepEqual(
    [...selected.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
    [
      "domain.IcsFeed*",
      "domain.ExternalCalendarInput*",
      "domain.ExternalCalendarSnapshot*",
      "domain.ExternalCalendarSubscription*",
      "domain.ExternalEvent*",
      "domain.ExternalEventsRange*",
      "domain.SyncSummary*",
      "application.SyncExternalCalendar*",
      "application.SaveExternalCalendar*",
      "application.ReadExternalCalendar*",
      "application.ReadExternalCalendarEvents*",
      "application.DeleteExternalCalendar*",
      "application.OutboundHostGuard*",
      "application.PublicAddressPolicy*",
      "application.AddressPolicy*",
      "application.ExternalEventsView*",
      "application.ExternalCalendarUseCases*",
      "application.ExternalCalendarNotConfiguredException*",
      "adapter.connectors.AesGcmSecretCipher*",
      "adapter.connectors.ConnectorKeyRing*",
      "adapter.feed.HttpCalendarFeed*",
      "adapter.net.SystemHostResolver*",
      "adapter.net.AnchoredConnection*",
      "adapter.http.ExternalCalendarController*",
      "adapter.http.ConnectorsGate*",
      "adapter.persistence.PostgresExternalCalendarStore*",
      "adapter.logging.Slf4jExternalCalendarAudit*",
    ].map((name) => `com.apptolast.organization.${name}`),
  );
  assert.match(
    build,
    /val externalCalendarOnly = scope == "external_calendar"/,
  );
  assert.match(build, /externalCalendarOnly -> externalCalendarClasses/);
  assert.match(
    build,
    /externalCalendarOnly -> setOf\("com\.apptolast\.organization\.\*"\)/,
  );
  assert.match(build, /else -> core \+ [^\n]*externalCalendarClasses/);
});

test("external calendar Stryker configuration mutates only its own files", () => {
  const configuration = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.external-calendar.config.json"),
      "utf8",
    ),
  );
  // La FORMA del ambito, sin repetir las coordenadas. Una lista literal de rangos
  // linea:columna dice lo mismo que la validacion por contenido de mas abajo, pero
  // se desplaza cada vez que alguien anade o quita una linea en App.tsx o en
  // workspace.tsx: la guarda se quedaba roja sin que nada estuviera mal. Paso el 10
  // de septiembre de 2026 al retirar tres features. Lo que de verdad hay que fijar
  // es que los tramos apunten al texto correcto, y de eso se encarga el bucle de
  // abajo.
  assert.equal(configuration.mutate.length, 7);
  assert.deepEqual(configuration.mutate.slice(0, 3), [
    "src/external-calendar-api.ts",
    "src/external-calendar.tsx",
    "src/today-external-calendar.tsx",
  ]);
  for (const selector of configuration.mutate.slice(3))
    assert.match(
      selector,
      /^src\/(App|workspace)\.tsx:\d+:\d+-\d+:\d+$/,
      selector,
    );
  assert.equal(configuration.thresholds.break, 80);
  assert.equal(configuration.testRunner, "vitest");
});

// Hallazgo 18 del dictamen: los cuatro rangos de arriba solo valen si siguen
// apuntando al tramo que @s37 exige (la ruta, su rama de seccion, su rama de
// render y la entrada de navegacion). Un rango desfasado mutaria codigo ajeno en
// silencio, asi que aqui se recorta el fuente por el rango y se comprueba que lo
// recortado es de verdad ese tramo.
test("external calendar Stryker ranges still cover the route and the navigation entry (@s37)", () => {
  const configuration = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.external-calendar.config.json"),
      "utf8",
    ),
  );
  const slice = (range) => {
    const parsed = /^(.+):(\d+):(\d+)-(\d+):(\d+)$/.exec(range);
    assert.ok(parsed, `rango ilegible: ${range}`);
    const [, file, startLine, startColumn, endLine, endColumn] = parsed.map(
      (part, index) => (index > 1 ? Number(part) : part),
    );
    const lines = readFileSync(resolve(root, "frontend", file), "utf8").split(
      "\n",
    );
    if (startLine === endLine)
      return lines[startLine - 1].slice(startColumn, endColumn);
    return [
      lines[startLine - 1].slice(startColumn),
      ...lines.slice(startLine, endLine - 1),
      lines[endLine - 1].slice(0, endColumn),
    ].join("\n");
  };
  const ranges = configuration.mutate.filter((entry) => /:\d+:/.test(entry));
  assert.equal(ranges.length, 4, "faltan tramos de App.tsx o workspace.tsx");
  const [route, section, render, navigation] = ranges.map(slice);
  assert.equal(route, 'externalCalendar = route === "/calendario-externo"');
  assert.match(section, /^externalCalendar\n\s+\? "Calendario externo"/);
  assert.match(section, /: null$/);
  assert.match(
    render,
    /^externalCalendar && username \? \(\n\s+<ExternalCalendar \/>/,
  );
  assert.match(navigation, /^<RouteLink\n\s+href="\/calendario-externo"/);
  assert.match(
    navigation,
    /aria-current=\{section === "Calendario externo" \? "page" : undefined\}/,
  );
  assert.match(navigation, /<\/RouteLink>$/);
});

test("the end to end stack enables the connectors with an explicit key and keeps the SSRF guard", () => {
  const compose = readFileSync(resolve(root, "docker-compose.yml"), "utf8");
  assert.match(compose, /APP_CONNECTOR_KEY: \$\{APP_CONNECTOR_KEY:-\}/);
  assert.match(
    compose,
    /APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES: \$\{APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES:-false\}/,
  );
  const harness = readFileSync(resolve(root, "scripts/e2e.mjs"), "utf8");
  const key = harness.match(/const connectorKey = "([^"]+)";/)?.[1];
  assert.ok(key, "el arnés de extremo a extremo debe fijar APP_CONNECTOR_KEY");
  assert.match(harness, /APP_CONNECTOR_KEY: connectorKey,/);
  assert.equal(Buffer.from(key, "base64").length, 32);
  assert.doesNotMatch(
    harness,
    /APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES: "true"/,
  );
});

// Guardas de la puerta de mutacion de la feature 25 (webhooks). La feature se
// integro en main sin alcance PIT, sin destinos en el arnes y sin configuracion
// de Stryker: su corte no recibia ni un mutante y nadie podia comprobar si sus
// pruebas muerden. Estas guardas sujetan las tres piezas.
test("webhooks targets reject other tasks and injected options before execution", () => {
  const { calls, project } = capture();
  for (const target of ["webhooks-backend", "webhooks-frontend"]) {
    assert.throws(() => project("test", target), /Invalid target/);
    assert.throws(
      () => project("mutate", `${target} -PmutationScope=other`),
      /Invalid target/,
    );
    assert.throws(() => project("mutate", `${target}-extra`), /Invalid target/);
  }
  assert.deepEqual(calls, []);
});

test("webhooks backend mutation runs only its PIT scope", () => {
  const { calls, project } = capture();
  project("mutate", "webhooks-backend");
  assert.deepEqual(calls, [
    [
      process.platform === "win32" ? "gradlew.bat" : "./gradlew",
      ["pitest", "--no-daemon", "-PmutationScope=webhooks"],
      { cwd: resolve(root, "backend"), shell: process.platform === "win32" },
    ],
  ]);
});

test("webhooks frontend mutation invokes only its fixed Stryker configuration", () => {
  const { calls, project } = capture();
  project("mutate", "webhooks-frontend");
  assert.deepEqual(calls, [
    [
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "stryker",
        "run",
        "stryker.webhooks.config.json",
      ],
    ],
  ]);
});

test("webhooks PIT scope covers the whole slice and extends the default", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const selected = build.match(
    /val webhooksClasses = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  assert.ok(selected);
  assert.deepEqual(
    [...selected.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]),
    [
      "domain.RetrySchedule*",
      "domain.WebhookAttempt*",
      "domain.WebhookCursor*",
      "domain.WebhookDelivery*",
      "domain.WebhookEndpoint*",
      "domain.WebhookIntent*",
      "domain.WebhookInvalidException*",
      "domain.WebhookPingPayload*",
      "application.ClaimedDelivery*",
      "application.CreateWebhook*",
      "application.DispatchWebhooks*",
      "application.EnqueueWebhookDeliveries*",
      "application.ManageWebhook*",
      "application.OutboxCandidate*",
      "application.ReadyEndpoint*",
      "application.WebhookAudit*",
      "application.WebhookCreation*",
      "application.WebhookDeliveries*",
      "application.WebhookDestinationGuard*",
      "application.WebhookEndpoints*",
      "application.WebhookOperationException*",
      "application.WebhookOutbox*",
      "application.WebhookSecrets*",
      "application.WebhookSender*",
      "application.WebhookWork*",
      "adapter.webhook.JdkWebhookSender*",
      "adapter.webhook.AesGcmWebhookSecrets*",
      "adapter.webhook.WebhookSignature*",
      "adapter.http.WebhookController*",
      "adapter.http.WebhookDeliveryView*",
      "adapter.http.WebhookEndpointView*",
      "adapter.persistence.PostgresWebhookStore*",
      "adapter.persistence.PostgresWebhookOutbox*",
      "adapter.persistence.PostgresWebhookWork*",
      "adapter.logging.Slf4jWebhookAudit*",
      "adapter.config.WebhookConfiguration*",
      "adapter.config.WebhookConnectorStartup*",
      "adapter.config.WebhookSchedule*",
      "adapter.net.AnchoredConnection*",
    ].map((name) => `com.apptolast.organization.${name}`),
  );
  // AddressPolicy y PublicAddressPolicy son compartidas y ya viven en el ambito
  // del calendario externo: nombrarlas aqui duplicaria la campana.
  assert.doesNotMatch(selected, /AddressPolicy/);
  // WebhookEndpointLookup, WebhookEndpointNotFoundException y NotifyWebhookAction
  // son de la feature 30, no de esta: las cubre automationsClasses.
  assert.doesNotMatch(
    selected,
    /WebhookEndpointLookup|WebhookEndpointNotFound/,
  );
  assert.doesNotMatch(selected, /NotifyWebhookAction/);
  assert.match(build, /val webhooksOnly = scope == "webhooks"/);
  assert.match(build, /webhooksOnly -> webhooksClasses/);
  assert.match(
    build,
    /webhooksOnly -> setOf\("com\.apptolast\.organization\.\*"\)/,
  );
  assert.match(
    build,
    /if \(webhooksOnly\) reportDir\.set\(layout\.buildDirectory\.dir\("reports\/pitest-webhooks"\)\)/,
  );
  // Sin la union en el perfil por defecto el ambito queda fuera de la campana global.
  assert.match(build, /else -> core \+ [^\n]*webhooksClasses/);
});

test("webhooks Stryker configuration mutates only the feature files", () => {
  const configuration = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.webhooks.config.json"),
      "utf8",
    ),
  );
  // Ficheros enteros y ningun rango linea:columna: un rango desfasado no falla,
  // muta el codigo equivocado en silencio.
  assert.deepEqual(configuration.mutate, [
    "src/webhooks-client.ts",
    "src/webhooks.tsx",
  ]);
  for (const entry of configuration.mutate) assert.doesNotMatch(entry, /:/);
  assert.equal(configuration.thresholds.break, 80);
  assert.equal(configuration.testRunner, "vitest");
  // Directorio temporal e informe propios: no pisan los de otra feature.
  assert.equal(configuration.tempDirName, ".stryker-tmp-webhooks");
  assert.equal(
    configuration.jsonReporter.fileName,
    "reports/mutation-webhooks/mutation.json",
  );
});

// Un patron que no casa con ninguna clase no aborta PIT: lo descarta en silencio
// y la campana cierra en verde sobre un corte que nunca recibio un mutante. Le
// paso al ambito del calendario externo con adapter.crypto.*; aqui no.
test("every webhooks mutation pattern resolves to a file that exists today", () => {
  const build = readFileSync(resolve(root, "backend/build.gradle.kts"), "utf8");
  const selected = build.match(
    /val webhooksClasses = setOf\(([\s\S]*?)\n    \)/,
  )?.[1];
  const unresolved = [...selected.matchAll(/"([^"]+)"/g)]
    .map((entry) => entry[1])
    .filter(
      (pattern) =>
        !existsSync(
          resolve(
            root,
            "backend/src/main/java",
            `${pattern.replace(/\*$/, "").replaceAll(".", "/")}.java`,
          ),
        ),
    );
  assert.deepEqual(unresolved, []);
  const configuration = JSON.parse(
    readFileSync(
      resolve(root, "frontend/stryker.webhooks.config.json"),
      "utf8",
    ),
  );
  for (const file of configuration.mutate) {
    assert.ok(
      existsSync(resolve(root, "frontend", file)),
      `${file} no existe: Stryker mutaria la nada`,
    );
  }
});
