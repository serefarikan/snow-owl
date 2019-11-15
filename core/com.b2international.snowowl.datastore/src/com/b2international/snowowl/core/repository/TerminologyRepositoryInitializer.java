/*
 * Copyright 2018-2019 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.core.repository;

import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.datastore.CodeSystem;
import com.b2international.snowowl.datastore.CodeSystems;
import com.b2international.snowowl.identity.domain.User;
import com.b2international.snowowl.terminologyregistry.core.request.CodeSystemRequests;

/**
 * Ensures that a particular terminology repository is in the expected state at
 * the beginning of its lifecycle.
 * <p>
 * Implementations are typically performing the following operations:
 * <ul>
 * <li>Register the repository's primary code system;
 * <li>Import/prepare default data from various source files;
 * </ul>
 * <p>
 */
public abstract class TerminologyRepositoryInitializer {

	/**
	 * Executes initialization steps for the corresponding repository.
	 * 
	 * @param context - the repository context to use for the repository initialization 
	 */
	public void initialize(RepositoryContext context) {
		CodeSystem primaryCodeSystem = createPrimaryCodeSystem();
	
		if (primaryCodeSystem != null) {
			CodeSystems codeSystems = CodeSystemRequests.prepareSearchCodeSystem()
					.filterById(primaryCodeSystem.getShortName())
					.setLimit(0)
					.build()
					.execute(context);
			
			if (codeSystems.getTotal() < 1) {
				primaryCodeSystem.toCreateRequest()
					.build(context.id(), Branch.MAIN_PATH, User.SYSTEM.getUsername(), "Create primary code system for repository")
					.getRequest()
					.execute(context);
			}
		}
	}

	/**
	 * Prepare and return {@link CodeSystem} that represents the primary codesystem for a given terminology.
	 * @return
	 */
	protected CodeSystem createPrimaryCodeSystem() {
		return null;
	}
}
