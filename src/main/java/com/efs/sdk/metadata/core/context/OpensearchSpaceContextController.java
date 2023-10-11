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
package com.efs.sdk.metadata.core.context;

import com.efs.sdk.common.domain.dto.SpaceContextDTO;
import com.efs.sdk.logging.AuditLogger;
import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.core.AuthService;
import com.efs.sdk.metadata.helper.AuthHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.INSUFFICIENT_RIGHTS;

@RequestMapping(value = OpensearchSpaceContextController.ENDPOINT)
@RestController
@Tag(name = OpensearchSpaceContextController.ENDPOINT)
public class OpensearchSpaceContextController {

    static final String VERSION = "v1.0";
    static final String RESOURCE = "context";
    static final String ENDPOINT = "/" + VERSION + "/" + RESOURCE + "/organization/{organizationName}/space/";

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchSpaceContextController.class);

    private final OpensearchContextService osCtxService;
    private final AuthHelper authHelper;
    private final AuthService authService;

    public OpensearchSpaceContextController(OpensearchContextService osCtxService, AuthHelper authHelper, AuthService authService) {
        this.osCtxService = osCtxService;
        this.authHelper = authHelper;
        this.authService = authService;
    }

    @Operation(summary = "Create OpenSearch context for the specified Space", description = "Creates the OpenSearch-context for the specified `Space`, " +
            "consisting of tenant, roles, rolesmappings and measurement-index")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully created OpenSearch-context")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to create OpenSearch-context")
    @ApiResponse(responseCode = "500", description = "Unable to create one of the context-types")
    @ApiResponse(responseCode = "502", description = "Connection to OpenSearch unavailable")
    public ResponseEntity<Void> createSpaceResources(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable String organizationName,
            @Valid @RequestBody SpaceContextDTO dto
    ) throws MetadataException {
        AuditLogger.info(LOG, "Creating OpenSearch context for space {} in organization {}", token, dto.getName(), dto.getOrganization().getName());
        if (!authHelper.isSuperuser(token)) {
            throw new MetadataException(INSUFFICIENT_RIGHTS);
        }
        String accessToken = authService.getSAAccessToken();
        osCtxService.createSpaceContext(dto, accessToken);

        LOG.debug("OpenSearch Space Context for {} created", dto.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Updates OpenSearch context", description = "Updates the OpenSearch-context for the specified `Space`, consisting of tenant, roles " +
            "and rolesmappings")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, path = "{spaceName}")
    @ApiResponse(responseCode = "200", description = "Successfully updated OpenSearch-context")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to update OpenSearch-context")
    @ApiResponse(responseCode = "500", description = "Unable to update one of the context-types")
    @ApiResponse(responseCode = "502", description = "Connection to OpenSearch unavailable")
    public ResponseEntity<Void> updateSpaceResources(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable String organizationName,
            @PathVariable String spaceName,
            @Valid @RequestBody SpaceContextDTO dto
    ) throws MetadataException {
        AuditLogger.info(LOG, "Updating OpenSearch context for space {} in organization {}", token, dto.getName(), dto.getOrganization().getName());
        if (!authHelper.isSuperuser(token)) {
            throw new MetadataException(INSUFFICIENT_RIGHTS);
        }
        String accessToken = authService.getSAAccessToken();
        osCtxService.updateSpaceContext(dto, accessToken);

        LOG.debug("OpenSearch Space Context for {} updated", dto.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Deletes OpenSearch context", description = "Deletes the OpenSearch-context for the specified `Space`, consisting of tenant, roles " +
            "and rolesmappings")
    @DeleteMapping(path = "{spaceName}")
    @ApiResponse(responseCode = "200", description = "Successfully deleted OpenSearch-context")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to delete OpenSearch-context")
    @ApiResponse(responseCode = "500", description = "Unable to delete one of the context-types")
    @ApiResponse(responseCode = "502", description = "Connection to OpenSearch unavailable")
    public ResponseEntity<Void> deleteSpaceResources(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable String organizationName,
            @PathVariable String spaceName) throws MetadataException {
        AuditLogger.info(LOG, "Deleting OpenSearch context for space {} in organization {}", token, spaceName, organizationName);
        if (!authHelper.isSuperuser(token)) {
            throw new MetadataException(INSUFFICIENT_RIGHTS);
        }
        String accessToken = authService.getSAAccessToken();
        osCtxService.deleteSpaceContext(accessToken, spaceName, organizationName);

        LOG.debug("OpenSearch Space Context for {} deleted", spaceName);
        return ResponseEntity.noContent().build();
    }
}
