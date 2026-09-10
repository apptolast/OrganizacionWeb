package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apptolast.organization.domain.ExternalCalendarInput;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @s1 y @s33: leer la suscripción del propietario, y solo la suya.
 *     <p>Este caso de uso no tenía ninguna prueba propia: las dos que lo rozaban son
 *     {@code @WebMvcTest} con el caso de uso simulado, de modo que afirmaban sobre lo que el propio
 *     doble devolvía. El resultado medible era que el único mutante de la clase —hacer que la
 *     lectura conteste siempre «sin suscripción»— salía sin cobertura.
 */
class ReadExternalCalendarTest {
  static final String A = "persona-a";
  static final String B = "persona-b";
  static final Instant NOW = Instant.parse("2030-01-07T12:00:00Z");
  static final String URL = "https://feed.example.test/calendar/ical/abc123/basic.ics";

  InMemoryExternalCalendarStore store;

  @BeforeEach
  void setUp() {
    store = new InMemoryExternalCalendarStore();
  }

  ReadExternalCalendar read() {
    return new ReadExternalCalendar(store);
  }

  UUID subscribe(String owner, String label) {
    var id = UUID.randomUUID();
    store.create(
        owner,
        id,
        ExternalCalendarInput.of(label, URL),
        URL.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        NOW);
    return id;
  }

  @Test
  void s1_readingAnExistingSubscriptionReturnsTheStoredOne() {
    var id = subscribe(A, "Trabajo");

    var found = read().execute(A);

    assertTrue(found.isPresent(), "la suscripción guardada tiene que leerse");
    assertEquals(id, found.orElseThrow().id());
    assertEquals("Trabajo", found.orElseThrow().label());
    assertEquals("feed.example.test", found.orElseThrow().urlHost());
    assertSame(
        store.find(A).orElseThrow().subscription(),
        found.orElseThrow(),
        "se publica la suscripción guardada, no una copia recompuesta");
  }

  @Test
  void s1_readingWithoutSubscriptionIsEmptyAndInsertsNothing() {
    assertTrue(read().execute(A).isEmpty());
    assertTrue(store.find(A).isEmpty(), "leer la ausencia no crea ninguna fila");
  }

  @Test
  void s33_theSubscriptionOfAnotherOwnerIsNeverReturned() {
    subscribe(B, "La suya");

    assertTrue(read().execute(A).isEmpty());
    assertEquals("La suya", read().execute(B).orElseThrow().label());
  }
}
