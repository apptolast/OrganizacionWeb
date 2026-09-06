package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
    properties = {
      "app.auth.username=persona-a",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@AutoConfigureMockMvc
class AvailabilityApiTest {
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static {
    // PIT repeats JUnit class lifecycles inside one JVM. Keep the database endpoint stable
    // for Spring's cached context; Ryuk removes this JVM-owned container on exit.
    postgres.start();
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;

  @Test
  void s1_readsUnconfiguredWithoutCreatingPreference() throws Exception {
    var response =
        mvc.perform(get("/api/v1/me/availability").with(user("persona-a")))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"availability:unconfigured\""))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    var body = json.readTree(response.getContentAsString());
    assertThat(body.size()).isEqualTo(4);
    assertThat(body.get("configured").booleanValue()).isFalse();
    for (String field : java.util.List.of("zoneId", "dailyMinutes", "updatedAt"))
      assertThat(body.get(field).isNull()).isTrue();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Long.class))
        .isZero();
  }

  @Test
  void s2_exposesExactSortedBackendCatalog() throws Exception {
    var response =
        mvc.perform(get("/api/v1/me/availability/zones").with(user("persona-a")))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var body = json.readTree(response.getContentAsString());
    assertThat(body.size()).isEqualTo(1);
    var expected = new java.util.TreeSet<>(java.time.ZoneId.getAvailableZoneIds());
    expected.add("UTC");
    assertThat(json.convertValue(body.get("items"), java.util.List.class))
        .containsExactlyElementsOf(expected);
  }

  @BeforeEach
  void reset() {
    jdbc.execute(
        "TRUNCATE planned_blocks,availability_preferences,task_status_history,tasks,outbox_events,projects");
  }

  String body(String zone, int minutes) {
    var root = json.createObjectNode().put("zoneId", zone);
    var days = root.putObject("dailyMinutes");
    for (var day : java.time.DayOfWeek.values()) days.put(day.name(), minutes);
    return root.toString();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"Europe/Madrid", "UTC"})
  void s3_confirmsFirstPreferenceAndSameGetSnapshot(String zone) throws Exception {
    var response =
        mvc.perform(
                put("/api/v1/me/availability")
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .header("If-Match", "\"availability:unconfigured\"")
                    .contentType("application/json")
                    .content(body(zone, 60)))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var stored = jdbc.queryForMap("SELECT * FROM availability_preferences");
    assertThat(stored.get("owner_id")).isEqualTo("persona-a");
    assertThat(stored.get("version")).isEqualTo(0L);
    assertThat(stored.get("created_at")).isEqualTo(stored.get("updated_at"));
    assertThat(response.getHeader("ETag")).isEqualTo("\"availability:" + stored.get("id") + ":0\"");
    var result = json.readTree(response.getContentAsString());
    assertThat(result.size()).isEqualTo(4);
    assertThat(result.get("configured").booleanValue()).isTrue();
    assertThat(result.get("zoneId").textValue()).isEqualTo(zone);
    assertThat(result.get("dailyMinutes").size()).isEqualTo(7);
    for (var day : java.time.DayOfWeek.values())
      assertThat(result.get("dailyMinutes").get(day.name()).intValue()).isEqualTo(60);
    assertThat(java.time.Instant.parse(result.get("updatedAt").textValue()))
        .isEqualTo(((java.sql.Timestamp) stored.get("updated_at")).toInstant());
    var read =
        mvc.perform(get("/api/v1/me/availability").with(user("persona-a")))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(json.readTree(read.getContentAsString())).isEqualTo(result);
    assertThat(read.getHeader("ETag")).isEqualTo(response.getHeader("ETag"));
  }

  org.springframework.test.web.servlet.ResultActions save(String tag, String zone, int minutes)
      throws Exception {
    return mvc.perform(
        put("/api/v1/me/availability")
            .with(user("persona-a"))
            .with(csrf().asHeader())
            .header("If-Match", tag)
            .contentType("application/json")
            .content(body(zone, minutes)));
  }

  @Test
  void s7_updatesOnlyContentAndNextRevision() throws Exception {
    var first =
        save("\"availability:unconfigured\"", "Europe/Madrid", 60)
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var before = jdbc.queryForMap("SELECT * FROM availability_preferences");
    var response =
        save(first.getHeader("ETag"), "UTC", 30)
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var stored = jdbc.queryForMap("SELECT * FROM availability_preferences");
    assertThat(stored.get("id")).isEqualTo(before.get("id"));
    assertThat(stored.get("owner_id")).isEqualTo(before.get("owner_id"));
    assertThat(stored.get("created_at")).isEqualTo(before.get("created_at"));
    assertThat(stored.get("version")).isEqualTo(1L);
    assertThat(response.getHeader("ETag")).isEqualTo("\"availability:" + stored.get("id") + ":1\"");
    var result = json.readTree(response.getContentAsString());
    assertThat(result.get("zoneId").textValue()).isEqualTo("UTC");
    for (var day : java.time.DayOfWeek.values())
      assertThat(result.get("dailyMinutes").get(day.name()).intValue()).isEqualTo(30);
  }

  @Test
  void s8_noOpDoesNotIssueAnUpdate() throws Exception {
    var first =
        save("\"availability:unconfigured\"", "UTC", 60)
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var before = jdbc.queryForMap("SELECT * FROM availability_preferences");
    jdbc.execute(
        "CREATE FUNCTION reject_availability_noop() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'No write expected'; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_availability_noop BEFORE UPDATE ON availability_preferences FOR EACH ROW EXECUTE FUNCTION reject_availability_noop()");
    try {
      var same =
          save(first.getHeader("ETag"), "UTC", 60)
              .andExpect(
                  header()
                      .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse();
      assertThat(same.getContentAsString()).isEqualTo(first.getContentAsString());
      assertThat(same.getHeader("ETag")).isEqualTo(first.getHeader("ETag"));
      assertThat(jdbc.queryForMap("SELECT * FROM availability_preferences")).isEqualTo(before);
    } finally {
      jdbc.execute("DROP TRIGGER reject_availability_noop ON availability_preferences");
      jdbc.execute("DROP FUNCTION reject_availability_noop()");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "missing",
        "empty",
        "star",
        "weak",
        "repeated",
        "list",
        "project",
        "partial",
        "uppercase",
        "negative",
        "zeros",
        "fraction",
        "overflow"
      })
  void s10_rejectsAmbiguousPreconditionBeforeMalformedBody(String defect) throws Exception {
    String tag = "\"availability:abcdef01-2345-6789-abcd-0123456789ab:0\"";
    switch (defect) {
      case "empty" -> tag = "";
      case "star" -> tag = "*";
      case "weak" -> tag = "W/" + tag;
      case "list" -> tag = tag + "," + tag;
      case "project" -> tag = "\"abcdef01-2345-6789-abcd-0123456789ab:0\"";
      case "partial" -> tag = "\"availability:1-1-1-1-1:0\"";
      case "uppercase" -> tag = "\"availability:ABCDEF01-2345-6789-ABCD-0123456789AB:0\"";
      case "negative" -> tag = tag.replace(":0", ":-1");
      case "zeros" -> tag = tag.replace(":0", ":01");
      case "fraction" -> tag = tag.replace(":0", ":1.5");
      case "overflow" -> tag = tag.replace(":0", ":9223372036854775808");
      default -> {}
    }
    var request =
        put("/api/v1/me/availability")
            .with(user("persona-a"))
            .with(csrf().asHeader())
            .contentType("application/json")
            .content("{");
    if (!defect.equals("missing")) request.header("If-Match", tag);
    if (defect.equals("repeated")) request.header("If-Match", tag);
    var result =
        mvc.perform(request)
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().is(defect.equals("missing") ? 428 : 400))
            .andExpect(
                jsonPath("$.code")
                    .value(
                        defect.equals("missing") ? "PRECONDITION_REQUIRED" : "VALIDATION_ERROR"));
    if (!defect.equals("missing"))
      result
          .andExpect(jsonPath("$.errors[0].field").value("If-Match"))
          .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Long.class))
        .isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"oldChanged", "oldNoOp", "unconfigured", "foreign", "missingId", "missingRow"})
  void s11_returnsConflictWithoutChangingOwnOrForeignRows(String defect) throws Exception {
    String tag = "\"availability:" + UUID.randomUUID() + ":0\"";
    java.util.List<java.util.Map<String, Object>> before;
    if (!defect.equals("missingRow")) {
      var first =
          save("\"availability:unconfigured\"", "UTC", 60)
              .andExpect(
                  header()
                      .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse();
      if (defect.startsWith("old")) {
        tag = first.getHeader("ETag");
        save(tag, "UTC", 30)
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk());
      }
      if (defect.equals("unconfigured")) tag = "\"availability:unconfigured\"";
      if (defect.equals("foreign")) {
        var foreign =
            mvc.perform(
                    put("/api/v1/me/availability")
                        .with(user("persona-b"))
                        .with(csrf().asHeader())
                        .header("If-Match", "\"availability:unconfigured\"")
                        .contentType("application/json")
                        .content(body("UTC", 90)))
                .andExpect(
                    header()
                        .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        tag = foreign.getHeader("ETag");
      }
    }
    before = jdbc.queryForList("SELECT * FROM availability_preferences ORDER BY owner_id");
    save(tag, "UTC", defect.equals("oldNoOp") ? 30 : 60)
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("AVAILABILITY_CONFLICT"));
    assertThat(jdbc.queryForList("SELECT * FROM availability_preferences ORDER BY owner_id"))
        .isEqualTo(before);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "empty",
        "blank",
        "truncated",
        "trailing",
        "zoneId",
        "dailyMinutes",
        "MONDAY",
        "TUESDAY",
        "WEDNESDAY",
        "THURSDAY",
        "FRIDAY",
        "SATURDAY",
        "SUNDAY"
      })
  void s12_rejectsMalformedAndDuplicateJson(String defect) throws Exception {
    String raw = body("UTC", 60);
    switch (defect) {
      case "empty" -> raw = "";
      case "blank" -> raw = "   ";
      case "truncated" -> raw = "{";
      case "trailing" -> raw = raw + " {}";
      case "zoneId" ->
          raw = raw.replace("\"zoneId\":\"UTC\"", "\"zoneId\":\"UTC\",\"zoneId\":\"CET\"");
      case "dailyMinutes" ->
          raw = raw.replace("\"dailyMinutes\":", "\"dailyMinutes\":{},\"dailyMinutes\":");
      default ->
          raw = raw.replace("\"" + defect + "\":60", "\"" + defect + "\":60,\"" + defect + "\":30");
    }
    mvc.perform(
            put("/api/v1/me/availability")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("If-Match", "\"availability:unconfigured\"")
                .contentType("application/json")
                .content(raw))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Long.class))
        .isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "rootNull,body,INVALID_TYPE",
    "rootArray,body,INVALID_TYPE",
    "rootText,body,INVALID_TYPE",
    "rootNumber,body,INVALID_TYPE",
    "rootBool,body,INVALID_TYPE",
    "zoneMissing,zoneId,REQUIRED",
    "zoneNull,zoneId,REQUIRED",
    "zoneNumber,zoneId,INVALID_TYPE",
    "zoneBool,zoneId,INVALID_TYPE",
    "zoneArray,zoneId,INVALID_TYPE",
    "zoneObject,zoneId,INVALID_TYPE",
    "zoneEmpty,zoneId,INVALID_VALUE",
    "zoneSpaces,zoneId,INVALID_VALUE",
    "zoneCase,zoneId,INVALID_VALUE",
    "zoneOffset,zoneId,INVALID_VALUE",
    "zonePrefixedOffset,zoneId,INVALID_VALUE",
    "zoneUnknown,zoneId,INVALID_VALUE",
    "daysMissing,dailyMinutes,REQUIRED",
    "daysNull,dailyMinutes,REQUIRED",
    "daysArray,dailyMinutes,INVALID_TYPE",
    "daysText,dailyMinutes,INVALID_TYPE",
    "daysNumber,dailyMinutes,INVALID_TYPE",
    "daysBool,dailyMinutes,INVALID_TYPE",
    "ownerId,ownerId,UNKNOWN_FIELD",
    "configured,configured,UNKNOWN_FIELD",
    "windows,windows,UNKNOWN_FIELD"
  })
  void s13_rejectsShapeAndPreferenceFields(String defect, String field, String code)
      throws Exception {
    var value = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(body("UTC", 60));
    String raw = null;
    switch (defect) {
      case "rootNull" -> raw = "null";
      case "rootArray" -> raw = "[]";
      case "rootText" -> raw = "\"UTC\"";
      case "rootNumber" -> raw = "42";
      case "rootBool" -> raw = "true";
      case "zoneMissing" -> value.remove("zoneId");
      case "zoneNull" -> value.putNull("zoneId");
      case "zoneNumber" -> value.put("zoneId", 42);
      case "zoneBool" -> value.put("zoneId", true);
      case "zoneArray" -> value.putArray("zoneId");
      case "zoneObject" -> value.putObject("zoneId");
      case "zoneEmpty" -> value.put("zoneId", "");
      case "zoneSpaces" -> value.put("zoneId", " Europe/Madrid ");
      case "zoneCase" -> value.put("zoneId", "europe/madrid");
      case "zoneOffset" -> value.put("zoneId", "+02:00");
      case "zonePrefixedOffset" -> value.put("zoneId", "UTC+02:00");
      case "zoneUnknown" -> value.put("zoneId", "Unknown/Zone");
      case "daysMissing" -> value.remove("dailyMinutes");
      case "daysNull" -> value.putNull("dailyMinutes");
      case "daysArray" -> value.putArray("dailyMinutes");
      case "daysText" -> value.put("dailyMinutes", "60");
      case "daysNumber" -> value.put("dailyMinutes", 60);
      case "daysBool" -> value.put("dailyMinutes", true);
      default -> value.put(defect, true);
    }
    expectInvalid(raw == null ? value.toString() : raw, field, code);
  }

  void expectInvalid(String raw, String field, String code) throws Exception {
    mvc.perform(
            put("/api/v1/me/availability")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("If-Match", "\"availability:unconfigured\"")
                .contentType("application/json")
                .content(raw))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Long.class))
        .isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("invalidDays")
  void s14_s15_rejectsEachMissingDayAndInvalidBudget(String day, String rawValue, String code)
      throws Exception {
    var value = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(body("UTC", 60));
    var daily = (com.fasterxml.jackson.databind.node.ObjectNode) value.get("dailyMinutes");
    if (rawValue.equals("missing")) daily.remove(day);
    else daily.set(day, json.readTree(rawValue));
    expectInvalid(value.toString(), "dailyMinutes." + day, code);
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> invalidDays() {
    var rows = new java.util.ArrayList<org.junit.jupiter.params.provider.Arguments>();
    for (var day : java.time.DayOfWeek.values()) {
      rows.add(org.junit.jupiter.params.provider.Arguments.of(day.name(), "missing", "REQUIRED"));
      rows.add(org.junit.jupiter.params.provider.Arguments.of(day.name(), "-1", "OUT_OF_RANGE"));
    }
    for (String raw :
        java.util.List.of(
            "null",
            "1441",
            "9223372036854775808",
            "1.5",
            "1.0",
            "\"60\"",
            "\"\"",
            "true",
            "[]",
            "{}"))
      rows.add(
          org.junit.jupiter.params.provider.Arguments.of(
              "MONDAY",
              raw,
              raw.equals("null")
                  ? "REQUIRED"
                  : raw.equals("1441") || raw.equals("9223372036854775808")
                      ? "OUT_OF_RANGE"
                      : "INVALID_TYPE"));
    return rows.stream();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"monday", "LUNES", "HOLIDAY", "weeklyTotal"})
  void s16_rejectsExtraDayKeysWithoutTranslation(String key) throws Exception {
    var value = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(body("UTC", 60));
    ((com.fasterxml.jackson.databind.node.ObjectNode) value.get("dailyMinutes")).put(key, 60);
    expectInvalid(value.toString(), "dailyMinutes." + key, "UNKNOWN_FIELD");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"GET", "zones", "PUT"})
  void s26_rejectsQueryBeforeMissingPrecondition(String operation) throws Exception {
    String path =
        "/api/v1/me/availability" + (operation.equals("zones") ? "/zones" : "") + "?limit=1";
    var request =
        operation.equals("PUT")
            ? put(path).with(csrf().asHeader()).contentType("application/json").content("{")
            : get(path);
    mvc.perform(request.with(user("persona-a")))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("query"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"read", "insertThrow", "insertZero", "updateThrow", "updateZero", "commit"})
  void s25_storageFailureNeverConfirmsOrChangesPreference(String fault) throws Exception {
    String tag = "\"availability:unconfigured\"";
    if (fault.startsWith("update") || fault.equals("commit"))
      tag =
          save(tag, "UTC", 60)
              .andExpect(
                  header()
                      .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getHeader("ETag");
    var otherTables = aggregateSnapshot();
    var before = jdbc.queryForList("SELECT * FROM availability_preferences ORDER BY owner_id");
    if (fault.equals("read"))
      jdbc.execute("ALTER TABLE availability_preferences RENAME TO availability_unavailable");
    else {
      String action =
          fault.endsWith("Zero")
              ? "RETURN NULL;"
              : "RAISE EXCEPTION 'synthetic storage rejection';";
      jdbc.execute(
          "CREATE FUNCTION reject_availability_write() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
              + action
              + " END $$");
      String definition =
          fault.equals("commit")
              ? "CREATE CONSTRAINT TRIGGER reject_availability_write AFTER UPDATE ON availability_preferences DEFERRABLE INITIALLY DEFERRED"
              : "CREATE TRIGGER reject_availability_write BEFORE "
                  + (fault.startsWith("insert") ? "INSERT" : "UPDATE")
                  + " ON availability_preferences";
      jdbc.execute(definition + " FOR EACH ROW EXECUTE FUNCTION reject_availability_write()");
    }
    try {
      var result =
          fault.equals("read")
              ? mvc.perform(get("/api/v1/me/availability").with(user("persona-a")))
              : save(tag, "UTC", 30);
      result
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
          .andExpect(header().doesNotExist("ETag"))
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    } finally {
      if (fault.equals("read"))
        jdbc.execute("ALTER TABLE availability_unavailable RENAME TO availability_preferences");
      else {
        jdbc.execute("DROP TRIGGER reject_availability_write ON availability_preferences");
        jdbc.execute("DROP FUNCTION reject_availability_write()");
      }
    }
    assertThat(jdbc.queryForList("SELECT * FROM availability_preferences ORDER BY owner_id"))
        .isEqualTo(before);
    assertThat(aggregateSnapshot()).isEqualTo(otherTables);
  }

  @Test
  void s19_firstCreationRaceHasOneWinnerWithoutOverwriting() throws Exception {
    try (var blocker =
            java.sql.DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      blocker.setAutoCommit(false);
      blocker.createStatement().execute("LOCK TABLE availability_preferences IN SHARE MODE");
      var first =
          executor.submit(
              () -> save("\"availability:unconfigured\"", "UTC", 30).andReturn().getResponse());
      var second =
          executor.submit(
              () -> save("\"availability:unconfigured\"", "CET", 90).andReturn().getResponse());
      try {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        int waiting = 0;
        while (System.nanoTime() < deadline) {
          waiting =
              jdbc.queryForObject(
                  "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE 'INSERT INTO availability_preferences%'",
                  Integer.class);
          if (waiting == 2) break;
          Thread.sleep(20);
        }
        assertThat(waiting).isEqualTo(2);
      } finally {
        blocker.commit();
      }
      var a = first.get(10, java.util.concurrent.TimeUnit.SECONDS);
      var b = second.get(10, java.util.concurrent.TimeUnit.SECONDS);
      assertThat(java.util.List.of(a.getStatus(), b.getStatus()))
          .containsExactlyInAnyOrder(200, 412);
      var winner = a.getStatus() == 200 ? a : b;
      var loser = a.getStatus() == 412 ? a : b;
      assertThat(json.readTree(loser.getContentAsString()).get("code").textValue())
          .isEqualTo("AVAILABILITY_CONFLICT");
      assertThat(
              jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Integer.class))
          .isEqualTo(1);
      var saved =
          mvc.perform(get("/api/v1/me/availability").with(user("persona-a")))
              .andReturn()
              .getResponse();
      assertThat(saved.getContentAsString()).isEqualTo(winner.getContentAsString());
      assertThat(saved.getHeader("ETag")).isEqualTo(winner.getHeader("ETag"));
      assertThat(jdbc.queryForObject("SELECT version FROM availability_preferences", Long.class))
          .isZero();
    }
  }

  @Test
  void s20_concurrentUpdatesShareOneRevision() throws Exception {
    String tag =
        save("\"availability:unconfigured\"", "UTC", 60)
            .andReturn()
            .getResponse()
            .getHeader("ETag");
    try (var blocker =
            java.sql.DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      blocker.setAutoCommit(false);
      blocker.createStatement().execute("SELECT * FROM availability_preferences FOR UPDATE");
      var first = executor.submit(() -> save(tag, "UTC", 30).andReturn().getResponse());
      var second = executor.submit(() -> save(tag, "CET", 90).andReturn().getResponse());
      try {
        awaitBlockedReaders(2);
      } finally {
        blocker.commit();
      }
      var a = first.get(10, java.util.concurrent.TimeUnit.SECONDS);
      var b = second.get(10, java.util.concurrent.TimeUnit.SECONDS);
      assertThat(java.util.List.of(a.getStatus(), b.getStatus()))
          .containsExactlyInAnyOrder(200, 412);
      var winner = a.getStatus() == 200 ? a : b;
      var actual =
          mvc.perform(get("/api/v1/me/availability").with(user("persona-a")))
              .andReturn()
              .getResponse();
      assertThat(actual.getContentAsString()).isEqualTo(winner.getContentAsString());
      assertThat(actual.getHeader("ETag")).isEqualTo(winner.getHeader("ETag"));
      assertThat(jdbc.queryForObject("SELECT version FROM availability_preferences", Long.class))
          .isEqualTo(1);
    }
  }

  void awaitBlockedReaders(int count) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
    int waiting = 0;
    while (System.nanoTime() < deadline) {
      waiting =
          jdbc.queryForObject(
              "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE 'SELECT * FROM availability_preferences WHERE owner_id=%FOR UPDATE'",
              Integer.class);
      if (waiting == count) break;
      Thread.sleep(20);
    }
    assertThat(waiting).isEqualTo(count);
  }

  @Test
  void s21_waitingOldNoOpMustConflictAfterWriterCommits() throws Exception {
    String tag =
        save("\"availability:unconfigured\"", "UTC", 60)
            .andReturn()
            .getResponse()
            .getHeader("ETag");
    var store =
        new com.apptolast.organization.adapter.persistence.PostgresAvailabilityStore(
            jdbc,
            new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                    jdbc.getDataSource())));
    var acquired = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var writer =
          executor.submit(
              () ->
                  store.save(
                      "persona-a",
                      previous -> {
                        acquired.countDown();
                        try {
                          if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                            throw new AssertionError("Writer release timed out");
                        } catch (InterruptedException error) {
                          throw new AssertionError(error);
                        }
                        var old = previous.orElseThrow();
                        var days =
                            new java.util.EnumMap<java.time.DayOfWeek, Integer>(
                                java.time.DayOfWeek.class);
                        for (var day : java.time.DayOfWeek.values()) days.put(day, 30);
                        return new com.apptolast.organization.domain.Availability(
                            old.id(),
                            old.ownerId(),
                            "UTC",
                            days,
                            old.version() + 1,
                            old.createdAt(),
                            old.updatedAt());
                      }));
      assertThat(acquired.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      var stale = executor.submit(() -> save(tag, "UTC", 60).andReturn().getResponse());
      try {
        awaitBlockedReaders(1);
      } finally {
        release.countDown();
      }
      writer.get(10, java.util.concurrent.TimeUnit.SECONDS);
      var result = stale.get(10, java.util.concurrent.TimeUnit.SECONDS);
      assertThat(result.getStatus()).isEqualTo(412);
      assertThat(json.readTree(result.getContentAsString()).get("code").textValue())
          .isEqualTo("AVAILABILITY_CONFLICT");
      assertThat(
              jdbc.queryForObject(
                  "SELECT monday_minutes FROM availability_preferences", Integer.class))
          .isEqualTo(30);
      assertThat(jdbc.queryForObject("SELECT version FROM availability_preferences", Long.class))
          .isEqualTo(1);
    } finally {
      release.countDown();
    }
  }

  @Autowired org.springframework.session.SessionRepository sessions;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "GET,false",
    "GET,true",
    "zones,false",
    "zones,true",
    "PUT,false",
    "PUT,true"
  })
  void s23_requiresSessionIncludingPersistedExpiration(String operation, boolean expired)
      throws Exception {
    String path = "/api/v1/me/availability" + (operation.equals("zones") ? "/zones" : "");
    var request =
        operation.equals("PUT")
            ? put(path).contentType("application/json").content("{")
            : get(path);
    if (expired) {
      var session = (org.springframework.session.Session) sessions.createSession();
      session.setAttribute(
          "SPRING_SECURITY_CONTEXT",
          new org.springframework.security.core.context.SecurityContextImpl(
              org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                  .authenticated(
                      "persona-a",
                      "",
                      java.util.List.of(
                          new org.springframework.security.core.authority.SimpleGrantedAuthority(
                              "ROLE_USER")))));
      sessions.save(session);
      jdbc.update(
          "UPDATE spring_session SET last_access_time=0,expiry_time=0 WHERE session_id=?",
          session.getId());
      request.cookie(
          new jakarta.servlet.http.Cookie(
              "SESSION",
              java.util.Base64.getEncoder()
                  .encodeToString(
                      session.getId().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }
    mvc.perform(request)
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(header().doesNotExist("WWW-Authenticate"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Integer.class))
        .isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"missing", "invalid", "foreignOrigin"})
  void s24_rejectsCsrfOrForeignOrigin(String defect) throws Exception {
    var request =
        put("/api/v1/me/availability")
            .with(user("persona-a"))
            .contentType("application/json")
            .header("If-Match", "\"availability:unconfigured\"")
            .content(body("UTC", 60));
    if (defect.equals("invalid")) request.with(csrf().useInvalidToken().asHeader());
    if (defect.equals("foreignOrigin"))
      request.with(csrf().asHeader()).header("Origin", "https://foreign.example");
    mvc.perform(request)
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.code")
                .value(defect.equals("foreignOrigin") ? "UNTRUSTED_ORIGIN" : "CSRF_INVALID"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Integer.class))
        .isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"GET", "PUT"})
  void s22_ownerComesOnlyFromSession(String operation) throws Exception {
    var a = save("\"availability:unconfigured\"", "UTC", 30).andReturn().getResponse();
    var b =
        mvc.perform(
                put("/api/v1/me/availability")
                    .with(user("persona-b"))
                    .with(csrf().asHeader())
                    .header("If-Match", "\"availability:unconfigured\"")
                    .contentType("application/json")
                    .content(body("CET", 90)))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var foreign =
        jdbc.queryForMap("SELECT * FROM availability_preferences WHERE owner_id='persona-b'");
    var result =
        operation.equals("GET")
            ? mvc.perform(get("/api/v1/me/availability").with(user("persona-a")))
                .andExpect(
                    header()
                        .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
            : save(a.getHeader("ETag"), "Europe/Madrid", 60)
                .andExpect(
                    header()
                        .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    assertThat(result.getHeader("ETag")).doesNotContain(b.getHeader("ETag"));
    assertThat(json.readTree(result.getContentAsString()).get("zoneId").textValue())
        .isEqualTo(operation.equals("GET") ? "UTC" : "Europe/Madrid");
    assertThat(
            jdbc.queryForMap("SELECT * FROM availability_preferences WHERE owner_id='persona-b'"))
        .isEqualTo(foreign);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"GET", "PUT"})
  void s45_s46_historicalZoneCanBeReadButCannotBeSaved(String operation) throws Exception {
    var first = save("\"availability:unconfigured\"", "UTC", 60).andReturn().getResponse();
    jdbc.update(
        "UPDATE availability_preferences SET zone_id='Historical/Removed' WHERE owner_id='persona-a'");
    var before = jdbc.queryForMap("SELECT * FROM availability_preferences");
    if (operation.equals("GET")) {
      mvc.perform(get("/api/v1/me/availability").with(user("persona-a")))
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.zoneId").value("Historical/Removed"))
          .andExpect(header().string("ETag", first.getHeader("ETag")));
    } else
      save(first.getHeader("ETag"), "Historical/Removed", 60)
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.errors[0].field").value("zoneId"))
          .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    assertThat(jdbc.queryForMap("SELECT * FROM availability_preferences")).isEqualTo(before);
  }

  @Test
  void s4_retainsCatalogAliasExactly() throws Exception {
    assertThat(java.time.ZoneId.getAvailableZoneIds()).contains("CET");
    save("\"availability:unconfigured\"", "CET", 60)
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.zoneId").value("CET"));
    assertThat(jdbc.queryForObject("SELECT zone_id FROM availability_preferences", String.class))
        .isEqualTo("CET");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("boundaryDays")
  void s5_acceptsBothBoundsForEveryDay(String day, int minutes) throws Exception {
    var value = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(body("UTC", 60));
    ((com.fasterxml.jackson.databind.node.ObjectNode) value.get("dailyMinutes")).put(day, minutes);
    mvc.perform(
            put("/api/v1/me/availability")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("If-Match", "\"availability:unconfigured\"")
                .contentType("application/json")
                .content(value.toString()))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dailyMinutes." + day).value(minutes));
    assertThat(
            jdbc.queryForObject(
                "SELECT "
                    + day.toLowerCase(java.util.Locale.ROOT)
                    + "_minutes FROM availability_preferences",
                Integer.class))
        .isEqualTo(minutes);
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> boundaryDays() {
    return java.util.Arrays.stream(java.time.DayOfWeek.values())
        .flatMap(
            day ->
                java.util.stream.Stream.of(
                    org.junit.jupiter.params.provider.Arguments.of(day.name(), 0),
                    org.junit.jupiter.params.provider.Arguments.of(day.name(), 1440)));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1440})
  void s6_persistsFullWeekWithoutDerivedColumn(int minutes) throws Exception {
    var response =
        save("\"availability:unconfigured\"", "UTC", minutes)
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var result = json.readTree(response.getContentAsString());
    assertThat(result.size()).isEqualTo(4);
    int total = 0;
    for (var value : result.get("dailyMinutes")) total += value.intValue();
    assertThat(total).isEqualTo(minutes * 7);
    assertThat(jdbc.queryForMap("SELECT * FROM availability_preferences"))
        .doesNotContainKeys("weekly_total", "weekly_minutes", "capacity");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "session",
        "csrf",
        "media",
        "query",
        "etag",
        "syntax",
        "rootExtras",
        "zone",
        "dayExtras",
        "dayOrder",
        "oldNoOp"
      })
  void s17_errorPrecedenceIsStable(String defect) throws Exception {
    var value = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(body("UTC", 60));
    String raw = null;
    String tag = "\"availability:unconfigured\"";
    String field = null;
    String fieldCode = null;
    String code = "VALIDATION_ERROR";
    int expected = 400;
    switch (defect) {
      case "session" -> {
        expected = 401;
        code = "UNAUTHENTICATED";
        tag = null;
      }
      case "csrf" -> {
        expected = 403;
        code = "CSRF_INVALID";
        raw = "{";
      }
      case "media" -> {
        expected = 415;
        code = "UNSUPPORTED_MEDIA_TYPE";
        tag = null;
      }
      case "query" -> {
        field = "query";
        fieldCode = "INVALID_VALUE";
        tag = null;
      }
      case "etag" -> {
        tag = "*";
        raw = "{";
        field = "If-Match";
        fieldCode = "INVALID_VALUE";
      }
      case "syntax" -> {
        raw = "{\"zoneId\":5";
        code = "MALFORMED_JSON";
      }
      case "rootExtras" -> {
        value.put("z", 1).put("a", 2);
        value.remove("zoneId");
        field = "a";
        fieldCode = "UNKNOWN_FIELD";
      }
      case "zone" -> {
        value.remove("zoneId");
        value.remove("dailyMinutes");
        field = "zoneId";
        fieldCode = "REQUIRED";
      }
      case "dayExtras" -> {
        var days = (com.fasterxml.jackson.databind.node.ObjectNode) value.get("dailyMinutes");
        days.put("z", 1).put("a", 2);
        days.remove("MONDAY");
        field = "dailyMinutes.a";
        fieldCode = "UNKNOWN_FIELD";
      }
      case "dayOrder" -> {
        var days = (com.fasterxml.jackson.databind.node.ObjectNode) value.get("dailyMinutes");
        days.put("MONDAY", -1);
        days.remove("TUESDAY");
        field = "dailyMinutes.MONDAY";
        fieldCode = "OUT_OF_RANGE";
      }
      case "oldNoOp" -> {
        tag = save(tag, "UTC", 30).andReturn().getResponse().getHeader("ETag");
        save(tag, "UTC", 60)
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk());
        expected = 412;
        code = "AVAILABILITY_CONFLICT";
      }
      default -> throw new AssertionError();
    }
    var before = jdbc.queryForList("SELECT * FROM availability_preferences");
    var request =
        put("/api/v1/me/availability" + (defect.equals("query") ? "?limit=1" : ""))
            .contentType(defect.equals("media") ? "text/plain" : "application/json")
            .content(raw == null ? value.toString() : raw);
    if (!defect.equals("session")) request.with(user("persona-a"));
    if (!defect.equals("csrf")) request.with(csrf().asHeader());
    if (tag != null) request.header("If-Match", tag);
    var result =
        mvc.perform(request)
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().is(expected))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    if (field != null)
      result
          .andExpect(jsonPath("$.errors[0].field").value(field))
          .andExpect(jsonPath("$.errors[0].code").value(fieldCode));
    assertThat(jdbc.queryForList("SELECT * FROM availability_preferences")).isEqualTo(before);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"first", "change", "noop"})
  void s18_personalPreferenceNeverChangesProjectHistoryOrOutbox(String operation) throws Exception {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','Project','', 'active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at,completed_at,version) VALUES (?,?,'Task','','completed',now(),now(),now(),1)",
        task,
        project);
    jdbc.update(
        "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,1,'pending','completed',now())",
        UUID.randomUUID(),
        project,
        task);
    for (String type :
        java.util.List.of(
            "ProjectCreated",
            "ProjectUpdated",
            "ProjectStatusChanged",
            "TaskCreated",
            "SubtaskCreated",
            "TaskStatusChanged"))
      jdbc.update(
          "INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload) VALUES (?,?,'persona-a',?,1,now(),'{}'::jsonb)",
          UUID.randomUUID(),
          project,
          type);
    String tag = "\"availability:unconfigured\"";
    if (!operation.equals("first"))
      tag = save(tag, "UTC", 60).andReturn().getResponse().getHeader("ETag");
    var before = aggregateSnapshot();
    save(tag, "UTC", operation.equals("noop") ? 60 : 30)
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isOk());
    assertThat(aggregateSnapshot()).isEqualTo(before);
  }

  java.util.Map<String, java.util.List<java.util.Map<String, Object>>> aggregateSnapshot() {
    var snapshot =
        new java.util.LinkedHashMap<String, java.util.List<java.util.Map<String, Object>>>();
    for (String table :
        java.util.List.of("projects", "tasks", "task_status_history", "outbox_events"))
      snapshot.put(table, jdbc.queryForList("SELECT * FROM " + table));
    return snapshot;
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"advance", "equal", "backwards"})
  void s9_confirmedClockValuesRoundTripPostgres(String clockCase) throws Exception {
    save("\"availability:unconfigured\"", "UTC", 60).andExpect(status().isOk());
    var prior = java.time.Instant.parse("2030-01-01T00:00:00.123456Z");
    jdbc.update("UPDATE availability_preferences SET updated_at=?", java.sql.Timestamp.from(prior));
    var id = jdbc.queryForObject("SELECT id FROM availability_preferences", UUID.class);
    var instant =
        clockCase.equals("advance")
            ? prior.plusNanos(123456789)
            : clockCase.equals("equal") ? prior : prior.minusSeconds(1);
    var store =
        new com.apptolast.organization.adapter.persistence.PostgresAvailabilityStore(
            jdbc,
            new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                    jdbc.getDataSource())));
    var useCase =
        new com.apptolast.organization.application.SaveAvailability(
            store,
            () -> java.util.Set.of("UTC"),
            java.time.Clock.fixed(instant, java.time.ZoneOffset.UTC));
    var days = new java.util.EnumMap<java.time.DayOfWeek, Integer>(java.time.DayOfWeek.class);
    for (var day : java.time.DayOfWeek.values()) days.put(day, 30);
    var confirmed =
        useCase.execute(
            "persona-a",
            new com.apptolast.organization.domain.AvailabilityRevision(id, 0),
            "UTC",
            days);
    var expected =
        instant.isAfter(prior) ? instant.truncatedTo(java.time.temporal.ChronoUnit.MICROS) : prior;
    assertThat(confirmed.updatedAt()).isEqualTo(expected);
    assertThat(
            jdbc.queryForObject(
                    "SELECT updated_at FROM availability_preferences", java.sql.Timestamp.class)
                .toInstant())
        .isEqualTo(expected);
    var response =
        mvc.perform(get("/api/v1/me/availability").with(user("persona-a")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(
            java.time.Instant.parse(
                json.readTree(response.getContentAsString()).get("updatedAt").textValue()))
        .isEqualTo(expected);
  }
}
