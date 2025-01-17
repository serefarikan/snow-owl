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
package com.b2international.snowowl.fhir.core.request.conceptmap;

import java.time.LocalDate;
import java.util.Map;

import com.b2international.commons.StringUtils;
import com.b2international.snowowl.core.ResourceURI;
import com.b2international.snowowl.core.context.ResourceRepositoryRequestBuilder;
import com.b2international.snowowl.core.domain.IComponent;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.fhir.core.model.conceptmap.ConceptMap;
import com.b2international.snowowl.fhir.core.request.FhirResourceUpdateResult;

/**
 * @since 8.2.0
 */
public final class FhirConceptMapUpdateRequestBuilder implements ResourceRepositoryRequestBuilder<FhirResourceUpdateResult> {

	private ConceptMap fhirConceptMap;
	private Map<String, ResourceURI> systemUriOverrides = Map.of();
	private String owner;
	private String ownerProfileName;
	private LocalDate defaultEffectiveDate;
	private String bundleId = IComponent.ROOT_ID;

	public FhirConceptMapUpdateRequestBuilder setFhirConceptMap(final ConceptMap fhirConceptMap) {
		this.fhirConceptMap = fhirConceptMap;
		return this;
	}

	public FhirConceptMapUpdateRequestBuilder setSystemUriOverrides(final Map<String, ResourceURI> systemUriOverrides) {
		if (systemUriOverrides == null) {
			this.systemUriOverrides = Map.of();
		} else {
			this.systemUriOverrides = systemUriOverrides;
		}
		return this;
	}
	
	public FhirConceptMapUpdateRequestBuilder setOwner(final String owner) {
		this.owner = owner;
		return this;
	}
	
	public FhirConceptMapUpdateRequestBuilder setOwnerProfileName(final String ownerProfileName) {
		this.ownerProfileName = ownerProfileName;
		return this;
	}
	
	public FhirConceptMapUpdateRequestBuilder setDefaultEffectiveDate(final LocalDate defaultEffectiveDate) {
		this.defaultEffectiveDate = defaultEffectiveDate;
		return this;
	}

	public FhirConceptMapUpdateRequestBuilder setBundleId(final String bundleId) {
		if (!StringUtils.isEmpty(bundleId)) {
			this.bundleId = bundleId;
		} else {
			this.bundleId = IComponent.ROOT_ID;
		}
		return this;
	}

	@Override
	public Request<RepositoryContext, FhirResourceUpdateResult> build() {
		return new FhirConceptMapUpdateRequest(
			fhirConceptMap, 
			systemUriOverrides, 
			owner, 
			ownerProfileName, 
			defaultEffectiveDate, 
			bundleId);
	}
}
