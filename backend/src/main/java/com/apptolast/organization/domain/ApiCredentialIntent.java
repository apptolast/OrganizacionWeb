package com.apptolast.organization.domain;

import java.util.List;

public record ApiCredentialIntent(String name, List<String> scopes, int expiresInDays) {}
