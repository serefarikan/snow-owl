/*
 * Copyright 2020-2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.codesystem;

import com.b2international.snowowl.core.ResourceURI;
import com.b2international.snowowl.core.context.ResourceRepositoryRequestBuilder;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.events.BaseRequestBuilder;
import com.b2international.snowowl.core.events.Request;

/**
 * @since 7.13.0
 */
public final class CodeSystemUpgradeRequestBuilder 
		extends BaseRequestBuilder<CodeSystemUpgradeRequestBuilder, RepositoryContext, String> 
		implements ResourceRepositoryRequestBuilder<String> {

	private final ResourceURI resource;
	private final ResourceURI extensionOf;
	
	private String resourceId;

	CodeSystemUpgradeRequestBuilder(ResourceURI resource, ResourceURI extensionOf) {
		this.resource = resource;
		this.extensionOf = extensionOf;
	}
	
	/**
	 * Optionally set the upgrade resource's unique ID instead of letting the system generate it automatically from the resource and the new extensionOf values.
	 * 
	 * @param resourceId
	 * @return
	 */
	public CodeSystemUpgradeRequestBuilder setResourceId(String resourceId) {
		this.resourceId = resourceId;
		return getSelf();
	}
	
	@Override
	protected Request<RepositoryContext, String> doBuild() {
		final CodeSystemUpgradeRequest req = new CodeSystemUpgradeRequest(resource, extensionOf);
		req.setResourceId(resourceId);
		return req;
	}

}
