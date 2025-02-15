package org.otcframework.compiler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.otcframework.common.config.OtcConfig;
import org.otcframework.common.dto.RegistryDto;
import org.otcframework.compiler.exception.OtcCompilerException;

import java.io.FileOutputStream;
import java.io.IOException;

abstract class AbstractCompiler {

    protected static final String OTC_CONFIGURED_TMD_LOCATION = OtcConfig.getConfiguredTmdLocation();
    /** The Constant srcDir. */
    protected static final String SOURCE_CODE_LOCATION = OtcConfig.getSourceCodeDirectoryPath();

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Creates the registration file.
     *
     * @param registryDto the registry dto
     */
    protected void createRegistrationFile(RegistryDto registryDto) {
        try (FileOutputStream fos = new FileOutputStream(registryDto.registryFileName)) {
            String str = OBJECT_MAPPER.writeValueAsString(registryDto);
            fos.write(str.getBytes());
            fos.flush();
        } catch (IOException e) {
            throw new OtcCompilerException(e);
        }
    }

}
