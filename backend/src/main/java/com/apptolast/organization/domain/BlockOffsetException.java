package com.apptolast.organization.domain;

import java.time.ZoneOffset;
import java.util.List;

public final class BlockOffsetException extends RuntimeException {
  private final FieldError error;
  private final List<ZoneOffset> validOffsets;

  public BlockOffsetException(FieldError error, List<ZoneOffset> validOffsets) {
    super("Revisa la ocurrencia de la hora indicada.");
    this.error = error;
    this.validOffsets = List.copyOf(validOffsets);
  }

  public FieldError error() {
    return error;
  }

  public List<ZoneOffset> validOffsets() {
    return validOffsets;
  }
}
