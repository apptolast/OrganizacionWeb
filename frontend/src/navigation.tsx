import { useSyncExternalStore, type ComponentProps } from "react";
const snapshot = () => window.location.pathname + window.location.search;
function subscribe(notify: () => void) {
  window.addEventListener("popstate", notify);
  return () => window.removeEventListener("popstate", notify);
}
export function useRoute() {
  return useSyncExternalStore(subscribe, snapshot);
}
export function isProjectRoute(route: string) {
  return route.startsWith("/proyectos");
}
export function RouteLink(props: ComponentProps<"a">) {
  return (
    <a
      {...props}
      onClick={(event) => {
        if (
          event.button !== 0 ||
          event.metaKey ||
          event.ctrlKey ||
          event.shiftKey ||
          event.altKey
        )
          return;
        event.preventDefault();
        window.history.pushState(null, "", props.href);
        window.dispatchEvent(new PopStateEvent("popstate"));
      }}
    />
  );
}
