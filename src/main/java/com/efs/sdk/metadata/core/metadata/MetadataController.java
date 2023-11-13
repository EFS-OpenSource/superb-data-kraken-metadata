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
package com.efs.sdk.metadata.core.metadata;

import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.core.SchemaService;
import com.efs.sdk.metadata.model.MeasurementDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;


@RequestMapping(value = MetadataController.ENDPOINT)
@RestController
@Tag(name = MetadataController.ENDPOINT)
public class MetadataController {

    static final String VERSION = "v1.0";
    static final String ENDPOINT = "/" + VERSION;
    private final MetadataService service;

    private final SchemaService schemaService;

    public MetadataController(MetadataService service, SchemaService schemaService) {
        this.service = service;
        this.schemaService = schemaService;
    }

    /**
     * Provides a REST interface for verifying a schemas.
     *
     * @return Boolean
     */
    @PostMapping(path = "/validateJson")
    @Operation(summary = "Validate json-schema", description = "Endpoint for validating a json against a predefined schema")
    @ApiResponse(responseCode = "200", description = "Validation successful")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    public ResponseEntity<Boolean> validateJson(@Parameter(hidden = true) JwtAuthenticationToken token, @RequestBody JsonNode jsonNode) throws MetadataException, ProcessingException {
        if (token == null) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }

        return ResponseEntity.ok(schemaService.validateJsonNode(jsonNode));
    }


    /**
     * Provides a REST interface for manual indexing.
     *
     * @return Boolean
     */
    @PostMapping(path = "/index")
    @Operation(summary = "Index", description = "Endpoint for indexing a metadata-document")
    @ApiResponse(responseCode = "200", description = "Successfully indexed document")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to index document")
    @ApiResponse(responseCode = "409", description = "One of the following properties is missing: \"organization\", \"space\", \"rootdir\"")
    public ResponseEntity<Boolean> index(@Parameter(hidden = true) JwtAuthenticationToken token, @RequestBody(required = true) MeasurementDTO measurement) throws MetadataException {
        if (token == null) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }

        return ResponseEntity.ok(service.index(token.getToken().getTokenValue(), measurement));
    }

    /**
     * Provides a REST interface for manual adding metadata attributes
     *
     * @return Boolean
     */
    @PutMapping(path = "/index")
    @Operation(summary = "Update a document", description = "Endpoint for updating a metadata-document")
    @ApiResponse(responseCode = "200", description = "Successfully indexed document")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to update document")
    @ApiResponse(responseCode = "507", description = "Unable to retrieve the indexed document")
    public ResponseEntity<Boolean> indexPut(@Parameter(hidden = true) JwtAuthenticationToken token, @Parameter(description = "Name of the `Organization`",
            example = "myorga") @RequestParam String organization,
            @Parameter(description = "Name of the `Space`", example = "myspace") @RequestParam String space, @Parameter(description = "id of the document",
            example = "d2a3cf15-cd6c-4a85-9752-da0628ce949e") @RequestParam String docid,
            @Parameter(description = "the new metadata") @RequestBody MeasurementDTO metadata) throws IOException, MetadataException {
        if (token == null) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }

        return ResponseEntity.ok(service.update(metadata, token.getToken().getTokenValue(), organization, space, docid));
    }
}
