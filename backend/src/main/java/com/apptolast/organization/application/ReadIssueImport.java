package com.apptolast.organization.application;

import com.apptolast.organization.domain.IssueImportReceipt;
import java.util.UUID;

/**
 * Lee un recibo del propietario. Un recibo ajeno y un identificador inexistente producen el mismo
 * error, así que la respuesta no delata qué importaciones existen fuera de la propia cuenta.
 */
public final class ReadIssueImport implements ReadIssueImportUseCase {
  private final IssueImportReceiptStore receipts;
  private final SecretCipher cipher;

  public ReadIssueImport(IssueImportReceiptStore receipts, SecretCipher cipher) {
    this.receipts = receipts;
    this.cipher = cipher;
  }

  @Override
  public IssueImportReceipt execute(String ownerId, UUID importId) {
    if (!cipher.enabled()) throw new ConnectorsDisabledException();
    return receipts.find(ownerId, importId).orElseThrow(IssueImportNotFoundException::new);
  }
}
