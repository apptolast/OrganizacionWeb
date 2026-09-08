package com.apptolast.organization.adapter.http;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class IntegrationOpenApiController {
  @GetMapping("/api/v1/integration-openapi.json")
  public ResponseEntity<Resource> document() {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header("Cache-Control", "no-store")
        .body(new ClassPathResource("integration-openapi.json"));
  }
}
