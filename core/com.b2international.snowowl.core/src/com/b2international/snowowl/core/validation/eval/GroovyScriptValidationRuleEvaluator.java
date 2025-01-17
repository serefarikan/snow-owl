/*
 * Copyright 2017-2020 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.validation.eval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.core.scripts.GroovyScriptEngine;
import com.b2international.snowowl.core.scripts.ScriptEngine;
import com.b2international.snowowl.core.scripts.ScriptSource;
import com.b2international.snowowl.core.validation.rule.ValidationRule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * @since 6.1
 */
public final class GroovyScriptValidationRuleEvaluator implements ValidationRuleEvaluator {

	private Path validationResourcesDirectory;

	public GroovyScriptValidationRuleEvaluator(Path validationResourcesDirectory) {
		this.validationResourcesDirectory = validationResourcesDirectory;
	}
	
	@VisibleForTesting
	public void setValidationResourcesDirectory(Path validationResourcesDirectory) {
		this.validationResourcesDirectory = validationResourcesDirectory;
	}
	
	@Override
	public List<?> eval(BranchContext context, ValidationRule rule, Map<String, Object> filterParams) throws IOException {
		final Path validationRuleFilePath = validationResourcesDirectory.resolve(rule.getImplementation());
		try (final Stream<String> lines = Files.lines(validationRuleFilePath)) {
			
			final String script = lines.collect(Collectors.joining(System.getProperty("line.separator")));
			
			final Builder<String, Object> paramsBuilder = ImmutableMap.<String, Object>builder().put("resourcesDir", validationResourcesDirectory);
			
			if (filterParams != null && !filterParams.isEmpty()) {
				paramsBuilder.putAll(filterParams);
			}
			
			return context.service(ScriptEngine.Registry.class).run(
				GroovyScriptEngine.EXTENSION, 
				context.service(ClassLoader.class), 
				new ScriptSource(validationRuleFilePath.getFileName().toString(), script),
				ImmutableMap.<String, Object>of(
					"ctx", context,
					"params", paramsBuilder.build()
				)
			);
		}
	}

	@Override
	public String type() {
		return "script-groovy";
	}

}
