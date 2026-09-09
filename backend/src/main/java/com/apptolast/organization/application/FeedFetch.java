package com.apptolast.organization.application;

import com.apptolast.organization.domain.FeedError;

/** Resultado de una descarga: o el texto del feed, o el código cerrado que explica el fallo. */
public sealed interface FeedFetch {
  record Downloaded(String text) implements FeedFetch {}

  record Failed(FeedError code) implements FeedFetch {}

  static FeedFetch downloaded(String text) {
    return new Downloaded(text);
  }

  static FeedFetch failed(FeedError code) {
    return new Failed(code);
  }
}
