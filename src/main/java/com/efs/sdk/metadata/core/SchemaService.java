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
package com.efs.sdk.metadata.core;

import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.helper.FileHandling;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.PROCESSING_EXCEPTION;


/**
 * Service for checking a json against a json schema
 */
@Service
public class SchemaService {

    private final JsonSchema schema;

    /**
     * Constructor for schema validation, prepares json schema validation by loading the json schema
     */
    public SchemaService(@Value("${metadata.schema.meta-json-schema}") String jsonSchema) throws IOException, ProcessingException {
        final JsonNode metaSchema = FileHandling.stringToJSONNode(jsonSchema);
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        schema = factory.getJsonSchema(metaSchema);
    }


    /**
     * Validates a json node against the schema with the help of a json against schema validation library
     */
    public boolean validateJsonNode(JsonNode jsonNode) throws ProcessingException, MetadataException {

        ProcessingReport report = schema.validate(jsonNode);

        if (!report.isSuccess()) {
            throw new MetadataException(PROCESSING_EXCEPTION, getErrorsList(report));
        }
        return true;
    }

    /**
     * helper function to get the errors from the processing report of the json against schema validation library
     */
    String getErrorsList(ProcessingReport report) {
        StringBuilder jsonValidationErrors = new StringBuilder();
        for (ProcessingMessage processingMessage : report) {
            if (LogLevel.ERROR.equals(processingMessage.getLogLevel())) {
                jsonValidationErrors.append(processingMessage.getMessage()).append("\n\r");
            }
        }
        return jsonValidationErrors.toString();
    }
}
