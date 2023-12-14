/*
Copyright (C) 2023 e:fs TechHub GmbH (sdk@efs-techhub.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.efs.sdk.metadata.helper;

import com.efs.sdk.metadata.commons.MetadataException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.micrometer.core.instrument.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.PROCESSING_EXCEPTION;

/**
 * Class for file handling.
 */

public class FileHandling {


    private FileHandling() {
    }

    /**
     * loads a file from resources, e.g. for testing purposes
     *
     * @param fileName the name of the file to load
     * @return InputStream: file content as InputStream.
     */
    public static InputStream getResourceFileAsInputStream(String fileName) throws IOException {
        InputStream inputStream = FileHandling.class.getResourceAsStream("/" + fileName);
        if (inputStream == null) {
            throw new IOException(String.format("input stream of %s is null", fileName));
        }
        return inputStream;
    }

    /**
     * loads a file from resources, e.g. for testing purposes
     *
     * @param fileName the name of the file to load
     * @return String: file content as String.
     */
    public static String getResourceFileAsString(String fileName) throws IOException {
        InputStream inputStream = getResourceFileAsInputStream(fileName);
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }


    public static JsonNode stringToJSONNode(String string) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper.readTree(string);
    }

    /**
     * loads a file from resources, e.g. for testing purposes
     *
     * @param fileName the name of the file to load
     * @return String: file content as JsonNode.
     */
    public static JsonNode getResourceFileAsJsonNode(String fileName) throws IOException, MetadataException {
        String fileString = getResourceFileAsString(fileName);

        try {
            return stringToJSONNode(fileString);
        } catch (Exception e) {
            throw new MetadataException(PROCESSING_EXCEPTION, e.getMessage());
        }
    }


}
