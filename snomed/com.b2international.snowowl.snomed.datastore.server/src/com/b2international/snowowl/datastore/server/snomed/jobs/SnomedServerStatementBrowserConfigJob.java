/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.datastore.server.snomed.jobs;

import com.b2international.snowowl.datastore.server.snomed.SnomedDatastoreServerActivator;
import com.b2international.snowowl.datastore.server.snomed.index.SnomedServerStatementBrowser;
import com.b2international.snowowl.datastore.serviceconfig.IndexServiceTrackingConfigJob;
import com.b2international.snowowl.snomed.datastore.SnomedStatementBrowser;
import com.b2international.snowowl.snomed.datastore.index.SnomedIndexService;

/**
 * Job for initializing and registering statement browser service for SNOMED&nbsp;CT ontology on the server side.
 */
public class SnomedServerStatementBrowserConfigJob extends IndexServiceTrackingConfigJob<SnomedStatementBrowser, SnomedIndexService> {

	/**
	 * Creates a new job instance to register predicate browser service for SNOMED&nbsp;CT ontology on the server side.
	 */
	public SnomedServerStatementBrowserConfigJob() {
		super("SNOMED CT server statement browser configuration...", SnomedDatastoreServerActivator.PLUGIN_ID);
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.datastore.serviceconfig.BrowserConfigJob#getBrowserClass()
	 */
	@Override
	protected Class<SnomedStatementBrowser> getTargetServiceClass() {
		return SnomedStatementBrowser.class;
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.datastore.serviceconfig.BrowserConfigJob#getIndexServiceClass()
	 */
	@Override
	protected Class<SnomedIndexService> getIndexServiceClass() {
		return SnomedIndexService.class;
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.datastore.serviceconfig.BrowserConfigJob#createBrowser(com.b2international.snowowl.core.api.index.IIndexService)
	 */
	@Override
	protected SnomedStatementBrowser createServiceImplementation(final SnomedIndexService indexService) {
		return new SnomedServerStatementBrowser(indexService);
	}

}