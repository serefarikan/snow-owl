/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.datastore.server.request;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.core.events.BaseRequest;
import com.b2international.snowowl.core.exceptions.ComponentNotFoundException;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.datastore.SnomedDescriptionLookupService;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry;
import com.b2international.snowowl.snomed.datastore.server.converter.SnomedConverters;

/**
 * @since 4.5
 */
final class SnomedDescriptionReadRequest extends BaseRequest<BranchContext, ISnomedDescription> {

	private String componentId;

	public SnomedDescriptionReadRequest(String componentId) {
		this.componentId = componentId;
	}
	
	@Override
	public ISnomedDescription execute(BranchContext context) {
		final IBranchPath branchPath = context.branch().branchPath();
		final SnomedDescriptionLookupService lookupService = new SnomedDescriptionLookupService();
		if (!lookupService.exists(branchPath, componentId)) {
			throw new ComponentNotFoundException(ComponentCategory.DESCRIPTION, componentId);
		}
		final SnomedDescriptionIndexEntry descriptionIndexEntry = lookupService.getComponent(branchPath, componentId);
		return SnomedConverters.newDescriptionConverter(context, null).convert(descriptionIndexEntry);
	}

	@Override
	protected Class<ISnomedDescription> getReturnType() {
		return ISnomedDescription.class;
	}

}
