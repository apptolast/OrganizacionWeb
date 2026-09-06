export const emptyToday = () => ({
  serverNow: "2030-01-07T12:00:00Z",
  date: "2030-01-07",
  zoneId: "UTC",
  zoneSource: "AVAILABILITY",
  availabilityZoneId: "UTC",
  dayStartAt: "2030-01-07T00:00:00Z",
  dayEndAt: "2030-01-08T00:00:00Z",
  budgetMinutes: 120,
  plannedSeconds: 0,
  remainingSeconds: 7200,
  excessSeconds: 0,
  currentBlockId: null,
  nextBlockId: null,
  closingAt: null,
  items: [],
});
export const agendaToday = () => {
  const block = {
    id: "00000000-0000-0000-0000-000000000001",
    projectId: "00000000-0000-0000-0000-000000000002",
    taskId: "00000000-0000-0000-0000-000000000003",
    objective: "Preparar borrador",
    startAt: "2030-01-07T12:00:00Z",
    endAt: "2030-01-07T13:00:00Z",
    zoneId: "Historical/Unknown",
    durationMinutes: 60,
    createdAt: "2030-01-07T12:00:01Z",
  };
  return {
    ...emptyToday(),
    plannedSeconds: 3600,
    remainingSeconds: 3600,
    currentBlockId: block.id,
    closingAt: block.endAt,
    items: [{ block, projectName: "Proyecto personal", taskTitle: "Escribir" }],
  };
};
