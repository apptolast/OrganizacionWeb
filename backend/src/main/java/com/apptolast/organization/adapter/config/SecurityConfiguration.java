package com.apptolast.organization.adapter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
  @Bean
  UserDetailsService users(
      @Value("${app.auth.username}") String username,
      @Value("${app.auth.password}") String password) {
    if (username.isBlank() || password.isBlank())
      throw new IllegalArgumentException("Bootstrap credentials must not be blank");
    return new InMemoryUserDetailsManager(
        User.withUsername(username)
            .password("{bcrypt}" + new BCryptPasswordEncoder().encode(password))
            .roles("USER")
            .build());
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      com.fasterxml.jackson.databind.ObjectMapper json,
      @Value("${app.public-origin}") String publicOrigin)
      throws Exception {
    // Browser writes require JSON and an explicitly trusted Origin; no CORS is enabled.
    return http.csrf(csrf -> csrf.disable())
        .addFilterAfter(
            new com.apptolast.organization.adapter.http.OriginGuard(publicOrigin, json),
            org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .httpBasic(
            basic ->
                basic.authenticationEntryPoint(
                    (request, response, error) -> {
                      response.setStatus(401);
                      response.setHeader(
                          "WWW-Authenticate", "Basic realm=\"OrganizationWeb\", charset=\"UTF-8\"");
                      response.setContentType("application/problem+json");

                      json.writeValue(
                          response.getOutputStream(),
                          com.apptolast.organization.adapter.http.ApiErrors.problem(
                              401, "UNAUTHENTICATED", "Identifícate para continuar."));
                    }))
        .build();
  }
}
