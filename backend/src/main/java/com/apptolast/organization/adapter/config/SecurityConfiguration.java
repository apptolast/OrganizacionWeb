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
  private static final String CONTENT_SECURITY_POLICY =
      "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:;"
          + " connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'";

  /**
   * La postura de cabeceras no depende del ingress: `deploy/nginx.conf` las añade para el frontend,
   * pero un despliegue que exponga el backend sin ese proxy debe seguir emitiéndolas.
   */
  private static void securityHeaders(
      org.springframework.security.config.annotation.web.configurers.HeadersConfigurer<HttpSecurity>
          headers) {
    headers
        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
        .referrerPolicy(
            referrer ->
                referrer.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                        .ReferrerPolicy.SAME_ORIGIN));
  }

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

  /**
   * The calendar feed is a capability in the path: it takes no session, no CSRF token and no bearer
   * credential, and it must never receive a session cookie in return.
   */
  @Bean
  @org.springframework.core.annotation.Order(0)
  SecurityFilterChain publicCalendarSecurity(HttpSecurity http) throws Exception {
    return http.securityMatcher(com.apptolast.organization.adapter.http.CalendarPaths::isPublicFeed)
        .csrf(csrf -> csrf.disable())
        .logout(logout -> logout.disable())
        .requestCache(cache -> cache.disable())
        .httpBasic(basic -> basic.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
  }

  @Bean
  @org.springframework.core.annotation.Order(1)
  SecurityFilterChain bearerSecurity(
      HttpSecurity http,
      @Value("${app.public-origin}") String publicOrigin,
      com.fasterxml.jackson.databind.ObjectMapper json,
      org.springframework.beans.factory.ObjectProvider<
              com.apptolast.organization.application.AuthenticateApiCredentialUseCase>
          authenticate,
      org.springframework.beans.factory.ObjectProvider<
              com.apptolast.organization.application.ConsumeApiQuotaUseCase>
          quota)
      throws Exception {
    // The calendar routes stay out of the bearer channel: the feed is not exposed by credentials.
    return http.securityMatcher(
            request ->
                request.getHeader("Authorization") != null
                    && !com.apptolast.organization.adapter.http.CalendarPaths.isCalendar(request))
        .headers(SecurityConfiguration::securityHeaders)
        .logout(logout -> logout.disable())
        .csrf(csrf -> csrf.disable())
        .requestCache(cache -> cache.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(
            new com.apptolast.organization.adapter.http.ApiCredentialBearerFilter(
                authenticate, quota, json, publicOrigin),
            org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .build();
  }

  @Bean
  @org.springframework.core.annotation.Order(2)
  SecurityFilterChain security(
      HttpSecurity http,
      com.fasterxml.jackson.databind.ObjectMapper json,
      @Value("${app.public-origin}") String publicOrigin)
      throws Exception {
    org.springframework.security.web.AuthenticationEntryPoint unauthorized =
        (request, response, error) -> {
          response.setStatus(401);
          response.setContentType("application/problem+json");
          json.writeValue(
              response.getOutputStream(),
              com.apptolast.organization.adapter.http.ApiErrors.problem(
                  401, "UNAUTHENTICATED", "Identifícate para continuar."));
        };
    // Browser writes retain their trusted Origin requirement; no CORS is enabled.
    return http.headers(SecurityConfiguration::securityHeaders)
        .csrf(org.springframework.security.config.Customizer.withDefaults())
        .addFilterBefore(
            new com.apptolast.organization.adapter.http.OriginGuard(publicOrigin, json),
            org.springframework.security.web.csrf.CsrfFilter.class)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/session")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .requestCache(cache -> cache.disable())
        .formLogin(
            form ->
                form.loginProcessingUrl("/api/session")
                    .successHandler((request, response, authentication) -> response.setStatus(204))
                    .failureHandler(
                        (request, response, error) ->
                            unauthorized.commence(request, response, error))
                    .permitAll())
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/session/logout")
                    .logoutSuccessHandler(
                        (request, response, authentication) -> response.setStatus(204)))
        .httpBasic(basic -> basic.disable())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(unauthorized)
                    .accessDeniedHandler(
                        new com.apptolast.organization.adapter.http.SessionAccessDeniedHandler(
                            json)))
        .build();
  }

  @Bean
  org.springframework.session.web.http.HttpSessionIdResolver sessionIdResolver(
      org.springframework.session.web.http.DefaultCookieSerializer serializer) {
    return new com.apptolast.organization.adapter.http.ApiCredentialSessionIdResolver(serializer);
  }

  @Bean
  org.springframework.session.web.http.DefaultCookieSerializer sessionCookie(
      @Value("${app.public-origin}") String origin) {
    return SessionCookiePolicy.create(origin);
  }
}
