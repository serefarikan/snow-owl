/*
 * Copyright 2020-2022 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.request.suggest;

import java.util.Collections;
import java.util.List;

import com.b2international.snowowl.core.domain.Concept;
import com.b2international.snowowl.core.domain.PageableCollectionResource;

/**
 * @since 7.7
 */
public final class Suggestions extends PageableCollectionResource<Concept> {

	private static final long serialVersionUID = 1L;
	
	private final List<String> topTokens;

	public Suggestions(final List<String> topTokens, int limit, int total) {
		super(Collections.emptyList(), null, limit, total);
		this.topTokens = topTokens;
	}
	
	public Suggestions(final List<String> topTokens, List<Concept> items, String searchAfter, int limit, int total) {
		super(items, searchAfter, limit, total);
		this.topTokens = topTokens;
	}
	
	public List<String> getTopTokens() {
		return topTokens;
	}

}
