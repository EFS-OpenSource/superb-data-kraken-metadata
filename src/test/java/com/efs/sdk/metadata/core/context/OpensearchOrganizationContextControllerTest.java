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
import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.core.AuthService;
import com.efs.sdk.metadata.helper.AuthHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.efs.sdk.metadata.core.context.OpensearchOrganizationContextController.ENDPOINT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpensearchOrganizationContextController.class)
@ActiveProfiles("test")
class OpensearchOrganizationContextControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private OpensearchContextService contextService;
    @MockBean
    private AuthHelper authHelper;
    @MockBean
    private AuthService authService;

    /* required for security tests to run. Do not remove! */
    @MockBean
    private JwtDecoder decoder;
    @MockBean
    private JwtAuthenticationToken token;
    private OrganizationContextDTO testOrganizationContextDTO;
    @Autowired
    private ObjectMapper objectMapper;

    private static String getOrganizationEndpoint(String organizationName) {
        return ENDPOINT + "/" + organizationName;
    }

    private static String getEndpoint() {
        return ENDPOINT;
    }

    @BeforeEach
    void setup() {
        this.token = Mockito.mock(JwtAuthenticationToken.class);
        this.testOrganizationContextDTO = OrganizationContextDTO.builder().id(1L).name("testorganization").build();
    }

    @Test
    void givenNoAuthentication_whenCreateOrganization_thenError() throws Exception {
        mvc.perform(post(getOrganizationEndpoint(testOrganizationContextDTO.getName())).contentType(MediaType.APPLICATION_JSON).content(this.objectMapper.writeValueAsString(this.testOrganizationContextDTO))).andExpect(status().isForbidden());
    }

    @Test
    void givenAuthentication_whenCreateOrganization_thenOk() throws Exception {
        given(authHelper.isSuperuser(any())).willReturn(true);
        Jwt token = getJwt(List.of("SDK_ADMIN"));
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());
        doNothing().when(contextService).createOrganizationContext(anyString(), any(OrganizationContextDTO.class));

        mvc.perform(post(getEndpoint()).contentType(MediaType.APPLICATION_JSON).content(this.objectMapper.writeValueAsString(this.testOrganizationContextDTO)).with(jwt())).andExpect(status().is2xxSuccessful());
    }

    @Test
    void givenNoSuperuser_whenCreateOrganization_thenForbidden() throws Exception {
        doNothing().when(contextService).createOrganizationContext(anyString(), any(OrganizationContextDTO.class));
        given(authHelper.isSuperuser(any())).willReturn(false);
        Jwt token = getJwt(List.of());
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());
        mvc.perform(post(getOrganizationEndpoint(testOrganizationContextDTO.getName())).contentType(MediaType.APPLICATION_JSON).content(this.objectMapper.writeValueAsString(this.testOrganizationContextDTO))).andExpect(status().isForbidden());
    }

    @Test
    void givenError_whenCreateOrganization_thenError() throws Exception {
        doThrow(MetadataException.class).when(contextService).createOrganizationContext(anyString(), any(OrganizationContextDTO.class));
        Jwt token = getJwt(List.of("SDK_ADMIN"));
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());

        mvc.perform(post(getOrganizationEndpoint(testOrganizationContextDTO.getName())).contentType(MediaType.APPLICATION_JSON).content(this.objectMapper.writeValueAsString(this.testOrganizationContextDTO)).with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenDeleteOrganization_thenError() throws Exception {
        mvc.perform(delete(getEndpoint())).andExpect(status().isForbidden());
    }

    @Test
    void givenAuthentication_whenDeleteOrganization_thenOk() throws Exception {
        doNothing().when(contextService).deleteOrganizationContext(anyString(), anyString());
        given(authHelper.isSuperuser(any())).willReturn(true);
        Jwt token = getJwt(List.of("SDK_ADMIN"));
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());

        mvc.perform(delete(getOrganizationEndpoint(testOrganizationContextDTO.getName())).with(jwt())).andExpect(status().is2xxSuccessful());
    }

    @Test
    void givenNoSuperuser_whenDeleteOrganization_thenForbidden() throws Exception {
        doNothing().when(contextService).createOrganizationContext(anyString(), any(OrganizationContextDTO.class));
        given(authHelper.isSuperuser(any())).willReturn(false);
        Jwt token = getJwt(List.of());
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());

        mvc.perform(delete(getOrganizationEndpoint(testOrganizationContextDTO.getName())).with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    void givenError_whenDeleteOrganization_thenError() throws Exception {
        doThrow(MetadataException.class).when(contextService).createOrganizationContext(anyString(), any(OrganizationContextDTO.class));
        mvc.perform(delete(getOrganizationEndpoint(testOrganizationContextDTO.getName())).with(jwt())).andExpect(status().is4xxClientError());
    }

    private Jwt getJwt(List<String> roles) {
        Jwt.Builder jwtBuilder = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user").claim("scope", "openid email profile");

        if (!roles.isEmpty()) {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("roles", roles);
            jwtBuilder.claim("realm_access", new JSONObject(roleMap));
        }

        return jwtBuilder.build();
    }
}
