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
package com.b2international.snowowl.fhir.api.model;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.b2international.snowowl.fhir.api.codesystems.IssueSeverity;
import com.b2international.snowowl.fhir.api.codesystems.IssueType;
import com.b2international.snowowl.fhir.api.codesystems.OperationOutcomeCode;
import com.b2international.snowowl.fhir.api.model.dt.Code;
import com.b2international.snowowl.fhir.api.model.dt.CodeableConcept;
import com.b2international.snowowl.fhir.api.model.dt.Coding;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.collect.Lists;

/**
 * "Throughout this specification, coded values are always treated as a pair
 * composed of "system" and "code", where the system is a URL that identifies
 * the code system that defines the codes." Except here.
 * 
 * @see <a href=
 *      "http://hl7.org/fhir/operationoutcome-definitions.html#OperationOutcome.issue">FHIR:OperationOutcome:issue</a>
 * 
 * @since 6.3
 *
 */
public class Issue {

	@NotNull
	@Valid
	@JsonSerialize(using = ToStringSerializer.class)
	private final Code severity;

	@NotNull
	@Valid
	@JsonSerialize(using = ToStringSerializer.class)
	private final Code code;

	@JsonProperty("details")
	private final CodeableConcept codeableConcept;

	/*
	 * Additional diagnostic information about the issue. Typically, this may be a
	 * description of how a value is erroneous, or a stack dump to help trace the
	 * issue.
	 */
	private final String diagnostics;

	/*
	 * For resource issues, this will be a simple XPath limited to element names,
	 * repetition indicators and the default child access that identifies one of the
	 * elements in the resource that caused this issue to be raised. For HTTP
	 * errors, will be "http." + the parameter name.
	 */
	@JsonProperty("location")
	private final Collection<String> locations;

	/*
	 * A simple FHIRPath limited to element names, repetition indicators and the
	 * default child access that identifies one of the elements in the resource that
	 * caused this issue to be raised.
	 */
	@JsonProperty("expression")
	private final Collection<String> expressions;

	Issue(Code severity, Code code, CodeableConcept codeableConcept, String diagnostics, Collection<String> locations,
			Collection<String> expressions) {

		this.severity = severity;
		this.code = code;
		this.codeableConcept = codeableConcept;
		this.diagnostics = diagnostics;
		this.locations = locations;
		this.expressions = expressions;
	}

	public Code getSeverity() {
		return severity;
	}

	public Code getCode() {
		return code;
	}

	public CodeableConcept getCodeableConcept() {
		return codeableConcept;
	}

	public String getDiagnostics() {
		return diagnostics;
	}

	public Collection<String> getLocations() {
		return locations;
	}

	public Collection<String> getExpressions() {
		return expressions;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder extends ValidatingBuilder<Issue> {

		private Code severity;
		private Code code;
		private CodeableConcept codeableConcept;
		private String diagnostics;
		private Collection<String> locations = Lists.newArrayList();
		private Collection<String> expressions = Lists.newArrayList();

		public Builder severity(final IssueSeverity severity) {
			this.severity = severity.getCode();
			return this;
		}

		public Builder code(final IssueType issueType) {
			this.code = issueType.getCode();
			return this;
		}

		public Builder codeableConcept(OperationOutcomeCode operationOutcomeCode) {
			
			//Displayname %s substitution missing
			Coding coding = Coding.builder()
					.code(operationOutcomeCode.getCodeValue())
					.system(OperationOutcomeCode.CODE_SYSTEM_URI)
					.display(operationOutcomeCode.displayName()).build();
			
			this.codeableConcept = new CodeableConcept(coding, operationOutcomeCode.displayName());
			return this;
		}
		
		public Builder codeableConcept(OperationOutcomeCode operationOutcomeCode, Object... args) {
			
			String substitutedDisplayName = String.format(operationOutcomeCode.displayName(), args);
			System.err.println("SDN: " + substitutedDisplayName);
			
			Coding coding = Coding.builder()
					.code(operationOutcomeCode.getCodeValue())
					.system(OperationOutcomeCode.CODE_SYSTEM_URI)
					.display(substitutedDisplayName)
					.build();
			
			this.codeableConcept = new CodeableConcept(coding, substitutedDisplayName);
			return this;
		}

		public Builder codeableConcept(CodeableConcept codeableConcept) {
			this.codeableConcept = codeableConcept;
			return this;
		}

		public Builder diagnostics(String diagnostics) {
			this.diagnostics = diagnostics;
			return this;
		}

		public Builder addLocations(String location) {
			locations.add(location);
			return this;
		}

		@Override
		protected Issue doBuild() {
			return new Issue(severity, code, codeableConcept, diagnostics, locations, expressions);
		}

	}

	@Override
	public String toString() {
		return "Issue [severity=" + severity + ", code=" + code + ", codeableConcept=" + codeableConcept + ", diagnostics=" + diagnostics
				+ ", locations=" + locations + ", expressions=" + expressions + "]";
	}

}
