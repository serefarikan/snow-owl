/*
 * Copyright 2020-2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.codesystem;

import java.util.*;
import java.util.stream.Collectors;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.commons.options.Options;
import com.b2international.index.revision.BaseRevisionBranching;
import com.b2international.index.revision.RevisionBranch;
import com.b2international.index.revision.RevisionBranch.BranchState;
import com.b2international.snowowl.core.ResourceURI;
import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.branch.BranchInfo;
import com.b2international.snowowl.core.internal.ResourceDocument;
import com.b2international.snowowl.core.request.BaseResourceConverter;
import com.b2international.snowowl.core.uri.ResourceURIPathResolver;
import com.b2international.snowowl.core.version.Version;
import com.b2international.snowowl.core.version.Versions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;

/**
 * @since 7.6
 */
public final class CodeSystemConverter extends BaseResourceConverter<ResourceDocument, CodeSystem, CodeSystems> {

	public CodeSystemConverter(ServiceProvider context, Options expand, List<ExtendedLocale> locales) {
		super(context, expand, locales);
	}

	@Override
	protected CodeSystems createCollectionResource(List<CodeSystem> results, String searchAfter, int limit, int total) {
		return new CodeSystems(results, searchAfter, limit, total);
	}

	@Override
	protected CodeSystem toResource(ResourceDocument doc) {
		CodeSystem codeSystem = new CodeSystem();
		
		codeSystem.setId(doc.getId());
		codeSystem.setUrl(doc.getUrl());
		codeSystem.setTitle(doc.getTitle());
		codeSystem.setLanguage(doc.getLanguage());
		codeSystem.setDescription(doc.getDescription());
		codeSystem.setStatus(doc.getStatus());
		codeSystem.setCopyright(doc.getCopyright());
		codeSystem.setOwner(doc.getOwner());
		codeSystem.setContact(doc.getContact());
		codeSystem.setUsage(doc.getUsage());
		codeSystem.setPurpose(doc.getPurpose());
		
		codeSystem.setOid(doc.getOid());
		codeSystem.setBranchPath(doc.getBranchPath());
		codeSystem.setToolingId(doc.getToolingId());
		codeSystem.setExtensionOf(doc.getExtensionOf());
		codeSystem.setUpgradeOf(doc.getUpgradeOf());
		codeSystem.setSettings(doc.getSettings());
		
		return codeSystem;
	}
	
	@Override
	protected void expand(List<CodeSystem> results) {
		if (expand().isEmpty()) {
			return;
		}
		
		expandAvailableUpgrades(results);
		expandExtensionOfBranchState(results);
		expandUpgradeOfBranchState(results);
	}

	private void expandExtensionOfBranchState(List<CodeSystem> results) {
		if (!expand().containsKey(CodeSystem.Expand.EXTENSION_OF_BRANCH_INFO)) {
			return;
		}
		
		// extensionOf branches are the parent branches of the CodeSystem, so simple branch state calculation is enough
		BaseRevisionBranching branching = context().service(BaseRevisionBranching.class);
		for (CodeSystem result : results) {
			RevisionBranch branch = branching.getBranch(result.getBranchPath());
			BranchState branchState = branching.getBranchState(branch);
			result.setExtensionOfBranchInfo(new BranchInfo(branch.getPath(), branchState, branch.getBaseTimestamp(), branch.getHeadTimestamp()));
		}
	}
	
