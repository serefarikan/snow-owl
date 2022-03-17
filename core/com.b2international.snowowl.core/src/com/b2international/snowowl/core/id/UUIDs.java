/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors and B2i Healthcare. See
 * GitHub history for details.
 */
package com.b2international.snowowl.core.id;

public class UUIDs {

    private static final RandomBasedUUIDGenerator RANDOM_UUID_GENERATOR = new RandomBasedUUIDGenerator();
    private static final TimeBasedUUIDGenerator TIME_UUID_GENERATOR = new TimeBasedUUIDGenerator();

    /** Generates a time-based UUID (similar to Flake IDs), which is preferred when generating an ID to be indexed into a Lucene index as
     *  primary key.  The id is opaque and the implementation is free to change at any time! */
    public static String base64UUID() {
        return TIME_UUID_GENERATOR.getBase64UUID();
    }
    
    /** Returns a Base64 encoded version of a Version 4.0 compatible UUID as defined here: http://www.ietf.org/rfc/rfc4122.txt, using a
     *  private {@code SecureRandom} instance */
    public static String randomBase64UUID() {
        return RANDOM_UUID_GENERATOR.getBase64UUID();
    }
}
