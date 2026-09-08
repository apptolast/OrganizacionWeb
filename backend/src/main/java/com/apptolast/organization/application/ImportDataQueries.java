package com.apptolast.organization.application;

import java.io.InputStream;

public interface ImportDataQueries {
  ImportPreview preview(String owner, InputStream body);
}
