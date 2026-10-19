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

import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.permission.PermissionAssertions.assertThat;

import java.security.PermissionCollection;

import org.junit.jupiter.api.Test;
import org.osgi.service.clusterinfo.ClusterTagPermission;

public class ClusterTagPermissionTestCase {

	@Test
	public void testInvalid() {
		invalidClusterTagPermission("mycluster", "x");
		invalidClusterTagPermission("mycluster", "   add  ,  x   ");
		invalidClusterTagPermission("mycluster", "");
		invalidClusterTagPermission("mycluster", "      ");
		invalidClusterTagPermission("mycluster", null);
		invalidClusterTagPermission("mycluster", ",");
		invalidClusterTagPermission("mycluster", ",xxx");
		invalidClusterTagPermission("mycluster", "xxx,");
		invalidClusterTagPermission("mycluster", "add,");
		invalidClusterTagPermission("mycluster", "add,   ");
		invalidClusterTagPermission("mycluster", "addme,");
		invalidClusterTagPermission("mycluster", "addme,");
		invalidClusterTagPermission("mycluster", ",add");
		invalidClusterTagPermission("mycluster", "   addme   ");
		invalidClusterTagPermission("mycluster", "   ad");
		invalidClusterTagPermission("", "   add");
	}

	@Test
	public void testPermissions() {

		ClusterTagPermission p = new ClusterTagPermission("mycluster", "ADD");
		ClusterTagPermission p2 = new ClusterTagPermission("mycluster", "ADD");
		ClusterTagPermission p3 = new ClusterTagPermission("tag", "ADD");
		ClusterTagPermission p_all = new ClusterTagPermission("*", "ADD");

		assertThat(p).implies(p2);
		assertThat(p_all).implies(p);
		assertThat(p_all).implies(p2);
		assertThat(p_all).implies(p3);

		assertThat(p).doesNotImply(p3);
		assertThat(p).doesNotImply(p_all);
		assertThat(p2).doesNotImply(p_all);
		assertThat(p3).doesNotImply(p_all);

		assertThat(p).isEquivalentTo(p);
		assertThat(p2).isEquivalentTo(p2);
		assertThat(p3).isEquivalentTo(p3);
		assertThat(p_all).isEquivalentTo(p_all);

		assertThat(p).isEquivalentTo(p2);
		assertThat(p).isNotEquivalentTo(p3);
		assertThat(p).isNotEquivalentTo(p_all);
		assertThat(p2).isNotEquivalentTo(p3);
		assertThat(p2).isNotEquivalentTo(p_all);
		assertThat(p3).isNotEquivalentTo(p_all);

		PermissionCollection pc = p.newPermissionCollection();

		assertThat(pc).hasNoElements();

		assertThat(pc).doesNotImply(p3);

		assertThat(pc).accepts(p3);
		assertThat(pc).implies(p3);
		assertThat(pc).doesNotImply(p_all);

		assertThat(pc).accepts(p_all);
		assertThat(pc).implies(p);
		assertThat(pc).implies(p2);
		assertThat(pc).implies(p3);
		assertThat(pc).implies(p_all);

		pc.setReadOnly();

		assertThat(pc).rejects(p2);

		assertThat(pc).hasElements();

		assertThat(p).isSerializable();
		assertThat(p2).isSerializable();
		assertThat(p3).isSerializable();
		assertThat(p_all).isSerializable();
	}

	private void invalidClusterTagPermission(String name, String actions) {
		try {
			ClusterTagPermission p = new ClusterTagPermission(name, actions);
			fail(p + " created with invalid actions"); 
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}
}
