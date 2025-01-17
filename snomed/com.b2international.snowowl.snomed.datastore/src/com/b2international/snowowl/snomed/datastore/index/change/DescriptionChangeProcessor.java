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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.b2international.index.revision.Revision;
import com.b2international.index.revision.RevisionSearcher;
import com.b2international.index.revision.StagingArea;
import com.b2international.snowowl.core.repository.ChangeSetProcessorBase;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.SnomedDescription;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry.Builder;
import com.b2international.snowowl.snomed.datastore.index.refset.RefSetMemberChange;
import com.b2international.snowowl.snomed.datastore.index.update.ReferenceSetMembershipUpdater;
import com.google.common.collect.*;

/**
 * @since 4.3
 */
public class DescriptionChangeProcessor extends ChangeSetProcessorBase {
	
	private final ReferringMemberChangeProcessor memberChangeProcessor;

	public DescriptionChangeProcessor() {
		super("description changes");
		this.memberChangeProcessor = new ReferringMemberChangeProcessor(SnomedDescription.TYPE);
	}

	@Override
	public void process(StagingArea staging, RevisionSearcher searcher) throws IOException {
		final Map<String, Multimap<Acceptability, RefSetMemberChange>> acceptabilityChangesByDescription = 
				new DescriptionAcceptabilityChangeProcessor().process(staging, searcher);
		
		final Multimap<String, RefSetMemberChange> referringRefSets = HashMultimap.create(memberChangeProcessor.process(staging, searcher));
		
		// (re)index new and dirty descriptions
		final Map<String, SnomedDescriptionIndexEntry> newDescriptionsById = staging
				.getNewObjects(SnomedDescriptionIndexEntry.class)
				.collect(Collectors.toMap(description -> description.getId(), description -> description));
		
		final Map<String, SnomedDescriptionIndexEntry> changedDescriptionsById = staging
				.getChangedRevisions(SnomedDescriptionIndexEntry.class)
				.collect(Collectors.toMap(diff -> diff.newRevision.getId(), diff -> (SnomedDescriptionIndexEntry) diff.newRevision));
		
		final Set<String> changedDescriptionIds = newHashSet(changedDescriptionsById.keySet());
		final Set<String> referencedDescriptionIds = newHashSet(referringRefSets.keySet());
		referencedDescriptionIds.removeAll(newDescriptionsById.keySet());
		changedDescriptionIds.addAll(referencedDescriptionIds);
		
		// load the known descriptions 
		final Iterable<SnomedDescriptionIndexEntry> changedDescriptionHits = searcher.get(SnomedDescriptionIndexEntry.class, changedDescriptionIds);
		final Map<String, SnomedDescriptionIndexEntry> changedDescriptionRevisionsById = Maps.uniqueIndex(changedDescriptionHits, Revision::getId);
		
		// load missing descriptions with only changed acceptability values
		final Set<String> descriptionsToBeLoaded = newHashSet();
		for (String descriptionWithAccepatibilityChange : acceptabilityChangesByDescription.keySet()) {
			if (!newDescriptionsById.containsKey(descriptionWithAccepatibilityChange)
					&& !changedDescriptionIds.contains(descriptionWithAccepatibilityChange)) {
				descriptionsToBeLoaded.add(descriptionWithAccepatibilityChange);
			}
		}
		
		// process changes
		for (final String id : Iterables.concat(newDescriptionsById.keySet(), changedDescriptionIds)) {
			if (newDescriptionsById.containsKey(id)) {
				final SnomedDescriptionIndexEntry description = newDescriptionsById.get(id);
				final Builder doc = SnomedDescriptionIndexEntry.builder(description);
				processChanges(id, doc, null, acceptabilityChangesByDescription.get(id), referringRefSets);
				stageNew(doc.build());
			} else if (changedDescriptionIds.contains(id)) {
				final SnomedDescriptionIndexEntry currentDoc = changedDescriptionRevisionsById.get(id);
				if (currentDoc == null) {
					throw new IllegalStateException(String.format("Current description revision should not be null for: %s", id));
				}
				
				final SnomedDescriptionIndexEntry description = changedDescriptionsById.get(id);
				final Builder doc;
				if (description != null) {
					doc = SnomedDescriptionIndexEntry.builder(description);
				} else {
					doc = SnomedDescriptionIndexEntry.builder(currentDoc);
				}
				
				processChanges(id, doc, currentDoc, acceptabilityChangesByDescription.get(id), referringRefSets);
				stageChange(currentDoc, doc.build());
			} else {
				throw new IllegalStateException(String.format("Description %s is missing from new and dirty maps", id));
			}
		}
		
		// process cascading acceptability changes in unchanged docs
		if (!descriptionsToBeLoaded.isEmpty()) {
			for (SnomedDescriptionIndexEntry unchangedDescription : searcher.get(SnomedDescriptionIndexEntry.class, descriptionsToBeLoaded)) {
				final Builder doc = SnomedDescriptionIndexEntry.builder(unchangedDescription);
				processChanges(unchangedDescription.getId(), doc, unchangedDescription,
						acceptabilityChangesByDescription.get(unchangedDescription.getId()),
						HashMultimap.<String, RefSetMemberChange> create());
				stageChange(unchangedDescription, doc.build());
			}
		}
	}
	
