/**
* Copyright (c) otcframework.org
*
* @author  Franklin J Abel
* @version 1.0
* @since   2020-06-08 
*
* This file is part of the OTC framework.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*/
package org.otcframework.common.config;

import org.otcframework.common.config.exception.OtcConfigException;
import org.otcframework.common.exception.OtcException;
import org.otcframework.common.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The Enum OtcConfig.
 */
public enum OtcConfig {

	/**
	 * The instance.
	 */
	INSTANCE;

	private static final Logger LOGGER = LoggerFactory.getLogger(OtcConfig.class);

	private static final String OTC_HOME_ENV_VAR = "OTC_HOME";

	private static final String OTC_UNITTEST_FOLDER = "otc-unittest" + File.separator;
	private static final String OTC_LIB_FOLDER = "lib" + File.separator;
	private static final String OTC_SRC_FOLDER = "src" + File.separator;
	private static final String OTC_CONFIG_FOLDER = "config" + File.separator;
	private static final String OTC_CONFIG_FILE = "otc.yaml";
	public static final String OTC_TMD_FOLDER = "tmd" + File.separator;
	public static final String OTC_RESULTS_EXPECTED_FOLDER = "results_expected" + File.separator;
	private static boolean isDefaultLocations = true;
	private static final String otcHome;

	private static final YamlConfig YAML_CONFIG;
	private static String sourceCodeDirectory;
	private static String configuredTmdDirectory;
	private static final Integer DEFAULT_CYCLIC_REFERENCE_DEPTH = 2;

	/**
	 * Instantiates a new otc config.
	 */
	OtcConfig() {
	}

	static {
		otcHome = getOtcHome();
		String otcConfigLocation = getOtcConfigLocation();
		YAML_CONFIG = loadOtcConfig(otcConfigLocation);

		// -- load sourceCodeLocation and tmdLocation properties
		if (Objects.nonNull(YAML_CONFIG.compiler) && Objects.nonNull(YAML_CONFIG.compiler.paths)) {
			sourceCodeDirectory = YAML_CONFIG.compiler.paths.sourceCodeDirectory;
			configuredTmdDirectory = YAML_CONFIG.compiler.paths.tmdDirectory;
			boolean isSourceCodeDirectoryDefined = !CommonUtils.isTrimmedAndEmpty(sourceCodeDirectory);
			boolean isTmdDirectoryDefined = !CommonUtils.isTrimmedAndEmpty(configuredTmdDirectory);
			if (isSourceCodeDirectoryDefined != isTmdDirectoryDefined) {
				throw new OtcConfigException("", String.format("Either BOTH or NONE of this set of 2 properties " +
								"('compiler.paths.sourceCodeDirectory:', 'compiler.paths.tmdDirectory:) should be" +
								" defined in the '%s%s' file.", otcConfigLocation, OTC_CONFIG_FILE));
			}
			if (isSourceCodeDirectoryDefined) {
				isDefaultLocations = false;
			}
			if (getCleanupBeforeCompile() && isDefaultLocations) {
				LOGGER.warn("You have set 'compiler.cleanupBeforeCompile' property to true. " +
						"Updated source-code if any in ${OTC_HOME} '{}' will be lost during clean-up.", otcHome);
			}
		}
		sourceCodeDirectory = getSourceCodeLocation(sourceCodeDirectory);
		Set<String> filteredPackages = YAML_CONFIG.filterPackages;
		PackagesFilterUtil.setFilteredPackages(filteredPackages);
	}

	public static boolean isDefaultLocations() {
		return isDefaultLocations;
	}

	/**
	 * Gets the otc lib location.
	 *
	 * @return the otc lib location
	 */
	public static String getOtcLibDirectoryPath() {
		if (Objects.nonNull(YAML_CONFIG.compiler) && Objects.nonNull(YAML_CONFIG.compiler.paths) &&
				Objects.nonNull(YAML_CONFIG.compiler.paths.libDirectory)) {
			return YAML_CONFIG.compiler.paths.libDirectory;
		}
		return otcHome + OTC_LIB_FOLDER;
	}

	public static boolean getCleanupBeforeCompile() {
		if (Objects.nonNull(YAML_CONFIG.compiler) && Objects.nonNull(YAML_CONFIG.compiler.cleanupBeforeCompile)) {
			return YAML_CONFIG.compiler.cleanupBeforeCompile;
		}
		return false;
	}

	/**
	 * Gets the otc source location.
	 *
	 * @return the otc source location
	 */
	public static String getUnitTestDirectoryPath() {
		return otcHome + OTC_UNITTEST_FOLDER;
	}

	/**
	 * Gets the source code location.
	 *
	 * @return the source code location
	 */
	public static String getSourceCodeDirectoryPath() {
		return sourceCodeDirectory;
	}

	public static Integer getCyclicReferenceDepth() {
		if (Objects.nonNull(YAML_CONFIG.compiler) && Objects.nonNull(YAML_CONFIG.compiler.cyclicReferenceDepthLimit)
				&& YAML_CONFIG.compiler.cyclicReferenceDepthLimit > 0) {
			return YAML_CONFIG.compiler.cyclicReferenceDepthLimit;
		}
		return DEFAULT_CYCLIC_REFERENCE_DEPTH;
	}

	/**
	 * Gets the test case expected result location.
	 *
	 * @return the test case expected result location
	 */
	public static String getTestCaseExpectedResultDirectoryPath() {
		String expectedLocation = otcHome + File.separator + OTC_RESULTS_EXPECTED_FOLDER;
		OtcUtils.creteDirectory(expectedLocation);
		return expectedLocation;
	}

