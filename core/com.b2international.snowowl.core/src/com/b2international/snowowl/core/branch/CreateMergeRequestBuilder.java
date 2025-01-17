/*
 * Copyright 2011-2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.branch;

import java.util.Set;

import com.b2international.commons.collections.Collections3;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.events.BaseRequestBuilder;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.core.merge.Merge;
import com.b2international.snowowl.core.request.RepositoryRequestBuilder;

/**
 * @since 4.5
 */
public final class CreateMergeRequestBuilder extends BaseRequestBuilder<CreateMergeRequestBuilder, RepositoryContext, Merge> implements RepositoryRequestBuilder<Merge> {
	
	private String source;
	private String target;
	private String commitComment;
	private Set<String> exclusions;
	
	private String userId;
	private String parentLockContext;
	private boolean squash = true;
	
	CreateMergeRequestBuilder() {}
	
	public CreateMergeRequestBuilder setSource(String source) {
		this.source = source;
		return this;
	}
	
	public CreateMergeRequestBuilder setTarget(String target) {
		this.target = target;
		return this;
	}
	
	public CreateMergeRequestBuilder setUserId(String userId) {
		this.userId = userId;
		return this;
	}
	
	public CreateMergeRequestBuilder setCommitComment(String commitComment) {
		this.commitComment = commitComment;
		return this;
	}
	
	public CreateMergeRequestBuilder setParentLockContext(String parentLockContext) {
		this.parentLockContext = parentLockContext;
		return this;
	}
	
	public CreateMergeRequestBuilder setExclusions(Set<String> exclusions) {
		this.exclusions = exclusions;
		return this;
	}
	
	public CreateMergeRequestBuilder setSquash(boolean squash) {
		this.squash = squash;
		return this;
	}
	
	@Override
	protected Request<RepositoryContext, Merge> doBuild() {
		final IBranchPath sourcePath = BranchPathUtils.createPath(source);
		final IBranchPath targetPath = BranchPathUtils.createPath(target);
		if (targetPath.getParent().equals(sourcePath)) {
			return new BranchRebaseRequest(source, target, userId, commitComment, parentLockContext);
		} else {
			return new BranchMergeRequest(source, target, Collections3.toImmutableSet(exclusions), userId, commitComment, parentLockContext, squash);
		}
	}
	
}
