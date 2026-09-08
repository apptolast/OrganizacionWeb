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
  void s21_eachIndependentScopeAdmitsItsDeclaredOperation(String method, String path, String scope)
      throws Exception {
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
    var access =
        new ApiCredentialAccess(
            UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a"), "owner", List.of(scope));
    when(authenticate.authenticate("test-token")).thenReturn(access);
    var request = new MockHttpServletRequest(method, path);
    request.setServletPath(path);
    request.addHeader("Authorization", "Bearer test-token");
    var response = new MockHttpServletResponse();
    var chain = mock(FilterChain.class);
    try {
      filter.doFilter(request, response, chain);
      var order = inOrder(authenticate, quota, chain);
      order.verify(authenticate).authenticate("test-token");
      order.verify(quota).consume(access);
      order.verify(chain).doFilter(request, response);
      assertEquals("owner", SecurityContextHolder.getContext().getAuthentication().getName());
      assertEquals(200, response.getStatus());
      assertNull(response.getHeader("Set-Cookie"));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
