/*
 * Copyright 2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.rest.resource;

import java.util.List;

import com.b2international.snowowl.core.rest.domain.BaseResourceRestSearch;

/**
 * @since 8.0
 */
public class ResourceRestSearch extends BaseResourceRestSearch {

	private List<String> branch;
	
	private List<String> resourceType;
	
	private List<String> toolingId;
	
	private List<String> status;

	public List<String> getBranch() {
		return branch;
	}

	public void setBranch(List<String> branchPath) {
		this.branch = branchPath;
	}

	public List<String> getResourceType() {
		return resourceType;
	}

	public void setResourceType(List<String> resourceType) {
		this.resourceType = resourceType;
	}

	public List<String> getToolingId() {
		return toolingId;
	}

	public void setToolingId(List<String> toolingId) {
		this.toolingId = toolingId;
	}

	public List<String> getStatus() {
		return status;
	}
	
	public void setStatus(List<String> status) {
		this.status = status;
	}
	
}