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
package com.efs.sdk.metadata.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class OrganizationManagerClientTest {
    private static final String ORGA_ENDPOINT_ORGA = "http://localhost:8090/organizationmanager/api/v1.0/organization";
    private static final String ORGA_ENDPOINT_SPACE = "http://localhost:8090/organizationmanager/api/v1.0/space";
    private OrganizationManagerClient client;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setup() {
        this.restTemplate = Mockito.mock(RestTemplate.class);
        this.client = new OrganizationManagerClient(restTemplate, ORGA_ENDPOINT_ORGA, ORGA_ENDPOINT_SPACE);
    }

    @Test
    void givenGetSpaceOk_whenGetSpace_thenOk() throws Exception {
        long orgaId = 1L;
        String orgaName = "myorga";
        String spaceName = "myspace";
        Map<String, Object> orga = Map.of("id", orgaId);
        Map<String, Object> space = Map.of("name", spaceName);
        List<Map<String, Object>> spaces = List.of(space);

        String orgaUrl = format("%s/name/%s", ORGA_ENDPOINT_ORGA, orgaName);
        ParameterizedTypeReference<Map<String, Object>> responseType = new ParameterizedTypeReference<>() {
        };
        given(restTemplate.exchange(eq(orgaUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(responseType))).willReturn(ResponseEntity.ok(orga));

        String spacesUrl = format("%s/%d?permissions=%s", ORGA_ENDPOINT_SPACE, orgaId, OrganizationManagerClient.Permissions.GET);
        ParameterizedTypeReference<List<Map<String, Object>>> spacesResponseType = new ParameterizedTypeReference<>() {
        };
        given(restTemplate.exchange(eq(spacesUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(spacesResponseType))).willReturn(ResponseEntity.ok(spaces));

        assertNotNull(client.getSpace("my-token", orgaName, spaceName, OrganizationManagerClient.Permissions.GET));
    }

}
