// @ts-nocheck
function stryNS_9fa48() {
  var g = typeof globalThis === 'object' && globalThis && globalThis.Math === Math && globalThis || new Function("return this")();
  var ns = g.__stryker__ || (g.__stryker__ = {});
  if (ns.activeMutant === undefined && g.process && g.process.env && g.process.env.__STRYKER_ACTIVE_MUTANT__) {
    ns.activeMutant = g.process.env.__STRYKER_ACTIVE_MUTANT__;
  }
  function retrieveNS() {
    return ns;
  }
  stryNS_9fa48 = retrieveNS;
  return retrieveNS();
}
stryNS_9fa48();
function stryCov_9fa48() {
  var ns = stryNS_9fa48();
  var cov = ns.mutantCoverage || (ns.mutantCoverage = {
    static: {},
    perTest: {}
  });
  function cover() {
    var c = cov.static;
    if (ns.currentTestId) {
      c = cov.perTest[ns.currentTestId] = cov.perTest[ns.currentTestId] || {};
    }
    var a = arguments;
    for (var i = 0; i < a.length; i++) {
      c[a[i]] = (c[a[i]] || 0) + 1;
    }
  }
  stryCov_9fa48 = cover;
  cover.apply(null, arguments);
}
function stryMutAct_9fa48(id) {
  var ns = stryNS_9fa48();
  function isActive(id) {
    if (ns.activeMutant === id) {
      if (ns.hitCount !== void 0 && ++ns.hitCount > ns.hitLimit) {
        throw new Error('Stryker: Hit count limit reached (' + ns.hitCount + ')');
      }
      return true;
    }
    return false;
  }
  stryMutAct_9fa48 = isActive;
  return isActive(id);
}
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { RouteLink } from "./navigation";
import { readProjects, type ProjectSummary } from "./read-projects-api";
import { readConnectorCatalog } from "./connectors-catalog-client";
import { GitlabConnectorError, connectGitlab, disconnectGitlab, readGitlabConnection, startGitlabImport, type GitlabConnection, type GitlabImportReceipt } from "./gitlab-connector-client";

/**
 * Pantalla del conector de GitLab. La sesión es la frontera: al cambiar de propietario se remonta
 * entera, de modo que ni el token escrito ni la ruta de un proyecto ajeno sobreviven al cambio.
 */
export function GitlabConnector({
  owner
}: {
  owner: string;
}) {
  if (stryMutAct_9fa48("454")) {
    {}
  } else {
    stryCov_9fa48("454");
    return <GitlabConnectorScreen key={owner} />;
  }
}
const MESSAGES: Record<string, string> = stryMutAct_9fa48("455") ? {} : (stryCov_9fa48("455"), {
  CONNECTION_INVALID: stryMutAct_9fa48("456") ? "" : (stryCov_9fa48("456"), "La conexión ya no es válida"),
  CONNECTION_NOT_FOUND: stryMutAct_9fa48("457") ? "" : (stryCov_9fa48("457"), "La conexión ya no existe"),
  GITLAB_UNAVAILABLE: stryMutAct_9fa48("458") ? "" : (stryCov_9fa48("458"), "GitLab no responde. Inténtalo más tarde"),
  IMPORT_IN_PROGRESS: stryMutAct_9fa48("459") ? "" : (stryCov_9fa48("459"), "Hay una importación en curso"),
  PROJECT_COMPLETED: stryMutAct_9fa48("460") ? "" : (stryCov_9fa48("460"), "El proyecto está terminado"),
  RESOURCE_NOT_FOUND: stryMutAct_9fa48("461") ? "" : (stryCov_9fa48("461"), "El proyecto ya no está disponible"),
  STORAGE_UNAVAILABLE: stryMutAct_9fa48("462") ? "" : (stryCov_9fa48("462"), "No se pudo completar. Inténtalo más tarde"),
  VALIDATION_ERROR: stryMutAct_9fa48("463") ? "" : (stryCov_9fa48("463"), "Revisa la ruta del proyecto y el token")
});

/**
 * Los fallos que no dicen nada del token pero sí dejan la vista desfasada: la acción honesta es
 * releer, nunca reintentar sola.
 */
