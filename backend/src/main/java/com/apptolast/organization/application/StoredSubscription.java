package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalCalendarSubscription;

/**
 * La suscripción publicable más lo que solo circula por dentro: la dirección cifrada y la versión
 * con la que se resuelve la concurrencia optimista.
 */
public record StoredSubscription(
    ExternalCalendarSubscription subscription, byte[] urlCiphertext, long version) {}
