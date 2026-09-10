package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * La frontera de los cuatro contadores. PIT genera un mutante de frontera por cada uno de los
 * cuatro {@code < 0} de {@code SyncSummary:17}; tres morían y el cuarto sobrevivía porque ninguna
 * prueba construía un resumen con ese contador exactamente en cero. Cero no es negativo: una
 * sincronización que no importa nada, o que no se salta nada, es una sincronización correcta.
 */
class SyncSummaryTest {

  @Test
  void everyCounterAtExactlyZeroIsAValidSummary() {
    var summary = new SyncSummary("UTC", 0, 0, 0, 0, false);

    assertEquals(0, summary.imported());
    assertEquals(0, summary.skippedRecurring());
    assertEquals(0, summary.skippedCancelled());
    assertEquals(0, summary.skippedInvalid());
  }

  @ParameterizedTest
  @CsvSource({"-1, 0, 0, 0", "0, -1, 0, 0", "0, 0, -1, 0", "0, 0, 0, -1"})
  void anyNegativeCounterIsRejected(
      int imported, int skippedRecurring, int skippedCancelled, int skippedInvalid) {
    var error =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new SyncSummary(
                    "UTC", imported, skippedRecurring, skippedCancelled, skippedInvalid, false));

    assertEquals("Los contadores no pueden ser negativos", error.getMessage());
  }
}
