package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationEvent;
import java.util.List;

/** The most recent non-blocked events of this owner, newest first, capped at the given limit. */
@FunctionalInterface
public interface AutomationEventTail {
  List<AutomationEvent> recent(String owner, int limit);
}
