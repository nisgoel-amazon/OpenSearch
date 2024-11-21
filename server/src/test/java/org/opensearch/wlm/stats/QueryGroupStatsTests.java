/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.wlm.stats;

import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.test.AbstractWireSerializingTestCase;
import org.opensearch.wlm.ResourceType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QueryGroupStatsTests extends AbstractWireSerializingTestCase<QueryGroupStats> {

    public void testToXContent() throws IOException {
        final Map<String, QueryGroupStats.QueryGroupStatsHolder> stats = new HashMap<>();
        final String queryGroupId = "afakjklaj304041-afaka";
        stats.put(
            queryGroupId,
            new QueryGroupStats.QueryGroupStatsHolder(
                123456789,
                2,
                0,
                13,
                Map.of(ResourceType.CPU, new QueryGroupStats.ResourceStats(0.3, 13, 2))
            )
        );
        XContentBuilder builder = JsonXContent.contentBuilder();
        QueryGroupStats queryGroupStats = new QueryGroupStats(stats);
        builder.startObject();
        queryGroupStats.toXContent(builder, ToXContent.EMPTY_PARAMS);
        builder.endObject();
        assertEquals(
            "{\"query_groups\":{\"afakjklaj304041-afaka\":{\"completions\":123456789,\"rejections\":2,\"failures\":0,\"total_cancellations\":13,\"cpu\":{\"current_usage\":0.3,\"cancellations\":13,\"rejections\":2}}}}",
            builder.toString()
        );
    }

    @Override
    protected Writeable.Reader<QueryGroupStats> instanceReader() {
        return QueryGroupStats::new;
    }

    @Override
    protected QueryGroupStats createTestInstance() {
        Map<String, QueryGroupStats.QueryGroupStatsHolder> stats = new HashMap<>();
        stats.put(
            randomAlphaOfLength(10),
            new QueryGroupStats.QueryGroupStatsHolder(
                randomNonNegativeLong(),
                randomNonNegativeLong(),
                randomNonNegativeLong(),
                randomNonNegativeLong(),
                Map.of(
                    ResourceType.CPU,
                    new QueryGroupStats.ResourceStats(
                        randomDoubleBetween(0.0, 0.90, false),
                        randomNonNegativeLong(),
                        randomNonNegativeLong()
                    )
                )
            )
        );
        return new QueryGroupStats(stats);
    }
}
