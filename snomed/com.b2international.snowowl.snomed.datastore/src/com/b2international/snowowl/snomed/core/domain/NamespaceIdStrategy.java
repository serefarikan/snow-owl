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
package com.b2international.snowowl.snomed.core.domain;

import com.google.common.base.MoreObjects;

/**
 * @since 4.5
 */
public final class NamespaceIdStrategy implements IdGenerationStrategy {

	private static final long serialVersionUID = 1L;
	
	private final String namespace;

	public NamespaceIdStrategy(final String namespace) {
		this.namespace = namespace;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("namespace", namespace).toString();
	}
	
	@Override
	public IdGenerationStrategy toNamespaceStrategy() {
		return this;
	}
	
}
