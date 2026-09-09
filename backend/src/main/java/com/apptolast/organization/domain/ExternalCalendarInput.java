package com.apptolast.organization.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/** Etiqueta y URL validadas sintácticamente; la resolución DNS pertenece a la aplicación. */
public record ExternalCalendarInput(String label, String url, String urlHost, String urlTail) {
  public static final int LABEL_LIMIT = 40;
  public static final int URL_LIMIT = 2048;
  public static final int TAIL_LENGTH = 4;

  public static ExternalCalendarInput of(String label, String url) {
    var errors = new ArrayList<FieldError>();
    var cleanLabel = label == null ? "" : strip(label);
    if (cleanLabel.isEmpty()) errors.add(error("label", "REQUIRED", "Indica una etiqueta."));
    else if (cleanLabel.codePointCount(0, cleanLabel.length()) > LABEL_LIMIT)
      errors.add(error("label", "TOO_LONG", "La etiqueta admite hasta 40 caracteres."));
    String host = null;
    if (url == null || url.isEmpty())
      errors.add(error("url", "REQUIRED", "Pega la dirección iCal."));
    else if (url.length() > URL_LIMIT)
      errors.add(error("url", "TOO_LONG", "La dirección admite hasta 2048 caracteres."));
    else {
      var uri = absolute(url);
      if (uri == null) errors.add(error("url", "INVALID_FORMAT", "La dirección no es una URL."));
      else if (!"https".equalsIgnoreCase(uri.getScheme())
          || uri.getRawUserInfo() != null
          || uri.getRawFragment() != null
          || isIpLiteral(uri.getHost()))
        errors.add(
            error(
                "url",
                "INVALID_VALUE",
                "Usa una dirección https con nombre de host, sin credenciales ni fragmento."));
      else host = uri.getHost();
    }
    if (!errors.isEmpty()) throw new ValidationException(errors);
    return new ExternalCalendarInput(
        cleanLabel, url, host, url.substring(url.length() - TAIL_LENGTH));
  }

  private static URI absolute(String url) {
    try {
      var uri = new URI(url);
      return uri.isAbsolute() && uri.getHost() != null && !uri.getHost().isEmpty() ? uri : null;
    } catch (URISyntaxException error) {
      return null;
    }
  }

  private static boolean isIpLiteral(String host) {
    return host.startsWith("[") || host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
  }

  private static String strip(String value) {
    return value.replaceAll("^\\p{IsWhite_Space}+|\\p{IsWhite_Space}+$", "");
  }

  private static FieldError error(String field, String code, String message) {
    return new FieldError(field, code, message);
  }
}
