package com.apptolast.organization.adapter.http;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public final class SessionController {
  @GetMapping("/api/session")
  public Map<String, Object> session(Principal principal, CsrfToken csrf) {
    var body = new LinkedHashMap<String, Object>();
    body.put("authenticated", principal != null);
    body.put("username", principal == null ? null : principal.getName());
    body.put("csrfToken", csrf.getToken());
    body.put("csrfHeaderName", csrf.getHeaderName());
    return body;
  }
}
