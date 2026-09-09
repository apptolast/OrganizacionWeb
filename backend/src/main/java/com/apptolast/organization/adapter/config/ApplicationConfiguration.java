package com.apptolast.organization.adapter.config;

import com.apptolast.organization.application.CreateProject;
import com.apptolast.organization.application.ProjectCommit;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
  @Bean
  java.util.function.Predicate<String> enabledApiCredentialOwner(
      @org.springframework.beans.factory.annotation.Value("${app.auth.username}")
          String configuredOwner,
      org.springframework.security.core.userdetails.UserDetailsService users) {
    return owner -> {
      if (!configuredOwner.equals(owner)) return false;
      try {
        var user = users.loadUserByUsername(owner);
        return user.isEnabled() && user.getUsername().equals(owner);
      } catch (org.springframework.security.core.userdetails.UsernameNotFoundException absent) {
        return false;
      }
    };
  }

  @Bean
  com.apptolast.organization.application.AuthenticateApiCredential authenticateApiCredential(
      com.apptolast.organization.application.ApiCredentialAuthentication credentials,
      Clock clock,
      java.util.function.Predicate<String> enabledApiCredentialOwner) {
    return new com.apptolast.organization.application.AuthenticateApiCredential(
        credentials, clock, enabledApiCredentialOwner);
  }

  @Bean
  com.apptolast.organization.application.ConsumeApiQuota consumeApiQuota(
      com.apptolast.organization.application.ApiQuotaAdmission store,
      Clock clock,
      java.util.function.Predicate<String> enabledApiCredentialOwner) {
    return new com.apptolast.organization.application.ConsumeApiQuota(
        store, clock, enabledApiCredentialOwner);
  }

  @Bean
  com.apptolast.organization.application.ReadApiCredentials readApiCredentials(
      com.apptolast.organization.application.ApiCredentialQueries queries) {
    return new com.apptolast.organization.application.ReadApiCredentials(queries);
  }

  @Bean
  com.apptolast.organization.application.RevokeApiCredential revokeApiCredential(
      com.apptolast.organization.application.ApiCredentialRevocations store, Clock clock) {
    return new com.apptolast.organization.application.RevokeApiCredential(store, clock);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresApiCredentialStore apiCredentialStore(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions) {
    return new com.apptolast.organization.adapter.persistence.PostgresApiCredentialStore(
        jdbc, transactions);
  }

  @Bean
  com.apptolast.organization.application.CreateApiCredential createApiCredential(
      com.apptolast.organization.application.ApiCredentialCommit store, Clock clock) {
    return new com.apptolast.organization.application.CreateApiCredential(
        store, clock, new java.security.SecureRandom());
  }

  @Bean
  com.apptolast.organization.application.SaveCustomFieldValues saveCustomFieldValues(
      com.apptolast.organization.application.CustomFieldValuesEditing store, Clock clock) {
    return new com.apptolast.organization.application.SaveCustomFieldValues(store, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadCustomFieldValues readCustomFieldValues(
      com.apptolast.organization.application.CustomFieldValuesQueries queries) {
    return new com.apptolast.organization.application.ReadCustomFieldValues(queries);
  }

  @Bean
  com.apptolast.organization.application.SaveCustomizationView saveCustomizationView(
      com.apptolast.organization.application.CustomizationEditing store, Clock clock) {
    return new com.apptolast.organization.application.SaveCustomizationView(store, clock);
  }

  @Bean
  com.apptolast.organization.application.CreateCustomField createCustomField(
      com.apptolast.organization.application.CustomizationEditing store, Clock clock) {
    return new com.apptolast.organization.application.CreateCustomField(store, clock);
  }

  @Bean
  com.apptolast.organization.application.UpdateCustomField updateCustomField(
      com.apptolast.organization.application.CustomizationEditing store, Clock clock) {
    return new com.apptolast.organization.application.UpdateCustomField(store, clock);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresCustomizationStore customizationStore(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    return new com.apptolast.organization.adapter.persistence.PostgresCustomizationStore(
        jdbc, transactions, json);
  }

  @Bean
  com.apptolast.organization.application.ReadCustomization readCustomization(
      com.apptolast.organization.application.CustomizationQueries queries) {
    return new com.apptolast.organization.application.ReadCustomization(queries);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresWeeklyReviewQueries weeklyReviewQueries(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions) {
    return new com.apptolast.organization.adapter.persistence.PostgresWeeklyReviewQueries(
        new com.apptolast.organization.adapter.persistence.PostgresAvailabilityStore(
            jdbc, new org.springframework.transaction.support.TransactionTemplate(transactions)),
        jdbc,
        transactions);
  }

  @Bean
  com.apptolast.organization.application.ReadWeeklyReview readWeeklyReview(
      com.apptolast.organization.application.WeeklyReviewQueries queries,
      Clock clock,
      com.apptolast.organization.application.ZoneCatalog catalog) {
    return new com.apptolast.organization.application.ReadWeeklyReview(queries, clock, catalog);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresHistoryQueries historyQueries(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    return new com.apptolast.organization.adapter.persistence.PostgresHistoryQueries(
        jdbc, transactions, json);
  }

  @Bean
  com.apptolast.organization.application.ReadHistory readHistory(
      com.apptolast.organization.application.HistoryQueries queries) {
    return new com.apptolast.organization.application.ReadHistory(queries);
  }

  @Bean
  com.apptolast.organization.application.ReadWorkSessionEnd readWorkSessionEnd(
      com.apptolast.organization.application.WorkSessionEndQueries queries, Clock clock) {
    return new com.apptolast.organization.application.ReadWorkSessionEnd(queries, clock);
  }

  @Bean
  com.apptolast.organization.application.ExtendWorkSession extendWorkSession(
      com.apptolast.organization.application.WorkSessionExtending store, Clock clock) {
    return new com.apptolast.organization.application.ExtendWorkSession(store, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadWorkSessionChanges readWorkSessionChanges(
      com.apptolast.organization.application.WorkSessionTransitionQueries queries) {
    return new com.apptolast.organization.application.ReadWorkSessionChanges(queries);
  }

  @Bean
  com.apptolast.organization.application.ReadWorkSessionState readWorkSessionState(
      com.apptolast.organization.application.WorkSessionStateQueries queries, Clock clock) {
    return new com.apptolast.organization.application.ReadWorkSessionState(queries, clock);
  }

  @Bean
  com.apptolast.organization.application.ChangeWorkSession changeWorkSession(
      com.apptolast.organization.application.WorkSessionChanging store, Clock clock) {
    return new com.apptolast.organization.application.ChangeWorkSession(store, clock);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresWorkSessionStore workSessionStore(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    return new com.apptolast.organization.adapter.persistence.PostgresWorkSessionStore(
        jdbc, transactions, json);
  }

  @Bean
  com.apptolast.organization.application.StartWorkSession startWorkSession(
      com.apptolast.organization.application.WorkSessionStarting store,
      Clock clock,
      com.apptolast.organization.application.ZoneCatalog catalog) {
    return new com.apptolast.organization.application.StartWorkSession(store, clock, catalog);
  }

  @Bean
  com.apptolast.organization.application.ReadWorkSessions readWorkSessions(
      com.apptolast.organization.application.WorkSessionQueries queries) {
    return new com.apptolast.organization.application.ReadWorkSessions(queries);
  }

  @Bean
  com.apptolast.organization.application.ReadBlockChangesUseCase readBlockChanges(
      com.apptolast.organization.application.BlockChangeQueries queries) {
    return new com.apptolast.organization.application.ReadBlockChanges(queries);
  }

  @Bean
  com.apptolast.organization.application.MoveBlock moveBlock(
      com.apptolast.organization.application.BlockMoving store,
      com.apptolast.organization.application.ZoneCatalog catalog,
      Clock clock) {
    return new com.apptolast.organization.application.MoveBlock(store, catalog, clock);
  }

  @Bean
  com.apptolast.organization.application.CancelBlock cancelBlock(
      com.apptolast.organization.application.BlockEditing store, Clock clock) {
    return new com.apptolast.organization.application.CancelBlock(store, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadBlocks readBlocks(
      com.apptolast.organization.application.BlockQueries queries) {
    return new com.apptolast.organization.application.ReadBlocks(queries);
  }

  @Bean
  com.apptolast.organization.application.PlanBlock planBlock(
      com.apptolast.organization.application.BlockPlanning planning,
      com.apptolast.organization.application.BlockCommit commit,
      com.apptolast.organization.application.ZoneCatalog catalog,
      Clock clock) {
    return new com.apptolast.organization.application.PlanBlock(planning, commit, catalog, clock);
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  CreateProject createProject(ProjectCommit commit, Clock clock) {
    return new CreateProject(commit, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadProjects readProjects(
      com.apptolast.organization.application.ProjectQueries queries) {
    return new com.apptolast.organization.application.ReadProjects(queries);
  }

  @Bean
  com.apptolast.organization.application.EditProject editProject(
      com.apptolast.organization.application.ProjectEditing store, Clock clock) {
    return new com.apptolast.organization.application.EditProject(store, clock);
  }

  @Bean
  com.apptolast.organization.application.ChangeProjectStatus changeProjectStatus(
      com.apptolast.organization.application.ProjectStatusEditing store,
      Clock clock,
      @org.springframework.beans.factory.annotation.Value("${app.max-active-projects:3}")
          String configured) {
    int limit = Integer.parseInt(configured);
    if (limit < 1 || limit > 10)
      throw new IllegalArgumentException("APP_MAX_ACTIVE_PROJECTS must be an integer from 1 to 10");
    return new com.apptolast.organization.application.ChangeProjectStatus(store, clock, limit);
  }

  @Bean
  com.apptolast.organization.application.CreateTask createTask(
      com.apptolast.organization.application.TaskCommit commit, Clock clock) {
    return new com.apptolast.organization.application.CreateTask(commit, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadTasks readTasks(
      com.apptolast.organization.application.TaskQueries queries) {
    return new com.apptolast.organization.application.ReadTasks(queries);
  }

  @Bean
  com.apptolast.organization.application.CreateSubtask createSubtask(
      com.apptolast.organization.application.SubtaskCommit commit, Clock clock) {
    return new com.apptolast.organization.application.CreateSubtask(commit, clock);
  }

  /** One adapter answers both calendar ports so a feed read stays inside one snapshot. */
  @Bean
  com.apptolast.organization.adapter.persistence.PostgresCalendarStore calendarStore(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager manager) {
    return new com.apptolast.organization.adapter.persistence.PostgresCalendarStore(jdbc, manager);
  }

  @Bean
  com.apptolast.organization.application.ManageCalendarFeedUseCase manageCalendarFeed(
      com.apptolast.organization.application.CalendarFeedTokens tokens,
      @org.springframework.beans.factory.annotation.Value("${app.public-origin}")
          String publicOrigin,
      Clock clock) {
    return new com.apptolast.organization.application.ManageCalendarFeed(
        tokens, publicOrigin, clock, new java.security.SecureRandom());
  }

  /** Wrapped so the token, the zone and the blocks of one feed come out of a single snapshot. */
  @Bean
  com.apptolast.organization.application.RenderCalendarUseCase renderCalendar(
      com.apptolast.organization.application.CalendarFeedTokens tokens,
      com.apptolast.organization.application.CalendarQueries calendars,
      @org.springframework.beans.factory.annotation.Value("${app.public-origin}")
          String publicOrigin,
      Clock clock,
      org.springframework.transaction.PlatformTransactionManager manager) {
    return new com.apptolast.organization.adapter.persistence.SnapshotRenderCalendar(
        new com.apptolast.organization.application.RenderCalendar(
            tokens, calendars, publicOrigin, clock),
        manager);
  }

  @Bean
  com.apptolast.organization.application.ReadSubtasks readSubtasks(
      com.apptolast.organization.application.SubtaskQueries queries) {
    return new com.apptolast.organization.application.ReadSubtasks(queries);
  }

  @Bean
  com.apptolast.organization.application.ReadTaskStatus readTaskStatus(
      com.apptolast.organization.application.TaskStatusQueries queries) {
    return new com.apptolast.organization.application.ReadTaskStatus(queries);
  }

  @Bean
  com.apptolast.organization.application.ChangeTaskStatus changeTaskStatus(
      com.apptolast.organization.application.TaskStatusEditing store, Clock clock) {
    return new com.apptolast.organization.application.ChangeTaskStatus(store, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadTaskHistory readTaskHistory(
      com.apptolast.organization.application.TaskHistoryQueries queries) {
    return new com.apptolast.organization.application.ReadTaskHistory(queries);
  }

  @Bean
  com.apptolast.organization.application.ReadAvailability readAvailability(
      com.apptolast.organization.application.AvailabilityQueries queries,
      com.apptolast.organization.application.ZoneCatalog catalog) {
    return new com.apptolast.organization.application.ReadAvailability(queries, catalog);
  }

  @Bean
  com.apptolast.organization.application.SaveAvailability saveAvailability(
      com.apptolast.organization.application.AvailabilityEditing store,
      com.apptolast.organization.application.ZoneCatalog catalog,
      Clock clock) {
    return new com.apptolast.organization.application.SaveAvailability(store, catalog, clock);
  }

  @org.springframework.context.annotation.Bean
  com.apptolast.organization.application.ReadToday readToday(
      com.apptolast.organization.application.TodayQueries queries,
      Clock clock,
      com.apptolast.organization.application.ZoneCatalog catalog) {
    return new com.apptolast.organization.application.ReadToday(queries, clock, catalog);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresAppearanceStore appearanceStore(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions) {
    return new com.apptolast.organization.adapter.persistence.PostgresAppearanceStore(
        jdbc, new org.springframework.transaction.support.TransactionTemplate(transactions));
  }

  @Bean
  com.apptolast.organization.application.ReadAppearance readAppearance(
      com.apptolast.organization.application.AppearanceQueries queries) {
    return new com.apptolast.organization.application.ReadAppearance(queries);
  }

  @Bean
  com.apptolast.organization.application.SaveAppearance saveAppearance(
      com.apptolast.organization.application.AppearanceEditing store, Clock clock) {
    return new com.apptolast.organization.application.SaveAppearance(store, clock);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresExportDataQueries exportDataQueries(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions) {
    return new com.apptolast.organization.adapter.persistence.PostgresExportDataQueries(
        jdbc, transactions);
  }

  @Bean
  com.apptolast.organization.application.PrepareExportData prepareExportData(
      com.apptolast.organization.application.ExportDataQueries queries, Clock clock) {
    return new com.apptolast.organization.application.PrepareExportData(queries, clock);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresImportDataStore importDataStore(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions,
      @org.springframework.beans.factory.annotation.Value("${app.max-active-projects:3}")
          String configured) {
    return new com.apptolast.organization.adapter.persistence.PostgresImportDataStore(
        jdbc, transactions, Integer.parseInt(configured));
  }

  @Bean
  com.apptolast.organization.application.PreviewImportData previewImportData(
      com.apptolast.organization.application.ImportDataQueries queries) {
    return new com.apptolast.organization.application.PreviewImportData(queries);
  }

  @Bean
  com.apptolast.organization.application.ApplyImportData applyImportData(
      com.apptolast.organization.application.ImportDataCommands commands, Clock clock) {
    return new com.apptolast.organization.application.ApplyImportData(commands, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadImportReceipt readImportReceipt(
      com.apptolast.organization.application.ImportReceiptQueries queries) {
    return new com.apptolast.organization.application.ReadImportReceipt(queries);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresWebhookStore webhookStore(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions) {
    return new com.apptolast.organization.adapter.persistence.PostgresWebhookStore(
        jdbc, transactions);
  }

  /**
   * Amendment B5: an absent connector key degrades into {@code WebhookSecrets.DISABLED} (503
   * CONNECTORS_DISABLED on the writing operations); a malformed one fails fast at startup.
   */
  @Bean
  com.apptolast.organization.application.WebhookSecrets webhookSecrets(
      @org.springframework.beans.factory.annotation.Value("${app.connectors.key:}") String key,
      @org.springframework.beans.factory.annotation.Value("${app.connectors.key-previous:}")
          String previous) {
    var secrets =
        com.apptolast.organization.adapter.webhook.AesGcmWebhookSecrets.from(key, previous);
    return secrets == null
        ? com.apptolast.organization.application.WebhookSecrets.DISABLED
        : secrets;
  }

  /**
   * La guardia de destino de los webhooks usa la política fija, NO el bean {@code
   * connectorAddressPolicy} de la feature 28: {@code APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES} está
   * documentado como válvula del perfil de extremo a extremo del calendario externo, y el envío de
   * webhooks firmados nunca queda a su merced. Decisión tomada al unificar {@code AddressPolicy};
   * el E2E de la feature 25 no la necesita porque simula todas las respuestas.
   */
  @Bean
  com.apptolast.organization.application.WebhookDestinationGuard webhookDestinationGuard() {
    return new com.apptolast.organization.application.WebhookDestinationGuard(
        java.net.InetAddress::getAllByName,
        com.apptolast.organization.application.AddressPolicy.blockingPrivateAddresses());
  }

  @Bean
  com.apptolast.organization.application.CreateWebhook createWebhook(
      com.apptolast.organization.application.WebhookEndpoints endpoints,
      com.apptolast.organization.application.WebhookSecrets secrets,
      com.apptolast.organization.application.WebhookDestinationGuard destinations,
      Clock clock) {
    return new com.apptolast.organization.application.CreateWebhook(
        endpoints, secrets, destinations, clock, new java.security.SecureRandom());
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresWebhookWork webhookWork(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions,
      com.apptolast.organization.application.WebhookSecrets secrets) {
    return new com.apptolast.organization.adapter.persistence.PostgresWebhookWork(
        jdbc, transactions, secrets::decrypt);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresWebhookOutbox webhookOutbox(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    return new com.apptolast.organization.adapter.persistence.PostgresWebhookOutbox(
        jdbc, transactions, json);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresAutomationStore automationStore(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    return new com.apptolast.organization.adapter.persistence.PostgresAutomationStore(
        jdbc, transactions, json);
  }

  @Bean
  com.apptolast.organization.application.WebhookSender webhookSender(Clock clock) {
    return new com.apptolast.organization.adapter.webhook.JdkWebhookSender(
        clock,
        com.apptolast.organization.application.AddressPolicy.blockingPrivateAddresses(),
        java.net.InetAddress::getAllByName);
  }

  @Bean
  com.apptolast.organization.application.WebhookAudit webhookAudit() {
    return new com.apptolast.organization.adapter.logging.Slf4jWebhookAudit();
  }

  @Bean
  com.apptolast.organization.application.DispatchWebhooks dispatchWebhooks(
      com.apptolast.organization.application.WebhookWork work,
      com.apptolast.organization.application.WebhookSender sender,
      com.apptolast.organization.application.WebhookAudit audit,
      com.apptolast.organization.application.WebhookSecrets secrets,
      Clock clock) {
    return new com.apptolast.organization.application.DispatchWebhooks(
        work, sender, audit, secrets, clock);
  }

  @Bean
  WebhookConnectorStartup webhookConnectorStartup(
      com.apptolast.organization.application.WebhookSecrets secrets,
      com.apptolast.organization.application.WebhookAudit audit) {
    return new WebhookConnectorStartup(secrets, audit);
  }

  @Bean
  com.apptolast.organization.application.EnqueueWebhookDeliveries enqueueWebhookDeliveries(
      com.apptolast.organization.application.WebhookOutbox outbox,
      com.apptolast.organization.application.WebhookAudit audit,
      Clock clock) {
    return new com.apptolast.organization.application.EnqueueWebhookDeliveries(
        outbox, audit, clock);
  }

  @Bean
  com.apptolast.organization.application.ManageWebhook manageWebhook(
      com.apptolast.organization.application.WebhookEndpoints endpoints,
      com.apptolast.organization.application.WebhookDeliveries deliveries,
      com.apptolast.organization.application.WebhookSecrets secrets,
      Clock clock) {
    return new com.apptolast.organization.application.ManageWebhook(
        endpoints, deliveries, secrets, clock);
  }

  // --- Feature 28: calendario externo ---

  @Bean
  com.apptolast.organization.application.AddressPolicy connectorAddressPolicy(
      @org.springframework.beans.factory.annotation.Value(
              "${app.connectors.allow-private-addresses:false}")
          boolean allowPrivateAddresses) {
    return allowPrivateAddresses
        ? com.apptolast.organization.application.AddressPolicy.allowingPrivateAddresses()
        : com.apptolast.organization.application.AddressPolicy.blockingPrivateAddresses();
  }

  @Bean
  com.apptolast.organization.application.HostResolver systemHostResolver() {
    return new com.apptolast.organization.adapter.net.SystemHostResolver();
  }

  @Bean
  com.apptolast.organization.application.OutboundGuard outboundGuard(
      com.apptolast.organization.application.HostResolver resolver,
      com.apptolast.organization.application.AddressPolicy policy) {
    return new com.apptolast.organization.application.OutboundHostGuard(resolver, policy);
  }

  @Bean
  com.apptolast.organization.application.CalendarFeed calendarFeed(
      com.apptolast.organization.application.HostResolver resolver,
      com.apptolast.organization.application.AddressPolicy policy) {
    return new com.apptolast.organization.adapter.feed.HttpCalendarFeed(
        com.apptolast.organization.adapter.feed.HttpCalendarFeed.TIMEOUT, resolver, policy);
  }

  @Bean
  com.apptolast.organization.application.ExternalCalendarAudit externalCalendarAudit() {
    return new com.apptolast.organization.adapter.logging.Slf4jExternalCalendarAudit();
  }

  @Bean
  com.apptolast.organization.application.ExternalCalendarStore externalCalendarStore(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions) {
    return new com.apptolast.organization.adapter.persistence.PostgresExternalCalendarStore(
        jdbc, new org.springframework.transaction.support.TransactionTemplate(transactions));
  }

  @Bean
  com.apptolast.organization.application.ExternalCalendarUseCases.Read readExternalCalendar(
      com.apptolast.organization.application.ExternalCalendarStore store) {
    return new com.apptolast.organization.application.ReadExternalCalendar(store);
  }

  @Bean
  com.apptolast.organization.application.ExternalCalendarUseCases.Save saveExternalCalendar(
      com.apptolast.organization.application.ExternalCalendarStore store,
      com.apptolast.organization.application.SecretCipher cipher,
      com.apptolast.organization.application.OutboundGuard guard,
      Clock clock) {
    return new com.apptolast.organization.application.SaveExternalCalendar(
        store, cipher, guard, clock);
  }

  @Bean
  com.apptolast.organization.application.ExternalCalendarUseCases.Delete deleteExternalCalendar(
      com.apptolast.organization.application.ExternalCalendarStore store) {
    return new com.apptolast.organization.application.DeleteExternalCalendar(store);
  }

  @Bean
  com.apptolast.organization.application.ExternalCalendarUseCases.Sync syncExternalCalendar(
      com.apptolast.organization.application.ExternalCalendarStore store,
      com.apptolast.organization.application.SecretCipher cipher,
      com.apptolast.organization.application.OutboundGuard guard,
      com.apptolast.organization.application.CalendarFeed feed,
      com.apptolast.organization.application.AvailabilityQueries availability,
      com.apptolast.organization.application.ZoneCatalog zones,
      Clock clock,
      com.apptolast.organization.application.ExternalCalendarAudit audit) {
    return new com.apptolast.organization.application.SyncExternalCalendar(
        store, cipher, guard, feed, availability, zones, clock, audit);
  }

  @Bean
  com.apptolast.organization.application.ExternalCalendarUseCases.ReadEvents
      readExternalCalendarEvents(
          com.apptolast.organization.application.ExternalCalendarStore store) {
    return new com.apptolast.organization.application.ReadExternalCalendarEvents(store);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresAutomationRuns automationRuns(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions) {
    return new com.apptolast.organization.adapter.persistence.PostgresAutomationRuns(
        jdbc, transactions);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresAutomationEvents automationEvents(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    return new com.apptolast.organization.adapter.persistence.PostgresAutomationEvents(
        jdbc, transactions, json);
  }

  /**
   * Whether a project belongs to the owner, whatever its status: a rule may target a completed
   * project and fail at run time with PROJECT_COMPLETED, which is not a save-time error.
   */
  @Bean
  com.apptolast.organization.application.AutomationTargets automationTargets(
      org.springframework.jdbc.core.JdbcTemplate jdbc) {
    return (owner, projectId) ->
        !jdbc.queryForList(
                "SELECT 1 FROM projects WHERE id = ? AND owner_id = ?",
                Integer.class,
                projectId,
                owner)
            .isEmpty();
  }

  /**
   * The endpoints of feature 25 as the rules see them: only an active endpoint of this very owner
   * counts. A deleted one and a disabled one are the same answer here, and the contract gives both
   * the same code.
   */
  @Bean
  com.apptolast.organization.application.WebhookEndpointLookup webhookEndpointLookup(
      org.springframework.jdbc.core.JdbcTemplate jdbc) {
    return (owner, endpointId) ->
        !jdbc.queryForList(
                "SELECT 1 FROM webhook_endpoints WHERE id = ? AND owner_id = ?"
                    + " AND status = 'active'",
                Integer.class,
                endpointId,
                owner)
            .isEmpty();
  }

  @Bean
  com.apptolast.organization.application.AutomationMatcher automationMatcher(
      com.apptolast.organization.application.AutomationEventProjects projects,
      com.apptolast.organization.application.AutomationLoopGuard guard) {
    return new com.apptolast.organization.application.AutomationMatcher(projects, guard);
  }

  @Bean
  com.apptolast.organization.application.CreateAutomationUseCase createAutomation(
      com.apptolast.organization.application.AutomationRuleStore rules,
      com.apptolast.organization.application.AutomationTargets targets,
      com.apptolast.organization.application.WebhookEndpointLookup endpoints,
      Clock clock) {
    return new com.apptolast.organization.application.CreateAutomation(
        rules, targets, endpoints, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadAutomationsUseCase readAutomations(
      com.apptolast.organization.application.AutomationRuleStore rules) {
    return new com.apptolast.organization.application.ReadAutomations(rules);
  }

  @Bean
  com.apptolast.organization.application.ReplaceAutomationUseCase replaceAutomation(
      com.apptolast.organization.application.AutomationRuleStore rules,
      com.apptolast.organization.application.AutomationTargets targets,
      com.apptolast.organization.application.WebhookEndpointLookup endpoints,
      Clock clock) {
    return new com.apptolast.organization.application.ReplaceAutomation(
        rules, targets, endpoints, clock);
  }

  @Bean
  com.apptolast.organization.application.DeleteAutomationUseCase deleteAutomation(
      com.apptolast.organization.application.AutomationRuleStore rules) {
    return new com.apptolast.organization.application.DeleteAutomation(rules);
  }

  @Bean
  com.apptolast.organization.application.SimulateAutomationUseCase simulateAutomation(
      com.apptolast.organization.application.AutomationEventTail events,
      com.apptolast.organization.application.AutomationMatcher matcher,
      com.apptolast.organization.application.AutomationFacts facts,
      com.apptolast.organization.application.AutomationTargets targets,
      com.apptolast.organization.application.WebhookEndpointLookup endpoints) {
    return new com.apptolast.organization.application.SimulateAutomation(
        events, matcher, facts, targets, endpoints);
  }

  @Bean
  com.apptolast.organization.adapter.persistence.PostgresAutomationWork automationWork(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager transactions,
      com.fasterxml.jackson.databind.ObjectMapper json,
      com.apptolast.organization.application.CreateTaskUseCase createTask) {
    return new com.apptolast.organization.adapter.persistence.PostgresAutomationWork(
        jdbc, transactions, json, createTask);
  }

  @Bean
  com.apptolast.organization.application.ExecuteAutomationsUseCase executeAutomations(
      com.apptolast.organization.application.AutomationWork work,
      com.apptolast.organization.application.AutomationRuleStore rules,
      com.apptolast.organization.application.AutomationMatcher matcher,
      com.apptolast.organization.application.AutomationFacts facts,
      com.apptolast.organization.application.WebhookEndpointLookup endpoints,
      Clock clock) {
    return new com.apptolast.organization.application.ExecuteAutomations(
        work, rules, matcher, facts, endpoints, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadAutomationRunsUseCase readAutomationRuns(
      com.apptolast.organization.application.AutomationRuleStore rules,
      com.apptolast.organization.application.AutomationRunStore runs) {
    return new com.apptolast.organization.application.ReadAutomationRuns(rules, runs);
  }
}
