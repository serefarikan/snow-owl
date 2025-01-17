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
package com.b2international.snowowl.fhir.core.model.conceptmap;

import java.util.Collection;

import javax.validation.Valid;

import com.b2international.snowowl.fhir.core.codesystems.ConceptMapEquivalence;
import com.b2international.snowowl.fhir.core.model.ValidatingBuilder;
import com.b2international.snowowl.fhir.core.model.dt.Code;
import com.b2international.snowowl.fhir.core.search.Summary;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableList;

/**
 * FHIR Concept map target backbone element
 * <br> Concept in target system for element
 * @since 6.10
 */
@JsonDeserialize(builder = Target.Builder.class)
public class Target {

	@Valid
	@JsonProperty
	private final Code code;

	@Summary
	@JsonProperty
	private final String display;

	@Valid
	@JsonProperty
	private final Code equivalence;

	@JsonProperty
	private final String comment;

	@Valid
	@JsonProperty("dependsOn")
	@JsonInclude(Include.NON_EMPTY)
	private final Collection<DependsOn> dependsOnElements;

	@Valid
	@JsonProperty("product")
	@JsonInclude(Include.NON_EMPTY)
	private final Collection<DependsOn> products;

	public Code getCode() {
		return code;
	}
	
	public String getDisplay() {
		return display;
	}
	
	public Code getEquivalence() {
		return equivalence;
	}
	
	public String getComment() {
		return comment;
	}
	
	public Collection<DependsOn> getDependsOnElements() {
		return dependsOnElements;
	}
	
	public Collection<DependsOn> getProducts() {
		return products;
	}
	
	Target(Code code, String display, Code equivalence, String comment, Collection<DependsOn> dependsOnElements,
			Collection<DependsOn> products) {
		this.code = code;
		this.display = display;
		this.equivalence = equivalence;
		this.comment = comment;
		this.dependsOnElements = dependsOnElements;
		this.products = products;
	}

	public static Builder builder() {
		return new Builder();
	}

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder extends ValidatingBuilder<Target> {

		private Code code;
		private String display;
		private Code equivalence;
		private String comment;
		private ImmutableList.Builder<DependsOn> dependsOnElements = ImmutableList.builder();
		private ImmutableList.Builder<DependsOn> products = ImmutableList.builder();
	
		public Builder code(final Code code) {
			this.code = code;
			return this;
		}
		
		public Builder code(final String codeString) {
			this.code = new Code(codeString);
			return this;
		}
		
		public Builder display(final String display) {
			this.display = display;
			return this;
		}

		public Builder equivalence(final ConceptMapEquivalence conceptMapEquivalence) {
			this.equivalence = conceptMapEquivalence.getCode();
			return this;
		}
		
		public Builder equivalence(final String equivalenceString) {
			this.equivalence = new Code(equivalenceString);
			return this;
		}
		
		public Builder comment(final String comment) {
			this.comment = comment;
			return this;
		}
		
		@JsonProperty("dependsOn")
		@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
		public Builder dependsOns(Collection<DependsOn> dependsOns) {
			dependsOnElements.addAll(dependsOns);
			return this;
		}
		
		public Builder addDependsOn(final DependsOn dependsOn) {
			dependsOnElements.add(dependsOn);
			return this;
		}
		
		@JsonProperty("product")
		@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
		public Builder products(Collection<DependsOn> products) {
			this.products.addAll(products);
			return this;
		}
		
		public Builder addProduct(final DependsOn product) {
			products.add(product);
			return this;
		}
		
		@Override
		protected Target doBuild() {
			return new Target(code, display, equivalence, comment, dependsOnElements.build(), products.build());
		}
	}

}
