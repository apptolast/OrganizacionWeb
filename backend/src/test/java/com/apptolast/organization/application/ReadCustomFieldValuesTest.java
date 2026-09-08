package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadCustomFieldValuesTest {
  @Test
  void s10_readsOwnedTaskWithSchemaRevisionAndNullActiveFieldWithoutInventingValuesRevision() {
    var projectId = UUID.randomUUID();
    var taskId = UUID.randomUUID();
    var schema = new CustomizationRevision(UUID.randomUUID(), 4);
    var field = new CustomFieldValue(UUID.randomUUID(), "Dato", CustomFieldType.TEXT, null);
    var expected =
        new CustomFieldValues(
            taskId,
            CustomizationScope.TASK,
            schema,
            new CustomizationRevision(null, 0),
            List.of(field),
            null);
    var read =
        new ReadCustomFieldValues(
            (owner, scope, project, entity) -> {
              assertThat(owner).isEqualTo("owner-a");
              assertThat(scope).isEqualTo(CustomizationScope.TASK);
              assertThat(project).isEqualTo(projectId);
              assertThat(entity).isEqualTo(taskId);
              return expected;
            });
    assertThat(read.get("owner-a", CustomizationScope.TASK, projectId, taskId)).isSameAs(expected);
  }
}
