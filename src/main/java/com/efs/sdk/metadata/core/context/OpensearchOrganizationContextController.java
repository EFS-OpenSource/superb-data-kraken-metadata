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

import com.efs.sdk.common.domain.dto.OrganizationContextDTO;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.INSUFFICIENT_RIGHTS;

@RequestMapping(value = OpensearchOrganizationContextController.ENDPOINT)
@RestController
@Tag(name = OpensearchOrganizationContextController.ENDPOINT)
public class OpensearchOrganizationContextController {

    static final String VERSION = "v1.0";
    static final String RESOURCE = "context";
    static final String ENDPOINT = "/" + VERSION + "/" + RESOURCE + "/organization/";

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchOrganizationContextController.class);

    private final OpensearchContextService osCtxService;
    private final AuthHelper authHelper;
    private final AuthService authService;

    public OpensearchOrganizationContextController(OpensearchContextService osCtxService, AuthHelper authHelper, AuthService authService) {
        this.osCtxService = osCtxService;
        this.authHelper = authHelper;
        this.authService = authService;
    }

    @Operation(summary = "Create OpenSearch context for the specified Organization", description = "Creates the opensearch-context for the specified " +
            "`Organization`, consisting of tenant, roles and rolesmappings")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('" + AuthHelper.ORG_CREATE_PERMISSION_ROLE + "')")
    @ApiResponse(responseCode = "200", description = "Successfully created OpenSearch-context")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to create OpenSearch-context")
    @ApiResponse(responseCode = "500", description = "Unable to create one of the context-types")
    @ApiResponse(responseCode = "502", description = "Connection to OpenSearch unavailable")
    public ResponseEntity<Void> createOrganizationResources(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @Valid @RequestBody OrganizationContextDTO dto
    ) throws MetadataException {
        AuditLogger.info(LOG, "Creating OpenSearch context for organization {}", token, dto.getName());
        LOG.debug("Creating OpenSearch context for organization {}", dto.getName());
        String accessToken = authService.getSAAccessToken();
        osCtxService.createOrganizationContext(accessToken, dto);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update OpenSearch resources, dependent on creating a new organization: tenant, roles and rolemappings")
    @PutMapping(path = "{orgName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('" + AuthHelper.ORG_CREATE_PERMISSION_ROLE + "')")
    @ApiResponse(responseCode = "200", description = "Successfully updated OpenSearch-context")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to update OpenSearch-context")
    @ApiResponse(responseCode = "500", description = "Unable to update one of the context-types")
    @ApiResponse(responseCode = "502", description = "Connection to OpenSearch unavailable")
    public ResponseEntity<Void> updateOrganizationResources(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable String orgName,
            @Valid @RequestBody OrganizationContextDTO dto
    ) throws MetadataException {
        AuditLogger.info(LOG, "Updating OpenSearch context for organization {}", token, dto.getName());
        LOG.debug("Updating OpenSearch context for organization {}", dto.getName());
        dto.setName(orgName);
        String accessToken = authService.getSAAccessToken();
        osCtxService.updateOrganizationContext(dto, accessToken);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Deletes OpenSearch resources, dependent on deleting an organization: tenant, roles and rolemappings")
    @DeleteMapping(path = "{organizationName}")
    @ApiResponse(responseCode = "204", description = "Successfully deleted OpenSearch-context")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to delete OpenSearch-context")
    @ApiResponse(responseCode = "500", description = "Unable to delete one of the context-types")
    @ApiResponse(responseCode = "502", description = "Connection to OpenSearch unavailable")
    public ResponseEntity<Void> deleteOrganizationResources(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable String organizationName
    ) throws MetadataException {
        AuditLogger.info(LOG, "Deleting OpenSearch context for organization {}", token, organizationName);
        LOG.debug("Deleting OpenSearch context for organization {}", organizationName);
        if (!authHelper.isSuperuser(token)) {
            throw new MetadataException(INSUFFICIENT_RIGHTS);
        }
        String accessToken = authService.getSAAccessToken();
        osCtxService.deleteOrganizationContext(accessToken, organizationName);

        return ResponseEntity.noContent().build();
    }
}
