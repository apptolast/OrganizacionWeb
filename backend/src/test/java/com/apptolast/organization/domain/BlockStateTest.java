package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class BlockStateTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "2,1,cancelled,BLOCK_CONFLICT",
    "2,2,cancelled,BLOCK_CANCELLED",
    "9223372036854775807,9223372036854775807,planned,BLOCK_VERSION_EXHAUSTED",
    "9223372036854775807,9223372036854775807,cancelled,BLOCK_CANCELLED",
    "2,1,planned,BLOCK_CONFLICT"
  })
  void s6_revisionStateAndExhaustionProtectCancellationInOrder(
      long version, long expected, String status, String code) {
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var request =
        new BlockRequest(
            "Meta", start, start.plusHours(1), "UTC", ZoneOffset.UTC, ZoneOffset.UTC, false);
    var time =
        new ResolvedBlockTime(
            start.toInstant(ZoneOffset.UTC),
            start.plusHours(1).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            60);
    var block =
        new PlannedBlock(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), request, time, Instant.EPOCH);
    var prior = new BlockState(block, version, status, Instant.EPOCH);
    assertThatThrownBy(() -> prior.cancel(expected, Instant.EPOCH)).hasMessage(code);
  }

  @Test
  void s12_s13_cancellationPreservesLastBlockWhenClockMovesBack() {
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var request =
        new BlockRequest(
            "Meta", start, start.plusHours(1), "UTC", ZoneOffset.UTC, ZoneOffset.UTC, false);
    var time =
        new ResolvedBlockTime(
            start.toInstant(ZoneOffset.UTC),
            start.plusHours(1).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            60);
    var block =
        new PlannedBlock(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            request,
            time,
            Instant.parse("2030-01-06T10:00:00Z"));
    var prior = new BlockState(block, 2, "planned", Instant.parse("2030-01-07T09:05:00Z"));
    var next = prior.cancel(2, Instant.parse("2030-01-07T09:04:00.123456Z"));
    assertThat(next.block()).isSameAs(block);
    assertThat(next.version()).isEqualTo(3);
    assertThat(next.status()).isEqualTo("cancelled");
    assertThat(next.updatedAt()).isEqualTo(Instant.parse("2030-01-07T09:04:00.123456Z"));
    assertThat(prior.status()).isEqualTo("planned");
  }

  @Test
  void s1_originalCreationDefinesInitialStateWithoutNewFacts() {
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var request =
        new BlockRequest(
            "Preparar borrador",
            start,
            start.plusHours(1),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    var time =
        new ResolvedBlockTime(
            start.toInstant(ZoneOffset.UTC),
            start.plusHours(1).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            60);
    var block =
        new PlannedBlock(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            request,
            time,
            Instant.parse("2030-01-06T10:00:00Z"));
    var state = BlockState.initial(block);
    assertThat(state.block()).isSameAs(block);
    assertThat(state.version()).isEqualTo(1);
    assertThat(state.status()).isEqualTo("planned");
    assertThat(state.updatedAt()).isEqualTo(block.createdAt());
  }
}
