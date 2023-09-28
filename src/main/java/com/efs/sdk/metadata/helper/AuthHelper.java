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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AuthHelper {

    public static final String ORG_CREATE_PERMISSION_ROLE = "org_create_permission";
    private static final Logger LOG = LoggerFactory.getLogger(AuthHelper.class);
    private static final String SUPERUSER_ROLE = "SDK_ADMIN";

    /**
     * Check if user has Superuser-Role
     *
     * @param token The user token.
     * @return If user has Superuser-Role.
     */
    public boolean isSuperuser(JwtAuthenticationToken token) {
        return hasRights(token, new String[]{SUPERUSER_ROLE});
    }


    /**
     * Checks if the tokens Authorities contains any valid role
     *
     * @param token      the token for checking
     * @param validRoles the valid roles
     * @return if the user has valid role
     */
    private boolean hasRights(JwtAuthenticationToken token, String[] validRoles) {
        for (GrantedAuthority auth : token.getAuthorities()) {
            for (String role : validRoles) {
                LOG.debug("role from token: {}  necessary role: {} (need to match)", auth.getAuthority(), role);
                if (role.equalsIgnoreCase(auth.getAuthority())) {
                    LOG.debug("User has necessary rights. Found match for {}", role);
                    return true;
                }
            }
        }
        LOG.debug("User does NOT have necessary rights. User needs one of those roles: {}", Arrays.toString(validRoles));
        return false;
    }
}
