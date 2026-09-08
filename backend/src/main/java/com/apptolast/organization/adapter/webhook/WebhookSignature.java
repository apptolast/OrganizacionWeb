package com.apptolast.organization.adapter.webhook;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** X-OrganizationWeb-Signature: HMAC-SHA256 over "t.body" keyed by the whole whsec_ secret. */
public final class WebhookSignature {
  private WebhookSignature() {}

  public static String header(String secret, long unixSeconds, byte[] body) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      mac.update((unixSeconds + ".").getBytes(StandardCharsets.UTF_8));
      var digest = mac.doFinal(body);
      return "t=" + unixSeconds + ",v1=" + HexFormat.of().formatHex(digest);
    } catch (GeneralSecurityException error) {
      throw new IllegalStateException(error);
    }
  }
}
