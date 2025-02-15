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
package org.otcframework.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.otcframework.common.OtcConstants;
import org.otcframework.common.config.OtcConfig;
import org.otcframework.common.dto.OtcChainDto;
import org.otcframework.common.dto.OtcCommandDto;
import org.otcframework.common.dto.RegistryDto;
import org.otcframework.common.dto.RegistryDto.CompiledInfo;
import org.otcframework.common.exception.OtcException;
import org.otcframework.common.exception.OtcUnsupportedJdkException;
import org.otcframework.common.executor.CodeExecutor;
import org.otcframework.common.factory.OtcCommandDtoFactory;
import org.otcframework.common.util.CommonUtils;
import org.otcframework.common.util.OtcReflectionUtil;
import org.otcframework.common.util.OtcUtils;
import org.otcframework.executor.exception.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * The Class OtcRegistryImpl.
 */
public enum OtcRegistryImpl implements OtcRegistry {

	/** The instance. */
	INSTANCE;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(OtcRegistryImpl.class);

	/** The map packaged otc dtos. */
	private static final Map<String, RegistryDto> mapPackagedOtcDtos = new HashMap<>();

	/** The Constant depFileFilter. */
	private static final FileFilter tmdFileFilter = CommonUtils.createFilenameFilter(OtcConstants.OTC_TMD_EXTN);

	/** The Constant objectMapper. */
	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Instantiates a new otc registry impl.
	 */
	private OtcRegistryImpl() {
	}

	/**
	 * register.
	 */
	@Override
	public void register() {
		File directory = new File(OtcConfig.getClasspathTmdLocation());
		File[] files = directory.listFiles(tmdFileFilter);
		if (files == null) {
			return;
		}
		LOGGER.info("Beginning OTC registrations...");
		long startTime = System.nanoTime();
		boolean hasRegistrations = false;
		for (File file : files) {
			if (file.isDirectory()) {
				continue;
			}
			try (FileInputStream fis = new FileInputStream(file)) {
				byte[] contents = new byte[fis.available()];
				int bytesRead = fis.read(contents);
				RegistryDto registryDto = objectMapper.readValue(contents, RegistryDto.class);
				if (registryDto.hasError) {
					LOGGER.error(
							"Ignoring registry of {}. Probable cause: full compilation did not succeed on previous attempt.",
							file.getAbsolutePath());
					continue;
				}
				for (CompiledInfo compiledInfo : registryDto.compiledInfos.values()) {
					// init source
					initOtcCommandDto(compiledInfo.id, compiledInfo.sourceOCDStem, registryDto.sourceClz,
							compiledInfo.sourceOtcChainDto);
					// init target
					initOtcCommandDto(compiledInfo.id, compiledInfo.targetOCDStem, registryDto.targetClz,
							compiledInfo.targetOtcChainDto);
				}
				register(registryDto);
				hasRegistrations = true;
			} catch (IOException e) {
				throw new RegistryException("", e);
			}
		}
		long endTime = System.nanoTime();
		if (hasRegistrations) {
			LOGGER.info("Completed OTC registrations in {} millis.", ((endTime - startTime) / 1000000.0));
		} else {
			LOGGER.info("Nothing to register - no registration files found !!");
		}
	}

	/**
	 * Inits the otc command dto.
	 *
	 * @param id            the id
	 * @param otcCommandDto the otc command dto
	 * @param clz           the clz
	 * @param otcChainDto   the otc chain dto
	 */
	private void initOtcCommandDto(String id, OtcCommandDto otcCommandDto, Class<?> clz, OtcChainDto otcChainDto) {
		if (otcCommandDto == null) {
			return;
		}
		Field field = OtcReflectionUtil.findField(clz, otcCommandDto.fieldName);
		otcCommandDto.field = field;
		if (otcCommandDto.children == null) {
			return;
		}
		for (int idx = 1; idx < otcChainDto.otcTokens.length; idx++) {
			String otcToken = otcChainDto.otcTokens[idx];
			if (otcCommandDto.isCollection() || otcCommandDto.isMap()) {
				OtcCommandDtoFactory.createMembers(id, otcCommandDto, otcChainDto.rawOtcTokens);
				if (otcCommandDto.isCollection()) {
					otcCommandDto = otcCommandDto.children.get(otcCommandDto.fieldName);
				} else if (otcCommandDto.isMap()) {
					if (otcChainDto.rawOtcTokens[otcCommandDto.otcTokenIndex].contains(OtcConstants.MAP_KEY_REF)) {
						otcCommandDto = otcCommandDto.children.get(OtcConstants.MAP_KEY_REF + otcCommandDto.fieldName);
					} else {
						otcCommandDto = otcCommandDto.children
								.get(OtcConstants.MAP_VALUE_REF + otcCommandDto.fieldName);
					}
				}
			}
			OtcCommandDto childOCD = otcCommandDto.children.get(otcToken);
			field = OtcReflectionUtil.findField(otcCommandDto.fieldType, childOCD.fieldName);
			childOCD.field = field;
			otcCommandDto = childOCD;
		}
	}

	/**
	 * register.
	 *
	 * @param registryDto the registry dto
	 */
	@Override
	public void register(RegistryDto registryDto) {
		if (registryDto == null) {
			LOGGER.warn("Nothing to register!");
			return;
		}
		String mainClass = registryDto.mainClass;
		// exception will be thrown for loadclass if class is not compiled.
		Class<?> mainClz = null;
		try {
			mainClz = OtcUtils.loadClass(mainClass);
		} catch (OtcUnsupportedJdkException ex) {
			throw ex;
		} catch (Exception ex) {
			LOGGER.error("Could not load entry file {} ", mainClass);
			throw new OtcException("", ex);
		}
		CodeExecutor codeExecutor;
		try {
			codeExecutor = (CodeExecutor) mainClz.getDeclaredConstructor().newInstance();
			registryDto.codeExecutor = codeExecutor;
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			throw new OtcException("", e);
		}
		mapPackagedOtcDtos.put(registryDto.registryId, registryDto);
	}

	/**
	 * Retrieve registry dto.
	 *
	 * @param otcNamespace the otc namespace
	 * @param source       the source
	 * @param targetClz    the target clz
	 * @return the registry dto
	 */
	@Override
	public RegistryDto retrieveRegistryDto(String otcNamespace, Object source, Class<?> targetClz) {
		String registryId = OtcUtils.createRegistryId(otcNamespace, source, targetClz);
		return retrieveRegistryDto(registryId);
	}

	/**
	 * Retrieve registry dto.
	 *
	 * @param otcNamespace the otc namespace
	 * @param sourceClz    the source clz
	 * @param targetClz    the target clz
	 * @return the registry dto
	 */
	@Override
	public RegistryDto retrieveRegistryDto(String otcNamespace, Class<?> sourceClz, Class<?> targetClz) {
		String registryId = OtcUtils.createRegistryId(otcNamespace, sourceClz, targetClz);
		return retrieveRegistryDto(registryId);
	}

	/**
	 * Retrieve registry dto.
	 *
	 * @param registryId the registry id
	 * @return the registry dto
	 */
	private RegistryDto retrieveRegistryDto(String registryId) {
		RegistryDto registryDto = mapPackagedOtcDtos.get(registryId);
		if (registryDto == null) {
			throw new RegistryException("",
					"OTCS file '" + registryId + ".otcs' not compiled and registered!");
		}
		return registryDto;
	}
}
