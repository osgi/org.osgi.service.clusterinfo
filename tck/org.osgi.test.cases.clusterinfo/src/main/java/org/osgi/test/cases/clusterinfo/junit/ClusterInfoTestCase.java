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

package org.osgi.test.cases.clusterinfo.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.service.clusterinfo.FrameworkManager;
import org.osgi.service.clusterinfo.FrameworkNodeStatus;
import org.osgi.service.clusterinfo.NodeStatus;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class ClusterInfoTestCase {

	@InjectBundleContext
	BundleContext		context;
	@Test
	public void testFrameworkNodeStatusProperties() {
		BundleContext ctx = context;
		ServiceReference<FrameworkNodeStatus> sref = ctx
				.getServiceReference(FrameworkNodeStatus.class);
		Map<String,Object> props = new HashMap<>();

		for (String key : sref.getPropertyKeys()) {
			props.put(key, sref.getProperty(key));
		}

		assertThat(props.get("osgi.clusterinfo.id")).isNotNull();
		assertThat(props.get("osgi.clusterinfo.cluster")).isNotNull();

		assertThat(props.get("org.osgi.framework.version")).isEqualTo(ctx.getProperty("org.osgi.framework.version"));
		assertThat(props.get("org.osgi.framework.processor")).isEqualTo(ctx.getProperty("org.osgi.framework.processor"));
		assertThat(props.get("org.osgi.framework.os.name")).isEqualTo(ctx.getProperty("org.osgi.framework.os.name"));
		assertThat(props.get("org.osgi.framework.os.version")).isEqualTo(ctx.getProperty("org.osgi.framework.os.version"));

		assertThat(props.get("java.version")).isEqualTo(ctx.getProperty("java.version"));
		assertThat(props.get("java.runtime.version")).isEqualTo(ctx.getProperty("java.runtime.version"));
		assertThat(props.get("java.specification.version")).isEqualTo(ctx.getProperty("java.specification.version"));
		assertThat(props.get("java.vm.version")).isEqualTo(ctx.getProperty("java.vm.version"));
    }

	@Test
	public void testFrameworkManager() throws Exception {
		BundleContext ctx = context;
		Collection<ServiceReference<FrameworkManager>> srefs = ctx
				.getServiceReferences(
				FrameworkManager.class,
				"(objectClass=" + FrameworkNodeStatus.class.getName() + ")");
		assertThat(srefs).isNotNull();

		assertThat(srefs).as("One framework manager expected").hasSize(1);
		FrameworkManager fm = ctx.getService(srefs.iterator().next());

		URL tb1URL = getClass().getClassLoader().getResource("tb1.jar");
		BundleDTO dto = fm.installBundle(tb1URL.toString());
		BundleDTO dto2 = fm.getBundle(dto.id);
		assertBundleDTOEquals(dto, dto2);

		assertThat(fm.getBundleHeaders(dto.id).get("Custom-Header").trim()).isEqualTo("A custom header");
		boolean foundDTO = false;
		for (BundleDTO bdto : fm.getBundles()) {
			if (bdto.id == dto.id) {
				assertBundleDTOEquals(bdto, dto);
				foundDTO = true;
			}
		}
		assertThat(foundDTO).isTrue();

		assertThat(fm.getServiceReferences(
				"(&(objectClass=java.lang.String)(testbundle=tb1))")).as("Precondition").isEmpty();
		fm.startBundle(dto.id);
		
		Collection<ServiceReferenceDTO> bsrefs = fm.getServiceReferences(
				"(&(objectClass=java.lang.String)(testbundle=tb1))");
		assertThat(bsrefs.size()).isEqualTo(1);
		ServiceReferenceDTO sdto = bsrefs.iterator().next();
		assertThat(sdto.bundle).isEqualTo(dto.id);
		assertThat(sdto.properties.get("some.property")).isEqualTo("some value");

		assertServiceReferenceDTOEquals(sdto, fm.getServiceReference(sdto.id));

		boolean foundService = false;
		for (ServiceReferenceDTO srdto : fm.getServiceReferences()) {
			if (srdto.id == sdto.id) {
				assertServiceReferenceDTOEquals(sdto, srdto);
				foundService = true;
			}
		}
		assertThat(foundService).isTrue();

		assertThat(fm.getServiceReferences(
				"(&(objectClass=java.lang.String)(testbundle=tb2))")).as("Precondition").isEmpty();

		URL tb2URL = getClass().getClassLoader().getResource("tb2.jar");
		BundleDTO udto = fm.updateBundle(dto.id, tb2URL.toString());

		assertThat(fm.getServiceReferences(
						"(&(objectClass=java.lang.String)(testbundle=tb1))")
						.size()).as("Service from pre-update bundle should be gone").isEqualTo(0);
		Collection<ServiceReferenceDTO> usrefs = fm.getServiceReferences(
				"(&(objectClass=java.lang.String)(testbundle=tb2))");
		assertThat(usrefs.size()).isEqualTo(1);
		ServiceReferenceDTO usdto = usrefs.iterator().next();
		assertThat(udto.id).isEqualTo(usdto.bundle);
		assertThat(usdto.properties.get("some.property")).isEqualTo("some different value");
		
		fm.stopBundle(udto.id);
		assertThat(fm.getServiceReferences(
						"(&(objectClass=java.lang.String)(testbundle=tb2))")
						.size()).as("TB2 registered service should be gone").isEqualTo(0);
		assertThat(fm.getBundleState(udto.id)).isEqualTo(Bundle.RESOLVED);

		int sizeBeforeUninstall = fm.getBundles().size();
		BundleDTO undto = fm.uninstallBundle(udto.id);
		assertThat(udto.id).isEqualTo(undto.id);
		assertThat(undto.state).isEqualTo(Bundle.UNINSTALLED);
		assertThat(fm.getBundles()).hasSize(sizeBeforeUninstall - 1);
	}

	@Test
	public void testFrameworkManager2() throws Exception {
		BundleContext ctx = context;
		Collection<ServiceReference<FrameworkManager>> srefs = ctx
				.getServiceReferences(FrameworkManager.class, "(objectClass="
						+ FrameworkNodeStatus.class.getName() + ")");
		assertThat(srefs).isNotNull();

		assertThat(srefs).as("One framework manager expected").hasSize(1);
		FrameworkManager fm = ctx.getService(srefs.iterator().next());

		URL tb4URL = getClass().getClassLoader().getResource("tb4.jar");
		BundleDTO dto = fm.installBundle(tb4URL.toString());

		fm.setBundleStartLevel(dto.id, 123);
		assertThat(fm.getBundleStartLevel(dto.id).startLevel).isEqualTo(123);
		
		fm.uninstallBundle(dto.id);
	}

	@Test
	public void testFrameworkManagerFrameworkStartLevel() throws Exception {
		BundleContext ctx = context;
		Collection<ServiceReference<FrameworkManager>> srefs = ctx
				.getServiceReferences(FrameworkManager.class, "(objectClass="
						+ FrameworkNodeStatus.class.getName() + ")");
		assertThat(srefs).isNotNull();

		assertThat(srefs).as("One framework manager expected").hasSize(1);
		FrameworkManager fm = ctx.getService(srefs.iterator().next());

		FrameworkStartLevel fwsl = ctx.getBundle(0)
				.adapt(FrameworkStartLevel.class);
		FrameworkStartLevelDTO fwsldto = fm.getFrameworkStartLevel();

		assertThat(fwsldto.initialBundleStartLevel).isEqualTo(fwsl.getInitialBundleStartLevel());
		assertThat(fwsldto.startLevel).isEqualTo(fwsl.getStartLevel());

		try {
			FrameworkStartLevelDTO fwsldto2 = new FrameworkStartLevelDTO();
			fwsldto2.initialBundleStartLevel = fwsldto.initialBundleStartLevel;
			fwsldto2.startLevel = 99;

			fm.setFrameworkStartLevel(fwsldto2);
			assertThat(fwsldto2.initialBundleStartLevel).isEqualTo(fwsl.getInitialBundleStartLevel());
			assertThat(fwsldto2.startLevel).isEqualTo(99);
		} finally {
			fm.setFrameworkStartLevel(fwsldto);
		}
	}

	@Test
	public void testMetrics() {
		BundleContext ctx = context;
		ServiceReference<NodeStatus> sref = ctx
				.getServiceReference(NodeStatus.class);
		NodeStatus ns = ctx.getService(sref);
		Map<String,Object> metrics = ns.getMetrics();
		assertThat(metrics.size() > 0).as("There should be some metrics").isTrue();
		
		String metricKey = metrics.keySet().iterator().next();
		Map<String,Object> metrics2 = ns.getMetrics(metricKey);
		assertThat(metrics2.size()).isEqualTo(1);
		assertThat(metrics2.get(metricKey)).isEqualTo(metrics.get(metricKey));

	}

	@Test
	public void testCustomTags() throws Exception {
		BundleContext ctx = context;

		ServiceTracker<NodeStatus,NodeStatus> st = new ServiceTracker<NodeStatus,NodeStatus>(
				ctx, ctx.createFilter("(&(objectClass="
						+ NodeStatus.class.getName()
						+ ")(osgi.clusterinfo.tags=foo))"),
				null);
		st.open();

		Collection<ServiceReference<FrameworkManager>> srefs = ctx
				.getServiceReferences(FrameworkManager.class,
						"(objectClass=" + NodeStatus.class.getName() + ")");
		assertThat(srefs).hasSize(1);
		FrameworkManager fm = ctx.getService(srefs.iterator().next());

		assertThat(st.getService()).as("Precondition").isNull();

		URL tb3URL = getClass().getClassLoader().getResource("tb3.jar");
		BundleDTO dto = fm.installBundle(tb3URL.toString());
		fm.startBundle(dto.id);

		assertThat(st.waitForService(5000)).as("The service with added custom properties should be there now").isNotNull();
	}

	private void assertBundleDTOEquals(BundleDTO dto, BundleDTO dto2) {
		assertThat(dto2.id).isEqualTo(dto.id);
		assertThat(dto2.lastModified).isEqualTo(dto.lastModified);
		assertThat(dto2.state).isEqualTo(dto.state);
		assertThat(dto2.symbolicName).isEqualTo(dto.symbolicName);
		assertThat(dto2.version).isEqualTo(dto.version);
	}

	private void assertServiceReferenceDTOEquals(ServiceReferenceDTO dto,
			ServiceReferenceDTO dto2) {
		assertThat(dto2.id).isEqualTo(dto.id);
		assertThat(dto2.bundle).isEqualTo(dto.bundle);
		assertServicePropMapEquals(dto.properties, dto2.properties);
		assertThat(Arrays.equals(dto.usingBundles, dto2.usingBundles)).isTrue();
	}

	private void assertServicePropMapEquals(Map<String,Object> p1,
			Map<String,Object> p2) {
		assertThat(p2.size()).isEqualTo(p1.size());

		for (Map.Entry<String,Object> entry : p1.entrySet()) {
			Object v1 = entry.getValue();
			Object v2 = p2.get(entry.getKey());
			if (v1 instanceof Object[]) {
				assertThat(Arrays.equals((Object[]) v1, (Object[]) v2)).isTrue();
			} else {
				assertThat(v2).isEqualTo(v1);
			}
		}
	}
}


