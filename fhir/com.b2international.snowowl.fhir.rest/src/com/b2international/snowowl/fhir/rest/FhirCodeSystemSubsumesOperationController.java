/*
 * Copyright 2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.fhir.rest;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.b2international.snowowl.fhir.core.exceptions.BadRequestException;
import com.b2international.snowowl.fhir.core.model.OperationOutcome;
import com.b2international.snowowl.fhir.core.model.codesystem.SubsumptionRequest;
import com.b2international.snowowl.fhir.core.model.codesystem.SubsumptionResult;
import com.b2international.snowowl.fhir.core.model.dt.Coding;
import com.b2international.snowowl.fhir.core.model.dt.Parameters;
import com.b2international.snowowl.fhir.core.provider.ICodeSystemApiProvider;

import io.swagger.annotations.*;

/**
 * @since 8.0
 */
@Api(value = "CodeSystem", description="FHIR CodeSystem Resource", tags = { "CodeSystem" })
@RestController
@RequestMapping(value="/CodeSystem", produces = { AbstractFhirController.APPLICATION_FHIR_JSON })
public class FhirCodeSystemSubsumesOperationController extends AbstractFhirController {

	/*
	 * Subsumes GET method with no codeSystemId and parameters
	 */
	@ApiOperation(
			value="Subsumption testing",
			notes="Test the subsumption relationship between code/Coding A and code/Coding B given the semantics of subsumption in the underlying code system (see hierarchyMeaning).")
	@ApiResponses({
		@ApiResponse(code = HTTP_OK, message = "OK"),
		@ApiResponse(code = HTTP_BAD_REQUEST, message = "Bad request", response = OperationOutcome.class),
		@ApiResponse(code = HTTP_NOT_FOUND, message = "Code system not found", response = OperationOutcome.class)
	})
	@RequestMapping(value="/$subsumes", method=RequestMethod.GET)
	public Parameters.Fhir subsumes(
			@ApiParam(value="The \"A\" code that is to be tested") @RequestParam(value="codeA") final String codeA,
			@ApiParam(value="The \"B\" code that is to be tested") @RequestParam(value="codeB") final String codeB,
			@ApiParam(value="The code system's uri") @RequestParam(value="system") final String system,
			@ApiParam(value="The code system version") @RequestParam(value="version", required=false) final String version) {
		
		validateSubsumptionRequest(codeA, codeB, system, version);
		
		final SubsumptionRequest req = SubsumptionRequest.builder()
				.codeA(codeA)
				.codeB(codeB)
				.system(system)
				.version(version)
				.build();
		
		ICodeSystemApiProvider codeSystemProvider = codeSystemProviderRegistry.getCodeSystemProvider(getBus(), locales, req.getSystem());
		final SubsumptionResult result = codeSystemProvider.subsumes(req);
		
		return toResponse(result);
	}
	
	/*
	 * Subsumes GET method with codeSystemId and parameters
	 */
	@ApiOperation(
			value="Subsumption testing",
			notes="Test the subsumption relationship between code/Coding A and code/Coding B given the semantics of subsumption in the underlying code system (see hierarchyMeaning).")
	@ApiResponses({
		@ApiResponse(code = HTTP_OK, message = "OK"),
		@ApiResponse(code = HTTP_BAD_REQUEST, message = "Bad request", response = OperationOutcome.class),
		@ApiResponse(code = HTTP_NOT_FOUND, message = "Code system not found", response = OperationOutcome.class)
	})
	@RequestMapping(value="{codeSystemId:**}/$subsumes", method=RequestMethod.GET)
	public Parameters.Fhir subsumes(
			@ApiParam(value="The id of the code system to invoke the operation on") 	@PathVariable("codeSystemId") String codeSystemId,
			@ApiParam(value="The \"A\" code that is to be tested") @RequestParam(value="codeA") final String codeA,
			@ApiParam(value="The \"B\" code that is to be tested") @RequestParam(value="codeB") final String codeB,
			@ApiParam(value="The code system's uri") @RequestParam(value="system") final String system,
			@ApiParam(value="The code system version") @RequestParam(value="version", required=false) final String version	) {
		
		validateSubsumptionRequest(codeSystemId, codeA, codeB, system, version);
		
		final SubsumptionRequest req = SubsumptionRequest.builder()
			.codeA(codeA)
			.codeB(codeB)
			.system(codeSystemId) //TODO: this is incorrect as this is an URL and should not be populated on the instance level
			.version(version)
			.build();
		
		ICodeSystemApiProvider codeSystemProvider = codeSystemProviderRegistry.getCodeSystemProvider(getBus(), locales, req.getSystem());
		final SubsumptionResult result = codeSystemProvider.subsumes(req);
		
		return toResponse(result);
	}
	
