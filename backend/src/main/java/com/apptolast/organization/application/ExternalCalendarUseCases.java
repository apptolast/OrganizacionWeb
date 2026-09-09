package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalCalendarSubscription;
import com.apptolast.organization.domain.ExternalEventsRange;
import java.util.Optional;

/** Puertos de entrada de la feature 28, agrupados por ser de una sola operación cada uno. */
public final class ExternalCalendarUseCases {
  private ExternalCalendarUseCases() {}

  public interface Read {
    Optional<ExternalCalendarSubscription> execute(String ownerId);
  }

  public interface Save {
    ExternalCalendarSubscription execute(String ownerId, String label, String url);
  }

  public interface Delete {
    void execute(String ownerId);
  }

  public interface Sync {
    SyncOutcome execute(String ownerId, boolean onlyIfStale);
  }

  public interface ReadEvents {
    ExternalEventsView execute(String ownerId, ExternalEventsRange range);
  }
}
