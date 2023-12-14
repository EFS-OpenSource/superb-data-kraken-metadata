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

import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.core.AuthService;
import com.efs.sdk.metadata.helper.AuthHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.INSUFFICIENT_RIGHTS;


@RequestMapping(value = OpensearchGlobalContextController.ENDPOINT)
@RestController
@Tag(name = OpensearchGlobalContextController.ENDPOINT)
public class OpensearchGlobalContextController {

    static final String VERSION = "v1.0";
    static final String RESOURCE = "context";
    static final String ENDPOINT = "/" + VERSION + "/" + RESOURCE;

    private final OpensearchContextService osCtxService;
    private final AuthHelper authHelper;
    private final AuthService authService;

    public OpensearchGlobalContextController(OpensearchContextService osCtxService, AuthHelper authHelper, AuthService authService) {
        this.osCtxService = osCtxService;
        this.authHelper = authHelper;
        this.authService = authService;
    }

    @Operation(summary = "Synchronize OpenSearch-context", description = "Recover all corresponding OpenSearch tenants, roles and rolesmappings by deleting " +
            "them and recreate (indices are only being created)")
    @PutMapping(value = "/sync_opensearch")
    @ApiResponse(responseCode = "204", description = "Successfully synced OpenSearch")
    @ApiResponse(responseCode = "400", description = "Unable to retrieve one of the following: all `Organizations`, all `Spaces`")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to sync OpenSearch")
    @ApiResponse(responseCode = "500", description = "Unable to delete/create one of the context-types")
    @ApiResponse(responseCode = "502", description = "Connection to OpenSearch unavailable")
    public ResponseEntity<Void> syncOpensearch(@Parameter(hidden = true) JwtAuthenticationToken token) throws MetadataException {
        if (!authHelper.isSuperuser(token)) {
            throw new MetadataException(INSUFFICIENT_RIGHTS);
        }
        String accessToken = authService.getSAAccessToken();
        osCtxService.syncOpensearch(accessToken);
        return ResponseEntity.noContent().build();
    }


}
