package com.apptolast.organization.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.adapter.http.ApiCredentialBearerFilter;
import com.apptolast.organization.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class ApiCredentialBearerAdmissionTest {
  private static final UUID CREDENTIAL_ID = UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a");
  private static final String TOKEN = "test-token";

  private record Admission(
      ApiCredentialAccess access,
      AuthenticateApiCredentialUseCase authenticate,
      ConsumeApiQuotaUseCase quota,
      FilterChain chain,
      MockHttpServletRequest request,
      MockHttpServletResponse response) {}

  private static Admission admit(String method, String path, String... scopes) throws Exception {
    SecurityContextHolder.clearContext();
    var authenticate = mock(AuthenticateApiCredentialUseCase.class);
    var quota = mock(ConsumeApiQuotaUseCase.class);
    var beans = new StaticListableBeanFactory();
    beans.addBean("authenticate", authenticate);
    beans.addBean("quota", quota);
    var filter =
        new ApiCredentialBearerFilter(
            beans.getBeanProvider(AuthenticateApiCredentialUseCase.class),
            beans.getBeanProvider(ConsumeApiQuotaUseCase.class),
            new ObjectMapper(),
            "https://organization.example");
    var access = new ApiCredentialAccess(CREDENTIAL_ID, "owner", List.of(scopes));
    when(authenticate.authenticate(TOKEN)).thenReturn(access);
    var request = new MockHttpServletRequest(method, path);
    request.setServletPath(path);
    request.addHeader("Authorization", "Bearer " + TOKEN);
    var response = new MockHttpServletResponse();
    var chain = mock(FilterChain.class);
    filter.doFilter(request, response, chain);
    return new Admission(access, authenticate, quota, chain, request, response);
  }

  @ParameterizedTest
  @CsvSource({
    "GET,/api/v1/projects,projects:read",
    "POST,/api/v1/projects,projects:write",
    "GET,/api/v1/projects/p,projects:read",
    "PUT,/api/v1/projects/p,projects:write",
    "GET,/api/v1/projects/p/tasks,tasks:read",
    "POST,/api/v1/projects/p/tasks,tasks:write",
    "GET,/api/v1/projects/p/tasks/t,tasks:read",
    "GET,/api/v1/projects/p/tasks/t/status,tasks:read",
    "GET,/api/v1/projects/p/tasks/t/parent,tasks:read",
    "GET,/api/v1/projects/p/tasks/t/subtasks,tasks:read",
    "GET,/api/v1/today,agenda:read",
    "GET,/api/v1/projects/p/tasks/t/blocks,agenda:read",
    "GET,/api/v1/projects/p/tasks/t/blocks/b,agenda:read",
    "GET,/api/v1/projects/p/tasks/t/blocks/b/state,agenda:read",
    "GET,/api/v1/projects/p/tasks/t/blocks/by-request/r,agenda:read",
    "GET,/api/v1/history,history:read",
    "GET,/api/v1/weekly-review,history:read",
    "GET,/api/v1/projects/p/tasks/t/history,history:read"
  })
  void s22_eachIndependentScopeAdmitsItsDeclaredOperation(String method, String path, String scope)
      throws Exception {
    try {
      var admitted = admit(method, path, scope);
      var order = inOrder(admitted.authenticate(), admitted.quota(), admitted.chain());
      order.verify(admitted.authenticate()).authenticate(TOKEN);
      order.verify(admitted.quota()).consume(admitted.access());
      order.verify(admitted.chain()).doFilter(admitted.request(), admitted.response());
      assertEquals("owner", SecurityContextHolder.getContext().getAuthentication().getName());
      assertEquals(200, admitted.response().getStatus());
      assertNull(admitted.response().getHeader("Set-Cookie"));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @ParameterizedTest
  @CsvSource({
    "GET,/api/v1/projects/p,projects:write",
    "PUT,/api/v1/projects/p,projects:read",
    "GET,/api/v1/projects,tasks:read",
    "POST,/api/v1/projects,history:read",
    "GET,/api/v1/projects/p/tasks,projects:read",
    "POST,/api/v1/projects/p/tasks,tasks:read",
    "GET,/api/v1/projects/p/tasks/t,agenda:read",
    "GET,/api/v1/today,history:read",
    "GET,/api/v1/projects/p/tasks/t/blocks,tasks:read",
    "GET,/api/v1/history,agenda:read",
    "GET,/api/v1/weekly-review,projects:write"
  })
  void s22_s23_anotherValidScopeNeverGrantsTheOperation(
      String method, String path, String grantedScope) throws Exception {
    try {
      var denied = admit(method, path, grantedScope);
      assertEquals(403, denied.response().getStatus());
      assertEquals("application/problem+json", denied.response().getContentType());
      assertTrue(denied.response().getContentAsString().contains("API_SCOPE_DENIED"));
      assertNull(denied.response().getHeader("Set-Cookie"));
      assertNull(SecurityContextHolder.getContext().getAuthentication());
      verify(denied.authenticate()).authenticate(TOKEN);
      verifyNoInteractions(denied.quota(), denied.chain());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
