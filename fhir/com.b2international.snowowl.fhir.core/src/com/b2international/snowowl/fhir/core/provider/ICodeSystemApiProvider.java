/*
 * Copyright 2018-2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.fhir.core.provider;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.ResourceURI;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.fhir.core.exceptions.BadRequestException;
import com.b2international.snowowl.fhir.core.model.codesystem.CodeSystem;
import com.b2international.snowowl.fhir.core.search.FhirSearchParameter;

/**
 * Extension point interface for code system specific FHIR API support. 
 * 
 * @see <a href="https://www.hl7.org/fhir/2016May/terminologies.html#system">FHIR:Terminologies:System</a> to determine whether a code system is supported
 * @since 7.0
 */
public interface ICodeSystemApiProvider {

	/**
	 * @since 7.2
	 */
	interface Factory {
		ICodeSystemApiProvider create(IEventBus bus, List<ExtendedLocale> locales);
	}
	
	/**
	 * Returns the code systems based on the search parameters provided.
	 * Passing in an empty collection as parameters returns all the available code systems.
	 * @param searchParameters
	 * @return collection of code systems found based on the parameters
	 */
	Collection<CodeSystem> getCodeSystems(Set<FhirSearchParameter> searchParameters);

	/**
	 * Returns the code system for the passed in logical id @see {@link ResourceURI}
	 * @param codeSystemId
	 * @return {@link CodeSystem}
	 * @throws BadRequestException if the code system is not supported by this provider
	 */
	CodeSystem getCodeSystem(ResourceURI codeSystemId);

}
