import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { SessionGate } from "./session-gate";
import "./styles.scss";
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <SessionGate />
  </StrictMode>,
);
