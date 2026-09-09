package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apptolast.organization.support.TestDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @s34 «Mantener idéntica la respuesta de Hoy con y sin suscripción», ejecutado sobre datos reales.
 *     <p>Hasta hoy los tres <em>examples</em> del contrato no se ejercían en ninguna parte: {@code
 *     ExternalCalendarIsolationTest} cierra el vector de <em>código</em> con dos reglas ArchUnit
 *     —nadie puede depender del carril desde Hoy o desde la planificación— y {@code
 *     e2e/external-calendar.spec.mjs} sólo mira Hoy <em>sin</em> suscripción. Queda descubierto el
 *     vector de <em>datos</em>: unir {@code external_calendar_events} dentro de una consulta SQL de
 *     {@code PostgresTodayQueries} o de {@code PostgresBlockStore} no añade ninguna dependencia de
 *     clase, así que ArchUnit sigue verde, y ningún test de bloques siembra esas filas, así que
 *     tampoco hay rojo por datos.
 *     <p>Esta prueba ejecuta el Given que faltaba —«persona-a suscribe un feed con &lt;evento&gt;
 *     ya sincronizado»— y los dos Then: el cuerpo de Hoy comparado byte a byte con la respuesta R0
 *     tomada antes de suscribirse, y la planificación del hueco que el evento externo ocupa
 *     respondiendo 201 sin error de solape.
 */
