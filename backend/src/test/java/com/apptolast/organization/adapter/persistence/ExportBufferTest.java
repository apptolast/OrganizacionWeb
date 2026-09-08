package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.apptolast.organization.application.ExportTooLargeException;
import org.junit.jupiter.api.Test;

class ExportBufferTest {
  @Test
  void s15_acceptsExactly32MiBAndRejectsTheNextByteBeforeGrowing() throws Exception {
    var buffer = new ExportBuffer();
    var block = new byte[65536];
    for (int index = 0; index < 512; index++) buffer.write(block);
    assertThat(buffer.size()).isEqualTo(33554432L);
    assertThatThrownBy(() -> buffer.write(1)).isInstanceOf(ExportTooLargeException.class);
    assertThat(buffer.size()).isEqualTo(33554432L);
  }

  @Test
  void s15_s17_bulkBytesCrossSegmentsAndRejectedBulkPreservesPriorContent() throws Exception {
    var pattern = new byte[131111];
    for (int index = 0; index < pattern.length; index++) pattern[index] = (byte) (index % 251);
    var buffer = new ExportBuffer();
    buffer.write(pattern, 7, 131090);
    buffer.write(254);
    var expected = new java.io.ByteArrayOutputStream();
    expected.write(pattern, 7, 131090);
    expected.write(254);
    var output = new java.io.ByteArrayOutputStream();
    buffer.writeTo(output);
    assertThat(output.toByteArray()).isEqualTo(expected.toByteArray());
    assertThatThrownBy(() -> buffer.write(new byte[33554432], 0, 33554432))
        .isInstanceOf(ExportTooLargeException.class);
    assertThat(buffer.size()).isEqualTo(131091);
    var after = new java.io.ByteArrayOutputStream();
    buffer.writeTo(after);
    assertThat(after.toByteArray()).isEqualTo(expected.toByteArray());
  }
}
