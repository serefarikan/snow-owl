/*
 * Copyright 2022 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.ecl;

import static org.junit.Assert.assertEquals;

import org.eclipse.xtext.parser.IParser;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.validation.IResourceValidator;
import org.junit.Before;
import org.junit.Test;

import com.b2international.snomed.ecl.EclStandaloneSetup;
import com.b2international.snowowl.core.request.ecl.EclRewriter;
import com.google.inject.Injector;

/**
 * @since 5.4
 */
public class EclRewriterTest {

	public static final String ROOT_CONCEPT = "138875005";
	public static final String HAS_ACTIVE_INGREDIENT = "127489000";
	public static final String SUBSTANCE = "105590001";
	
	private EclRewriter rewriter;
	private EclParser parser;
	private EclSerializer serializer;

	@Before
	public void givenRewriter() {
		final Injector injector = new EclStandaloneSetup().createInjectorAndDoEMFRegistration();
		rewriter = new EclRewriter();
		parser = new DefaultEclParser(injector.getInstance(IParser.class), injector.getInstance(IResourceValidator.class));
		serializer = new DefaultEclSerializer(injector.getInstance(ISerializer.class));
	}
	
	@Test
	public void rewriteNotEqualsAttributeComparison() throws Exception {
		final String rewritten = rewrite(String.format("<%s : %s != <%s", ROOT_CONCEPT, HAS_ACTIVE_INGREDIENT, SUBSTANCE));
		assertEquals(String.format("<%s : %s = ( * MINUS < %s )", ROOT_CONCEPT, HAS_ACTIVE_INGREDIENT, SUBSTANCE), rewritten);
	}

	private String rewrite(String ecl) {
		return serializer.serialize(rewriter.rewrite(parser.parse(ecl)));
	}
	
}
