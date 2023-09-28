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
package com.efs.sdk.metadata.core.index.application;

import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.model.ApplicationIndexCreateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RequestMapping(value = IndexController.ENDPOINT)
@RestController
@Tag(name = IndexController.ENDPOINT)
public class IndexController {

    static final String VERSION = "v1.0";
    static final String RESOURCE = "application-index";
    static final String ENDPOINT = "/" + VERSION + "/" + RESOURCE;
    private static final Logger LOG = LoggerFactory.getLogger(IndexController.class);
    private final IndexService indexService;

    public IndexController(IndexService indexService) {
        this.indexService = indexService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "create application index", description = "Creates an application index with given index mapping, if the caller has sufficient rights for this operation (space trustee)")
    @ApiResponse(responseCode = "201", description = "Successfully created an application index")
    @ApiResponse(responseCode = "400", description = "Custom-name of the application-index is not valid")
    @ApiResponse(responseCode = "403", description = "User does not have the required permissions to create an application-index")
    @ApiResponse(responseCode = "409", description = "Application-index already exists")
    public ResponseEntity<String> createApplicationIndex(JwtAuthenticationToken token, @RequestBody ApplicationIndexCreateDTO dto) throws MetadataException, IOException {
        LOG.debug("create application-index: {}", dto);

        return new ResponseEntity<>(this.indexService.createApplicationIndex(token, dto), HttpStatus.CREATED);
    }

    @DeleteMapping(value = "{indexName}")
    @Operation(summary = "Delete application index", description = "Deletes an application index")

    @ApiResponse(responseCode = "204", description = "Successfully deleted an application index")
    @ApiResponse(responseCode = "400", description = "Given application-index is not valid")
    @ApiResponse(responseCode = "403", description = "User does not have the required permissions to create an application-index")
    public ResponseEntity<Void> deleteApplicationIndex(JwtAuthenticationToken token, @PathVariable String indexName) throws MetadataException, IOException {
        LOG.debug("delete application-index: {}", indexName);

        this.indexService.deleteApplicationIndex(indexName, token);
        return ResponseEntity.noContent().build();
    }


}
