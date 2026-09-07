import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { randomUUID, createHash } from "node:crypto";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";

const root = fileURLToPath(new URL("../", import.meta.url));
const prefix = `owdns-${randomUUID()}`;
const image = `${prefix}:test`;
const directory = resolve(root, ".build", "web-dns", prefix);
mkdirSync(directory, { recursive: true });
const report = {
  prefix,
  startedAt: new Date().toISOString(),
  stages: [],
  cleanup: [],
  configSha256: createHash("sha256")
    .update(readFileSync(resolve(root, "deploy/nginx.conf")))
    .digest("hex"),
};
const containers = new Set();
let networkCreated = false;
let imageCreated = false;
function docker(args, options = {}) {
  const result = spawnSync("docker", args, {
    cwd: root,
    encoding: "utf8",
    maxBuffer: 16 * 1024 * 1024,
    ...options,
  });
  if (result.error || result.status !== 0)
    throw new Error(
      `Docker ${args[0]} failed: ${result.stderr || result.error?.message}`,
    );
  return result.stdout.trim();
}
function stage(value) {
  report.stages.push(value);
  console.log(`PASS ${value}`);
}
async function wait(label, operation, milliseconds = 60000) {
  const end = Date.now() + milliseconds;
  let failure;
  while (Date.now() < end) {
    try {
      const value = await operation();
      if (value) return value;
    } catch (error) {
      failure = error;
    }
    await new Promise((resolve) => setTimeout(resolve, 200));
  }
  throw new Error(
    `Timed out: ${label}${failure ? ` (${failure.message})` : ""}`,
  );
}
function inspect(name) {
  return JSON.parse(docker(["inspect", name]))[0];
}
function run(name, args) {
  docker(["run", "-d", "--name", name, ...args]);
  containers.add(name);
}
function remove(name) {
  docker(["rm", "-f", name]);
  containers.delete(name);
}
const fixture = `const http=require('node:http');let posts=0;http.createServer(async(req,res)=>{let body='';for await(const part of req)body+=part;if(req.method==='POST')posts++;res.setHeader('Content-Type','application/json');res.end(JSON.stringify({marker:process.argv[1],method:req.method,url:req.url,headers:req.headers,body,posts}));}).listen(8080,'0.0.0.0');`;
function startBackend(marker) {
  run(`${prefix}-backend`, [
    "--network",
    prefix,
    "--network-alias",
    "backend",
    "--user",
    "1000:1000",
    "--read-only",
    "--cap-drop",
    "ALL",
    "node:22.23.2-alpine3.23",
    "node",
    "-e",
    fixture,
    marker,
  ]);
}
try {
  const build = spawnSync(
    "docker",
    ["build", "-f", "deploy/web.Dockerfile", "-t", image, "."],
    { cwd: root, encoding: "utf8", maxBuffer: 32 * 1024 * 1024 },
  );
  writeFileSync(
    resolve(directory, "build.log"),
    `${build.stdout || ""}\n${build.stderr || ""}`,
  );
  assert.equal(build.status, 0, "Production web Dockerfile must build");
  imageCreated = true;
  report.imageId = docker(["image", "inspect", image, "--format", "{{.Id}}"]);
  docker(["network", "create", prefix]);
  networkCreated = true;
  const web = `${prefix}-web`;
  run(web, [
    "--network",
    prefix,
    "--user",
    "101:101",
    "--read-only",
    "--cap-drop",
    "ALL",
    "--mount",
    "type=tmpfs,destination=/run,tmpfs-size=16777216",
    "--mount",
    "type=tmpfs,destination=/var/cache/nginx,tmpfs-size=16777216",
    "-p",
    "127.0.0.1::8080",
    image,
  ]);
  const port = inspect(web).NetworkSettings.Ports["8080/tcp"][0].HostPort;
  assert.ok(
    !["8080", "18080"].includes(port),
    "Use an isolated ephemeral host port",
  );
  const origin = `http://127.0.0.1:${port}`;
  const request = (path, options) =>
    fetch(origin + path, { ...options, signal: AbortSignal.timeout(3000) });
  await wait(
    "web healthy without backend DNS",
    async () => {
      const state = inspect(web).State;
      if (!state.Running)
        throw new Error(`Web exited ${state.ExitCode} before backend exists`);
      return (await request("/healthz")).status === 204;
    },
    10000,
  );
  const original = inspect(web);
  report.webIdentity = {
    id: original.Id,
    startedAt: original.State.StartedAt,
    pid: original.State.Pid,
    restartCount: original.RestartCount,
  };
  const html = await request("/");
  assert.equal(html.status, 200);
  assert.match(await html.text(), /<html/i);
  const absent = await request("/api/session");
  assert.equal(absent.status, 502);
  stage("web remains available without backend; health204, static200, API502");
  startBackend("first");
  async function backendResponse(marker) {
    return wait(`DNS recovers to ${marker}`, async () => {
      const response = await request("/api/probe?x=1");
      if (response.status !== 200) return false;
      const value = await response.json();
      return value.marker === marker ? value : false;
    });
  }
  const first = await backendResponse("first");
  assert.equal(first.url, "/api/probe?x=1");
  const post = await request("/api/probe?x=1", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Cookie: "synthetic=fixture",
      "X-CSRF-TOKEN": "synthetic-csrf",
    },
    body: '{"exact":true}',
  });
  assert.equal(post.status, 200);
  const echoed = await post.json();
  assert.equal(echoed.url, "/api/probe?x=1");
  assert.equal(echoed.method, "POST");
  assert.equal(echoed.body, '{"exact":true}');
  assert.equal(echoed.posts, 1);
  assert.equal(echoed.headers.cookie, "synthetic=fixture");
  assert.equal(echoed.headers["x-csrf-token"], "synthetic-csrf");
  assert.equal(echoed.headers.host, `127.0.0.1:${port}`);
  assert.equal(echoed.headers["x-forwarded-proto"], "http");
  assert.ok(echoed.headers["x-forwarded-for"]);
  stage(
    "backend appearing later receives intact URI/query/body/headers and exactly one POST",
  );
  const firstIp = inspect(`${prefix}-backend`).NetworkSettings.Networks[prefix]
    .IPAddress;
  remove(`${prefix}-backend`);
  run(`${prefix}-reserve`, [
    "--network",
    prefix,
    "--read-only",
    "--cap-drop",
    "ALL",
    "node:22.23.2-alpine3.23",
    "node",
    "-e",
    "setInterval(()=>{},1000)",
  ]);
  startBackend("second");
  const secondIp = inspect(`${prefix}-backend`).NetworkSettings.Networks[prefix]
    .IPAddress;
  assert.notEqual(secondIp, firstIp);
  report.addresses = { firstIp, secondIp };
  await backendResponse("second");
  const final = inspect(web);
  assert.deepEqual(
    {
      id: final.Id,
      startedAt: final.State.StartedAt,
      pid: final.State.Pid,
      restartCount: final.RestartCount,
    },
    report.webIdentity,
  );
  stage(
    "backend IP replacement recovers without restarting the web container or master",
  );
  report.status = "PASS";
} catch (error) {
  report.status = "FAIL";
  report.failure = error.message;
  console.error(`FAIL ${error.message}`);
  process.exitCode = 1;
} finally {
  for (const name of [...containers].reverse()) {
    assert.ok(name.startsWith(prefix));
    if (name.endsWith("-web")) {
      const logs = spawnSync("docker", ["logs", name], { encoding: "utf8" });
      writeFileSync(
        resolve(directory, "web.log"),
        `${logs.stdout || ""}\n${logs.stderr || ""}`,
      );
    }
    try {
      remove(name);
      report.cleanup.push(name);
    } catch {
      report.cleanup.push(`FAILED:${name}`);
      report.status = "FAIL";
      process.exitCode = 1;
    }
  }
  if (networkCreated) {
    try {
      docker(["network", "rm", prefix]);
      report.cleanup.push(prefix);
    } catch {
      report.cleanup.push(`FAILED:${prefix}`);
      report.status = "FAIL";
      process.exitCode = 1;
    }
  }
  if (imageCreated) {
    try {
      docker(["image", "rm", image]);
      report.cleanup.push(image);
    } catch {
      report.cleanup.push(`FAILED:${image}`);
      report.status = "FAIL";
      process.exitCode = 1;
    }
  }
  report.finishedAt = new Date().toISOString();
  writeFileSync(
    resolve(directory, "result.json"),
    `${JSON.stringify(report, null, 2)}\n`,
  );
  console.log(`Evidence: ${directory}`);
}
