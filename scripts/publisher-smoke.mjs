import { request as playwrightRequest } from "@playwright/test";
import { loginSession, csrfHeaders } from "./session-client.mjs";
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { randomBytes, randomUUID } from "node:crypto";
import { createServer as createHttpServer } from "node:http";
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
function serviceSnapshot(service) {
  const id = docker(["ps", "-q", service]);
  assert.match(id, /^[0-9a-f]+$/);
  const result = spawnSync(
    "docker",
    ["inspect", "--format", "{{json .State.StartedAt}} {{json .Mounts}}", id],
    {
      encoding: "utf8",
      timeout: 10000,
    },
  );
  assert.equal(result.status, 0, "Inspect only the isolated smoke service");
  const [startedAt, ...mounts] = result.stdout.trim().split(" ");
  return {
    id,
    startedAt: JSON.parse(startedAt),
    mounts: JSON.parse(mounts.join(" ")),
  };
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
async function postWithLostAck(path, headers, data) {
  let upstreamStatus;
  const relay = createHttpServer(async (incoming, outgoing) => {
    try {
      const chunks = [];
      for await (const chunk of incoming) chunks.push(chunk);
      const upstream = await fetch(`${origin}${path}`, {
        method: "POST",
        headers: { ...incoming.headers, host: new URL(origin).host },
        body: Buffer.concat(chunks),
        redirect: "manual",
        signal: AbortSignal.timeout(4500),
      });
      upstreamStatus = upstream.status;
      await upstream.arrayBuffer();
    } catch {
      upstreamStatus = undefined;
    } finally {
      outgoing.destroy();
    }
  });
  await new Promise((done) => relay.listen(0, "127.0.0.1", done));
  try {
    await assert.rejects(() =>
      application.post(`http://127.0.0.1:${relay.address().port}${path}`, {
        headers,
        data,
        maxRetries: 0,
        timeout: 6000,
      }),
    );
    assert.equal(
      upstreamStatus,
      201,
      "API committed before relay discarded its response",
    );
  } finally {
    await new Promise((done) => relay.close(done));
  }
}
function outbox(id, type = "ProjectCreated.v1", taskId, revision) {
  assert.ok(
    [
      "ProjectCreated.v1",
      "ProjectUpdated.v1",
      "ProjectStatusChanged.v1",
      "TaskCreated.v1",
      "SubtaskCreated.v1",
      "TaskStatusChanged.v1",
      "WorkSessionStarted.v1",
      "WorkSessionStateChanged.v1",
    ].includes(type),
  );
  assert.match(id, /^[0-9a-f-]{36}$/i);
  const taskEvent = [
    "TaskCreated.v1",
    "SubtaskCreated.v1",
    "TaskStatusChanged.v1",
  ].includes(type);
  if (taskEvent) assert.match(taskId, /^[0-9a-f-]{36}$/i);
  const taskFilter = taskEvent ? ` AND payload->>'taskId'='${taskId}'` : "";
  const transitionEvent = type === "WorkSessionStateChanged.v1";
  if (transitionEvent) assert.match(revision, /^[1-9][0-9]*$/);
  const revisionFilter = transitionEvent
    ? ` AND payload->>'revision'='${revision}'`
    : "";
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
    `SELECT row_to_json(e) FROM outbox_events e WHERE aggregate_id='${id}' AND event_type='${type}'${taskFilter}${revisionFilter}`,
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
  const specificFields = {
    "ProjectCreated.v1": ["name"],
    "ProjectUpdated.v1": ["name"],
    "ProjectStatusChanged.v1": ["fromStatus", "toStatus"],
    "TaskCreated.v1": ["taskId", "title"],
    "SubtaskCreated.v1": ["taskId", "parentTaskId", "title"],
    "TaskStatusChanged.v1": ["taskId", "fromStatus", "toStatus"],
    "WorkSessionStarted.v1": [
      "projectId",
      "taskId",
      "plannedMinutes",
      "plannedEndAt",
      "zoneId",
    ],
    "WorkSessionStateChanged.v1": [
      "action",
      "revision",
      "fromStatus",
      "toStatus",
      "workedMicroseconds",
      "runningSince",
    ],
  }[row.event_type];
  assert.ok(specificFields, "Only approved event schemas are accepted");
  assert.deepEqual(
    Object.keys(row.payload).sort(),
    [
      "aggregateId",
      "eventId",
      "occurredAt",
      "ownerId",
      "schemaVersion",
      "type",
      ...specificFields,
    ].sort(),
  );
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
  const taskResponse = await application.post(
    `/api/v1/projects/${created.id}/tasks`,
    {
      headers: { ...(await csrfHeaders(application)), Origin: origin },
      data: {
        title: "Tarea durante caída del broker",
        completionCriterion: "Criterio privado sintético",
        estimatedMinutes: 25,
      },
      timeout: 4500,
    },
  );
  assert.equal(
    taskResponse.status(),
    201,
    "Task creation must confirm independently of broker",
  );
  const task = await taskResponse.json();
  const taskPending = await eventually(
    "tasks @s16 worker retries while broker is stopped",
    () => {
      const row = outbox(created.id, "TaskCreated.v1", task.id);
      return row?.status === "pending" && row.attempts >= 1 ? row : false;
    },
    30,
  );
  assert.equal(taskPending.published_at, null);
  assert.equal(taskPending.last_error_code, "BROKER_UNAVAILABLE");
  assert.deepEqual(
    Object.keys(taskPending.payload).sort(),
    [
      "eventId",
      "aggregateId",
      "ownerId",
      "occurredAt",
      "schemaVersion",
      "type",
      "taskId",
      "title",
    ].sort(),
  );
  assert.equal(taskPending.aggregate_id, created.id);
  assert.equal(taskPending.payload.aggregateId, created.id);
  assert.equal(taskPending.payload.taskId, task.id);
  assert.equal(taskPending.payload.title, task.title);
  const childResponse = await application.post(
    `/api/v1/projects/${created.id}/tasks/${task.id}/subtasks`,
    {
      headers: { ...(await csrfHeaders(application)), Origin: origin },
      data: {
        title: "Paso durante caída del broker",
        completionCriterion: "Criterio privado del paso",
        estimatedMinutes: 5,
      },
      timeout: 4500,
    },
  );
  assert.equal(
    childResponse.status(),
    201,
    "Subtask must commit independently of broker",
  );
  const child = await childResponse.json();
  const childPending = await eventually(
    "split @s37 pending retry with broker stopped",
    () => {
      const row = outbox(created.id, "SubtaskCreated.v1", child.id);
      return row?.status === "pending" && row.attempts >= 1 ? row : false;
    },
    30,
  );
  assert.equal(childPending.last_error_code, "BROKER_UNAVAILABLE");
  assert.equal(childPending.published_at, null);
  assert.equal(childPending.payload.aggregateId, created.id);
  assert.equal(childPending.payload.taskId, child.id);
  assert.equal(childPending.payload.parentTaskId, task.id);
  assert.notEqual(child.id, task.id);
  assert.equal(outbox(created.id, "TaskCreated.v1", child.id), undefined);
  const taskStatePath = `/api/v1/projects/${created.id}/tasks/${child.id}/status`;
  const stateBefore = await application.get(taskStatePath);
  assert.equal(stateBefore.status(), 200);
  const completedResponse = await application.put(taskStatePath, {
    headers: {
      ...(await csrfHeaders(application)),
      Origin: origin,
      "If-Match": stateBefore.headers().etag,
    },
    data: { status: "completed" },
    timeout: 4500,
  });
  assert.equal(
    completedResponse.status(),
    200,
    "Task transition must commit with broker stopped",
  );
  const completedState = await completedResponse.json();
  assert.equal(completedState.status, "completed");
  assert.equal(completedState.completedAt, completedState.updatedAt);
  const historyResponse = await application.get(
    `/api/v1/projects/${created.id}/tasks/${child.id}/history`,
  );
  assert.equal(historyResponse.status(), 200);
  const taskHistory = await historyResponse.json();
  assert.equal(taskHistory.items.length, 1);
  assert.equal(taskHistory.items[0].toStatus, "completed");
  assert.equal(taskHistory.items[0].occurredAt, completedState.updatedAt);
  const taskStatePending = await eventually(
    "complete @s19 pending task transition with broker stopped",
    () => {
      const row = outbox(created.id, "TaskStatusChanged.v1", child.id);
      return row?.status === "pending" && row.attempts >= 1 ? row : false;
    },
    30,
  );
  assert.equal(taskStatePending.last_error_code, "BROKER_UNAVAILABLE");
  assert.equal(taskStatePending.published_at, null);
  assert.equal(taskStatePending.payload.aggregateId, created.id);
  assert.equal(taskStatePending.payload.taskId, child.id);
  assert.equal(taskStatePending.payload.fromStatus, "pending");
  assert.equal(taskStatePending.payload.toStatus, "completed");
  assert.equal(taskStatePending.payload.occurredAt, completedState.updatedAt);
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
  const taskPublished = await eventually(
    "tasks @s17 publishes after broker recovery",
    () => {
      const row = outbox(created.id, "TaskCreated.v1", task.id);
      return row?.status === "published" ? row : false;
    },
    120,
  );
  assert.equal(taskPublished.event_id, taskPending.event_id);
  assert.deepEqual(taskPublished.payload, taskPending.payload);
  assert.ok(taskPublished.attempts > taskPending.attempts);
  const taskReceived = await eventually(
    "tasks @s17 dedicated queue receives original event",
    () => {
      const received = management(
        "queues/organization/organization.task-created.v1/get",
        {
          count: 100,
          ackmode: "ack_requeue_true",
          encoding: "auto",
          truncate: 1000000,
        },
      );
      return received.some(
        (item) => item.properties.message_id === taskPublished.event_id,
      )
        ? received
        : false;
    },
    30,
  );
  assertMessage(taskPublished, taskReceived);
  const taskQueue = management(
    "queues/organization/organization.task-created.v1",
  );
  assert.equal(taskQueue.durable, true);
  assert.equal(taskQueue.type, "quorum");
  assert.ok(
    management(
      "bindings/organization/e/organization.events/q/organization.task-created.v1",
    ).some((binding) => binding.routing_key === "task.created.v1"),
  );
  console.log(
    "PASS tasks @s16/@s17: broker stopped, HTTP 201, original TaskCreated recovered with distinct project and task identities",
  );
  const childPublished = await eventually(
    "split @s21 publishes original child event",
    () => {
      const row = outbox(created.id, "SubtaskCreated.v1", child.id);
      return row?.status === "published" ? row : false;
    },
    120,
  );
  assert.equal(childPublished.event_id, childPending.event_id);
  assert.deepEqual(childPublished.payload, childPending.payload);
  assert.ok(childPublished.attempts > childPending.attempts);
  async function receiveChild() {
    return eventually(
      "SubtaskCreated original message receipt",
      () => {
        const messages = management(
          "queues/organization/organization.subtask-created.v1/get",
          {
            count: 100,
            ackmode: "ack_requeue_true",
            encoding: "auto",
            truncate: 1000000,
          },
        );
        return messages.some(
          (item) => item.properties.message_id === childPublished.event_id,
        )
          ? messages
          : false;
      },
      30,
    );
  }
  assertMessage(childPublished, await receiveChild());
  const childQueue = management(
    "queues/organization/organization.subtask-created.v1",
  );
  assert.equal(childQueue.durable, true);
  assert.equal(childQueue.type, "quorum");
  assert.ok(
    management(
      "bindings/organization/e/organization.events/q/organization.subtask-created.v1",
    ).some((binding) => binding.routing_key === "subtask.created.v1"),
  );
  console.log(
    "PASS split @s21/@s37: HTTP 201 while broker stopped; original nine-field SubtaskCreated recovered",
  );
  const taskStatePublished = await eventually(
    "complete @s20 original transition publishes after recovery",
    () => {
      const row = outbox(created.id, "TaskStatusChanged.v1", child.id);
      return row?.status === "published" ? row : false;
    },
    120,
  );
  assert.equal(taskStatePublished.event_id, taskStatePending.event_id);
  assert.deepEqual(taskStatePublished.payload, taskStatePending.payload);
  assert.ok(taskStatePublished.attempts > taskStatePending.attempts);
  async function receiveTaskState() {
    return eventually(
      "TaskStatusChanged original message receipt",
      () => {
        const messages = management(
          "queues/organization/organization.task-status-changed.v1/get",
          {
            count: 100,
            ackmode: "ack_requeue_true",
            encoding: "auto",
            truncate: 1000000,
          },
        );
        return messages.some(
          (item) => item.properties.message_id === taskStatePublished.event_id,
        )
          ? messages
          : false;
      },
      30,
    );
  }
  assertMessage(taskStatePublished, await receiveTaskState());
  const taskStateQueue = management(
    "queues/organization/organization.task-status-changed.v1",
  );
  assert.equal(taskStateQueue.durable, true);
  assert.equal(taskStateQueue.type, "quorum");
  assert.ok(
    management(
      "bindings/organization/e/organization.events/q/organization.task-status-changed.v1",
    ).some((binding) => binding.routing_key === "task.status-changed.v1"),
  );
  console.log(
    "PASS complete @s19/@s20: broker stopped, HTTP 200 with durable history, original nine-field TaskStatusChanged recovered",
  );
  docker(["stop", "backend"], undefined, 30000);
  docker(["restart", "rabbitmq"], undefined, 30000);
  const retained = await receiveOriginals(
    "@s14 Rabbit management after same-volume restart",
    [published, recovered],
    90,
  );
  assertMessage(published, retained);
  assertMessage(recovered, retained);
  assertMessage(childPublished, await receiveChild());
  assert.deepEqual(
    outbox(created.id, "SubtaskCreated.v1", child.id),
    childPublished,
  );
  assertMessage(taskStatePublished, await receiveTaskState());
  assert.deepEqual(
    outbox(created.id, "TaskStatusChanged.v1", child.id),
    taskStatePublished,
  );
  const retainedTasks = await eventually(
    "TaskCreated retained after same-volume Rabbit restart",
    () => {
      const messages = management(
        "queues/organization/organization.task-created.v1/get",
        {
          count: 100,
          ackmode: "ack_requeue_true",
          encoding: "auto",
          truncate: 1000000,
        },
      );
      return messages.some(
        (item) => item.properties.message_id === taskPublished.event_id,
      )
        ? messages
        : false;
    },
    30,
  );
  assertMessage(taskPublished, retainedTasks);
  assert.deepEqual(
    outbox(created.id, "TaskCreated.v1", task.id),
    taskPublished,
  );
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
  docker(["start", "backend"], undefined, 30000);
  await eventually(
    "Task history after backend restart",
    async () => {
      const response = await application
        .get(`/api/v1/projects/${created.id}/tasks/${child.id}/history`, {
          timeout: 2000,
        })
        .catch(() => null);
      if (!response || response.status() !== 200) return false;
      assert.deepEqual(await response.json(), taskHistory);
      return true;
    },
    90,
  );
  const restartedState = await application.get(taskStatePath);
  assert.equal(restartedState.status(), 200);
  assert.deepEqual(await restartedState.json(), completedState);
  assert.equal(restartedState.headers().etag, completedResponse.headers().etag);
  console.log(
    "PASS complete @s11: persisted session, task snapshot and history survive backend restart",
  );
  // One request relay drops the actual HTTP response after the API has completed it.
  docker(["stop", "rabbitmq"], undefined, 30000);
  const workKey = randomUUID();
  const workPath = `/api/v1/projects/${created.id}/tasks/${task.id}/work-sessions`;
  const workHeaders = {
    ...(await csrfHeaders(application)),
    Origin: origin,
    "Idempotency-Key": workKey,
  };
  await postWithLostAck(workPath, workHeaders, { plannedMinutes: 25 });
  const workLookup = `/api/v1/work-sessions/by-request/${workKey}`;
  const recoveredStartResponse = await application.get(workLookup);
  assert.equal(recoveredStartResponse.status(), 200);
  const recoveredStart = await recoveredStartResponse.json();
  assert.deepEqual(
    Object.keys(recoveredStart).sort(),
    [
      "id",
      "projectId",
      "taskId",
      "startedAt",
      "plannedMinutes",
      "plannedEndAt",
      "zoneId",
    ].sort(),
  );
  assert.equal(recoveredStart.projectId, created.id);
  assert.equal(recoveredStart.taskId, task.id);
  assert.equal(recoveredStart.plannedMinutes, 25);
  assert.match(
    recoveredStart.startedAt,
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?Z$/,
  );
  assert.match(
    recoveredStart.plannedEndAt,
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?Z$/,
  );
  assert.equal(
    Date.parse(recoveredStart.plannedEndAt) -
      Date.parse(recoveredStart.startedAt),
    25 * 60 * 1000,
  );
  function workCounts() {
    assert.match(recoveredStart.id, /^[0-9a-f-]{36}$/i);
    return JSON.parse(
      docker([
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
        `SELECT json_build_object('sessions',(SELECT count(*) FROM work_sessions WHERE owner_id=(SELECT owner_id FROM work_sessions WHERE id='${recoveredStart.id}')),'events',(SELECT count(*) FROM outbox_events WHERE aggregate_id='${recoveredStart.id}' AND event_type='WorkSessionStarted.v1'))`,
      ]),
    );
  }
  assert.deepEqual(workCounts(), { sessions: 1, events: 1 });
  const workPending = await eventually(
    "start work broker failure preserves original event",
    () => {
      const row = outbox(recoveredStart.id, "WorkSessionStarted.v1");
      return row?.status === "pending" && row.attempts >= 1 ? row : false;
    },
    30,
  );
  assert.equal(workPending.last_error_code, "BROKER_UNAVAILABLE");
  assert.equal(workPending.published_at, null);
  assert.equal(workPending.payload.aggregateId, recoveredStart.id);
  assert.notEqual(workPending.payload.eventId, recoveredStart.id);
  assert.equal(workPending.payload.ownerId, env.APP_AUTH_USERNAME);
  assert.equal(workPending.payload.projectId, created.id);
  assert.equal(workPending.payload.taskId, task.id);
  assert.equal(workPending.payload.plannedMinutes, 25);
  assert.equal(workPending.payload.schemaVersion, 1);
  assert.equal(workPending.payload.type, "WorkSessionStarted.v1");
  assert.equal(workPending.payload.occurredAt, recoveredStart.startedAt);
  assert.equal(workPending.payload.plannedEndAt, recoveredStart.plannedEndAt);
  assert.equal(workPending.payload.zoneId, recoveredStart.zoneId);
  docker(["start", "rabbitmq"], undefined, 30000);
  const workPublished = await eventually(
    "start work original event published after broker recovery",
    () => {
      const row = outbox(recoveredStart.id, "WorkSessionStarted.v1");
      return row?.status === "published" ? row : false;
    },
  );
  assert.equal(workPublished.event_id, workPending.event_id);
  assert.deepEqual(workPublished.payload, workPending.payload);
  assert.ok(workPublished.attempts > workPending.attempts);
  const workQueue = "organization.work-session-started.v1";
  const workMessages = await eventually(
    "ninth route receives original WorkSessionStarted",
    () => {
      const receivedWork = management(`queues/organization/${workQueue}/get`, {
        count: 100,
        ackmode: "ack_requeue_true",
        encoding: "auto",
        truncate: 1000000,
      });
      return receivedWork.some(
        (item) => item.properties.message_id === workPublished.event_id,
      )
        ? receivedWork
        : false;
    },
  );
  assertMessage(workPublished, workMessages);
  const workTopology = management(`queues/organization/${workQueue}`);
  assert.equal(workTopology.durable, true);
  assert.equal(workTopology.type, "quorum");
  assert.ok(
    management(
      `bindings/organization/e/organization.events/q/${workQueue}`,
    ).some((binding) => binding.routing_key === "work-session.started.v1"),
  );
  assert.match(workPublished.event_id, /^[0-9a-f-]{36}$/i);
  docker([
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
    `DELETE FROM outbox_events WHERE event_id='${workPublished.event_id}' AND status='published'`,
  ]);
  assert.equal(outbox(recoveredStart.id, "WorkSessionStarted.v1"), undefined);
  docker(["restart", "backend"], undefined, 30000);
  await eventually(
    "start work receipt survives backend restart without outbox",
    async () => {
      const response = await application
        .get(workLookup, { timeout: 2000 })
        .catch(() => null);
      if (!response || response.status() !== 200) return false;
      assert.deepEqual(await response.json(), recoveredStart);
      return true;
    },
  );
  const workActive = await application.get("/api/v1/work-sessions/active");
  assert.equal(workActive.status(), 200);
  assert.deepEqual(await workActive.json(), { session: recoveredStart });
  assert.deepEqual(workCounts(), { sessions: 1, events: 0 });
  assert.equal(outbox(recoveredStart.id, "WorkSessionStarted.v1"), undefined);
  console.log(
    "PASS start_work_session @s25/@s26: lost HTTP response, owner/key recovery, real Rabbit retry/publication, restart without published outbox",
  );
  const sessionStatePath = `/api/v1/work-sessions/${recoveredStart.id}/state`;
  const initialStateResponse = await application.get(sessionStatePath);
  assert.equal(initialStateResponse.status(), 200);
  const initialState = await initialStateResponse.json();
  assert.equal(initialState.state.status, "running");
  assert.equal(initialState.state.revision, "1");
  assert.deepEqual(initialState.state.session, recoveredStart);
  const pauseKey = randomUUID();
  docker(["stop", "rabbitmq"], undefined, 30000);
  await postWithLostAck(
    `/api/v1/work-sessions/${recoveredStart.id}/pause`,
    {
      ...(await csrfHeaders(application)),
      Origin: origin,
      "Idempotency-Key": pauseKey,
      "Work-Session-Revision":
        initialStateResponse.headers()["work-session-revision"],
    },
    {},
  );
  const pauseLookup = `/api/v1/work-session-changes/by-request/${pauseKey}`;
  const pauseResponse = await application.get(pauseLookup);
  assert.equal(pauseResponse.status(), 200);
  const pauseReceipt = await pauseResponse.json();
  assert.deepEqual(
    Object.keys(pauseReceipt).sort(),
    ["id", "sessionId", "action", "occurredAt", "before", "after"].sort(),
  );
  assert.equal(pauseReceipt.sessionId, recoveredStart.id);
  assert.equal(pauseReceipt.action, "PAUSE");
  assert.deepEqual(pauseReceipt.before, initialState.state);
  assert.equal(pauseReceipt.after.status, "paused");
  assert.equal(pauseReceipt.after.revision, "2");
  assert.equal(pauseReceipt.after.runningSince, null);
  assert.equal(pauseReceipt.after.changedAt, pauseReceipt.occurredAt);
  assert.deepEqual(pauseReceipt.after.session, recoveredStart);
  const pausedStateResponse = await application.get(sessionStatePath);
  assert.equal(pausedStateResponse.status(), 200);
  assert.deepEqual(
    (await pausedStateResponse.json()).state,
    pauseReceipt.after,
  );
  const resumeResponse = await application.post(
    `/api/v1/work-sessions/${recoveredStart.id}/resume`,
    {
      headers: {
        ...(await csrfHeaders(application)),
        Origin: origin,
        "Idempotency-Key": randomUUID(),
        "Work-Session-Revision":
          pausedStateResponse.headers()["work-session-revision"],
      },
      data: {},
    },
  );
  assert.equal(resumeResponse.status(), 201);
  const resumeReceipt = await resumeResponse.json();
  assert.equal(
    resumeResponse.headers().location,
    `/api/v1/work-session-changes/${resumeReceipt.id}`,
  );
  assert.equal(resumeReceipt.action, "RESUME");
  assert.deepEqual(resumeReceipt.before, pauseReceipt.after);
  assert.equal(resumeReceipt.after.status, "running");
  assert.equal(resumeReceipt.after.revision, "3");
  assert.equal(resumeReceipt.after.runningSince, resumeReceipt.occurredAt);
  assert.equal(
    resumeReceipt.after.workedMicroseconds,
    pauseReceipt.after.workedMicroseconds,
  );
  assert.deepEqual(resumeReceipt.after.session, recoveredStart);
  const changeType = "WorkSessionStateChanged.v1";
  const pendingChanges = [];
  for (const receipt of [pauseReceipt, resumeReceipt]) {
    const row = await eventually(
      "transition remains pending while RabbitMQ is unavailable",
      () => {
        const value = outbox(
          recoveredStart.id,
          changeType,
          undefined,
          receipt.after.revision,
        );
        return value?.status === "pending" && value.attempts >= 1
          ? value
          : false;
      },
      30,
    );
    assert.equal(row.last_error_code, "BROKER_UNAVAILABLE");
    assert.equal(row.published_at, null);
    assert.deepEqual(row.payload, {
      eventId: row.event_id,
      aggregateId: recoveredStart.id,
      ownerId: env.APP_AUTH_USERNAME,
      occurredAt: receipt.occurredAt,
      schemaVersion: 1,
      type: changeType,
      action: receipt.action,
      revision: receipt.after.revision,
      fromStatus: receipt.before.status,
      toStatus: receipt.after.status,
      workedMicroseconds: receipt.after.workedMicroseconds,
      runningSince: receipt.after.runningSince,
    });
    pendingChanges.push(row);
  }
  docker(["start", "rabbitmq"], undefined, 30000);
  const publishedChanges = [];
  for (const pending of pendingChanges) {
    const row = await eventually(
      "original transition is published after broker recovery",
      () => {
        const value = outbox(
          recoveredStart.id,
          changeType,
          undefined,
          pending.payload.revision,
        );
        return value?.status === "published" ? value : false;
      },
    );
    assert.equal(row.event_id, pending.event_id);
    assert.deepEqual(row.payload, pending.payload);
    assert.ok(row.attempts > pending.attempts);
    publishedChanges.push(row);
  }
  const changeQueue = "organization.work-session-state-changed.v1";
  const changeMessages = await eventually(
    "tenth route receives both original transitions",
    () => {
      const received = management(`queues/organization/${changeQueue}/get`, {
        count: 100,
        ackmode: "ack_requeue_true",
        encoding: "auto",
        truncate: 1000000,
      });
      return publishedChanges.every((row) =>
        received.some((item) => item.properties.message_id === row.event_id),
      )
        ? received
        : false;
    },
  );
  for (const row of publishedChanges) assertMessage(row, changeMessages);
  const changeTopology = management(`queues/organization/${changeQueue}`);
  assert.equal(changeTopology.durable, true);
  assert.equal(changeTopology.type, "quorum");
  assert.ok(
    management(
      `bindings/organization/e/organization.events/q/${changeQueue}`,
    ).some(
      (binding) => binding.routing_key === "work-session.state-changed.v1",
    ),
  );
  function transitionCounts() {
    return JSON.parse(
      docker([
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
        `SELECT json_build_object('changes',(SELECT count(*) FROM work_session_changes WHERE session_id='${recoveredStart.id}'),'intervals',(SELECT count(*) FROM work_session_intervals WHERE session_id='${recoveredStart.id}'),'events',(SELECT count(*) FROM outbox_events WHERE aggregate_id='${recoveredStart.id}' AND event_type='WorkSessionStateChanged.v1'))`,
      ]),
    );
  }
  assert.deepEqual(transitionCounts(), { changes: 2, intervals: 1, events: 2 });
  for (const row of publishedChanges) {
    assert.match(row.event_id, /^[0-9a-f-]{36}$/i);
    docker([
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
      `DELETE FROM outbox_events WHERE event_id='${row.event_id}' AND status='published'`,
    ]);
  }
  const countsBeforeRecovery = transitionCounts();
  assert.deepEqual(countsBeforeRecovery, {
    changes: 2,
    intervals: 1,
    events: 0,
  });
  const backendBefore = serviceSnapshot("backend");
  const postgresBefore = serviceSnapshot("postgres");
  docker(["restart", "backend"], undefined, 30000);
  await eventually(
    "historical pause receipt survives restart and later resume without outbox",
    async () => {
      const response = await application
        .get(pauseLookup, { timeout: 2000 })
        .catch(() => null);
      if (!response || response.status() !== 200) return false;
      assert.deepEqual(await response.json(), pauseReceipt);
      return true;
    },
  );
  // The compose service restart preserves both container identities and PostgreSQL storage.
  const backendAfter = serviceSnapshot("backend");
  assert.equal(backendAfter.id, backendBefore.id);
  assert.notEqual(backendAfter.startedAt, backendBefore.startedAt);
  assert.deepEqual(serviceSnapshot("postgres"), postgresBefore);
  const byId = await application.get(
    `/api/v1/work-session-changes/${pauseReceipt.id}`,
  );
  assert.equal(byId.status(), 200);
  assert.equal(byId.headers().location, undefined);
  assert.deepEqual(await byId.json(), pauseReceipt);
  const currentStateResponse = await application.get(sessionStatePath);
  assert.equal(currentStateResponse.status(), 200);
  assert.deepEqual(
    (await currentStateResponse.json()).state,
    resumeReceipt.after,
  );
  assert.deepEqual(
    await (await application.get(workLookup)).json(),
    recoveredStart,
  );
  assert.deepEqual(transitionCounts(), countsBeforeRecovery);
  console.log(
    "PASS pause_resume_session @s25/@s26: lost PAUSE ACK, later RESUME, two exact persistent events, real backend restart and immutable C/K recovery without outbox",
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
