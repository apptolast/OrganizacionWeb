package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @s8 leer la conexión de GitLab, con conexión y sin ella, con los ocho campos del contrato.
 */
class GitlabConnectionUseCasesTest {
  private static final String OWNER = "owner-1";
  private static final String API_BASE = "https://gitlab.example.com/api/v4";
  private static final Instant CONNECTED_AT = Instant.parse("2026-09-09T10:00:00Z");
  private static final Instant FAILED_AT = Instant.parse("2026-09-09T11:30:00Z");

  private GitlabFakes fakes;

  @BeforeEach
  void setUp() {
    fakes = new GitlabFakes();
  }

  private ReadGitlabConnectionUseCase read() {
    return new ReadGitlabConnection(fakes.connections, API_BASE, fakes.cipher);
  }

  @Test
  void s8_readingWithoutConnectionAnswersNotConnectedAndNothingElse() {
    var view = read().execute(OWNER);

    assertEquals("not_connected", view.status());
    assertNull(view.apiBase());
    assertNull(view.projectPath());
    assertNull(view.projectId());
    assertNull(view.tokenHint());
    assertNull(view.lastActivityAt());
    assertNull(view.lastError());
    assertNull(view.version());
  }

  @Test
  void s8_readingAConnectedRowAnswersItsFieldsAndTheApiBaseOfTheServer() {
    fakes.connections.put(OWNER, connected());

    var view = read().execute(OWNER);

    assertEquals("connected", view.status());
    assertEquals(API_BASE, view.apiBase());
    assertEquals("grupo/proyecto", view.projectPath());
    assertEquals(4821L, view.projectId());
    assertEquals("WXYZ", view.tokenHint());
    assertEquals(CONNECTED_AT, view.lastActivityAt());
    assertNull(view.lastError());
    assertEquals(1L, view.version());
  }

  @Test
  void s8_readingARowInErrorAnswersItsLastErrorAsCodeAndInstant() {
    fakes.connections.put(OWNER, connected().withError("CONNECTION_INVALID", FAILED_AT));

    var view = read().execute(OWNER);

    assertEquals("error", view.status());
    assertEquals("CONNECTION_INVALID", view.lastError().code());
    assertEquals(FAILED_AT, view.lastError().at());
  }

  private static GitlabConnection connected() {
    return new GitlabConnection(
        "grupo/proyecto",
        4821L,
        "WXYZ",
        GitlabConnection.CONNECTED,
        "cifrado".getBytes(StandardCharsets.UTF_8),
        CONNECTED_AT,
        null,
        null,
        1L);
  }
}
