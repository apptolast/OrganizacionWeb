package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.RenderCalendarUseCase;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs a whole render inside one read-only repeatable-read transaction, so that resolving the
 * token, reading the availability zone and reading the blocks share a single snapshot and a
 * concurrent move lands entirely inside or entirely outside the document.
 */
public final class SnapshotRenderCalendar implements RenderCalendarUseCase {
  private final RenderCalendarUseCase delegate;
  private final TransactionTemplate snapshot;

  public SnapshotRenderCalendar(
      RenderCalendarUseCase delegate, PlatformTransactionManager manager) {
    this.delegate = delegate;
    this.snapshot = new TransactionTemplate(manager);
    this.snapshot.setReadOnly(true);
    this.snapshot.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  @Override
  public String forToken(String candidate) {
    return snapshot.execute(status -> delegate.forToken(candidate));
  }

  @Override
  public String forOwner(String owner) {
    return snapshot.execute(status -> delegate.forOwner(owner));
  }
}
