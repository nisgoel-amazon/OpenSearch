/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.wlm.tracker;

import org.opensearch.action.search.SearchShardTask;
import org.opensearch.action.search.SearchTask;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.tasks.resourcetracker.ResourceStats;
import org.opensearch.tasks.CancellableTask;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskResourceTrackingService;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.TestThreadPool;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.wlm.QueryGroupLevelResourceUsageView;
import org.opensearch.wlm.QueryGroupTask;
import org.opensearch.wlm.ResourceType;
import org.junit.After;
import org.junit.Before;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.opensearch.wlm.QueryGroupTask.QUERY_GROUP_ID_HEADER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueryGroupResourceUsageTrackerServiceTests extends OpenSearchTestCase {
    TestThreadPool threadPool;
    TaskResourceTrackingService mockTaskResourceTrackingService;
    QueryGroupResourceUsageTrackerService queryGroupResourceUsageTrackerService;

    @Before
    public void setup() {
        threadPool = new TestThreadPool(getTestName());
        mockTaskResourceTrackingService = mock(TaskResourceTrackingService.class);
        queryGroupResourceUsageTrackerService = new QueryGroupResourceUsageTrackerService(mockTaskResourceTrackingService);
    }

    @After
    public void cleanup() {
        ThreadPool.terminate(threadPool, 5, TimeUnit.SECONDS);
    }

    public void testConstructQueryGroupLevelViews_CreatesQueryGroupLevelUsageView_WhenTasksArePresent() {
        List<String> queryGroupIds = List.of("queryGroup1", "queryGroup2", "queryGroup3");

        Map<Long, Task> activeSearchShardTasks = createActiveSearchShardTasks(queryGroupIds);
        when(mockTaskResourceTrackingService.getResourceAwareTasks()).thenReturn(activeSearchShardTasks);
        Map<String, QueryGroupLevelResourceUsageView> stringQueryGroupLevelResourceUsageViewMap = queryGroupResourceUsageTrackerService
            .constructQueryGroupLevelUsageViews();

        for (String queryGroupId : queryGroupIds) {
            assertEquals(
                400,
                (long) stringQueryGroupLevelResourceUsageViewMap.get(queryGroupId).getResourceUsageData().get(ResourceType.MEMORY)
            );
            assertEquals(2, stringQueryGroupLevelResourceUsageViewMap.get(queryGroupId).getActiveTasks().size());
        }
    }

    public void testConstructQueryGroupLevelViews_CreatesQueryGroupLevelUsageView_WhenTasksAreNotPresent() {
        Map<String, QueryGroupLevelResourceUsageView> stringQueryGroupLevelResourceUsageViewMap = queryGroupResourceUsageTrackerService
            .constructQueryGroupLevelUsageViews();
        assertTrue(stringQueryGroupLevelResourceUsageViewMap.isEmpty());
    }

    public void testConstructQueryGroupLevelUsageViews_WithTasksHavingDifferentResourceUsage() {
        Map<Long, Task> activeSearchShardTasks = new HashMap<>();
        activeSearchShardTasks.put(1L, createMockTask(SearchShardTask.class, 100, 200, "queryGroup1"));
        activeSearchShardTasks.put(2L, createMockTask(SearchShardTask.class, 200, 400, "queryGroup1"));
        when(mockTaskResourceTrackingService.getResourceAwareTasks()).thenReturn(activeSearchShardTasks);

        Map<String, QueryGroupLevelResourceUsageView> queryGroupViews = queryGroupResourceUsageTrackerService
            .constructQueryGroupLevelUsageViews();

        assertEquals(600, (long) queryGroupViews.get("queryGroup1").getResourceUsageData().get(ResourceType.MEMORY));
        assertEquals(2, queryGroupViews.get("queryGroup1").getActiveTasks().size());
    }

    private Map<Long, Task> createActiveSearchShardTasks(List<String> queryGroupIds) {
        Map<Long, Task> activeSearchShardTasks = new HashMap<>();
        long task_id = 0;
        for (String queryGroupId : queryGroupIds) {
            for (int i = 0; i < 2; i++) {
                activeSearchShardTasks.put(++task_id, createMockTask(SearchShardTask.class, 100, 200, queryGroupId));
            }
        }
        return activeSearchShardTasks;
    }

    private <T extends CancellableTask> T createMockTask(Class<T> type, long cpuUsage, long heapUsage, String queryGroupId) {
        T task = mock(type);
        if (task instanceof SearchTask || task instanceof SearchShardTask) {
            // Stash the current thread context to ensure that any existing context is preserved and restored after setting the query group
            // ID.
            try (ThreadContext.StoredContext ignore = threadPool.getThreadContext().stashContext()) {
                threadPool.getThreadContext().putHeader(QUERY_GROUP_ID_HEADER, queryGroupId);
                ((QueryGroupTask) task).setQueryGroupId(threadPool.getThreadContext());
            }
        }
        when(task.getTotalResourceUtilization(ResourceStats.CPU)).thenReturn(cpuUsage);
        when(task.getTotalResourceUtilization(ResourceStats.MEMORY)).thenReturn(heapUsage);
        when(task.getStartTimeNanos()).thenReturn((long) 0);

        AtomicBoolean isCancelled = new AtomicBoolean(false);
        doAnswer(invocation -> {
            isCancelled.set(true);
            return null;
        }).when(task).cancel(anyString());
        doAnswer(invocation -> isCancelled.get()).when(task).isCancelled();

        return task;
    }
}