	private void expandUpgradeOfBranchState(List<CodeSystem> results) {
		if (!expand().containsKey(CodeSystem.Expand.UPGRADE_OF_BRANCH_INFO)) {
			return;
		}
		
		final List<ResourceURI> upgradeOfURIs = results.stream()
				.filter(codeSystem -> codeSystem.getUpgradeOf() != null)
				.map(codeSystem -> codeSystem.getUpgradeOf())
				.collect(Collectors.toList());
		
		// nothing to expand, quit early
		if (upgradeOfURIs.isEmpty()) {
			return;
		}
		
		final List<String> upgradeOfBranches = context().service(ResourceURIPathResolver.class).resolve(context(), upgradeOfURIs);
		
		final Map<ResourceURI, String> branchesByUpgradeOf = Maps.newHashMap();
		Iterator<ResourceURI> uriIterator = upgradeOfURIs.iterator();
		Iterator<String> branchIterator = upgradeOfBranches.iterator();
		while (uriIterator.hasNext() && branchIterator.hasNext()) {
			ResourceURI uri = uriIterator.next();
			String branch = branchIterator.next();
			branchesByUpgradeOf.put(uri, branch);
		}

		BaseRevisionBranching branching = context().service(BaseRevisionBranching.class);
		for (CodeSystem result : results) {
			String upgradeOfBranchPath = branchesByUpgradeOf.get(result.getUpgradeOf());
			if (!Strings.isNullOrEmpty(upgradeOfBranchPath)) {
				RevisionBranch branch = branching.getBranch(result.getBranchPath());
				BranchState branchState = branching.getBranchState(result.getBranchPath(), upgradeOfBranchPath);
				result.setUpgradeOfBranchInfo(new BranchInfo(branch.getPath(), branchState, branch.getBaseTimestamp(), branch.getHeadTimestamp()));
			}
		}
	}

	private void expandAvailableUpgrades(List<CodeSystem> results) {
		if (!expand().containsKey(CodeSystem.Expand.AVAILABLE_UPGRADES)) {
			return;
		}
		
		final Set<ResourceURI> parentResources = results.stream()
			.map(CodeSystem::getExtensionOf)
			.filter(uri -> uri != null)
			.collect(Collectors.toSet());
		
		final Versions parentVersions = CodeSystemRequests.prepareSearchVersion()
			.all()
			.filterByResources(parentResources)
			.build()
			.execute(context());
		
		final TreeMultimap<ResourceURI, Version> versionsByResource = TreeMultimap.create(
				Comparator.naturalOrder(), 
				Comparator.comparing(Version::getEffectiveTime));
		
		versionsByResource.putAll(Multimaps.index(parentVersions, Version::getResource));
		
		for (final CodeSystem result : results) {
			final ResourceURI extensionOf = result.getExtensionOf();
			
			// skip if there is not dependency set in extensionOf
			// or if this is an upgrade CodeSystem
			// or the CodeSystem already has an upgrade
			if (extensionOf == null || result.getUpgradeOf() != null || hasUpgrade(result, results)) {
				// always set the field if user expands it
				result.setAvailableUpgrades(List.of());
				continue;
			}
			
			final ResourceURI resource = extensionOf.withoutPath();
			final String version = extensionOf.getPath();
			
			final NavigableSet<Version> candidates = versionsByResource.get(resource);
			
			final Optional<Version> currentExtensionVersion = candidates.stream()
					.filter(v -> v.getVersion().equals(version))
					.findFirst();
			
			final Optional<List<ResourceURI>> upgradeUris = currentExtensionVersion.map(currentVersion -> {
				final SortedSet<Version> upgradeVersions = candidates.tailSet(currentVersion, false);
				return upgradeVersions.stream()
						.map(upgradeVersion -> upgradeVersion.getResource())
						.collect(Collectors.toList());
			});
	
			result.setAvailableUpgrades(upgradeUris.orElseGet(List::of));
		}			
	}

	private boolean hasUpgrade(CodeSystem result, List<CodeSystem> results) {
		// first check the results
		return results
			.stream()
			.filter(cs -> result.getResourceURI().equals(cs.getUpgradeOf()))
			.findFirst()
			.or(() -> {
				// then the index
				return CodeSystemRequests.prepareSearchCodeSystem()
					.one()
					.filterByUpgradeOf(result.getResourceURI())
					.build()
					.execute(context())
					.first();
			})
			.isPresent();
	}

}
