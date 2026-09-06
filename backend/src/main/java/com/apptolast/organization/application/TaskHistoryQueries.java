package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.*;

public interface TaskHistoryQueries {
  List<TaskHistoryEntry> list(String owner, UUID project, UUID task, TaskHistoryPosition after);
}
