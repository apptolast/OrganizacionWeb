package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.ConnectorsDisabledException;
import com.apptolast.organization.application.SecretCipher;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @s3 sin clave el conector queda deshabilitado y ninguna ruta lee ni escribe, @s4 una clave
 *     presente pero inválida impide arrancar sin revelarla, @s35 la base de la API es del servidor.
 */
class GithubConnectorWiringTest {
  private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

  private final ConnectorConfiguration configuration = new ConnectorConfiguration();

  @Test
  void s3_withoutAKeyTheCipherStartsDisabledInsteadOfStoppingTheApplication() {
    var cipher = configuration.secretCipher(null, null);

    assertThat(cipher.enabled()).isFalse();
    assertThatThrownBy(() -> cipher.encrypt("owner", "ghp_x"))
        .isInstanceOf(ConnectorsDisabledException.class);
    assertThatThrownBy(() -> cipher.decrypt("owner", new byte[42]))
        .isInstanceOf(ConnectorsDisabledException.class);
  }

  @Test
  void s3_anEmptyValueCountsAsAbsentBecauseThatIsWhatAnUnsetVariableLooksLike() {
    assertThat(configuration.secretCipher("", "").enabled()).isFalse();
    assertThat(configuration.secretCipher("   ", null).enabled()).isFalse();
  }

  @Test
  void s1_withAKeyTheCipherWorksAndTiesTheSecretToItsOwner() {
    var cipher = configuration.secretCipher(KEY, null);

    assertThat(cipher.enabled()).isTrue();
    var sealed = cipher.encrypt("owner", "ghp_secreto123");
    assertThat(cipher.decrypt("owner", sealed)).contains("ghp_secreto123");
    assertThat(sealed).hasSize(12 + "ghp_secreto123".length() + 16);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "AAAAAAAAAAAAAAAAAAAAAA==",
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "no-es-base64-!!!",
        "c2hvcnQ="
      })
  void s4_aKeyThatIsPresentButMalformedStopsTheStartupWithoutRevealingIt(String value) {
    assertThatThrownBy(() -> configuration.secretCipher(value, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("app.connectors.key")
        .satisfies(error -> assertThat(error.getMessage()).doesNotContain(value));
  }

  @Test
  void s4_aMalformedPreviousKeyAlsoStopsTheStartupNamingItsOwnProperty() {
    assertThatThrownBy(() -> configuration.secretCipher(KEY, "no-es-base64-!!!"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("app.connectors.key-previous");
  }

  @Test
  void b5_aRotatedKeyStillDecipherWhatThePreviousOneSealed() {
    var previous = Base64.getEncoder().encodeToString(filled((byte) 7));
    var sealed = configuration.secretCipher(previous, null).encrypt("owner", "ghp_viejo");

    var rotated = configuration.secretCipher(KEY, previous);

    assertThat(rotated.decrypt("owner", sealed)).contains("ghp_viejo");
    assertThat(rotated.decrypt("owner", rotated.encrypt("owner", "ghp_nuevo")))
        .contains("ghp_nuevo");
  }

  @Test
  void s35_theConfiguredApiBaseIsValidatedWhenTheBeanIsBuilt() {
    assertThat(configuration.githubApiBase("https://api.github.com").value())
        .isEqualTo("https://api.github.com");
    assertThat(configuration.githubApiBase("http://127.0.0.1:18093").value())
        .isEqualTo("http://127.0.0.1:18093");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"http://api.github.com", "https://atacante.test", ""})
  void s35_anApiBaseOutsideTheAllowedListStopsTheStartupNamingTheProperty(String value) {
    assertThatThrownBy(() -> configuration.githubApiBase(value))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("app.github.api-base");
  }

  @Test
  void s3_theDisabledCipherIsTheOnlyThingThatDegradesTheConnector() {
    SecretCipher disabled = configuration.secretCipher(null, null);
    SecretCipher enabled = configuration.secretCipher(KEY, null);

    assertThat(disabled.enabled()).isNotEqualTo(enabled.enabled());
  }

  private static byte[] filled(byte value) {
    var material = new byte[32];
    java.util.Arrays.fill(material, value);
    return material;
  }
}
