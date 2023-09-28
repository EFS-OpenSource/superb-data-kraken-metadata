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

import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomJwtGrantedAuthoritiesConverterTest {

    private CustomJwtGrantedAuthoritiesConverter converter;

    @BeforeEach
    void setup() {
        this.converter = new CustomJwtGrantedAuthoritiesConverter();
    }

    @Test
    void givenMapperRoles_whenConvert_thenOk() {
        List<String> roles = List.of("offline_access", "uma_authorization");
        Collection<? extends GrantedAuthority> roleAuthorities = converter.convert(new MapperRoleBuilder().getJwt(roles));
        assertThat(roleAuthorities, hasSize(2));
        for (String role : roles) {
            assertTrue(roleAuthorities.stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase(role)));
        }
    }

    @Test
    void givenEmptyRoles_whenExtractResourceRoles_thenOk() {
        List<String> roles = Collections.emptyList();
        Collection<? extends GrantedAuthority> collection = converter.convert(new RealmRoleBuilder().getJwt(roles));
        assertThat(collection, hasSize(0));
    }

    @Test
    void givenRealmRoles_whenConvert_thenOk() {
        List<String> roles = List.of("offline_access", "uma_authorization");
        Collection<? extends GrantedAuthority> roleAuthorities = converter.convert(new RealmRoleBuilder().getJwt(roles));
        assertThat(roleAuthorities, hasSize(2));
        for (String role : roles) {
            assertTrue(roleAuthorities.stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase(role)));
        }
    }

    static abstract class JwtBuilder {

        Jwt getJwt(List<String> roles) {
            Jwt.Builder jwtBuilder = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user").claim("scope", "openid email profile");

            if (!roles.isEmpty()) {
                addRolesClaim(jwtBuilder, roles);
            }

            return jwtBuilder.build();
        }

        abstract void addRolesClaim(Jwt.Builder jwtBuilder, List<String> roles);

    }

    static class MapperRoleBuilder extends JwtBuilder {

        @Override
        void addRolesClaim(Jwt.Builder jwtBuilder, List<String> roles) {
            jwtBuilder.claim("roles", roles);
        }
    }

    static class RealmRoleBuilder extends JwtBuilder {

        @Override
        void addRolesClaim(Jwt.Builder jwtBuilder, List<String> roles) {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("roles", roles);
            jwtBuilder.claim("realm_access", new JSONObject(roleMap));
        }
    }
}