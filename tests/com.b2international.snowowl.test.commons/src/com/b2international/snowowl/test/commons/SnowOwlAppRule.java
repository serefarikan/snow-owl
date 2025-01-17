/*
 * Copyright 2011-2022 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.test.commons;

import java.io.File;
import java.nio.file.Path;

import org.junit.rules.ExternalResource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.MountableFile;

import com.b2international.commons.FileUtils;
import com.b2international.commons.exceptions.AlreadyExistsException;
import com.b2international.index.IndexClientFactory;
import com.b2international.index.IndexResource;
import com.b2international.index.es.EsIndexClientFactory;
import com.b2international.index.es.EsNode;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.SnowOwl;
import com.b2international.snowowl.core.config.IndexConfiguration;
import com.b2international.snowowl.core.config.RepositoryConfiguration;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.core.identity.IdentityProvider;
import com.b2international.snowowl.core.identity.IdentityWriter;
import com.b2international.snowowl.core.setup.Plugin;
import com.b2international.snowowl.core.util.PlatformUtil;
import com.b2international.snowowl.test.commons.rest.RestExtensions;

/**
 * Bootstraps a {@link SnowOwl} and runs it before test method execution. After all test execution finished, shuts the application down.
 * <p>
 * Usage:
 *
 * <pre>
 * &#064;RunWith(Suite.class)
 * public class SnowOwlAppTestSuite {
 *
 * 	&#064;ClassRule
 * 	public static final SnowOwlAppRule appRule = SnowOwlAppRule.snowOwl();
 *
 * }
 * </pre>
 *
 * Usage example with configuration, the configuration file is at the same location at the SnowOwlAppTestSuite class.
 *
 * <pre>
 * &#064;RunWith(Suite.class)
 * public class SnowOwlAppTestSuite {
 *
 * 	&#064;ClassRule
 * 	public static final SnowOwlAppRule appRule = SnowOwlAppRule.snowOwl().config(PlatformUtil.toAbsolutePath(SnowOwlAppTestSuite.class, &quot;test-config.json&quot;));
 *
 * }
 * </pre>
 *
 * @since 3.3
 * @see PlatformUtil#toAbsolutePath(Class, String)
 * @see #snowOwl()
 */
public class SnowOwlAppRule extends ExternalResource {

//	private final static Logger LOGGER = LogManager.getLogger(SnowOwlAppRule.class);
	
	private boolean clearResources = true;
	private Plugin[] plugins;
	private SnowOwl snowowl;
	private ElasticsearchContainer container;

	private SnowOwlAppRule() {
//		String requestLoggerLevelProperty = System.getProperty("request.logger.level");
//		if (!Strings.isNullOrEmpty(requestLoggerLevelProperty)) {
//			LOGGER.info("Using the system property 'request.logger.level' to set the request logger level to {}. Default level is INFO.", requestLoggerLevelProperty);
//			Level requestLoggerLevel = Level.toLevel(requestLoggerLevelProperty, Level.INFO);
//			
//			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
//			ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("request");
//			rootLogger.setLevel(requestLoggerLevel);
//		}
	}

	/**
	 * Sets Snow Owl's HOME variable to the given absolute path.
	 * 
	 * @param homePath
	 * @return
	 */
	public SnowOwlAppRule home(Path homePath) {
		if (homePath != null) {
			System.setProperty(SnowOwl.SO_PATH_HOME, homePath.toString());
		}
		return this;
	}
	
	/**
	 * Sets the absolute configuration directory path to the given argument.
	 *
	 * @param configPath
	 * @return
	 */
	public SnowOwlAppRule config(Path configPath) {
		if (configPath != null) {
			System.setProperty(SnowOwl.SO_PATH_CONF, configPath.toString());
		}
		return this;
	}

	/**
	 * Set whether to clear the {@link SnowOwlConfiguration#getResourceDirectory()} or not.
	 *
	 * @param clearResources
	 * @return
	 */
	public SnowOwlAppRule clearResources(boolean clearResources) {
		this.clearResources = clearResources;
		return this;
	}

	/**
	 * Defines additional {@link Plugin} instances to be part of the setup process.
	 * @param plugins
	 * @return
	 */
	public SnowOwlAppRule plugins(Plugin...plugins) {
		this.plugins = plugins;
		return this;
	}

	@Override
	protected void before() throws Throwable {
		super.before();
		snowowl = SnowOwl.create(this.plugins);
		if (clearResources) {
			final File resourceDirectory = snowowl.getEnviroment().getDataPath().toFile();
			FileUtils.cleanDirectory(resourceDirectory);
		}
		
		// modify the Snow Owl configuration values if useDocker is defined as JVM argument
		String testElasticsearchContainer = System.getProperty(IndexResource.ES_USE_TEST_CONTAINER_VARIABLE);
		if (testElasticsearchContainer != null) {
			if (testElasticsearchContainer.isEmpty()) {
				testElasticsearchContainer = IndexResource.DEFAULT_ES_DOCKER_IMAGE;
			}
			// initialize an Elasticsearch test container
			container = new ElasticsearchContainer(testElasticsearchContainer);
			container.withEnv("rest.action.multi.allow_explicit_index", "false");
			final MountableFile localSynonymsFilePath = MountableFile.forHostPath(EsIndexClientFactory.DEFAULT_PATH.resolve(IndexClientFactory.DEFAULT_CLUSTER_NAME).resolve(EsNode.CONFIG_DIR).resolve(EsNode.SYNONYMS_FILE));
			final String containerSynonymsFilePath = "/usr/share/elasticsearch/config/" + EsNode.SYNONYMS_FILE;
			container.withCopyFileToContainer(localSynonymsFilePath, containerSynonymsFilePath);
			container.start();
			
			IndexConfiguration index = snowowl.getConfiguration().getModuleConfig(RepositoryConfiguration.class).getIndexConfiguration();
			index.setClusterUrl("https://" + container.getHttpHostAddress());
			index.setClusterUsername("elastic");
			index.setClusterPassword(ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD);
			index.setSslContext(container.createSslContextFromCa());
		}
		
		snowowl.bootstrap();
		snowowl.run();
		// inject the test user to the current identity provider
		try {
			((IdentityWriter) ApplicationContext.getInstance().getServiceChecked(IdentityProvider.class)).addUser(RestExtensions.USER, RestExtensions.PASS);
		} catch (AlreadyExistsException e) {
			// ignore existing user
		}
	}

	@Override
	protected void after() {
		super.after();
		
		snowowl.shutdown();
		snowowl = null;
		
		if (container != null) {
			container.stop();
			container.close();
			container = null;
		}
	}

	/**
	 * Constructs a new {@link SnowOwlAppRule}.
	 *
	 * @return
	 */
	public static SnowOwlAppRule snowOwl() {
		return snowOwl(null);
	}
	
	public static SnowOwlAppRule snowOwl(Class<?> testSuiteClass) {
		final Path configPath = testSuiteClass != null ? PlatformUtil.toAbsolutePath(testSuiteClass, "/configuration") : null;
		return new SnowOwlAppRule().config(configPath);
	}

}
