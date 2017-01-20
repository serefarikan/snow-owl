/*
 * Copyright 2011-2017 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.datastore.request;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.util.Tuples;

import com.b2international.snowowl.core.domain.IComponent;
import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.core.exceptions.ComponentNotFoundException;
import com.b2international.snowowl.snomed.Concept;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.ConstantIdStrategy;
import com.b2international.snowowl.snomed.core.domain.DefinitionStatus;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.core.domain.SubclassDefinitionStatus;
import com.b2international.snowowl.snomed.core.store.SnomedComponents;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;

/**
 * @since 4.5
 */
public final class SnomedConceptCreateRequest extends BaseSnomedComponentCreateRequest {

	@Size(min = 2)
	private List<SnomedDescriptionCreateRequest> descriptions = Collections.emptyList();
	
	@Size(min = 1)
	private List<SnomedRelationshipCreateRequest> relationships = Collections.emptyList();
	
	private List<SnomedRefSetMemberCreateRequest> members = Collections.emptyList();

	@NotNull
	private DefinitionStatus definitionStatus = DefinitionStatus.PRIMITIVE;
	
	@NotNull
	private SubclassDefinitionStatus subclassDefinitionStatus = SubclassDefinitionStatus.NON_DISJOINT_SUBCLASSES;

	SnomedConceptCreateRequest() {
	}
	
	void setDefinitionStatus(DefinitionStatus definitionStatus) {
		this.definitionStatus = definitionStatus;
	}
	
	void setDescriptions(final List<SnomedDescriptionCreateRequest> descriptions) {
		this.descriptions = ImmutableList.copyOf(descriptions);
	}
	
	void setRelationships(final List<SnomedRelationshipCreateRequest> relationships) {
		this.relationships = ImmutableList.copyOf(relationships);
	}
	
	void setMembers(final List<SnomedRefSetMemberCreateRequest> members) {
		this.members = ImmutableList.copyOf(members);
	}

	@Override
	public String execute(TransactionContext context) {
		final Concept concept = convertConcept(context);
		context.add(concept);

		convertDescriptions(context, concept.getId());
		convertRelationships(context, concept.getId());
		convertMembers(context, concept.getId());

		return concept.getId();
	}

	private Concept convertConcept(final TransactionContext context) {
		try {
			final String conceptId = ((ConstantIdStrategy) getIdGenerationStrategy()).getId();
			return SnomedComponents.newConcept()
					.withId(conceptId)
					.withActive(isActive())
					.withModule(getModuleId())
					.withDefinitionStatus(definitionStatus)
					.withExhaustive(subclassDefinitionStatus.isExhaustive())
					.build(context);
		} catch (final ComponentNotFoundException e) {
			throw e.toBadRequestException();
		}
	}

	private void convertDescriptions(TransactionContext context, final String conceptId) {
		final Set<String> requiredDescriptionTypes = newHashSet(Concepts.FULLY_SPECIFIED_NAME, Concepts.REFSET_DESCRIPTION_ACCEPTABILITY_PREFERRED);
		final Multiset<String> preferredLanguageRefSetIds = HashMultiset.create();
		final Set<String> synonymAndDescendantIds = getSynonymAndDescendantIds(context);

		for (final SnomedDescriptionCreateRequest descriptionRequest : descriptions) {

			descriptionRequest.setConceptId(conceptId);

			if (null == descriptionRequest.getModuleId()) {
				descriptionRequest.setModuleId(getModuleId());
			}

			descriptionRequest.execute(context);

			final String typeId = descriptionRequest.getTypeId();

			if (synonymAndDescendantIds.contains(typeId)) {
				for (final Entry<String, Acceptability> acceptability : descriptionRequest.getAcceptability().entrySet()) {
					if (Acceptability.PREFERRED.equals(acceptability.getValue())) {
						preferredLanguageRefSetIds.add(acceptability.getKey());
						requiredDescriptionTypes.remove(Concepts.REFSET_DESCRIPTION_ACCEPTABILITY_PREFERRED);
					}
				}
			}

			requiredDescriptionTypes.remove(typeId);
		}

		if (!requiredDescriptionTypes.isEmpty()) {
			throw new BadRequestException("At least one fully specified name and one preferred term must be supplied with the concept.");
		}

		for (final com.google.common.collect.Multiset.Entry<String> languageRefSetIdOccurence : preferredLanguageRefSetIds.entrySet()) {
			if (languageRefSetIdOccurence.getCount() > 1) {
				throw new BadRequestException("More than one preferred term has been added for language reference set %s.", languageRefSetIdOccurence.getElement());				
			}
		}
	}
	
	private Set<String> getSynonymAndDescendantIds(TransactionContext context) {
		final SnomedConcepts concepts = SnomedRequests.prepareGetSynonyms().build().execute(context);
		return FluentIterable.from(concepts).transform(IComponent.ID_FUNCTION).toSet();
	}

	private void convertRelationships(final TransactionContext context, String conceptId) {
		final Set<Pair<String, CharacteristicType>> requiredRelationships = newHashSet(Tuples.pair(Concepts.IS_A, CharacteristicType.STATED_RELATIONSHIP));
		
		for (final SnomedRelationshipCreateRequest relationshipRequest : relationships) {
			relationshipRequest.setSourceId(conceptId);
			
			if (null == relationshipRequest.getModuleId()) {
				relationshipRequest.setModuleId(getModuleId());
			}
			
			relationshipRequest.execute(context);
			
			requiredRelationships.remove(Tuples.pair(relationshipRequest.getTypeId(), relationshipRequest.getCharacteristicType()));
		}
		
		if (!requiredRelationships.isEmpty()) {
			throw new BadRequestException("The following relationships must be supplied with the concept [%s].", Joiner.on(",").join(requiredRelationships));
		}
	}
	
	private void convertMembers(final TransactionContext context, final String conceptId) {
		for (final SnomedRefSetMemberCreateRequest memberRequest : members) {
			memberRequest.setReferencedComponentId(conceptId);
			if (null == memberRequest.getModuleId()) {
				memberRequest.setModuleId(getModuleId());
			}
			
			memberRequest.execute(context);
		}
	}
	
	@Override
	public Collection<SnomedComponentCreateRequest> getNestedRequests() {
		return ImmutableList.<SnomedComponentCreateRequest>builder()
			.add(this)
			.addAll(descriptions)
			.addAll(relationships)
			.build();
	}

	/**
	 * @return all parent concept IDs from the relationship create requests.
	 */
	Set<String> getParents() {
		return relationships.stream().filter(req -> Concepts.IS_A.equals(req.getTypeId())).map(req -> req.getDestinationId()).collect(Collectors.toSet());
	}
}
