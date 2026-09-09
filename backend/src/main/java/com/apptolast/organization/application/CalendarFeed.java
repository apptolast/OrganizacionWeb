package com.apptolast.organization.application;

/** Descarga el texto de un calendario externo. Nunca escribe en el proveedor. */
public interface CalendarFeed {
  FeedFetch fetch(String url);
}
