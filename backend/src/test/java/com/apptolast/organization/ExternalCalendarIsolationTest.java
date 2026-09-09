package com.apptolast.organization;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @s34: Hoy y la planificación de bloques no saben que existe el calendario externo, así que su
 *     respuesta y sus solapes no pueden cambiar por tener una suscripción. @s30: nadie sincroniza
 *     al arrancar.
 */
class ExternalCalendarIsolationTest {
  static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.apptolast.organization");

  static final List<String> EXTERNAL_CALENDAR_TYPES =
      List.of(
          "com.apptolast.organization.domain.ExternalCalendarSubscription",
          "com.apptolast.organization.domain.ExternalCalendarSnapshot",
          "com.apptolast.organization.domain.ExternalCalendarInput",
          "com.apptolast.organization.domain.ExternalEvent",
          "com.apptolast.organization.domain.ExternalEventsRange",
          "com.apptolast.organization.domain.IcsFeed",
          "com.apptolast.organization.application.ExternalCalendarStore",
          "com.apptolast.organization.application.SyncExternalCalendar",
          "com.apptolast.organization.application.ReadExternalCalendarEvents");

  /**
   * El analizador del carril se llama {@code IcsFeed}; {@code IcsCalendar} es el escritor de la
   * feature 26 y no entra en esta regla.
   */
  private static final String SLICE_NAMES =
      ".*\\.(ExternalCalendar|ExternalEvent|IcsFeed|CalendarFeed|FeedFetch|FeedError"
          + "|SyncExternalCalendar|SecretCipher).*";

  @ParameterizedTest
  @ValueSource(
      strings = {
        "com.apptolast.organization.application.ReadToday",
        "com.apptolast.organization.application.PlanBlock",
        "com.apptolast.organization.adapter.http.TodayController",
        "com.apptolast.organization.adapter.http.BlockController"
      })
  void s34_todayAndSchedulingIgnoreTheExternalCalendar(String isolated) {
    noClasses()
        .that()
        .haveFullyQualifiedName(isolated)
        .should()
        .dependOnClassesThat()
        .haveNameMatching(SLICE_NAMES)
        .check(CLASSES);
  }

  @Test
  void s34_nothingOutsideTheSliceDependsOnTheExternalCalendarTypes() {
    noClasses()
        .that()
        .haveNameNotMatching(
            ".*(ExternalCalendar|ExternalEvent|IcsFeed|SyncSummary|SyncStatus|FeedError|SecretCipher|CalendarFeed|FeedFetch|StoredSubscription|OutboundGuard|OutboundHostGuard|AddressPolicy|HostResolver|SyncOutcome|ConnectorsGate|ConnectorCipher|AesGcmSecretCipher|SystemHostResolver|SecretUrl).*")
        .and()
        .haveNameNotMatching(".*(ApplicationConfiguration|SecurityConfiguration|ApiErrors).*")
        .should()
        .dependOnClassesThat()
        .haveNameMatching(
            ".*\\.(ExternalCalendarStore|SyncExternalCalendar|ReadExternalCalendarEvents)")
        .check(CLASSES);
  }

  @Test
  void s30_noSchedulerTouchesTheExternalCalendar() throws Exception {
    var configuration =
        Files.readString(
            Path.of(
                "src/main/java/com/apptolast/organization/adapter/config/PublisherSchedule.java"));
    org.junit.jupiter.api.Assertions.assertFalse(
        configuration.contains("ExternalCalendar"),
        "el único planificador del backend no puede disparar sincronizaciones");
    org.junit.jupiter.api.Assertions.assertTrue(
        EXTERNAL_CALENDAR_TYPES.stream()
            .allMatch(name -> CLASSES.stream().anyMatch(type -> type.getName().equals(name))),
        "la lista de tipos vigilados debe seguir existiendo");
  }

  @Test
  void s30_noApplicationRunnerSyncsAtStartup() {
    noClasses()
        .that()
        .haveNameMatching(".*ExternalCalendar.*")
        .should()
        .beAssignableTo(org.springframework.boot.ApplicationRunner.class)
        .orShould()
        .beAssignableTo(org.springframework.boot.CommandLineRunner.class)
        .check(CLASSES);
  }
}
