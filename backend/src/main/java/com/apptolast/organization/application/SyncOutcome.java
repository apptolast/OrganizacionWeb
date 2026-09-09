package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalCalendarSubscription;

/**
 * {@code performed} es falso solo cuando no se llegó a escribir: porque la instantánea aún estaba
 * fresca o porque otra escritura ganó la carrera por la versión. Un fallo de descarga sí se
 * realiza.
 */
public record SyncOutcome(boolean performed, ExternalCalendarSubscription subscription) {}
