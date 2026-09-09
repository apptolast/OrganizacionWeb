package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationCursor;
import com.apptolast.organization.domain.AutomationRun;
import java.util.List;
import java.util.Optional;

/**
 * The single transactional port of the worker. Everything one event produces travels in one {@link
 * AutomationCommit} so the run, the effect, the emitted event and the cursor land together or not
 * at all.
 */
public interface AutomationWork {
  /** Owners with at least one rule, enabled or not: a disabled rule still moves the cursor. */
  List<String> ownersWithRules();

  Optional<AutomationCursor> cursor(String owner);

  void startCursor(String owner, AutomationCursor present);

  /** The rows of this owner that sit after the cursor, in tuple order. */
  List<AutomationCandidate> after(String owner, AutomationCursor from);

  List<AutomationRetry> pendingRetries(String owner);

  /** Throws when the storage is unavailable, leaving nothing of the commit behind. */
  void commit(AutomationCommit commit);

  /** Writes one run row on its own, outside any rolled back transaction. */
  void record(AutomationRun run);
}
