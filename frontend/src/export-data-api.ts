import { apiRequest } from "./api-client";
import { exact, instant } from "./schedule-block-api";

const collections =
  "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues";

export async function readExportData(owner: string, signal: AbortSignal) {
  signal.throwIfAborted();
  const response = await apiRequest("/api/v1/me/export", {
    signal,
    headers: { Accept: "application/json" },
  });
  signal.throwIfAborted();
  if (response.status !== 200) throw response;
  try {
    if (
      !/^application\/json\s*;\s*charset=utf-8$/i.test(
        response.headers.get("Content-Type") ?? "",
      )
    )
      throw new Error("Archivo de exportación incompatible.");
    const encoding = response.headers.get("Content-Encoding");
    if (encoding !== null && encoding.toLowerCase() !== "identity")
      throw new Error("Archivo de exportación incompatible.");
    const disposition = response.headers.get("Content-Disposition") ?? "";
    const name =
      /^attachment; filename="(organizationweb-export-v1-[0-9]{8}T[0-9]{12}Z\.json)"$/.exec(
        disposition,
      );
    if (!name || name[0] !== disposition)
      throw new Error("Archivo de exportación incompatible.");
    const declared = response.headers.get("Content-Length");
    const length = Number(declared);
    if (!declared || !/^[1-9][0-9]*$/.test(declared) || length > 33554432)
      throw new Error("Archivo de exportación incompatible.");
    const bytes = new Uint8Array(length);
    const reader = response.body!.getReader();
    const cancel = () => {
      void reader.cancel().catch(() => {});
    };
    signal.addEventListener("abort", cancel, { once: true });
    let offset = 0;
    try {
      while (true) {
        const next = await reader.read();
        signal.throwIfAborted();
        if (next.done) break;
        if (offset + next.value.byteLength > length)
          throw new Error("Archivo de exportación incompatible.");
        bytes.set(next.value, offset);
        offset += next.value.byteLength;
      }
    } catch (error) {
      await reader.cancel().catch(() => {});
      throw error;
    } finally {
      signal.removeEventListener("abort", cancel);
      reader.releaseLock();
    }
    if (offset !== length)
      throw new Error("Archivo de exportación incompatible.");
    const document: unknown = JSON.parse(
      new TextDecoder("utf-8", { fatal: true, ignoreBOM: true }).decode(bytes),
    );
    if (
      !exact(document, "format schemaVersion exportedAt owner data counts") ||
      document.owner !== owner ||
      document.format !== "organizationweb-export" ||
      document.schemaVersion !== 1 ||
      !exact(document.data, collections)
    )
      throw new Error("Archivo de exportación incompatible.");
    if (!exact(document.counts, collections))
      throw new Error("Archivo de exportación incompatible.");
    if (
      !instant(document.exportedAt) ||
      !/\.\d{6}Z$/.test(document.exportedAt) ||
      name[1] !==
        `organizationweb-export-v1-${document.exportedAt.replace(/[-:.]/g, "")}.json`
    )
      throw new Error("Archivo de exportación incompatible.");
    for (const name of collections.split(" ")) {
      const items = document.data[name];
      if (!Array.isArray(items) || document.counts[name] !== items.length)
        throw new Error("Archivo de exportación incompatible.");
    }
    return {
      bytes,
      fileName: name[1],
      exportedAt: document.exportedAt as string,
    };
  } catch (error) {
    await response.body?.cancel().catch(() => {});
    throw error;
  }
}
