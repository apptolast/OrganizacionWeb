package com.apptolast.organization.application;

import com.apptolast.organization.domain.CalendarFeedSecret;
import com.apptolast.organization.domain.CalendarSnapshot;
import com.apptolast.organization.domain.CalendarWindow;
import com.apptolast.organization.domain.IcsCalendar;
import java.time.Clock;

/**
 * Turns a public candidate, or an owner already known by the session, into the iCalendar document.
 * The window and the event ceiling are fixed by the contract and take no parameters.
 */
public final class RenderCalendar implements RenderCalendarUseCase {
  private static final int MAX_EVENTS = 2000;

  private final CalendarFeedTokens tokens;
  private final CalendarQueries calendars;
  private final String publicOrigin;
  private final Clock clock;

  public RenderCalendar(
      CalendarFeedTokens tokens, CalendarQueries calendars, String publicOrigin, Clock clock) {
    this.tokens = tokens;
    this.calendars = calendars;
    this.publicOrigin = publicOrigin;
    this.clock = clock;
  }

  /** Any candidate resolves through the full digest; an unresolved one never reaches the feed. */
  @Override
  public String forToken(String candidate) {
    var owner =
        tokens
            .ownerOf(CalendarFeedSecret.fingerprintOf(candidate))
            .orElseThrow(CalendarNotFoundException::new);
    return forOwner(owner);
  }

  @Override
  public String forOwner(String owner) {
    var window = CalendarWindow.around(clock.instant());
    var snapshot = calendars.read(owner, window.from(), window.to());
    return IcsCalendar.render(publicOrigin, withinCeiling(snapshot));
  }

  /** Refuses before a single byte of document exists, so no partial calendar can escape. */
  private static CalendarSnapshot withinCeiling(CalendarSnapshot snapshot) {
    if (snapshot.entries().size() > MAX_EVENTS) throw new CalendarTooLargeException();
    return snapshot;
  }
}
