/*
 * Copyright 2011-2022 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.fhir.core.model.dt;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.b2international.commons.StringUtils;

/**
 * Div validator for the {@link ValidDiv} annotation
 * 
 * @since 6.4
 */
public class DivValidator implements ConstraintValidator<ValidDiv, String> {

	@Override
	public void initialize(ValidDiv constraintAnnotation) {
	}

	@Override
	public boolean isValid(String divString, ConstraintValidatorContext constraintContext) {

		// use @NotNull for null validation
		if (StringUtils.isEmpty(divString)) {
			return false;
		}
		
		// empty (self-closing) tag is OK
		if (divString.equalsIgnoreCase("<div/>")) {
			return true;
		}
		
		// allow attributes after the opening tag, eg. xmlns
		if (!divString.toLowerCase().startsWith("<div ") && !divString.toLowerCase().startsWith("<div>")) {
			return false;
		}

		if (!divString.toLowerCase().endsWith("</div>")) {
			return false;
		}
		
		return true;
	}
}
