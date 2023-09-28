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

import com.efs.sdk.metadata.commons.MetadataException;
import io.micrometer.core.instrument.util.IOUtils;
import io.swagger.models.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.opensearch.client.RestClient;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.efs.sdk.metadata.utils.TestHelper.findRandomPort;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class MetadataOpensearchClientTest {

    private ClientAndServer mockServer;
    private MetadataOpensearchClient mESClient;
    @MockBean
    private OpenSearchRestClientBuilder esBuilder;

    private RestClient restClient;

    @BeforeEach
    void setup() throws Exception {
        Integer port = findRandomPort();
        ConfigurationProperties.logLevel("INFO");
        mockServer = ClientAndServer.startClientAndServer(port);

        this.esBuilder = new OpenSearchRestClientBuilderTest("http://127.0.0.1:" + port);
        this.restClient = esBuilder.buildRestClient("");
        this.mESClient = new MetadataOpensearchClient();

    }

    @AfterEach
    void destroy() {
        mockServer.stop();
    }

    @Test
    void givenUuidExists_whenUuidExists_thenTrue() throws Exception {
        String index = "test";
        String uuid = "123";

        String searchResult = IOUtils.toString(getClass().getResourceAsStream("/uuidFoundResult.json"));
        HttpRequest idRequest = HttpRequest.request().withMethod(HttpMethod.GET.name()).withPath(format("/%s/_search", index));
        mockServer.when(idRequest).respond(HttpResponse.response().withBody(searchResult).withStatusCode(200));

        assertTrue(mESClient.documentExists(restClient, index, uuid));
    }

    @Test
    void givenUuidDoesNotExist_whenUuidExists_thenFalse() throws Exception {
        String index = "test";
        String uuid = "123";

        String searchResult = IOUtils.toString(getClass().getResourceAsStream("/noUuidFoundResult.json"));
        HttpRequest idRequest = HttpRequest.request().withMethod(HttpMethod.GET.name()).withPath(format("/%s/_search", index));
        mockServer.when(idRequest).respond(HttpResponse.response().withBody(searchResult).withStatusCode(200));

        assertFalse(mESClient.documentExists(restClient, index, uuid));
    }

    @Test
    void givenMetadata_whenCreateMetadata_thenOk() throws Exception {
        String metadataValue = IOUtils.toString(getClass().getResourceAsStream("/metadataPutQuery.json"));
        String index = "test";
        String uuid = "123";

        String searchResult = IOUtils.toString(getClass().getResourceAsStream("/putMetadataResult.json"));
        HttpRequest putRequest = HttpRequest.request().withMethod(HttpMethod.PUT.name()).withPath(format("/%s/_doc/%s", index, uuid));
        mockServer.when(putRequest).respond(HttpResponse.response().withBody(searchResult).withStatusCode(200));

        assertTrue(mESClient.createMetadata(restClient, index, metadataValue, uuid) > 0);
    }

    @Test
    void givenMetadata_whenUpdateData_thenOk() throws Exception {
        String metadataValue = IOUtils.toString(getClass().getResourceAsStream("/metadataPutQuery.json"));
        String index = "test";
        String uuid = "123";
        String pipeline = "update-metadata";

        String result = IOUtils.toString(getClass().getResourceAsStream("/putMetadataResult.json"));
        HttpRequest putRequest = HttpRequest.request().withMethod(HttpMethod.PUT.name()).withPath(format("/%s/_doc/%s", index, uuid));

        mockServer.when(putRequest).respond(HttpResponse.response().withBody(result).withStatusCode(200));

        assertTrue(mESClient.updateMetadata(restClient, index, uuid, metadataValue) > 0);
    }

    @Test
    void thrownIOException_whenUpdateData_thenThrowMetadataException() throws Exception {
        String metadataValue = IOUtils.toString(getClass().getResourceAsStream("/metadataPutQuery.json"));
        String index = "test";
        String uuid = "123";
        String pipeline = "update-metadata";

        HttpRequest putRequest = HttpRequest.request().withMethod(HttpMethod.PUT.name()).withPath(format("/%s/_doc/%s", index, uuid));

        mockServer.when(putRequest).error(HttpError.error().withDropConnection(true));
        assertThrows(MetadataException.class, () -> mESClient.updateMetadata(restClient, index, uuid, metadataValue));
    }
}
