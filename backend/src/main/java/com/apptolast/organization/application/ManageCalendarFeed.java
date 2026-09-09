package com.apptolast.organization.application;

import com.apptolast.organization.domain.CalendarFeedSecret;
import java.security.SecureRandom;
import java.time.Clock;

public final class ManageCalendarFeed implements ManageCalendarFeedUseCase {
  private final CalendarFeedTokens tokens;
  private final String publicOrigin;
  private final Clock clock;
  private final SecureRandom random;

  public ManageCalendarFeed(
      CalendarFeedTokens tokens, String publicOrigin, Clock clock, SecureRandom random) {
    this.tokens = tokens;
    this.publicOrigin = publicOrigin;
    this.clock = clock;
    this.random = random;
  }

  @Override
  public CalendarFeedLink generate(String owner) {
    var secret = CalendarFeedSecret.issue(random);
    var createdAt = CustomizationTime.capture(clock);
    tokens.replace(owner, secret.fingerprint(), createdAt);
    return new CalendarFeedLink(CalendarFeedAddress.of(publicOrigin, secret.token()), createdAt);
  }

  @Override
  public CalendarFeedStatus status(String owner) {
    return tokens
        .createdAt(owner)
        .map(createdAt -> new CalendarFeedStatus(true, createdAt))
        .orElseGet(() -> new CalendarFeedStatus(false, null));
  }

  @Override
  public void revoke(String owner) {
    tokens.revoke(owner);
  }
}
