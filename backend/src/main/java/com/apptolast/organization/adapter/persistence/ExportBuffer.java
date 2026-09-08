package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ExportTooLargeException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

final class ExportBuffer extends OutputStream {
  static final int LIMIT = 33554432;
  private static final int SEGMENT = 65536;
  private final List<byte[]> segments = new ArrayList<>();
  private int size;

  int size() {
    return size;
  }

  public void write(int value) {
    if (size == LIMIT) throw new ExportTooLargeException();
    int offset = size % SEGMENT;
    if (offset == 0) segments.add(new byte[SEGMENT]);
    segments.getLast()[offset] = (byte) value;
    size++;
  }

  public void write(byte[] bytes, int offset, int length) {
    java.util.Objects.checkFromIndexSize(offset, length, bytes.length);
    if (length > LIMIT - size) throw new ExportTooLargeException();
    while (length > 0) {
      int position = size % SEGMENT;
      if (position == 0) segments.add(new byte[SEGMENT]);
      int count = Math.min(length, SEGMENT - position);
      System.arraycopy(bytes, offset, segments.getLast(), position, count);
      size += count;
      offset += count;
      length -= count;
    }
  }

  void writeTo(OutputStream output) throws IOException {
    int remaining = size;
    for (var segment : segments) {
      int count = Math.min(remaining, SEGMENT);
      output.write(segment, 0, count);
      remaining -= count;
    }
  }
}
