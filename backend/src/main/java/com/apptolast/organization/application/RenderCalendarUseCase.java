package com.apptolast.organization.application;

public interface RenderCalendarUseCase {
  /** The public feed: the candidate is an untrusted path segment, never assumed well formed. */
  String forToken(String candidate);

  /** The session download: the owner is already known, so no token is read or written. */
  String forOwner(String owner);
}
