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
import java.net.URLClassLoader;
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
	private static final String OTC_TARGET_FOLDER = "target" + File.separator;
	private static final String OTC_CONFIG_FILE = "otc.yaml";
	public static final String OTC_TMD_FOLDER = "tmd" + File.separator;
	private static boolean isDefaultLocations = true;
	private static String otcHome;

	private static final YamlConfig YAML_CONFIG;
	private static final URLClassLoader CLZ_LOADER;
	private static String sourceCodeDirectory;
	private static String tmdDirectory;
	private static String targetDirectory;
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
			tmdDirectory = YAML_CONFIG.compiler.paths.tmdDirectory;
			targetDirectory = YAML_CONFIG.compiler.paths.targetDirectory;
			boolean isSourceCodeDirectoryDefined = !CommonUtils.isTrimmedAndEmpty(sourceCodeDirectory);
			boolean isTmdDirectoryDefined = !CommonUtils.isTrimmedAndEmpty(tmdDirectory);
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
		sourceCodeDirectory = initFolder(sourceCodeDirectory, OTC_SRC_FOLDER);
		targetDirectory = initFolder(targetDirectory, OTC_TARGET_FOLDER);
		if (!CommonUtils.isTrimmedAndEmpty(tmdDirectory)) {
			if (!tmdDirectory.endsWith(File.separator)) {
				tmdDirectory += File.separator;
			}
			tmdDirectory += OTC_TMD_FOLDER;
		} else {
			tmdDirectory = otcConfigLocation + OTC_TMD_FOLDER;
		}

		try {
			URL url = new File(targetDirectory).toURI().toURL();
			URL[] urls = new URL[]{url};
			CLZ_LOADER = URLClassLoader.newInstance(urls);
		} catch (MalformedURLException e) {
			throw new OtcConfigException(e);
		}
		Set<String> filteredPackages = YAML_CONFIG.filterPackages;
		PackagesFilterUtil.setFilteredPackages(filteredPackages);
	}

	private static String initFolder(String configuredPath, String defaultPath) {
		String path = configuredPath;
		if (!CommonUtils.isTrimmedAndEmpty(configuredPath)) {
			if (!configuredPath.endsWith(File.separator)) {
				path += File.separator;
			}
		} else {
			path = otcHome + defaultPath;
		}
		return path;
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
		if (!CommonUtils.isTrimmedAndEmpty(sourceCodeDirectory)) {
			return sourceCodeDirectory;
		}
		return otcHome + OTC_SRC_FOLDER;
	}

	public static Integer getCyclicReferenceDepth() {
		if (Objects.nonNull(YAML_CONFIG.compiler) && Objects.nonNull(YAML_CONFIG.compiler.cyclicReferenceDepthLimit)
				&& YAML_CONFIG.compiler.cyclicReferenceDepthLimit > 0) {
			return YAML_CONFIG.compiler.cyclicReferenceDepthLimit;
		}
		return DEFAULT_CYCLIC_REFERENCE_DEPTH;
	}

	/**
	 * Gets the otc tmd location.
	 *
	 * @return the otc tmd location
	 */
	public static String getOtcTmdDirectoryPath() {
		if (!CommonUtils.isTrimmedAndEmpty(tmdDirectory)) {
			return tmdDirectory;
		}
		return otcHome + OTC_TMD_FOLDER;
	}

	/**
	 * Gets the compiled code location.
	 *
	 * @return the compiled code location
	 */
	public static String getTargetDirectoryPath() {
		if (!CommonUtils.isTrimmedAndEmpty(targetDirectory)) {
			return targetDirectory;
		}
		return otcHome + OTC_TARGET_FOLDER;
	}

	/**
	 * Gets the test case expected result location.
	 *
	 * @return the test case expected result location
	 */
	public static String getTestCaseExpectedResultDirectoryPath() {
		String expectedLocation = otcHome + File.separator + "result_expected" + File.separator;
		OtcUtils.creteDirectory(expectedLocation);
		return expectedLocation;
	}

	/**
	 * Gets the target class loader.
	 *
	 * @return the target class loader
	 */
	public static URLClassLoader getTargetClassLoader() {
		return CLZ_LOADER;
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
				public String targetDirectory;
			}
		}
	}

	private static YamlConfig loadOtcConfig(String otcConfigLocation) {
		try {
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

	public static String getOtcConfigLocation() {
		try {
			// -- this below line is to only check if file exists. If file is not present an exception is thrown.
			OtcConfig.class.getClassLoader().getResource("." + File.separator + OTC_CONFIG_FILE);

			return Paths.get(OtcConfig.class.getClassLoader().getResource(".").toURI()).toString() + File.separator;
		} catch (URISyntaxException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return otcHome;
	}
}
