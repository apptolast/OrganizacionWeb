package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ConnectorError;
import com.apptolast.organization.application.ConnectorRow;
import com.apptolast.organization.application.ReadConnectorCatalogUseCase;
import com.apptolast.organization.domain.ValidationException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontera HTTP del catálogo de conectores. Es la única ruta que ve las seis integraciones a la
 * vez, y por eso es la que más disciplina necesita: cuatro campos por fila, ninguna clave de más y
 * {@code Cache-Control: no-store}, porque describe credenciales de terceros.
 *
 * <p>No traduce {@code StorageUnavailableException}: la deja subir a {@link ApiErrors}, que la
 * convierte en 503 {@code STORAGE_UNAVAILABLE}. Envolverla aquí daría el catálogo optimista que la
 * feature prohíbe.
 */
@RestController
public final class ConnectorCatalogController {
  private static final String CATALOG = "/api/v1/me/connectors";
  private static final String NO_STORE = "no-store, private";

  private final ReadConnectorCatalogUseCase catalog;

  public ConnectorCatalogController(ReadConnectorCatalogUseCase catalog) {
    this.catalog = catalog;
  }

  @GetMapping(CATALOG)
  public ResponseEntity<CatalogResponse> get(
      Principal principal, @RequestParam MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw new ValidationException(List.of());
    return ResponseEntity.ok()
        .header("Cache-Control", NO_STORE)
        .body(CatalogResponse.of(catalog.execute(principal.getName())));
  }

  /** Una sola clave: la pantalla no tiene que adivinar dónde está la lista. */
  public record CatalogResponse(List<RowResponse> connectors) {
    static CatalogResponse of(com.apptolast.organization.application.ConnectorCatalog catalog) {
      return new CatalogResponse(catalog.connectors().stream().map(RowResponse::of).toList());
    }
  }

  /**
   * Cuatro campos y ni uno más. Los nulos se serializan, porque «sin actividad» es información que
   * la interfaz necesita distinguir de «este campo no ha venido».
   */
  public record RowResponse(
      String id, String status, Instant lastActivityAt, ErrorResponse lastError) {
    static RowResponse of(ConnectorRow row) {
      return new RowResponse(
          row.id(), row.status(), row.lastActivityAt(), ErrorResponse.of(row.lastError()));
    }
  }

  /** Un código estable y un instante: sin mensaje libre del proveedor, sin URL y sin secretos. */
  public record ErrorResponse(String code, Instant at) {
    static ErrorResponse of(ConnectorError error) {
      return error == null ? null : new ErrorResponse(error.code(), error.at());
    }
  }
}
