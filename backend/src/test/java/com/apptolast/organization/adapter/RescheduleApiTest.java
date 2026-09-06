package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(properties={"app.auth.username=persona-a", "app.auth.password=test-only-secret", "app.public-origin=https://organization.example"})
@AutoConfigureMockMvc
class RescheduleApiTest {
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");
  static { postgres.start(); }
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @org.springframework.test.context.bean.override.mockito.MockitoBean java.time.Clock clock;
  UUID project, task, block, key;
  String base() { return "/api/v1/projects/"+project+"/tasks/"+task+"/blocks"; }
  @BeforeEach
  void seed() {
    org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.parse("2030-01-07T09:00:00.123456789Z"));
    jdbc.execute("TRUNCATE block_changes,block_projections,planned_blocks,availability_preferences,task_status_history,tasks,outbox_events,projects");
    project=UUID.randomUUID(); task=UUID.randomUUID(); block=UUID.randomUUID(); key=UUID.randomUUID();
    jdbc.update("INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','Proyecto','','active',now(),now())",project);
    jdbc.update("INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Tarea','','pending',now(),now())",task,project);
    jdbc.update("INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) VALUES (?,?,?,?,'Preparar borrador',TIMESTAMP '2030-01-07 10:00',TIMESTAMP '2030-01-07 11:00','UTC','Z','Z',false,TIMESTAMPTZ '2030-01-07 10:00Z',TIMESTAMPTZ '2030-01-07 11:00Z',60,TIMESTAMPTZ '2030-01-06 10:00Z')",block,project,task,key);
  }
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"query,400,query","project,400,projectId","task,400,taskId","block,400,blockId","absent,428,none","weak,400,If-Match","wildcard,400,If-Match","list,400,If-Match","repeat,400,If-Match","other,400,If-Match","zero,400,If-Match","leading,400,If-Match","overflow,400,If-Match","keyMissing,400,Idempotency-Key","keyBad,400,Idempotency-Key","keyRepeated,400,Idempotency-Key"})
  void s5_s19_cancelHeadersAreValidatedInContractOrder(String defect,int status,String field) throws Exception {
    var revision="\"block:"+block+":1\"";
    revision=switch(defect) {
      case "weak" -> "W/"+revision;
      case "wildcard" -> "*";
      case "list" -> revision+","+revision;
      case "other" -> "\"block:"+UUID.randomUUID()+":1\"";
      case "zero" -> "\"block:"+block+":0\"";
      case "leading" -> "\"block:"+block+":01\"";
      case "overflow" -> "\"block:"+block+":9223372036854775808\"";
      default -> revision;
    };
    var path="/api/v1/projects/"+(defect.equals("project")?"1":project)+"/tasks/"+(defect.equals("task")?"1":task)+"/blocks/"+(defect.equals("block")?"1":block)+"/cancel";
    var request=post(path).with(user("persona-a")).with(csrf().asHeader()).contentType("application/json").content("{}");
    if(defect.equals("query")) request.queryParam("extra","1");
    if(!Set.of("absent","query","project","task","block").contains(defect)) request.header("If-Match",revision);
    if(defect.equals("repeat")) request.header("If-Match",revision);
    if(!defect.equals("keyMissing")) request.header("Idempotency-Key",defect.equals("keyBad")?"1":UUID.randomUUID());
    if(defect.equals("keyRepeated")) request.header("Idempotency-Key",UUID.randomUUID());
    var result=mvc.perform(request).andExpect(status().is(status))
      .andExpect(jsonPath("$.code").value(status==428?"PRECONDITION_REQUIRED":"VALIDATION_ERROR"));
    if(status!=428) result.andExpect(jsonPath("$.errors[0].field").value(field));
    for(var table:List.of("block_projections","block_changes","outbox_events")) assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class)).isZero();
  }
  @Test
  void s15_cancelReplayReturnsOriginalReceiptBeforeRevisionAndClock() throws Exception {
    var requestKey=UUID.randomUUID();
    var first=mvc.perform(post(base()+"/"+block+"/cancel").with(user("persona-a")).with(csrf().asHeader())
      .contentType("application/json").content("{}").header("If-Match","\"block:"+block+":1\"").header("Idempotency-Key",requestKey))
      .andExpect(status().isCreated()).andReturn().getResponse();
    var before=jdbc.queryForList("SELECT * FROM block_projections");
    org.mockito.Mockito.clearInvocations(clock);
    org.mockito.Mockito.when(clock.instant()).thenThrow(new AssertionError("Replay must not read clock"));
    var replay=mvc.perform(post(base()+"/"+block+"/cancel").with(user("persona-a")).with(csrf().asHeader())
      .contentType("application/json").content("{}").header("If-Match","\"block:"+block+":1\"").header("Idempotency-Key",requestKey))
      .andExpect(status().isOk()).andReturn().getResponse();
    assertThat(replay.getContentAsString()).isEqualTo(first.getContentAsString());
    assertThat(replay.getHeader("Location")).isEqualTo(first.getHeader("Location"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(before);
    for(var table:List.of("block_changes","outbox_events")) assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class)).isEqualTo(1);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }
  @Test
  void s12_s13_s26_cancelCommitsReceiptProjectionAndEventWithoutPreference() throws Exception {
    var original=jdbc.queryForList("SELECT * FROM planned_blocks");
    var response=mvc.perform(post(base()+"/"+block+"/cancel").with(user("persona-a")).with(csrf().asHeader())
      .contentType("application/json").content("{}").header("If-Match","\"block:"+block+":1\"").header("Idempotency-Key",UUID.randomUUID()))
      .andExpect(status().isCreated()).andReturn().getResponse();
    var receipt=json.readTree(response.getContentAsString());
    assertThat(receipt.size()).isEqualTo(7);
    var change=UUID.fromString(receipt.get("id").asText());
    assertThat(response.getHeader("Location")).isEqualTo(base()+"/changes/"+change);
    assertThat(receipt.get("blockId").asText()).isEqualTo(block.toString());
    assertThat(receipt.get("kind").asText()).isEqualTo("CANCELLED");
    assertThat(receipt.get("revision").asText()).isEqualTo("\"block:"+block+":2\"");
    assertThat(receipt.get("occurredAt").asText()).isEqualTo("2030-01-07T09:00:00.123456Z");
    assertThat(receipt.get("before").size()).isEqualTo(9);
    assertThat(receipt.get("before").get("id").asText()).isEqualTo(block.toString());
    assertThat(receipt.get("after").isNull()).isTrue();
    mvc.perform(get(base()+"/"+block+"/state").with(user("persona-a")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("cancelled"))
      .andExpect(jsonPath("$.updatedAt").value("2030-01-07T09:00:00.123456Z"));
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes",Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences",Long.class)).isZero();
    var event=json.readTree(jdbc.queryForObject("SELECT payload::text FROM outbox_events",String.class));
    assertThat(event.size()).isEqualTo(13);
    assertThat(event.get("type").asText()).isEqualTo("BlockChanged.v1");
    assertThat(event.get("schemaVersion").asInt()).isEqualTo(1);
    assertThat(event.get("aggregateId").asText()).isEqualTo(project.toString());
    assertThat(event.get("taskId").asText()).isEqualTo(task.toString());
    assertThat(event.get("ownerId").asText()).isEqualTo("persona-a");
    assertThat(event.get("blockId").asText()).isEqualTo(block.toString());
    assertThat(event.get("changeId").asText()).isEqualTo(change.toString());
    assertThat(event.get("eventId").asText()).isNotEqualTo(change.toString());
    assertThat(event.get("kind").asText()).isEqualTo("CANCELLED");
    assertThat(event.get("revision").asLong()).isEqualTo(2);
    assertThat(event.get("occurredAt")).isEqualTo(receipt.get("occurredAt"));
    assertThat(event.get("before")).isEqualTo(json.readTree("{\"startAt\":\"2030-01-07T10:00:00Z\",\"endAt\":\"2030-01-07T11:00:00Z\",\"zoneId\":\"UTC\",\"durationMinutes\":60}"));
    assertThat(event.get("after").isNull()).isTrue();
    org.mockito.Mockito.verify(clock,org.mockito.Mockito.times(1)).instant();
  }
  @Test
  void s18_missingBlockHasSpecificProblemWithoutDisclosingOtherResources() throws Exception {
    mvc.perform(get(base()+"/"+UUID.randomUUID()+"/state").with(user("persona-a")))
      .andExpect(status().isNotFound()).andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.code").value("BLOCK_NOT_FOUND"));
    mvc.perform(get(base()+"/"+block+"/state").with(user("persona-b")))
      .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }
  @Test
  void s2_s3_projectionDoesNotRewriteCreationReceipt() throws Exception {
    var original=jdbc.queryForList("SELECT * FROM planned_blocks");
    jdbc.update("INSERT INTO block_projections(block_id,version,status,updated_at,start_local,end_local,zone_id,start_offset,end_offset,start_at,end_at,duration_minutes) VALUES (?,2,'planned',TIMESTAMPTZ '2030-01-07 09:00Z',TIMESTAMP '2030-01-07 13:00',TIMESTAMP '2030-01-07 14:30','Europe/Madrid','+01:00','+01:00',TIMESTAMPTZ '2030-01-07 12:00Z',TIMESTAMPTZ '2030-01-07 13:30Z',90)",block);
    mvc.perform(get(base()+"/"+block+"/state").with(user("persona-a")))
      .andExpect(status().isOk()).andExpect(header().string("ETag","\"block:"+block+":2\""))
      .andExpect(jsonPath("$.block.startAt").value("2030-01-07T12:00:00Z"))
      .andExpect(jsonPath("$.block.endAt").value("2030-01-07T13:30:00Z"))
      .andExpect(jsonPath("$.block.durationMinutes").value(90))
      .andExpect(jsonPath("$.block.zoneId").value("Europe/Madrid"));
    mvc.perform(get(base()+"/by-request/"+key).with(user("persona-a")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.startAt").value("2030-01-07T10:00:00Z"))
      .andExpect(jsonPath("$.durationMinutes").value(60)).andExpect(jsonPath("$.zoneId").value("UTC"));
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
  }
  @Test
  void s3_s4_readsCancelledProjectionWithExactLargeRevisionAndLastBlock() throws Exception {
    jdbc.update("INSERT INTO block_projections(block_id,version,status,updated_at) VALUES (?,9007199254740993,'cancelled',TIMESTAMPTZ '2030-01-07 08:59:59.123456Z')",block);
    var response=mvc.perform(get(base()+"/"+block+"/state").with(user("persona-a")))
      .andExpect(status().isOk()).andExpect(header().string("ETag","\"block:"+block+":9007199254740993\""))
      .andReturn().getResponse();
    var state=json.readTree(response.getContentAsString());
    assertThat(state.size()).isEqualTo(3);
    assertThat(state.get("status").asText()).isEqualTo("cancelled");
    assertThat(state.get("updatedAt").asText()).isEqualTo("2030-01-07T08:59:59.123456Z");
    assertThat(state.get("block").size()).isEqualTo(9);
    assertThat(state.get("block").get("startAt").asText()).isEqualTo("2030-01-07T10:00:00Z");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections",Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events",Long.class)).isZero();
  }
  @Test
  void s1_readsOriginalStateWithoutMaterializingOrChangingFacts() throws Exception {
    var original=jdbc.queryForList("SELECT * FROM planned_blocks");
    var response=mvc.perform(get(base()+"/"+block+"/state").with(user("persona-a")))
      .andExpect(status().isOk()).andExpect(header().string("ETag","\"block:"+block+":1\""))
      .andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store"))).andReturn().getResponse();
    assertThat(json.readTree(response.getContentAsString())).isEqualTo(json.readTree("""
      {"block":{"id":"%s","projectId":"%s","taskId":"%s","objective":"Preparar borrador","startAt":"2030-01-07T10:00:00Z","endAt":"2030-01-07T11:00:00Z","zoneId":"UTC","durationMinutes":60,"createdAt":"2030-01-06T10:00:00Z"},"status":"planned","updatedAt":"2030-01-06T10:00:00Z"}
      """.formatted(block,project,task)));
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
    for(var table:List.of("availability_preferences","outbox_events")) assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class)).isZero();
  }
}
