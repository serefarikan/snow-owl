/*
 * Copyright 2011-2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.fhir.core.model;

import org.hibernate.validator.constraints.NotEmpty;

import com.b2international.snowowl.fhir.core.model.dt.Code;
import com.b2international.snowowl.fhir.core.model.dt.Coding;

/**
 * This class represents a FHIR designation.
 *
 * @see <a href="https://http://hl7.org/fhir/codesystem-definitions.html#CodeSystem.concept.designation">FHIR:CodeSystem:Designation</a>
 * @since 6.3
 */
public class Designation {
	
	//The language code this designation is defined for (0..1)
	private Code language;
	
	//A code that details how this designation would be used (0..1)
	private Coding use;
	
	//The text value for this designation (1..1)
	@NotEmpty
	private String value;
	
	Designation(final Code language, final Coding use, final String value) {
		this.language = language;
		this.use = use;
		this.value = value;
	}
	
	public Code getLanguage() {
		return language;
	}

	public Coding getUse() {
		return use;
	}

	public String getValue() {
		return value;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder extends ValidatingBuilder<Designation>{
		
		private Code languageCode;
		private Coding use;
		private String value;

		public Builder languageCode(final String languageCode) {
			this.languageCode = new Code(languageCode);
			return this;
		}
		
		public Builder use(final Coding use) {
			this.use = use;
			return this;
		}
		
		public Builder value(final String value) {
			this.value = value;
			return this;
		}

		@Override
		protected Designation doBuild() {
			return new Designation(languageCode, use, value);
		}
	}
	
}