const NEEDS_REFRESH = new Set(stryMutAct_9fa48("464") ? [] : (stryCov_9fa48("464"), [stryMutAct_9fa48("465") ? "" : (stryCov_9fa48("465"), "IMPORT_IN_PROGRESS"), stryMutAct_9fa48("466") ? "" : (stryCov_9fa48("466"), "CONNECTOR_ERROR")]));
const STATUS_TEXT: Record<GitlabConnection["status"], string> = stryMutAct_9fa48("467") ? {} : (stryCov_9fa48("467"), {
  connected: stryMutAct_9fa48("468") ? "" : (stryCov_9fa48("468"), "Conectado"),
  error: stryMutAct_9fa48("469") ? "" : (stryCov_9fa48("469"), "Error"),
  not_connected: stryMutAct_9fa48("470") ? "" : (stryCov_9fa48("470"), "No conectado")
});
export function describeFailure(error: GitlabConnectorError): string {
  if (stryMutAct_9fa48("471")) {
    {}
  } else {
    stryCov_9fa48("471");
    if (stryMutAct_9fa48("474") ? error.code !== "RATE_LIMITED" : stryMutAct_9fa48("473") ? false : stryMutAct_9fa48("472") ? true : (stryCov_9fa48("472", "473", "474"), error.code === (stryMutAct_9fa48("475") ? "" : (stryCov_9fa48("475"), "RATE_LIMITED")))) return stryMutAct_9fa48("476") ? `` : (stryCov_9fa48("476"), `GitLab limita las peticiones. Reintenta en ${stryMutAct_9fa48("477") ? error.retryAfterSeconds && 60 : (stryCov_9fa48("477"), error.retryAfterSeconds ?? 60)} segundos`);
    return stryMutAct_9fa48("478") ? MESSAGES[error.code] && "No se pudo completar. Inténtalo más tarde" : (stryCov_9fa48("478"), MESSAGES[error.code] ?? (stryMutAct_9fa48("479") ? "" : (stryCov_9fa48("479"), "No se pudo completar. Inténtalo más tarde")));
  }
}

