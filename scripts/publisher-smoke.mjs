import { request as playwrightRequest } from "@playwright/test";
import { loginSession, csrfHeaders } from "./session-client.mjs";
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { randomBytes } from "node:crypto";
import { mkdirSync, mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { createServer } from "node:net";
import { resolve, join, relative, isAbsolute } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../", import.meta.url));
const scratchRoot = resolve(root, ".e2e-work");
mkdirSync(scratchRoot, { recursive: true });
const scratch = mkdtempSync(join(scratchRoot, "publisher-"));
const nonce = randomBytes(8).toString("hex");
const project = `organizationweb-publisher-${nonce}`;
const listener = createServer();
await new Promise((done) => listener.listen(0, "127.0.0.1", done));
const port = listener.address().port;
await new Promise((done) => listener.close(done));
const fixture = {
  DB_USERNAME: "smoke_user",
  DB_PASSWORD: randomBytes(24).toString("hex"),
  APP_AUTH_USERNAME: `smoke-${nonce}`,
  APP_AUTH_PASSWORD: randomBytes(24).toString("hex"),
  RABBITMQ_USERNAME: `smoke-${nonce}`,
  RABBITMQ_PASSWORD: randomBytes(24).toString("hex"),
  WEB_PORT: String(port),
  APP_PUBLIC_ORIGIN: `http://127.0.0.1:${port}`,
  APP_MAX_ACTIVE_PROJECTS: "3",
};
const env = { ...process.env, ...fixture };
const environmentFile = join(scratch, "test.env");
writeFileSync(
  environmentFile,
  Object.entries(fixture)
    .map(([key, value]) => `${key}=${value}`)
    .join("\n"),
  { mode: 0o600 },
);
const probeFile = join(scratch, "probe.json");
writeFileSync(
  probeFile,
  JSON.stringify({
    services: {
      probe: {
        image: "node:22.23.2-alpine3.23",
        command: ["sleep", "infinity"],
        environment: {
          RABBITMQ_USERNAME: "${RABBITMQ_USERNAME}",
          RABBITMQ_PASSWORD: "${RABBITMQ_PASSWORD}",
        },
      },
    },
  }),
);
const compose = [
  "compose",
  "--env-file",
  environmentFile,
  "-p",
  project,
  "-f",
  resolve(root, "docker-compose.yml"),
  "-f",
  resolve(root, "deploy/compose.publisher.yml"),
  "-f",
  probeFile,
];
function docker(args, input, timeout = 30000) {
  const result = spawnSync("docker", [...compose, ...args], {
    cwd: root,
    env,
    input,
    encoding: "utf8",
    timeout,
    maxBuffer: 8 * 1024 * 1024,
  });
  if (result.error || result.status !== 0)
    throw new Error(
      `Docker ${args[0]} failed (exit ${result.status ?? "timeout"}); output withheld to protect fixture credentials`,
    );
  return result.stdout.trim();
}
const pause = () => new Promise((done) => setTimeout(done, 1000));
async function eventually(label, operation, seconds = 90) {
  const deadline = Date.now() + seconds * 1000;
  do {
    const result = await operation();
    if (result) return result;
    await pause();
  } while (Date.now() < deadline);
  throw new Error(`Timed out: ${label}`);
}
const origin = `http://127.0.0.1:${port}`;
let application;
async function createProject(name) {
  const response = await application.post("/api/v1/projects", {
    headers: { ...(await csrfHeaders(application)), Origin: origin },
    data: { name, description: "smoke-private-description" },
    timeout: 4500,
  });
  assert.equal(
    response.status(),
    201,
    "API must confirm creation independently of broker",
  );
  return response.json();
}
function outbox(id, type = "ProjectCreated.v1") {
  assert.ok(
    [
      "ProjectCreated.v1",
      "ProjectUpdated.v1",
      "ProjectStatusChanged.v1",
    ].includes(type),
  );
  assert.match(id, /^[0-9a-f-]{36}$/i);
  const value = docker([
    "exec",
    "-T",
    "postgres",
    "psql",
    "-U",
    env.DB_USERNAME,
    "-d",
    "organization",
    "-At",
    "-c",
    `SELECT row_to_json(e) FROM outbox_events e WHERE aggregate_id='${id}' AND event_type='${type}'`,
  ]);
  return value ? JSON.parse(value) : undefined;
}
function management(path, body) {
  const code = `const credentials = Buffer.from(process.env.RABBITMQ_USERNAME + ':' + process.env.RABBITMQ_PASSWORD).toString('base64');
const response = await fetch('http://rabbitmq:15672/api/'+${JSON.stringify(path)}, { method:${JSON.stringify(body ? "POST" : "GET")}, headers:{Authorization:'Basic '+credentials,'Content-Type':'application/json'}, body:${JSON.stringify(body ? JSON.stringify(body) : undefined)}, signal:AbortSignal.timeout(5000) });
if(!response.ok) process.exit(1); console.log(JSON.stringify(await response.json()));`;
  return JSON.parse(
    docker(["exec", "-T", "probe", "node", "--input-type=module"], code),
  );
}
function messages() {
  return management("queues/organization/organization.project-created.v1/get", {
    count: 100,
    ackmode: "ack_requeue_true",
    encoding: "auto",
    truncate: 1000000,
  });
}
async function receiveOriginals(label, rows, seconds = 30) {
  return eventually(
    label,
    () => {
      let received;
      try {
        received = messages();
      } catch {
        return false;
      }
      return rows.every((row) =>
        received.some((item) => item.properties.message_id === row.event_id),
      )
        ? received
        : false;
    },
    seconds,
  );
}
function assertMessage(row, received) {
  const message = received.find(
    (item) => item.properties.message_id === row.event_id,
  );
  assert.ok(message, "Original event must remain available in RabbitMQ");
  assert.deepEqual(JSON.parse(message.payload), row.payload);
  assert.deepEqual(Object.keys(row.payload).sort(), [
    "aggregateId",
    "eventId",
    ...(row.event_type === "ProjectStatusChanged.v1"
      ? ["fromStatus"]
      : ["name"]),
    "occurredAt",
    "ownerId",
    "schemaVersion",
    ...(row.event_type === "ProjectStatusChanged.v1" ? ["toStatus"] : []),
    "type",
  ]);
  assert.equal(message.properties.content_type, "application/json");
  assert.equal(message.properties.delivery_mode, 2);
}
try {
  console.log(
    "Publisher smoke: starting isolated PostgreSQL/RabbitMQ/application stack",
  );
  docker(
    ["up", "--build", "-d", "--wait", "--wait-timeout", "180"],
    undefined,
    300000,
  );
  application = await playwrightRequest.newContext({
    baseURL: origin,
    timeout: 4500,
  });
  await eventually("API readiness", async () => {
    try {
      return (
        (await application.get("/api/session", { timeout: 2000 })).status() ===
        200
      );
    } catch {
      return false;
    }
  });
  await loginSession(application, {
    username: env.APP_AUTH_USERNAME,
    password: env.APP_AUTH_PASSWORD,
  });
  const created = await createProject("Smoke publicación real");
  const published = await eventually(
    "@s1 background publisher marks original event published",
    () => {
      const row = outbox(created.id);
      return row?.status === "published" ? row : false;
    },
  );
  assert.ok(published.published_at);
  assert.equal(published.attempts, 1);
  const received = await receiveOriginals("Rabbit management readiness", [
    published,
  ]);
  assertMessage(published, received);
  console.log(
    "PASS @s1: real POST -> background publisher -> original persistent RabbitMQ JSON",
  );
  docker(["stop", "rabbitmq"], undefined, 30000);
  const duringOutage = await createProject("Smoke recuperación del broker");
  const pending = await eventually(
    "@s16 failed publication remains pending",
    () => {
      const row = outbox(duringOutage.id);
      return row?.status === "pending" && row.attempts >= 1 ? row : false;
    },
    30,
  );
  assert.equal(pending.published_at, null);
  assert.equal(pending.last_error_code, "BROKER_UNAVAILABLE");
  const detail = await application.get(`/api/v1/projects/${created.id}`);
  assert.equal(detail.status(), 200);
  const edited = await application.put(`/api/v1/projects/${created.id}`, {
    headers: {
      ...(await csrfHeaders(application)),
      Origin: origin,
      "If-Match": detail.headers().etag,
    },
    data: {
      name: "Smoke edición durante caída",
      description: "smoke-private-description",
    },
    timeout: 4500,
  });
  assert.equal(
    edited.status(),
    200,
    "Edit API must confirm independently of broker",
  );
  const updatedPending = await eventually(
    "edit @s14 enabled worker retries Updated while broker is stopped",
    () => {
      const row = outbox(created.id, "ProjectUpdated.v1");
      return row?.status === "pending" && row.attempts >= 1 ? row : false;
    },
    30,
  );
  assert.equal(updatedPending.published_at, null);
  assert.equal(updatedPending.last_error_code, "BROKER_UNAVAILABLE");
  const stateChanged = await application.put(
    `/api/v1/projects/${created.id}/status`,
    {
      headers: {
        ...(await csrfHeaders(application)),
        Origin: origin,
        "If-Match": edited.headers().etag,
      },
      data: { status: "active" },
      timeout: 4500,
    },
  );
  assert.equal(
    stateChanged.status(),
    200,
    "State API must confirm independently of broker",
  );
  const statePending = await eventually(
    "states @s11 enabled worker retries with stopped broker",
    () => {
      const row = outbox(created.id, "ProjectStatusChanged.v1");
      return row?.status === "pending" && row.attempts >= 1 ? row : false;
    },
    30,
  );
  assert.equal(statePending.published_at, null);
  assert.equal(statePending.last_error_code, "BROKER_UNAVAILABLE");
  assert.equal(statePending.payload.fromStatus, "idea");
  assert.equal(statePending.payload.toStatus, "active");
  docker(["start", "rabbitmq"], undefined, 30000);
  const recovered = await eventually(
    "@s9 background publisher recovers automatically",
    () => {
      const row = outbox(duringOutage.id);
      return row?.status === "published" ? row : false;
    },
    120,
  );
  assert.equal(recovered.event_id, pending.event_id);
  assert.deepEqual(recovered.payload, pending.payload);
  assert.ok(recovered.attempts > pending.attempts);
  assertMessage(
    recovered,
    await receiveOriginals("Recovered Rabbit management readiness", [
      recovered,
    ]),
  );
  console.log(
    "PASS @s16/@s9: broker stopped, API201 under4.5s, original pending event published after recovery",
  );
  const updatedPublished = await eventually(
    "edit @s14 Updated publishes after broker recovery",
    () => {
      const row = outbox(created.id, "ProjectUpdated.v1");
      return row?.status === "published" ? row : false;
    },
    120,
  );
  assert.equal(updatedPublished.event_id, updatedPending.event_id);
  assert.deepEqual(updatedPublished.payload, updatedPending.payload);
  assert.ok(updatedPublished.attempts > updatedPending.attempts);
  const updatedReceived = await eventually(
    "edit @s15 dedicated Updated queue receives original event",
    () => {
      const received = management(
        "queues/organization/organization.project-updated.v1/get",
        {
          count: 100,
          ackmode: "ack_requeue_true",
          encoding: "auto",
          truncate: 1000000,
        },
      );
      return received.some(
        (item) => item.properties.message_id === updatedPublished.event_id,
      )
        ? received
        : false;
    },
    30,
  );
  assertMessage(updatedPublished, updatedReceived);
  console.log(
    "PASS edit @s14/@s15: enabled worker, broker stopped, PUT200 under4.5s, pending retry, original Updated published after recovery",
  );
  const statePublished = await eventually(
    "states @s13 StatusChanged publishes after recovery",
    () => {
      const row = outbox(created.id, "ProjectStatusChanged.v1");
      return row?.status === "published" ? row : false;
    },
    120,
  );
  assert.equal(statePublished.event_id, statePending.event_id);
  assert.deepEqual(statePublished.payload, statePending.payload);
  assert.ok(statePublished.attempts > statePending.attempts);
  const stateReceived = await eventually(
    "states @s13 dedicated queue receives original event",
    () => {
      const received = management(
        "queues/organization/organization.project-status-changed.v1/get",
        {
          count: 100,
          ackmode: "ack_requeue_true",
          encoding: "auto",
          truncate: 1000000,
        },
      );
      return received.some(
        (item) => item.properties.message_id === statePublished.event_id,
      )
        ? received
        : false;
    },
    30,
  );
  assertMessage(statePublished, stateReceived);
  const stateQueue = management(
    "queues/organization/organization.project-status-changed.v1",
  );
  assert.equal(stateQueue.durable, true);
  assert.equal(stateQueue.type, "quorum");
  assert.ok(
    management(
      "bindings/organization/e/organization.events/q/organization.project-status-changed.v1",
    ).some((binding) => binding.routing_key === "project.status-changed.v1"),
  );
  console.log(
    "PASS states @s11/@s13: enabled worker, stopped broker, HTTP 200, pending retry, original eight-field event received after recovery",
  );
  // Freeze publisher activity: recovery must come from the existing Rabbit volume,
  // not a worker silently recreating topology or republishing during the assertion.
  docker(["stop", "backend"], undefined, 30000);
  docker(["restart", "rabbitmq"], undefined, 30000);
  const retained = await receiveOriginals(
    "@s14 Rabbit management after same-volume restart",
    [published, recovered],
    90,
  );
  assertMessage(published, retained);
  assertMessage(recovered, retained);
  const queue = management(
    "queues/organization/organization.project-created.v1",
  );
  assert.equal(queue.durable, true);
  assert.equal(queue.auto_delete, false);
  assert.equal(queue.exclusive, false);
  assert.equal(queue.type, "quorum");
  assert.deepEqual(management("consumers/organization"), []);
  const exchange = management("exchanges/organization/organization.events");
  assert.equal(exchange.type, "direct");
  assert.equal(exchange.durable, true);
  assert.equal(exchange.auto_delete, false);
  const bindings = management(
    "bindings/organization/e/organization.events/q/organization.project-created.v1",
  );
  assert.ok(
    bindings.some((binding) => binding.routing_key === "project.created.v1"),
  );
  assert.deepEqual(outbox(created.id), published);
  assert.deepEqual(outbox(duringOutage.id), recovered);
  console.log(
    "PASS @s14/@s20: same-volume Rabbit restart retains messages and durable quorum topology with worker stopped",
  );
} catch (error) {
  console.error(
    error instanceof assert.AssertionError
      ? error.message
      : "Publisher smoke failed; transport details withheld to protect session credentials",
  );
  process.exitCode = 1;
} finally {
  if (application)
    await application.dispose().catch(() => {
      process.exitCode = 1;
    });
  try {
    docker(["down", "--volumes", "--remove-orphans"], undefined, 90000);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
  const target = resolve(scratch);
  const within = relative(scratchRoot, target);
  if (!within || within.startsWith("..") || isAbsolute(within))
    throw new Error("Refusing cleanup outside smoke scratch root");
  rmSync(target, { recursive: true, force: true });
}
