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
package com.b2international.snowowl.core.bundle;

import java.util.Collection;
import java.util.List;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.commons.options.Options;
import com.b2international.snowowl.core.Resource;
import com.b2international.snowowl.core.ResourceTypeConverter;
import com.b2international.snowowl.core.Resources;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.internal.ResourceDocument;
import com.b2international.snowowl.core.plugin.Component;
import com.b2international.snowowl.core.request.ResourceRequests;
import com.b2international.snowowl.core.request.expand.ResourceExpander;

/**
 * @since 8.0
 */
@Component
public final class BundleResourceTypeConverter implements ResourceTypeConverter {

	@Override
	public String getResourceType() {
		return Bundle.RESOURCE_TYPE;
	}

	@Override
	public Resource toResource(ResourceDocument doc) {
		return Bundle.from(doc);
	}
	
	@Override
	public Integer getRank() {
		return 1;
	}
	
	@Override
	public <T extends Resource> void expand(RepositoryContext context, Options expand, List<ExtendedLocale> locales, Collection<T> results) {
		if (expand.containsKey("content")) {
			final Options expandOptions = expand.getOptions("content");
			// allow expanding content via content expansion, for now hit count only
			results.forEach(bundle -> {
				final Resources resources = ResourceRequests.prepareSearch()
						.filterByBundleAncestorId(bundle.getId())
						.setLimit(expandOptions.containsKey("limit") ? expandOptions.get("limit", Integer.class) : ResourceExpander.DEFAULT_LIMIT)
						.build()
						.execute(context);
				bundle.setProperties("content", resources);
			});
		}
	}

}
