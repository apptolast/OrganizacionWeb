package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @s8 leer la conexión de GitLab sin conexión, con los ocho campos del contrato.
 */
class GitlabConnectionUseCasesTest {
  private static final String OWNER = "owner-1";
  private static final String API_BASE = "https://gitlab.example.com/api/v4";

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
}
