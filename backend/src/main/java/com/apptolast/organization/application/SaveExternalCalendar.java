package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalCalendarInput;
import com.apptolast.organization.domain.ExternalCalendarSubscription;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Reemplaza la suscripción del propietario. Valida la sintaxis y resuelve el host antes de guardar;
 * nunca descarga el feed. Cambiar la dirección borra la instantánea; cambiar solo la etiqueta la
 * conserva; no cambiar nada no escribe.
 */
public final class SaveExternalCalendar implements ExternalCalendarUseCases.Save {
  private final ExternalCalendarStore store;
  private final SecretCipher cipher;
  private final OutboundGuard guard;
  private final Clock clock;

  public SaveExternalCalendar(
      ExternalCalendarStore store, SecretCipher cipher, OutboundGuard guard, Clock clock) {
    this.store = store;
    this.cipher = cipher;
    this.guard = guard;
    this.clock = clock;
  }

  @Override
  public ExternalCalendarSubscription execute(String ownerId, String label, String url) {
    var input = ExternalCalendarInput.of(label, url);
    reject(guard.check(input.urlHost()));
    var now = clock.instant();
    var existing = store.find(ownerId);
    if (existing.isEmpty())
      return store
          .create(ownerId, UUID.randomUUID(), input, cipher.encrypt(ownerId, input.url()), now)
          .subscription();
    var current = existing.get();
    boolean sameUrl =
        cipher.decrypt(ownerId, current.urlCiphertext()).filter(input.url()::equals).isPresent();
    if (sameUrl && current.subscription().label().equals(input.label()))
      return current.subscription();
    var sealed = cipher.encrypt(ownerId, input.url());
    return sameUrl
        ? store.relabel(ownerId, input.label(), sealed, now).subscription()
        : store.rebind(ownerId, input, sealed, now).subscription();
  }

  private static void reject(OutboundGuard.Verdict verdict) {
    if (verdict == OutboundGuard.Verdict.ALLOWED) return;
    var code =
        verdict == OutboundGuard.Verdict.UNRESOLVABLE ? "UNRESOLVABLE_HOST" : "BLOCKED_ADDRESS";
    var message =
        verdict == OutboundGuard.Verdict.UNRESOLVABLE
            ? "No se ha podido resolver el nombre de esa dirección."
            : "Esa dirección apunta a una red interna y no se puede usar.";
    throw new ValidationException(List.of(new FieldError("url", code, message)));
  }
}
