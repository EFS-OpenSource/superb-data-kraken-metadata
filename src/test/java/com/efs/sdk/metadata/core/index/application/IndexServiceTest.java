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

import com.efs.sdk.metadata.clients.OpenSearchRestClientBuilder;
import com.efs.sdk.metadata.clients.OpenSearchRestClientBuilderTest;
import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.core.AuthService;
import com.efs.sdk.metadata.model.ApplicationIndexCreateDTO;
import com.efs.sdk.metadata.model.ApplicationIndexType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static com.efs.sdk.metadata.utils.TestHelper.findRandomPort;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.http.HttpStatus.*;

@ActiveProfiles("test")
@RestClientTest(IndexService.class)
class IndexServiceTest {

    @MockBean
    private JwtAuthenticationToken token;
    @MockBean
    private OpenSearchRestClientBuilder clientBuilder;
    @MockBean
    private AuthService authService;
    private IndexService service;
    private ClientAndServer mockServer;

    @BeforeEach
    void setup() throws IOException {
        Integer port = findRandomPort();
        ConfigurationProperties.logLevel("INFO");
        this.clientBuilder = new OpenSearchRestClientBuilderTest("http://127.0.0.1:" + port);
        this.mockServer = ClientAndServer.startClientAndServer(port);
        this.service = Mockito.spy(new IndexService(clientBuilder, authService));
    }

    @AfterEach
    void destroy() {
        mockServer.stop();
    }

    @Test
    void givenAuthorised_whenCreateIndex_thenCreateIndex() throws IOException, MetadataException {
        ApplicationIndexCreateDTO testDTO = buildTestDTO();
        String correctIndexName = getValidIndexName(testDTO);

        HttpRequest aliasRequest;
        aliasRequest = HttpRequest.request().withMethod(HttpMethod.GET.name()).withPath(format("/_cat/indices/%s", correctIndexName));
        mockServer.when(aliasRequest).respond(response().withStatusCode(NOT_FOUND.value()));
        aliasRequest = HttpRequest.request().withMethod(HttpMethod.PUT.name()).withPath(format("/%s", correctIndexName));
        mockServer.when(aliasRequest).respond(response().withStatusCode(OK.value()));
        aliasRequest = HttpRequest.request().withMethod(HttpMethod.PUT.name()).withPath(format("/%s/_alias/%s", correctIndexName, testDTO.getIndexType().getIndexAlias()));
        mockServer.when(aliasRequest).respond(response().withStatusCode(OK.value()));
        doReturn(true).when(service).canCreateApplicationIndex(token, testDTO);

        assertEquals(correctIndexName, service.createApplicationIndex(token, testDTO));
    }

    @Test
    void givenIndexFound_whenCreateIndex_thenThrowException() {
        ApplicationIndexCreateDTO testDTO = buildTestDTO();
        String correctIndexName = getValidIndexName(testDTO);

        HttpRequest aliasRequest = HttpRequest.request().withMethod(HttpMethod.GET.name()).withPath(format("/_cat/indices/%s", correctIndexName));
        mockServer.when(aliasRequest).respond(response().withStatusCode(OK.value()));

        doReturn(true).when(service).canCreateApplicationIndex(token, testDTO);

        MetadataException metadataException = assertThrows(MetadataException.class, () -> service.createApplicationIndex(token, testDTO));
        assertEquals(CONFLICT.value(), metadataException.getHttpStatus().value());
    }


    @Test
    void givenInvalidCustomIndexName_whenCreateIndex_thenThrowException() throws MetadataException {
        ApplicationIndexCreateDTO testDTO = buildTestDTO();
        String invalidCustomIndexName = "der marco liest sich seine Pull Requests aber ganz genau durch!!";
        testDTO.setCustomName(invalidCustomIndexName);

        MetadataException metadataException = assertThrows(MetadataException.class, () -> service.createApplicationIndex(token, testDTO));
        assertEquals(BAD_REQUEST.value(), metadataException.getHttpStatus().value());
    }


    @Test
    void givenNotAuthorised_whenCreateIndex_thenThrowException() {
        ApplicationIndexCreateDTO testDTO = buildTestDTO();
        String correctIndexName = getValidIndexName(testDTO);

        HttpRequest aliasRequest = HttpRequest.request().withMethod(HttpMethod.GET.name()).withPath(format("/_cat/indices/%s", correctIndexName));
        mockServer.when(aliasRequest).respond(response().withStatusCode(NOT_FOUND.value()));

        MetadataException metadataException = assertThrows(MetadataException.class, () -> service.createApplicationIndex(token, testDTO));
        assertEquals(FORBIDDEN.value(), metadataException.getHttpStatus().value());
    }

    @Test
    void givenInvalidIndexName_whenDeleteIndex_thenThrowException() {
        String incorrectIndexName = "one_two_three_four_five";


        MetadataException metadataException = assertThrows(MetadataException.class, () -> service.deleteApplicationIndex(incorrectIndexName, token));
        assertEquals(BAD_REQUEST.value(), metadataException.getHttpStatus().value());
    }

    @Test
    void givenInvalidApplicationPrefix_whenDeleteIndex_thenThrowException() throws MetadataException {
        String incorrectIndexName = "org_space_invalid_custom";

        doReturn(true).when(service).canDeleteApplicationIndex(token, incorrectIndexName);

        MetadataException metadataException = assertThrows(MetadataException.class, () -> service.deleteApplicationIndex(incorrectIndexName, token));
        assertEquals(BAD_REQUEST.value(), metadataException.getHttpStatus().value());
    }


    @Test
    void givenValidIndexName_whenDeleteIndex_thenSucceed() throws MetadataException {
        String incorrectIndexName = "org_space_analysis_custom";

        doReturn(true).when(service).canDeleteApplicationIndex(token, incorrectIndexName);

        assertDoesNotThrow(() -> service.deleteApplicationIndex(incorrectIndexName, token), "");
    }

    private ApplicationIndexCreateDTO buildTestDTO() {
        ApplicationIndexCreateDTO indexCreateDTO = new ApplicationIndexCreateDTO();
        indexCreateDTO.setIndexType(ApplicationIndexType.ANALYSIS);
        indexCreateDTO.setOrganizationName("testorg");
        indexCreateDTO.setSpaceName("testspace");
        indexCreateDTO.setCustomName("customname");
        return indexCreateDTO;
    }

    private String getValidIndexName(ApplicationIndexCreateDTO dto) {
        return format("%s_%s_%s_%s", dto.getOrganizationName(), dto.getSpaceName(), dto.getIndexType().getIndexAlias(), dto.getCustomName());
    }
}