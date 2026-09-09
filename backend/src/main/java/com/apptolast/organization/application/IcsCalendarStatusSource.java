package com.apptolast.organization.application;

/**
 * La fila del feed iCalendar (26). Un token activo es toda la conexión que hay: no se guarda ni
 * huella de cuándo se leyó el feed, así que {@code lastActivityAt} es nulo por honestidad y no por
 * olvido. Revocar el token devuelve la fila a {@code not_connected}.
 */
public final class IcsCalendarStatusSource implements ConnectorStatusSource {
  public static final String ID = "ics_calendar";

  private final CalendarFeedTokens tokens;

  public IcsCalendarStatusSource(CalendarFeedTokens tokens) {
    this.tokens = tokens;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public boolean encryptsSecrets() {
    return false;
  }

  @Override
  public ConnectorRow read(String ownerId) {
    return tokens.createdAt(ownerId).isPresent()
        ? ConnectorRow.connected(ID, null)
        : ConnectorRow.notConnected(ID);
  }
}
