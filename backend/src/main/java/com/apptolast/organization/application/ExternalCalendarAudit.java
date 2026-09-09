package com.apptolast.organization.application;

import com.apptolast.organization.domain.FeedError;
import com.apptolast.organization.domain.SyncStatus;

/**
 * Deja constancia de cada intento. Recibe el host y nunca la ruta, porque la ruta es el secreto de
 * la suscripción.
 */
public interface ExternalCalendarAudit {
  void syncFinished(String urlHost, SyncStatus status, FeedError error, long millis);
}
