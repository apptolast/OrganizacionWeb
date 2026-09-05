@authentication @approved
Feature: Entrar y salir de la web con una sesión privada revocable
  Como propietario quiero acceder desde un formulario y cerrar mi sesión desde la web.
  Contrato dentro de la autorización global; producción posterior al cierre de project_states.

  @s1
  Scenario: Consultar acceso antes de mostrar datos privados
    Given un navegador sin sesión autenticada
    When consulta GET /api/session
    Then recibe HTTP 200 con authenticated false, username null, csrfToken y csrfHeaderName
    And esos cuatro campos son los únicos y csrfHeaderName es X-CSRF-TOKEN
    And la respuesta tiene Cache-Control no-store sin contraseñas ni identificador de sesión
    And la web presenta el formulario de acceso sin montar las vistas privadas

  @s2
  Scenario: Iniciar sesión con las credenciales configuradas
    Given una sesión anónima y su token CSRF vigente
    When envía las credenciales válidas a POST /api/session como formulario estándar
    Then recibe HTTP 204 y se renueva el identificador de sesión
    And la siguiente consulta de sesión confirma authenticated true y el propietario configurado
    And una nueva consulta proporciona el token para posteriores escrituras
    And conserva el acceso a los proyectos existentes de ese propietario

  @s3
  Scenario Outline: Rechazar credenciales sin revelar qué dato es incorrecto
    Given una sesión anónima y su token CSRF vigente
    When intenta acceder con <credenciales>
    Then recibe HTTP 401 UNAUTHENTICATED con el mismo cuerpo público
    And no obtiene acceso a los proyectos
    Examples:
      | credenciales         |
      | usuario desconocido  |
      | contraseña incorrecta |
      | valores vacíos       |

  @s4
  Scenario: Retirar autenticación Basic de los datos de la aplicación
    Given un cliente sin cookie autenticada
    When solicita proyectos enviando únicamente credenciales Basic válidas
    Then recibe HTTP 401 UNAUTHENTICATED sin desafío Basic ni datos privados

  @s5
  Scenario: Recuperar sesión tras reiniciar la API
    Given una sesión autenticada guardada en PostgreSQL
    And se reinicia la instancia de la API sin borrar la base de datos
    When consulta la sesión con la cookie vigente
    Then recibe authenticated true y el mismo propietario sin volver a introducir credenciales

  @s6
  Scenario: Expirar la sesión por inactividad
    Given una sesión que supera treinta minutos de inactividad
    When solicita una operación privada con su cookie antigua
    Then recibe HTTP 401 UNAUTHENTICATED sin realizar escrituras
    And la web retira los datos privados y ofrece volver a acceder

  @s7
  Scenario: Cerrar e invalidar la sesión del servidor
    Given una sesión autenticada con token CSRF vigente
    When solicita POST /api/session/logout
    Then recibe HTTP 204 y expira la cookie con el mismo nombre y Path
    And reutilizar la cookie antigua no permite consultar ni modificar proyectos
    And la web elimina vistas y borradores privados antes de mostrar acceso

  @s8
  Scenario: No cerrar sesión mediante un enlace GET
    Given una sesión autenticada
    When solicita GET /api/session/logout
    Then no invalida la sesión ni ejecuta un cierre

  @s9
  Scenario Outline: Rechazar operaciones con protección CSRF inválida
    Given una sesión con el nivel de acceso requerido para <operación>
    When envía <operación> sin token CSRF válido
    Then recibe HTTP 403 CSRF_INVALID sin ejecutar la operación
    Examples:
      | operación            |
      | iniciar sesión       |
      | cerrar sesión        |
      | crear proyecto       |
      | editar proyecto      |
      | cambiar estado       |

  @s10
  Scenario: Distinguir sesión caducada de token inválido
    Given una cookie que ya no autentica al propietario
    When intenta modificar un proyecto sin token CSRF vigente
    Then recibe HTTP 401 UNAUTHENTICATED sin modificar proyecto ni outbox

  @s11
  Scenario: Mantener la protección de origen
    Given una sesión y un token CSRF válidos
    When un origen de navegador ajeno solicita una escritura
    Then recibe HTTP 403 UNTRUSTED_ORIGIN sin ejecutar la operación

  @s12
  Scenario Outline: No presentar fallo de almacenamiento como éxito de sesión
    Given una sesión y credenciales o token vigentes para <operación>
    And PostgreSQL falla al <persistencia>
    When solicita <operación>
    Then recibe HTTP 503 SESSION_UNAVAILABLE sin respuesta de éxito ni detalles internos
    And la web no afirma haber iniciado o cerrado la sesión
    And un cierre no confirmado conserva la cookie anterior para reintentar tras recuperar el servicio
    And el cierre no confirmado retira la vista privada y ofrece recuperación deliberada
    Examples:
      | operación     | persistencia                 |
      | iniciar sesión | guardar la sesión autenticada |
      | cerrar sesión  | eliminar la sesión guardada   |

  @s13
  Scenario: Proteger la cookie y evitar almacenamiento de secretos en la web
    Given la aplicación usa su origen público HTTPS configurado
    When obtiene una cookie de sesión
    Then la cookie SESSION usa HttpOnly, Secure, SameSite Lax y Path /api
    And ni contraseña, cookie ni token CSRF se guardan en almacenamiento web persistente
    And las cabeceras arbitrarias del cliente no alteran la configuración Secure

  @s14
  Scenario: Conservar entrada y recuperación accesibles
    Given el formulario tiene usuario y contraseña con etiquetas y autocomplete estándar
    When inicia una solicitud demorada o fallida
    Then anuncia espera antes de 400 ms y bloquea el doble envío
    And permite pegar y usar gestores de contraseñas
    And borra la contraseña tras la respuesta y muestra errores comprensibles
    And una respuesta antigua no abre una vista después de abandonar el flujo

  @s15
  Scenario: Retirar acceso en otra pestaña
    Given dos pestañas comparten una sesión autenticada
    When una confirma el cierre de sesión
    Then ambas retiran los datos privados sin transmitir secretos entre pestañas
    And recuperar visibilidad comprueba si la sesión sigue siendo válida

  @s16
  Scenario: Recuperar sólo una ruta local propia
    Given el propietario accede tras abrir una ruta de proyecto
    When confirma el inicio de sesión
    Then recupera una ruta local válida de la aplicación
    And no redirige hacia un origen externo suministrado por el cliente

  @s17
  Scenario: Renovar CSRF sin repetir escrituras automáticamente
    Given una escritura falla porque el token ya no es válido
    When recupera el estado de sesión y un token nuevo
    Then no reenvía la escritura sin otra acción explícita del propietario

  @s18
  Scenario: Mantener los recorridos existentes mediante sesión
    Given un cliente autenticado por el formulario real y su cookie
    When crea, consulta, edita y cambia el estado de un proyecto
    Then conserva los contratos de persistencia, precondiciones, capacidad y publicación de eventos
    And documentos y assets públicos permiten cargar el acceso sin autenticar datos privados

  @s19
  Scenario: Adaptar acceso y cierre a las pantallas y el teclado
    Given el formulario de acceso o su estado de error
    When se revisa la matriz UX con teclado y zoom real al 200 por ciento
    Then conserva etiquetas, foco visible, anuncios y controles principales de 44 por 44 píxeles CSS
    And no hay desplazamiento horizontal a 320 píxeles CSS
    And registra los treinta principios y los límites de las comprobaciones realizadas
