/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/

package org.osgi.test.cases.clusterinfo.secure.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.service.clusterinfo.FrameworkManager;
import org.osgi.service.clusterinfo.NodeStatus;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class ClusterInfoSecurityTestCase {

	@InjectBundleContext
	BundleContext		context;

	@Test
	public void testPermissionDenied() throws Exception {
		BundleContext ctx = context;

		ServiceTracker<NodeStatus,NodeStatus> st = new ServiceTracker<NodeStatus,NodeStatus>(
				ctx,
				ctx.createFilter("(&(objectClass=" + NodeStatus.class.getName()
						+ ")(osgi.clusterinfo.tags=foo))"),
				null);
		st.open();

		Collection<ServiceReference<FrameworkManager>> srefs = ctx
				.getServiceReferences(FrameworkManager.class,
						"(objectClass=" + NodeStatus.class.getName() + ")");
		assertThat(srefs).hasSize(1);
		FrameworkManager fm = ctx.getService(srefs.iterator().next());

		URL tbURL = getClass().getClassLoader().getResource("tb1.jar");
		BundleDTO dto = fm.installBundle(tbURL.toString());

		try {
			fm.startBundle(dto.id);
			assertThat(st.getService()).as("Bundle should not have permission to add tag").isNull();
		} finally {
			fm.uninstallBundle(dto.id);
		}
	}

	@Test
	public void testPermissionOk() throws Exception {
		BundleContext ctx = context;

		ServiceTracker<NodeStatus,NodeStatus> st = new ServiceTracker<NodeStatus,NodeStatus>(
				ctx,
				ctx.createFilter("(&(objectClass=" + NodeStatus.class.getName()
						+ ")(osgi.clusterinfo.tags=foo))"),
				null);
		st.open();

		Collection<ServiceReference<FrameworkManager>> srefs = ctx
				.getServiceReferences(FrameworkManager.class,
						"(objectClass=" + NodeStatus.class.getName() + ")");
		assertThat(srefs).hasSize(1);
		FrameworkManager fm = ctx.getService(srefs.iterator().next());

		assertThat(st.getService()).as("Precondition").isNull();

		URL tbURL = getClass().getClassLoader().getResource("tb2.jar");
		BundleDTO dto = fm.installBundle(tbURL.toString());

		try {
			fm.startBundle(dto.id);

			assertThat(st.waitForService(5000)).as("The service with added custom properties should be there now").isNotNull();
		} finally {
			fm.uninstallBundle(dto.id);
		}
	}
}


