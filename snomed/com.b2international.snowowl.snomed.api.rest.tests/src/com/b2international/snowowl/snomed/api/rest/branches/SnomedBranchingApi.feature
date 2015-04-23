/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.api.rest.branches

import com.jayway.restassured.response.Response
import java.util.Date
import java.util.UUID
import com.b2international.snowowl.snomed.api.rest.concept.*

import static extension com.b2international.snowowl.test.commons.rest.RestExtensions.*
import com.jayway.restassured.http.ContentType

/**
 * @since 2.0
 */
Feature: SnomedBranchingApi

	Background:
		static String API = "/v2/snomed-ct"
		var public String parent = "MAIN"
		var public String branchName = UUID.randomUUID.toString
		var description = "Description at " + new Date
		var req = givenAuthenticatedRequest(API)
		var Response res
		
	Scenario: Nonexistent SNOMED CT Branch
	
		Given parent "MAIN"
			parent = args.first
		And branchName "nonexistent"
			branchName = args.first
		When sending GET to "/branches/${parent}/${branchName}"
			res = req.get(args.first.renderWithFields(this))
		Then return "404" status
		And return body with status "404"
		
	Scenario: New SNOMED-CT branch under nonexistent parent 
	
		Given new SNOMED-CT branch request under parent branch "nonexistent"
		When sending POST to "/branches"
		Then return "400" status
		And return body with status "400"
		
	Scenario: New SNOMED CT Branch under MAIN
	
		Given new SNOMED-CT branch request under parent branch "MAIN"
			parent = args.first
			req.withJson(#{
				"parent" -> parent,
  				"name" -> branchName
			})
		When sending POST to "/branches"
		Then return "201" status
		And return location header pointing to "/branches/${parent}/${branchName}"
		And return "200" with body when sending GET to "/branches/${parent}/${branchName}"
			res = givenAuthenticatedRequest(API).accept(ContentType.JSON).get(args.second.renderWithFields(this))
			res.expectStatus(args.first.toInt)
	
	Scenario: Duplicate branch on same parent
	
		Given new SNOMED-CT branch request under parent branch "MAIN"
		And another new SNOMED-CT branch request under parent branch "MAIN"
			req.withJson(#{
				"parent" -> args.first,
  				"name" -> branchName
			})
		When sending POST to "/branches"
		And sending the second POST to "/branches"
			res = req.post(args.first.renderWithFields(this))
		Then return "409" status
		And return body with status "409"