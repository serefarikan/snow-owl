/*
 * Copyright 2021-2022 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.rest.suggest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.common.Strings;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import com.b2international.snowowl.core.codesystem.CodeSystemRequests;
import com.b2international.snowowl.core.events.util.Promise;
import com.b2international.snowowl.core.request.suggest.Suggester;
import com.b2international.snowowl.core.request.suggest.Suggestions;
import com.b2international.snowowl.core.rest.AbstractRestService;
import com.google.common.collect.Iterables;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 8.0
 */
@Tag(description = "Suggest", name = "suggest")
@RestController
@RequestMapping(value = "/suggest", produces = { AbstractRestService.JSON_MEDIA_TYPE })
public class SuggestRestService extends AbstractRestService {
	
	@Operation(
		summary = "Concept suggestion", 
		description = "Returns an actual concept of the specified code system based on the source term. Deprecated: due to the introduction of the selectable suggester implementation, the GET endpoint is no longer supported. Use the POST endpoint to properly configure your request.",
		deprecated = true
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request") 
	})
	@GetMapping
	public Promise<Suggestions> getSuggest(
		@ParameterObject
		final SuggestRestParameters params,
		
		@Parameter(description = "Accepted language tags, in order of preference", example = "en-US;q=0.8,en-GB;q=0.6")
		@RequestHeader(value=HttpHeaders.ACCEPT_LANGUAGE, defaultValue="en-US;q=0.8,en-GB;q=0.6", required=false) 
		final String acceptLanguage) {
		
		return CodeSystemRequests.prepareSuggestConcepts()
				// set the old parameters first, then if any new parameters present, override the old ones
				// configure from
				.setFrom(params.getCodeSystemPath())
				.setFrom(params.getFrom())
				
				// configure like
				.filterByTerm(params.getTerm())
				.filterByQuery(params.getQuery())
				.setLike(params.getQuery() == null ? null : List.of(params.getQuery()))
				.setLike(params.getLike())
				
				// configure unlike
				.filterByExclusion(params.getMustNotQuery())
				.setUnlike(params.getMustNotQuery() == null ? null : List.of(params.getMustNotQuery()))
				.setUnlike(params.getUnlike())
				
				// configure suggester
				.setMinOccurrenceCount(params.getMinOccurrenceCount())
				.setSuggester(params.getSuggester() == null && params.getTerm() != null ? Suggester.of("term", Map.of()) : params.getSuggester())

				// configure limits, locales, display
				.setLimit(params.getLimit())
				.setLocales(Strings.isNullOrEmpty(params.getAcceptLanguage()) ? acceptLanguage : params.getAcceptLanguage())
				.setPreferredDisplay(params.getPreferredDisplay())
				
				.buildAsync()
				.execute(getBus());
	}
	
	@Operation(
		summary = "Concept suggestion", 
		description = "Returns an actual concept of the specified code system based on the source term.")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request") 
	})
	@PostMapping(consumes = { AbstractRestService.JSON_MEDIA_TYPE })
	public Promise<Suggestions> postSuggest(
		@RequestBody
		final SuggestRestParameters body,
		
		@Parameter(description = "Accepted language tags, in order of preference", example = "en-US;q=0.8,en-GB;q=0.6")
		@RequestHeader(value=HttpHeaders.ACCEPT_LANGUAGE, defaultValue="en-US;q=0.8,en-GB;q=0.6", required=false) 
		final String acceptLanguage) {
		return getSuggest(body, acceptLanguage);
	}
	
	@Operation(
		summary = "Bulk concept suggestion", 
		description = "Perform a bulk suggestion request.")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request") 
	})
	@PostMapping(value = "/bulk", consumes = { AbstractRestService.JSON_MEDIA_TYPE })
	public List<Suggestions> postBulkSuggest(
		@RequestBody
		final List<SuggestRestParameters> body,
		
		@Parameter(description = "The number of suggestion requests to process in a batch (aka parallel)", example = "10")
		@RequestParam(value = "batchSize", defaultValue = "10", required = false)
		final Integer batchSize,
		
		@Parameter(description = "Timeout after a batch of suggestion request will fail and report back as timeout error (specified in seconds)", example = "120")
		@RequestParam(value = "batchTimeout", defaultValue = "120", required = false)
		final Integer batchTimeout,
		
		@Parameter(description = "Accepted language tags, in order of preference", example = "en-US;q=0.8,en-GB;q=0.6")
		@RequestHeader(value=HttpHeaders.ACCEPT_LANGUAGE, defaultValue="en-US;q=0.8,en-GB;q=0.6", required=false) 
		final String acceptLanguage) {
		
		final List<Suggestions> response = new ArrayList<>(body.size());
		for (Iterable<SuggestRestParameters> batch : Iterables.partition(body, batchSize)) {
			final List<Promise<Suggestions>> batchResponse = new ArrayList<>(batchSize);
			batch.forEach(params -> {
				batchResponse.add(getSuggest(params, acceptLanguage));
			});
			// wait for batch completion then proceed to next batch, a batch must complete in under 2 minutes (configurable)
			Promise.all(batchResponse)
				.getSync(batchTimeout, TimeUnit.SECONDS)
				.stream()
				.filter(Suggestions.class::isInstance)
				.map(Suggestions.class::cast)
				.forEach(response::add);
		}
		
		return response;
	}
}
