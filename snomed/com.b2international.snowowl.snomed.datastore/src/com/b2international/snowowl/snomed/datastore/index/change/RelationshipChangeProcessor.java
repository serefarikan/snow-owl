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
package com.b2international.snowowl.snomed.datastore.index.change;

import static com.google.common.collect.Sets.newHashSet;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.b2international.index.revision.Revision;
import com.b2international.index.revision.RevisionSearcher;
import com.b2international.index.revision.StagingArea;
import com.b2international.snowowl.core.repository.ChangeSetProcessorBase;
import com.b2international.snowowl.snomed.core.domain.SnomedRelationship;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry.Builder;
import com.b2international.snowowl.snomed.datastore.index.refset.RefSetMemberChange;
import com.b2international.snowowl.snomed.datastore.index.update.ReferenceSetMembershipUpdater;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * @since 4.3
 */
public class RelationshipChangeProcessor extends ChangeSetProcessorBase {
	
	private final ReferringMemberChangeProcessor memberChangeProcessor;

	public RelationshipChangeProcessor() {
		super("relationship changes");
		this.memberChangeProcessor = new ReferringMemberChangeProcessor(SnomedRelationship.TYPE);
	}

	@Override
	public void process(StagingArea staging, RevisionSearcher searcher) throws IOException {
		final Multimap<String, RefSetMemberChange> referringRefSets = memberChangeProcessor.process(staging, searcher);
		
		final Set<String> referencedRelationshipIds = newHashSet(referringRefSets.keySet());
		staging
			.getNewObjects(SnomedRelationshipIndexEntry.class)
			.map(SnomedRelationshipIndexEntry::getId)
			.forEach(referencedRelationshipIds::remove);

		final Map<String, SnomedRelationshipIndexEntry> changedRelationshipsById = staging.getChangedRevisions(SnomedRelationshipIndexEntry.class)
				.map(diff -> (SnomedRelationshipIndexEntry) diff.newRevision)
				.collect(Collectors.toMap(relationship -> relationship.getId(), relationship -> relationship));
		final Set<String> changedRelationshipIds = newHashSet(changedRelationshipsById.keySet());
		changedRelationshipIds.addAll(referencedRelationshipIds);
		
		final Iterable<SnomedRelationshipIndexEntry> changedRelationshipHits = searcher.get(SnomedRelationshipIndexEntry.class, changedRelationshipIds);
		final Map<String, SnomedRelationshipIndexEntry> changedRelationshipRevisionsById = Maps.uniqueIndex(changedRelationshipHits, Revision::getId);
		
		for (final String id : changedRelationshipIds) {
			final SnomedRelationshipIndexEntry currentDoc = changedRelationshipRevisionsById.get(id);
			if (currentDoc == null) {
				throw new IllegalStateException(String.format("Current relationship revision should not be null for %s", id));
			}
			
			final SnomedRelationshipIndexEntry relationship = changedRelationshipsById.get(id);
			final Builder doc;
			if (relationship != null) {
				doc = SnomedRelationshipIndexEntry.builder(relationship);
			} else {
				doc = SnomedRelationshipIndexEntry.builder(currentDoc);
			}
			
			final Collection<String> currentMemberOf = currentDoc.getMemberOf();
			final Collection<String> currentActiveMemberOf = currentDoc.getActiveMemberOf();
			new ReferenceSetMembershipUpdater(referringRefSets.removeAll(id), currentMemberOf, currentActiveMemberOf)
					.update(doc);
			
			stageChange(currentDoc, doc.build());
		}
	}
	
}