/** Cuatro asteriscos y los cuatro caracteres que el servidor sí publica: el token no cabe aquí. */
export function maskHint(hint: string): string {
  if (stryMutAct_9fa48("480")) {
    {}
  } else {
    stryCov_9fa48("480");
    return stryMutAct_9fa48("481") ? `` : (stryCov_9fa48("481"), `••••${hint}`);
  }
}
function GitlabConnectorScreen() {
  if (stryMutAct_9fa48("482")) {
    {}
  } else {
    stryCov_9fa48("482");
    const [connection, setConnection] = useState<GitlabConnection | null>(null);
    const [disabled, setDisabled] = useState(stryMutAct_9fa48("483") ? true : (stryCov_9fa48("483"), false));
    const [loading, setLoading] = useState(stryMutAct_9fa48("484") ? false : (stryCov_9fa48("484"), true));
    const [projectPath, setProjectPath] = useState(stryMutAct_9fa48("485") ? "Stryker was here!" : (stryCov_9fa48("485"), ""));
    const [replacing, setReplacing] = useState(stryMutAct_9fa48("486") ? true : (stryCov_9fa48("486"), false));
    const [connecting, setConnecting] = useState(stryMutAct_9fa48("487") ? true : (stryCov_9fa48("487"), false));
    const [connectError, setConnectError] = useState<GitlabConnectorError | null>(null);
    const [projects, setProjects] = useState<ProjectSummary[]>(stryMutAct_9fa48("488") ? ["Stryker was here"] : (stryCov_9fa48("488"), []));
    const [selected, setSelected] = useState(stryMutAct_9fa48("489") ? "Stryker was here!" : (stryCov_9fa48("489"), ""));
    const [importing, setImporting] = useState(stryMutAct_9fa48("490") ? true : (stryCov_9fa48("490"), false));
    const [receipt, setReceipt] = useState<GitlabImportReceipt | null>(null);
    const [actionError, setActionError] = useState<GitlabConnectorError | null>(null);
    const [confirming, setConfirming] = useState(stryMutAct_9fa48("491") ? true : (stryCov_9fa48("491"), false));
    const heading = useRef<HTMLHeadingElement>(null);
    const tokenField = useRef<HTMLInputElement>(null);
    const replaceButton = useRef<HTMLButtonElement>(null);
    const receiptBox = useRef<HTMLElement>(null);
    const focusReplace = useRef(stryMutAct_9fa48("492") ? true : (stryCov_9fa48("492"), false));
    const focusHeading = useRef(stryMutAct_9fa48("493") ? true : (stryCov_9fa48("493"), false));
    const focusReceipt = useRef(stryMutAct_9fa48("494") ? true : (stryCov_9fa48("494"), false));
    const pending = useRef<AbortController | null>(null);
    const mounted = useRef(stryMutAct_9fa48("495") ? false : (stryCov_9fa48("495"), true));
    useLayoutEffect(() => {
      if (stryMutAct_9fa48("497")) {
        {}
      } else {
        stryCov_9fa48("497");
        mounted.current = stryMutAct_9fa48("498") ? false : (stryCov_9fa48("498"), true);
        stryMutAct_9fa48("499") ? heading.current.focus() : (stryCov_9fa48("499"), heading.current?.focus());
        return () => {
          if (stryMutAct_9fa48("500")) {
            {}
          } else {
            stryCov_9fa48("500");
            mounted.current = stryMutAct_9fa48("501") ? true : (stryCov_9fa48("501"), false);
            stryMutAct_9fa48("502") ? pending.current.abort() : (stryCov_9fa48("502"), pending.current?.abort());
            pending.current = null;
          }
        };
      }
    }, stryMutAct_9fa48("503") ? ["Stryker was here"] : (stryCov_9fa48("503"), []));
    const live = stryMutAct_9fa48("504") ? () => undefined : (stryCov_9fa48("504"), (() => {
      const live = (controller: AbortController) => stryMutAct_9fa48("507") ? mounted.current || !controller.signal.aborted : stryMutAct_9fa48("506") ? false : stryMutAct_9fa48("505") ? true : (stryCov_9fa48("505", "506", "507"), mounted.current && (stryMutAct_9fa48("508") ? controller.signal.aborted : (stryCov_9fa48("508"), !controller.signal.aborted)));
      return live;
    })());
    const loadConnection = useCallback(async () => {
      if (stryMutAct_9fa48("509")) {
        {}
      } else {
        stryCov_9fa48("509");
        const controller = new AbortController();
        pending.current = controller;
        try {
          if (stryMutAct_9fa48("510")) {
            {}
          } else {
            stryCov_9fa48("510");
            const view = await readGitlabConnection(controller.signal);
            if (stryMutAct_9fa48("513") ? false : stryMutAct_9fa48("512") ? true : stryMutAct_9fa48("511") ? live(controller) : (stryCov_9fa48("511", "512", "513"), !live(controller))) return;
            if (stryMutAct_9fa48("514")) {
              ;
            } else {
              stryCov_9fa48("514");
              setConnection(view);
            }
            setDisabled(stryMutAct_9fa48("516") ? true : (stryCov_9fa48("516"), false));
          }
        } catch (error) {
          if (stryMutAct_9fa48("517")) {
            {}
          } else {
            stryCov_9fa48("517");
            if (stryMutAct_9fa48("520") ? false : stryMutAct_9fa48("519") ? true : stryMutAct_9fa48("518") ? live(controller) : (stryCov_9fa48("518", "519", "520"), !live(controller))) return;
            if (stryMutAct_9fa48("523") ? error instanceof GitlabConnectorError || error.code === "CONNECTORS_DISABLED" : stryMutAct_9fa48("522") ? false : stryMutAct_9fa48("521") ? true : (stryCov_9fa48("521", "522", "523"), error instanceof GitlabConnectorError && (stryMutAct_9fa48("525") ? error.code !== "CONNECTORS_DISABLED" : stryMutAct_9fa48("524") ? true : (stryCov_9fa48("524", "525"), error.code === (stryMutAct_9fa48("526") ? "" : (stryCov_9fa48("526"), "CONNECTORS_DISABLED")))))) setDisabled(stryMutAct_9fa48("528") ? false : (stryCov_9fa48("528"), true));
            if (stryMutAct_9fa48("529")) {
              ;
            } else {
              stryCov_9fa48("529");
              setConnection(null);
            }
          }
        } finally {
          if (stryMutAct_9fa48("530")) {
            {}
          } else {
            stryCov_9fa48("530");
            if (stryMutAct_9fa48("532") ? false : stryMutAct_9fa48("531") ? true : (stryCov_9fa48("531", "532"), live(controller))) {
              if (stryMutAct_9fa48("533")) {
                {}
              } else {
                stryCov_9fa48("533");
                setLoading(stryMutAct_9fa48("535") ? true : (stryCov_9fa48("535"), false));
                if (stryMutAct_9fa48("538") ? pending.current !== controller : stryMutAct_9fa48("537") ? false : stryMutAct_9fa48("536") ? true : (stryCov_9fa48("536", "537", "538"), pending.current === controller)) pending.current = null;
              }
            }
          }
        }
      }
    }, stryMutAct_9fa48("539") ? ["Stryker was here"] : (stryCov_9fa48("539"), []));
    useEffect(() => {
      if (stryMutAct_9fa48("541")) {
        {}
      } else {
        stryCov_9fa48("541");
        void (async () => {
          if (stryMutAct_9fa48("542")) {
            {}
          } else {
            stryCov_9fa48("542");
            await loadConnection();
          }
        })();
      }
    }, stryMutAct_9fa48("543") ? [] : (stryCov_9fa48("543"), [loadConnection]));

    /**
     * Cada cambio de estado deja el foco donde el resultado se cuenta: en el encabezado o en el
     * aviso. Perderlo en el body obliga a quien navega con teclado o lector a buscar qué ha pasado.
     */
    useEffect(() => {
      if (stryMutAct_9fa48("545")) {
        {}
      } else {
        stryCov_9fa48("545");
        if (stryMutAct_9fa48("547") ? false : stryMutAct_9fa48("546") ? true : (stryCov_9fa48("546", "547"), focusReplace.current)) {
          if (stryMutAct_9fa48("548")) {
            {}
          } else {
            stryCov_9fa48("548");
            focusReplace.current = stryMutAct_9fa48("549") ? true : (stryCov_9fa48("549"), false);
            stryMutAct_9fa48("550") ? replaceButton.current.focus() : (stryCov_9fa48("550"), replaceButton.current?.focus());
            return;
          }
        }
        if (stryMutAct_9fa48("552") ? false : stryMutAct_9fa48("551") ? true : (stryCov_9fa48("551", "552"), focusReceipt.current)) {
          if (stryMutAct_9fa48("553")) {
            {}
          } else {
            stryCov_9fa48("553");
            focusReceipt.current = stryMutAct_9fa48("554") ? true : (stryCov_9fa48("554"), false);
            stryMutAct_9fa48("555") ? receiptBox.current.focus() : (stryCov_9fa48("555"), receiptBox.current?.focus());
            return;
          }
        }
        if (stryMutAct_9fa48("557") ? false : stryMutAct_9fa48("556") ? true : (stryCov_9fa48("556", "557"), focusHeading.current)) {
          if (stryMutAct_9fa48("558")) {
            {}
          } else {
            stryCov_9fa48("558");
            focusHeading.current = stryMutAct_9fa48("559") ? true : (stryCov_9fa48("559"), false);
            stryMutAct_9fa48("560") ? heading.current.focus() : (stryCov_9fa48("560"), heading.current?.focus());
          }
        }
      }
    });
    useEffect(() => {
      if (stryMutAct_9fa48("562")) {
        {}
      } else {
        stryCov_9fa48("562");
        const controller = new AbortController();
        void (async () => {
          if (stryMutAct_9fa48("563")) {
            {}
          } else {
            stryCov_9fa48("563");
            try {
              if (stryMutAct_9fa48("564")) {
                {}
              } else {
                stryCov_9fa48("564");
                const page = await readProjects(stryMutAct_9fa48("565") ? "" : (stryCov_9fa48("565"), "/proyectos"), controller.signal);
                if (stryMutAct_9fa48("568") ? (controller.signal.aborted || !mounted.current) && !("items" in page) : stryMutAct_9fa48("567") ? false : stryMutAct_9fa48("566") ? true : (stryCov_9fa48("566", "567", "568"), (stryMutAct_9fa48("570") ? controller.signal.aborted && !mounted.current : stryMutAct_9fa48("569") ? false : (stryCov_9fa48("569", "570"), controller.signal.aborted || (stryMutAct_9fa48("571") ? mounted.current : (stryCov_9fa48("571"), !mounted.current)))) || (stryMutAct_9fa48("572") ? "items" in page : (stryCov_9fa48("572"), !((stryMutAct_9fa48("573") ? "" : (stryCov_9fa48("573"), "items")) in page))))) return;
                // Un proyecto terminado no admite tareas nuevas: ofrecerlo sería ofrecer un 409.
                const open = stryMutAct_9fa48("574") ? page.items : (stryCov_9fa48("574"), page.items.filter(stryMutAct_9fa48("575") ? () => undefined : (stryCov_9fa48("575"), item => stryMutAct_9fa48("578") ? item.status === "completed" : stryMutAct_9fa48("577") ? false : stryMutAct_9fa48("576") ? true : (stryCov_9fa48("576", "577", "578"), item.status !== (stryMutAct_9fa48("579") ? "" : (stryCov_9fa48("579"), "completed"))))));
                if (stryMutAct_9fa48("580")) {
                  ;
                } else {
                  stryCov_9fa48("580");
                  setProjects(open);
                }
                setSelected(stryMutAct_9fa48("582") ? () => undefined : (stryCov_9fa48("582"), current => stryMutAct_9fa48("585") ? current && (open[0]?.id ?? "") : stryMutAct_9fa48("584") ? false : stryMutAct_9fa48("583") ? true : (stryCov_9fa48("583", "584", "585"), current || (stryMutAct_9fa48("586") ? open[0]?.id && "" : (stryCov_9fa48("586"), (stryMutAct_9fa48("587") ? open[0].id : (stryCov_9fa48("587"), open[0]?.id)) ?? (stryMutAct_9fa48("588") ? "Stryker was here!" : (stryCov_9fa48("588"), "")))))));
              }
            } catch {
              // El selector sin proyectos ya cuenta por sí mismo que no hay dónde importar.
            }
          }
        })();
        return stryMutAct_9fa48("589") ? () => undefined : (stryCov_9fa48("589"), () => controller.abort());
      }
    }, stryMutAct_9fa48("590") ? ["Stryker was here"] : (stryCov_9fa48("590"), []));
    async function submitConnection(event: React.FormEvent) {
      if (stryMutAct_9fa48("591")) {
        {}
      } else {
        stryCov_9fa48("591");
        if (stryMutAct_9fa48("592")) {
          ;
        } else {
          stryCov_9fa48("592");
          event.preventDefault();
        }
        if (stryMutAct_9fa48("594") ? false : stryMutAct_9fa48("593") ? true : (stryCov_9fa48("593", "594"), connecting)) return;
        setConnecting(stryMutAct_9fa48("596") ? false : (stryCov_9fa48("596"), true));
        if (stryMutAct_9fa48("597")) {
          ;
        } else {
          stryCov_9fa48("597");
          setConnectError(null);
        }
        const controller = new AbortController();
        pending.current = controller;
        try {
          if (stryMutAct_9fa48("598")) {
            {}
          } else {
            stryCov_9fa48("598");
            const view = await connectGitlab(stryMutAct_9fa48("599") ? {} : (stryCov_9fa48("599"), {
              token: stryMutAct_9fa48("600") ? tokenField.current?.value && "" : (stryCov_9fa48("600"), (stryMutAct_9fa48("601") ? tokenField.current.value : (stryCov_9fa48("601"), tokenField.current?.value)) ?? (stryMutAct_9fa48("602") ? "Stryker was here!" : (stryCov_9fa48("602"), ""))),
              projectPath
            }), controller.signal);
            if (stryMutAct_9fa48("605") ? false : stryMutAct_9fa48("604") ? true : stryMutAct_9fa48("603") ? live(controller) : (stryCov_9fa48("603", "604", "605"), !live(controller))) return;
            if (stryMutAct_9fa48("606")) {
              ;
            } else {
              stryCov_9fa48("606");
              setConnection(view);
            } // Al ocultarse el formulario, el nodo que tenía el token desaparece con él.
            setReplacing(stryMutAct_9fa48("608") ? true : (stryCov_9fa48("608"), false));
            focusHeading.current = stryMutAct_9fa48("609") ? false : (stryCov_9fa48("609"), true);
          }
        } catch (error) {
          if (stryMutAct_9fa48("610")) {
            {}
          } else {
            stryCov_9fa48("610");
            if (stryMutAct_9fa48("613") ? false : stryMutAct_9fa48("612") ? true : stryMutAct_9fa48("611") ? live(controller) : (stryCov_9fa48("611", "612", "613"), !live(controller))) return;
            if (stryMutAct_9fa48("614")) {
              ;
            } else {
              stryCov_9fa48("614");
              setConnectError(error instanceof GitlabConnectorError ? error : new GitlabConnectorError({}));
            }
          }
        } finally {
          if (stryMutAct_9fa48("615")) {
            {}
          } else {
            stryCov_9fa48("615");
            if (stryMutAct_9fa48("617") ? false : stryMutAct_9fa48("616") ? true : (stryCov_9fa48("616", "617"), live(controller))) {
              if (stryMutAct_9fa48("618")) {
                {}
              } else {
                stryCov_9fa48("618");
                setConnecting(stryMutAct_9fa48("620") ? true : (stryCov_9fa48("620"), false));
                if (stryMutAct_9fa48("623") ? pending.current !== controller : stryMutAct_9fa48("622") ? false : stryMutAct_9fa48("621") ? true : (stryCov_9fa48("621", "622", "623"), pending.current === controller)) pending.current = null;
              }
            }
          }
        }
      }
    }
    async function startImport() {
      if (stryMutAct_9fa48("624")) {
        {}
      } else {
        stryCov_9fa48("624");
        if (stryMutAct_9fa48("627") ? importing && !selected : stryMutAct_9fa48("626") ? false : stryMutAct_9fa48("625") ? true : (stryCov_9fa48("625", "626", "627"), importing || (stryMutAct_9fa48("628") ? selected : (stryCov_9fa48("628"), !selected)))) return;
        setImporting(stryMutAct_9fa48("630") ? false : (stryCov_9fa48("630"), true));
        if (stryMutAct_9fa48("631")) {
          ;
        } else {
          stryCov_9fa48("631");
          setActionError(null);
        }
        if (stryMutAct_9fa48("632")) {
          ;
        } else {
          stryCov_9fa48("632");
          setReceipt(null);
        }
        const controller = new AbortController();
        pending.current = controller;
        try {
          if (stryMutAct_9fa48("633")) {
            {}
          } else {
            stryCov_9fa48("633");
            const started = await startGitlabImport(selected, controller.signal);
            if (stryMutAct_9fa48("636") ? false : stryMutAct_9fa48("635") ? true : stryMutAct_9fa48("634") ? live(controller) : (stryCov_9fa48("634", "635", "636"), !live(controller))) return;
            if (stryMutAct_9fa48("637")) {
              ;
            } else {
              stryCov_9fa48("637");
              setReceipt(started);
            }
            focusReceipt.current = stryMutAct_9fa48("638") ? false : (stryCov_9fa48("638"), true);
          }
        } catch (error) {
          if (stryMutAct_9fa48("639")) {
            {}
          } else {
            stryCov_9fa48("639");
            if (stryMutAct_9fa48("642") ? false : stryMutAct_9fa48("641") ? true : stryMutAct_9fa48("640") ? live(controller) : (stryCov_9fa48("640", "641", "642"), !live(controller))) return;
            const failure = error instanceof GitlabConnectorError ? error : new GitlabConnectorError({});
            if (stryMutAct_9fa48("643")) {
              ;
            } else {
              stryCov_9fa48("643");
              setActionError(failure);
            }
            if (stryMutAct_9fa48("646") ? failure.code !== "CONNECTION_INVALID" : stryMutAct_9fa48("645") ? false : stryMutAct_9fa48("644") ? true : (stryCov_9fa48("644", "645", "646"), failure.code === (stryMutAct_9fa48("647") ? "" : (stryCov_9fa48("647"), "CONNECTION_INVALID")))) {
              if (stryMutAct_9fa48("648")) {
                {}
              } else {
                stryCov_9fa48("648");
                // El 409 es el servidor confirmando que el token dejó de valer: no es optimismo.
                setConnection(stryMutAct_9fa48("650") ? () => undefined : (stryCov_9fa48("650"), current => current ? stryMutAct_9fa48("651") ? {} : (stryCov_9fa48("651"), {
                  ...current,
                  status: stryMutAct_9fa48("652") ? "" : (stryCov_9fa48("652"), "error"),
                  lastError: stryMutAct_9fa48("653") ? {} : (stryCov_9fa48("653"), {
                    code: failure.code,
                    at: new Date().toISOString()
                  })
                }) : current));
                focusReplace.current = stryMutAct_9fa48("654") ? false : (stryCov_9fa48("654"), true);
              }
            }
          }
        } finally {
          if (stryMutAct_9fa48("655")) {
            {}
          } else {
            stryCov_9fa48("655");
            if (stryMutAct_9fa48("657") ? false : stryMutAct_9fa48("656") ? true : (stryCov_9fa48("656", "657"), live(controller))) {
              if (stryMutAct_9fa48("658")) {
                {}
              } else {
                stryCov_9fa48("658");
                setImporting(stryMutAct_9fa48("660") ? true : (stryCov_9fa48("660"), false));
                if (stryMutAct_9fa48("663") ? pending.current !== controller : stryMutAct_9fa48("662") ? false : stryMutAct_9fa48("661") ? true : (stryCov_9fa48("661", "662", "663"), pending.current === controller)) pending.current = null;
              }
            }
          }
        }
      }
    }
    async function confirmDisconnect() {
      if (stryMutAct_9fa48("664")) {
        {}
      } else {
        stryCov_9fa48("664");
        setConfirming(stryMutAct_9fa48("666") ? true : (stryCov_9fa48("666"), false));
        if (stryMutAct_9fa48("667")) {
          ;
        } else {
          stryCov_9fa48("667");
          setActionError(null);
        }
        const controller = new AbortController();
        pending.current = controller;
        try {
          if (stryMutAct_9fa48("668")) {
            {}
          } else {
            stryCov_9fa48("668");
            await disconnectGitlab(controller.signal);
            if (stryMutAct_9fa48("671") ? false : stryMutAct_9fa48("670") ? true : stryMutAct_9fa48("669") ? live(controller) : (stryCov_9fa48("669", "670", "671"), !live(controller))) return;
            // Sólo tras el 204 se vuelve al estado sin conexión, nunca antes.
            if (stryMutAct_9fa48("672")) {
              ;
            } else {
              stryCov_9fa48("672");
              setConnection(null);
            }
            if (stryMutAct_9fa48("673")) {
              ;
            } else {
              stryCov_9fa48("673");
              setReceipt(null);
            }
            setProjectPath(stryMutAct_9fa48("675") ? "Stryker was here!" : (stryCov_9fa48("675"), ""));
            setReplacing(stryMutAct_9fa48("677") ? true : (stryCov_9fa48("677"), false));
            focusHeading.current = stryMutAct_9fa48("678") ? false : (stryCov_9fa48("678"), true);
          }
        } catch (error) {
          if (stryMutAct_9fa48("679")) {
            {}
          } else {
            stryCov_9fa48("679");
            if (stryMutAct_9fa48("682") ? false : stryMutAct_9fa48("681") ? true : stryMutAct_9fa48("680") ? live(controller) : (stryCov_9fa48("680", "681", "682"), !live(controller))) return;
            if (stryMutAct_9fa48("683")) {
              ;
            } else {
              stryCov_9fa48("683");
              setActionError(error instanceof GitlabConnectorError ? error : new GitlabConnectorError({}));
            }
          }
        } finally {
          if (stryMutAct_9fa48("684")) {
            {}
          } else {
            stryCov_9fa48("684");
            if (stryMutAct_9fa48("686") ? false : stryMutAct_9fa48("685") ? true : (stryCov_9fa48("685", "686"), live(controller))) {
              if (stryMutAct_9fa48("687")) {
                {}
              } else {
                stryCov_9fa48("687");
                if (stryMutAct_9fa48("690") ? pending.current !== controller : stryMutAct_9fa48("689") ? false : stryMutAct_9fa48("688") ? true : (stryCov_9fa48("688", "689", "690"), pending.current === controller)) pending.current = null;
              }
            }
          }
        }
      }
    }

    /**
     * Relee lo que el servidor sabe: la conexión y el catálogo. No reintenta la acción que falló;
     * un reintento automático sobre una importación en curso es exactamente lo que la agravaría.
     */
    async function refreshStatus() {
      if (stryMutAct_9fa48("691")) {
        {}
      } else {
        stryCov_9fa48("691");
        if (stryMutAct_9fa48("692")) {
          ;
        } else {
          stryCov_9fa48("692");
          setActionError(null);
        }
        const controller = new AbortController();
        await loadConnection();
        try {
          if (stryMutAct_9fa48("693")) {
            {}
          } else {
            stryCov_9fa48("693");
            await readConnectorCatalog(controller.signal);
          }
        } catch {
          // El catálogo es contexto: que no se pueda leer no cambia esta pantalla.
        }
      }
    }
    function replaceToken() {
      if (stryMutAct_9fa48("694")) {
        {}
      } else {
        stryCov_9fa48("694");
        if (stryMutAct_9fa48("695")) {
          ;
        } else {
          stryCov_9fa48("695");
          setConnectError(null);
        }
        setProjectPath(stryMutAct_9fa48("697") ? connection?.projectPath && "" : (stryCov_9fa48("697"), (stryMutAct_9fa48("698") ? connection.projectPath : (stryCov_9fa48("698"), connection?.projectPath)) ?? (stryMutAct_9fa48("699") ? "Stryker was here!" : (stryCov_9fa48("699"), ""))));
        setReplacing(stryMutAct_9fa48("701") ? false : (stryCov_9fa48("701"), true));
      }
    }
    const isConnected = stryMutAct_9fa48("704") ? connection?.status !== "connected" : stryMutAct_9fa48("703") ? false : stryMutAct_9fa48("702") ? true : (stryCov_9fa48("702", "703", "704"), (stryMutAct_9fa48("705") ? connection.status : (stryCov_9fa48("705"), connection?.status)) === (stryMutAct_9fa48("706") ? "" : (stryCov_9fa48("706"), "connected")));
    const showPanel = stryMutAct_9fa48("709") ? !disabled && !loading && Boolean(connection) || !replacing : stryMutAct_9fa48("708") ? false : stryMutAct_9fa48("707") ? true : (stryCov_9fa48("707", "708", "709"), (stryMutAct_9fa48("711") ? !disabled && !loading || Boolean(connection) : stryMutAct_9fa48("710") ? true : (stryCov_9fa48("710", "711"), (stryMutAct_9fa48("713") ? !disabled || !loading : stryMutAct_9fa48("712") ? true : (stryCov_9fa48("712", "713"), (stryMutAct_9fa48("714") ? disabled : (stryCov_9fa48("714"), !disabled)) && (stryMutAct_9fa48("715") ? loading : (stryCov_9fa48("715"), !loading)))) && Boolean(connection))) && (stryMutAct_9fa48("716") ? replacing : (stryCov_9fa48("716"), !replacing)));
    const showForm = stryMutAct_9fa48("719") ? !disabled && !loading || !isConnected || replacing : stryMutAct_9fa48("718") ? false : stryMutAct_9fa48("717") ? true : (stryCov_9fa48("717", "718", "719"), (stryMutAct_9fa48("721") ? !disabled || !loading : stryMutAct_9fa48("720") ? true : (stryCov_9fa48("720", "721"), (stryMutAct_9fa48("722") ? disabled : (stryCov_9fa48("722"), !disabled)) && (stryMutAct_9fa48("723") ? loading : (stryCov_9fa48("723"), !loading)))) && (stryMutAct_9fa48("725") ? !isConnected && replacing : stryMutAct_9fa48("724") ? true : (stryCov_9fa48("724", "725"), (stryMutAct_9fa48("726") ? isConnected : (stryCov_9fa48("726"), !isConnected)) || replacing)));
    return <main id="proyectos" className="gitlab-connector" tabIndex={stryMutAct_9fa48("727") ? +1 : (stryCov_9fa48("727"), -1)}>
      <h1 ref={heading} tabIndex={stryMutAct_9fa48("728") ? +1 : (stryCov_9fa48("728"), -1)}>
        Conector de GitLab
      </h1>
      <p>
        <RouteLink href="/conectores">Conectores</RouteLink>
      </p>

      {disabled ? <p role="alert">
          Falta configuración del servidor para usar los conectores. Pide a
          quien administra esta instalación que configure la clave de
          conectores.
        </p> : null}

      {(stryMutAct_9fa48("731") ? showPanel && connection || connection.status !== "not_connected" : stryMutAct_9fa48("730") ? false : stryMutAct_9fa48("729") ? true : (stryCov_9fa48("729", "730", "731"), (stryMutAct_9fa48("733") ? showPanel || connection : stryMutAct_9fa48("732") ? true : (stryCov_9fa48("732", "733"), showPanel && connection)) && (stryMutAct_9fa48("735") ? connection.status === "not_connected" : stryMutAct_9fa48("734") ? true : (stryCov_9fa48("734", "735"), connection.status !== (stryMutAct_9fa48("736") ? "" : (stryCov_9fa48("736"), "not_connected")))))) ? <section aria-label="Conexión">
          <dl>
            <dt>Estado</dt>
            <dd>{STATUS_TEXT[connection.status]}</dd>
            <dt>Proyecto</dt>
            <dd>{connection.projectPath}</dd>
            <dt>Identificador del proyecto</dt>
            <dd>{connection.projectId}</dd>
            <dt>Token</dt>
            <dd>{maskHint(stryMutAct_9fa48("737") ? connection.tokenHint && "" : (stryCov_9fa48("737"), connection.tokenHint ?? (stryMutAct_9fa48("738") ? "Stryker was here!" : (stryCov_9fa48("738"), ""))))}</dd>
          </dl>

          <div>
            <label htmlFor="gitlab-project">Proyecto de destino</label>
            <select id="gitlab-project" value={selected} disabled={importing} onChange={stryMutAct_9fa48("739") ? () => undefined : (stryCov_9fa48("739"), event => setSelected(event.target.value))}>
              {projects.map(stryMutAct_9fa48("740") ? () => undefined : (stryCov_9fa48("740"), project => <option key={project.id} value={project.id}>
                  {project.name}
                </option>))}
            </select>
            <button type="button" disabled={importing} onClick={stryMutAct_9fa48("741") ? () => undefined : (stryCov_9fa48("741"), () => void startImport())}>
              Importar issues
            </button>
          </div>

          <button type="button" ref={replaceButton} onClick={replaceToken}>
            Actualizar token
          </button>

          {confirming ? <div role="group" aria-label="Confirmar desconexión">
              <p>
                Se borrará el token guardado. Las tareas ya importadas y sus
                enlaces se conservan.
              </p>
              <button type="button" onClick={stryMutAct_9fa48("742") ? () => undefined : (stryCov_9fa48("742"), () => void confirmDisconnect())}>
                Confirmar desconexión
              </button>
              <button type="button" onClick={stryMutAct_9fa48("743") ? () => undefined : (stryCov_9fa48("743"), () => setConfirming(stryMutAct_9fa48("744") ? true : (stryCov_9fa48("744"), false)))}>
                Cancelar
              </button>
            </div> : <button type="button" onClick={stryMutAct_9fa48("745") ? () => undefined : (stryCov_9fa48("745"), () => setConfirming(stryMutAct_9fa48("746") ? false : (stryCov_9fa48("746"), true)))}>
              Desconectar
            </button>}
        </section> : null}

      {showForm ? <form onSubmit={stryMutAct_9fa48("747") ? () => undefined : (stryCov_9fa48("747"), event => void submitConnection(event))} noValidate>
          <div>
            <label htmlFor="gitlab-path">Ruta del proyecto</label>
            <input id="gitlab-path" name="projectPath" autoComplete="off" value={projectPath} readOnly={connecting} aria-invalid={(stryMutAct_9fa48("748") ? connectError.fields.projectPath : (stryCov_9fa48("748"), connectError?.fields.projectPath)) ? stryMutAct_9fa48("749") ? false : (stryCov_9fa48("749"), true) : undefined} aria-describedby="gitlab-path-help" onChange={stryMutAct_9fa48("750") ? () => undefined : (stryCov_9fa48("750"), event => setProjectPath(event.target.value))} />
            <p id="gitlab-path-help">
              Con la forma grupo/proyecto, como aparece en la dirección del
              proyecto.
            </p>
          </div>
          <div>
            <label htmlFor="gitlab-token">Token de acceso personal</label>
            <input id="gitlab-token" name="token" type="password" autoComplete="off" ref={tokenField} defaultValue="" readOnly={connecting} aria-invalid={(stryMutAct_9fa48("751") ? connectError.fields.token : (stryCov_9fa48("751"), connectError?.fields.token)) ? stryMutAct_9fa48("752") ? false : (stryCov_9fa48("752"), true) : undefined} aria-describedby="gitlab-token-help" />
            <p id="gitlab-token-help">
              Usa un token personal con alcance read_api y nada más.
            </p>
          </div>
          {connectError ? <p role="alert">{describeFailure(connectError)}</p> : null}
          <button type="submit" disabled={connecting}>
            Conectar
          </button>
        </form> : null}

      {actionError ? <div>
          <p role="alert">{describeFailure(actionError)}</p>
          {NEEDS_REFRESH.has(actionError.code) ? <button type="button" onClick={stryMutAct_9fa48("753") ? () => undefined : (stryCov_9fa48("753"), () => void refreshStatus())}>
              Actualizar estado
            </button> : null}
        </div> : null}

      {receipt ? <Receipt receipt={receipt} boxRef={receiptBox} /> : null}

      <p role="status" aria-live="polite" aria-atomic="true">
        {connecting ? stryMutAct_9fa48("754") ? "" : (stryCov_9fa48("754"), "Guardando…") : importing ? stryMutAct_9fa48("755") ? "" : (stryCov_9fa48("755"), "Importando issues. Esto puede tardar un poco…") : stryMutAct_9fa48("756") ? "Stryker was here!" : (stryCov_9fa48("756"), "")}
      </p>
    </main>;
  }
}

