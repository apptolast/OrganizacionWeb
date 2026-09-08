package com.apptolast.organization.domain;

import java.util.HashSet;
import java.util.List;

public record ApiCredentialIntent(String name, List<String> scopes, int expiresInDays) {
  public static final List<String> SCOPES =
      List.of(
          "projects:read",
          "projects:write",
          "tasks:read",
          "tasks:write",
          "agenda:read",
          "history:read");

  public ApiCredentialIntent {
    if (name == null) throw new ApiCredentialInvalidException("name");
    name = name.replaceAll("(?U)^\\s+|\\s+$", "");
    if (name.isEmpty()
        || name.codePointCount(0, name.length()) > 80
        || name.codePoints().anyMatch(Character::isISOControl))
      throw new ApiCredentialInvalidException("name");
    if (scopes == null
        || scopes.isEmpty()
        || !SCOPES.containsAll(scopes)
        || new HashSet<>(scopes).size() != scopes.size())
      throw new ApiCredentialInvalidException("scopes");
    var selected = new HashSet<>(scopes);
    scopes = SCOPES.stream().filter(selected::contains).toList();
    if (expiresInDays != 7 && expiresInDays != 30 && expiresInDays != 90)
      throw new ApiCredentialInvalidException("expiresInDays");
  }
}
