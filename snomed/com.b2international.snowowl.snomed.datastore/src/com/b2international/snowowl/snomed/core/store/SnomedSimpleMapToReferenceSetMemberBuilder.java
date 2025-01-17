/*
 * Copyright 2022 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.core.store;

import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRefSetMemberIndexEntry;

/**
 * @since 8.4
 */
public final class SnomedSimpleMapToReferenceSetMemberBuilder extends SnomedMemberBuilder<SnomedSimpleMapToReferenceSetMemberBuilder> {

	private String mapSourceId;

	public SnomedSimpleMapToReferenceSetMemberBuilder withMapSourceId(String mapSourceId) {
		this.mapSourceId = mapSourceId;
		return getSelf();
	}

	@Override
	public void init(SnomedRefSetMemberIndexEntry.Builder component, TransactionContext context) {
		super.init(component, context);
		component.field(SnomedRf2Headers.FIELD_MAP_SOURCE, mapSourceId);
	}
}
