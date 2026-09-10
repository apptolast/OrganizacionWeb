package com.apptolast.organization;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.adapter.http.ConnectorsGate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Dos frases del contrato de la 27 que se daban por buenas sin que nada las midiera.
 *
 * <p>@s27, tercera línea del Then: «no se relanza ninguna importación automáticamente ni se
 * contacta con el servidor falso». Hoy es cierto porque no existe ningún planificador del conector,
 * pero nada lo sujetaba: quien añadiera mañana un reintento periódico no rompería ninguna prueba.
 *
 * <p>@s3, tercera línea del Then: «el resto de rutas de proyectos y tareas responde con
 * normalidad». Sin clave de conectores hay un filtro que contesta 503 antes que el controlador, y
 * lo que hay que sujetar es su alcance: que no cubra nunca proyectos ni tareas, y que la parte del
 * producto que no es conector no dependa de la clave para funcionar.
 */
class GithubConnectorIsolationTest {
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.apptolast.organization");

  private static final String IMPORT_USE_CASE =
      "com.apptolast.organization.application.ImportIssuesUseCase";

  /** Lo que sólo tiene sentido con clave de conectores: nada de proyectos ni tareas lo toca. */
  private static final String CONNECTOR_KEY_TYPES =
      ".*\\.(SecretCipher|ConnectorsDisabledException|ConnectorsGate|ConnectorKeyRing"
          + "|AesGcmSecretCipher|SecretUndecipherableException)";

  // ------------------------------------------------------------------ @s27 nadie relanza sola

  /**
   * Importar es bajo demanda: el único que puede pedirlo es la frontera HTTP que atiende el POST de
   * la persona. Un planificador, un {@code ApplicationRunner} o un reintento en el arranque que
   * dependiera del caso de uso caería aquí.
   */
  @Test
  void s27_onlyTheHttpBoundaryAndItsWiringCanStartAnImport() {
    noClasses()
        .that()
        .haveSimpleNameNotEndingWith("Controller")
        .and()
        .haveSimpleNameNotContaining("ImportIssues")
        .and()
        .haveSimpleNameNotContaining("ConnectorConfiguration")
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName(IMPORT_USE_CASE)
        .because("importar es bajo demanda y nadie más puede dispararlo")
        .check(CLASSES);
  }

  @Test
  void s27_nothingOfTheConnectorRunsOnStartup() {
    noClasses()
        .that()
        .haveNameMatching(".*\\.(ImportIssues|GithubIssueConnections|ConnectGithub|HttpGithub).*")
        .should()
        .beAssignableTo(org.springframework.boot.ApplicationRunner.class)
        .orShould()
        .beAssignableTo(org.springframework.boot.CommandLineRunner.class)
        .check(CLASSES);
  }

  // --------------------------------------------------- @s3 el resto del producto sigue normal

  /**
   * El filtro que contesta 503 sin clave cubre su prefijo exacto y lo que cuelga de él. Ninguna
   * ruta de proyectos ni de tareas puede caer dentro, ni siquiera si el prefijo se acorta.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/v1/projects",
        "/api/v1/projects/11111111-2222-3333-4444-555555555555",
        "/api/v1/projects/11111111-2222-3333-4444-555555555555/tasks",
        "/api/v1/me/today",
        "/api/v1/me/history"
      })
  void s3_theGateThatAnswersServiceUnavailableNeverCoversProjectsOrTasks(String route) {
    assertFalse(
        route.equals(ConnectorsGate.PREFIX) || route.startsWith(ConnectorsGate.PREFIX + "/"),
        "el filtro de conectores no puede alcanzar " + route);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "com.apptolast.organization.adapter.http.ProjectController",
        "com.apptolast.organization.adapter.http.ProjectReadController",
        "com.apptolast.organization.adapter.http.ProjectEditController",
        "com.apptolast.organization.adapter.http.TaskController",
        "com.apptolast.organization.application.CreateProject",
        "com.apptolast.organization.application.CreateTask",
        "com.apptolast.organization.application.ReadProjects",
        "com.apptolast.organization.application.ReadTasks"
      })
  void s3_projectAndTaskRoutesDoNotNeedTheConnectorKeyToAnswer(String isolated) {
    noClasses()
        .that()
        .haveFullyQualifiedName(isolated)
        .should()
        .dependOnClassesThat()
        .haveNameMatching(CONNECTOR_KEY_TYPES)
        .because("sin APP_CONNECTOR_KEY el resto del producto responde con normalidad")
        .check(CLASSES);
  }

  @Test
  void theWatchedTypesStillExist() {
    var watched =
        List.of(
            IMPORT_USE_CASE,
            "com.apptolast.organization.application.ImportIssues",
            "com.apptolast.organization.adapter.http.ConnectorsGate",
            "com.apptolast.organization.adapter.http.ProjectController");
    assertTrue(
        watched.stream()
            .allMatch(name -> CLASSES.stream().anyMatch(type -> type.getName().equals(name))),
        "la lista de tipos vigilados debe seguir existiendo");
  }
}
