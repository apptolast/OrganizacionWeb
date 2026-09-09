package com.apptolast.organization.adapter.connectors;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @s11 @s32 la base de la API es configuración del servidor y se valida al construir el bean. El
 *     PAT del usuario viaja hacia ella, así que sólo se admite la instancia oficial o un bucle
 *     local para pruebas, siempre bajo la ruta {@code /api/v4} y sin nada más pegado.
 */
class GitlabApiBaseTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://gitlab.com/api/v4",
        "https://gitlab.com/api/v4/",
        "http://127.0.0.1:8099/api/v4",
        "http://localhost:8099/api/v4",
        "https://[::1]:8443/api/v4"
      })
  void s11_acceptsTheOfficialInstanceAndLoopbackUnderTheVersionedPath(String raw) {
    assertThat(GitlabApiBase.of(raw).value()).doesNotEndWith("/");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://gitlab.example.com/api/v4",
        "http://169.254.169.254/api/v4",
        "https://gitlab.com.atacante.test/api/v4",
        "https://gitlab.com:8443/api/v4",
        "http://gitlab.com/api/v4",
        "https://gitlab.com/api/v3",
        "https://gitlab.com/api/v4/projects",
        "https://gitlab.com/api/v4?token=x",
        "https://gitlab.com/api/v4#x",
        "https://user:clave@gitlab.com/api/v4",
        "https://gitlab.com",
        "file:///etc/passwd",
        "no-es-una-uri absoluta"
      })
  void s11_refusesAnythingElseWhenTheBeanIsBuilt(String raw) {
    assertThatThrownBy(() -> GitlabApiBase.of(raw))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("app.gitlab.api-base");
  }

  @Test
  void s11_refusesAnAbsentBaseInsteadOfGuessingOne() {
    assertThatThrownBy(() -> GitlabApiBase.of(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("app.gitlab.api-base");
  }

  @Test
  void s11_theProjectPathTravelsEncodedSoItCannotLeaveTheProjectsResource() {
    var base = GitlabApiBase.of("https://gitlab.com/api/v4");

    assertThat(base.project("grupo/proyecto"))
        .isEqualTo("https://gitlab.com/api/v4/projects/grupo%2Fproyecto");
    assertThat(base.project("grupo/sub/proyecto"))
        .isEqualTo("https://gitlab.com/api/v4/projects/grupo%2Fsub%2Fproyecto");
    assertThat(base.project("gr.u+p-o_1/pro.yecto"))
        .isEqualTo("https://gitlab.com/api/v4/projects/gr.u%2Bp-o_1%2Fpro.yecto");
  }

  @Test
  void s16_theIssuesQueryAlwaysAsksForOpenIssuesAHundredAtATime() {
    var base = GitlabApiBase.of("https://gitlab.com/api/v4");

    assertThat(base.issues("4821", 2))
        .isEqualTo(
            "https://gitlab.com/api/v4/projects/4821/issues?state=opened&per_page=100&page=2");
  }
}
