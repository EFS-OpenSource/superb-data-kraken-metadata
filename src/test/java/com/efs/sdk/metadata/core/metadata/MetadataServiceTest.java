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
package com.efs.sdk.metadata.core.metadata;

import com.efs.sdk.metadata.clients.*;
import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.core.events.EventPublisher;
import com.efs.sdk.metadata.helper.EntityConverter;
import com.efs.sdk.metadata.model.MeasurementDTO;
import com.efs.sdk.metadata.model.MetadataDTO;
import com.efs.sdk.metadata.model.TokenModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.client.RestClient;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

class MetadataServiceTest {

    @MockBean
    private EventPublisher publisher;
    @MockBean
    private EntityConverter converter;
    @MockBean
    //TODO umbenennen
    private OpenSearchRestClientBuilder esClientBuilder;
    @MockBean
    private MetadataRestClient client;
    @MockBean
    private RestClient restClient;
    @MockBean
    private MetadataOpensearchClient mESClient;
    @MockBean
    private OrganizationManagerClient orgaClient;
    @MockBean
    private SchemaManagerClient schemaClient;
    private MetadataService service;

    @BeforeEach
    void setup() {
        this.publisher = Mockito.mock(EventPublisher.class);
        this.converter = Mockito.spy(new EntityConverter(new ObjectMapper()));
        this.restClient = Mockito.mock(RestClient.class);
        this.esClientBuilder = Mockito.mock(OpenSearchRestClientBuilder.class);
        this.client = Mockito.mock(MetadataRestClient.class);
        this.mESClient = Mockito.mock(MetadataOpensearchClient.class);
        this.orgaClient = Mockito.mock(OrganizationManagerClient.class);
        this.schemaClient = Mockito.mock(SchemaManagerClient.class);
        this.service = new MetadataService(publisher, converter, esClientBuilder, client, mESClient, orgaClient, schemaClient);
    }

    @Test
    void givenNoContainer_whenIndex_thenError() {
        MeasurementDTO dto = new MeasurementDTO();
        dto.setRootdir("123");
        dto.setOrganization("testorga");

        TokenModel token = new TokenModel();
        token.setAccessToken("sa-token");
        given(client.getSAToken()).willReturn(token);
        assertThrows(MetadataException.class, () -> service.index("user-token", dto));
    }

    @Test
    void givenNoAccount_whenIndex_thenError() {
        MeasurementDTO dto = new MeasurementDTO();
        dto.setRootdir("123");
        dto.setSpace("testspc");

        TokenModel token = new TokenModel();
        token.setAccessToken("sa-token");
        given(client.getSAToken()).willReturn(token);
        assertThrows(MetadataException.class, () -> service.index("user-token", dto));
    }

    @Test
    void givenNoRootDir_whenIndex_thenError() {
        MeasurementDTO dto = new MeasurementDTO();
        dto.setOrganization("testorga");
        dto.setSpace("testspc");

        TokenModel token = new TokenModel();
        token.setAccessToken("sa-token");
        given(client.getSAToken()).willReturn(token);
        assertThrows(MetadataException.class, () -> service.index("user-token", dto));
    }

    @Test
    @Disabled("somehow MetadataElasticsearchClient.createMetadata returns 0 despite assumption -> check again!!")
    // TODO
    void givenMetadata_whenIndex_thenOk() throws Exception {
        Map<String, Object> space = Map.of("name", "container");

        given(esClientBuilder.buildRestClient(anyString())).willReturn(restClient);

        given(mESClient.documentExists(any(), anyString(), anyString())).willReturn(false);
        given(client.getSAToken()).willReturn(new TokenModel("token"));

        given(mESClient.createMetadata(any(), anyString(), anyString(), anyString())).willReturn(1);
        given(orgaClient.getSpace(anyString(), anyString(), anyString(), any(OrganizationManagerClient.Permissions.class))).willReturn(space);
        given(schemaClient.indexModel(anyString(), anyString())).willReturn(1);

        ObjectMapper mapper = new ObjectMapper();

        InputStream is = getClass().getResourceAsStream("/measurement.json");
        MeasurementDTO metadata = mapper.readValue(is, MeasurementDTO.class);
        assertTrue(service.index("token", metadata));
    }

    @Test
    void givenMetadataIsCorrect_whenUpdate_thenTrue() throws Exception {
        given(esClientBuilder.buildRestClient(anyString())).willReturn(restClient);
        given(mESClient.getSourceDocument(any(RestClient.class), anyString(), anyString())).willReturn(emptyMap());
        given(mESClient.updateMetadata(any(RestClient.class), anyString(), anyString(), anyString())).willReturn(1);

        assertTrue(service.update(new MeasurementDTO(), "", "", "", "some-id"));
    }

    @Test
    void givenMetadata_whenUpdate_thenTrue() throws Exception {
        Map<String, Object> foundDocument = new HashMap<>();
        foundDocument.put("uuid", "83674ab8-23de-4a73-9003-868f9a24177c");
        foundDocument.put("space", "sdkcorestorage");
        foundDocument.put("organization", "space-container");
        foundDocument.put("metadata", Map.of("testkey", "testvalue"));
        foundDocument.put("massdata", Collections.emptyList());

        given(esClientBuilder.buildRestClient(anyString())).willReturn(restClient);
        given(mESClient.getSourceDocument(any(RestClient.class), anyString(), anyString())).willReturn(foundDocument);
        given(mESClient.updateMetadata(any(RestClient.class), anyString(), anyString(), anyString())).willReturn(1);

        assertTrue(service.update(new MeasurementDTO(), "asd", "organization", "space", "id"));
    }
}
