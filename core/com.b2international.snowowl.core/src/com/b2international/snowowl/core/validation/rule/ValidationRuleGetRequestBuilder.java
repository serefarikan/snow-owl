/*
 * Copyright 2017-2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.validation.rule;

import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.request.GetResourceRequestBuilder;
import com.b2international.snowowl.core.request.SystemRequestBuilder;

/**
 * @since 6.0
 */
public final class ValidationRuleGetRequestBuilder extends
		GetResourceRequestBuilder<ValidationRuleGetRequestBuilder, ValidationRuleSearchRequestBuilder, ServiceProvider, ValidationRules, ValidationRule>
		implements SystemRequestBuilder<ValidationRule> {

	ValidationRuleGetRequestBuilder(final String ruleId) {
		super(new ValidationRuleGetRequest(ruleId));
	}

}