	private void processChanges(final String id, final Builder doc, final SnomedDescriptionIndexEntry currentRevision,
			Multimap<Acceptability, RefSetMemberChange> acceptabilityChanges, Multimap<String, RefSetMemberChange> referringRefSets) {
		final Multimap<Acceptability, String> acceptabilityMap = currentRevision == null ? ImmutableMultimap.<Acceptability, String> of()
				: ImmutableMap.copyOf(currentRevision.getAcceptabilityMap()).asMultimap().inverse();
		
		final Collection<String> preferredLanguageRefSets = newHashSet(acceptabilityMap.get(Acceptability.PREFERRED));
		final Collection<String> acceptableLanguageRefSets = newHashSet(acceptabilityMap.get(Acceptability.ACCEPTABLE));
		
		
		if (acceptabilityChanges != null) {
			collectChanges(acceptabilityChanges.get(Acceptability.PREFERRED), preferredLanguageRefSets);
			collectChanges(acceptabilityChanges.get(Acceptability.ACCEPTABLE), acceptableLanguageRefSets);
		}
		
		// clear acceptability map first then apply new acceptability settings
		doc.acceptabilityMap(Collections.emptyMap());
		
		for (String preferredLanguageRefSet : preferredLanguageRefSets) {
			doc.acceptability(preferredLanguageRefSet, Acceptability.PREFERRED);
		}
		
		for (String acceptableLanguageRefSet : acceptableLanguageRefSets) {
			doc.acceptability(acceptableLanguageRefSet, Acceptability.ACCEPTABLE);
		}
		
		final Collection<String> currentMemberOf = currentRevision == null ? Collections.emptySet()
				: currentRevision.getMemberOf();
		final Collection<String> currentActiveMemberOf = currentRevision == null ? Collections.emptySet()
				: currentRevision.getActiveMemberOf();
		new ReferenceSetMembershipUpdater(referringRefSets.removeAll(id), currentMemberOf, currentActiveMemberOf)
				.update(doc);
	}
	
	private void collectChanges(Collection<RefSetMemberChange> changes, Collection<String> refSetIds) {
		for (final RefSetMemberChange change : changes) {
			switch (change.getChangeKind()) {
				case REMOVED:
					refSetIds.remove(change.getRefSetId());
					break;
				default:
					break;
			}
		}
		
		for (final RefSetMemberChange change : changes) {
			switch (change.getChangeKind()) {
				case ADDED:
					refSetIds.add(change.getRefSetId());
					break;
				default:
					break;
			}
		}
	}
	
}
