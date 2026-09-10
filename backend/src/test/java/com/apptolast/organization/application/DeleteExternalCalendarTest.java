package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apptolast.organization.domain.ExternalCalendarInput;
import com.apptolast.organization.domain.ExternalEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @s7 y @s33: borrar arrastra la instantánea, es idempotente y no toca a nadie más.
 *     <p>El caso de uso tampoco tenía prueba propia. Aquí no hay mutante que matar —la clase no
 *     genera ninguno con los operadores por defecto de PIT, porque su único cuerpo es una llamada
 *     que devuelve valor y se descarta—, de modo que la presión no puede venir de la mutación: sin
 *     esta clase, vaciar {@code execute} de contenido dejaría la suite verde. Está razonado en
 *     progress/bloqueantes_external_calendar.md, B6.
 */
class DeleteExternalCalendarTest {
  static final String A = "persona-a";
  static final String B = "persona-b";
  static final Instant NOW = Instant.parse("2030-01-07T12:00:00Z");
  static final Instant FROM = Instant.parse("2030-01-08T00:00:00Z");
  static final Instant TO = Instant.parse("2030-01-09T00:00:00Z");
  static final String URL = "https://feed.example.test/calendar/ical/abc123/basic.ics";

  InMemoryExternalCalendarStore store;

  @BeforeEach
  void setUp() {
    store = new InMemoryExternalCalendarStore();
  }

  DeleteExternalCalendar delete() {
    return new DeleteExternalCalendar(store);
  }

  void subscribeWithSnapshot(String owner) {
    store.create(
        owner,
        UUID.randomUUID(),
        ExternalCalendarInput.of("Trabajo", URL),
        URL.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        NOW);
    store.seedSnapshot(
        owner,
        List.of(
            new ExternalEvent(
                "u1",
                "Reunión",
                Instant.parse("2030-01-08T09:00:00Z"),
                Instant.parse("2030-01-08T10:00:00Z"),
                false)));
  }

  @Test
  void s7_deletingDragsTheSubscriptionAndItsSnapshot() {
    subscribeWithSnapshot(A);

    delete().execute(A);

    assertTrue(store.find(A).isEmpty(), "no queda fila de suscripción");
    assertTrue(store.events(A, FROM, TO).isEmpty(), "no queda ningún evento");
  }

  @Test
  void s7_deletingWithoutSubscriptionIsIdempotent() {
    assertDoesNotThrow(() -> delete().execute(A));
    assertTrue(store.find(A).isEmpty());

    subscribeWithSnapshot(A);
    delete().execute(A);
    assertDoesNotThrow(() -> delete().execute(A), "borrar dos veces responde igual que una");
    assertTrue(store.find(A).isEmpty());
  }

  @Test
  void s33_deletingOneOwnerLeavesTheOtherUntouched() {
    subscribeWithSnapshot(A);
    subscribeWithSnapshot(B);

    delete().execute(B);

    assertTrue(store.find(A).isPresent(), "persona-a conserva su suscripción");
    assertEquals(1, store.events(A, FROM, TO).size(), "y sus eventos");
    assertTrue(store.find(B).isEmpty());
  }
}
