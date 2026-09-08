package com.apptolast.organization.application;

import java.io.InputStream;

public interface ImportDataUseCase {
  /** Consumes the request once; its caller retains ownership of closing the stream. */
  ImportPreview preview(String owner, InputStream body);
}
