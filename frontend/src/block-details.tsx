import type { Block } from "./schedule-block-api";
export function BlockDetails({ block }: { block: Block }) {
  return (
    <>
      <p>
        Inicio: <BlockTime value={block.startAt} zoneId={block.zoneId} />
      </p>
      <p>
        Fin: <BlockTime value={block.endAt} zoneId={block.zoneId} />
      </p>
      <p>{block.durationMinutes} minutos planificados</p>
    </>
  );
}
export function BlockTime({
  value,
  zoneId,
}: {
  value: string;
  zoneId: string;
}) {
  let label: string;
  try {
    label = new Intl.DateTimeFormat("es", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      timeZone: zoneId,
      timeZoneName: "longOffset",
    }).format(new Date(value));
  } catch {
    label = `${value} UTC`;
  }
  return (
    <time dateTime={value}>
      {label} · {zoneId}
    </time>
  );
}
