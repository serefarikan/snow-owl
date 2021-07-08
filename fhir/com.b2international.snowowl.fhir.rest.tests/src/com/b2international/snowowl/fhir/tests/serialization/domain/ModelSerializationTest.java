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
package com.b2international.snowowl.fhir.tests.serialization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import com.b2international.snowowl.fhir.core.FhirConstants;
import com.b2international.snowowl.fhir.core.codesystems.IssueSeverity;
import com.b2international.snowowl.fhir.core.codesystems.IssueType;
import com.b2international.snowowl.fhir.core.codesystems.OperationOutcomeCode;
import com.b2international.snowowl.fhir.core.exceptions.ValidationException;
import com.b2international.snowowl.fhir.core.model.ContactDetail;
import com.b2international.snowowl.fhir.core.model.Issue;
import com.b2international.snowowl.fhir.core.model.Issue.Builder;
import com.b2international.snowowl.fhir.core.model.Meta;
import com.b2international.snowowl.fhir.core.model.OperationOutcome;
import com.b2international.snowowl.fhir.core.model.dt.*;
import com.b2international.snowowl.fhir.tests.FhirExceptionIssueMatcher;
import com.b2international.snowowl.fhir.tests.FhirTest;

import io.restassured.path.json.JsonPath;

/**
 * 
 * Test for domain model serialization.
 * @since 6.3
 */
public class ModelSerializationTest extends FhirTest {
	
	private Builder builder = Issue.builder()
		.code(IssueType.INVALID)
		.severity(IssueSeverity.ERROR)
		.diagnostics("1 validation error");
	
	@Test
	public void contactDetailTest() throws Exception {
		
		ContactPoint cp = ContactPoint.builder()
			.period(new Period(null, null))
			.rank(1)
			.system("system")
			.value("value")
			.build();
		
		ContactDetail cd = ContactDetail.builder()
			.name("name")
			.addContactPoint(cp)
			.build();
		
		JsonPath jsonPath = JsonPath.from(objectMapper.writeValueAsString(cd));
		assertThat(jsonPath.getString("name")).isEqualTo("name");
		jsonPath.setRoot("telecom[0]");
		assertThat(jsonPath.getString("system")).isEqualTo("system");
		assertThat(jsonPath.getString("period.start")).isEqualTo(null);
		assertThat(jsonPath.getString("value")).isEqualTo("value");
		assertThat(jsonPath.getInt("rank")).isEqualTo(1);
	}
	
	@Test
	public void metaTest() throws Exception {
		
		DateFormat df = new SimpleDateFormat(FhirConstants.DATE_TIME_FORMAT);
		Date date = df.parse(TEST_DATE_STRING);
		Instant instant = Instant.builder().instant(date).build();
		
		Meta meta = Meta.builder()
			.versionId(new Id("versionId"))
			.lastUpdated(instant)
			.addProfile("profileValue")
			.addSecurity(Coding.builder()
					.code("code").build())
			.addTag(Coding.builder()
					.code("tag").build())
			.build();
		
		JsonPath jsonPath = JsonPath.from(objectMapper.writeValueAsString(meta));
		assertThat(jsonPath.getString("versionId")).isEqualTo("versionId");
		assertThat(jsonPath.getString("lastUpdated")).isEqualTo("2018-03-23T07:49:40Z");
		assertThat(jsonPath.getString("security[0].code")).isEqualTo("code");
		assertThat(jsonPath.getString("tag[0].code")).isEqualTo("tag");
		assertThat(jsonPath.getString("profile[0]")).isEqualTo("profileValue");
	}
	
	@Test
	public void operationOutcomeTest() throws Exception {
		OperationOutcome ou = OperationOutcome.builder()
				.addIssue(Issue.builder()
						.severity(IssueSeverity.ERROR)
						.code(IssueType.REQUIRED)
						.build())
				.build();
		
		JsonPath jsonPath = JsonPath.from(objectMapper.writeValueAsString(ou));
		assertThat(jsonPath.getString("resourceType")).isEqualTo("OperationOutcome");
		jsonPath.setRoot("issue[0]");
		
		assertThat(jsonPath.getString("severity")).isEqualTo("error");
		assertThat(jsonPath.getString("code")).isEqualTo("required");
	}
	
	@Test
	public void missingIssueTest() throws Exception {
		Issue expectedIssue = builder.addLocation("OperationOutcome.issues")
			.codeableConceptWithDisplay(OperationOutcomeCode.MSG_PARAM_INVALID, "Parameter 'issues' content is invalid [null]. Violation: may not be empty.")
			.build();
		
		exception.expect(ValidationException.class);
		exception.expectMessage("1 validation error");
		exception.expect(FhirExceptionIssueMatcher.issue(expectedIssue));
		
		OperationOutcome.builder().build();
	}
	
}
