/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.index.write;

import java.io.IOException;
import java.util.Map;

/**
 * @since 4.7
 */
public interface Writer extends AutoCloseable {

	void put(String key, Object object) throws IOException;
	
	void putAll(Map<String, Object> objectByKeys) throws IOException;

	void remove(Class<?> type, String key) throws IOException;
	
	void removeAll(Map<Class<?>, String> keysByType) throws IOException;

	void commit() throws IOException;
	
}
