package com.apptolast.organization.application;
import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
public final class CancelBlock implements CancelBlockUseCase {
  private final BlockEditing store;
  private final Clock clock;
  public CancelBlock(BlockEditing store,Clock clock) { this.store=store;this.clock=clock; }
  public BlockChangeConfirmation cancel(String owner,UUID project,UUID task,UUID block,UUID key,long expected) {
    return store.cancel(owner,project,task,block,key,prior -> {
      var now=clock.instant().truncatedTo(ChronoUnit.MICROS);
      var next=prior.cancel(expected,now);
      var receipt=new BlockChangeReceipt(UUID.randomUUID(),block,"CANCELLED",next.version(),now,prior.block(),null);
      var event=new BlockChanged(UUID.randomUUID(),project,owner,now,1,"BlockChanged.v1",receipt.id(),block,task,"CANCELLED",next.version(),BlockChanged.Interval.from(prior.block()),null);
      return new BlockMutation(next,receipt,event);
    });
  }
}