/** Cuatro cifras etiquetadas, sin porcentajes ni barras: lo que el recibo dice y nada más. */
function Receipt({
  receipt,
  boxRef
}: {
  receipt: GitlabImportReceipt;
  boxRef: React.RefObject<HTMLElement | null>;
}) {
  if (stryMutAct_9fa48("757")) {
    {}
  } else {
    stryCov_9fa48("757");
    return <section aria-label="Resultado de la importación" ref={boxRef} tabIndex={stryMutAct_9fa48("758") ? +1 : (stryCov_9fa48("758"), -1)}>
      <dl>
        <dt>Creadas</dt>
        <dd>{receipt.created}</dd>
        <dt>Omitidas</dt>
        <dd>{receipt.skipped}</dd>
        <dt>Fallidas</dt>
        <dd>{receipt.failed}</dd>
        <dt>Truncado</dt>
        <dd>{receipt.truncated ? stryMutAct_9fa48("759") ? "" : (stryCov_9fa48("759"), "Sí") : stryMutAct_9fa48("760") ? "" : (stryCov_9fa48("760"), "No")}</dd>
      </dl>
      {receipt.truncated ? <p>
          El proyecto tiene más issues abiertas de las que caben en una
          importación: quedaron issues sin traer. Vuelve a importar para
          continuar.
        </p> : null}
    </section>;
  }
}