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
    return http.csrf(org.springframework.security.config.Customizer.withDefaults())
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
  org.springframework.session.web.http.DefaultCookieSerializer sessionCookie(
      @Value("${app.public-origin}") String origin) {
    return SessionCookiePolicy.create(origin);
  }
}
