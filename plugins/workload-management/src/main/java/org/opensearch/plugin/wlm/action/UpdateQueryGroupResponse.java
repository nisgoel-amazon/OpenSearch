/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.wlm.action;

import org.opensearch.cluster.metadata.QueryGroup;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;

/**
 * Response for the update API for QueryGroup
 *
 * @opensearch.experimental
 */
public class UpdateQueryGroupResponse extends ActionResponse implements ToXContent, ToXContentObject {
    private final QueryGroup queryGroup;
    private final RestStatus restStatus;

    /**
     * Constructor for UpdateQueryGroupResponse
     * @param queryGroup - the QueryGroup to be updated
     * @param restStatus - the rest status for the response
     */
    public UpdateQueryGroupResponse(final QueryGroup queryGroup, RestStatus restStatus) {
        this.queryGroup = queryGroup;
        this.restStatus = restStatus;
    }

    /**
     * Constructor for UpdateQueryGroupResponse
     * @param in - a {@link StreamInput} object
     */
    public UpdateQueryGroupResponse(StreamInput in) throws IOException {
        queryGroup = new QueryGroup(in);
        restStatus = RestStatus.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        queryGroup.writeTo(out);
        RestStatus.writeTo(out, restStatus);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return queryGroup.toXContent(builder, params);
    }

    /**
     * queryGroup getter
     */
    public QueryGroup getQueryGroup() {
        return queryGroup;
    }

    /**
     * restStatus getter
     */
    public RestStatus getRestStatus() {
        return restStatus;
    }
}