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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaServiceTest {

    private SchemaService schemaService;

    @BeforeEach
    public void setup() throws IOException, ProcessingException {
        String jsonSchema = FileHandling.getResourceFileAsString("schema_validation/meta-schema.json");
        this.schemaService = new SchemaService(jsonSchema);
    }


    @Test
    void givenGoodJson_whenValidateJsonNode_thenOk() throws IOException, ProcessingException, MetadataException {
        final JsonNode good = FileHandling.getResourceFileAsJsonNode("jsons_for_schema_validation/example-meta.json");
        assertTrue(schemaService.validateJsonNode(good));
    }

    @Test
    void givenBadJson_whenValidateJsonNode_thenError() throws IOException, MetadataException {
        final JsonNode bad = FileHandling.getResourceFileAsJsonNode("jsons_for_schema_validation/example-not-meta.json");
        assertThrows(MetadataException.class, () -> schemaService.validateJsonNode(bad));
    }

}