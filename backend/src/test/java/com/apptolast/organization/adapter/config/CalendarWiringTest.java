package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.ManageCalendarFeedUseCase;
import com.apptolast.organization.application.RenderCalendarUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The real beans, over the real database: @s1, @s4, @s5, @s12, @s27 and @s29 end to end. */
@SpringBootTest(
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example",
      "app.publisher.enabled=false"
    })
@Testcontainers
class CalendarWiringTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", postgres::getJdbcUrl);
    properties.add("spring.datasource.username", postgres::getUsername);
    properties.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired ManageCalendarFeedUseCase feeds;
  @Autowired RenderCalendarUseCase calendars;
  @Autowired JdbcTemplate jdbc;

  static String tokenOf(String url) {
    return url.substring(url.lastIndexOf('/') + 1, url.length() - ".ics".length());
  }

  @Test
  void s1_s4_s5_s12_s29_theRealBeansIssueResolveAndRevokeAgainstTheDatabase() {
    var owner = "calendar-wiring-owner";
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var block = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at)"
            + " VALUES (?,?,'Propio','','active',0,'2026-09-01Z','2026-09-01Z')",
        project,
        owner);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,version,created_at,"
            + "updated_at) VALUES (?,?,'Revisar','','pending',0,'2026-09-01Z','2026-09-01Z')",
        task,
        project);
    var start = java.time.Instant.now().plus(java.time.Duration.ofDays(1));
    var startAt = start.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
    jdbc.update(
        "INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,"
            + "end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,"
            + "duration_minutes,created_at) VALUES (?,?,?,?,'Objetivo',?,?,'Europe/Madrid',"
            + "'+01:00','+01:00',false,?,?,60,'2026-09-01Z')",
        block,
        project,
        task,
        UUID.randomUUID(),
        java.time.LocalDateTime.ofInstant(startAt, java.time.ZoneOffset.UTC),
        java.time.LocalDateTime.ofInstant(
            startAt.plus(java.time.Duration.ofHours(1)), java.time.ZoneOffset.UTC),
        java.sql.Timestamp.from(startAt),
        java.sql.Timestamp.from(startAt.plus(java.time.Duration.ofHours(1))));

    var link = feeds.generate(owner);
    assertThat(link.url()).startsWith("https://organization.example/calendar/").endsWith(".ics");
    assertThat(tokenOf(link.url())).matches("[A-Za-z0-9_-]{43}");
    assertThat(feeds.status(owner).active()).isTrue();

    var document = calendars.forToken(tokenOf(link.url()));
    assertThat(document).startsWith("BEGIN:VCALENDAR\r\n").contains("UID:" + block + "@");
    assertThat(document).isEqualTo(calendars.forOwner(owner));

    var regenerated = feeds.generate(owner);
    assertThat(regenerated.url()).isNotEqualTo(link.url());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM calendar_feed_tokens WHERE owner_id=?", Integer.class, owner))
        .isOne();

    feeds.revoke(owner);
    assertThat(feeds.status(owner).active()).isFalse();
    assertThat(calendars.forOwner(owner)).contains("UID:" + block + "@");
  }
}
