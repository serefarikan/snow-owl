/*
 * Copyright 2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.reasoner.exceptions;

import java.util.Map;

import com.b2international.commons.exceptions.ApiException;
import com.google.common.collect.ImmutableMap;

/**
 * @since 7.0
 */
public final class ReasonerApiException extends ApiException {

	public ReasonerApiException(final String template, final Object... args) {
		super(template, args);
	}
	
	@Override
	protected Map<String, Object> getAdditionalInfo() {
		final ImmutableMap.Builder<String, Object> additionalInfoBuilder = ImmutableMap.<String, Object>builder()
				.putAll(super.getAdditionalInfo());
		
		if (getCause() != null) {
			additionalInfoBuilder.put("cause", getCause().getMessage());
		}
		
		return additionalInfoBuilder.build();
	}

	@Override
	protected Integer getStatus() {
		return 500;
	}
}
