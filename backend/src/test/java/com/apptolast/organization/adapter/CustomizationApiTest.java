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