	/*
	 * Subsumes POST method without codeSystemId and body
	 */
	@ApiOperation(value="Subsumption testing", notes="Test the subsumption relationship between code/Coding A and code/Coding B given the semantics of subsumption in the underlying code system (see hierarchyMeaning).")
	@ApiResponses({
		@ApiResponse(code = HTTP_OK, message = "OK"),
		@ApiResponse(code = HTTP_NOT_FOUND, message = "Not found", response = OperationOutcome.class),
		@ApiResponse(code = HTTP_BAD_REQUEST, message = "Bad request", response = OperationOutcome.class)
	})
	@RequestMapping(value="/$subsumes", method=RequestMethod.POST, consumes = AbstractFhirResourceController.APPLICATION_FHIR_JSON)
	public Parameters.Fhir subsumes(
			@ApiParam(name = "body", value = "The lookup request parameters")
			@RequestBody Parameters.Fhir in) {
		
		SubsumptionRequest request = toRequest(in, SubsumptionRequest.class);
		
		validateSubsumptionRequest(request);
		
		ICodeSystemApiProvider codeSystemProvider = codeSystemProviderRegistry.getCodeSystemProvider(getBus(), locales, request.getSystem());
		SubsumptionResult result = codeSystemProvider.subsumes(request);
		return toResponse(result);
	}
	
	/*
	 * Subsumes POST method with code system as path parameter
	 */
	@ApiOperation(value="Subsumption testing", notes="Test the subsumption relationship between code/Coding A and code/Coding B given the semantics of subsumption in the underlying code system (see hierarchyMeaning).")
	@ApiResponses({
		@ApiResponse(code = HTTP_OK, message = "OK"),
		@ApiResponse(code = HTTP_NOT_FOUND, message = "Not found", response = OperationOutcome.class),
		@ApiResponse(code = HTTP_BAD_REQUEST, message = "Bad request", response = OperationOutcome.class)
	})
	@RequestMapping(value="{codeSystemId:**}/$subsumes", method=RequestMethod.POST, consumes = AbstractFhirResourceController.APPLICATION_FHIR_JSON)
	public Parameters.Fhir subsumes(
			@ApiParam(value="The id of the code system to invoke the operation on") @PathVariable("codeSystemId") String codeSystemId,
			@ApiParam(name = "body", value = "The lookup request parameters") @RequestBody Parameters.Fhir in) {
		
		SubsumptionRequest request = toRequest(in, SubsumptionRequest.class);
		
		validateSubsumptionRequest(request);
		
		//TODO: incorrect as it should use the codeSystemID instead of the systemID!
		SubsumptionResult result = codeSystemProviderRegistry.getCodeSystemProvider(getBus(), locales, request.getSystem()).subsumes(request);
		return toResponse(result);
	}
	
	private void validateSubsumptionRequest(String codeA, String codeB, String system, String version) {
		validateSubsumptionRequest(null, codeA,  codeB, system, version);
	}
	
	private void validateSubsumptionRequest(SubsumptionRequest request) {
		validateSubsumptionRequest(null, request);
	}
	
	private void validateSubsumptionRequest(String codeSystemId, SubsumptionRequest request) {
		validateSubsumptionRequest(codeSystemId, request.getCodeA(), request.getCodeB(), request.getSystem(), request.getVersion(), request.getCodingA(), request.getCodingB());
	}

	private void validateSubsumptionRequest(String codeSystemId, String codeA, String codeB, String system, String version) {
		validateSubsumptionRequest(codeSystemId, codeA, codeB, system, version, null, null);
	}
	
	private void validateSubsumptionRequest(String codeSystemId, String codeA, String codeB, String system, String version, Coding codingA, Coding codingB) {
		
		//check the systems
		if (StringUtils.isEmpty(system) && StringUtils.isEmpty(codeSystemId)) {
			throw new BadRequestException("Parameter 'system' is not specified for subsumption testing.", "SubsumptionRequest.system");
		}
		
		//TODO: this probably incorrect as codeSystemId is an internal id vs. system that is external
		if (!StringUtils.isEmpty(system) && !StringUtils.isEmpty(codeSystemId)) {
			if (!codeSystemId.equals(system)) {
				throw new BadRequestException(String.format("Parameter 'system: %s' and path parameter 'codeSystem: %s' are not the same.", system, codeSystemId), "SubsumptionRequest.system");
			}
		}
		
		//all empty
		if (StringUtils.isEmpty(codeA) && StringUtils.isEmpty(codeA) && codingA == null && codingB == null) {
			throw new BadRequestException("No codes or Codings are provided for subsumption testing.", "SubsumptionRequest");
		}
		
		//No codes
		if (StringUtils.isEmpty(codeA) && StringUtils.isEmpty(codeA)) {
			if (codingA == null || codingB == null) {
				throw new BadRequestException("No Codings are provided for subsumption testing.", "SubsumptionRequest.Coding");
			}
		}
		
		//No codings
		if (codingA == null && codingB == null) {
			if (StringUtils.isEmpty(codeA) || StringUtils.isEmpty(codeB)) {
				throw new BadRequestException("No codes are provided for subsumption testing.", "SubsumptionRequest.code");
			}
		}
		
		//Codes are there
		if (!StringUtils.isEmpty(codeA) && !StringUtils.isEmpty(codeA)) {
			if (codingA != null || codingB != null) {
				throw new BadRequestException("Provide either codes or Codings.", "SubsumptionRequest");
			}
		}
		
		//Coding are there
		if (codingA != null && codingB != null) {
			if (!StringUtils.isEmpty(codeA) || !StringUtils.isEmpty(codeA)) {
				throw new BadRequestException("Provide either codes or Codings.", "SubsumptionRequest");
			}
		}
	}

	
}