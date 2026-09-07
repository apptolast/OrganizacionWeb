package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class ReadWorkSessionChangesTest {
  @Test
  void s13_missingKeyHasTheSameNotFound() {
    var queries = mock(WorkSessionTransitionQueries.class);
    assertThatThrownBy(
            () -> new ReadWorkSessionChanges(queries).byRequest("owner", UUID.randomUUID()))
        .isInstanceOf(WorkSessionChangeNotFoundException.class);
  }

  @Test
  void s13_missingReceiptHasItsOwnNotFound() {
    var queries = mock(WorkSessionTransitionQueries.class);
    assertThatThrownBy(() -> new ReadWorkSessionChanges(queries).detail("owner", UUID.randomUUID()))
        .isInstanceOf(WorkSessionChangeNotFoundException.class);
  }

  @Test
  void s25_keyReturnsTheSameDurableReceipt() {
    var key = UUID.randomUUID();
    var receipt = mock(WorkSessionTransitionReceipt.class);
    var queries = mock(WorkSessionTransitionQueries.class);
    when(queries.changeByRequest("owner", key)).thenReturn(Optional.of(receipt));
    ReadWorkSessionChangesUseCase useCase = new ReadWorkSessionChanges(queries);
    assertThat(useCase.byRequest("owner", key)).isSameAs(receipt);
    verify(queries).changeByRequest("owner", key);
    verifyNoMoreInteractions(queries);
  }

  @Test
  void s25_detailReturnsTheDurableReceiptForOwner() {
    var id = UUID.randomUUID();
    var receipt = mock(WorkSessionTransitionReceipt.class);
    var queries = mock(WorkSessionTransitionQueries.class);
    when(queries.changeDetail("owner", id)).thenReturn(Optional.of(receipt));
    ReadWorkSessionChangesUseCase useCase = new ReadWorkSessionChanges(queries);
    assertThat(useCase.detail("owner", id)).isSameAs(receipt);
    verify(queries).changeDetail("owner", id);
    verifyNoMoreInteractions(queries);
  }

  @Test
  void s26_closureReturnsDurableReceiptByOwnedSession() {
    var session = UUID.randomUUID();
    var receipt = mock(WorkSessionTransitionReceipt.class);
    var queries = mock(WorkSessionTransitionQueries.class);
    when(queries.closure("owner", session)).thenReturn(Optional.of(receipt));
    ReadWorkSessionChangesUseCase useCase = new ReadWorkSessionChanges(queries);
    assertThat(useCase.closure("owner", session)).isSameAs(receipt);
    verify(queries).closure("owner", session);
    verifyNoMoreInteractions(queries);
  }
}
