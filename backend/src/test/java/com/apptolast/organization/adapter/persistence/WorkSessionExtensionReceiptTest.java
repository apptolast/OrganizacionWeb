package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.WorkSessionTransitionReceipt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

class WorkSessionExtensionReceiptTest {
  @Test
  void s22_readsThePersistedPauseShapeWithoutExtension() throws Exception {
    var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    var historical =
        """
        {"id":"12345678-1234-1234-1234-123456789abc","sessionId":"22345678-1234-1234-1234-123456789abc","action":"PAUSE","occurredAt":"2026-09-07T10:00:01Z",
        "before":{"session":{"id":"22345678-1234-1234-1234-123456789abc","projectId":"32345678-1234-1234-1234-123456789abc","taskId":"42345678-1234-1234-1234-123456789abc","startedAt":"2026-09-07T10:00:00Z","plannedMinutes":25,"plannedEndAt":"2026-09-07T10:25:00Z","zoneId":"UTC"},"status":"running","revision":1,"changedAt":"2026-09-07T10:00:00Z","workedMicroseconds":0,"runningSince":"2026-09-07T10:00:00Z"},
        "after":{"session":{"id":"22345678-1234-1234-1234-123456789abc","projectId":"32345678-1234-1234-1234-123456789abc","taskId":"42345678-1234-1234-1234-123456789abc","startedAt":"2026-09-07T10:00:00Z","plannedMinutes":25,"plannedEndAt":"2026-09-07T10:25:00Z","zoneId":"UTC"},"status":"paused","revision":2,"changedAt":"2026-09-07T10:00:01Z","workedMicroseconds":1000000,"runningSince":null}}
        """;
    var receipt = mapper.readValue(historical, WorkSessionTransitionReceipt.class);
    assertThat(receipt.action()).isEqualTo("PAUSE");
    assertThat(receipt.before().revision()).isEqualTo(1);
    assertThat(receipt.after().revision()).isEqualTo(2);
    assertThat(receipt.after().workedMicroseconds()).isEqualTo(1000000);
    assertThat(receipt.before().session()).isEqualTo(receipt.after().session());
    assertThat(receipt.closure()).isNull();
    assertThat(receipt.extension()).isNull();
  }
}
