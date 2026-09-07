package com.apptolast.organization.adapter;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.CustomizationController;
import com.apptolast.organization.application.ReadCustomizationUseCase;
import com.apptolast.organization.domain.CustomizationScope;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = CustomizationController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class CustomizationApiTest {
  @MockitoBean com.apptolast.organization.application.SaveCustomFieldValuesUseCase saveValues;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "404,RESOURCE_NOT_FOUND",
    "412,CUSTOMIZATION_CONFLICT",
    "503,STORAGE_UNAVAILABLE"
  })
  void s17_valuesWriteReturnsOnlyTheDelegatedFailureWithoutExtraReads(int expected, String code)
      throws Exception {
    var id = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    var revision = new com.apptolast.organization.domain.CustomizationRevision(null, 0);
    var tag =
        new com.apptolast.organization.domain.CustomFieldValuesRevision(
            CustomizationScope.TASK, id, revision, revision);
    RuntimeException failure =
        switch (expected) {
          case 404 -> new com.apptolast.organization.application.ResourceNotFoundException();
          case 412 -> new com.apptolast.organization.application.CustomizationConflictException();
          default ->
              new com.apptolast.organization.application.StorageUnavailableException(
                  new IllegalStateException("private data"));
        };
    when(saveValues.save("owner", CustomizationScope.TASK, id, id, tag, java.util.List.of()))
        .thenThrow(failure);
    mvc.perform(
            put("/api/v1/projects/" + id + "/tasks/" + id + "/custom-fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header(
                    "If-Match",
                    "\"custom-values:TASK:" + id + ":schema:unconfigured:values:unconfigured\"")
                .contentType("application/json")
                .content("{\"values\":[]}"))
        .andExpect(status().is(expected))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.values").doesNotExist())
        .andExpect(header().doesNotExist("ETag"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("private data"))));
    verify(saveValues).save("owner", CustomizationScope.TASK, id, id, tag, java.util.List.of());
    verifyNoInteractions(readValues, read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "{",
        "{\"values\":[],\"values\":[]}",
        "{\"values\":[]} {}",
        "{\"values\":[{\"fieldId\":\"a\",\"fieldId\":\"b\",\"value\":null}]}"
      })
  void s25_valuesMalformedJsonIsRejectedBeforeDelegation(String body) throws Exception {
    var id = "abcdefab-1111-1111-1111-111111111111";
    mvc.perform(
            put("/api/v1/projects/" + id + "/custom-fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header(
                    "If-Match",
                    "\"custom-values:PROJECT:" + id + ":schema:unconfigured:values:unconfigured\"")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(saveValues, readValues, read, save, create, update);
  }

  @Test
  void s25_duplicateFieldIdPrecedesMissingValueAndStaleRevision() throws Exception {
    var id = "abcdefab-1111-1111-1111-111111111111";
    mvc.perform(
            put("/api/v1/projects/" + id + "/custom-fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header(
                    "If-Match",
                    "\"custom-values:PROJECT:" + id + ":schema:" + id + ":0:values:" + id + ":0\"")
                .contentType("application/json")
                .content(
                    "{\"values\":[{\"fieldId\":\""
                        + id
                        + "\",\"value\":null},{\"fieldId\":\""
                        + id.toUpperCase()
                        + "\"}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("values[1].fieldId"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(saveValues, readValues, read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "PROJECT,anonymous,401", "TASK,anonymous,401",
    "PROJECT,csrf,403", "TASK,csrf,403",
    "PROJECT,origin,403", "TASK,origin,403",
    "PROJECT,content,415", "TASK,content,415",
    "PROJECT,accept,406", "TASK,accept,406",
    "PROJECT,query,400", "TASK,query,400"
  })
  void s23_valuesWritesRejectSecurityAndNegotiationBeforeDelegation(
      CustomizationScope scope, String mode, int expected) throws Exception {
    var id = "abcdefab-1111-1111-1111-111111111111";
    var request =
        put("/api/v1/projects/"
                + id
                + (scope == CustomizationScope.TASK ? "/tasks/" + id : "")
                + "/custom-fields")
            .contentType(mode.equals("content") ? "text/plain" : "application/json")
            .content("{");
    if (!mode.equals("anonymous")) request.with(user("owner"));
    if (!mode.equals("csrf")) request.with(csrf().asHeader());
    if (mode.equals("origin")) request.header("Origin", "https://foreign.example");
    if (mode.equals("accept")) request.accept("application/json;q=0, */*;q=1");
    if (mode.equals("query")) request.queryParam("extra", "1");
    mvc.perform(request).andExpect(status().is(expected)).andExpect(header().doesNotExist("ETag"));
    verifyNoInteractions(saveValues, readValues, read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"null", "false", "\"  texto  \"", "0", "1.0", "1e3", "1e999"})
  void s11_valuesPrimitivesReachTheDomainWithoutTypeGuessing(String rawValue) throws Exception {
    var id = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    var revision = new com.apptolast.organization.domain.CustomizationRevision(null, 0);
    var expected =
        new com.apptolast.organization.domain.CustomFieldValuesRevision(
            CustomizationScope.PROJECT, id, revision, revision);
    when(saveValues.save(
            eq("owner"), eq(CustomizationScope.PROJECT), eq(id), eq(id), eq(expected), anyList()))
        .thenAnswer(
            call -> {
              java.util.List<com.apptolast.organization.domain.CustomFieldInput> inputs =
                  call.getArgument(5);
              org.junit.jupiter.api.Assertions.assertEquals(1, inputs.size());
              org.junit.jupiter.api.Assertions.assertEquals(id, inputs.getFirst().fieldId());
              var value = inputs.getFirst().value();
              if (rawValue.equals("null")) org.junit.jupiter.api.Assertions.assertNull(value);
              else if (rawValue.equals("false"))
                org.junit.jupiter.api.Assertions.assertEquals(false, value);
              else if (rawValue.startsWith("\""))
                org.junit.jupiter.api.Assertions.assertEquals("  texto  ", value);
              else {
                var number =
                    org.junit.jupiter.api.Assertions.assertInstanceOf(
                        java.math.BigDecimal.class, value);
                org.junit.jupiter.api.Assertions.assertEquals(
                    0, number.compareTo(new java.math.BigDecimal(rawValue)));
              }
              return new com.apptolast.organization.domain.CustomFieldValues(
                  id,
                  CustomizationScope.PROJECT,
                  revision,
                  new com.apptolast.organization.domain.CustomizationRevision(id, 0),
                  java.util.List.of(),
                  java.time.Instant.parse("2026-09-07T12:00:00Z"));
            });
    mvc.perform(
            put("/api/v1/projects/" + id + "/custom-fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header(
                    "If-Match",
                    "\"custom-values:PROJECT:" + id + ":schema:unconfigured:values:unconfigured\"")
                .contentType("application/json")
                .content("{\"values\":[{\"fieldId\":\"" + id + "\",\"value\":" + rawValue + "}]}"))
        .andExpect(status().isOk());
    verify(saveValues)
        .save(eq("owner"), eq(CustomizationScope.PROJECT), eq(id), eq(id), eq(expected), anyList());
    verifyNoInteractions(readValues, read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource(
      delimiter = '|',
      textBlock =
          """
      {} | values | REQUIRED
      {"values":null} | values | REQUIRED
      {"values":false} | values | INVALID_TYPE
      {"values":[null]} | values[0] | REQUIRED
      {"values":[0]} | values[0] | INVALID_TYPE
      {"values":[{"z":0,"a":0}]} | values[0].a | UNKNOWN_FIELD
      {"values":[{}]} | values[0].fieldId | REQUIRED
      {"values":[{"fieldId":false}]} | values[0].fieldId | INVALID_TYPE
      {"values":[{"fieldId":"1-1-1-1-1"}]} | values[0].fieldId | INVALID_VALUE
      {"values":[{"fieldId":"abcdefab-1111-1111-1111-111111111111"}]} | values[0].value | REQUIRED
      {"values":[{"fieldId":"abcdefab-1111-1111-1111-111111111111","value":{}}]} | values[0].value | INVALID_TYPE
      {"values":[{"fieldId":"abcdefab-1111-1111-1111-111111111111","value":null},{}]} | values[1].fieldId | REQUIRED
      """)
  void s25_valuesShapeUsesDeclaredFieldsAndInputIndicesBeforeDelegation(
      String body, String field, String code) throws Exception {
    var id = "abcdefab-1111-1111-1111-111111111111";
    mvc.perform(
            put("/api/v1/projects/" + id + "/custom-fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header(
                    "If-Match",
                    "\"custom-values:PROJECT:" + id + ":schema:" + id + ":0:values:" + id + ":0\"")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    verifyNoInteractions(saveValues, readValues, read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "false,428,PRECONDITION_REQUIRED",
    "true,400,VALIDATION_ERROR"
  })
  void s24_valuesTagMustBePresentExactlyOnce(boolean repeated, int expected, String code)
      throws Exception {
    var id = "abcdefab-1111-1111-1111-111111111111";
    var request =
        put("/api/v1/projects/" + id + "/tasks/" + id + "/custom-fields")
            .with(user("owner"))
            .with(csrf().asHeader())
            .contentType("application/json")
            .content("{");
    if (repeated)
      request.header(
          "If-Match",
          "\"custom-values:TASK:" + id + ":schema:unconfigured:values:unconfigured\"",
          "\"custom-values:TASK:" + id + ":schema:unconfigured:values:unconfigured\"");
    mvc.perform(request).andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(code));
    verifyNoInteractions(saveValues, readValues, read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "W/\"custom-values:PROJECT:{id}:schema:unconfigured:values:unconfigured\"",
        "\"custom-values:TASK:{id}:schema:unconfigured:values:unconfigured\"",
        "\"custom-values:PROJECT:abcdefab-2222-2222-2222-222222222222:schema:unconfigured:values:unconfigured\"",
        "\"customization:PROJECT:unconfigured\"",
        "\"custom-values:PROJECT:{id}:schema:{id}:01:values:unconfigured\"",
        "\"custom-values:PROJECT:{id}:schema:unconfigured:values:{id}:9223372036854775808\"",
        "\"custom-values:PROJECT:{id}:schema:ABCDEFAB-1111-1111-1111-111111111111:0:values:unconfigured\"",
        "\"custom-values:PROJECT:{id}:schema:unconfigured:values:unconfigured\", *"
      })
  void s24_valuesTagRejectsNonCanonicalOrUnboundRevisionBeforeBody(String tag) throws Exception {
    var id = "abcdefab-1111-1111-1111-111111111111";
    mvc.perform(
            put("/api/v1/projects/" + id + "/custom-fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", tag.replace("{id}", id))
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("If-Match"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(saveValues, readValues, read, save, create, update);
  }

  @Test
  void s11_taskWritePreservesExactDecimalAndInputOrderForDomainValidation() throws Exception {
    var project = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    var task = java.util.UUID.fromString("abcdefab-2222-2222-2222-222222222222");
    var revision =
        new com.apptolast.organization.domain.CustomizationRevision(project, Long.MAX_VALUE);
    var expected =
        new com.apptolast.organization.domain.CustomFieldValuesRevision(
            CustomizationScope.TASK,
            task,
            revision,
            new com.apptolast.organization.domain.CustomizationRevision(task, 9007199254740993L));
    var inputs =
        java.util.List.of(
            new com.apptolast.organization.domain.CustomFieldInput(
                task, new java.math.BigDecimal("1.0000000000000000000001")),
            new com.apptolast.organization.domain.CustomFieldInput(project, null));
    when(saveValues.save("owner", CustomizationScope.TASK, project, task, expected, inputs))
        .thenThrow(
            new com.apptolast.organization.domain.ValidationException(
                java.util.List.of(
                    new com.apptolast.organization.domain.FieldError(
                        "values[0].value", "INVALID_VALUE", "Entero requerido."))));
    mvc.perform(
            put("/api/v1/projects/"
                    + project.toString().toUpperCase()
                    + "/tasks/"
                    + task.toString().toUpperCase()
                    + "/custom-fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header(
                    "If-Match",
                    "\"custom-values:TASK:"
                        + task
                        + ":schema:"
                        + project
                        + ":9223372036854775807:values:"
                        + task
                        + ":9007199254740993\"")
                .contentType("application/json")
                .content(
                    "{\"values\":[{\"fieldId\":\""
                        + task
                        + "\",\"value\":1.0000000000000000000001},{\"fieldId\":\""
                        + project
                        + "\",\"value\":null}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("values[0].value"));
    verify(saveValues).save("owner", CustomizationScope.TASK, project, task, expected, inputs);
    verifyNoInteractions(readValues, read, save, create, update);
  }

  @Test
  void s11_firstProjectValuesWriteUsesTheCompositeRevisionAndConfirmsAfterSave() throws Exception {
    var id = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    var revision = new com.apptolast.organization.domain.CustomizationRevision(null, 0);
    var expected =
        new com.apptolast.organization.domain.CustomFieldValuesRevision(
            CustomizationScope.PROJECT, id, revision, revision);
    when(saveValues.save(
            "owner", CustomizationScope.PROJECT, id, id, expected, java.util.List.of()))
        .thenReturn(
            new com.apptolast.organization.domain.CustomFieldValues(
                id,
                CustomizationScope.PROJECT,
                revision,
                new com.apptolast.organization.domain.CustomizationRevision(id, 0),
                java.util.List.of(),
                java.time.Instant.parse("2026-09-07T12:00:00Z")));
    mvc.perform(
            put("/api/v1/projects/" + id + "/custom-fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header(
                    "If-Match",
                    "\"custom-values:PROJECT:" + id + ":schema:unconfigured:values:unconfigured\"")
                .contentType("application/json")
                .content("{\"values\":[]}"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "ETag",
                    "\"custom-values:PROJECT:" + id + ":schema:unconfigured:values:" + id + ":0\""))
        .andExpect(
            content()
                .json(
                    "{\"configured\":true,\"values\":[],\"updatedAt\":\"2026-09-07T12:00:00Z\"}",
                    true));
    verify(saveValues)
        .save("owner", CustomizationScope.PROJECT, id, id, expected, java.util.List.of());
    verifyNoInteractions(readValues, read, save, create, update);
  }

  @MockitoBean com.apptolast.organization.application.ReadCustomFieldValuesUseCase readValues;

  @Test
  void s42_emptyActiveProjectionCanStillHaveStoredValues() throws Exception {
    var id = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    when(readValues.get("owner", CustomizationScope.PROJECT, id, id))
        .thenReturn(
            new com.apptolast.organization.domain.CustomFieldValues(
                id,
                CustomizationScope.PROJECT,
                new com.apptolast.organization.domain.CustomizationRevision(id, 2),
                new com.apptolast.organization.domain.CustomizationRevision(id, 0),
                java.util.List.of(),
                java.time.Instant.parse("2026-09-07T12:00:00Z")));
    mvc.perform(get("/api/v1/projects/" + id + "/custom-fields").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    "{\"configured\":true,\"values\":[],\"updatedAt\":\"2026-09-07T12:00:00Z\"}",
                    true));
    verify(readValues).get("owner", CustomizationScope.PROJECT, id, id);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "PROJECT,404,RESOURCE_NOT_FOUND",
    "TASK,404,RESOURCE_NOT_FOUND",
    "PROJECT,503,STORAGE_UNAVAILABLE",
    "TASK,503,STORAGE_UNAVAILABLE"
  })
  void s17_valuesDelegatePropertyAndStorageFailuresWithoutPrivateRepresentation(
      CustomizationScope scope, int expected, String code) throws Exception {
    var project = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    var task = java.util.UUID.fromString("abcdefab-2222-2222-2222-222222222222");
    var entity = scope == CustomizationScope.PROJECT ? project : task;
    RuntimeException failure =
        expected == 404
            ? new com.apptolast.organization.application.ResourceNotFoundException()
            : new com.apptolast.organization.application.StorageUnavailableException(
                new IllegalStateException("private storage detail"));
    when(readValues.get("owner", scope, project, entity)).thenThrow(failure);
    var path =
        "/api/v1/projects/"
            + project
            + (scope == CustomizationScope.TASK ? "/tasks/" + task : "")
            + "/custom-fields";
    mvc.perform(get(path).with(user("owner")))
        .andExpect(status().is(expected))
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.values").doesNotExist())
        .andExpect(jsonPath("$.configured").doesNotExist())
        .andExpect(header().doesNotExist("ETag"));
    verify(readValues).get("owner", scope, project, entity);
    verifyNoInteractions(read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "/api/v1/projects/bad/custom-fields,false,401",
    "/api/v1/projects/bad/tasks/bad/custom-fields,false,401",
    "/api/v1/projects/bad/custom-fields,true,406",
    "/api/v1/projects/bad/tasks/bad/custom-fields,true,406"
  })
  void s23_valuesAuthenticationAndAcceptPrecedeInvalidQuery(
      String path, boolean authenticated, int expected) throws Exception {
    var request = get(path).accept("application/json;q=0, */*;q=1").queryParam("extra", "1");
    if (authenticated) request.with(user("owner"));
    mvc.perform(request).andExpect(status().is(expected)).andExpect(header().doesNotExist("ETag"));
    verifyNoInteractions(readValues, read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "/api/v1/projects/1-1-1-1-1/custom-fields,false,projectId,INVALID_FORMAT",
    "/api/v1/projects/1-1-1-1-1/tasks/bad/custom-fields,false,projectId,INVALID_FORMAT",
    "/api/v1/projects/abcdefab-1111-1111-1111-111111111111/tasks/1-1-1-1-1/custom-fields,false,taskId,INVALID_FORMAT",
    "/api/v1/projects/bad/custom-fields,true,query,INVALID_VALUE",
    "/api/v1/projects/bad/tasks/bad/custom-fields,true,query,INVALID_VALUE"
  })
  void s23_valuesQueryAndPathValidationPrecedeDelegation(
      String path, boolean query, String field, String code) throws Exception {
    var request = get(path).with(user("owner"));
    if (query) request.queryParam("extra", "1");
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    verifyNoInteractions(readValues, read, save, create, update);
  }

  @Test
  void s10_activeNullValuesDoNotImplyAStoredValuesRow() throws Exception {
    var id = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    when(readValues.get("owner", CustomizationScope.PROJECT, id, id))
        .thenReturn(
            new com.apptolast.organization.domain.CustomFieldValues(
                id,
                CustomizationScope.PROJECT,
                new com.apptolast.organization.domain.CustomizationRevision(id, 0),
                new com.apptolast.organization.domain.CustomizationRevision(null, 0),
                java.util.List.of(
                    new com.apptolast.organization.domain.CustomFieldValue(
                        id, "Vacío", com.apptolast.organization.domain.CustomFieldType.TEXT, null)),
                null));
    mvc.perform(get("/api/v1/projects/" + id + "/custom-fields").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "ETag",
                    "\"custom-values:PROJECT:" + id + ":schema:" + id + ":0:values:unconfigured\""))
        .andExpect(
            content()
                .json(
                    "{\"configured\":false,\"values\":[{\"fieldId\":\""
                        + id
                        + "\",\"label\":\"Vacío\",\"type\":\"TEXT\",\"value\":null}],\"updatedAt\":null}",
                    true));
    verify(readValues).get("owner", CustomizationScope.PROJECT, id, id);
  }

  @Test
  void s10_taskValuesKeepTypedValuesAndBothExactLongRevisions() throws Exception {
    var project = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    var task = java.util.UUID.fromString("abcdefab-2222-2222-2222-222222222222");
    var schema = java.util.UUID.fromString("abcdefab-3333-3333-3333-333333333333");
    var values = java.util.UUID.fromString("abcdefab-4444-4444-4444-444444444444");
    when(readValues.get("owner", CustomizationScope.TASK, project, task))
        .thenReturn(
            new com.apptolast.organization.domain.CustomFieldValues(
                task,
                CustomizationScope.TASK,
                new com.apptolast.organization.domain.CustomizationRevision(schema, Long.MAX_VALUE),
                new com.apptolast.organization.domain.CustomizationRevision(
                    values, 9007199254740993L),
                java.util.List.of(
                    new com.apptolast.organization.domain.CustomFieldValue(
                        project,
                        "Texto",
                        com.apptolast.organization.domain.CustomFieldType.TEXT,
                        "  privado  "),
                    new com.apptolast.organization.domain.CustomFieldValue(
                        task,
                        "Número",
                        com.apptolast.organization.domain.CustomFieldType.NUMBER,
                        0),
                    new com.apptolast.organization.domain.CustomFieldValue(
                        schema,
                        "Fecha",
                        com.apptolast.organization.domain.CustomFieldType.DATE,
                        "0001-01-01"),
                    new com.apptolast.organization.domain.CustomFieldValue(
                        values,
                        "Booleano",
                        com.apptolast.organization.domain.CustomFieldType.BOOLEAN,
                        false)),
                java.time.Instant.parse("2026-09-07T12:00:00.123456Z")));
    mvc.perform(
            get("/api/v1/projects/"
                    + project.toString().toUpperCase()
                    + "/tasks/"
                    + task.toString().toUpperCase()
                    + "/custom-fields")
                .with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "ETag",
                    "\"custom-values:TASK:"
                        + task
                        + ":schema:"
                        + schema
                        + ":9223372036854775807:values:"
                        + values
                        + ":9007199254740993\""))
        .andExpect(
            content()
                .json(
                    """
            {"configured":true,"values":[
              {"fieldId":"abcdefab-1111-1111-1111-111111111111","label":"Texto","type":"TEXT","value":"  privado  "},
              {"fieldId":"abcdefab-2222-2222-2222-222222222222","label":"Número","type":"NUMBER","value":0},
              {"fieldId":"abcdefab-3333-3333-3333-333333333333","label":"Fecha","type":"DATE","value":"0001-01-01"},
              {"fieldId":"abcdefab-4444-4444-4444-444444444444","label":"Booleano","type":"BOOLEAN","value":false}
            ],"updatedAt":"2026-09-07T12:00:00.123456Z"}
            """,
                    true));
    verify(readValues).get("owner", CustomizationScope.TASK, project, task);
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s10_projectValuesWithoutConfigurationHaveOneCompositeRevisionAndClosedBody()
      throws Exception {
    var project = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    var absent = new com.apptolast.organization.domain.CustomizationRevision(null, 0);
    when(readValues.get("owner", CustomizationScope.PROJECT, project, project))
        .thenReturn(
            new com.apptolast.organization.domain.CustomFieldValues(
                project, CustomizationScope.PROJECT, absent, absent, java.util.List.of(), null));
    mvc.perform(get("/api/v1/projects/" + project + "/custom-fields").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "ETag",
                    "\"custom-values:PROJECT:"
                        + project
                        + ":schema:unconfigured:values:unconfigured\""))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(content().json("{\"configured\":false,\"values\":[],\"updatedAt\":null}", true));
    verify(readValues).get("owner", CustomizationScope.PROJECT, project, project);
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s23_getDoesNotRequireCsrfOrJsonRequestContentAndDoesNotEnableCors() throws Exception {
    when(read.get("owner", CustomizationScope.PROJECT)).thenReturn(Optional.empty());
    mvc.perform(
            get("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .header("Origin", "https://foreign.example")
                .contentType("text/plain")
                .accept("application/json"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    verify(read).get("owner", CustomizationScope.PROJECT);
    verifyNoInteractions(save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"false,401", "true,406"})
  void s23_malformedAcceptIsLocalButAuthenticationStillComesFirst(
      boolean authenticated, int expected) throws Exception {
    var request = get("/api/v1/me/customization/PROJECT").header("Accept", "not a media type");
    if (authenticated) request.with(user("owner"));
    mvc.perform(request).andExpect(status().is(expected));
    verifyNoInteractions(read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "GET,/api/v1/me/customization/PROJECT",
    "POST,/api/v1/me/customization/PROJECT/fields",
    "PUT,/api/v1/me/customization/TASK/fields/abcdefab-1111-1111-1111-111111111111"
  })
  void s23_unacceptableRepresentationPrecedesQueryOnOtherRoutes(String method, String path)
      throws Exception {
    mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), path)
                .with(user("owner"))
                .with(csrf().asHeader())
                .accept("application/xml")
                .queryParam("forbidden", "true")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isNotAcceptable())
        .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"))
        .andExpect(header().doesNotExist("ETag"));
    verifyNoInteractions(read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "'application/json;q=0, */*;q=1',406",
    "'application/*;q=0, application/json;q=0.5',200",
    "'*/*;q=0.5',200",
    "'application/*;q=0, */*;q=1',406"
  })
  void s23_acceptQualityUsesSpecificityBeforeWildcardPreference(String accept, int expected)
      throws Exception {
    when(read.get("owner", CustomizationScope.PROJECT)).thenReturn(Optional.empty());
    mvc.perform(get("/api/v1/me/customization/PROJECT").with(user("owner")).accept(accept))
        .andExpect(status().is(expected));
    if (expected == 200) verify(read).get("owner", CustomizationScope.PROJECT);
    else verifyNoInteractions(read);
    verifyNoInteractions(save, create, update);
  }

  @Test
  void s23_unacceptableViewResponseNeverExecutesTheWrite() throws Exception {
    when(save.save(anyString(), any(), any(), any()))
        .thenReturn(
            new com.apptolast.organization.domain.Customization(
                java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111"),
                "owner",
                CustomizationScope.PROJECT,
                java.util.List.of(),
                java.util.List.of(),
                0,
                java.time.Instant.parse("2026-09-07T12:00:00Z")));
    var response =
        mvc.perform(
                put("/api/v1/me/customization/PROJECT")
                    .with(user("owner"))
                    .with(csrf().asHeader())
                    .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                    .contentType("application/json")
                    .accept("application/xml")
                    .content("{\"visibleFields\":[]}"))
            .andReturn()
            .getResponse();
    verifyNoInteractions(read, save, create, update);
    org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(406);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "PUT,/api/v1/me/customization/PROJECT",
    "POST,/api/v1/me/customization/PROJECT/fields",
    "PUT,/api/v1/me/customization/TASK/fields/abcdefab-1111-1111-1111-111111111111"
  })
  void s23_writesRejectNonJsonContentBeforeDelegation(String method, String path) throws Exception {
    mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), path)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("text/plain")
                .content("{}"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    verifyNoInteractions(read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "PUT,/api/v1/me/customization/PROJECT",
    "POST,/api/v1/me/customization/PROJECT/fields",
    "PUT,/api/v1/me/customization/TASK/fields/abcdefab-1111-1111-1111-111111111111"
  })
  void s23_foreignOriginRejectsWritesEvenWithValidCsrf(String method, String path)
      throws Exception {
    mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), path)
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("Origin", "https://foreign.example")
                .queryParam("forbidden", "true")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"));
    verifyNoInteractions(read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "PUT,/api/v1/me/customization/PROJECT",
    "POST,/api/v1/me/customization/PROJECT/fields",
    "PUT,/api/v1/me/customization/TASK/fields/abcdefab-1111-1111-1111-111111111111"
  })
  void s23_authenticatedWritesRequireCsrfBeforeInputValidation(String method, String path)
      throws Exception {
    mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), path)
                .with(user("owner"))
                .queryParam("forbidden", "true")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    verifyNoInteractions(read, save, create, update);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "PUT,/api/v1/me/customization/PROJECT",
    "POST,/api/v1/me/customization/PROJECT/fields",
    "PUT,/api/v1/me/customization/TASK/fields/abcdefab-1111-1111-1111-111111111111"
  })
  void s23_anonymousWritesNeverReachTheCustomizationPorts(String method, String path)
      throws Exception {
    mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), path)
                .with(csrf().asHeader())
                .queryParam("forbidden", "true")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s25_firstInvalidVisibleFieldPrecedesLaterTypeFailure() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"visibleFields\":[\"estimatedMinutes\",7]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("visibleFields"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s25_updateCannotChangeTheDefinitionType() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/TASK/fields/abcdefab-1111-1111-1111-111111111111")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:TASK:unconfigured\"")
                .contentType("application/json")
                .content("{\"label\":\"Dato\",\"active\":true,\"type\":\"NUMBER\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("type"))
        .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_FIELD"));
    verifyNoInteractions(read, save, create, update);
  }

  @MockitoBean com.apptolast.organization.application.CreateCustomFieldUseCase create;
  @MockitoBean com.apptolast.organization.application.UpdateCustomFieldUseCase update;

  @Test
  void s17_updateNotFoundUsesThePrivateProblemWithoutCurrentTag() throws Exception {
    when(update.update(
            eq("owner"), eq(CustomizationScope.TASK), any(), any(), eq("Dato"), eq(true)))
        .thenThrow(new com.apptolast.organization.application.ResourceNotFoundException());
    mvc.perform(
            put("/api/v1/me/customization/TASK/fields/abcdefab-1111-1111-1111-111111111111")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:TASK:unconfigured\"")
                .contentType("application/json")
                .content("{\"label\":\"Dato\",\"active\":true}"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.customFields").doesNotExist())
        .andExpect(jsonPath("$.label").doesNotExist())
        .andExpect(header().doesNotExist("ETag"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    verifyNoInteractions(read, save, create);
  }

  @Test
  void s25_updateDoesNotCoerceStringActive() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/TASK/fields/abcdefab-1111-1111-1111-111111111111")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:TASK:unconfigured\"")
                .contentType("application/json")
                .content("{\"label\":\"Dato\",\"active\":\"false\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("active"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_TYPE"));
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s25_updateRequiresActive() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/TASK/fields/abcdefab-1111-1111-1111-111111111111")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:TASK:unconfigured\"")
                .contentType("application/json")
                .content("{\"label\":\"Dato\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("active"))
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s23_updateRejectsAbbreviatedUuidBeforeRequiredHeader() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/TASK/fields/1-1-1-1-1")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("fieldId"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s7_updateDeactivatesTheExactDefinitionWithCanonicalLabel() throws Exception {
    var fieldId = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    var configId = java.util.UUID.fromString("fedcbafe-2222-2222-2222-222222222222");
    var expected = new com.apptolast.organization.domain.CustomizationRevision(configId, 3);
    var saved =
        new com.apptolast.organization.domain.Customization(
            configId,
            "another-owner",
            CustomizationScope.TASK,
            java.util.List.of(),
            java.util.List.of(
                new com.apptolast.organization.domain.CustomFieldDefinition(
                    fieldId,
                    "Nueva nota",
                    com.apptolast.organization.domain.CustomFieldType.TEXT,
                    false)),
            4,
            java.time.Instant.parse("2026-09-07T12:00:00Z"));
    when(update.update(
            "another-owner", CustomizationScope.TASK, fieldId, expected, "Nueva nota", false))
        .thenReturn(saved);
    mvc.perform(
            put("/api/v1/me/customization/TASK/fields/"
                    + fieldId.toString().toUpperCase(java.util.Locale.ROOT))
                .with(user("another-owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:TASK:" + configId + ":3\"")
                .header("Origin", "https://organization.example")
                .accept("application/json")
                .contentType("application/json;charset=UTF-8")
                .content("{\"label\":\"  Nueva nota  \",\"active\":false}"))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"customization:TASK:" + configId + ":4\""))
        .andExpect(jsonPath("$.customFields[0].id").value(fieldId.toString()))
        .andExpect(jsonPath("$.customFields[0].label").value("Nueva nota"))
        .andExpect(jsonPath("$.customFields[0].type").value("TEXT"))
        .andExpect(jsonPath("$.customFields[0].active").value(false));
    verify(update)
        .update("another-owner", CustomizationScope.TASK, fieldId, expected, "Nueva nota", false);
    verifyNoInteractions(read, save, create);
  }

  @Test
  void s4_labelDomainValidationPrecedesMissingType() throws Exception {
    mvc.perform(
            post("/api/v1/me/customization/PROJECT/fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"label\":\"\\u2003\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("label"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s25_createRejectsUnknownType() throws Exception {
    mvc.perform(
            post("/api/v1/me/customization/PROJECT/fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"label\":\"Dato\",\"type\":\"text\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("type"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s25_createRequiresTypeAfterValidLabel() throws Exception {
    mvc.perform(
            post("/api/v1/me/customization/PROJECT/fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"label\":\"Dato\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("type"))
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s25_createRejectsNumericLabelBeforeType() throws Exception {
    mvc.perform(
            post("/api/v1/me/customization/PROJECT/fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"label\":7}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("label"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_TYPE"));
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s25_createRequiresLabelBeforeType() throws Exception {
    mvc.perform(
            post("/api/v1/me/customization/PROJECT/fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("label"))
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
    verifyNoInteractions(read, save, create, update);
  }

  @Test
  void s3_createTextDefinitionConfirmsTheNewServerIdentityAndCanonicalLabel() throws Exception {
    var fieldId = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    var configId = java.util.UUID.fromString("fedcbafe-2222-2222-2222-222222222222");
    var expected = new com.apptolast.organization.domain.CustomizationRevision(null, 0);
    var saved =
        new com.apptolast.organization.domain.Customization(
            configId,
            "owner",
            CustomizationScope.PROJECT,
            java.util.List.of("createdAt"),
            java.util.List.of(
                new com.apptolast.organization.domain.CustomFieldDefinition(
                    fieldId, "Dato", com.apptolast.organization.domain.CustomFieldType.TEXT, true)),
            0,
            java.time.Instant.parse("2026-09-07T12:00:00Z"));
    when(create.create(
            "owner",
            CustomizationScope.PROJECT,
            expected,
            "Dato",
            com.apptolast.organization.domain.CustomFieldType.TEXT))
        .thenReturn(saved);
    mvc.perform(
            post("/api/v1/me/customization/PROJECT/fields")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .header("Origin", "https://organization.example")
                .accept("application/json")
                .contentType("application/json;charset=UTF-8")
                .content("{\"label\":\"  Dato  \",\"type\":\"TEXT\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"customization:PROJECT:" + configId + ":0\""))
        .andExpect(header().doesNotExist("Location"))
        .andExpect(
            content()
                .json(
                    "{\"configured\":true,\"visibleFields\":[\"createdAt\"],\"customFields\":[{\"id\":\""
                        + fieldId
                        + "\",\"label\":\"Dato\",\"type\":\"TEXT\",\"active\":true}],\"updatedAt\":\"2026-09-07T12:00:00Z\"}",
                    true));
    verify(create)
        .create(
            "owner",
            CustomizationScope.PROJECT,
            expected,
            "Dato",
            com.apptolast.organization.domain.CustomFieldType.TEXT);
    verifyNoInteractions(read, save, update);
  }

  @Autowired MockMvc mvc;
  @MockitoBean ReadCustomizationUseCase read;
  @MockitoBean com.apptolast.organization.application.SaveCustomizationViewUseCase save;

  @Test
  void s25_blankBodyIsMalformed() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content(" \n\t"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s8_staleViewRevisionHasACustomizationConflictWithoutCurrentData() throws Exception {
    when(save.save(
            eq("owner"), eq(CustomizationScope.PROJECT), any(), eq(java.util.List.of("createdAt"))))
        .thenThrow(new com.apptolast.organization.application.CustomizationConflictException());
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"visibleFields\":[\"createdAt\"]}"))
        .andExpect(status().isPreconditionFailed())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("CUSTOMIZATION_CONFLICT"))
        .andExpect(jsonPath("$.configured").doesNotExist())
        .andExpect(header().doesNotExist("ETag"));
    verifyNoInteractions(read);
  }

  @Test
  void s25_duplicateVisibleFieldsAreRejectedBeforeRevisionDelegation() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header(
                    "If-Match", "\"customization:PROJECT:abcdefab-1111-1111-1111-111111111111:3\"")
                .contentType("application/json")
                .content("{\"visibleFields\":[\"createdAt\",\"createdAt\"]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("visibleFields"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s25_absentBodyIsMalformedWithAValidHeader() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s24_missingHeaderPrecedesAnAbsentBody() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json"))
        .andExpect(status().is(428))
        .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s25_nullVisibleFieldIsRequiredAtItsIndex() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"visibleFields\":[null]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("visibleFields[0]"))
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s25_nonTextVisibleFieldHasAnIndexedTypeError() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"visibleFields\":[\"createdAt\",7]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("visibleFields[1]"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_TYPE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s25_visibleFieldsMustBeAnArray() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"visibleFields\":\"createdAt\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("visibleFields"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_TYPE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s25_rootArrayHasABodyTypeError() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("[]"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("body"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_TYPE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s25_nullVisibleFieldsIsRequired() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"visibleFields\":null}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("visibleFields"))
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s25_missingVisibleFieldsIsRequired() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("visibleFields"))
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s25_unknownFieldsAreSelectedLexicallyBeforeMissingRequiredFields() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"z\":1,\"a\":2}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("a"))
        .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_FIELD"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s25_concatenatedJsonIsMalformed() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"visibleFields\":[]} {}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s25_duplicateJsonFieldsAreMalformedBeforeDelegation() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{\"visibleFields\":[],\"visibleFields\":[\"createdAt\"]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s23_putScopePrecedesMissingHeader() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/project")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("scope"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s23_putQueryPrecedesMissingHeader() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .queryParam("unexpected", "true")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("query"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s24_repeatedIdenticalTagsAreRejectedBeforeBody() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header(
                    "If-Match",
                    "\"customization:PROJECT:unconfigured\"",
                    "\"customization:PROJECT:unconfigured\"")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("If-Match"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s24_versionBeyondBigintIsAHeaderError() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header(
                    "If-Match",
                    "\"customization:PROJECT:abcdefab-1111-1111-1111-111111111111:9223372036854775808\"")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("If-Match"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s24_noncanonicalVersionIsRejectedBeforeBody() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header(
                    "If-Match", "\"customization:PROJECT:abcdefab-1111-1111-1111-111111111111:01\"")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("If-Match"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s24_otherScopeTagIsRejectedBeforeMalformedBody() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:TASK:unconfigured\"")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("If-Match"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s24_missingIfMatchPrecedesMalformedBody() throws Exception {
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{"))
        .andExpect(status().is(428))
        .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s2_putTaskViewPassesTheExactConfiguredRevisionAndOrderedFields() throws Exception {
    var id = java.util.UUID.fromString("abcdefab-1111-1111-1111-111111111111");
    var expected =
        new com.apptolast.organization.domain.CustomizationRevision(id, 9007199254740993L);
    var fields = java.util.List.of("updatedAt", "estimatedMinutes");
    var saved =
        new com.apptolast.organization.domain.Customization(
            id,
            "another-owner",
            CustomizationScope.TASK,
            fields,
            java.util.List.of(),
            9007199254740994L,
            java.time.Instant.parse("2026-09-07T13:00:00Z"));
    when(save.save("another-owner", CustomizationScope.TASK, expected, fields)).thenReturn(saved);
    mvc.perform(
            put("/api/v1/me/customization/TASK")
                .with(user("another-owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:TASK:" + id + ":9007199254740993\"")
                .contentType("application/json")
                .content("{\"visibleFields\":[\"updatedAt\",\"estimatedMinutes\"]}"))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"customization:TASK:" + id + ":9007199254740994\""))
        .andExpect(jsonPath("$.visibleFields[0]").value("updatedAt"))
        .andExpect(jsonPath("$.visibleFields[1]").value("estimatedMinutes"));
    verify(save).save("another-owner", CustomizationScope.TASK, expected, fields);
    verifyNoInteractions(read);
  }

  @Test
  void s41_firstPutOfProjectDefaultsCreatesConfiguredRepresentation() throws Exception {
    var expected = new com.apptolast.organization.domain.CustomizationRevision(null, 0);
    var saved =
        new com.apptolast.organization.domain.Customization(
            java.util.UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111"),
            "owner",
            CustomizationScope.PROJECT,
            java.util.List.of("createdAt"),
            java.util.List.of(),
            0,
            java.time.Instant.parse("2026-09-07T12:00:00Z"));
    when(save.save("owner", CustomizationScope.PROJECT, expected, java.util.List.of("createdAt")))
        .thenReturn(saved);
    mvc.perform(
            put("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"customization:PROJECT:unconfigured\"")
                .header("Origin", "https://organization.example")
                .accept("application/json")
                .contentType("application/json;charset=UTF-8")
                .content("{\"visibleFields\":[\"createdAt\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.configured").value(true))
        .andExpect(jsonPath("$.visibleFields[0]").value("createdAt"))
        .andExpect(jsonPath("$.visibleFields.length()").value(1))
        .andExpect(jsonPath("$.customFields").isEmpty())
        .andExpect(jsonPath("$.updatedAt").isString())
        .andExpect(
            header()
                .string(
                    "ETag",
                    org.hamcrest.Matchers.matchesPattern(
                        "\"customization:PROJECT:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:0\"")))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    verify(save)
        .save("owner", CustomizationScope.PROJECT, expected, java.util.List.of("createdAt"));
    verifyNoInteractions(read);
  }

  @Test
  void s20_storageFailureDoesNotPublishDefaultsOrPrivateDetails() throws Exception {
    when(read.get("owner", CustomizationScope.PROJECT))
        .thenThrow(
            new com.apptolast.organization.application.StorageUnavailableException(
                new IllegalStateException("private-storage-detail")));
    mvc.perform(get("/api/v1/me/customization/PROJECT").with(user("owner")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
        .andExpect(jsonPath("$.configured").doesNotExist())
        .andExpect(header().doesNotExist("ETag"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("private-storage-detail"))));
    verify(read).get("owner", CustomizationScope.PROJECT);
  }

  @Test
  void s23_authenticationPrecedesQueryValidation() throws Exception {
    mvc.perform(get("/api/v1/me/customization/PROJECT").queryParam("owner", "foreign"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    verifyNoInteractions(read);
  }

  @Test
  void s23_scopeIsCaseSensitiveAndHasItsOwnValidationProblem() throws Exception {
    mvc.perform(get("/api/v1/me/customization/project").with(user("owner")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("scope"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read);
  }

  @Test
  void s23_queryIsRejectedBeforeReadingPrivateConfiguration() throws Exception {
    mvc.perform(
            get("/api/v1/me/customization/PROJECT")
                .with(user("owner"))
                .queryParam("owner", "foreign"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("query"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read);
  }

  @Test
  void s2_configuredResponseKeepsInactiveDefinitionsAndHidesInternalIdentity() throws Exception {
    var id = java.util.UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");
    var field = java.util.UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");
    when(read.get("owner", CustomizationScope.PROJECT))
        .thenReturn(
            Optional.of(
                new com.apptolast.organization.domain.Customization(
                    id,
                    "owner",
                    CustomizationScope.PROJECT,
                    java.util.List.of("updatedAt", "createdAt"),
                    java.util.List.of(
                        new com.apptolast.organization.domain.CustomFieldDefinition(
                            field,
                            "Nota",
                            com.apptolast.organization.domain.CustomFieldType.TEXT,
                            false)),
                    9223372036854775807L,
                    java.time.Instant.parse("2026-09-07T12:00:00.123456Z"))));
    mvc.perform(get("/api/v1/me/customization/PROJECT").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(
            header().string("ETag", "\"customization:PROJECT:" + id + ":9223372036854775807\""))
        .andExpect(
            content()
                .json(
                    "{\"configured\":true,\"visibleFields\":[\"updatedAt\",\"createdAt\"],\"customFields\":[{\"id\":\""
                        + field
                        + "\",\"label\":\"Nota\",\"type\":\"TEXT\",\"active\":false}],\"updatedAt\":\"2026-09-07T12:00:00.123456Z\"}",
                    true));
    verify(read).get("owner", CustomizationScope.PROJECT);
  }

  @Test
  void s1_taskDefaultsUseTheAuthenticatedOwnerAndTaskScope() throws Exception {
    when(read.get("another-owner", CustomizationScope.TASK)).thenReturn(Optional.empty());
    mvc.perform(get("/api/v1/me/customization/TASK").with(user("another-owner")))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"customization:TASK:unconfigured\""))
        .andExpect(
            content()
                .json(
                    "{\"configured\":false,\"visibleFields\":[\"completionCriterion\",\"estimatedMinutes\"],\"customFields\":[],\"updatedAt\":null}",
                    true));
    verify(read).get("another-owner", CustomizationScope.TASK);
  }

  @Test
  void s1_projectDefaultsHaveAnUnconfiguredTagWithoutCsrf() throws Exception {
    when(read.get("owner", CustomizationScope.PROJECT)).thenReturn(Optional.empty());
    mvc.perform(get("/api/v1/me/customization/PROJECT").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"customization:PROJECT:unconfigured\""))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(
            content()
                .json(
                    "{\"configured\":false,\"visibleFields\":[\"createdAt\"],\"customFields\":[],\"updatedAt\":null}",
                    true));
    verify(read).get("owner", CustomizationScope.PROJECT);
  }
}