@SpringBootTest(
    properties = {
      "app.auth.username=persona-a",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@AutoConfigureMockMvc
class ExternalCalendarTodayApiTest {
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static {
    postgres.start();
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  /**
   * El reloj se para dentro del bloque propio: así {@code currentBlockId} y {@code closingAt} —los
   * dos campos que el Then nombra— llevan valor y no {@code null}, que es el valor que cualquier
   * regresión conservaría por accidente.
   */
  static final Instant NOW = Instant.parse("2030-01-07T10:30:00Z");

  static final String OWN_START = "2030-01-07T10:00:00Z";
  static final String OWN_END = "2030-01-07T11:00:00Z";
  static final int BUDGET_MINUTES = 120;
  static final int TODAY_FIELDS = 15;

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @org.springframework.test.context.bean.override.mockito.MockitoBean Clock clock;

  @org.springframework.test.context.bean.override.mockito.MockitoBean
  com.apptolast.organization.application.ZoneCatalog catalog;

  UUID project;
  UUID task;
  UUID availability;
  UUID ownBlock;

  @BeforeEach
  void reset() {
    when(catalog.zones()).thenReturn(Set.of("UTC", "Europe/Madrid"));
    when(clock.instant()).thenReturn(NOW);
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    TestDatabase.empty(jdbc);
    project = UUID.randomUUID();
    task = UUID.randomUUID();
    availability = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES"
            + " (?,'persona-a','Proyecto','','active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at)"
            + " VALUES (?,?,'Tarea','','pending',now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO"
            + " availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at)"
            + " VALUES (?,'persona-a','UTC',?,?,?,?,?,?,?,0,now(),now())",
        availability,
        BUDGET_MINUTES,
        BUDGET_MINUTES,
        BUDGET_MINUTES,
        BUDGET_MINUTES,
        BUDGET_MINUTES,
        BUDGET_MINUTES,
        BUDGET_MINUTES);
    ownBlock = seedBlock(OWN_START, OWN_END);
  }

  UUID seedBlock(String start, String end) {
    var id = UUID.randomUUID();
    var from = Instant.parse(start);
    var to = Instant.parse(end);
    jdbc.update(
        "INSERT INTO"
            + " planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)"
            + " VALUES (?,?,?,?,'Meta',?,?,'UTC','Z','Z',false,?,?,?,?)",
        id,
        project,
        task,
        UUID.randomUUID(),
        LocalDateTime.ofInstant(from, ZoneOffset.UTC),
        LocalDateTime.ofInstant(to, ZoneOffset.UTC),
        Timestamp.from(from),
        Timestamp.from(to),
        (int) Duration.between(from, to).toMinutes(),
        Timestamp.from(Instant.parse("2030-01-07T09:00:00Z")));
    return id;
  }

  /** El Given del contrato: «persona-a suscribe un feed con &lt;evento&gt; ya sincronizado». */
  void subscribeWithEvent(String start, String end, boolean allDay) {
    jdbc.update(
        "INSERT INTO"
            + " external_calendar_subscriptions(owner_id,id,label,url_ciphertext,url_host,url_tail,version,created_at,updated_at,last_attempt_at,last_sync_at,last_status,snapshot_zone_id,imported)"
            + " VALUES"
            + " ('persona-a',?,'Trabajo',decode('000102030405060708090a0b0c0d0e0f','hex'),'calendar.google.com','.ics',0,now(),now(),?,?,'OK','UTC',1)",
        UUID.randomUUID(),
        Timestamp.from(NOW),
        Timestamp.from(NOW));
    jdbc.update(
        "INSERT INTO external_calendar_events(owner_id,uid,summary,start_at,end_at,all_day) VALUES"
            + " ('persona-a','uid-externo','Evento externo',?,?,?)",
        Timestamp.from(Instant.parse(start)),
        Timestamp.from(Instant.parse(end)),
        allDay);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM external_calendar_events WHERE owner_id='persona-a'",
                Long.class))
        .as("el Given no llegó a sembrar el evento externo")
        .isEqualTo(1L);
  }

  String today() throws Exception {
    return mvc.perform(get("/api/v1/today").with(user("persona-a")))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @ParameterizedTest(name = "{3}–{4} con un evento externo de {0} a {1}")
  @CsvSource({
    "2030-01-07T10:00:00Z,2030-01-07T11:00:00Z,false,2030-01-07T14:00,2030-01-07T15:00",
    "2030-01-07T14:00:00Z,2030-01-07T15:00:00Z,false,2030-01-07T14:00,2030-01-07T15:00",
    "2030-01-07T00:00:00Z,2030-01-08T00:00:00Z,true,2030-01-07T16:00,2030-01-07T17:00"
  })
  void s34_todayIsByteForByteTheSameAndTheOverlappingSlotStillPlans(
      String eventStart, String eventEnd, boolean allDay, String planStart, String planEnd)
      throws Exception {
    var r0 = today();
    var snapshot = json.readTree(r0);
    assertThat(snapshot.size()).isEqualTo(TODAY_FIELDS);
    assertThat(snapshot.get("plannedSeconds").asLong()).isEqualTo(3600);
    assertThat(snapshot.get("remainingSeconds").asLong()).isEqualTo(3600);
    assertThat(snapshot.get("currentBlockId").asText()).isEqualTo(ownBlock.toString());
    assertThat(snapshot.get("nextBlockId").isNull()).isTrue();
    assertThat(snapshot.get("closingAt").asText()).isEqualTo(OWN_END);
    assertThat(snapshot.get("items").size()).isEqualTo(1);

    subscribeWithEvent(eventStart, eventEnd, allDay);

    assertThat(today())
        .as("Hoy cambió al haber una suscripción con un evento ya sincronizado")
        .isEqualTo(r0);

    var response =
        mvc.perform(
                post("/api/v1/projects/" + project + "/tasks/" + task + "/blocks")
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .header("Availability-Revision", "\"availability:" + availability + ":0\"")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType("application/json")
                    .content(
                        "{\"objective\":\"Meta\",\"startLocal\":\""
                            + planStart
                            + "\",\"endLocal\":\""
                            + planEnd
                            + "\",\"zoneId\":\"UTC\",\"startOffset\":\"Z\",\"endOffset\":\"Z\",\"allowOverBudget\":false}"))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus())
        .as("la planificación tropezó con el evento externo: %s", response.getContentAsString())
        .isEqualTo(201);
    assertThat(response.getContentAsString()).doesNotContain("BLOCK_OVERLAP");
  }
}
