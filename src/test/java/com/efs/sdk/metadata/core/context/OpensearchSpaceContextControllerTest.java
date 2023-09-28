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
import com.efs.sdk.common.domain.dto.SpaceContextDTO;
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

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.UNABLE_CREATE_INDEX;
import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.UNABLE_DELETE_ROLE;
import static com.efs.sdk.metadata.core.context.OpensearchSpaceContextController.ENDPOINT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpensearchSpaceContextController.class)
@ActiveProfiles("test")
class OpensearchSpaceContextControllerTest {
    @MockBean
    private AuthHelper authHelper;
    @MockBean
    private AuthService authService;
    @MockBean
    private OpensearchContextService contextService;
    /* required for security tests to run. Do not remove! */
    @MockBean
    private JwtDecoder decoder;
    @Autowired
    private MockMvc mvc;
    private ObjectMapper objectMapper;
    private OrganizationContextDTO testOrganizationContextDTO;
    private SpaceContextDTO testSpaceContextDTO;
    @MockBean
    private JwtAuthenticationToken token;

    private static String getOpensearchContectSpaceEndpoint(String orgName) {
        return ENDPOINT.replace("{organizationName}", orgName);
    }

    @BeforeEach
    void setup() {
        this.token = Mockito.mock(JwtAuthenticationToken.class);
        this.testOrganizationContextDTO = OrganizationContextDTO.builder().id(1L).name("testorganization").build();
        this.testSpaceContextDTO = SpaceContextDTO.builder().name("testspace").organization(this.testOrganizationContextDTO).build();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    void givenNoAuthentication_whenCreateSpace_thenError() throws Exception {
        mvc.perform(post(getOpensearchContectSpaceEndpoint("testorganization")).contentType(MediaType.APPLICATION_JSON).content(this.objectMapper.writeValueAsString(this.testSpaceContextDTO))).andExpect(status().isForbidden());
    }

    @Test
    void givenAuthentication_whenCreateSpace_thenOk() throws Exception {
        doNothing().when(contextService).createSpaceContext(any(SpaceContextDTO.class), anyString());
        given(authHelper.isSuperuser(any())).willReturn(true);
        Jwt token = getJwt(List.of("SDK_ADMIN"));
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());
        mvc.perform(post(getOpensearchContectSpaceEndpoint("testorganization")).contentType(MediaType.APPLICATION_JSON_VALUE).content(this.objectMapper.writeValueAsString(this.testSpaceContextDTO)).with(jwt())).andExpect(status().is2xxSuccessful());
    }

    @Test
    void givenNoSuperuser_whenCreateSpace_thenForbidden() throws Exception {
        doNothing().when(contextService).createSpaceContext(any(SpaceContextDTO.class), anyString());
        given(authHelper.isSuperuser(any())).willReturn(false);
        Jwt token = getJwt(List.of(""));
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());

        mvc.perform(post(getOpensearchContectSpaceEndpoint("testorganization")).contentType(MediaType.APPLICATION_JSON).content(this.objectMapper.writeValueAsString(this.testSpaceContextDTO))).andExpect(status().isForbidden());
    }

    @Test
    void givenError_whenCreateSpace_thenError() throws Exception {
        doThrow(new MetadataException(UNABLE_CREATE_INDEX)).when(contextService).createSpaceContext(any(SpaceContextDTO.class), anyString());
        mvc.perform(post(getOpensearchContectSpaceEndpoint("testorganization")).contentType(MediaType.APPLICATION_JSON).content(this.objectMapper.writeValueAsString(this.testSpaceContextDTO))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenDeleteSpace_thenError() throws Exception {
        mvc.perform(delete(getOpensearchContectSpaceEndpoint(testOrganizationContextDTO.getName()) + "/" + testSpaceContextDTO.getName())).andExpect(status().isForbidden());
    }

    @Test
    void givenAuthentication_whenDeleteSpace_thenOk() throws Exception {
        given(authHelper.isSuperuser(any())).willReturn(true);
        Jwt token = getJwt(List.of("SDK_ADMIN"));
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());
        doNothing().when(contextService).createSpaceContext(any(SpaceContextDTO.class), anyString());
        mvc.perform(delete(getOpensearchContectSpaceEndpoint(testOrganizationContextDTO.getName()) + "/" + testSpaceContextDTO.getName()).with(jwt())).andExpect(status().is2xxSuccessful());
    }

    @Test
    void givenNoSuperuser_whenDeleteSpace_thenForbidden() throws Exception {
        given(authHelper.isSuperuser(any())).willReturn(false);
        Jwt token = getJwt(List.of());
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());
        doNothing().when(contextService).createSpaceContext(any(SpaceContextDTO.class), anyString());
        mvc.perform(delete(getOpensearchContectSpaceEndpoint(testOrganizationContextDTO.getName()) + "/" + testSpaceContextDTO.getName()).with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    void givenError_whenDeleteSpace_thenError() throws Exception {
        doThrow(new MetadataException(UNABLE_DELETE_ROLE)).when(contextService).deleteSpaceContext(anyString(), anyString(), anyString());
        given(authHelper.isSuperuser(any())).willReturn(true);
        Jwt token = getJwt(List.of("SDK_ADMIN"));
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());
        mvc.perform(delete(getOpensearchContectSpaceEndpoint(testOrganizationContextDTO.getName()) + testSpaceContextDTO.getName()).with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenInternalError_whenDeleteSpace_thenError() throws Exception {
        doThrow(new IllegalArgumentException("something")).when(contextService).deleteSpaceContext(anyString(), anyString(), anyString());
        given(authHelper.isSuperuser(any())).willReturn(true);
        Jwt token = getJwt(List.of("SDK_ADMIN"));
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());
        mvc.perform(delete(getOpensearchContectSpaceEndpoint(testOrganizationContextDTO.getName()) + testSpaceContextDTO.getName()).with(jwt())).andExpect(status().is5xxServerError());
    }


    @Test
    void givenInternalError_whenCreateSpace_thenError() throws Exception {
        given(authHelper.isSuperuser(any())).willReturn(true);
        Jwt token = getJwt(List.of("SDK_ADMIN"));
        given(authService.getSAAccessToken()).willReturn(token.getTokenValue());
        doThrow(new IllegalArgumentException("something")).when(contextService).createSpaceContext(any(SpaceContextDTO.class), anyString());
        mvc.perform(post(getOpensearchContectSpaceEndpoint("testorganization")).contentType(MediaType.APPLICATION_JSON).content(this.objectMapper.writeValueAsString(this.testSpaceContextDTO)).with(jwt())).andExpect(status().is5xxServerError());
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
