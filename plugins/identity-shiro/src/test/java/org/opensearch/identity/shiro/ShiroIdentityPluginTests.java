/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.shiro;

import org.opensearch.OpenSearchException;
import org.opensearch.common.settings.Settings;
import org.opensearch.identity.IdentityService;
import org.opensearch.plugins.IdentityPlugin;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.TestThreadPool;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

public class ShiroIdentityPluginTests extends OpenSearchTestCase {

    public void testSingleIdentityPluginSucceeds() {
        TestThreadPool threadPool = new TestThreadPool(getTestName());
        IdentityPlugin identityPlugin1 = new ShiroIdentityPlugin(Settings.EMPTY);
        List<IdentityPlugin> pluginList1 = List.of(identityPlugin1);
        IdentityService identityService1 = new IdentityService(Settings.EMPTY, threadPool, pluginList1);
        assertThat(identityService1.getTokenManager(), is(instanceOf(ShiroTokenManager.class)));
        terminate(threadPool);
    }

    public void testMultipleIdentityPluginsFail() {
        TestThreadPool threadPool = new TestThreadPool(getTestName());
        IdentityPlugin identityPlugin1 = new ShiroIdentityPlugin(Settings.EMPTY);
        IdentityPlugin identityPlugin2 = new ShiroIdentityPlugin(Settings.EMPTY);
        IdentityPlugin identityPlugin3 = new ShiroIdentityPlugin(Settings.EMPTY);
        List<IdentityPlugin> pluginList = List.of(identityPlugin1, identityPlugin2, identityPlugin3);
        Exception ex = assertThrows(OpenSearchException.class, () -> new IdentityService(Settings.EMPTY, threadPool, pluginList));
        assert (ex.getMessage().contains("Multiple identity plugins are not supported,"));
        terminate(threadPool);
    }

}
