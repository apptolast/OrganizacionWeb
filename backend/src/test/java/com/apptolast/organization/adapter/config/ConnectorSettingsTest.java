package com.apptolast.organization.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Enmienda B10 de la revisión de seguridad: la guardia SSRF solo se desactiva a propósito y el
 * perfil de producción la deja activada. Comprueba el cableado sin levantar el contexto.
 */
class ConnectorSettingsTest {
  static final String ALLOW_PRIVATE = "app.connectors.allow-private-addresses";
  static final Path REPOSITORY = Path.of("..");

  static Properties applicationProperties() throws IOException {
    var properties = new Properties();
    try (var stream = ConnectorSettingsTest.class.getResourceAsStream("/application.properties")) {
      properties.load(stream);
    }
    return properties;
  }

  @Test
  void b10_theProductionProfileLeavesTheSsrfGuardEnabled() throws IOException {
    assertEquals(
        "${APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES:false}",
        applicationProperties().getProperty(ALLOW_PRIVATE),
        "el valor por defecto sin variable de entorno debe ser false");
  }

  @ParameterizedTest
  @ValueSource(strings = {"app.connectors.key", "app.connectors.key-previous", ALLOW_PRIVATE})
  void b10_connectorSettingsComeFromTheEnvironment(String setting) throws IOException {
    var value = applicationProperties().getProperty(setting);
    assertTrue(value != null && value.startsWith("${"), setting + " debe leerse del entorno");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "APP_CONNECTOR_KEY",
        "APP_CONNECTOR_KEY_PREVIOUS",
        "APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES"
      })
  void b10_theExampleEnvironmentDeclaresEveryConnectorVariable(String variable) throws IOException {
    assertTrue(example().contains(variable + "="), variable + " no está declarada");
  }

  @Test
  void b10_theExampleEnvironmentDisablesPrivateAddressesWithAnExplicitWarning() throws IOException {
    var example = example();
    assertTrue(example.contains("APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES=false"));
    assertFalse(example.contains("APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES=true"));
    var warning =
        example
            .lines()
            .dropWhile(line -> !line.contains("DANGER"))
            .takeWhile(line -> line.startsWith("#"))
            .toList();
    assertTrue(warning.size() >= 2, "falta el comentario de advertencia");
    assertTrue(
        String.join(" ", warning).contains("SSRF"),
        "la advertencia debe decir que desactiva la guardia SSRF");
  }

  static String example() throws IOException {
    return Files.readString(REPOSITORY.resolve(".env.example"), StandardCharsets.UTF_8);
  }
}
