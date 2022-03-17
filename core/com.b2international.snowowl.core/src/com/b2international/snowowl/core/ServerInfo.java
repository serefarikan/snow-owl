/*
 * Copyright 2017-2019 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core;

import java.io.Serializable;

import com.b2international.index.ClusterStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 5.8
 */
public final class ServerInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final String version;
	private final String description;
	private final Repositories repositories;
	private final ClusterStatus cluster;
	
	public ServerInfo(String version, String description, Repositories repositories, ClusterStatus cluster) {
		this.version = version;
		this.description = description;
		this.repositories = repositories;
		this.cluster = cluster;
	}

	@JsonProperty
	public String version() {
		return version;
	}

	@JsonProperty
	public String description() {
		return description;
	}
	
	@JsonProperty
	public Repositories repositories() {
		return repositories;
	}
	
	@JsonProperty
	public ClusterStatus cluster() {
		return cluster;
	}
	
}
