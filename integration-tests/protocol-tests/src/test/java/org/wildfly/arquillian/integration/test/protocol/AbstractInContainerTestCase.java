/*
 * Copyright 2022 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.arquillian.integration.test.protocol;

import java.security.AccessController;
import java.security.PrivilegedAction;

import jakarta.inject.Inject;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ServerSetup(AbstractInContainerTestCase.SystemPropertyServerSetupTask.class)
abstract class AbstractInContainerTestCase {

    @Inject
    private Protocol protocol;

    @Test
    public void testInjection() {
        Assertions.assertNotNull(protocol);
        Assertions.assertEquals(getExpectedProtocol(), protocol.getProtocol());
    }

    private static String getExpectedProtocol() {
        if (System.getSecurityManager() == null) {
            return System.getProperty("arq.protocol", "");
        }
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("arq.protocol", ""));
    }

    public static class SystemPropertyServerSetupTask implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode op = Operations.createAddOperation(Operations.createAddress("system-property", "arq.protocol"));
            op.get(ClientConstants.VALUE).set(System.getProperty("arq.protocol", ""));
            final ModelNode result = managementClient.getControllerClient().execute(op);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new RuntimeException("Failed to configure properties: " + Operations.getFailureDescription(result)
                        .asString());
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode result = managementClient.getControllerClient()
                    .execute(Operations.createRemoveOperation(Operations.createAddress("system-property", "arq.protocol")));
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new RuntimeException("Failed to configure properties: " + Operations.getFailureDescription(result)
                        .asString());
            }
        }
    }
}
