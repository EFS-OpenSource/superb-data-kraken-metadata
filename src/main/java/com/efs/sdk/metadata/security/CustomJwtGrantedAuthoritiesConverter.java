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
package com.efs.sdk.metadata.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom converter for authorities contained in a JWT token.
 *
 * @author e:fs TechHub GmbH
 */
public class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    public static final String PROP_ROLES = "roles";

    /**
     * Extracts the authorities from the given token and returns them.
     * <p>
     * Supports mapper- as well as realm-access-roles.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> authorities = new ArrayList<>();
        authorities.addAll(getFromMapper(jwt));
        authorities.addAll(getFromRealm(jwt));
        return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    /**
     * Get roles from mapper
     *
     * @param jwt The jwt-token
     * @return roles from mapper
     * @deprecated will be removed, once realm-access-roles is implemented in all services
     */
    @Deprecated
    private Collection<String> getFromMapper(Jwt jwt) {
        if (jwt.getClaims().containsKey(PROP_ROLES)) {
            return jwt.getClaim(PROP_ROLES);
        }
        return Collections.emptyList();
    }

    /**
     * Get roles from realm-access
     *
     * @param jwt The jwt-token
     * @return roles from realm-access
     */
    private Collection<String> getFromRealm(Jwt jwt) {
        final Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
        if (realmAccess == null || !realmAccess.containsKey(PROP_ROLES)) {
            return Collections.emptySet();
        }
        return (Collection<String>) realmAccess.get(PROP_ROLES);
    }

}