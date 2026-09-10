-- A stored secret that cannot be opened -a row sealed with a key that no longer reads, which
-- project-spec declares an expected consequence of rotating APP_CONNECTOR_KEY- used to abort the
-- whole claim before the lease was written, so the poisoned row headed the queue again on every
-- tick and no delivery of any owner ever left again, in silence and for good.
--
-- It is now settled as one ordinary failed attempt, which needs its own class of error. The
-- attempt never reaches the network, so it carries no http_status and no elapsed time.
ALTER TABLE webhook_deliveries DROP CONSTRAINT webhook_deliveries_error_class_check;

ALTER TABLE webhook_deliveries ADD CONSTRAINT webhook_deliveries_error_class_check
  CHECK (error_class IN (
    'HTTP_ERROR', 'REDIRECT', 'TIMEOUT', 'CONNECTION', 'TLS', 'DNS', 'BLOCKED_ADDRESS',
    'SECRET_UNREADABLE'));
