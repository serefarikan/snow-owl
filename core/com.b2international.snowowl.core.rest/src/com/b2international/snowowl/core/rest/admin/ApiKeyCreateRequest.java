/*
 * Copyright 2023 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.rest.admin;

import java.util.List;

/**
 * @since 8.9.1
 */
public class ApiKeyCreateRequest {

	private String username;
	private String password;
	private String token;
	private String expiration = "1d";
	private List<String> permissions;

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
	
	public String getToken() {
		return token;
	}
	
	public String getExpiration() {
		return expiration;
	}
	
	public void setExpiration(String expiration) {
		this.expiration = expiration;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public List<String> getPermissions() {
		return permissions;
	}
	
	public void setPermissions(List<String> permissions) {
		this.permissions = permissions;
	}
	
}
