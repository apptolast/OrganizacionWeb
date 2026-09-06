package com.apptolast.organization.domain;

import java.time.Instant;

public record BlockState(PlannedBlock block, long version, String status, Instant updatedAt) {
  public BlockState cancel(long expected, Instant now) {
    if (version != expected) throw new BlockStateException("BLOCK_CONFLICT");
    if (status.equals("cancelled")) throw new BlockStateException("BLOCK_CANCELLED");
    if (version == Long.MAX_VALUE) throw new BlockStateException("BLOCK_VERSION_EXHAUSTED");
    return new BlockState(block, version + 1, "cancelled", now);
  }

  public static BlockState initial(PlannedBlock block) {
    return new BlockState(block, 1, "planned", block.createdAt());
  }
}