	/**
	 * Gets the concrete types.
	 *
	 * @return the concrete types
	 */
	public static Map<Class<?>, String> getConcreteTypes() {
		Map<String, String> yamlConcreteTypes = YAML_CONFIG.concreteTypes;
		if (yamlConcreteTypes != null) {
			IdentityHashMap<Class<?>, String> concreteTypes = new IdentityHashMap<>(yamlConcreteTypes.size());
			yamlConcreteTypes.forEach((key, value) -> {
				Class<?> clz = OtcUtils.loadClass(key);
				concreteTypes.put(clz, value);
			});
			return concreteTypes;
		}
		return null;
	}

	public static final class YamlConfig {
		public CompilerProps compiler;
		public Map<String, String> concreteTypes;
		public Set<String> filterPackages;

		public static final class CompilerProps {
			public Boolean cleanupBeforeCompile;
			public Integer cyclicReferenceDepthLimit;
			public Paths paths;

			public static final class Paths {
				public String libDirectory;
				public String sourceCodeDirectory;
				public String tmdDirectory;
			}
		}
	}

	private static YamlConfig loadOtcConfig(String otcConfigLocation) {
		try {
			LOGGER.info("Loading OTC config file '{}' from classpath - {}", OTC_CONFIG_FILE, otcConfigLocation);
			return YamlSerializationHelper.deserialize(otcConfigLocation + OTC_CONFIG_FILE, YamlConfig.class);
		} catch (Exception ex) {
			throw new OtcConfigException(ex);
		}
	}

	private static String getOtcHome() {
		Map<String, String> sysEnv = System.getenv();
		if (!sysEnv.containsKey(OTC_HOME_ENV_VAR)) {
			throw new OtcConfigException("",
					"Oops... Cannot proceed - '" + OTC_HOME_ENV_VAR + "' not set! Please set '" +
							OTC_HOME_ENV_VAR + "' environment variable.");
		}
		String otcHome = sysEnv.get(OTC_HOME_ENV_VAR);
		if (CommonUtils.isTrimmedAndEmpty(otcHome)) {
			throw new OtcException("", "Oops... Environment variable '" + OTC_HOME_ENV_VAR + "' not set! ");
		}
		if (!otcHome.endsWith(File.separator)) {
			otcHome += File.separator;
		}
		return otcHome;
	}

	private static String getResourceLocation(String resourceName) {
		try {
			URL otcConfigUrl = new URL(OtcConfig.class.getClassLoader().getResource(".") + resourceName);
			if (Paths.get(otcConfigUrl.toURI()).toFile().exists()) {
				return Paths.get(OtcConfig.class.getClassLoader().getResource(".").toURI()) + File.separator;
			}
		} catch (URISyntaxException | MalformedURLException | NullPointerException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	public static String getOtcConfigLocation() {
		String otcConfigLocation = getResourceLocation(OTC_CONFIG_FILE);
		if (otcConfigLocation != null) {
			return otcConfigLocation;
		}
		return otcHome + OTC_CONFIG_FOLDER;
	}

	private static String getSourceCodeLocation(String configuredPath) {
		String path = configuredPath;
		if (!CommonUtils.isTrimmedAndEmpty(configuredPath)) {
			if (!configuredPath.endsWith(File.separator)) {
				path += File.separator;
			}
			LOGGER.info("Picked configured 'compiler.paths.sourceCodeDirectory:' location - {} ", path);
		} else {
			path = otcHome + OTC_SRC_FOLDER;
			LOGGER.info("'compiler.paths.sourceCodeDirectory:' not configured - picking ${OTC_HOME} 'src' folder location - {} ",
					path);
		}
		return path;
	}

	public static String getConfiguredTmdLocation() {
		if (!CommonUtils.isTrimmedAndEmpty(configuredTmdDirectory)) {
			if (!configuredTmdDirectory.endsWith(File.separator)) {
				configuredTmdDirectory += File.separator;
			}
			configuredTmdDirectory += OTC_TMD_FOLDER;
			LOGGER.info("Picked configured \"compiler.paths.tmdDirectory:\" location - {}", configuredTmdDirectory);
			return configuredTmdDirectory;
		}
		String otcHomeTmdLocation = otcHome + OTC_TMD_FOLDER;
		LOGGER.info("\"compiler.paths.tmdDirectory:\" location not configured - picking ${OTC_HOME} 'tmd' location - {}",
				otcHomeTmdLocation);
		return otcHomeTmdLocation;
	}

	public static String getClasspathTmdLocation() {
		String otcClasspathTmdLocation = getResourceLocation(OTC_TMD_FOLDER);
		if (!CommonUtils.isTrimmedAndEmpty(otcClasspathTmdLocation)) {
			if (!otcClasspathTmdLocation.endsWith(File.separator)) {
				otcClasspathTmdLocation += File.separator;
			}
			otcClasspathTmdLocation += OTC_TMD_FOLDER;
			LOGGER.info("Found './tmd/' folder in classpath : {}", otcClasspathTmdLocation);
			return otcClasspathTmdLocation;
		}
		otcClasspathTmdLocation = otcHome + OTC_TMD_FOLDER;
		LOGGER.info("Did not find './tmd/' folder in classpath - returning OTC_HOME 'tmd' location - {} ", otcClasspathTmdLocation);
		return otcClasspathTmdLocation;
	}
}
