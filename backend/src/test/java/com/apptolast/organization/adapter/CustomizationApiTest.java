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
                .contentType("application/json")
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
